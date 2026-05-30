package com.aistudio.calculator.ywrbt.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.BlendMode
import coil.compose.AsyncImage
import com.aistudio.calculator.ywrbt.*
import com.aistudio.calculator.ywrbt.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ReelsScreen(viewModel: CalculatorViewModel) {
    val reels by viewModel.reelsList.collectAsState()
    val profiles by viewModel.profilesList.collectAsState()
    val currentUsername by viewModel.currentUsername.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showCreateStudio by remember { mutableStateOf(false) }
    var activeCommentsReelId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchGlobalReels()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (reels.isEmpty()) {
            // Empty placeholder state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Reels Yet",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Be the first to create and share an amazing cinematic Reel on the global feed!",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showCreateStudio = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRose)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create Reel")
                }
            }
        } else {
            val listState = rememberLazyListState()
            
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = true
            ) {
                itemsIndexed(reels) { index, reel ->
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .background(Color.Black)
                    ) {
                        ReelItemPlayer(
                            reel = reel,
                            viewModel = viewModel,
                            currentUsername = currentUsername,
                            onOpenComments = { activeCommentsReelId = reel.id }
                        )
                    }
                }
            }

            // Floater Create Icon of Instagram Reels (Camera in Top Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reels",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                IconButton(
                    onClick = { showCreateStudio = true },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoCall,
                        contentDescription = "Create Reel",
                        tint = Color.White
                    )
                }
            }
        }

        // Live Real-Time Comments Overlay Sheet/Dialog
        if (activeCommentsReelId != null) {
            val targetedReel = reels.find { it.id == activeCommentsReelId }
            if (targetedReel != null) {
                ReelCommentsDialog(
                    reel = targetedReel,
                    viewModel = viewModel,
                    onDismiss = { activeCommentsReelId = null }
                )
            }
        }

        // Create Reels Studio
        if (showCreateStudio) {
            CreateReelStudioDialog(
                viewModel = viewModel,
                onDismiss = { showCreateStudio = false }
            )
        }
    }
}

