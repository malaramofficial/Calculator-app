package com.aistudio.calculator.ywrbt.ui.profile

fun isOnlineUserCheckShared(username: String, lastActive: Long): Boolean {
    val isVirtual = username.trim().lowercase() in listOf("instagram_support", "sneha_kapoor", "rahul_dev", "elon_musk")
    if (isVirtual) return true
    return (System.currentTimeMillis() - lastActive) < 180_000 // 3 minutes
}

object SettingsLoc {
    fun t(key: String, lang: String): String {
        return when (lang) {
            "hi" -> when (key) {
                "settings_title" -> "सेटिंग्स (Settings)"
                "theme_section" -> "दिखावट और रंग थीम"
                "theme_desc" -> "अपनी पसंदीदा प्रीमियम डिज़ाइन थीम चुनें"
                "theme_slate" -> "डार्क स्लेट"
                "theme_cosmic" -> "कॉस्मिक मिडनाइट"
                "theme_forest" -> "फॉरेस्ट एमराल्ड"
                "theme_amethyst" -> "शाही नीलम"
                "theme_love_velvet" -> "मखमली प्यार ❤️"
                
                "lang_section" -> "भाषा सेटिंग"
                "lang_desc" -> "चैट और सिस्टम संवाद भाषा चुनें"
                "lang_hi" -> "हिन्दी (Hindi)"
                "lang_en" -> "English"
                "lang_hinglish" -> "Hinglish (हिन्गलिश)"
                
                "sound_section" -> "मैसेज रिंगटोन और अलर्ट"
                "sound_desc" -> "संदेश आने पर बजने वाली टोन चुनें"
                "sound_bell" -> "🔔 डिफ़ॉल्ट टोन (Bell)"
                "sound_bubble" -> "🫧 वाटर बबल (Bubble)"
                "sound_digital" -> "🎵 डिजिटल अलर्ट (Alert)"
                "sound_silent" -> "🔕 मूक (Silent)"
                
                "vibrate_title" -> "मैसेज आने पर वाइब्रेट करें"
                "vibrate_desc" -> "अधिसूचनाओं पर वाइब्रेशन अलर्ट"
                "seen_title" -> "पढ़ने के टिक (Read Receipts)"
                "seen_desc" -> "दूसरों को पता चलने दें कि आपने संदेश पढ़ा है"
                "bg_sync_title" -> "व्हाट्सएप स्टाइल बैकग्राउंड नोटिफिकेशन (Background Sync)"
                "bg_sync_desc" -> "एप्लिकेशन बंद होने पर भी व्हाट्सएप की तरह तुरंत नए संदेशों के नोटिफिकेशन प्राप्त करें"
                
                "account_info" -> "सत्यापित गूगल खाता"
                "app_version" -> "संस्करण (Version)"
                "dev_credit" -> "सेफ्टी फ़ायरवॉल और सेफ स्टोरेज सक्रिय है"
                "close" -> "बंद करें"
                "save" -> "सुरक्षित करें"
                "toast_theme" -> "थीम सफलतापूर्वक अपडेट की गई!"
                "toast_lang" -> "भाषा बदलकर हिन्दी/English सक्रिय की गई!"
                "toast_sound" -> "रिंगटोन चयन अपडेट हुआ!"
                "badge_profile_details" -> "प्रोफ़ाइल विवरण"
                "badge_bio" -> "बायो (Bio)"
                "badge_status" -> "सुरक्षा स्थिति"
                "badge_online" -> "ऑनलाइन (Online)"
                "badge_offline" -> "ऑफ़लाइन (Offline)"
                "badge_start_chat" -> "सुरक्षित संदेश भेजें"
                else -> key
            }
            "hinglish" -> when (key) {
                "settings_title" -> "Settings"
                "theme_section" -> "Look & Theme"
                "theme_desc" -> "Apna pasandida premium style select karein"
                "theme_slate" -> "Dark Slate (Default)"
                "theme_cosmic" -> "Cosmic Midnight"
                "theme_forest" -> "Forest Emerald"
                "theme_amethyst" -> "Royal Amethyst"
                "theme_love_velvet" -> "Velvet Love ❤️"
                
                "lang_section" -> "Language"
                "lang_desc" -> "System aur chat ki language choice"
                "lang_hi" -> "हिन्दी (Hindi)"
                "lang_en" -> "English"
                "lang_hinglish" -> "Hinglish"
                
                "sound_section" -> "Message Ringtone & Alerts"
                "sound_desc" -> "Automatic alert tones for new chats"
                "sound_bell" -> "🔔 Bell Ringtone"
                "sound_bubble" -> "🫧 Water Bubble Sound"
                "sound_digital" -> "🎵 Digital Beep Alert"
                "sound_silent" -> "🔕 Silent Mode"
                
                "vibrate_title" -> "Notification Vibration"
                "vibrate_desc" -> "Aane wale message par haptic alert"
                "seen_title" -> "Read Receipts (Double Green Tick)"
                "seen_desc" -> "Dusro ko batayein ki aapne read kar liya"
                "bg_sync_title" -> "WhatsApp Style Background Sync"
                "bg_sync_desc" -> "App band hone par bhi instant messages ke notifications payein"
                
                "account_info" -> "Verified Google Account"
                "app_version" -> "App Version"
                "dev_credit" -> "Premium Safety Firewall Activated"
                "close" -> "Close Settings"
                "save" -> "Save Changes"
                "toast_theme" -> "Theme changes applied successfully!"
                "toast_lang" -> "Language updated!"
                "toast_sound" -> "Ringtone updated successfully!"
                "badge_profile_details" -> "Profile Badge Info"
                "badge_bio" -> "User Bio"
                "badge_status" -> "Active Status"
                "badge_online" -> "Online Abhie"
                "badge_offline" -> "Offline Hai"
                "badge_start_chat" -> "Secure Message Karein"
                else -> key
            }
            else -> when (key) { // english default
                "settings_title" -> "Settings"
                "theme_section" -> "Appearance & Color Theme"
                "theme_desc" -> "Select your personalized premium theme design"
                "theme_slate" -> "Dark Slate (Default)"
                "theme_cosmic" -> "Cosmic Midnight"
                "theme_forest" -> "Forest Emerald"
                "theme_amethyst" -> "Royal Amethyst"
                "theme_love_velvet" -> "Velvet Love ❤️"
                
                "lang_section" -> "App Language"
                "lang_desc" -> "Select the display & interaction language"
                "lang_hi" -> "हिन्दी (Hindi)"
                "lang_en" -> "English"
                "lang_hinglish" -> "Hinglish (हिन्गलिश)"
                
                "sound_section" -> "Message Ringtone & Alerts"
                "sound_desc" -> "Choose the alert tone for incoming messages"
                "sound_bell" -> "🔔 Standard Bell Ringtone"
                "sound_bubble" -> "🫧 Water Bubble Sound"
                "sound_digital" -> "🎵 Digital Sharp Beep"
                "sound_silent" -> "🔕 Silent / Mute"
                
                "vibrate_title" -> "Vibrate on Message Toast"
                "vibrate_desc" -> "Haptic vibrations for incoming alerts"
                "seen_title" -> "Read Receipts (Seen tag)"
                "seen_desc" -> "Allow others to see when you read their text"
                "bg_sync_title" -> "Background Message Sync (WhatsApp Style)"
                "bg_sync_desc" -> "Receive instant notifications even when the app is completely closed"
                
                "account_info" -> "Verified Google Account"
                "app_version" -> "App Version"
                "dev_credit" -> "Encrypted Database Firewall Active"
                "close" -> "Dismiss Settings"
                "save" -> "Save Preferences"
                "toast_theme" -> "Theme updated successfully!"
                "toast_lang" -> "Language successfully switched!"
                "toast_sound" -> "Ringtone successfully changed!"
                "badge_profile_details" -> "Profile Info Badge"
                "badge_bio" -> "Bio Description"
                "badge_status" -> "Security Clearance & Status"
                "badge_online" -> "Online Active"
                "badge_offline" -> "Offline"
                "badge_start_chat" -> "Send Secure Message"
                else -> key
            }
        }
    }
}
