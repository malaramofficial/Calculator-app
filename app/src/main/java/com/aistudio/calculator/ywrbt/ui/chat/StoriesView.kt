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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
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

// Extract frame theme and story caption from stored text payload
fun parsePhotoStory(storedText: String): Pair<String, String> {
    return if (storedText.startsWith("[FRAME:") && storedText.contains("]")) {
        val closeIndex = storedText.indexOf("]")
        val theme = storedText.substring(7, closeIndex)
        val caption = storedText.substring(closeIndex + 1)
        theme to caption
    } else {
        "polaroid" to storedText
    }
}

// Persist the picked image in internal sandbox storage cleanly
fun saveStoryImageToInternal(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val extension = context.contentResolver.getType(uri)?.substringAfterLast("/") ?: "jpg"
        val fileName = "story_img_${System.currentTimeMillis()}.$extension"
        val destFile = java.io.File(context.filesDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        destFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun PolaroidFrame(
    imageUri: String?,
    caption: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp)
            .shadow(12.dp, RoundedCornerShape(4.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBF9F2)), // Warm retro off-white
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, Color(0xFFE3DCCF))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Standard photo frame square aspect ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Polaroid shot",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Cursive handwritten retro caption
            Text(
                text = caption.ifEmpty { "पल यादें... 💭" },
                color = Color(0xFF2E2722),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NeonGlassFrame(
    imageUri: String?,
    caption: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Dynamic Neon vignette overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // Frosted Glass custom block
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(18.dp)
                .border(
                    BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = caption.ifEmpty { "🌌 Cosmic Glass..." },
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun PaperJournalFrame(
    imageUri: String?,
    caption: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFFFAF8EA)) // Textured beige diary paper
            .padding(24.dp)
            .drawBehind {
                // Faint brown-lined journal grids
                val strokeWidth = 1f
                val spacing = 26.dp.toPx()
                var currentY = 130.dp.toPx()
                while (currentY < size.height) {
                    drawLine(
                        color = Color(0xFFEADACD).copy(alpha = 0.5f), // Clean elegant faint brown line
                        start = androidx.compose.ui.geometry.Offset(20.dp.toPx(), currentY),
                        end = androidx.compose.ui.geometry.Offset(size.width - 20.dp.toPx(), currentY),
                        strokeWidth = strokeWidth
                    )
                    currentY += spacing
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Angled stuck memo format with an tape seal on top
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxWidth(0.9f)
                    .graphicsLayer(rotationZ = -2.5f)
                    .shadow(8.dp, RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
                
                // Old translucent desk tape overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-12).dp)
                        .width(64.dp)
                        .height(16.dp)
                        .graphicsLayer(alpha = 0.55f)
                        .background(Color(0xFFE4D2B5), RoundedCornerShape(2.dp))
                        .border(0.5.dp, Color(0xFFC0AA8C), RoundedCornerShape(2.dp))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Journal logs
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "MEMO WRITING • कलात्मक जर्नल",
                    color = Color(0xFFAA7039),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = caption.ifEmpty { "आज मेरे विचारों की डायरी..." },
                    color = Color(0xFF332D2A),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 26.sp
                )
            }
        }
    }
}

@Composable
fun AmbientAuroraFrame(
    imageUri: String?,
    caption: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // High blurred underlying image
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = 0.5f)
                    .blur(28.dp)
            )
        }
        
        // Deep radial cover
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        radius = 1100f
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        BorderStroke(
                            3.dp,
                            Brush.linearGradient(
                                listOf(Color(0xFFFF007F), Color(0xFF7F00FF))
                            )
                        ),
                        RoundedCornerShape(28.dp)
                    )
                    .shadow(16.dp, RoundedCornerShape(28.dp))
                    .background(Color.DarkGray)
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .weight(0.7f),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = caption.ifEmpty { "🔮 आवा मंडल (Aurora glow)..." },
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 25.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun CinemaWideFrame(
    imageUri: String?,
    caption: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.background(Color.Black),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Aesthetic caption details
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.3f)
                .background(Color.Black),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "🎬 STILL FRAME FROM MEMORIES",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 24.dp)
            )
        }

        // Center cinema snapshot area (21:9 formatting feel)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0F0F0F)),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.25f)
                            )
                        )
                    )
            )
        }

        // Beautiful subtitle captions on widescreen bottom border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f)
                .background(Color.Black)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (caption.isNotEmpty()) "“ $caption ”" else "सिनेमैटिक पल...",
                color = Color(0xFFFEE140), // Classic yellow movie subtitle color tone
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun DispatchFrameTheme(
    themeKey: String,
    imageUri: String?,
    caption: String,
    modifier: Modifier = Modifier
) {
    when (themeKey) {
        "polaroid" -> PolaroidFrame(imageUri, caption, modifier)
        "glass" -> NeonGlassFrame(imageUri, caption, modifier)
        "journal" -> PaperJournalFrame(imageUri, caption, modifier)
        "aurora" -> AmbientAuroraFrame(imageUri, caption, modifier)
        "cinema" -> CinemaWideFrame(imageUri, caption, modifier)
        else -> PolaroidFrame(imageUri, caption, modifier)
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
    val context = LocalContext.current
    var storyText by remember { mutableStateOf("") }
    val presets = listOf("sunset", "midnight", "forest", "cosmic", "neon", "love")
    var selectedPreset by remember { mutableStateOf(presets[0]) }

    // Image integration states
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedFrameTheme by remember { mutableStateOf("polaroid") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedImageUri = uri
            }
        }
    )

    val frameThemes = listOf(
        "polaroid" to "🖼️ Polaroid",
        "glass" to "🌌 Cosmic Glass",
        "journal" to "📝 Journal",
        "aurora" to "🔮 Aurora Glow",
        "cinema" to "🎬 Cinema Wide"
    )

    val isPostButtonEnabled = if (selectedImageUri != null) true else storyText.trim().isNotEmpty()

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
                    title = {
                        Text(
                            text = if (selectedImageUri != null) "फोटो स्टेटस बनाएं" else "नया टेक्स्ट स्टेटस",
                            color = TextLight,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextLight)
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                if (selectedImageUri != null) {
                                    val clonedPath = saveStoryImageToInternal(context, selectedImageUri!!)
                                    if (clonedPath != null) {
                                        val payload = "[FRAME:$selectedFrameTheme]${storyText.trim()}"
                                        onPost(payload, clonedPath)
                                    }
                                } else {
                                    val formattedPayload = "[BG:$selectedPreset]${storyText.trim()}"
                                    onPost(formattedPayload, null)
                                }
                            },
                            enabled = isPostButtonEnabled
                        ) {
                            Text(
                                "Post (लगाएं)",
                                color = if (isPostButtonEnabled) AccentBlue else TextMuted,
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
                // Interactive Status Typing/Preview Area with templates
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSlate)
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        // Show beautiful custom live frame preview
                        DispatchFrameTheme(
                            themeKey = selectedFrameTheme,
                            imageUri = selectedImageUri.toString(),
                            caption = storyText,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Standard Typing Canvas for Text story with gradients
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
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
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedImageUri != null) {
                    // Photo customization interface: caption field + frame themes switching
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Text input field for the photo caption
                        OutlinedTextField(
                            value = storyText,
                            onValueChange = { if (it.length <= 100) storyText = it },
                            placeholder = { Text("फोटो के बारे में कुछ लिखें (Type caption here)...", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = CardSlate,
                                focusedContainerColor = CardSlate,
                                unfocusedContainerColor = CardSlate
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            singleLine = true
                        )

                        // Visual unique themes switcher row
                        Text(
                            "अनोखा थीम फ्रेम चुनें (Select Visual Layout Frame):",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(frameThemes) { (themeKey, themeLabel) ->
                                val isSelected = (themeKey == selectedFrameTheme)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) AccentBlue else CardSlate)
                                        .border(1.dp, if (isSelected) Color.White else Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .clickable { selectedFrameTheme = themeKey }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = themeLabel,
                                        color = if (isSelected) DarkSlateBg else TextLight,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Remove photo action button
                        Button(
                            onClick = { selectedImageUri = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("फोटो हटाएं (Remove Photo & Go Text)", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                } else {
                    // Traditional Gradient theme picker + Option to add visual image status
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "रंगीन थीम चुनें (Select background):",
                            color = TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
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

                        // Premium Add Image action center
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = DarkSlateBg)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "गैलरी से फोटो जोड़ें (Add Image Story)",
                                color = DarkSlateBg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getCleanStoryText(storedText: String): String {
    return when {
        storedText.startsWith("[BG:") && storedText.contains("]") -> {
            storedText.substring(storedText.indexOf("]") + 1)
        }
        storedText.startsWith("[FRAME:") && storedText.contains("]") -> {
            storedText.substring(storedText.indexOf("]") + 1)
        }
        else -> storedText
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
                if (!currentStory.imageUri.isNullOrEmpty()) {
                    val (frameKey, caption) = remember(currentStory.text) {
                        parsePhotoStory(currentStory.text)
                    }
                    DispatchFrameTheme(
                        themeKey = frameKey,
                        imageUri = currentStory.imageUri,
                        caption = caption,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
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
                                    val cleanText = getCleanStoryText(currentStory.text).ifEmpty { "Photo Status" }
                                    val formattedWithStoryContext = "📲 Stories Reply: \"$cleanText\" — $replyText"
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