@Composable
fun ReelItemPlayer(
    reel: UserReel,
    viewModel: CalculatorViewModel,
    currentUsername: String,
    onOpenComments: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val profiles by viewModel.profilesList.collectAsState()
    val followersMap by viewModel.followersMap.collectAsState()

    val authorProfile = remember(profiles, reel.username) {
        profiles.find { it.username == reel.username }
    }
    val myFollowing = followersMap[currentUsername] ?: emptyList()
    val isFollowingAuthor = myFollowing.contains(reel.username)

    // Double tap heart animation states
    var showBigHeart by remember { mutableStateOf(false) }
    var heartScale = remember { Animatable(0f) }

    // Vinyl Record spin animation
    val infiniteTransition = rememberInfiniteTransition(label = "music_vinyl")
    val spinningAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    // Apply color filter matrix overlay dynamically
    val colorFilter = when (reel.filter) {
        "Sunset" -> ColorFilter.tint(Color(0xFFFF7F50).copy(alpha = 0.22f), BlendMode.ColorBurn)
        "Cyberpunk" -> ColorFilter.tint(Color(0xFFEC4899).copy(alpha = 0.16f), BlendMode.Color)
        "Cosmic" -> ColorFilter.tint(Color(0xFF3B82F6).copy(alpha = 0.2f), BlendMode.Plus)
        "Noir" -> ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) })
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(reel.id) {
                detectTapGestures(
                    onDoubleTap = {
                        coroutineScope.launch {
                            if (!reel.isLikedByMe) {
                                viewModel.likeReel(reel.id)
                            }
                            showBigHeart = true
                            heartScale.snapTo(0f)
                            heartScale.animateTo(
                                targetValue = 1.3f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                            delay(450)
                            heartScale.animateTo(0f, tween(150))
                            showBigHeart = false
                        }
                    },
                    onTap = {
                        // Toggle play/pause simulation indicator
                        Toast.makeText(context, "Double-tap to dynamic like!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
    ) {
        // Dynamic full embed backdrop
        if (!reel.mediaUrl.isNullOrEmpty()) {
            AsyncImage(
                model = reel.mediaUrl,
                contentDescription = "Reel Backdrop",
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // High aesthetic backdrop gradients
            val backgroundGradient = when (reel.filter) {
                "Sunset" -> Brush.verticalGradient(listOf(Color(0xFFF43F5E), Color(0xFFF59E0B)))
                "Cyberpunk" -> Brush.linearGradient(listOf(Color(0xFFA855F7), Color(0xFFEC4899)))
                "Cosmic" -> Brush.sweepGradient(listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6), Color(0xFF1E3A8A)))
                "Noir" -> Brush.verticalGradient(listOf(Color(0xFF475569), Color(0xFF0F172A)))
                else -> Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF020617)))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(140.dp)
                )
            }
        }

        // Cinematic Dark Vignette Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.82f)
                        ),
                        startY = 0f
                    )
                )
        )

        // Floating Action Buttons on the Right Margin
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 54.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Heart/Like button with live updates
            val heartTint = if (reel.isLikedByMe) Color.Red else Color.White
            val heartIcon = if (reel.isLikedByMe) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { viewModel.likeReel(reel.id) },
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .border(0.5.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        imageVector = heartIcon,
                        contentDescription = "Like Reel",
                        tint = heartTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${reel.likesCount}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Global comments trigger
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onOpenComments,
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .border(0.5.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ModeComment,
                        contentDescription = "Reel Comments",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${reel.commentsCount}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Share / Copy Link option
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("reel_link", "Instagram Reel from @${reel.username}")
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Reel Link Copied!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .size(46.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Rotating Vinyl Sound label indicator
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .rotate(spinningAngle)
                    .background(Color(0xFF1E293B))
                    .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(AccentBlue, AccentRose)
                            )
                        )
                )
            }
        }

        // Overlay Poster Info on the Bottom Left Block
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(bottom = 24.dp, start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Profile & username info row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val parsedColor = try {
                    Color(android.graphics.Color.parseColor(reel.avatarColorHex))
                } catch (e: Exception) {
                    AccentBlue
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(parsedColor)
                        .border(1.5.dp, Color.White, CircleShape)
                        .clickable {
                            viewModel.showProfileBadge(authorProfile ?: ChatProfile(reel.username, reel.fullName, "", reel.avatarColorHex))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (reel.avatarUrl != null) {
                        AsyncImage(
                            model = reel.avatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = reel.username.take(2).uppercase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "@${reel.username}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        viewModel.showProfileBadge(authorProfile ?: ChatProfile(reel.username, reel.fullName, "", reel.avatarColorHex))
                    }
                )

                // Follow button next to name for quick discovery
                if (reel.username != currentUsername) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .background(if (isFollowingAuthor) Color.Transparent else AccentBlue.copy(alpha = 0.8f))
                            .clickable {
                                if (isFollowingAuthor) {
                                    viewModel.unfollowUser(reel.username)
                                } else {
                                    viewModel.followUser(reel.username)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isFollowingAuthor) "Following" else "Follow",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Caption details
            Text(
                text = reel.caption,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            // Music track label scrolling overlay (Ig aesthetic ticker)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = reel.musicTrack,
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Floating popup Big Heart animation
        if (showBigHeart) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.Red.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(110.dp)
                        .scale(heartScale.value)
                )
            }
        }
    }
}

