package com.example.clinexusapp.ui.screens.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.example.clinexusapp.ui.components.ElegantTopAppBar
import com.example.clinexusapp.ui.theme.*
import com.example.clinexusapp.util.DateUtils
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.viewmodel.ChatViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit, 
    viewModel: ChatViewModel,
    onVisibilityChange: (Boolean) -> Unit = {},
) {
    var messageText by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val contactsState by viewModel.contactsState.collectAsState()
    val conversationsState by viewModel.conversationsState.collectAsState()
    val conversationMessagesState by viewModel.conversationMessagesState.collectAsState()
    val sendMessageState by viewModel.sendMessageState.collectAsState()

    val selectedConversation by viewModel.selectedConversation.collectAsState()
    val selectedContact by viewModel.selectedContact.collectAsState()

    var currentView by remember { mutableStateOf(ChatView.CONVERSATIONS) }

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
                c.moveToFirst()
                
                val name = c.getString(nameIndex)
                val size = c.getLong(sizeIndex)
                
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

    val messagesData = (conversationMessagesState as? Resource.Success)?.data
    val dynamicPartner = messagesData?.otherParticipant
    
    val chatPartnerName = dynamicPartner?.name 
        ?: selectedConversation?.name 
        ?: selectedContact?.name 
        ?: "Clinic Chat"
        
    val chatPartnerProfilePicture = dynamicPartner?.profilePicture 
        ?: selectedConversation?.profilePicture 
        ?: selectedContact?.profilePicture

    LaunchedEffect(sendMessageState) {
        if (sendMessageState is Resource.Success) {
            messageText = ""
            selectedFileUri = null
            selectedFileName = null
            viewModel.resetSendMessageState()
        }
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
                                    color = RoyalNavy,
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
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Back",
                                tint = VibrantTeal,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White.copy(alpha = 0.95f),
                    )
                )
            } else {
                ElegantTopAppBar(
                    title = when(currentView) {
                        ChatView.CONVERSATIONS -> "Messages"
                        ChatView.CONTACTS -> "New Chat"
                        else -> "Clinic Chat"
                    },
                    onBack = {
                        when(currentView) {
                            ChatView.CONVERSATIONS -> onBack()
                            ChatView.CONTACTS -> {
                                viewModel.selectContact(null)
                                currentView = ChatView.CONVERSATIONS
                            }
                            else -> {}
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (currentView == ChatView.CONVERSATIONS) {
                FloatingActionButton(
                    onClick = { currentView = ChatView.CONTACTS },
                    containerColor = RoyalNavy,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat")
                }
            }
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentView) {
                ChatView.CONVERSATIONS -> {
                    Box(modifier = Modifier.weight(1f)) {
                        when (val convState = conversationsState) {
                            is Resource.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = VibrantTeal)
                                }
                            }
                            is Resource.Error -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(text = convState.message ?: "An error occurred", color = Color.Red)
                                }
                            }
                            is Resource.Success -> {
                                val conversations = convState.data
                                if (conversations.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(text = "No active conversations", color = SlateGray)
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(conversations) { conversation ->
                                            ConversationItem(conversation = conversation) {
                                                viewModel.selectConversation(conversation)
                                                currentView = ChatView.MESSAGES
                                            }
                                            HorizontalDivider(
                                                modifier = Modifier.padding(start = 76.dp),
                                                thickness = 0.5.dp,
                                                color = SoftMist
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
                ChatView.CONTACTS -> {
                    Box(modifier = Modifier.weight(1f)) {
                        when (val cState = contactsState) {
                            is Resource.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = VibrantTeal)
                                }
                            }
                            is Resource.Error -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(text = cState.message ?: "An error occurred", color = Color.Red)
                                }
                            }
                            is Resource.Success -> {
                                val contacts = cState.data
                                if (contacts.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(text = "No contacts available", color = SlateGray)
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(contacts) { contact ->
                                            ContactItem(contact = contact) {
                                                viewModel.selectContact(contact)
                                                currentView = ChatView.MESSAGES
                                            }
                                            HorizontalDivider(
                                                modifier = Modifier.padding(start = 76.dp),
                                                thickness = 0.5.dp,
                                                color = SoftMist
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {}
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
                                    Text(text = "Message with $chatPartnerName", color = SlateGray, fontSize = 13.sp)
                                }
                            }
                        } else {
                            when (val messagesState = conversationMessagesState) {
                                is Resource.Loading -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = VibrantTeal)
                                    }
                                }
                                is Resource.Error -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(text = messagesState.message ?: "An error occurred", color = Color.Red)
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
                                                    isLastInGroup = isLastInGroup
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
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        tonalElevation = 0.dp
                    ) {
                        Column {
                            selectedFileName?.let {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 4.dp)
                                        .background(SoftMist, RoundedCornerShape(12.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AttachFile, null, tint = VibrantTeal, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = it, color = RoyalNavy, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { selectedFileUri = null; selectedFileName = null }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, null, tint = SlateGray, modifier = Modifier.size(16.dp))
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
                                    onClick = { filePickerLauncher.launch("*/*") },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(SoftMist, CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, null, tint = RoyalNavy, modifier = Modifier.size(20.dp))
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(24.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SoftMist),
                                    color = Color.Transparent
                                ) {
                                    OutlinedTextField(
                                        value = messageText,
                                        onValueChange = { messageText = it },
                                        placeholder = { Text("Type a message", color = SlateGray, fontSize = 15.sp) },
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
                                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
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

enum class ChatView { CONVERSATIONS, CONTACTS, MESSAGES }

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
            modifier = Modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.ic_person_placeholder),
            error = painterResource(R.drawable.ic_person_placeholder)
        )
    } else {
        Box(
            modifier = Modifier.size(size).background(SoftMist, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = getInitials(name), color = SlateGray, fontWeight = FontWeight.Bold, fontSize = fontSize)
        }
    }
}

@Composable
fun ConversationItem(conversation: ConversationDTO, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatAvatar(imageUrl = conversation.profilePicture, name = conversation.name, size = 52.dp, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = conversation.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RoyalNavy, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (conversation.lastMessageTime != null) {
                    Text(text = DateUtils.formatChatTime(conversation.lastMessageTime), fontSize = 13.sp, color = SlateGray)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = SoftMist, modifier = Modifier.size(16.dp))
                }
            }
            Text(text = conversation.lastMessage ?: "No messages yet", fontSize = 14.sp, color = SlateGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ContactItem(contact: ContactDTO, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatAvatar(imageUrl = contact.profilePicture, name = contact.name, size = 52.dp, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = contact.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RoyalNavy)
            Text(text = contact.role, fontSize = 13.sp, color = SlateGray)
        }
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
                color = SlateGray,
                fontSize = 12.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(SoftMist.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun TealChatBubble(
    message: MessageDetailDTO, 
    isFromMe: Boolean, 
    isSeen: Boolean = false,
    isLastInGroup: Boolean = true
) {
    val horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    val hasAttachment = message.attachmentId != null && message.attachmentId != 0
    val hasText = message.messageContent.isNotEmpty()
    
    val bubbleColor = if (isFromMe) RoyalNavy else SoftMist
    val textColor = if (isFromMe) Color.White else RoyalNavy
    
    val shape = if (isFromMe) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = if (isLastInGroup) 4.dp else 20.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (isLastInGroup) 4.dp else 20.dp, bottomEnd = 20.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
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
                color = SlateGray,
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
    val contentColor = if (isFromMe && !isStandalone) Color.White else RoyalNavy
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
                .background(SoftMist.copy(alpha = 0.8f), CircleShape)
        ) {
            Icon(
                Icons.Default.ArrowDownward, 
                contentDescription = "Download", 
                tint = RoyalNavy,
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
                    Icon(Icons.Default.AttachFile, null, tint = contentColor, modifier = Modifier.size(18.dp))
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
