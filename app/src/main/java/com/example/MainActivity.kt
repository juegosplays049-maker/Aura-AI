package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.ui.ChatViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.PasswordVisualTransformation

class MainActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val currentUser by viewModel.currentUser.collectAsState()
                val isGuest by viewModel.isGuest.collectAsState()
                
                if (currentUser == null && !isGuest) {
                    LoginScreen(viewModel = viewModel)
                } else {
                    ChatScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: ChatViewModel) {
    var isRegistering by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loginError by viewModel.loginError.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Aura AI (Versión 1.0)",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Diseñada para estar a la moda",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Usuario") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            if (loginError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = loginError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (username.isNotBlank() && password.isNotBlank()) {
                        if (isRegistering) {
                            viewModel.register(username, password)
                        } else {
                            viewModel.login(username, password)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (isRegistering) "Registrarse" else "Iniciar Sesión", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = { viewModel.continueAsGuest() },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Continuar como Invitado (Sin guardar)", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { isRegistering = !isRegistering }) {
                Text(if (isRegistering) "¿Ya tienes cuenta? Inicia Sesión" else "¿No tienes cuenta? Regístrate")
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            val uriHandler = LocalUriHandler.current
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Aura puede cometer errores. Creado por AngelCode-IA y marymansilla6533.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Visitar AngelCode-IA",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://www.youtube.com/@AngelCode-AI")
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Visitar marymansilla6533",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://www.youtube.com/@marymansilla6533")
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val chatHistory by viewModel.chatHistory.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val voiceEnabled by viewModel.voiceEnabled.collectAsState()
    val fontSizeMultiplier by viewModel.fontSizeMultiplier.collectAsState()
    val recognizedText by viewModel.recognizedText.collectAsState()
    
    var inputText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    // Dialog states
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<Long?>(null) }
    
    LaunchedEffect(recognizedText) {
        if (recognizedText.isNotEmpty()) {
            inputText = recognizedText
        }
    }
    
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }
    
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        }
    }
    
    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    val isGuest by viewModel.isGuest.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                if (isGuest) {
                    Text(
                        "El historial no se guarda en Modo Invitado.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Historial de Chats", style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = { viewModel.createNewSession(); coroutineScope.launch { drawerState.close() } }) {
                            Icon(Icons.Default.Add, contentDescription = "Nuevo Chat")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(allSessions) { session ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(100.dp))
                                    .combinedClickable(
                                        onClick = { 
                                            viewModel.changeSession(session.sessionId)
                                            coroutineScope.launch { drawerState.close() }
                                        },
                                        onLongClick = { sessionToDelete = session.sessionId }
                                    ),
                                color = if (session.sessionId == currentSessionId) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = session.title,
                                        maxLines = 1,
                                        color = if (session.sessionId == currentSessionId) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                    
                    if (allSessions.isNotEmpty()) {
                        HorizontalDivider()
                        NavigationDrawerItem(
                            label = { Text("Eliminar Todo el Historial", color = MaterialTheme.colorScheme.error) },
                            selected = false,
                            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { showDeleteAllDialog = true },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text("Aura AI (v1.0)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.increaseFontSize() }) {
                            Icon(Icons.Default.FormatSize, contentDescription = "Agrandar Letras", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { viewModel.toggleVoice() }) {
                            Icon(
                                if (voiceEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff, 
                                contentDescription = "Activar/Desactivar Voz",
                                tint = if (voiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (chatHistory.isNotEmpty()) {
                            IconButton(onClick = { viewModel.downloadLastAudio(context) }) {
                                Icon(Icons.Default.Download, contentDescription = "Descargar audio", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (chatHistory.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Mic, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(64.dp), 
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "¿En qué te puedo ayudar hoy con estilo?", 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 18.sp * fontSizeMultiplier,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    
                    items(chatHistory) { message ->
                        MessageBubble(
                            text = message.text, 
                            isUser = message.isUser, 
                            imageBase64 = message.imageBase64,
                            fontSizeMultiplier = fontSizeMultiplier
                        )
                    }
                    
                    if (isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                // Input Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    AnimatedVisibility(visible = selectedImageUri != null) {
                        Box(modifier = Modifier.padding(bottom = 8.dp)) {
                            Image(
                                painter = rememberAsyncImagePainter(selectedImageUri),
                                contentDescription = "Selected Image",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { 
                                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) 
                            }
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Adjuntar Imagen", tint = MaterialTheme.colorScheme.primary)
                        }
                        
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            placeholder = { 
                                Text(if (isListening) "Escuchando..." else "Escribe o habla...", color = MaterialTheme.colorScheme.onSurfaceVariant) 
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background
                            ),
                            maxLines = 4
                        )
                        
                        if (inputText.isBlank() && selectedImageUri == null) {
                            // Mic Button (Hold to talk)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isListening) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                                if (permission == PackageManager.PERMISSION_GRANTED) {
                                                    viewModel.startListening()
                                                } else {
                                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                }
                                                tryAwaitRelease()
                                                viewModel.stopListening()
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isListening) Icons.Default.Mic else Icons.Default.MicOff, 
                                    contentDescription = "Hablar", 
                                    tint = Color.White
                                )
                            }
                        } else {
                            // Send Button
                            IconButton(
                                onClick = {
                                    var base64Image: String? = null
                                    selectedImageUri?.let { uri ->
                                        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                                        val bitmap = BitmapFactory.decodeStream(inputStream)
                                        val outputStream = ByteArrayOutputStream()
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                                        base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                                    }
                                    viewModel.sendMessage(inputText, base64Image)
                                    inputText = ""
                                    selectedImageUri = null
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Color.White)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    val uriHandler = LocalUriHandler.current
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Aura puede cometer errores. Creado por AngelCode-IA y marymansilla6533.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Visitar AngelCode-IA",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    uriHandler.openUri("https://www.youtube.com/@AngelCode-AI")
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Visitar marymansilla6533",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    uriHandler.openUri("https://www.youtube.com/@marymansilla6533")
                                }
                            )
                        }
                    }
                }
            }
            
            // Dialogs
            if (showDeleteAllDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteAllDialog = false },
                    title = { Text("Eliminar todo") },
                    text = { Text("¿Estás seguro de que quieres eliminar todo el historial de chats? Esta acción no se puede deshacer.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteAllSessions()
                                showDeleteAllDialog = false
                            }
                        ) {
                            Text("Eliminar", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteAllDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
            
            if (sessionToDelete != null) {
                AlertDialog(
                    onDismissRequest = { sessionToDelete = null },
                    title = { Text("Eliminar chat") },
                    text = { Text("¿Estás seguro de que quieres eliminar este chat?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteSession(sessionToDelete!!)
                                sessionToDelete = null
                            }
                        ) {
                            Text("Sí", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { sessionToDelete = null }) {
                            Text("No")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MessageBubble(text: String, isUser: Boolean, imageBase64: String?, fontSizeMultiplier: Float) {
    val backgroundColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
    val shape = if (isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(shape)
                .background(backgroundColor)
                .padding(16.dp)
        ) {
            if (imageBase64 != null) {
                val bitmap = remember(imageBase64) {
                    try {
                        val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    } catch (e: Exception) { null }
                }
                if (bitmap != null) {
                    Image(
                        painter = coil.compose.rememberAsyncImagePainter(model = bitmap),
                        contentDescription = "Image",
                        modifier = Modifier
                            .fillMaxWidth(if (isUser) 0.5f else 1f)
                            .height(if (isUser) 120.dp else 250.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .padding(bottom = 8.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            if (!isUser) {
                com.example.ui.MarkdownViewer(
                    text = text,
                    color = textColor,
                    fontSize = (16.sp * fontSizeMultiplier),
                    lineHeight = (22.sp * fontSizeMultiplier)
                )
            } else {
                Text(
                    text = text,
                    color = textColor,
                    fontSize = (16.sp * fontSizeMultiplier),
                    lineHeight = (22.sp * fontSizeMultiplier)
                )
            }
        }
    }
}
