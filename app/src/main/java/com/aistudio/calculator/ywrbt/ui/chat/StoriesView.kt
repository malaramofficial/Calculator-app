package com.aistudio.calculator.ywrbt.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.focus.onFocusChanged
import com.aistudio.calculator.ywrbt.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Custom Theme colors matching existing structures
private val AccentBlue = Color(0xFF00F260) // High-contrast green/blue accent
private val DarkSlateBg = Color(0xFF0B0E14)
private val CardSlate = Color(0xFF151B26)
private val TextLight = Color(0xFFF3F4F6)
private val TextMuted = Color(0xFF9CA3AF)

// Parsing helpers for Gradient background stories
fun getGradientForPreset(presetKey: String): List<Color> {
    return when (presetKey) {
        "sunset" -> listOf(Color(0xFFF27121), Color(0xFFE94057), Color(0xFF8A2387))
        "midnight" -> listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
        "forest" -> listOf(Color(0xFF11998E), Color(0xFF38EF7D))
        "cosmic" -> listOf(Color(0xFF00C9FF), Color(0xFF92FE9D))
        "neon" -> listOf(Color(0xFFF12711), Color(0xFFF5AF19))
        "love" -> listOf(Color(0xFFFF007F), Color(0xFF7F00FF))
        else -> listOf(Color(0xFF141E30), Color(0xFF243B55))
    }
}

// Extract background preset key and clean status message from stored text payload
fun parseStoryContent(storedText: String): Pair<String, String> {
    return if (storedText.startsWith("[BG:") && storedText.contains("]")) {
        val closeIndex = storedText.indexOf("]")
        val preset = storedText.substring(4, closeIndex)
        val msg = storedText.substring(closeIndex + 1)
        preset to msg
    } else {
        "default" to storedText
    }
}

