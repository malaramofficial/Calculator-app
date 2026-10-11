package com.aistudio.calculator.ywrbt

import java.util.Locale

object ExpressionEvaluator {
    fun evaluate(expression: String): String {
        val cleanExpr = expression.replace("×", "*").replace("÷", "/")
        if (cleanExpr.isBlank()) return "0"
        return try {
            val result = eval(cleanExpr)
            if (!result.isFinite()) {
                "Error"
            } else {
                // Avoid Long overflow when formatting very large whole numbers.
                if (result % 1.0 == 0.0 &&
                    result >= Long.MIN_VALUE.toDouble() &&
                    result < Long.MAX_VALUE.toDouble()
                ) {
                    result.toLong().toString()
                } else {
                    // Use a stable decimal separator regardless of device locale.
                    String.format(Locale.US, "%.6f", result).trimEnd('0').trimEnd('.')
                }
            }
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun eval(str: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm() // addition
                    else if (eat('-'.code)) x -= parseTerm() // subtraction
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor() // multiplication
                    else if (eat('/'.code)) {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor // division
                    } else return x
                }
            }

            fun parseFactor(): Double {
                while (ch != -1 && ch.toChar().isWhitespace()) nextChar()
                if (eat('+'.code)) return +parseFactor() // unary plus
                if (eat('-'.code)) return -parseFactor() // unary minus

                var x: Double
                val startPos = pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    if (!eat(')'.code)) throw RuntimeException("Missing closing parenthesis")
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) { // numbers
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    x = str.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }
                return x
            }
        }.parse()
    }
}
