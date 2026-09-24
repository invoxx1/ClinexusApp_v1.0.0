package com.example.clinexusapp.ui.screens.chat

import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ArrowDown
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.ImagePlus
import com.composables.icons.lucide.Paperclip
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Send
import com.composables.icons.lucide.X


import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.clinexusapp.R
import com.example.clinexusapp.model.*
import com.example.clinexusapp.ui.components.ClinexusSnackbarHost
import com.example.clinexusapp.ui.theme.*
import com.example.clinexusapp.util.DateUtils
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.viewmodel.ChatViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit, 
    viewModel: ChatViewModel,
    onWalkthroughTarget: (Rect) -> Unit = {},
    onVisibilityChange: (Boolean) -> Unit = {},
) {
    var messageText by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var showAttachmentOptions by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<MessageDetailDTO?>(null) }
    var deleteConversationTarget by remember { mutableStateOf<ConversationDTO?>(null) }

    val context = LocalContext.current
    val contactsState by viewModel.contactsState.collectAsState()
    val conversationsState by viewModel.conversationsState.collectAsState()
    val conversationSearchMatches by viewModel.conversationSearchMatches.collectAsState()
    val conversationSearchLoading by viewModel.conversationSearchLoading.collectAsState()
    val conversationMessagesState by viewModel.conversationMessagesState.collectAsState()
    val sendMessageState by viewModel.sendMessageState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedConversation by viewModel.selectedConversation.collectAsState()
    val selectedContact by viewModel.selectedContact.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, selectedConversation?.conversationId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.stopPollingMessages()
                Lifecycle.Event.ON_START -> selectedConversation?.conversationId?.let(viewModel::startPollingMessages)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopPollingMessages()
        }
    }

    deleteTarget?.let { message ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete message") },
            text = { Text("This message will be removed from the conversation.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteMessage(message.messageId); deleteTarget = null }) { Text("Delete", color = ErrorRed) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }
    deleteConversationTarget?.let { conversation ->
        AlertDialog(
            onDismissRequest = { deleteConversationTarget = null },
            title = { Text("Delete conversation") },
            text = { Text("Delete your conversation with ${conversation.name}? This cannot be undone.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteConversation(conversation.conversationId); deleteConversationTarget = null }) { Text("Delete", color = ErrorRed) } },
            dismissButton = { TextButton(onClick = { deleteConversationTarget = null }) { Text("Cancel") } },
        )
    }

    var currentView by remember { mutableStateOf(ChatView.CONVERSATIONS) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(currentView) {
        onVisibilityChange(currentView != ChatView.MESSAGES)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                val hasRow = c.moveToFirst()
                val name = if (hasRow && nameIndex >= 0) c.getString(nameIndex) else null
                val size = if (hasRow && sizeIndex >= 0 && !c.isNull(sizeIndex)) c.getLong(sizeIndex) else 0L
                
                // 10MB limit (10 * 1024 * 1024 bytes)
                if (size > (10 * 1024 * 1024)) {
                    Toast.makeText(context, "File size exceeds 10MB limit", Toast.LENGTH_LONG).show()
                } else {
                    selectedFileUri = it
                    selectedFileName = name ?: it.path?.substringAfterLast('/') ?: "file"
                }
            } ?: run {
                selectedFileUri = it
                selectedFileName = it.path?.substringAfterLast('/') ?: "file"
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            selectedFileName = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
            } ?: it.path?.substringAfterLast('/') ?: "image"
        }
    }

    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) imagePickerLauncher.launch("image/*")
        else Toast.makeText(context, "Allow photo access to choose an image.", Toast.LENGTH_LONG).show()
    }

    fun openGallery() {
        val permissions = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (permissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            imagePickerLauncher.launch("image/*")
        } else {
            galleryPermissionLauncher.launch(permissions)
        }
    }

    if (showAttachmentOptions) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentOptions = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Add attachment", color = MaterialTheme.colorScheme.onSurface, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Choose a photo from your gallery or attach a file.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Surface(
                    onClick = { showAttachmentOptions = false; openGallery() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Lucide.ImagePlus, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Choose image", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    }
                }
                Surface(
                    onClick = { showAttachmentOptions = false; filePickerLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Lucide.Paperclip, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Attach file", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    val messagesData = (conversationMessagesState as? Resource.Success)?.data
    val dynamicPartner = messagesData?.otherParticipant
    
    val chatPartnerName = if (selectedConversation != null) {
        dynamicPartner?.name ?: selectedConversation?.name
    } else {
        selectedContact?.name
    } ?: "Clinic Chat"

    val chatPartnerProfilePicture = if (selectedConversation != null) {
        dynamicPartner?.profilePicture ?: selectedConversation?.profilePicture
    } else {
        selectedContact?.profilePicture
    }

    LaunchedEffect(sendMessageState) {
        if (sendMessageState is Resource.Success) {
            messageText = ""
            selectedFileUri = null
            selectedFileName = null
            viewModel.resetSendMessageState()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.actionMessages.collect { snackbarHostState.showSnackbar(it) }
    }

    BackHandler(enabled = currentView != ChatView.CONVERSATIONS) {
        when(currentView) {
            ChatView.MESSAGES -> {
                viewModel.selectConversation(null)
                viewModel.stopPollingMessages()
                currentView = ChatView.CONVERSATIONS
            }
            ChatView.CONTACTS -> {
                viewModel.selectContact(null)
                currentView = ChatView.CONVERSATIONS
            }
            else -> {}
        }
    }

    BackHandler(enabled = currentView == ChatView.CONVERSATIONS) { onBack() }

    Scaffold(
        topBar = {
            if (currentView == ChatView.MESSAGES) {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ChatAvatar(
                                imageUrl = chatPartnerProfilePicture,
                                name = chatPartnerName,
                                size = 34.dp,
                                fontSize = 12.sp
                            )
                            Text(
                                text = chatPartnerName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.selectConversation(null)
                            viewModel.stopPollingMessages()
                            currentView = ChatView.CONVERSATIONS
                        }) {
                            Icon(
                                Lucide.ArrowLeft,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    )
                )
            }
        },
        snackbarHost = { ClinexusSnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (currentView == ChatView.CONVERSATIONS) {
                FloatingActionButton(
                    onClick = {
                        searchQuery = ""
                        currentView = ChatView.CONTACTS
                    },
                    containerColor = Color(0xFF1F3A6D),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(12.dp).size(64.dp)
                ) {
                    Icon(Lucide.Plus, contentDescription = "New Chat", modifier = Modifier.size(34.dp))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .chatScreenBackground()
        ) {
            when (currentView) {
                ChatView.CONVERSATIONS -> {
                    Column(modifier = Modifier.weight(1f)) {
                        Column(
                            Modifier.onGloballyPositioned { onWalkthroughTarget(it.boundsInWindow()) }
                        ) {
                            ChatListHeader(title = "Messages")
                            MessageSearchField(searchQuery, { searchQuery = it }, "Search people or messages")
                            if (conversationSearchLoading) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 34.dp, vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(26.dp))
                        Box(modifier = Modifier.weight(1f)) {
                        when (val convState = conversationsState) {
                            is Resource.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            is Resource.Error -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    ChatLoadError(
                                        message = convState.message ?: "Could not load conversations.",
                                        onRetry = viewModel::fetchConversations,
                                    )
                                }
                            }
                            is Resource.Success -> {
                                LaunchedEffect(searchQuery, convState.data) {
                                    viewModel.searchConversationHistory(searchQuery, convState.data)
                                }
                                val conversations = remember(convState.data, searchQuery, conversationSearchMatches) {
                                    searchConversations(convState.data, searchQuery, conversationSearchMatches)
                                }
                                if (conversations.isEmpty() && !conversationSearchLoading) {
                                    ConversationSearchEmptyState(searchQuery)
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 96.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        if (searchQuery.isNotBlank()) {
                                            item {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Text("Conversations", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                                    Text("${conversations.size} found", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                                }
                                            }
                                        }
                                        items(conversations, key = { it.conversationId }) { conversation ->
                                            ConversationItem(
                                                conversation = conversation,
                                                searchQuery = searchQuery,
                                                searchMessage = conversationSearchMatches[conversation.conversationId],
                                                onDelete = { deleteConversationTarget = conversation },
                                            ) {
                                                viewModel.selectConversation(conversation)
                                                currentView = ChatView.MESSAGES
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {}
                        }
                        }
                    }
                }
                ChatView.CONTACTS -> {
                    Column(modifier = Modifier.weight(1f)) {
                        ChatListHeader(
                            title = "New Chat",
                            centered = true,
                            onBack = {
                                searchQuery = ""
                                viewModel.selectContact(null)
                                currentView = ChatView.CONVERSATIONS
                            },
                        )
                        MessageSearchField(searchQuery, { searchQuery = it }, "Search staff or contacts...")
                        Spacer(Modifier.height(30.dp))
                        Text(
                            "Available Contacts",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                        when (val cState = contactsState) {
                            is Resource.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            is Resource.Error -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    ChatLoadError(
                                        message = cState.message ?: "Could not load contacts.",
                                        onRetry = viewModel::fetchContacts,
                                    )
                                }
                            }
                            is Resource.Success -> {
                                val contacts = cState.data.filter {
                                    searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) ||
                                        it.role.contains(searchQuery, ignoreCase = true)
                                }
                                if (contacts.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(text = if (searchQuery.isBlank()) "No contacts available" else "No contacts found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                                    ) {
                                        item {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f), spotColor = Color.Transparent),
                                                shape = RoundedCornerShape(20.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                            ) {
                                                Column(Modifier.padding(horizontal = 14.dp)) {
                                                    contacts.forEachIndexed { index, contact ->
                                                        ContactItem(contact = contact) {
                                                            viewModel.selectContact(contact)
                                                            currentView = ChatView.MESSAGES
                                                        }
                                                        if (index < contacts.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {}
                        }
                        }
                    }
                }
                ChatView.MESSAGES -> {
                    DisposableEffect(Unit) {
                        onDispose { viewModel.stopPollingMessages() }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        val isNewChat = selectedConversation == null
                        if (isNewChat) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    ChatAvatar(imageUrl = chatPartnerProfilePicture, name = chatPartnerName, size = 80.dp, fontSize = 32.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(text = "Message with $chatPartnerName", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                }
                            }
                        } else {
                            when (val messagesState = conversationMessagesState) {
                                is Resource.Loading -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                is Resource.Error -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        ChatLoadError(
                                            message = messagesState.message ?: "Could not load messages.",
                                            onRetry = { selectedConversation?.conversationId?.let(viewModel::fetchConversationMessages) },
                                        )
                                    }
                                }
                                is Resource.Success -> {
                                    val data = messagesState.data
                                    val messages = data.messages
                                    val lastReadId = data.otherParticipantLastReadMessageID ?: -1

                                    LaunchedEffect(messages) {
                                        if (messages.isNotEmpty()) {
                                            selectedConversation?.let { conv ->
                                                viewModel.markConversationAsRead(conv.conversationId, messages.last().messageId)
                                            }
                                        }
                                    }

                                    val groupedMessages = remember(messages) {
                                        messages.groupBy { it.messageDate ?: DateUtils.getDateOnly(it.messageTime) }
                                    }

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(vertical = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        groupedMessages.forEach { (date, messagesInDate) ->
                                            item(key = "date_$date") {
                                                DateHeader(date = date)
                                            }
                                            
                                            itemsIndexed(
                                                items = messagesInDate,
                                                key = { _, message -> message.messageId }
                                            ) { index, message ->
                                                val isFromMe = message.accountType.equals("patient", ignoreCase = true)
                                                val nextIsFromMe = if (index + 1 < messagesInDate.size) {
                                                    messagesInDate[index + 1].accountType.equals("patient", ignoreCase = true)
                                                } else null
                                                
                                                val isLastInGroup = nextIsFromMe == null || nextIsFromMe != isFromMe
                                                val isSeen = isFromMe && message.messageId <= lastReadId
                                                
                                                TealChatBubble(
                                                    message = message, 
                                                    isFromMe = isFromMe, 
                                                    isSeen = isSeen,
                                                    isLastInGroup = isLastInGroup,
                                                    onDelete = { deleteTarget = message },
                                                )
                                                
                                                if (isLastInGroup) {
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }

                    // Floating Pill Input area
                    Surface(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        Column {
                            selectedFileName?.let {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 4.dp)
                                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Lucide.Paperclip, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = it, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { selectedFileUri = null; selectedFileName = null }, modifier = Modifier.size(24.dp)) {
                                        Icon(Lucide.X, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 8.dp)
                            ) {
                                IconButton(
                                    onClick = { showAttachmentOptions = true },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.background, CircleShape)
                                ) {
                                    Icon(Lucide.Plus, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(24.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.background),
                                    color = Color.Transparent
                                ) {
                                    OutlinedTextField(
                                        value = messageText,
                                        onValueChange = { messageText = it },
                                        placeholder = { Text("Type a message", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedBorderColor = Color.Transparent
                                        ),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
                                        maxLines = 5
                                    )
                                }

                                val canSend = messageText.isNotEmpty() || selectedFileUri != null
                                if (canSend) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    IconButton(
                                        onClick = {
                                            val receiverType = selectedConversation?.accountType ?: selectedContact?.accountType ?: ""
                                            val receiverId = selectedConversation?.accountId ?: selectedContact?.accountId ?: -1
                                            val filePart = selectedFileUri?.let { uri ->
                                                try {
                                                    val inputStream = context.contentResolver.openInputStream(uri)
                                                    val tempFile = File(context.cacheDir, selectedFileName ?: "upload_file")
                                                    FileOutputStream(tempFile).use { inputStream?.copyTo(it) }
                                                    val requestBody = tempFile.asRequestBody(context.contentResolver.getType(uri)?.toMediaTypeOrNull())
                                                    MultipartBody.Part.createFormData("file", selectedFileName ?: tempFile.name, requestBody)
                                                } catch (_: Exception) { null }
                                            }
                                            viewModel.sendMessage(receiverType, receiverId, messageText, selectedConversation?.conversationId, filePart)
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(VibrantTeal),
                                        enabled = sendMessageState !is Resource.Loading
                                    ) {
                                        if (sendMessageState is Resource.Loading) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                        } else {
                                            Icon(Lucide.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.chatScreenBackground(): Modifier {
    val backgroundColor = MaterialTheme.colorScheme.background
    val accentColor = MaterialTheme.colorScheme.surfaceVariant
    return drawBehind {
    drawRect(backgroundColor)
    val topAccent = Path().apply {
        moveTo(size.width * 0.55f, 0f)
        cubicTo(size.width * 0.58f, size.height * 0.11f, size.width * 0.78f, size.height * 0.13f, size.width, size.height * 0.18f)
        lineTo(size.width, 0f)
        close()
    }
    drawPath(
        topAccent,
        Brush.linearGradient(
            listOf(backgroundColor, accentColor),
            Offset(size.width * 0.55f, 0f),
            Offset(size.width, size.height * 0.18f),
        ),
    )
    val lowerAccent = Path().apply {
        moveTo(0f, size.height * 0.69f)
        cubicTo(size.width * 0.25f, size.height * 0.69f, size.width * 0.39f, size.height * 0.91f, size.width * 0.76f, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(lowerAccent, Brush.linearGradient(listOf(accentColor, backgroundColor), Offset(0f, size.height * 0.69f), Offset(size.width * 0.76f, size.height)))
}
}

@Composable
private fun ChatListHeader(
    title: String,
    centered: Boolean = false,
    onBack: (() -> Unit)? = null,
) {
    val headerHeight = if (centered) 72.dp else 44.dp
    Box(
        modifier = Modifier.fillMaxWidth().height(headerHeight).padding(horizontal = 22.dp),
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).size(48.dp)) {
                Icon(Lucide.ArrowLeft, "Back", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(30.dp))
            }
        }
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (centered) 24.sp else 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = if (centered) Modifier.align(Alignment.Center) else Modifier.align(Alignment.BottomStart).padding(bottom = 2.dp),
        )
    }
}

@Composable
private fun MessageSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val focusManager = LocalFocusManager.current
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).height(58.dp),
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp, maxLines = 1) },
        leadingIcon = { Icon(Lucide.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp)) },
        trailingIcon = if (value.isNotEmpty()) {
            { IconButton(onClick = { onValueChange("") }) { Icon(Lucide.X, "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
        } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        shape = RoundedCornerShape(30.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

enum class ChatView { CONVERSATIONS, CONTACTS, MESSAGES }

internal fun searchConversations(
    conversations: List<ConversationDTO>,
    rawQuery: String,
    historicalMatches: Map<Int, String> = emptyMap(),
): List<ConversationDTO> {
    val query = rawQuery.trim()
    if (query.isEmpty()) return conversations
    val terms = query.split(Regex("\\s+")).filter(String::isNotBlank)

    return conversations
        .filter { conversation ->
            val searchable = listOf(
                conversation.name,
                conversation.role,
                conversation.lastMessage.orEmpty(),
                historicalMatches[conversation.conversationId].orEmpty(),
            ).joinToString(" ")
            terms.all { term -> searchable.contains(term, ignoreCase = true) }
        }
        .sortedWith(
            compareBy<ConversationDTO> {
                when {
                    it.name.equals(query, ignoreCase = true) -> 0
                    it.name.startsWith(query, ignoreCase = true) -> 1
                    it.name.contains(query, ignoreCase = true) -> 2
                    it.role.contains(query, ignoreCase = true) -> 3
                    else -> 4
                }
            }.thenBy { it.name.lowercase() }
        )
}

fun getInitials(name: String): String {
    return name.split(" ")
        .asSequence()
        .filter { it.isNotEmpty() }
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
}

@Composable
fun ChatAvatar(imageUrl: String?, name: String, size: Dp, fontSize: TextUnit = 16.sp) {
    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = name,
            modifier = Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.ic_person_placeholder),
            error = painterResource(R.drawable.ic_person_placeholder)
        )
    } else {
        Box(
            modifier = Modifier.size(size).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = getInitials(name), color = Color(0xFF1F3A6D), fontWeight = FontWeight.Bold, fontSize = fontSize)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationItem(
    conversation: ConversationDTO,
    searchQuery: String = "",
    searchMessage: String? = null,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.35f }
    )

    LaunchedEffect(dismissState.settledValue) {
        if (dismissState.settledValue != SwipeToDismissBoxValue.Settled) {
            onDelete()
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(ErrorRed).padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) { Text("Delete", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        enableDismissFromStartToEnd = false,
    ) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f), spotColor = Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onDelete),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            ChatAvatar(imageUrl = conversation.profilePicture, name = conversation.name, size = 58.dp, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(text = highlightedSearchText(conversation.name, searchQuery), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    conversation.lastMessageTime?.let {
                        Text(text = DateUtils.formatChatTime(it), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = highlightedSearchText(searchMessage ?: conversation.lastMessage ?: "No messages yet", searchQuery),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(Lucide.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
        }
    }
    }
}

@Composable
private fun highlightedSearchText(text: String, rawQuery: String) = buildAnnotatedString {
    val terms = rawQuery.trim()
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
        .distinctBy(String::lowercase)
        .sortedByDescending(String::length)
    if (terms.isEmpty()) {
        append(text)
        return@buildAnnotatedString
    }
    val matches = Regex(
        terms.joinToString("|") { Regex.escape(it) },
        RegexOption.IGNORE_CASE,
    ).findAll(text).toList()
    if (matches.isEmpty()) {
        append(text)
        return@buildAnnotatedString
    }

    var cursor = 0
    matches.forEach { match ->
        if (match.range.first > cursor) append(text.substring(cursor, match.range.first))
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)) {
            append(text.substring(match.range))
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) append(text.substring(cursor))
}

@Composable
private fun ConversationSearchEmptyState(searchQuery: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Lucide.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (searchQuery.isBlank()) "No active conversations" else "No results for “${searchQuery.trim()}”",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (searchQuery.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Try searching a name, role, or recent message.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ChatLoadError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(message, color = ErrorRed, textAlign = TextAlign.Center)
        TextButton(onClick = onRetry) { Text("Try again") }
    }
}

@Composable
fun ContactItem(contact: ContactDTO, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatAvatar(imageUrl = contact.profilePicture, name = contact.name, size = 58.dp, fontSize = 21.sp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(text = contact.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(text = contact.role, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Lucide.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun DateHeader(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = DateUtils.formatSeparatorDate(date),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TealChatBubble(
    message: MessageDetailDTO, 
    isFromMe: Boolean, 
    isSeen: Boolean = false,
    isLastInGroup: Boolean = true,
    onDelete: () -> Unit = {},
) {
    val horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    val hasAttachment = message.attachmentId != null && message.attachmentId != 0
    val hasText = message.messageContent.isNotEmpty()
    
    val bubbleColor = if (isFromMe) VibrantTeal else MaterialTheme.colorScheme.background
    val textColor = if (isFromMe) Color.White else MaterialTheme.colorScheme.onSurface
    
    val shape = if (isFromMe) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = if (isLastInGroup) 4.dp else 20.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (isLastInGroup) 4.dp else 20.dp, bottomEnd = 20.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).then(if (isFromMe) Modifier.combinedClickable(onClick = {}, onLongClick = onDelete) else Modifier),
        horizontalAlignment = horizontalAlignment
    ) {
        if (hasAttachment) {
            AttachmentBox(
                fileName = message.originalFileName ?: message.fileName ?: "Attachment",
                fileUrl = message.fileUrl,
                downloadUrl = message.downloadUrl,
                fileType = message.fileType,
                isFromMe = isFromMe,
                isStandalone = true
            )
            if (hasText) Spacer(modifier = Modifier.height(4.dp))
        }

        if (hasText) {
            Surface(
                color = bubbleColor,
                shape = shape,
                shadowElevation = 0.5.dp
            ) {
                Text(
                    text = message.messageContent,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
            }
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
        ) {
            val time = DateUtils.formatChatTime(message.messageTime)
            val status = if (isFromMe) {
                if (isSeen) "Seen" else "Sent"
            } else ""

            Text(
                text = if (status.isNotEmpty()) "$time · $status" else time,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (isFromMe) TextAlign.End else TextAlign.Start
            )
        }
    }
}

@Composable
fun AttachmentBox(
    fileName: String,
    fileUrl: String?,
    downloadUrl: String?,
    fileType: String?,
    isFromMe: Boolean,
    isStandalone: Boolean = false
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val contentColor = if (isFromMe && !isStandalone) Color.White else MaterialTheme.colorScheme.onSurface
    val isImage = fileType?.startsWith("image/", ignoreCase = true) == true

    // Use the base URL (before query parameters) as the stable key to prevent blinking during polling
    val stableKey = remember(fileUrl) { fileUrl?.substringBefore('?') }

    val imageRequest = remember(stableKey) {
        if (isImage && fileUrl != null) {
            ImageRequest.Builder(context)
                .data(fileUrl)
                .crossfade(enable = true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()
        } else null
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(if (isStandalone) 0.dp else 8.dp)
            .then(
                if (!isStandalone) {
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isFromMe) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.5f))
                        .padding(10.dp)
                } else Modifier.wrapContentSize()
            )
    ) {
        IconButton(
            onClick = { (downloadUrl ?: fileUrl)?.let { uriHandler.openUri(it) } },
            modifier = Modifier
                .padding(end = 12.dp)
                .size(36.dp)
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f), CircleShape)
        ) {
            Icon(
                Lucide.ArrowDown,
                contentDescription = "Download", 
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = 240.dp)
                .heightIn(min = if (isImage && !isStandalone) 100.dp else 0.dp)
        ) {
            if (isImage && fileUrl != null && imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = fileName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { (downloadUrl ?: fileUrl).let { uriHandler.openUri(it) } },
                    contentScale = ContentScale.Fit,
                    placeholder = painterResource(R.drawable.ic_image_placeholder),
                    error = painterResource(R.drawable.ic_image_placeholder)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { (downloadUrl ?: fileUrl)?.let { uriHandler.openUri(it) } }
                ) {
                    Icon(Lucide.Paperclip, null, tint = contentColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = fileName, 
                        color = contentColor, 
                        fontSize = 13.sp, 
                        fontWeight = FontWeight.Medium, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