@Composable
fun StoriesBar(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val stories by viewModel.storiesList.collectAsState()
    val profiles by viewModel.profilesList.collectAsState()
    val currentUsername by viewModel.currentUsername.collectAsState()
    val seenStoryIds by viewModel.seenStoryIds.collectAsState()

    var showPostDialog by remember { mutableStateOf(false) }
    var viewingUsername by remember { mutableStateOf<String?>(null) }

    // Auto-populate offline sample stories to keep UI looking active if empty!
    LaunchedEffect(stories.size) {
        if (stories.isEmpty()) {
            val validSampleAuthors = listOf("priya_sharma", "aarav_official")
            for (author in validSampleAuthors) {
                val p = profiles.find { it.username == author }
                if (p != null) {
                    val sampleText = if (author == "priya_sharma") {
                        "[BG:sunset]मौसम बहुत प्यारा है! ✨ कैलकुलेटर के पीछे सुरक्षित सोशल वर्ल्ड 💫 #MyStory"
                    } else {
                        "[BG:midnight]Focused on code! 🚀 Building private app lock vault with AdMob monetization."
                    }
                    viewModel.postStoryFromProfile(author, sampleText)
                }
            }
        }
    }

    // Group active stories by authors
    val activeStoriesGrouped = remember(stories) {
        stories.groupBy { it.username }
    }

    // Identify current user's profile info
    val currentUserProfile = remember(profiles, currentUsername) {
        profiles.find { it.username == currentUsername }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "Your Story" profile item
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(68.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clickable(
                                onClick = {
                                    val myStories = activeStoriesGrouped[currentUsername]
                                    if (myStories != null && myStories.isNotEmpty()) {
                                        viewingUsername = currentUsername
                                    } else {
                                        showPostDialog = true
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing ring around the avatar
                        val myStories = activeStoriesGrouped[currentUsername] ?: emptyList()
                        val hasUnseenMyStories = myStories.any { it.id !in seenStoryIds }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawBehind {
                                    if (myStories.isNotEmpty()) {
                                        val borderBrush = if (hasUnseenMyStories) {
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(0xFFFE0879),
                                                    Color(0xFFFF5225),
                                                    Color(0xFFFFF135)
                                                )
                                            )
                                        } else {
                                            Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.5f), Color.Gray.copy(alpha = 0.5f)))
                                        }
                                        drawCircle(
                                            brush = borderBrush,
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Avatar circle representing Current User
                            val hexStr = currentUserProfile?.avatarColorHex ?: "#FF5722"
                            val parsedColor = remember(hexStr) {
                                try { Color(android.graphics.Color.parseColor(hexStr)) } catch (e: Exception) { Color(0xFFFE5225) }
                            }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(parsedColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (currentUserProfile?.fullName ?: "You").take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Cute mini rounded "+" Badge overlaid on bottom right
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 1.dp, y = 1.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(AccentBlue)
                                .border(1.5.dp, DarkSlateBg, CircleShape)
                                .clickable { showPostDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Story",
                                tint = DarkSlateBg,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your Story",
                        color = TextLight,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Other users' stories
            val otherStoryAuthors = activeStoriesGrouped.keys.filter { it != currentUsername }
            items(otherStoryAuthors) { authorUsername ->
                val authorStories = activeStoriesGrouped[authorUsername] ?: emptyList()
                if (authorStories.isNotEmpty()) {
                    val authorProfile = profiles.find { it.username == authorUsername }
                    val hasUnseen = authorStories.any { it.id !in seenStoryIds }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(68.dp)
                            .clickable { viewingUsername = authorUsername }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawBehind {
                                        val ringBrush = if (hasUnseen) {
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(0xFFFE0879),
                                                    Color(0xFFFF5225),
                                                    Color(0xFFFFF135)
                                                )
                                            )
                                        } else {
                                            Brush.linearGradient(
                                                listOf(
                                                    Color.Gray.copy(alpha = 0.4f),
                                                    Color.Gray.copy(alpha = 0.4f)
                                                )
                                            )
                                        }
                                        drawCircle(
                                            brush = ringBrush,
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Background Colored Initials Avatar
                                val hexStr = authorProfile?.avatarColorHex ?: "#2196F3"
                                val parsedColor = remember(hexStr) {
                                    try { Color(android.graphics.Color.parseColor(hexStr)) } catch (e: Exception) { Color(0xFF2196F3) }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(parsedColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (authorProfile?.fullName ?: authorUsername).take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = authorProfile?.fullName ?: authorUsername,
                            color = if (hasUnseen) TextLight else TextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Post Story Screen / Dialog
    if (showPostDialog) {
        PostStoryDialog(
            onDismiss = { showPostDialog = false },
            onPost = { text, imageUri ->
                viewModel.postStory(text, imageUri)
                showPostDialog = false
            }
        )
    }

    // Story Viewer Page Slider Dialog
    if (viewingUsername != null) {
        val targetStories = activeStoriesGrouped[viewingUsername] ?: emptyList()
        if (targetStories.isNotEmpty()) {
            StoryViewerDialog(
                username = viewingUsername!!,
                stories = targetStories,
                viewModel = viewModel,
                isOwnStories = (viewingUsername == currentUsername),
                onDismiss = { viewingUsername = null }
            )
        } else {
            viewingUsername = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostStoryDialog(
    onDismiss: () -> Unit,
    onPost: (String, String?) -> Unit
) {
    var storyText by remember { mutableStateOf("") }
    val presets = listOf("sunset", "midnight", "forest", "cosmic", "neon", "love")
    var selectedPreset by remember { mutableStateOf(presets[0]) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("नया स्टेटस (Create Status)", color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextLight)
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                if (storyText.trim().isNotEmpty()) {
                                    // Save customized gradient inside the stored text string
                                    val formattedPayload = "[BG:$selectedPreset]${storyText.trim()}"
                                    onPost(formattedPayload, null)
                                }
                            },
                            enabled = storyText.trim().isNotEmpty()
                        ) {
                            Text(
                                "Post (लगाएं)",
                                color = if (storyText.trim().isNotEmpty()) AccentBlue else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSlateBg)
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkSlateBg)
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive Status Typing Area with beautiful gradients
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(colors = getGradientForPreset(selectedPreset))
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextField(
                        value = storyText,
                        onValueChange = { if (it.length <= 150) storyText = it },
                        placeholder = {
                            Text(
                                "क्या चल रहा है? यहाँ लिखें...\n(Type status update here)",
                                color = Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                fontSize = 20.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color.White
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 28.sp
                        ),
                        modifier = Modifier.fillMaxSize()
                    )

                    Text(
                        text = "${150 - storyText.length} characters left",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Beautiful Color Gradient Preset Picker Row
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "रंगीन थीम चुनें (Select theme):",
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(presets) { preset ->
                            val isSelected = (preset == selectedPreset)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(colors = getGradientForPreset(preset))
                                    )
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) TextLight else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedPreset = preset }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoryViewerDialog(
    username: String,
    stories: List<UserStory>,
    viewModel: CalculatorViewModel,
    isOwnStories: Boolean,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var currentIndex by remember { mutableStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }

    val currentStory = stories.getOrNull(currentIndex)
    val profiles by viewModel.profilesList.collectAsState()
    val authorProfile = remember(profiles, username) {
        profiles.find { it.username == username }
    }

    // Reply states
    var replyText by remember { mutableStateOf("") }
    var showReplySentSnackbar by remember { mutableStateOf(false) }

    // Read status syncing
    LaunchedEffect(currentStory) {
        currentStory?.let {
            viewModel.markStoryAsSeen(it.id)
        }
    }

    // Auto advancing and progress bar updating timer logic
    LaunchedEffect(currentIndex, isPaused) {
        if (isPaused) return@LaunchedEffect
        progress = 0f
        val durationSteps = 40 // 4 seconds (40 * 100ms)
        for (step in 1..durationSteps) {
            delay(100)
            progress = step.toFloat() / durationSteps
        }
        // Advance story index
        if (currentIndex < stories.size - 1) {
            currentIndex++
        } else {
            // Dismiss when last story completed
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val width = size.width
                            // Tap left 30% to go back, otherwise go forward
                            if (offset.x < width * 0.3f) {
                                if (currentIndex > 0) {
                                    currentIndex--
                                } else {
                                    onDismiss()
                                }
                            } else {
                                if (currentIndex < stories.size - 1) {
                                    currentIndex++
                                } else {
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
        ) {
            if (currentStory != null) {
                val (preset, displayedText) = remember(currentStory.text) {
                    parseStoryContent(currentStory.text)
                }

                // Fill gradient status background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(colors = getGradientForPreset(preset))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayedText,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp)
                    )
                }
            }

            // Foreground UI Overlays: Indicators, Metadata, Controls
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Segmented Stories progress bar (like Instagram/WhatsApp)
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        stories.forEachIndexed { index, _ ->
                            val itemProgress = when {
                                index < currentIndex -> 1f
                                index > currentIndex -> 0f
                                else -> progress
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(Color.White.copy(alpha = 0.35f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(itemProgress)
                                        .background(Color.White)
                                )
                            }
                        }
                    }

                    // Avatar & Author Row info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val hexStr = authorProfile?.avatarColorHex ?: "#2196F3"
                            val parsedColor = remember(hexStr) {
                                try { Color(android.graphics.Color.parseColor(hexStr)) } catch (e: Exception) { Color(0xFF2196F3) }
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(parsedColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (authorProfile?.fullName ?: username).take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = authorProfile?.fullName ?: username,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentStory?.let { formatStoryTime(it.timestamp) } ?: "",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Close dialog option
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Interactive Bottom Action (Reply / Delete)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isOwnStories && currentStory != null) {
                        // Delete own story button
                        Button(
                            onClick = {
                                viewModel.deleteStory(currentStory.id)
                                if (currentIndex < stories.size - 1) {
                                    currentIndex++
                                } else {
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.82f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("स्टोरी हटाएं (Delete Story)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else if (currentStory != null) {
                        // Instagram-style swipe-up / quick reply message input box
                        var isFocusingReply by remember { mutableStateOf(false) }

                        LaunchedEffect(isFocusingReply) {
                            if (isFocusingReply) isPaused = true
                        }

                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("स्टोरी पर रिप्लाई भेजें...", color = Color.White.copy(alpha = 0.6f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                cursorColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isFocusingReply = true }
                                .onFocusChanged { state ->
                                    if (state.isFocused) {
                                        isPaused = true
                                    }
                                },
                            singleLine = true
                        )

                        IconButton(
                            onClick = {
                                if (replyText.trim().isNotEmpty()) {
                                    val formattedWithStoryContext = "📲 Stories Reply: \"${parseStoryContent(currentStory.text).second}\" — $replyText"
                                    viewModel.sendInstagramStoryReply(username, formattedWithStoryContext)
                                    replyText = ""
                                    isPaused = false
                                    showReplySentSnackbar = true
                                    coroutineScope.launch {
                                        delay(1500)
                                        showReplySentSnackbar = false
                                        onDismiss()
                                    }
                                }
                            },
                            modifier = Modifier
                                .background(Color.White, CircleShape)
                                .size(44.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.Send, contentDescription = "Send Reply", tint = Color.Black)
                        }
                    }
                }
            }

            // Beautiful customized pop-up notification on quick reply sent success!
            AnimatedVisibility(
                visible = showReplySentSnackbar,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            ) {
                Surface(
                    color = AccentBlue,
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 6.dp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        "रिप्लाई भेज दिया गया है! (Reply sent!)",
                        color = DarkSlateBg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

private fun formatStoryTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    if (diff < 60000L) {
        return "Just now"
    }
    val minutes = diff / 60000L
    if (minutes < 60) {
        return "${minutes}m ago"
    }
    val hours = minutes / 60
    return "${hours}h ago"
}
