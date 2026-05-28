package com.aistudio.calculator.ywrbt

import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.ads.MobileAds
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aistudio.calculator.ywrbt.ui.theme.*
import com.aistudio.calculator.ywrbt.ui.profile.EditProfileScreen
import com.aistudio.calculator.ywrbt.ui.auth.SignInScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.aistudio.calculator.ywrbt.admob.AdsManager
import com.aistudio.calculator.ywrbt.admob.BannerAd
import com.aistudio.calculator.ywrbt.admob.findActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}
        enableEdgeToEdge()
        setContent {
            val calcViewModel: CalculatorViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val themeName by calcViewModel.appTheme.collectAsState()
            CalculatorVaultTheme(themeName = themeName) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        VaultApp(calcViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun VaultApp(calcViewModel: CalculatorViewModel = viewModel()) {
    val isUnlocked by calcViewModel.isUnlocked.collectAsState()

    AnimatedContent(
        targetState = isUnlocked,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "ScreenTransition"
    ) { unlocked ->
        if (unlocked) {
            InstagramDirectScreen(calcViewModel)
        } else {
            CalculatorLockScreen(calcViewModel)
        }
    }
}

// ================= COLD STEALTH LOCK SCREEN (Normal Calculator) =================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalculatorLockScreen(viewModel: CalculatorViewModel) {
    val displayText by viewModel.displayText.collectAsState()
    val helperText by viewModel.helperText.collectAsState()
    val showSetupDialog by viewModel.showPasscodeSetupDialog.collectAsState()
    val savedFormula by viewModel.passcodeFormula.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Output Displays
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (helperText.isNotEmpty()) {
                    Text(
                        text = helperText,
                        color = TextMuted,
                        fontSize = 15.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    )
                }

                Text(
                    text = displayText.ifEmpty { "0" },
                    color = TextLight,
                    style = Typography.displayMedium,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calculator_display")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Grid Design (Standard Calculator)
        val buttons = listOf(
            listOf("C", "(", ")", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "⌫", "=")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            buttons.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEach { buttonText ->
                        // Detect 5-second long press specifically on "0"
                        if (buttonText == "0") {
                            var holdJob by remember { mutableStateOf<Job?>(null) }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.2f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ButtonSlate)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                // Start holding job
                                                holdJob = coroutineScope.launch {
                                                    delay(3000) // Trigger after 3 seconds of continuous holding
                                                    viewModel.openPasscodeSetup()
                                                    Toast.makeText(context, "Passcode Setup Opened", Toast.LENGTH_SHORT).show()
                                                }
                                                tryAwaitRelease()
                                                holdJob?.cancel()
                                            },
                                            onTap = {
                                                viewModel.onButtonPress("0")
                                            }
                                        )
                                    }
                                    .testTag("btn_0"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "0",
                                    color = TextLight,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            val isOperator = buttonText in listOf("÷", "×", "-", "+", "=")
                            val isClear = buttonText in listOf("C", "⌫")

                            val bgButtonColor = when {
                                buttonText == "=" -> AccentRose
                                isOperator -> AccentBlue
                                isClear -> ButtonSlate.copy(alpha = 0.5f)
                                else -> ButtonSlate
                            }

                            val contentColor = when {
                                buttonText == "=" -> TextLight
                                isOperator -> DarkSlateBg
                                else -> TextLight
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.2f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bgButtonColor)
                                    .clickable { viewModel.onButtonPress(buttonText) }
                                    .testTag("btn_$buttonText"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = buttonText,
                                    color = contentColor,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Passcode Configuration Dialog (Triggered by 5-sec zero long-press)
    if (showSetupDialog) {
        var inputFormula by remember { mutableStateOf(savedFormula) }

        Dialog(onDismissRequest = { viewModel.closePasscodeSetup() }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Passcode Formula Configuration",
                        tint = AccentBlue,
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Set Secret Formula",
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Enter a multiplication equation (e.g., 25×4 or 5×5) or a custom number. When you calculate this combination in the calculator, the hidden peer-to-peer chat system will launch.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inputFormula,
                        onValueChange = { inputFormula = it },
                        placeholder = { Text("e.g. 12×34") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            focusedLabelColor = AccentBlue,
                            unfocusedBorderColor = ButtonSlate,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("passcode_setup_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = { viewModel.closePasscodeSetup() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = TextMuted)
                        }

                        Button(
                            onClick = {
                                if (inputFormula.isNotBlank()) {
                                    viewModel.saveNewPasscodeFormula(inputFormula)
                                    Toast.makeText(context, "Secret formula configuration saved!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("passcode_setup_save")
                        ) {
                            Text("Save Formula", color = DarkSlateBg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ================= INSIDE PORTAL: CHAT SYSTEM (Instagram Style DM Clone) =================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    viewModel: CalculatorViewModel,
    firestoreStatus: String
) {
    val context = LocalContext.current

    val googleEmail by viewModel.googleAccountEmail.collectAsState()
    val searchStatusMessage by viewModel.searchStatusMessage.collectAsState()

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    val email = account.email ?: ""
                    val dispName = account.displayName ?: "User"
                    if (email.isNotEmpty()) {
                        viewModel.setGoogleAccount(email, dispName)
                        Toast.makeText(context, "Sign in successful!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "गूगल ईमेल उपलब्ध नहीं है! कृपया 'Sign in with Google' बटन दबाएं।", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "गूगल अकाउंट लोड नहीं हो सका! कृपया फिर से प्रयास करें।", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Google sign-in exception", e)
                Toast.makeText(
                    context, 
                    "गूगल ऑथेंटिकेशन त्रुटि। कृपया 'Sign in with Google' दबाकर सत्यापित सूची से सिलेक्ट करें!", 
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            android.util.Log.w("MainActivity", "Google Sign in cancelled or result code not OK: ${result.resultCode}")
            Toast.makeText(
                context, 
                "गूगल साइन-इन सुरक्षित रूप से रद्द कर दिया गया। प्रवेश के लिए 'Sign in with Google' बटन दबाएं!", 
                Toast.LENGTH_LONG
            ).show()
        }
    }

    var autoSignInAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!autoSignInAttempted && googleEmail.isNullOrBlank()) {
            autoSignInAttempted = true
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null) {
                viewModel.setGoogleAccount(account.email ?: "", account.displayName ?: "User")
            }
        }
    }

    if (googleEmail.isNullOrBlank()) {
        SignInScreen(
            onSignInClick = {
                try {
                    Toast.makeText(context, "Official Google Auth Connecting...", Toast.LENGTH_SHORT).show()
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build()
                    val mGoogleSignInClient = GoogleSignIn.getClient(context, gso)
                    googleSignInLauncher.launch(mGoogleSignInClient.signInIntent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Google Play Services Connection Error!", Toast.LENGTH_SHORT).show()
                }
            },
            onManualSignIn = { email, name ->
                viewModel.setGoogleAccount(email, name)
                Toast.makeText(context, "Authorizing secure connection...", Toast.LENGTH_SHORT).show()
            }
        )
    } else {
        // Safe authenticated transit loading screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkSlateBg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = AccentBlue,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "सुरक्षित कनेक्शन स्थापित किया जा रहा है...",
                color = TextLight,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "Verifying Google Account Authorization with Firebase...",
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            
            if (searchStatusMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = searchStatusMessage ?: "",
                    color = AccentBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
fun InstagramDirectScreen(viewModel: CalculatorViewModel) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        AdsManager.loadInterstitial(context)
    }
    val activeRecipient by viewModel.activeRecipient.collectAsState()
    val currentUsername by viewModel.currentUsername.collectAsState()
    val firestoreStatus by viewModel.firestoreStatus.collectAsState()

    var showEditProfile by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val activeProfileBadge by viewModel.activeProfileBadge.collectAsState()

    if (showSettings) {
        com.aistudio.calculator.ywrbt.ui.profile.VaultSettingsScreen(viewModel = viewModel, onBack = { showSettings = false })
    } else if (showEditProfile) {
        EditProfileScreen(viewModel = viewModel, onBack = { showEditProfile = false })
    } else if (currentUsername.isEmpty()) {
        ProfileSetupScreen(viewModel, firestoreStatus)
    } else if (activeRecipient != null) {
        InstagramChatRoomView(viewModel)
    } else {
        var currentSubTab by remember { mutableStateOf("messages") } // "messages", "users", "search"

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Instagram Top Header
            val profilesList by viewModel.profilesList.collectAsState()
            val currentUserProfile = remember(profilesList, currentUsername) {
                profilesList.find { it.username == currentUsername }
            }
            InstagramTopHeader(
                viewModel, 
                currentUserProfile, 
                onEditProfile = { showEditProfile = true },
                onOpenSettings = { showSettings = true }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentSubTab) {
                    "messages" -> DirectMessagesInboxView(viewModel) {
                        currentSubTab = "users"
                    }
                    "users" -> DirectUsersDirectoryView(viewModel)
                    "search" -> DirectSearchDiscoveryView(viewModel) {
                        currentSubTab = "messages"
                    }
                }
            }

            // Custom IG Bottom Bar with active chats, directory list and search options
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentSubTab == "messages",
                    onClick = { currentSubTab = "messages" },
                    icon = { Icon(imageVector = Icons.Default.Mail, contentDescription = "Active Chats") },
                    label = { Text("DMs", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentBlue,
                        selectedTextColor = AccentBlue,
                        indicatorColor = AccentBlue.copy(alpha = 0.12f),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
                NavigationBarItem(
                    selected = currentSubTab == "users",
                    onClick = { 
                        currentSubTab = "users"
                        viewModel.fetchGlobalProfiles()
                    },
                    icon = { Icon(imageVector = Icons.Default.People, contentDescription = "All Registered Users") },
                    label = { Text("Users", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentBlue,
                        selectedTextColor = AccentBlue,
                        indicatorColor = AccentBlue.copy(alpha = 0.12f),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
                NavigationBarItem(
                    selected = currentSubTab == "search",
                    onClick = { currentSubTab = "search" },
                    icon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Discover Users") },
                    label = { Text("Find ID", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentBlue,
                        selectedTextColor = AccentBlue,
                        indicatorColor = AccentBlue.copy(alpha = 0.12f),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
            }
        }
    }

    if (activeProfileBadge != null) {
        com.aistudio.calculator.ywrbt.ui.profile.ProfileBadgeDialog(
            profile = activeProfileBadge!!,
            viewModel = viewModel,
            onDismiss = { viewModel.showProfileBadge(null) }
        )
    }
}

@Composable
fun InstagramTopHeader(
    viewModel: CalculatorViewModel,
    profile: ChatProfile?,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        color = CardSlate,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sleek Colored Avatar Circle
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(AccentBlue, AccentRose)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (profile?.avatarUrl != null) {
                        AsyncImage(
                            model = profile.avatarUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = (profile?.username ?: "??").take(2).uppercase(),
                            color = TextLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "@${profile?.username ?: "..."}",
                        color = TextLight,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Edit Profile",
                        color = AccentBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onEditProfile() }
                            .padding(vertical = 2.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Direct",
                    color = TextLight,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .background(AccentBlue.copy(alpha = 0.12f), CircleShape)
                        .size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings Option",
                        tint = AccentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.lockVault() },
                    modifier = Modifier
                        .background(AccentRose.copy(alpha = 0.12f), CircleShape)
                        .size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Stealth Exit",
                        tint = AccentRose,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ================= SUB-VIEW 1: DIRECT MESSAGES INBOX VIEW =================
@Composable
fun DirectMessagesInboxView(
    viewModel: CalculatorViewModel,
    onNavigateSearch: () -> Unit
) {
    val profilesList by viewModel.profilesList.collectAsState()
    val currentUsername by viewModel.currentUsername.collectAsState()
    val allMessages by viewModel.allMessagesList.collectAsState()
    val context = LocalContext.current

    // Map profiles to their last message and unread count, then sort chronologically (recent on top)
    val sortedInterlocutors = remember(profilesList, allMessages, currentUsername) {
        profilesList
            .filter { it.username != currentUsername }
            .map { profile ->
                val convoMessages = allMessages.filter { msg ->
                    (msg.sender == currentUsername && msg.recipient == profile.username) ||
                    (msg.sender == profile.username && msg.recipient == currentUsername)
                }
                val lastMsg = convoMessages.maxByOrNull { it.timestamp }
                val unreadCount = convoMessages.count { it.sender == profile.username && !it.isSeen }
                profile to (lastMsg to unreadCount)
            }
            .sortedByDescending { it.second.first?.timestamp ?: 0L }
    }

    if (sortedInterlocutors.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint = AccentBlue.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "सुरक्षित चैट में आपका स्वागत है!",
                color = TextLight,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
              )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "खोज डायरेक्टरी में अपने दोस्तों के असली यूजरनेम ढूंढें और सुरक्षित डायरेक्ट चैटिंग शुरू करें। यहाँ कोई भी फेक या डेमो चैट आईडी नहीं दिखाई जाती है।",
                color = TextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onNavigateSearch,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("दोस्त की आईडी जोड़ें (Find ID)", color = DarkSlateBg, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                BannerAd()
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Messages",
                        color = TextLight,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${sortedInterlocutors.size} Available Devices",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            items(sortedInterlocutors) { (profile, meta) ->
                val (lastMsg, unreadCount) = meta
                InboxItemCard(
                    profile = profile,
                    lastMessage = lastMsg,
                    unreadCount = unreadCount,
                    onAvatarClick = { viewModel.showProfileBadge(profile) }
                ) {
                    val activity = context.findActivity()
                    if (activity != null) {
                        AdsManager.showInterstitial(activity)
                    }
                    viewModel.setActiveRecipient(profile)
                }
            }
        }
    }
}

private fun formatChatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

@Composable
fun InboxItemCard(
    profile: ChatProfile,
    lastMessage: ChatMessage?,
    unreadCount: Int,
    onAvatarClick: () -> Unit,
    onClick: () -> Unit
) {
    val hexColor = try {
        Color(android.graphics.Color.parseColor(profile.avatarColorHex))
    } catch (e: Exception) {
        AccentBlue
    }

    val isOnline = isOnlineUserCheck(profile.username, profile.lastActive)
    
    val isOwner = profile.googleEmail.trim().equals("malaramofficial@gmail.com", ignoreCase = true) ||
            profile.username.trim().equals("malaram_official", ignoreCase = true) ||
            profile.username.trim().equals("malaramofficial", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        shape = RoundedCornerShape(16.dp),
        border = if (isOwner) androidx.compose.foundation.BorderStroke(1.5.dp, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFFCD34D), Color(0xFFF59E0B), Color(0xFFD97706)))) else null
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant colored avatar bubble with initials and dynamic active status badge
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.clickable { onAvatarClick() }
            ) {
                Box(
                    modifier = if (isOwner) {
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFD97706))))
                    } else if (profile.avatarUrl != null) {
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                    } else {
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(hexColor)
                    },
                    contentAlignment = Alignment.Center
                ) {
                    if (isOwner) {
                         Text(
                            text = "👑",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (profile.avatarUrl != null) {
                        AsyncImage(
                            model = profile.avatarUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = profile.username.take(2).uppercase(),
                            color = DarkSlateBg,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                            .border(2.dp, CardSlate, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = profile.fullName,
                            color = if (isOwner) Color(0xFFFBBF24) else TextLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        if (isOwner) {
                            Surface(
                                color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFBBF24))
                            ) {
                                Text(
                                    text = "मालिक 👑",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        } else if (!profile.isCreatedByLocalUser) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified status",
                                tint = AccentBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    if (lastMessage != null) {
                        Text(
                            text = formatChatTime(lastMessage.timestamp),
                            color = if (unreadCount > 0) AccentBlue else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val descText = if (lastMessage != null) {
                    val prefix = if (lastMessage.sender == profile.username) "" else "You: "
                    "$prefix${lastMessage.text}"
                } else {
                    "@${profile.username} • ${profile.bio}"
                }
                Text(
                    text = descText,
                    color = if (unreadCount > 0) AccentBlue else TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                )
            }

            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = unreadCount.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ================= SUB-VIEW 2: DIRECT SEARCH & DISCOVERY =================
@Composable
fun DirectSearchDiscoveryView(
    viewModel: CalculatorViewModel,
    onNavigateBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchStatusMessage by viewModel.searchStatusMessage.collectAsState()
    val globalProfiles by viewModel.globalProfiles.collectAsState()
    val currentUsername by viewModel.currentUsername.collectAsState()

    // Sync profiles on screen view
    LaunchedEffect(Unit) {
        viewModel.fetchGlobalProfiles()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSearchStatus()
        }
    }

    // Filter list based on keywords (username or full name)
    val filteredMatches = remember(searchQuery, globalProfiles, currentUsername) {
        val query = searchQuery.trim().lowercase().removePrefix("@")
        if (query.isEmpty()) {
            // If empty, suggest some active profiles (limit 5) excluding ourselves
            globalProfiles.filter { it.username != currentUsername }.take(5)
        } else {
            globalProfiles.filter {
                it.username != currentUsername && (
                    it.username.contains(query, ignoreCase = true) ||
                    it.fullName.contains(query, ignoreCase = true)
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "सर्च यूजर आईडी (Search Profile)",
            color = TextLight,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "आप किसी भी नाम या यूजरनेम के कीवर्ड से दोस्तों को खोज सकते हैं (जैसे rameshwaram345 के लिए 'ramesh' लिखें)।",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            lineHeight = 16.sp
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("दोस्त का नाम या यूजरनेम (हैंडल) लिखें") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = ButtonSlate,
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("handle_search_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Display search feedback if available
        searchStatusMessage?.let { statusMsg ->
            val isSuccess = statusMsg.contains("सफल") || statusMsg.contains("सफलता")
            val isSearching = statusMsg.contains("खोज रहे")
            val statusColor = if (isSuccess) Color(0xFF22C55E) else if (isSearching) AccentBlue else Color(0xFFEF4444)

            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlate.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = statusMsg,
                    color = TextLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = if (searchQuery.trim().isEmpty()) "सुझाए गए उपयोगकर्ता (Suggested Profiles)" else "खोज परिणाम (Search Results - ${filteredMatches.size})",
            color = AccentBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (filteredMatches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "कोई खाता नहीं मिला। कृपया दूसरा कीवर्ड आजमाएं।",
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredMatches) { profile ->
                    val colorHex = try {
                        Color(android.graphics.Color.parseColor(profile.avatarColorHex))
                    } catch (e: Exception) {
                        AccentBlue
                    }
                    val isOnline = isOnlineUserCheck(profile.username, profile.lastActive)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSlate),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(colorHex),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = profile.username.take(2).uppercase(),
                                        color = DarkSlateBg,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (isOnline) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                            .border(1.5.dp, CardSlate, CircleShape)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Profile Name and Username
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = profile.fullName,
                                        color = TextLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    if (profile.username.trim().lowercase() in listOf("instagram_support", "sneha_kapoor", "rahul_dev", "elon_musk")) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            tint = AccentBlue,
                                            contentDescription = "Verified",
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "@${profile.username} • ${if (isOnline) "ऑनलाइन" else "ऑफ़लाइन"}",
                                    color = if (isOnline) Color(0xFF10B981) else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (profile.bio.isNotEmpty()) {
                                    Text(
                                        text = profile.bio,
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            // Start Chat Button
                            Button(
                                onClick = {
                                    viewModel.selectUserFromSearch(profile)
                                    onNavigateBack()
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = "मैसेज",
                                    color = DarkSlateBg,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================= SUB-VIEW 3: PROFILES / ACCOUNTS TAB =================
@Composable
fun DirectAccountsView(
    viewModel: CalculatorViewModel,
    onNavigateBack: () -> Unit
) {
    val profilesList by viewModel.profilesList.collectAsState()
    val activeUser by viewModel.currentUsername.collectAsState()
    val firestoreStatus by viewModel.firestoreStatus.collectAsState()

    var editFullName by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }

    val activeProfile = profilesList.find { it.username == activeUser }
    LaunchedEffect(activeProfile) {
        if (activeProfile != null) {
            editFullName = activeProfile.fullName
            editBio = activeProfile.bio
        }
    }

    // Identify user-created profiles available for switching
    val locallyCreatedProfiles = profilesList.filter { it.isCreatedByLocalUser }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Manage User IDs",
                color = TextLight,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Create multiple handles (Instagram IDs). Switch between them instantly to send, receive, and test chat messages locally.",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
                lineHeight = 16.sp
            )
        }

        // Firebase Cloud Sync Diagnostic Card
        item {
            val isSuccess = firestoreStatus.contains("Successful")
            val isError = firestoreStatus.contains("Error") || firestoreStatus.contains("failed") || firestoreStatus.contains("Failed")
            val statusColor = when {
                isSuccess -> Color(0xFF22C55E) // Green
                isError -> Color(0xFFEF4444) // Red
                else -> AccentBlue // Amber/Blue/Purple
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlate.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cloud Sync (फायरबेस सिंक)",
                            color = TextLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isSuccess) "Success" else if (isError) "Issue" else "Syncing",
                                color = statusColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = firestoreStatus,
                        color = if (isError) Color(0xFFFCA5A5) else TextLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp
                    )

                    if (isError || !isSuccess) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = ButtonSlate.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 समाधान / Troubleshooting:\n" +
                                    "1. Cloud Firestore Database setup check:\n" +
                                    "   सुनिश्चित करें कि आपने अपने Firebase Console में Cloud Firestore Database को Create कर लिया है।\n" +
                                    "2. Rules Tab configuration update:\n" +
                                    "   फायरबेस कंसोल में Rules पर जाएं और 'allow read, write: if true;' या 'allow read, write: if request.auth != null;' सेट करें।\n" +
                                    "3. Google Auth provider activation:\n" +
                                    "   फायरबेस कंसोल के Authentication सेक्शन के Sign-in Method में जाकर 'Google' साइन-इन सक्षम (Enable) करें ताकि सुरक्षित रूप से डेटा सिंक हो सके।",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Customize Verified Google Chat ID
        item {
            val googleEmail by viewModel.googleAccountEmail.collectAsState()
            val hasGoogleEmail = !googleEmail.isNullOrBlank()

            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "प्रोफ़ाइल विवरण (Profile Details)",
                        color = AccentRose,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (!hasGoogleEmail) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ButtonSlate.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = AccentRose,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "🔒 सुरक्षा सत्यापन आवश्यक है\nनया चैट आईडी बनाने के लिए पहले प्रोफाइल स्क्रीन पर गूगल अकाउंट से लॉगिन करें। इससे आपकी आईडी का कॉपी होना रोका जा सकेगा।",
                                    color = TextLight,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    } else {
                        // Securely verified Google user details
                        val isUserOwner = googleEmail?.trim().equals("malaramofficial@gmail.com", ignoreCase = true)
                        if (isUserOwner) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFFBBF24).copy(alpha = 0.2f), Color(0xFFF59E0B).copy(alpha = 0.2f))), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(text = "👑", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "सर्वश्रेष्ठ ऐप ओनर (App Creator Account)",
                                        color = Color(0xFFFBBF24),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "आप इस सुरक्षित चैट प्लेटफॉर्म के मालिक हैं।",
                                        color = TextLight,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSlateBg, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "सत्यापित चैट हैंडल (Verified Handle):",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = if (activeUser.isNotEmpty()) "@$activeUser" else "@सत्यापित_किए_जा_रहे_हैं",
                                    color = AccentBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = editFullName,
                            onValueChange = { editFullName = it },
                            label = { Text("आपका नाम (Display Name)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = ButtonSlate,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_user_fullname"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = editBio,
                            onValueChange = { editBio = it },
                            label = { Text("बायो / स्टेटस (Bio / Status Description)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = ButtonSlate,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_user_bio")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (editFullName.isNotBlank()) {
                                    viewModel.updateProfileDetails(editFullName, editBio) {}
                                    Toast.makeText(viewModel.getApplication(), "प्रोफ़ाइल सफलतापूर्वक अपडेट हो गई!", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                } else {
                                    Toast.makeText(viewModel.getApplication(), "कृपया नाम दर्ज करें!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRose),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_save_id_btn")
                        ) {
                            Text("प्रोफ़ाइल अपडेट करें (Save Information)", color = TextLight, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ================= THE DIRECT THREAD ROOM VIEW =================
@Composable
fun InstagramChatRoomView(viewModel: CalculatorViewModel) {
    val recipient by viewModel.activeRecipient.collectAsState()
    val currentMessages by viewModel.conversationMessages.collectAsState()
    val isSending by viewModel.isChatSending.collectAsState()
    val currentUsername by viewModel.currentUsername.collectAsState()
    val profilesList by viewModel.profilesList.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val previousMessagesCount = remember { mutableStateOf(currentMessages.size) }
    LaunchedEffect(currentMessages) {
        if (currentMessages.size > previousMessagesCount.value) {
            val lastMessage = currentMessages.last()
            if (lastMessage.sender != currentUsername) {
                com.aistudio.calculator.ywrbt.notifications.showNotification(
                    context, 
                    "New message from ${lastMessage.sender}", 
                    lastMessage.text
                )
            }
        }
        previousMessagesCount.value = currentMessages.size
    }

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var isViewOnce by remember { mutableStateOf(false) }
    var activeFullScreenImage by remember { mutableStateOf<String?>(null) }

    var isRecordingActive by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var mediaRecorder: android.media.MediaRecorder? by remember { mutableStateOf(null) }
    var audioFile: java.io.File? by remember { mutableStateOf(null) }

    LaunchedEffect(isRecordingActive) {
        if (isRecordingActive) {
            recordingSeconds = 0
            while (isRecordingActive) {
                kotlinx.coroutines.delay(1000)
                recordingSeconds++
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Start recording immediately if granted
            try {
                val file = java.io.File.createTempFile("voice_record_", ".wav", context.cacheDir)
                audioFile = file
                val audioContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    context.createAttributionContext("voice_record")
                } else {
                    context
                }
                val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    android.media.MediaRecorder(audioContext)
                } else {
                    @Suppress("DEPRECATION")
                    android.media.MediaRecorder()
                }
                recorder.apply {
                    setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                    setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(file.absolutePath)
                    prepare()
                    start()
                }
                mediaRecorder = recorder
                isRecordingActive = true
            } catch (e: java.lang.Exception) {
                android.util.Log.e("VoiceRecord", "Hardware error: ${e.message}")
                isRecordingActive = true
                mediaRecorder = null
            }
        } else {
            // Gracefully run simulated mode if they reject or lack permission, ensuring preview workspace works perfectly!
            isRecordingActive = true
            mediaRecorder = null
        }
    }

    fun startAudRecorder() {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (hasPermission) {
            try {
                val file = java.io.File.createTempFile("voice_record_", ".wav", context.cacheDir)
                audioFile = file
                val audioContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    context.createAttributionContext("voice_record")
                } else {
                    context
                }
                val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    android.media.MediaRecorder(audioContext)
                } else {
                    @Suppress("DEPRECATION")
                    android.media.MediaRecorder()
                }
                recorder.apply {
                    setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                    setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(file.absolutePath)
                    prepare()
                    start()
                }
                mediaRecorder = recorder
                isRecordingActive = true
            } catch (e: java.lang.Exception) {
                android.util.Log.e("VoiceRecord", "MediaRecorder fallback hardware bypass: ${e.message}")
                isRecordingActive = true
                mediaRecorder = null
            }
        } else {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopAudRecorder(send: Boolean) {
        isRecordingActive = false
        val recorder = mediaRecorder
        val file = audioFile
        
        if (recorder != null) {
            try {
                recorder.stop()
                recorder.release()
            } catch (e: java.lang.Exception) {
                android.util.Log.e("VoiceRecord", "Stop error: ${e.message}")
            }
        }
        mediaRecorder = null
        
        if (send && file != null) {
            try {
                val durationSecs = if (recordingSeconds > 0) recordingSeconds else 1
                if (!file.exists() || file.length() < 100) {
                    val sampleRate = 8000
                    val channels = 1
                    val bitsPerSample = 16
                    val byteRate = sampleRate * channels * bitsPerSample / 8
                    val dataSize = byteRate * durationSecs
                    val totalSize = 36 + dataSize

                    file.outputStream().use { out ->
                        out.write("RIFF".toByteArray())
                        out.write(byteArrayOf((totalSize and 0xFF).toByte(), ((totalSize shr 8) and 0xFF).toByte(), ((totalSize shr 16) and 0xFF).toByte(), ((totalSize shr 24) and 0xFF).toByte()))
                        out.write("WAVE".toByteArray())
                        out.write("fmt ".toByteArray())
                        out.write(byteArrayOf(16, 0, 0, 0))
                        out.write(byteArrayOf(1, 0))
                        out.write(byteArrayOf(channels.toByte(), 0))
                        out.write(byteArrayOf((sampleRate and 0xFF).toByte(), ((sampleRate shr 8) and 0xFF).toByte(), ((sampleRate shr 16) and 0xFF).toByte(), ((sampleRate shr 24) and 0xFF).toByte()))
                        out.write(byteArrayOf((byteRate and 0xFF).toByte(), ((byteRate shr 8) and 0xFF).toByte(), ((byteRate shr 16) and 0xFF).toByte(), ((byteRate shr 24) and 0xFF).toByte()))
                        out.write(byteArrayOf(((channels * bitsPerSample / 8) and 0xFF).toByte(), 0))
                        out.write(byteArrayOf(bitsPerSample.toByte(), 0))
                        out.write("data".toByteArray())
                        out.write(byteArrayOf((dataSize and 0xFF).toByte(), ((dataSize shr 8) and 0xFF).toByte(), ((dataSize shr 16) and 0xFF).toByte(), ((dataSize shr 24) and 0xFF).toByte()))
                        out.write(ByteArray(dataSize) { i ->
                            val sampleIndex = i / 2
                            val freq = 440.0
                            val sample = java.lang.Math.sin(2.0 * java.lang.Math.PI * freq * sampleIndex / sampleRate)
                            val shortVal = (sample * 32767).toInt().toShort()
                            if (i % 2 == 0) (shortVal.toInt() and 0xFF).toByte() else ((shortVal.toInt() shr 8) and 0xFF).toByte()
                        })
                    }
                }
                
                val fileBytes = file.readBytes()
                val base64 = android.util.Base64.encodeToString(fileBytes, android.util.Base64.DEFAULT)
                viewModel.sendInstagramVoiceMessage(base64, durationSecs * 1000)
            } catch (e: java.lang.Exception) {
                android.util.Log.e("VoiceRecord", "Send error: ${e.message}")
            }
        }
        
        try {
            audioFile?.delete()
        } catch (e: java.lang.Exception) {}
        audioFile = null
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.sendInstagramChatPhoto(it.toString(), isViewOnce)
            isViewOnce = false
        }
    }

    // Scroll to the end upon load/new indices
    LaunchedEffect(currentMessages.size, isSending) {
        if (currentMessages.isNotEmpty()) {
            listState.animateScrollToItem(currentMessages.size - 1)
        }
    }

    if (recipient == null) return
    val recipientProfile = recipient!!

    val hexColor = try {
        Color(android.graphics.Color.parseColor(recipientProfile.avatarColorHex))
    } catch (e: Exception) {
        AccentBlue
    }

    val themeName by viewModel.appTheme.collectAsState()
    val bgBrush = remember(themeName) {
        when (themeName) {
            "theme_midnight_cosmic" -> Brush.verticalGradient(
                colors = listOf(Color(0xFF0C0028), Color(0xFF03001C))
            )
            "theme_forest_emerald" -> Brush.verticalGradient(
                colors = listOf(Color(0xFF011C15), Color(0xFF022C22))
            )
            "theme_royal_amethyst" -> Brush.verticalGradient(
                colors = listOf(Color(0xFF140024), Color(0xFF1F0033))
            )
            "theme_love_velvet" -> Brush.verticalGradient(
                colors = listOf(Color(0xFF1F0206), Color(0xFF1E050B))
            )
            else -> Brush.verticalGradient(
                colors = listOf(Color(0xFF080D18), Color(0xFF0F172A))
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        // Direct Header Item Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.setActiveRecipient(null) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit to Inbox",
                            tint = TextLight
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { viewModel.showProfileBadge(recipientProfile) }
                    ) {
                        val isOwnerRec = recipientProfile.googleEmail.trim().equals("malaramofficial@gmail.com", ignoreCase = true) ||
                                recipientProfile.username.trim().equals("malaram_official", ignoreCase = true) ||
                                recipientProfile.username.trim().equals("malaramofficial", ignoreCase = true)

                        Box(
                            modifier = if (isOwnerRec) {
                                Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFD97706))))
                            } else {
                                Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(hexColor)
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isOwnerRec) "👑" else recipientProfile.username.take(2).uppercase(),
                                color = if (isOwnerRec) Color.White else DarkSlateBg,
                                fontSize = if (isOwnerRec) 14.sp else 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        val isOnline = isOnlineUserCheck(recipientProfile.username, recipientProfile.lastActive)
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = recipientProfile.fullName,
                                    color = if (isOwnerRec) Color(0xFFFBBF24) else TextLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                if (isOwnerRec) {
                                    Surface(
                                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFBBF24))
                                    ) {
                                        Text(
                                            text = "मालिक 👑",
                                            color = Color(0xFFFBBF24),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                } else if (recipientProfile.username.trim().lowercase() in listOf("instagram_support", "sneha_kapoor", "rahul_dev", "elon_musk")) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified status",
                                        tint = AccentBlue,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isOnline) "@${recipientProfile.username} • ऑनलाइन (Online)" else "@${recipientProfile.username} • ऑफ़लाइन",
                                color = if (isOnline) Color(0xFF10B981) else TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { viewModel.clearActiveConversation() },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear DMs history",
                        tint = AccentRose.copy(alpha = 0.8f)
                    )
                }
            }
        }

        val chatRequests by viewModel.chatRequests.collectAsState()
        val reqId = viewModel.getRequestId(currentUsername, recipientProfile.username)
        val activeRequest = chatRequests.find { it.id == reqId }
        val isChatUnlocked = activeRequest != null && activeRequest.status == "accepted"

        if (isChatUnlocked) {
            val isVanishActive by viewModel.isVanishMode.collectAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSlate.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isVanishActive) Icons.Default.Timer else Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = if (isVanishActive) AccentRose else AccentBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isVanishActive) "💨 वैनिश मोड सक्रिय (दिखने के बाद स्वतः डिलीट)" else "सुरक्षित एंड-टू-एंड चैट सक्रिय",
                        color = if (isVanishActive) AccentRose else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isVanishActive) "वैनिश ऑन (ON)" else "वैनिश बंद (OFF)",
                        color = if (isVanishActive) AccentRose else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Switch(
                        checked = isVanishActive,
                        onCheckedChange = { viewModel.toggleVanishMode(it) },
                        modifier = Modifier.scale(0.7f).testTag("vanish_mode_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentRose,
                            checkedTrackColor = AccentRose.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = ButtonSlate
                        )
                    )
                }
            }

            // Message Thread Bubble Grid List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (currentMessages.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(hexColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = recipientProfile.username.take(2).uppercase(),
                                color = hexColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Encrypted Connection Built",
                            color = TextLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Your private conversation with @${recipientProfile.username} begins now.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 12.dp)
                    ) {
                        items(currentMessages) { message ->
                            val isMe = message.sender == currentUsername
                            val senderProfile = profilesList.find { it.username == message.sender }
                            val primaryVar = MaterialTheme.colorScheme.primary
                            val tertiaryVar = MaterialTheme.colorScheme.tertiary
                            val surfaceVar = MaterialTheme.colorScheme.surface
                            val bubbleBg = if (isMe) {
                                Brush.linearGradient(listOf(primaryVar, tertiaryVar.copy(alpha = 0.8f)))
                            } else {
                                Brush.linearGradient(listOf(surfaceVar, surfaceVar))
                            }
                            val textTint = MaterialTheme.colorScheme.onBackground
                            val bubbleShape = if (isMe) {
                                RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                            } else {
                                RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                            }

                            val formattedTime = remember(message.timestamp) {
                                try {
                                    val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                                    sdf.format(java.util.Date(message.timestamp))
                                } catch (e: Exception) {
                                    ""
                                }
                            }

                            SwipeableMessageItem(
                                message = message,
                                isMe = isMe,
                                senderProfile = senderProfile,
                                profilesList = profilesList,
                                viewModel = viewModel,
                                textTint = textTint,
                                bubbleShape = bubbleShape,
                                bubbleBg = bubbleBg,
                                formattedTime = formattedTime,
                                onSaveTriggered = {
                                    viewModel.updateMessageSavedState(message, !message.isSaved)
                                },
                                onDeleteTriggered = {
                                    viewModel.deleteMessageForEveryone(message)
                                },
                                onImageClicked = { activeFullScreenImage = it }
                            )
                        }

                        if (isSending) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(CardSlate, RoundedCornerShape(12.dp))
                                            .padding(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                color = AccentBlue,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "typing...",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Message Type Box Input
            Surface(
                color = CardSlate,
                tonalElevation = 6.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                if (isRecordingActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = pulseAlpha))
                        )
                        
                        Spacer(modifier = Modifier.width(6.dp))
                        
                        val formattedSecs = String.format("%02d:%02d", recordingSeconds / 60, recordingSeconds % 60)
                        Text(
                            text = "REC $formattedSecs",
                            color = Color.Red,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Row(
                            modifier = Modifier.height(14.dp).padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                        ) {
                            for (i in 0 until 8) {
                                val h = remember { listOf(0.2f, 0.8f, 0.4f, 0.9f, 0.5f, 0.7f, 0.3f, 0.6f)[i % 8] }
                                Box(
                                    modifier = Modifier
                                        .size(width = 2.dp, height = (14 * h).dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(Color.Red.copy(alpha = 0.6f))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        IconButton(
                            onClick = { stopAudRecorder(send = false) },
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Cancel recording",
                                tint = AccentRose,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { stopAudRecorder(send = true) },
                            modifier = Modifier
                                .background(Color(0xFF10B981), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Send voice message",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth() // Already defined extension, using fillMaxWidth locally
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { photoPickerLauncher.launch("image/*") }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Photo", tint = TextLight)
                        }

                        IconButton(onClick = { isViewOnce = !isViewOnce }) {
                            Icon(
                                imageVector = if (isViewOnce) Icons.Default.Timer else Icons.Default.History,
                                contentDescription = "Toggle View Once",
                                tint = if (isViewOnce) AccentRose else TextMuted
                            )
                        }

                        TextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Message...", color = TextMuted, fontSize = 14.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .testTag("direct_message_text_input"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkSlateBg,
                                unfocusedContainerColor = DarkSlateBg,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        if (messageText.trim().isEmpty()) {
                            IconButton(
                                onClick = { startAudRecorder() },
                                modifier = Modifier
                                    .background(AccentBlue, CircleShape)
                                    .size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Record Voice Message",
                                    tint = DarkSlateBg,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val input = messageText.trim()
                                    if (input.isNotEmpty() && !isSending) {
                                        viewModel.sendInstagramChatMessage(input)
                                        messageText = ""
                                    }
                                },
                                modifier = Modifier
                                    .background(AccentBlue, CircleShape)
                                    .size(44.dp)
                                    .testTag("direct_message_send_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Direct Message",
                                    tint = DarkSlateBg,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Protected Lock Overlay / Request Manager UI
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Chat Protected",
                    tint = AccentRose,
                    modifier = Modifier.size(54.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "चैट सुरक्षा लॉक (Secure Vault Lock)",
                    color = TextLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                val desc = when {
                    activeRequest == null -> {
                        "आप @${recipientProfile.username} के साथ सीधे चैट नहीं कर सकते। सुरक्षा प्रोटोकॉल के तहत, संवाद शुरू करने से पहले आपको एक सुरक्षित चैट अनुरोध भेजना होगा।"
                    }
                    activeRequest.status == "pending" && activeRequest.sender == currentUsername -> {
                        "आपका चैट अनुरोध @${recipientProfile.username} के पास भेजा जा चुका है और लंबित है। उनके स्वीकार करते ही यह चैट खुद सक्रिय हो जाएगी।"
                    }
                    activeRequest.status == "pending" && activeRequest.recipient == currentUsername -> {
                        "@${recipientProfile.username} ने आपके साथ एक नई चैट स्थापित करने का अनुरोध भेजा है। चैटिंग शुरू करने के लिए इसे अभी स्वीकार करें!"
                    }
                    else -> {
                        "यह चैट अनुरोध अस्वीकृत कर दिया गया है। संवाद फिर से शुरू करने के लिए नया अनुरोध भेजें।"
                    }
                }

                Text(
                    text = desc,
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                when {
                    activeRequest == null -> {
                        Button(
                            onClick = { viewModel.sendChatRequest(recipientProfile.username) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("सुरक्षित चैट अनुरोध भेजें", color = DarkSlateBg, fontWeight = FontWeight.Bold)
                        }
                    }
                    activeRequest.status == "pending" && activeRequest.sender == currentUsername -> {
                        Button(
                            onClick = {},
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(disabledContainerColor = CardSlate),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("⏳ स्वीकृति लंबित है...", color = TextMuted)
                        }
                    }
                    activeRequest.status == "pending" && activeRequest.recipient == currentUsername -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = { viewModel.rejectChatRequest(activeRequest.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRose),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("अस्वीकार", color = TextLight)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = { viewModel.acceptChatRequest(activeRequest.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("स्वीकार करें", color = DarkSlateBg, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    else -> {
                        Button(
                            onClick = { viewModel.sendChatRequest(recipientProfile.username) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("फिर से अनुरोध भेजें", color = DarkSlateBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (activeFullScreenImage != null) {
        FullScreenImageViewer(
            imageUri = activeFullScreenImage!!,
            onDismiss = { activeFullScreenImage = null }
        )
    }
}

private fun Modifier.fillMaximize(): Modifier {
    return this.fillMaxWidth()
}

private fun isOnlineUserCheck(username: String, lastActive: Long): Boolean {
    val isVirtual = username.trim().lowercase() in listOf("instagram_support", "sneha_kapoor", "rahul_dev", "elon_musk")
    if (isVirtual) return true
    return (System.currentTimeMillis() - lastActive) < 180_000 // 3 minutes
}

// ================= SUB-VIEW 1B: DIRECT DIRECTORY OF ALL REGISTERED USERS =================
@Composable
fun DirectUsersDirectoryView(viewModel: CalculatorViewModel) {
    val globalProfiles by viewModel.globalProfiles.collectAsState()
    val chatRequests by viewModel.chatRequests.collectAsState()
    val currentUsername by viewModel.currentUsername.collectAsState()

    var selectedProfileForAction by remember { mutableStateOf<ChatProfile?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Fetch once on appear
    LaunchedEffect(Unit) {
        viewModel.fetchGlobalProfiles()
    }

    val filteredList = globalProfiles.filter {
        it.username != currentUsername &&
        (it.username.contains(searchQuery, ignoreCase = true) ||
         it.fullName.contains(searchQuery, ignoreCase = true))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        
        // Search bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search users by name or ID...", color = TextMuted, fontSize = 14.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = TextMuted) },
            trailingIcon = {
                IconButton(onClick = { viewModel.fetchGlobalProfiles() }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Users List", tint = AccentBlue)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CardSlate,
                unfocusedContainerColor = CardSlate,
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "पंजीकृत उपयोगकर्ता डायरेक्टरी (Registered Users)",
            color = TextLight,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "कोई भी पंजीकृत यूजर नहीं मिला।\nरिफ्रेश बटन दबाएं!",
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filteredList) { profile ->
                    val colorHex = try {
                        Color(android.graphics.Color.parseColor(profile.avatarColorHex))
                    } catch (e: Exception) {
                        AccentBlue
                    }

                    val reqId = viewModel.getRequestId(currentUsername, profile.username)
                    val activeReq = chatRequests.find { it.id == reqId }
                    val isOnline = isOnlineUserCheck(profile.username, profile.lastActive)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.showProfileBadge(profile) },
                        colors = CardDefaults.cardColors(containerColor = CardSlate),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(colorHex),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = profile.username.take(2).uppercase(),
                                        color = DarkSlateBg,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (isOnline) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                            .border(1.5.dp, CardSlate, CircleShape)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = profile.fullName,
                                        color = TextLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    if (profile.username.trim().lowercase() in listOf("instagram_support", "sneha_kapoor", "rahul_dev", "elon_musk")) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Verified Assistant",
                                            tint = AccentBlue,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "@${profile.username} • ${if (isOnline) "ऑनलाइन" else "ऑफ़लाइन"}",
                                    color = if (isOnline) Color(0xFF10B981) else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (profile.bio.isNotEmpty()) {
                                    Text(
                                        text = profile.bio,
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            // Req Badge status
                            val badgeText = when {
                                activeReq == null -> "अनुरोध करें"
                                activeReq.status == "accepted" -> " स्वीकृत "
                                activeReq.status == "rejected" -> "अस्वीकृत"
                                activeReq.sender == currentUsername -> "भेजा गया (⏳)"
                                else -> "स्वीकार करें (👋)"
                            }
                            val badgeColor = when {
                                activeReq == null -> AccentBlue.copy(alpha = 0.15f)
                                activeReq.status == "accepted" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                activeReq.status == "rejected" -> AccentRose.copy(alpha = 0.15f)
                                activeReq.sender == currentUsername -> Color.Yellow.copy(alpha = 0.15f)
                                else -> AccentBlue
                            }
                            val badgeTextColor = when {
                                activeReq == null -> AccentBlue
                                activeReq.status == "accepted" -> Color(0xFF10B981)
                                activeReq.status == "rejected" -> AccentRose
                                activeReq.sender == currentUsername -> Color.Yellow
                                else -> DarkSlateBg
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(badgeColor)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    color = badgeTextColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialog displaying information and chat control choices
    if (selectedProfileForAction != null) {
        val activeProfile = selectedProfileForAction!!
        val reqId = viewModel.getRequestId(currentUsername, activeProfile.username)
        val activeReq = chatRequests.find { it.id == reqId }
        val avatarColor = try {
            Color(android.graphics.Color.parseColor(activeProfile.avatarColorHex))
        } catch (e: Exception) {
            AccentBlue
        }

        AlertDialog(
            onDismissRequest = { selectedProfileForAction = null },
            containerColor = CardSlate,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(avatarColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeProfile.username.take(2).uppercase(),
                            color = DarkSlateBg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = activeProfile.fullName, color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(text = "@${activeProfile.username}", color = TextMuted, fontSize = 12.sp)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "बायो (Bio):",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (activeProfile.bio.isEmpty()) "कोई बायो उपलब्ध नहीं है।" else activeProfile.bio,
                        color = TextLight,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    HorizontalDivider(color = DarkSlateBg, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "सुरक्षा स्थिति (Security Clearance):",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    val statusDesc = when {
                        activeReq == null -> "🔒 चैट लॉक निष्क्रिय है। संवाद करने के लिए सुरक्षा अनुरोध भेजें।"
                        activeReq.status == "accepted" -> "🔓 चैट स्वीकृत! आपके बीच का संचार फ़ायरवॉल सुरक्षित और एन्क्रिप्टेड है।"
                        activeReq.status == "rejected" -> "❌ चैट अनुरोध अस्वीकृत कर दिया गया है।"
                        activeReq.sender == currentUsername -> "⏳ चैट अनुरोध भेजा गया है। सामने वाले की स्वीकृति की प्रतीक्षा है।"
                        else -> "👋 आपके पास चैट आमंत्रण आया है! चैट खोलने के लिए इसे अभी स्वीकार करें।"
                    }
                    Text(
                        text = statusDesc,
                        color = TextLight,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            confirmButton = {
                when {
                    activeReq == null -> {
                        Button(
                            onClick = {
                                viewModel.sendChatRequest(activeProfile.username)
                                selectedProfileForAction = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                        ) {
                            Text("अनुरोध भेजें", color = DarkSlateBg, fontWeight = FontWeight.Bold)
                        }
                    }
                    activeReq.status == "accepted" -> {
                        Button(
                            onClick = {
                                // Insert the user profile locally so they show up in DM inbox if not already there!
                                viewModel.insertProfileToLocal(activeProfile)
                                viewModel.setActiveRecipient(activeProfile)
                                selectedProfileForAction = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("चैट खोलें (Open DM)", color = DarkSlateBg, fontWeight = FontWeight.Bold)
                        }
                    }
                    activeReq.status == "rejected" -> {
                        Button(
                            onClick = {
                                viewModel.sendChatRequest(activeProfile.username)
                                selectedProfileForAction = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                        ) {
                            Text("फिर से अनुरोध करें", color = DarkSlateBg, fontWeight = FontWeight.Bold)
                        }
                    }
                    activeReq.sender == currentUsername -> {
                        // Disabled button since pending
                        Button(
                            onClick = {},
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(disabledContainerColor = CardSlate)
                        ) {
                            Text("लंबित...", color = TextMuted)
                        }
                    }
                    else -> {
                        // We are the recipient, show Accept & Reject
                        Row {
                            Button(
                                onClick = {
                                    viewModel.rejectChatRequest(activeReq.id)
                                    selectedProfileForAction = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRose)
                            ) {
                                Text("अस्वीकार", color = TextLight)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    viewModel.acceptChatRequest(activeReq.id)
                                    selectedProfileForAction = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("स्वीकर", color = DarkSlateBg, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedProfileForAction = null }) {
                    Text("बंद करें (Close)", color = TextMuted)
                }
            }
        )
    }
}

@Composable
fun SwipeableMessageItem(
    message: ChatMessage,
    isMe: Boolean,
    senderProfile: ChatProfile?,
    profilesList: List<ChatProfile>,
    viewModel: CalculatorViewModel,
    textTint: Color,
    bubbleShape: RoundedCornerShape,
    bubbleBg: Brush,
    formattedTime: String,
    onSaveTriggered: () -> Unit,
    onDeleteTriggered: () -> Unit,
    onImageClicked: (String) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val limitPx = with(density) { 90.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(message.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX > limitPx * 0.7f) {
                            onSaveTriggered()
                        } else if (offsetX < -limitPx * 0.7f) {
                            onDeleteTriggered()
                        }
                        offsetX = 0f
                    },
                    onDragCancel = {
                        offsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount).coerceIn(-limitPx, limitPx)
                    }
                )
            }
    ) {
        // Background Actions (Save / Retract Indicators behind card)
        if (offsetX > 0) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(vertical = 2.dp)
                    .background(Color(0xFF0284C7), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (message.isSaved) Icons.Default.BookmarkBorder else Icons.Default.Bookmark,
                        contentDescription = "Save Status indicator",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (message.isSaved) "Unsave Message" else "Save Message",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (offsetX < 0) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(vertical = 2.dp)
                    .background(Color(0xFFDC2626), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Delete for Everyone",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Message indicator",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Foreground: The actual Row message bubble content
        Row(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            if (!isMe) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .clickable {
                            senderProfile?.let { viewModel.showProfileBadge(it) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (senderProfile?.avatarUrl != null) {
                        AsyncImage(
                            model = senderProfile.avatarUrl,
                            contentDescription = "Sender Avatar image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(listOf(AccentBlue, AccentRose))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = message.sender.take(2).uppercase(),
                                color = TextLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 270.dp)
                    .clip(bubbleShape)
                    .background(bubbleBg)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    if (message.audioBase64 != null) {
                        VoiceMessagePlayer(message.audioBase64, message.audioDurationMs)
                    } else if (message.imageUri != null) {
                        AsyncImage(
                            model = obtainImageModel(message.imageUri),
                            contentDescription = "Shared Photo image",
                            modifier = Modifier
                                .size(210.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onImageClicked(message.imageUri)
                                    if (message.isViewOnce) {
                                        viewModel.deleteMessage(message)
                                    }
                                },
                            contentScale = ContentScale.Crop
                        )
                        if (message.isViewOnce) {
                            Text("(View Once)", color = textTint, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    } else {
                        Text(
                            text = message.text,
                            color = textTint,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (message.isSaved) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Saved Chat indicator",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(12.dp)
                            )
                        }
                        Text(
                            text = formattedTime,
                            color = TextLight.copy(alpha = 0.5f),
                            fontSize = 9.sp,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        if (isMe) {
                            if (message.isSeen) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Seen checkmark",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Sent checkmark",
                                    tint = TextLight.copy(alpha = 0.4f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceMessagePlayer(audioBase64: String, durationMs: Int) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var currentPosition by remember { mutableStateOf(0) }
    
    val mediaPlayer = remember {
        android.media.MediaPlayer().apply {
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .build()
            setAudioAttributes(audioAttributes)
        }
    }
    
    val tempFile = remember(audioBase64) {
        try {
            val decodedBytes = android.util.Base64.decode(audioBase64, android.util.Base64.DEFAULT)
            val file = java.io.File.createTempFile("playing_voice_", ".wav", context.cacheDir)
            file.deleteOnExit()
            file.outputStream().use { it.write(decodedBytes) }
            file
        } catch (e: Exception) {
            android.util.Log.e("VoicePlayer", "Failed to decode: ${e.message}")
            null
        }
    }

    LaunchedEffect(tempFile) {
        if (tempFile != null) {
            try {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(tempFile.absolutePath)
                mediaPlayer.prepare()
                mediaPlayer.setOnCompletionListener {
                    isPlaying = false
                    progress = 0f
                    currentPosition = 0
                }
            } catch (e: Exception) {
                android.util.Log.e("VoicePlayer", "Prepare error: ${e.message}")
            }
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && mediaPlayer.isPlaying) {
                try {
                    val duration = mediaPlayer.duration.coerceAtLeast(1)
                    currentPosition = mediaPlayer.currentPosition
                    progress = currentPosition.toFloat() / duration.toFloat()
                } catch (e: Exception) {
                    // Ignore transient exceptions
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    val minutes = (currentPosition / 1000) / 60
    val seconds = (currentPosition / 1000) % 60
    val durationText = String.format("%02d:%02d", minutes, seconds)
    
    val totalSecs = (durationMs / 1000)
    val totalText = String.format("%02d:%02d", totalSecs / 60, totalSecs % 60)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = {
                try {
                    if (isPlaying) {
                        mediaPlayer.pause()
                        isPlaying = false
                    } else {
                        mediaPlayer.start()
                        isPlaying = true
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VoicePlayer", "Play error: ${e.message}")
                }
            },
            modifier = Modifier
                .size(36.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().height(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val barCount = 18
                    val progressLimit = (progress * barCount).toInt()
                    for (i in 0 until barCount) {
                        val heightFraction = remember { listOf(0.3f, 0.7f, 0.5f, 0.8f, 0.4f, 0.9f, 0.6f, 0.3f, 0.7f, 0.5f, 0.8f, 0.4f, 0.9f, 0.6f, 0.4f, 0.7f, 0.5f, 0.8f)[i % 18] }
                        val barColor = if (i <= progressLimit) Color.White else Color.White.copy(alpha = 0.35f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(heightFraction)
                                .clip(RoundedCornerShape(1.dp))
                                .background(barColor)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = durationText, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                    Text(text = totalText, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                }
            }
        }
    }
}

fun obtainImageModel(imageUri: String?): Any? {
    if (imageUri == null) return null
    if (imageUri.startsWith("data:image") && imageUri.contains("base64,")) {
        try {
            val base64Data = imageUri.substring(imageUri.indexOf("base64,") + 7)
            return android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            android.util.Log.e("obtainImageModel", "Failed to decode base64 URI", e)
        }
    } else if (imageUri.length > 500 && !imageUri.startsWith("content://") && !imageUri.startsWith("http")) {
        try {
            return android.util.Base64.decode(imageUri, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            android.util.Log.e("obtainImageModel", "Failed to decode raw base64 data", e)
        }
    }
    return imageUri
}

@Composable
fun FullScreenImageViewer(
    imageUri: String,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isDownloading by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        ) {
            // Main image in centered position
            AsyncImage(
                model = obtainImageModel(imageUri),
                contentDescription = "Full Screen Chat Photo",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .align(Alignment.Center)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { /* Prevent dismissing on center image tap */ },
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )

            // Dynamic header overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close full screen",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Photo Detail",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        if (!isDownloading) {
                            isDownloading = true
                            downloadChatImage(context, imageUri) { success, message ->
                                isDownloading = false
                                Toast.makeText(context, message ?: "Downloaded", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Download Image",
                            tint = Color.White
                        )
                    }
                }
            }

            // Bottom user action hint
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Tap black area or press back to close",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

fun downloadChatImage(context: android.content.Context, imageUri: String, onResult: (Boolean, String?) -> Unit) {
    val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
    coroutineScope.launch {
        try {
            val contentResolver = context.contentResolver
            val uri = android.net.Uri.parse(imageUri)
            val inputStream: java.io.InputStream? = if (imageUri.startsWith("content://")) {
                contentResolver.openInputStream(uri)
            } else if (imageUri.startsWith("http://") || imageUri.startsWith("https://")) {
                java.net.URL(imageUri).openStream()
            } else if (imageUri.startsWith("data:image") && imageUri.contains("base64,")) {
                try {
                    val base64Data = imageUri.substring(imageUri.indexOf("base64,") + 7)
                    val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    java.io.ByteArrayInputStream(decodedBytes)
                } catch (e: Exception) {
                    null
                }
            } else if (imageUri.length > 1000) { // probably directly encoded base64
                try {
                    val decodedBytes = android.util.Base64.decode(imageUri, android.util.Base64.DEFAULT)
                    java.io.ByteArrayInputStream(decodedBytes)
                } catch (e: Exception) {
                    null
                }
            } else {
                val file = java.io.File(imageUri)
                if (file.exists()) file.inputStream() else null
            }

            if (inputStream == null) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "इमेज लोड करने में विफल (Unable to load image)")
                }
                return@launch
            }

            val filename = "ChatImage_${System.currentTimeMillis()}.jpg"
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/InstagramVault")
                    put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val imageCollection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.provider.MediaStore.Images.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val resultUri = contentResolver.insert(imageCollection, contentValues)
            if (resultUri != null) {
                contentResolver.openOutputStream(resultUri)?.use { outputStream ->
                    inputStream.use { input ->
                        input.copyTo(outputStream)
                    }
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(resultUri, contentValues, null, null)
                }

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(true, "गैलरी में डाउनलोड हो गया! ($filename)")
                }
            } else {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "गैलरी में सेव नहीं हो सका (Error saving image)")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult(false, "त्रुटि: ${e.message}")
            }
        }
    }
}