@Composable
fun ReelCommentsDialog(
    reel: UserReel,
    viewModel: CalculatorViewModel,
    onDismiss: () -> Unit
) {
    var commentsList by remember { mutableStateOf<List<ReelComment>>(emptyList()) }
    var activeCommentInput by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Query and listen comments live
    DisposableEffect(reel.id) {
        val listener = viewModel.listenReelComments(reel.id) { comments ->
            commentsList = comments
        }
        onDispose {
            listener?.remove()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                color = CardSlate
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with counts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comments (${commentsList.size})",
                        color = TextLight,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextLight)
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                // Scroll comments
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (commentsList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No comments yet. Write yours below!",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        items(commentsList.size) { index ->
                            val comm = commentsList[index]
                            val parsedColor = try {
                                Color(android.graphics.Color.parseColor(comm.avatarColorHex))
                            } catch (e: Exception) {
                                AccentBlue
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(parsedColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (comm.avatarUrl != null) {
                                        AsyncImage(
                                            model = comm.avatarUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = comm.username.take(2).uppercase(),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "@${comm.username}",
                                            color = TextLight,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val timeStr = android.text.format.DateUtils.getRelativeTimeSpanString(comm.timestamp).toString()
                                        Text(
                                            text = timeStr,
                                            color = TextMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = comm.text,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 13.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Comment composer row
                Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextField(
                        value = activeCommentInput,
                        onValueChange = { activeCommentInput = it },
                        placeholder = { Text("Add comment info...", color = TextMuted, fontSize = 13.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(alpha = 0.25f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.25f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 80.dp)
                    )

                    IconButton(
                        onClick = {
                            if (activeCommentInput.trim().isNotEmpty()) {
                                viewModel.postReelComment(reel.id, activeCommentInput)
                                activeCommentInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(AccentBlue, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReelStudioDialog(
    viewModel: CalculatorViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var captionText by remember { mutableStateOf("") }
    
    val musicTracks = listOf(
        "Lofi Sunset Groove • Original Audio",
        "Cyberpunk Synthwave 2088",
        "Space Odyssey Cinematic Echo",
        "Urban Tech-house Club",
        "Acoustic Romance Instrumental",
        "Classic Piano Keys Melody"
    )
    var selectedMusic by remember { mutableStateOf(musicTracks[0]) }

    val filters = listOf("Normal", "Sunset", "Cyberpunk", "Cosmic", "Noir")
    var selectedFilter by remember { mutableStateOf(filters[0]) }

    // Selected image URI
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isPosting by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedImageUri = uri
            }
        }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
            color = CardSlate
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create Reel Studio 🤩",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Frame or mockup preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .clickable {
                            imagePicker.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected Reel",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Tap to Change", color = Color.White, fontSize = 9.sp)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Select Backdrop Photo",
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Click to open device pictures",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Caption details
                Text(
                    text = "Caption",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    placeholder = { Text("What is this reel about?", color = Color.Gray, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Audio Track selection list
                Text(
                    text = "Select Audio Music Background",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(count = musicTracks.size) { index ->
                        val item = musicTracks[index]
                        val isMatched = selectedMusic == item
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isMatched) AccentRose else Color.Black.copy(alpha = 0.35f))
                                .border(1.dp, if (isMatched) AccentRose else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .clickable { selectedMusic = item }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (isMatched) Color.White else Color.Gray,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.substringBefore(" •"),
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Filters
                Text(
                    text = "Cinematic Visual Tint Filter",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(count = filters.size) { index ->
                        val filt = filters[index]
                        val isMatched = selectedFilter == filt
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isMatched) AccentBlue else Color.Black.copy(alpha = 0.35f))
                                .border(1.dp, if (isMatched) AccentBlue else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .clickable { selectedFilter = filt }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = filt,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Submit button with loading validation
                Button(
                    onClick = {
                        if (captionText.trim().isEmpty()) {
                            Toast.makeText(context, "Please enter a caption first!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isPosting = true
                        viewModel.postReel(
                            caption = captionText,
                            mediaUrl = selectedImageUri?.toString(),
                            musicTrack = selectedMusic,
                            filter = selectedFilter
                        )
                        Toast.makeText(context, "Reel posted successfully on global feed! 🚀", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isPosting
                ) {
                    if (isPosting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Post Reel to Global Network 🌍", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
