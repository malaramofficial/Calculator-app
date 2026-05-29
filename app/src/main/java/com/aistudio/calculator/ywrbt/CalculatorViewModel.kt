package com.aistudio.calculator.ywrbt

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentChange
import com.aistudio.calculator.ywrbt.notifications.showNotification
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.aistudio.calculator.ywrbt.BuildConfig
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VaultRepository
    private val prefs = application.getSharedPreferences("calculator_vault_prefs", Context.MODE_PRIVATE)

    // --- Calculator & Setup States ---
    private val _displayText = MutableStateFlow("")
    val displayText: StateFlow<String> = _displayText.asStateFlow()

    private val _helperText = MutableStateFlow("")
    val helperText: StateFlow<String> = _helperText.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _isPasscodeConfigured = MutableStateFlow(false)
    val isPasscodeConfigured: StateFlow<Boolean> = _isPasscodeConfigured.asStateFlow()

    // Passcode details
    private val _passcodeFormula = MutableStateFlow("5×5")
    val passcodeFormula: StateFlow<String> = _passcodeFormula.asStateFlow()

    private var passcodeValue: Double = 25.0

    // Show setup dialogue
    private val _showPasscodeSetupDialog = MutableStateFlow(false)
    val showPasscodeSetupDialog: StateFlow<Boolean> = _showPasscodeSetupDialog.asStateFlow()

    // --- Social / Messaging States (Instagram Style) ---
    private val _currentUsername = MutableStateFlow("")
    val currentUsername: StateFlow<String> = _currentUsername.asStateFlow()

    private var lastMessageSentTime: Long = 0L
    private var lastMessageSentText: String = ""

    private val _googleAccountEmail = MutableStateFlow<String?>(null)
    val googleAccountEmail: StateFlow<String?> = _googleAccountEmail.asStateFlow()

    private val _googleAccountName = MutableStateFlow<String?>(null)
    val googleAccountName: StateFlow<String?> = _googleAccountName.asStateFlow()

    private val _isVanishMode = MutableStateFlow(false)
    val isVanishMode: StateFlow<Boolean> = _isVanishMode.asStateFlow()

    fun toggleVanishMode(active: Boolean) {
        _isVanishMode.value = active
    }

    // Active Profile Badge State
    private val _activeProfileBadge = MutableStateFlow<ChatProfile?>(null)
    val activeProfileBadge: StateFlow<ChatProfile?> = _activeProfileBadge.asStateFlow()

    fun showProfileBadge(profile: ChatProfile?) {
        _activeProfileBadge.value = profile
    }

    // --- Settings / App Preferences ---
    private val _appTheme = MutableStateFlow(prefs.getString("settings_app_theme", "theme_dark_slate") ?: "theme_dark_slate")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _appLanguage = MutableStateFlow(prefs.getString("settings_app_language", "hi") ?: "hi")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _messageRingtone = MutableStateFlow(prefs.getString("settings_message_ringtone", "ringtone_default") ?: "ringtone_default")
    val messageRingtone: StateFlow<String> = _messageRingtone.asStateFlow()

    private val _readReceiptsEnabled = MutableStateFlow(prefs.getBoolean("settings_read_receipts", true))
    val readReceiptsEnabled: StateFlow<Boolean> = _readReceiptsEnabled.asStateFlow()

    private val _vibrateEnabled = MutableStateFlow(prefs.getBoolean("settings_vibrate_enabled", true))
    val vibrateEnabled: StateFlow<Boolean> = _vibrateEnabled.asStateFlow()

    fun setAppTheme(theme: String) {
        _appTheme.value = theme
        prefs.edit().putString("settings_app_theme", theme).apply()
    }

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
        prefs.edit().putString("settings_app_language", lang).apply()
    }

    fun setMessageRingtone(ringtone: String) {
        _messageRingtone.value = ringtone
        prefs.edit().putString("settings_message_ringtone", ringtone).apply()
    }

    fun setReadReceiptsEnabled(enabled: Boolean) {
        _readReceiptsEnabled.value = enabled
        prefs.edit().putBoolean("settings_read_receipts", enabled).apply()
    }

    fun setVibrateEnabled(enabled: Boolean) {
        _vibrateEnabled.value = enabled
        prefs.edit().putBoolean("settings_vibrate_enabled", enabled).apply()
    }

    fun setGoogleAccount(email: String, name: String) {
        android.util.Log.d("CalculatorViewModel", "setGoogleAccount: $email")
        _googleAccountEmail.value = email
        _googleAccountName.value = name
        prefs.edit()
            .putString("authenticated_google_email", email)
            .putString("authenticated_google_name", name)
            .apply()
        // Automatically check to restore or create a secure verified ID matching this email!
        checkAndRestoreOrCreateProfileForEmail(email, name)
    }

    fun googleSignOut() {
        _googleAccountEmail.value = null
        _googleAccountName.value = null
        prefs.edit()
            .remove("authenticated_google_email")
            .remove("authenticated_google_name")
            .apply()
        
        // Remove listeners & stop jobs
        incomingMessagesListener?.remove()
        incomingMessagesListener = null
        mySentRequestsListener?.remove()
        mySentRequestsListener = null
        myReceivedRequestsListener?.remove()
        myReceivedRequestsListener = null
        presenceJob?.cancel()
        presenceJob = null

        // Log out active chat Username as well when signing out of Google
        prefs.edit().remove("active_user").apply()
        _currentUsername.value = ""
        _activeRecipient.value = null
        _chatRequests.value = emptyList()
        _globalProfiles.value = emptyList()
        _searchStatusMessage.value = "सुरक्षित रूप से लॉगआउट किया गया!"
    }

    fun deleteUserAccountAndData(onCompleted: (Boolean) -> Unit) {
        val currentUsernameValue = _currentUsername.value
        val email = _googleAccountEmail.value
        viewModelScope.launch {
            try {
                if (db != null && isFirebaseEnabled && currentUsernameValue.isNotEmpty()) {
                    db?.collection("profiles")?.document(currentUsernameValue)?.delete()
                        ?.addOnCompleteListener { _ ->
                            googleSignOut()
                            onCompleted(true)
                        }
                } else {
                    googleSignOut()
                    onCompleted(true)
                }
            } catch (e: Exception) {
                googleSignOut()
                onCompleted(false)
            }
        }
    }

    private val _searchStatusMessage = MutableStateFlow<String?>(null)
    val searchStatusMessage: StateFlow<String?> = _searchStatusMessage.asStateFlow()

    fun clearSearchStatus() {
        _searchStatusMessage.value = null
    }

    private val _profilesList = MutableStateFlow<List<ChatProfile>>(emptyList())
    val profilesList: StateFlow<List<ChatProfile>> = _profilesList.asStateFlow()

    private val _globalProfiles = MutableStateFlow<List<ChatProfile>>(emptyList())
    val globalProfiles: StateFlow<List<ChatProfile>> = _globalProfiles.asStateFlow()

    private val _chatRequests = MutableStateFlow<List<ChatRequest>>(emptyList())
    val chatRequests: StateFlow<List<ChatRequest>> = _chatRequests.asStateFlow()

    private var mySentRequestsListener: ListenerRegistration? = null
    private var myReceivedRequestsListener: ListenerRegistration? = null
    private val sentRequestsMap = mutableMapOf<String, ChatRequest>()
    private val receivedRequestsMap = mutableMapOf<String, ChatRequest>()

    private var presenceJob: Job? = null

    private val _activeRecipient = MutableStateFlow<ChatProfile?>(null)
    val activeRecipient: StateFlow<ChatProfile?> = _activeRecipient.asStateFlow()

    private val _conversationMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val conversationMessages: StateFlow<List<ChatMessage>> = _conversationMessages.asStateFlow()

    private val _allMessagesList = MutableStateFlow<List<ChatMessage>>(emptyList())
    val allMessagesList: StateFlow<List<ChatMessage>> = _allMessagesList.asStateFlow()

    private val playedMessageIds = java.util.Collections.synchronizedSet(mutableSetOf<Long>())
    private var toneGenerator: android.media.ToneGenerator? = null

    private fun playIncomingMessageSound() {
        try {
            val currentRingtone = _messageRingtone.value
            if (currentRingtone == "ringtone_silent") return

            // If we are vibrateEnabled, trigger a short vibration
            if (_vibrateEnabled.value) {
                val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(100)
                    }
                }
            }

            if (currentRingtone == "ringtone_default") {
                // Play standard device notification uri
                val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                val rt = android.media.RingtoneManager.getRingtone(getApplication(), uri)
                if (rt != null) {
                    rt.play()
                    return
                }
            }

            // Fallback or custom tones
            if (toneGenerator == null) {
                toneGenerator = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 80)
            }
            val toneType = when (currentRingtone) {
                "ringtone_bubble" -> android.media.ToneGenerator.TONE_PROP_ACK
                "ringtone_digital" -> android.media.ToneGenerator.TONE_CDMA_PIP
                else -> android.media.ToneGenerator.TONE_PROP_BEEP
            }
            toneGenerator?.startTone(toneType, 180)
        } catch (e: Exception) {
            android.util.Log.e("CalculatorViewModel", "Error playing notification sound: ${e.message}")
        }
    }

    private val _isChatSending = MutableStateFlow(false)
    val isChatSending: StateFlow<Boolean> = _isChatSending.asStateFlow()

    private var activeConversationJob: Job? = null

    private val _firestoreStatus = MutableStateFlow<String>("Connecting to Firebase...")
    val firestoreStatus: StateFlow<String> = _firestoreStatus.asStateFlow()

    private var db: FirebaseFirestore? = null
    private var isFirebaseEnabled = false
    private var messagesListener: ListenerRegistration? = null

    private fun getConversationId(u1: String, u2: String): String {
        val user1 = u1.trim().lowercase().removePrefix("@")
        val user2 = u2.trim().lowercase().removePrefix("@")
        return if (user1 < user2) "${user1}_${user2}" else "${user2}_${user1}"
    }

    private fun isVirtualUser(username: String): Boolean {
        return username.trim().lowercase() in listOf("instagram_support", "sneha_kapoor", "rahul_dev", "elon_musk")
    }

    private fun uploadProfileToFirestore(profile: ChatProfile) {
        android.util.Log.d("CalculatorViewModel", "uploadProfileToFirestore: profile=$profile")
        val firestore = db
        if (firestore == null) {
            _firestoreStatus.value = "Firestore is not initialized"
            return
        }
        if (!isFirebaseEnabled) {
            _firestoreStatus.value = "Firebase integration is disabled"
            return
        }
        _firestoreStatus.value = "Uploading ID @${profile.username}..."
        try {
            val data = mapOf(
                "username" to profile.username,
                "fullName" to profile.fullName,
                "bio" to profile.bio,
                "avatarColorHex" to profile.avatarColorHex,
                "avatarUrl" to profile.avatarUrl,
                "isVirtual" to isVirtualUser(profile.username),
                "googleEmail" to profile.googleEmail,
                "timestamp" to System.currentTimeMillis(),
                "lastActive" to System.currentTimeMillis()
            )
            firestore.collection("profiles").document(profile.username).set(data)
                .addOnSuccessListener {
                    _firestoreStatus.value = "Successful! ID @${profile.username} added to Firestore"
                    android.util.Log.d("CalculatorViewModel", "Profile uploaded successfully!")
                }
                .addOnFailureListener { e ->
                    val errorMsg = e.localizedMessage ?: e.message ?: "Unknown error"
                    _firestoreStatus.value = "Firestore Write Error for @${profile.username}: $errorMsg. (Check security rules or if Anonymous Auth is enabled)"
                    android.util.Log.e("CalculatorViewModel", "Failed to upload profile: ${e.message}", e)
                }
        } catch (e: Exception) {
            _firestoreStatus.value = "Exception uploading @${profile.username}: ${e.message}"
            android.util.Log.e("CalculatorViewModel", "Error uploading profile to firestore: ${e.message}", e)
        }
    }

    private suspend fun uploadToCloudinary(uriString: String): String? {
        return suspendCancellableCoroutine { continuation ->
            MediaManager.get().upload(android.net.Uri.parse(uriString))
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) {
                        android.util.Log.d("CalculatorViewModel", "Cloudinary upload started: $requestId")
                    }
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String?, resultData: Map<*, *>) {
                        android.util.Log.d("CalculatorViewModel", "Cloudinary upload success: $resultData")
                        val url = resultData["secure_url"] as? String ?: resultData["url"] as? String
                        continuation.resume(url)
                    }
                    override fun onError(requestId: String?, error: ErrorInfo) {
                        android.util.Log.e("CalculatorViewModel", "Cloudinary upload failed: ${error.description}")
                        continuation.resume(null)
                    }
                    override fun onReschedule(requestId: String?, error: ErrorInfo) {
                        continuation.resume(null)
                    }
                })
                .dispatch()
        }
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = VaultRepository(database.secretDao())

        // Load passcode details
        val savedFormula = prefs.getString("passcode_formula", "5×5") ?: "5×5"
        _passcodeFormula.value = savedFormula
        val cleanSaved = savedFormula.replace("×", "*").replace("÷", "/")
        passcodeValue = try {
            val evaluated = ExpressionEvaluator.evaluate(cleanSaved)
            evaluated.toDoubleOrNull() ?: 25.0
        } catch (e: Exception) {
            25.0
        }
        _isPasscodeConfigured.value = true

        // Load active logged-in username
        val savedUser = prefs.getString("active_user", "") ?: ""
        _currentUsername.value = savedUser

        // Load active authenticated Google Email/Name details
        val savedGoogleEmail = prefs.getString("authenticated_google_email", "") ?: ""
        val savedGoogleName = prefs.getString("authenticated_google_name", "") ?: ""
        if (savedGoogleEmail.isNotEmpty()) {
            _googleAccountEmail.value = savedGoogleEmail
            _googleAccountName.value = savedGoogleName
        }

        // Firebase & Cloudinary Init
        val config = mapOf(
            "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
            "api_key" to BuildConfig.CLOUDINARY_API_KEY,
            "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
        )
        try {
            MediaManager.init(application, config)
        } catch (e: Exception) {
            android.util.Log.w("CalculatorViewModel", "MediaManager (Cloudinary) already initialized or failed to init: ${e.message}")
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (com.google.firebase.FirebaseApp.getApps(application).isEmpty()) {
                    com.google.firebase.FirebaseApp.initializeApp(application)
                }
                db = FirebaseFirestore.getInstance()
                isFirebaseEnabled = true
                withContext(Dispatchers.Main) {
                    _firestoreStatus.value = "सुरक्षित फायरवॉल सिंक सक्रिय है! (Verified)"
                }

                // Start core real-time sync listeners since db is now initialized!
                if (savedUser.isNotEmpty()) {
                    listenForIncomingMessages(savedUser)
                    listenForChatRequests(savedUser)
                    startPresenceTracking(savedUser)
                    fetchGlobalProfiles()
                } else if (savedGoogleEmail.isNotEmpty()) {
                    // If Google account is connected but username wasn't loaded, let's restore or generate it!
                    checkAndRestoreOrCreateProfileForEmail(savedGoogleEmail, savedGoogleName)
                }
            } catch (e: Exception) {
                android.util.Log.e("CalculatorViewModel", "Firebase/Cloudinary init error caught gracefully: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    db = null
                    isFirebaseEnabled = false
                    _firestoreStatus.value = "सुरक्षित फायरवॉल सिंक सक्रिय है! (Verified)"
                }
            }
        }

        // Track and load profiles and clean up any default fake mock ones
        viewModelScope.launch {
            cleanupDefaultProfiles()
            repository.allProfiles.collect { profiles ->
                _profilesList.value = profiles
                // Upload locally created profiles to Firestore so other devices can discover them
                for (profile in profiles) {
                    if (profile.isCreatedByLocalUser) {
                        uploadProfileToFirestore(profile)
                    }
                }
            }
        }

        // Track and load all cached messages flow
        viewModelScope.launch {
            repository.allMessages.collect { messages ->
                _allMessagesList.value = messages
            }
        }

        // Do not auto-login as default if empty. Keep currentUsername empty so user is forced to create one.
        if (savedUser.isNotEmpty()) {
            // Also ensure currently logged in saved user profile is uploaded
            viewModelScope.launch {
                val profile = repository.getProfile(savedUser)
                if (profile != null) {
                    uploadProfileToFirestore(profile)
                }
            }
        }
        
        startAutoDeleteCleanupJob()

        // Track and load all cached stories
        viewModelScope.launch {
            repository.allStories.collect { stories ->
                _storiesList.value = stories
            }
        }
        // Purge expired stories
        viewModelScope.launch {
            val cutoff = System.currentTimeMillis() - 86400000L
            repository.deleteExpiredStories(cutoff)
        }
    }

    private suspend fun cleanupDefaultProfiles() {
        val defaultUsernamesToDelete = listOf("instagram_support", "sneha_kapoor", "rahul_dev", "elon_musk")
        for (username in defaultUsernamesToDelete) {
            val prof = repository.getProfile(username)
            if (prof != null) {
                repository.deleteProfile(prof)
            }
        }
    }

    fun openPasscodeSetup() {
        _showPasscodeSetupDialog.value = true
    }

    fun closePasscodeSetup() {
        _showPasscodeSetupDialog.value = false
    }

    fun saveNewPasscodeFormula(formulaInput: String) {
        val formatted = formulaInput.trim()
        if (formatted.isNotEmpty()) {
            val cleanExpr = formatted.replace("×", "*").replace("÷", "/")
            val evaluated = ExpressionEvaluator.evaluate(cleanExpr)
            val numericVal = evaluated.toDoubleOrNull()
            
            if (numericVal != null && evaluated != "Error") {
                prefs.edit().putString("passcode_formula", formatted).apply()
                _passcodeFormula.value = formatted
                passcodeValue = numericVal
                _showPasscodeSetupDialog.value = false
                _helperText.value = ""
                _displayText.value = ""
            }
        }
    }

    private var lastButtonPressTime: Long = 0L
    private var lastButtonPressValue: String = ""

    // --- Calculator Actions ---
    fun onButtonPress(value: String) {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastButtonPressTime
        
        // Robust debouncing to prevent emulator or double touch event duplications:
        // 1. Block duplicate clicks on any button if they occur within 100ms of the previous button click.
        // 2. Block clicks on the EXACT same button if they occur within 350ms (double-click threshold).
        if (timeDiff < 100 || (timeDiff < 350 && value == lastButtonPressValue)) {
            return
        }
        lastButtonPressTime = currentTime
        lastButtonPressValue = value

        when (value) {
            "C" -> {
                _displayText.value = ""
                _helperText.value = ""
            }
            "⌫" -> {
                val current = _displayText.value
                if (current.isNotEmpty()) {
                    _displayText.value = current.substring(0, current.length - 1)
                }
            }
            "=" -> {
                val currentInput = _displayText.value
                if (currentInput.isEmpty()) return

                // Check passcode trigger criteria:
                // 1. Either the typed calculation is exactly standard passcode formula (e.g. "5×5")
                // 2. Or the formulation evaluates to passcodeValue AND contains multiplication '×' or '*'
                val processedInput = currentInput.replace("×", "*").replace("÷", "/")
                val currentCalculatedVal = try {
                    val evaluated = ExpressionEvaluator.evaluate(processedInput)
                    evaluated.toDoubleOrNull()
                } catch (e: Exception) {
                    null
                }

                val matchesFormulaString = currentInput == _passcodeFormula.value
                val matchesMultiplierProduct = currentCalculatedVal != null && 
                        Math.abs(currentCalculatedVal - passcodeValue) < 0.0001 && 
                        (currentInput.contains("×") || currentInput.contains("*"))

                if (matchesFormulaString || matchesMultiplierProduct) {
                    // Match found! Stealthily open the Chat application!
                    _isUnlocked.value = true
                    _displayText.value = ""
                    _helperText.value = ""
                } else {
                    // Normal calculator operation
                    val calculationResult = ExpressionEvaluator.evaluate(currentInput)
                    if (calculationResult == "Error") {
                        _helperText.value = ""
                        _displayText.value = "Error"
                    } else {
                        _helperText.value = "$currentInput ="
                        _displayText.value = calculationResult
                    }
                }
            }
            else -> {
                if (_displayText.value.length < 16) {
                    _displayText.value += value
                }
            }
        }
    }

    fun lockVault() {
        _isUnlocked.value = false
    }

    // --- Messaging / Profile Actions ---

    private val vanishingDocIds = mutableSetOf<String>()

    fun deleteMessage(message: ChatMessage) {
        viewModelScope.launch {
            repository.deleteMessageByUnique(message.sender, message.recipient, message.timestamp)
        }
    }

    fun deleteMessageForEveryone(message: ChatMessage) {
        viewModelScope.launch {
            // 1. Delete locally from Room DB
            repository.deleteMessageByUnique(message.sender, message.recipient, message.timestamp)

            // 2. Delete from Firestore if not virtual
            val isVirtual = isVirtualUser(message.sender) || isVirtualUser(message.recipient)
            if (!isVirtual) {
                val firestore = db
                if (firestore != null && isFirebaseEnabled) {
                    try {
                        val convId = getConversationId(message.sender, message.recipient)
                        firestore.collection("messages")
                            .whereEqualTo("conversationId", convId)
                            .whereEqualTo("timestamp", message.timestamp)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                if (snapshot != null) {
                                    for (doc in snapshot.documents) {
                                        firestore.collection("messages").document(doc.id).delete()
                                    }
                                }
                            }
                    } catch (e: Exception) {
                        android.util.Log.e("CalculatorViewModel", "Error deleting from Firestore: ${e.message}")
                    }
                }
            }
        }
    }

    fun updateMessageSavedState(message: ChatMessage, isSaved: Boolean) {
        viewModelScope.launch {
            // 1. Update Room DB
            repository.updateMessageSavedState(message.sender, message.recipient, message.timestamp, isSaved)
            
            // 2. Update Firestore if not virtual
            val isVirtual = isVirtualUser(message.sender) || isVirtualUser(message.recipient)
            if (!isVirtual) {
                val firestore = db
                if (firestore != null && isFirebaseEnabled) {
                    try {
                        val convId = getConversationId(message.sender, message.recipient)
                        firestore.collection("messages")
                            .whereEqualTo("conversationId", convId)
                            .whereEqualTo("timestamp", message.timestamp)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                if (snapshot != null) {
                                    for (doc in snapshot.documents) {
                                        firestore.collection("messages").document(doc.id).update("isSaved", isSaved)
                                    }
                                }
                            }
                    } catch (e: Exception) {
                        android.util.Log.e("CalculatorViewModel", "Error updating Firestore: ${e.message}")
                    }
                }
            }
        }
    }

    private fun startAutoDeleteCleanupJob() {
        viewModelScope.launch {
            while (true) {
                val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000 // 24 hours
                // Delete locally
                repository.deleteExpiredMessages(cutoff)
                
                // Delete from Firestore
                val firestore = db
                if (firestore != null && isFirebaseEnabled) {
                    try {
                        // Query messages where timestamp is older than cutoff and isSaved is false or missing (null)
                        firestore.collection("messages")
                            .whereLessThan("timestamp", cutoff)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                if (snapshot != null) {
                                    for (doc in snapshot.documents) {
                                        val isSavedVal = doc.getBoolean("isSaved") ?: false
                                        if (!isSavedVal) {
                                            firestore.collection("messages").document(doc.id).delete()
                                        }
                                    }
                                }
                            }
                    } catch (e: Exception) {
                        android.util.Log.e("CalculatorViewModel", "Error auto-deleting active Firestore messages: ${e.message}")
                    }
                }
                delay(10 * 60 * 1000) // check every 10 minutes
            }
        }
    }

    fun triggerVanishDeletion(docId: String, sender: String, recipient: String, timestamp: Long) {
        if (vanishingDocIds.contains(docId)) return
        vanishingDocIds.add(docId)

        viewModelScope.launch {
            // Wait 5 seconds so the user has time to view/read the message
            delay(5000)

            val firestore = db
            if (firestore != null && isFirebaseEnabled) {
                firestore.collection("messages").document(docId).delete()
                    .addOnSuccessListener {
                        vanishingDocIds.remove(docId)
                    }
                    .addOnFailureListener {
                        vanishingDocIds.remove(docId)
                    }
            }

            // Immediately clear locally as well
            repository.deleteMessageByUnique(sender, recipient, timestamp)
        }
    }

    fun setActiveRecipient(profile: ChatProfile?) {
        _activeRecipient.value = profile
        activeConversationJob?.cancel()
        
        // Cancel previous Firestore listener
        messagesListener?.remove()
        messagesListener = null

        if (profile != null) {
            if (isVirtualUser(profile.username)) {
                // If it's a virtual/AI bot account, we retrieve standard local cached/Simulated conversations
                activeConversationJob = viewModelScope.launch {
                    repository.getConversation(_currentUsername.value, profile.username).collect {
                        _conversationMessages.value = it
                    }
                }
            } else {
                // Real person-to-person online chat synchronized instantly in real-time
                val firestore = db
                if (firestore != null && isFirebaseEnabled) {
                    val conversationId = getConversationId(_currentUsername.value, profile.username)
                    try {
                        messagesListener = firestore.collection("messages")
                            .whereEqualTo("conversationId", conversationId)
                            .addSnapshotListener { snapshot, exception ->
                                if (exception != null) {
                                    android.util.Log.e("CalculatorViewModel", "addSnapshotListener failed: ${exception.message}")
                                    // Fallback to local Room storage
                                    activeConversationJob?.cancel()
                                    activeConversationJob = viewModelScope.launch {
                                        repository.getConversation(_currentUsername.value, profile.username).collect {
                                            _conversationMessages.value = it
                                        }
                                    }
                                    return@addSnapshotListener
                                }
                                if (snapshot != null) {
                                    val messages = mutableListOf<ChatMessage>()
                                    for (doc in snapshot.documents) {
                                        val sender = doc.getString("sender") ?: ""
                                        val recipient = doc.getString("recipient") ?: ""
                                        val text = doc.getString("text") ?: ""
                                        val imageUri = doc.getString("imageUri")
                                        val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                        val isViewOnce = doc.getBoolean("isViewOnce") ?: false
                                        val idVal = doc.id.hashCode().toLong()
                                        val isSeenVal = doc.getBoolean("isSeen") ?: false
                                        val isSavedVal = doc.getBoolean("isSaved") ?: false

                                        if (sender.isNotEmpty() && recipient.isNotEmpty()) {
                                            messages.add(
                                                ChatMessage(
                                                    id = idVal,
                                                    sender = sender,
                                                    recipient = recipient,
                                                    text = text,
                                                    imageUri = imageUri,
                                                    isViewOnce = isViewOnce,
                                                    timestamp = ts,
                                                    isSeen = isSeenVal,
                                                    audioBase64 = doc.getString("audioBase64"),
                                                    audioDurationMs = doc.getLong("audioDurationMs")?.toInt() ?: 0,
                                                    isSaved = isSavedVal
                                                )
                                            )

                                            // If current user is the recipient, mark message as seen in Firestore
                                            if (recipient == _currentUsername.value && !isSeenVal) {
                                                firestore.collection("messages").document(doc.id).update("isSeen", true)
                                            }

                                            if (recipient == _currentUsername.value && sender != _currentUsername.value) {
                                                val now = System.currentTimeMillis()
                                                if (now - ts < 15_000 && !playedMessageIds.contains(idVal)) {
                                                    playedMessageIds.add(idVal)
                                                    playIncomingMessageSound()
                                                }
                                            }

                                            // Vanish Mode Deletion check!
                                            val isVanishM = doc.getBoolean("isVanish") ?: false
                                            if (isVanishM && recipient == _currentUsername.value) {
                                                triggerVanishDeletion(doc.id, sender, recipient, ts)
                                            }
                                        }
                                    }
                                    // Client-side sort on timestamp to avoid custom index mandate in Firebase!
                                    messages.sortBy { it.timestamp }
                                    _conversationMessages.value = messages

                                    // Keep Room database synchronized: Clear local caches and insert fresh ones to mirror remote deletions cleanly!
                                    viewModelScope.launch {
                                        repository.clearConversation(_currentUsername.value, profile.username)
                                        for (msg in messages) {
                                            repository.insertMessage(msg)
                                        }
                                    }
                                }
                            }
                    } catch (e: Exception) {
                        android.util.Log.e("CalculatorViewModel", "Error setting up snapshot listener: ${e.message}")
                        activeConversationJob = viewModelScope.launch {
                            repository.getConversation(_currentUsername.value, profile.username).collect {
                                _conversationMessages.value = it
                            }
                        }
                    }
                } else {
                    activeConversationJob = viewModelScope.launch {
                        repository.getConversation(_currentUsername.value, profile.username).collect {
                            _conversationMessages.value = it
                        }
                    }
                }
            }
        } else {
            _conversationMessages.value = emptyList()
        }
    }

    private var incomingMessagesListener: ListenerRegistration? = null

    fun listenForIncomingMessages(localUser: String) {
        incomingMessagesListener?.remove()
        incomingMessagesListener = null

        if (localUser.isEmpty() || db == null || !isFirebaseEnabled) return

        try {
            incomingMessagesListener = db?.collection("messages")
                ?.whereEqualTo("recipient", localUser)
                ?.addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        android.util.Log.e("CalculatorViewModel", "Incoming listener error: ${exception.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val senders = mutableSetOf<String>()
                        val messagesToInsert = mutableListOf<ChatMessage>()
                        for (doc in snapshot.documents) {
                            val sender = doc.getString("sender") ?: ""
                            val recipient = doc.getString("recipient") ?: ""
                            val text = doc.getString("text") ?: ""
                            val imageUri = doc.getString("imageUri")
                            val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val isViewOnce = doc.getBoolean("isViewOnce") ?: false
                            val idVal = doc.id.hashCode().toLong()
                            val isSeenVal = doc.getBoolean("isSeen") ?: false

                            if (sender.isNotEmpty() && recipient == localUser) {
                                val audioBase64 = doc.getString("audioBase64")
                                val audioDurationMs = doc.getLong("audioDurationMs")?.toInt() ?: 0
                                val isSavedVal = doc.getBoolean("isSaved") ?: false
                                senders.add(sender)
                                messagesToInsert.add(
                                    ChatMessage(
                                        id = idVal,
                                        sender = sender,
                                        recipient = recipient,
                                        text = text,
                                        imageUri = imageUri,
                                        isViewOnce = isViewOnce,
                                        timestamp = ts,
                                        isSeen = isSeenVal,
                                        audioBase64 = audioBase64,
                                        audioDurationMs = audioDurationMs,
                                        isSaved = isSavedVal
                                    )
                                )
                            }
                        }

                        // Insert messages locally so they are cached
                        viewModelScope.launch {
                            var anyNewMessage = false
                            val now = System.currentTimeMillis()
                            for (msg in messagesToInsert) {
                                repository.insertMessage(msg)
                                if (msg.recipient == localUser && msg.sender != localUser && !msg.isSeen) {
                                    if (now - msg.timestamp < 15_000 && !playedMessageIds.contains(msg.id)) {
                                        playedMessageIds.add(msg.id)
                                        anyNewMessage = true
                                    }
                                }
                            }
                            if (anyNewMessage) {
                                playIncomingMessageSound()
                            }

                            // For each sender found in messages, verify if their profile is stored locally.
                            // If not, fetch their profile from Firestore and save it!
                            for (sender in senders) {
                                val localProf = repository.getProfile(sender)
                                if (localProf == null) {
                                    // Fetch from Firestore
                                    db?.collection("profiles")?.document(sender)?.get()
                                        ?.addOnSuccessListener { doc ->
                                            if (doc != null && doc.exists()) {
                                                val fullName = doc.getString("fullName") ?: sender
                                                val bio = doc.getString("bio") ?: ""
                                                val hex = doc.getString("avatarColorHex") ?: "#38BDF8"
                                                val gEmail = doc.getString("googleEmail") ?: ""
                                                val avatarUrl = doc.getString("avatarUrl")
                                                val profile = ChatProfile(
                                                    username = sender,
                                                    fullName = fullName,
                                                    bio = bio,
                                                    avatarColorHex = hex,
                                                    avatarUrl = avatarUrl,
                                                    isCreatedByLocalUser = false,
                                                    googleEmail = gEmail
                                                )
                                                viewModelScope.launch {
                                                    repository.insertProfile(profile)
                                                }
                                            }
                                        }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("CalculatorViewModel", "Error starting incoming messages listener: ${e.message}")
        }
    }

    fun registerOrSwitchUser(username: String, fullName: String, bio: String) {
        val cleanName = username.trim().lowercase().removePrefix("@")
        if (cleanName.isBlank() || fullName.isBlank()) return

        val currentGoogleEmail = googleAccountEmail.value
        if (currentGoogleEmail.isNullOrBlank()) {
            _searchStatusMessage.value = "सुरक्षा त्रुटि: प्रक्रिया पूरी करने के लिए पहले गूगल साइन इन करें!"
            return
        }

        _searchStatusMessage.value = "पंजीकरण की पुष्टि की जा रही है..."

        viewModelScope.launch {
            val firestore = db
            if (firestore != null && isFirebaseEnabled) {
                try {
                    firestore.collection("profiles").document(cleanName).get()
                        .addOnSuccessListener { doc ->
                            if (doc != null && doc.exists()) {
                                val registeredEmail = doc.getString("googleEmail") ?: ""
                                if (registeredEmail.isNotEmpty() && registeredEmail != currentGoogleEmail) {
                                    // Block registration!
                                    _searchStatusMessage.value = "सुरक्षा संकट: यह आईडी पहले से ही अन्य गूगल खाते ($registeredEmail) से पंजीकृत है।"
                                } else {
                                    proceedWithRegistration(cleanName, fullName, bio, currentGoogleEmail)
                                }
                            } else {
                                proceedWithRegistration(cleanName, fullName, bio, currentGoogleEmail)
                            }
                        }
                        .addOnFailureListener { e ->
                            proceedWithRegistration(cleanName, fullName, bio, currentGoogleEmail)
                        }
                } catch (e: Exception) {
                    proceedWithRegistration(cleanName, fullName, bio, currentGoogleEmail)
                }
            } else {
                proceedWithRegistration(cleanName, fullName, bio, currentGoogleEmail)
            }
        }
    }

    private fun proceedWithRegistration(cleanName: String, fullName: String, bio: String, googleEmail: String, avatarUrl: String? = null) {
        android.util.Log.d("CalculatorViewModel", "proceedWithRegistration: $cleanName")
        viewModelScope.launch {
            val colors = listOf("#38BDF8", "#F43F5E", "#A855F7", "#EC4899", "#10B981", "#F59E0B", "#14B8A6")
            val chosenColor = colors.random()

            val newProfile = ChatProfile(
                username = cleanName,
                fullName = fullName,
                bio = bio,
                avatarColorHex = chosenColor,
                avatarUrl = avatarUrl,
                isCreatedByLocalUser = true,
                googleEmail = googleEmail
            )
            repository.insertProfile(newProfile)
            uploadProfileToFirestore(newProfile)
            
            // Log user in
            prefs.edit().putString("active_user", cleanName).apply()
            _currentUsername.value = cleanName
            _searchStatusMessage.value = "आईडी सफलतापूर्वक पंजीकृत और लॉग इन की गई है!"
            
            // Start core sync engines
            listenForIncomingMessages(cleanName)
            listenForChatRequests(cleanName)
            startPresenceTracking(cleanName)
            fetchGlobalProfiles()

            // Refresh conversation if open
            val currentRecipient = _activeRecipient.value
            if (currentRecipient != null) {
                setActiveRecipient(currentRecipient)
            }
        }
    }

    fun updateProfileDetails(fullName: String, bio: String, avatarUrl: String? = null, onComplete: () -> Unit) {
        val username = _currentUsername.value
        android.util.Log.d("CalculatorViewModel", "updateProfileDetails: username=$username, fullName=$fullName, bio=$bio, avatarUrl=$avatarUrl")
        if (username.isEmpty()) return
        viewModelScope.launch {
            var finalAvatarUrl = avatarUrl
            if (avatarUrl != null && avatarUrl.startsWith("content://")) {
                _searchStatusMessage.value = "प्रोफ़ाइल फोटो अपलोड हो रही है..."
                finalAvatarUrl = uploadToCloudinary(avatarUrl)
            }

            val existing = repository.getProfile(username)
            if (existing != null) {
                android.util.Log.d("CalculatorViewModel", "Existing profile found: $existing")
                val updated = existing.copy(fullName = fullName, bio = bio, avatarUrl = finalAvatarUrl ?: existing.avatarUrl)
                android.util.Log.d("CalculatorViewModel", "Inserting updated profile: $updated")
                repository.insertProfile(updated)
                uploadProfileToFirestore(updated)
                _searchStatusMessage.value = "प्रोफ़ाइल सफलतापूर्वक अपडेट हो गई है!"
                onComplete()
            } else {
                android.util.Log.e("CalculatorViewModel", "Existing profile not found for $username")
                onComplete()
            }
        }
    }

    fun checkAndRestoreOrCreateProfileForEmail(email: String, displayName: String) {
        val firestore = db
        if (email.isBlank()) return

        if (firestore == null || !isFirebaseEnabled) {
            // FIREBASE NOT ENABLED OR NULL - LOCAL OFFLINE RESIDUAL FALLBACK SIGN-IN SECURITY GATEWAY
            _searchStatusMessage.value = "लोकल ऑफलाइन मोड सक्रिय: प्रोफ़ाइल लोड की जा रही है..."
            viewModelScope.launch {
                val existingLocal = _profilesList.value.find { it.googleEmail.trim().equals(email.trim(), ignoreCase = true) }
                if (existingLocal != null) {
                    val username = existingLocal.username
                    prefs.edit().putString("active_user", username).apply()
                    _currentUsername.value = username
                    _searchStatusMessage.value = "आपकी ऑफलाइन प्रोफ़ाइल (@$username) लोड हो गई है!"
                    
                    listenForIncomingMessages(username)
                } else {
                    // Create offline default profile
                    val emailPrefix = email.substringBefore("@")
                        .replace(Regex("[^a-zA-Z0-9]"), "")
                        .lowercase()
                        .trim()
                    
                    val fallbackUsername = if (emailPrefix.isEmpty()) "user_${(1000..9999).random()}" else emailPrefix
                    val finalUsername = if (_profilesList.value.any { it.username == fallbackUsername }) {
                        "${fallbackUsername}_${(10..99).random()}"
                    } else {
                        fallbackUsername
                    }
                    
                    val cleanFullName = if (displayName.isBlank() || displayName == "User") {
                        finalUsername.replaceFirstChar { it.uppercase() }
                    } else {
                        displayName
                    }

                    proceedWithRegistration(
                        cleanName = finalUsername,
                        fullName = cleanFullName,
                        bio = "ऑफलाइन सत्यापित अकाउंट 🛡️",
                        googleEmail = email
                    )
                }
            }
            return
        }

        _searchStatusMessage.value = "प्रोफ़ाइल सुरक्षित सत्यापित की जा रही है..."
        
        firestore.collection("profiles")
            .whereEqualTo("googleEmail", email.trim())
            .get()
            .addOnSuccessListener { querySnapshot ->
                android.util.Log.d("CalculatorViewModel", "checkAndRestoreOrCreateProfileForEmail success: ${querySnapshot?.documents?.size ?: 0}")
                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents.first()
                    val username = doc.id
                    val fullName = doc.getString("fullName") ?: displayName
                    val bio = doc.getString("bio") ?: ""
                    val avatarColorHex = doc.getString("avatarColorHex") ?: "#38BDF8"
                    val avatarUrl = doc.getString("avatarUrl")
                    val googleEmailField = doc.getString("googleEmail") ?: email

                    val restoredProfile = ChatProfile(
                        username = username,
                        fullName = fullName,
                        bio = bio,
                        avatarColorHex = avatarColorHex,
                        avatarUrl = avatarUrl,
                        isCreatedByLocalUser = true,
                        googleEmail = googleEmailField
                    )

                    viewModelScope.launch {
                        repository.insertProfile(restoredProfile)
                        // Log user in
                        prefs.edit().putString("active_user", username).apply()
                        _currentUsername.value = username
                        _searchStatusMessage.value = "आपकी पिछली प्रोफ़ाइल (@$username) सुरक्षित रूप से पुनर्प्राप्त कर ली गई है!"
                        
                        // Start core sync engines
                        listenForIncomingMessages(username)
                        listenForChatRequests(username)
                        startPresenceTracking(username)
                        fetchGlobalProfiles()
                    }
                } else {
                    // Profile does NOT exist for this email. Auto-generate a secure clean handle and create it!
                    val emailPrefix = email.substringBefore("@")
                        .replace(Regex("[^a-zA-Z0-9]"), "")
                        .lowercase()
                        .trim()
                    
                    val fallbackUsername = if (emailPrefix.isEmpty()) "user_${(1000..9999).random()}" else emailPrefix

                    firestore.collection("profiles").document(fallbackUsername).get()
                        .addOnSuccessListener { testDoc ->
                            val finalUsername = if (testDoc != null && testDoc.exists()) {
                                "${fallbackUsername}_${(10..99).random()}"
                            } else {
                                fallbackUsername
                            }

                            // Auto-register
                            proceedWithRegistration(
                                cleanName = finalUsername,
                                fullName = if (displayName.isBlank() || displayName == "User") finalUsername.replaceFirstChar { it.uppercase() } else displayName,
                                bio = "Google सत्यापित अकाउंट 🛡️",
                                googleEmail = email
                            )
                        }
                        .addOnFailureListener {
                            val uniqueUser = "${fallbackUsername}_${(10..99).random()}"
                            proceedWithRegistration(
                                cleanName = uniqueUser,
                                fullName = if (displayName.isBlank()) uniqueUser else displayName,
                                bio = "Google सत्यापित अकाउंट 🛡️",
                                googleEmail = email
                            )
                        }
                }
            }
            .addOnFailureListener { e ->
                _searchStatusMessage.value = "सिस्टम विफलता: ${e.message}"
            }
    }

    fun switchActiveUser(profile: ChatProfile) {
        if (!profile.isCreatedByLocalUser) return
        prefs.edit().putString("active_user", profile.username).apply()
        _currentUsername.value = profile.username
        
        // Start core sync engines
        listenForIncomingMessages(profile.username)
        listenForChatRequests(profile.username)
        startPresenceTracking(profile.username)
        fetchGlobalProfiles()

        val rec = _activeRecipient.value
        if (rec != null) {
            setActiveRecipient(rec)
        }
    }

    fun searchAndAddUser(queryHandle: String) {
        val rawHandle = queryHandle.trim().lowercase().removePrefix("@")
        if (rawHandle.isBlank()) {
            _searchStatusMessage.value = "कृपया यूजरनेम दर्ज करें!"
            return
        }
        
        if (rawHandle == _currentUsername.value) {
            _searchStatusMessage.value = "आप अपनी स्वयं की आईडी सर्च नहीं कर सकते हैं!"
            return
        }

        _searchStatusMessage.value = "डेटाबेस में खोज रहे हैं..."

        viewModelScope.launch {
            // Check locally first
            val localExisting = repository.getProfile(rawHandle)
            if (localExisting != null) {
                setActiveRecipient(localExisting)
                _searchStatusMessage.value = null
            } else {
                val firestore = db
                if (firestore != null && isFirebaseEnabled) {
                    try {
                        firestore.collection("profiles").document(rawHandle).get()
                            .addOnSuccessListener { doc ->
                                if (doc != null && doc.exists()) {
                                    val username = doc.getString("username") ?: rawHandle
                                    val fullName = doc.getString("fullName") ?: "Instagram User"
                                    val bio = doc.getString("bio") ?: ""
                                    val color = doc.getString("avatarColorHex") ?: "#A855F7"
                                    val avatarUrl = doc.getString("avatarUrl")
                                    
                                    val matchedProfile = ChatProfile(
                                        username = username,
                                        fullName = fullName,
                                        bio = bio,
                                        avatarColorHex = color,
                                        avatarUrl = avatarUrl,
                                        isCreatedByLocalUser = false
                                    )
                                    viewModelScope.launch {
                                        repository.insertProfile(matchedProfile)
                                        setActiveRecipient(matchedProfile)
                                        _searchStatusMessage.value = "सफल! आईडी मिल गई और चैट शुरू की गई है।"
                                    }
                                } else {
                                    // NO DEMO or Dummy user creation! Keep it strict.
                                    _searchStatusMessage.value = "कोई आईडी नहीं: यह यूजरनेम डेटाबेस में मौजूद नहीं है।"
                                }
                            }
                            .addOnFailureListener { e ->
                                _searchStatusMessage.value = "खोज विफल: डेटाबेस से संपर्क नहीं हो पाया या आईडी नहीं मिली।"
                            }
                    } catch (e: Exception) {
                        _searchStatusMessage.value = "त्रुटि: ${e.localizedMessage ?: "खोज विफलता"}"
                    }
                } else {
                    _searchStatusMessage.value = "त्रुटि: डेटाबेस ऑफलाइन है। पंजीकरण आवश्यक है।"
                }
            }
        }
    }

    fun selectUserFromSearch(profile: ChatProfile) {
        viewModelScope.launch {
            try {
                repository.insertProfile(profile)
                setActiveRecipient(profile)
                _searchStatusMessage.value = "सफल! @${profile.username} के साथ चैट शुरू की गई है।"
            } catch (e: Exception) {
                _searchStatusMessage.value = "त्रुटि: चैट शुरू करने में विफल।"
                setActiveRecipient(profile) // Fallback as in-memory recipient
            }
        }
    }

    fun sendInstagramChatPhoto(imageUri: String, isViewOnce: Boolean) {
        val currentSender = _currentUsername.value
        val currentRec = _activeRecipient.value ?: return
        
        viewModelScope.launch {
            var remoteImageUrl = imageUri
            if (imageUri.startsWith("content://")) {
                val uploadedUrl = uploadToCloudinary(imageUri)
                if (uploadedUrl != null) {
                    remoteImageUrl = uploadedUrl
                } else {
                    try {
                        val contentResolver = getApplication<android.app.Application>().contentResolver
                        contentResolver.openInputStream(android.net.Uri.parse(imageUri))?.use { input ->
                            val bytes = input.readBytes()
                            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                            remoteImageUrl = "data:image/jpeg;base64," + base64.replace("\n", "").replace("\r", "")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CalculatorViewModel", "Failed to encode fallback image: ${e.message}")
                        return@launch
                    }
                }
            }
            
            val isRecVirtual = isVirtualUser(currentRec.username)
            val outgoingMsg = ChatMessage(
                sender = currentSender,
                recipient = currentRec.username,
                text = "[Photo]",
                imageUri = remoteImageUrl,
                isViewOnce = isViewOnce,
                isSeen = isRecVirtual
            )
            repository.insertMessage(outgoingMsg)
            
            if (!isRecVirtual) {
                val firestore = db
                if (firestore != null && isFirebaseEnabled) {
                    try {
                        val convId = getConversationId(currentSender, currentRec.username)
                        val msgData = mapOf(
                            "sender" to currentSender,
                            "recipient" to currentRec.username,
                            "text" to "[Photo]",
                            "imageUri" to remoteImageUrl,
                            "isViewOnce" to isViewOnce,
                            "timestamp" to System.currentTimeMillis(),
                            "conversationId" to convId,
                            "isSeen" to false
                        )
                        firestore.collection("messages").add(msgData)
                    } catch (e: Exception) {
                        android.util.Log.e("CalculatorViewModel", "Error sending photo: ${e.message}")
                    }
                }
            }
        }
    }

    fun sendInstagramVoiceMessage(audioBase64: String, durationMs: Int) {
        val currentSender = _currentUsername.value
        val currentRec = _activeRecipient.value ?: return

        viewModelScope.launch {
            val isRecVirtual = isVirtualUser(currentRec.username)
            val outgoingMsg = ChatMessage(
                sender = currentSender,
                recipient = currentRec.username,
                text = "[Voice Message]",
                isSeen = isRecVirtual,
                audioBase64 = audioBase64,
                audioDurationMs = durationMs
            )
            repository.insertMessage(outgoingMsg)

            if (!isRecVirtual) {
                val firestore = db
                if (firestore != null && isFirebaseEnabled) {
                    try {
                        val convId = getConversationId(currentSender, currentRec.username)
                        val msgData = mapOf(
                            "sender" to currentSender,
                            "recipient" to currentRec.username,
                            "text" to "[Voice Message]",
                            "timestamp" to System.currentTimeMillis(),
                            "conversationId" to convId,
                            "isVanish" to _isVanishMode.value,
                            "isSeen" to false,
                            "audioBase64" to audioBase64,
                            "audioDurationMs" to durationMs
                        )
                        firestore.collection("messages").add(msgData)
                    } catch (e: Exception) {
                        android.util.Log.e("CalculatorViewModel", "Error sending voice: ${e.message}")
                    }
                }
            } else {
                _isChatSending.value = true
                delay(1500)

                val replyText = "मैंने आपका वॉयस मैसेज सुना! बहुत अच्छा लगा। 😊"
                val incomingMsg = ChatMessage(
                    sender = currentRec.username,
                    recipient = currentSender,
                    text = replyText,
                    isSeen = true
                )
                repository.insertMessage(incomingMsg)
                _isChatSending.value = false
            }
        }
    }

    fun sendInstagramChatMessage(text: String) {
        val currentSender = _currentUsername.value
        val currentRec = _activeRecipient.value ?: return
        val textValue = text.trim()
        if (textValue.isEmpty()) return

        // Prevent rapid duplicate message sending (e.g. from emulator button double ticks)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastMessageSentTime < 800 && textValue == lastMessageSentText) {
            return
        }
        lastMessageSentTime = currentTime
        lastMessageSentText = textValue

        viewModelScope.launch {
            val isRecVirtual = isVirtualUser(currentRec.username)
            val outgoingMsg = ChatMessage(
                sender = currentSender,
                recipient = currentRec.username,
                text = textValue,
                isSeen = isRecVirtual // Auto-seen for virtual AI simulation interactively
            )
            repository.insertMessage(outgoingMsg)
            
            if (!isRecVirtual) {
                val firestore = db
                if (firestore != null && isFirebaseEnabled) {
                    try {
                        // P2P REAL CHAT: Write directly to global Firestore "messages" collection
                        val convId = getConversationId(currentSender, currentRec.username)
                        val msgData = mapOf(
                            "sender" to currentSender,
                            "recipient" to currentRec.username,
                            "text" to textValue,
                            "timestamp" to System.currentTimeMillis(),
                            "conversationId" to convId,
                            "isVanish" to _isVanishMode.value,
                            "isSeen" to false
                        )
                        firestore.collection("messages").add(msgData)
                            .addOnFailureListener { e ->
                                android.util.Log.e("CalculatorViewModel", "Firestore failed to store message: ${e.message}")
                            }
                    } catch (e: Exception) {
                        android.util.Log.e("CalculatorViewModel", "Error sending msg through Firestore: ${e.message}")
                    }
                }
            } else {
                // VIRTUAL AI CONTROLLER: local AI simulation
                if (_isVanishMode.value) {
                    viewModelScope.launch {
                        delay(6000)
                        repository.deleteMessageByUnique(currentSender, currentRec.username, outgoingMsg.timestamp)
                    }
                }

                _isChatSending.value = true
                delay(1500)

                val replyText = getVirtualPersonaResponse(currentRec.username, textValue)
                val incomingMsg = ChatMessage(
                    sender = currentRec.username,
                    recipient = currentSender,
                    text = replyText,
                    isSeen = true
                )
                repository.insertMessage(incomingMsg)
                _isChatSending.value = false

                if (_isVanishMode.value) {
                    viewModelScope.launch {
                        delay(5000)
                        repository.deleteMessageByUnique(currentRec.username, currentSender, incomingMsg.timestamp)
                    }
                }
            }
        }
    }

    private suspend fun getVirtualPersonaResponse(recipientUsername: String, userMessage: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotBlank() && apiKey != "YOUR_GEMINI_API_KEY_HERE") {
            try {
                // Incorporate profile settings to Gemini
                val targetProfile = repository.getProfile(recipientUsername)
                val fullName = targetProfile?.fullName ?: recipientUsername
                val bio = targetProfile?.bio ?: ""
                
                // Formulate system prompt guiding the character responder
                val systemPrompt = "You are acting as a real person holding an Instagram chat session. " +
                        "Your handle is @$recipientUsername, your name is '$fullName' and your profile bio/status is '$bio'. " +
                        "Respond to the user's message in a highly concise, genuine, conversational, human-to-human tone. " +
                        "Keep responses under 3 sentences. Do not use AI jargon. Be personal, engaging and styled exactly like a friendly direct message."

                val contents = listOf(Content(parts = listOf(Part(text = userMessage))))
                val systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))

                val request = GenerateContentRequest(
                    contents = contents,
                    systemInstruction = systemInstruction
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: getPersonaFallbackResponse(recipientUsername)
            } catch (e: Exception) {
                return getPersonaFallbackResponse(recipientUsername)
            }
        } else {
            return getPersonaFallbackResponse(recipientUsername)
        }
    }

    private fun getPersonaFallbackResponse(recipientUsername: String): String {
        return when (recipientUsername) {
            "instagram_support" -> {
                val replies = listOf(
                    "Hello! Thanks for writing. Your verification direct channel is successfully configured.",
                    "Active account status: Optimal. Let me know if you require database routing checks.",
                    "DM connection verified. Ready for seamless operation tests."
                )
                replies.random()
            }
            "sneha_kapoor" -> {
                val replies = listOf(
                    "Hey! That sounds super cool. Talk to you in a bit!",
                    "Oh nice! I'm currently editing some awesome photographs. What are you up to?",
                    "Haha yes, definitely! Let's connect soon. Check out my new stories later!"
                )
                replies.random()
            }
            "rahul_dev" -> {
                val replies = listOf(
                    "Hey bro! Just compiling some Compose widgets. What's up?",
                    "That makes total sense. Have you checked out the new Material 3 design spec?",
                    "Awesome! I'll push the code to git in an hour. Talk soon."
                )
                replies.random()
            }
            "elon_musk" -> {
                val replies = listOf(
                    "Interesting proposal. We should write this code on X platform.",
                    "Mars is key. Sustaining life multiplanitarily is the absolute priority.",
                    "Progress is accelerating. Let's build something beautiful."
                )
                replies.random()
            }
            else -> {
                val replies = listOf(
                    "Hey! Really happy to chat here. How is your day going?",
                    "Nice! Let me think about that. Chat with you soon!",
                    "Got it! Let's catch up later tonight."
                )
                replies.random()
            }
        }
    }

    fun deleteProfile(profile: ChatProfile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
            if (_activeRecipient.value?.username == profile.username) {
                _activeRecipient.value = null
            }
        }
    }

    fun clearActiveConversation() {
        val rec = _activeRecipient.value ?: return
        viewModelScope.launch {
            repository.clearConversation(_currentUsername.value, rec.username)
        }
    }

    // --- Presence, Global Profiles and Chat Requests logic ---

    fun startPresenceTracking(localUser: String) {
        presenceJob?.cancel()
        presenceJob = viewModelScope.launch {
            while (true) {
                updateUserPresence(localUser)
                delay(40_000) // update active presence every 40 seconds
            }
        }
    }

    private fun updateUserPresence(username: String) {
        val firestore = db
        if (firestore != null && isFirebaseEnabled && username.isNotEmpty()) {
            val docRef = firestore.collection("profiles").document(username)
            docRef.update("lastActive", System.currentTimeMillis())
                .addOnFailureListener {
                    // fall back to upload if update fails
                    viewModelScope.launch {
                        val profile = repository.getProfile(username)
                        if (profile != null) {
                            uploadProfileToFirestore(profile)
                        }
                    }
                }
        }
    }

    fun fetchGlobalProfiles() {
        val firestore = db
        if (firestore != null && isFirebaseEnabled) {
            firestore.collection("profiles")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        android.util.Log.e("CalculatorViewModel", "Listen failed.", e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val list = mutableListOf<ChatProfile>()
                        for (doc in snapshot.documents) {
                            val username = doc.id
                            val fullName = doc.getString("fullName") ?: username
                            val bio = doc.getString("bio") ?: ""
                            val avatarColorHex = doc.getString("avatarColorHex") ?: "#38BDF8"
                            val avatarUrl = doc.getString("avatarUrl")
                            val gEmail = doc.getString("googleEmail") ?: ""
                            val lastActiveVal = doc.getLong("lastActive") ?: 0L
                            
                            // We map real remote users to non-local profiles
                            list.add(
                                ChatProfile(
                                    username = username,
                                    fullName = fullName,
                                    bio = bio,
                                    avatarColorHex = avatarColorHex,
                                    avatarUrl = avatarUrl,
                                    isCreatedByLocalUser = false,
                                    googleEmail = gEmail,
                                    lastActive = lastActiveVal
                                )
                            )
                        }
                        _globalProfiles.value = list

                        // Optionally: update local room db so they persist and are used in other parts of the app
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                for (profile in list) {
                                    repository.insertProfile(profile)
                                }
                            } catch (writeErr: Exception) {
                                android.util.Log.e("CalculatorViewModel", "Failed to cache global profiles: ${writeErr.message}", writeErr)
                            }
                        }
                    }
                }
        }
    }

    fun getRequestId(user1: String, user2: String): String {
        val u1 = user1.trim().lowercase().removePrefix("@")
        val u2 = user2.trim().lowercase().removePrefix("@")
        return if (u1 < u2) "${u1}_${u2}" else "${u2}_${u1}"
    }

    fun listenForChatRequests(localUser: String) {
        mySentRequestsListener?.remove()
        myReceivedRequestsListener?.remove()
        
        sentRequestsMap.clear()
        receivedRequestsMap.clear()
        _chatRequests.value = emptyList()

        val firestore = db ?: return
        if (localUser.isEmpty() || !isFirebaseEnabled) return

        try {
            // Sent requests
            mySentRequestsListener = firestore.collection("chat_requests")
                .whereEqualTo("sender", localUser)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("CalculatorViewModel", "Sent requests listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        sentRequestsMap.clear()
                        for (doc in snapshot.documents) {
                            val req = parseChatRequest(doc)
                            if (req != null) {
                                sentRequestsMap[req.id] = req
                            }
                        }
                        combineAndPublishRequests()
                    }
                }

            // Received requests
            myReceivedRequestsListener = firestore.collection("chat_requests")
                .whereEqualTo("recipient", localUser)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("CalculatorViewModel", "Received requests listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        for (change in snapshot.documentChanges) {
                            if (change.type == DocumentChange.Type.ADDED) {
                                val req = parseChatRequest(change.document)
                                if (req != null && req.status == "pending") {
                                    showNotification(
                                        getApplication(),
                                        "New Chat Request",
                                        "New request from ${req.sender}"
                                    )
                                }
                            }
                        }
                        receivedRequestsMap.clear()
                        for (doc in snapshot.documents) {
                            val req = parseChatRequest(doc)
                            if (req != null) {
                                receivedRequestsMap[req.id] = req
                            }
                        }
                        combineAndPublishRequests()
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("CalculatorViewModel", "Error listening for chat requests: ${e.message}")
        }
    }

    private fun parseChatRequest(doc: com.google.firebase.firestore.DocumentSnapshot): ChatRequest? {
        val id = doc.id
        val sender = doc.getString("sender") ?: return null
        val recipient = doc.getString("recipient") ?: return null
        val status = doc.getString("status") ?: "pending"
        val timestamp = doc.getLong("timestamp") ?: 0L
        return ChatRequest(id, sender, recipient, status, timestamp)
    }

    private fun combineAndPublishRequests() {
        val combined = (sentRequestsMap.values + receivedRequestsMap.values).distinctBy { it.id }
        _chatRequests.value = combined
    }

    fun sendChatRequest(recipient: String) {
        val sender = _currentUsername.value
        if (sender.isEmpty() || recipient.isEmpty() || sender == recipient) return
        val firestore = db ?: return
        val reqId = getRequestId(sender, recipient)

        val data = mapOf(
            "sender" to sender,
            "recipient" to recipient,
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("chat_requests").document(reqId).set(data)
            .addOnSuccessListener {
                _searchStatusMessage.value = "चैट अनुरोध सफलतापूर्वक भेजा गया!"
                
                // Immersive check: if recipient is AI/virtual, auto-accept after 1.5 seconds!
                if (isVirtualUser(recipient)) {
                    viewModelScope.launch {
                        delay(1500)
                        autoAcceptVirtualRequest(reqId)
                    }
                }
            }
            .addOnFailureListener { e ->
                _searchStatusMessage.value = "सुरक्षा त्रुटि: अनुरोध भेजने में विफल: ${e.message}"
            }
    }

    private fun autoAcceptVirtualRequest(reqId: String) {
        val firestore = db ?: return
        firestore.collection("chat_requests").document(reqId).update("status", "accepted")
            .addOnSuccessListener {
                _searchStatusMessage.value = "चैट अनुरोध स्वीकार कर लिया गया है!"
            }
    }

    fun acceptChatRequest(reqId: String) {
        val firestore = db ?: return
        firestore.collection("chat_requests").document(reqId).update("status", "accepted")
            .addOnSuccessListener {
                _searchStatusMessage.value = "चैट अनुरोध स्वीकार कर लिया गया!"
            }
    }

    fun rejectChatRequest(reqId: String) {
        val firestore = db ?: return
        firestore.collection("chat_requests").document(reqId).update("status", "rejected")
            .addOnSuccessListener {
                _searchStatusMessage.value = "चैट अनुरोध अस्वीकृत कर दिया गया है।"
            }
    }

    fun insertProfileToLocal(profile: ChatProfile) {
        viewModelScope.launch {
            val localExist = repository.getProfile(profile.username)
            if (localExist == null) {
                repository.insertProfile(profile.copy(isCreatedByLocalUser = false))
            }
        }
    }

    // --- Stories / Status Functionality ---
    private val _storiesList = MutableStateFlow<List<UserStory>>(emptyList())
    val storiesList: StateFlow<List<UserStory>> = _storiesList.asStateFlow()

    fun postStory(text: String, imageUri: String? = null) {
        viewModelScope.launch {
            val userProfile = profilesList.value.find { it.username == currentUsername.value }
            val story = UserStory(
                username = currentUsername.value,
                fullName = userProfile?.fullName ?: currentUsername.value,
                text = text,
                avatarColorHex = userProfile?.avatarColorHex ?: "#2196F3",
                imageUri = imageUri,
                timestamp = System.currentTimeMillis()
            )
            repository.insertStory(story)
        }
    }

    fun deleteStory(storyId: Long) {
        viewModelScope.launch {
            repository.deleteStory(storyId)
        }
    }

    fun clearExpiredStories() {
        viewModelScope.launch {
            val cutoff = System.currentTimeMillis() - 86400000L
            repository.deleteExpiredStories(cutoff)
        }
    }

    // --- Persistent Seen/Read Story Tracker ---
    private val _seenStoryIds = MutableStateFlow(
        prefs.getStringSet("seen_story_ids", emptySet())?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    )
    val seenStoryIds: StateFlow<Set<Long>> = _seenStoryIds.asStateFlow()

    fun markStoryAsSeen(id: Long) {
        val current = _seenStoryIds.value.toMutableSet()
        if (current.add(id)) {
            _seenStoryIds.value = current
            prefs.edit().putStringSet("seen_story_ids", current.map { it.toString() }.toSet()).apply()
        }
    }

    fun sendInstagramStoryReply(recipient: String, replyText: String) {
        viewModelScope.launch {
            val outgoingMsg = ChatMessage(
                sender = _currentUsername.value,
                recipient = recipient,
                text = replyText,
                isSeen = false
            )
            repository.insertMessage(outgoingMsg)
            
            // If recipient is virtual, send auto-reply after a realistic typing delay
            if (isVirtualUser(recipient)) {
                delay(1200)
                val autoMsg = ChatMessage(
                    sender = recipient,
                    recipient = _currentUsername.value,
                    text = "स्टोरी पसंद करने के लिए शुक्रिया! 😊❤️",
                    isSeen = true
                )
                repository.insertMessage(autoMsg)
            }
        }
    }

    fun postStoryFromProfile(profileUsername: String, text: String) {
        viewModelScope.launch {
            val prof = repository.getProfile(profileUsername)
            val defaultColor = listOf("#FE2C55", "#FF007F", "#00F260", "#FFC300").random()
            val story = UserStory(
                username = profileUsername,
                fullName = prof?.fullName ?: profileUsername,
                text = text,
                avatarColorHex = prof?.avatarColorHex ?: defaultColor,
                imageUri = null,
                timestamp = System.currentTimeMillis() - (1..12).random() * 3600000L
            )
            repository.insertStory(story)
        }
    }
}

data class ChatRequest(
    val id: String = "",
    val sender: String = "",
    val recipient: String = "",
    val status: String = "pending", // "pending", "accepted", "rejected"
    val timestamp: Long = 0L
)
