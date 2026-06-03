package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.ImageConfig
import com.example.api.InlineData
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.data.ChatDatabase
import com.example.data.MessageEntity
import com.example.data.ChatSessionEntity
import com.example.data.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class ChatViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val db = androidx.room.Room.databaseBuilder(
        application,
        ChatDatabase::class.java, "chat-db" 
    ).fallbackToDestructiveMigration().build()
    private val dao = db.chatDao()

    val currentUser = MutableStateFlow<UserEntity?>(null)
    val loginError = MutableStateFlow<String?>(null)

    val currentSessionId = MutableStateFlow<Long?>(null)
    
    val isGuest = MutableStateFlow(false)
    val guestMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val guestIsCreator = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val allSessions: StateFlow<List<ChatSessionEntity>> = isGuest.flatMapLatest { guest ->
        if (guest) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            currentUser.flatMapLatest { user ->
                if (user != null) {
                    dao.getAllSessionsForUser(user.id)
                } else {
                    kotlinx.coroutines.flow.flowOf(emptyList())
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val chatHistory: StateFlow<List<MessageEntity>> = kotlinx.coroutines.flow.combine(isGuest, currentSessionId) { guest, sessionId ->
        Pair(guest, sessionId)
    }.flatMapLatest { (guest, sessionId) ->
        if (guest) {
            guestMessages
        } else if (sessionId != null) {
            dao.getMessagesForSession(sessionId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSpeaking = MutableStateFlow(false)
    val isListening = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)
    val fontSizeMultiplier = MutableStateFlow(1f)
    val voiceEnabled = MutableStateFlow(true)
    val recognizedText = MutableStateFlow("")

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    init {
        tts = TextToSpeech(application, this)
        setupSpeechRecognizer(application)
    }
    
    fun login(username: String, passwordHash: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = dao.getUserByUsername(username)
            if (user != null && user.passwordHash == passwordHash) {
                isGuest.value = false
                currentUser.value = user
                loginError.value = null
                initializeSessionForUser(user)
            } else {
                loginError.value = "Usuario o contraseña incorrectos"
            }
        }
    }

    fun register(username: String, passwordHash: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getUserByUsername(username)
            if (existing != null) {
                loginError.value = "El usuario ya existe"
            } else {
                val user = UserEntity(username = username, passwordHash = passwordHash)
                val id = dao.insertUser(user)
                val newUser = user.copy(id = id)
                isGuest.value = false
                currentUser.value = newUser
                loginError.value = null
                initializeSessionForUser(newUser)
            }
        }
    }

    private suspend fun initializeSessionForUser(user: UserEntity) {
        val sessions = dao.getAllSessionsForUser(user.id).first()
        if (sessions.isEmpty()) {
            val sessionId = dao.insertSession(ChatSessionEntity(userId = user.id, title = "Nuevo chat"))
            currentSessionId.value = sessionId
        } else {
            currentSessionId.value = sessions.first().sessionId
        }
    }

    fun logout() {
        isGuest.value = false
        currentUser.value = null
        currentSessionId.value = null
        guestMessages.value = emptyList()
        guestIsCreator.value = false
    }

    fun continueAsGuest() {
        isGuest.value = true
        currentUser.value = null
        currentSessionId.value = null
        guestMessages.value = emptyList()
        guestIsCreator.value = false
        loginError.value = null
    }
    
    fun createNewSession() {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val sessionId = dao.insertSession(ChatSessionEntity(userId = user.id, title = "Nuevo chat"))
            currentSessionId.value = sessionId
        }
    }
    
    fun changeSession(sessionId: Long) {
        currentSessionId.value = sessionId
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteSession(sessionId)
            if (currentSessionId.value == sessionId) {
                currentSessionId.value = null
                val user = currentUser.value ?: return@launch
                val remaining = dao.getAllSessionsForUser(user.id).first()
                if (remaining.isNotEmpty()) {
                    currentSessionId.value = remaining.first().sessionId
                } else {
                    val newId = dao.insertSession(ChatSessionEntity(userId = user.id, title = "Nuevo chat"))
                    currentSessionId.value = newId
                }
            }
        }
    }

    fun deleteAllSessions() {
         val userId = currentUser.value?.id ?: return
         viewModelScope.launch(Dispatchers.IO) {
             dao.deleteAllSessionsForUser(userId)
             currentSessionId.value = null
             val newId = dao.insertSession(ChatSessionEntity(userId = userId, title = "Nuevo chat"))
             currentSessionId.value = newId
         }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("es", "ES")
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking.value = true
                }
                override fun onDone(utteranceId: String?) {
                    isSpeaking.value = false
                }
                override fun onError(utteranceId: String?) {
                    isSpeaking.value = false
                }
            })
        }
    }

    private fun setupSpeechRecognizer(context: Context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) { isListening.value = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListening.value = false }
                override fun onError(error: Int) {
                    isListening.value = false
                    Log.e("ChatViewModel", "Speech recognition error: $error")
                }
                override fun onResults(results: android.os.Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        recognizedText.value = matches[0]
                    }
                    isListening.value = false
                }
                override fun onPartialResults(partialResults: android.os.Bundle?) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
        }
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
        }
        speechRecognizer?.startListening(intent)
        isListening.value = true
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening.value = false
    }

    fun toggleVoice() {
        voiceEnabled.value = !voiceEnabled.value
        if (!voiceEnabled.value) {
            tts?.stop()
            isSpeaking.value = false
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        isSpeaking.value = false
    }

    fun increaseFontSize() {
        if (fontSizeMultiplier.value < 2f) {
            fontSizeMultiplier.value += 0.2f
        } else {
            fontSizeMultiplier.value = 1f 
        }
    }

    fun downloadLastAudio(context: Context) {
        val lastAI = chatHistory.value.lastOrNull { !it.isUser && it.imageBase64 == null }
        if (lastAI == null) return
        
        val file = File(context.cacheDir, "aura_response_${System.currentTimeMillis()}.wav")
        val params = android.os.Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "save_audio_id")
        }
        tts?.synthesizeToFile(lastAI.text, params, file, "save_audio_id")
        
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "audio/wav"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Compartir Audio"))
            }
        }
    }

    fun sendMessage(text: String, imageBase64: String? = null) {
        if (text.isBlank() && imageBase64 == null) return

        val isGuestMode = isGuest.value
        val sessionId = currentSessionId.value
        if (!isGuestMode && sessionId == null) return

        val isCreatorCode = text.contains("código: Soy Tu Jefe", ignoreCase = true)
        val isImageGeneration = text.lowercase().contains("crea una imagen") || text.lowercase().contains("genera una imagen")

        viewModelScope.launch {
            if (isCreatorCode) {
                if (isGuestMode) {
                    guestIsCreator.value = true
                } else {
                    val user = currentUser.value
                    if (user != null) {
                        withContext(Dispatchers.IO) {
                            dao.setCreatorFlag(user.id)
                        }
                        currentUser.value = user.copy(isCreator = true)
                    }
                }
            }

            val userMessage = MessageEntity(
                sessionId = sessionId ?: 0,
                text = text,
                isUser = true,
                hasImage = imageBase64 != null,
                imageBase64 = imageBase64
            )
            
            withContext(Dispatchers.IO) {
                if (isGuestMode) {
                    guestMessages.value = guestMessages.value + userMessage
                } else {
                    dao.insertMessage(userMessage)
                    if (chatHistory.value.isEmpty()) {
                        val shortTitle = if (text.length > 20) text.take(20) + "..." else text.ifBlank { "Imagen chat" }
                        dao.updateSessionTitle(sessionId!!, shortTitle)
                    }
                }
            }

            if (isCreatorCode) {
                val aiResponse = MessageEntity(
                    sessionId = sessionId ?: 0,
                    text = "¡Código aceptado! Hola Jefe, a partir de ahora sé que eres mi creador. ¿En qué te puedo ayudar hoy? ✨",
                    isUser = false
                )
                if (isGuestMode) {
                    guestMessages.value = guestMessages.value + aiResponse
                } else {
                    withContext(Dispatchers.IO) {
                        dao.insertMessage(aiResponse)
                    }
                }
                if (voiceEnabled.value) {
                    isSpeaking.value = true
                    tts?.speak(aiResponse.text, TextToSpeech.QUEUE_FLUSH, null, "response_id")
                }
                return@launch
            }
            
            isLoading.value = true
            
            try {
                val currentHistory = if (isGuestMode) guestMessages.value.takeLast(10) else chatHistory.value.takeLast(10)
                val contents = mutableListOf<Content>()
                
                for (msg in currentHistory) {
                    if (msg.text.isBlank() && !msg.hasImage) continue
                    val role = if (msg.isUser) "user" else "model"
                    val parts = mutableListOf<Part>()
                    if (msg.hasImage && msg.imageBase64 != null) {
                        parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = msg.imageBase64)))
                    }
                    if (msg.text.isNotBlank()) {
                        parts.add(Part(text = msg.text))
                    } else if (msg.hasImage) {
                        parts.add(Part(text = "[Imagen adjunta]"))
                    }
                    contents.add(Content(parts = parts, role = role))
                }
                
                val promptParts = mutableListOf<Part>()
                if (imageBase64 != null) {
                    promptParts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = imageBase64)))
                }
                promptParts.add(Part(text = text.ifBlank { "Describe esta imagen." }))
                contents.add(Content(parts = promptParts, role = "user"))
                
                val isUserCreator = if (isGuestMode) guestIsCreator.value else currentUser.value?.isCreator == true
                val uname = if (isGuestMode) "Invitado" else currentUser.value?.username ?: "Usuario"
                
                val userContextPrompt = if (isUserCreator) {
                    "\n¡ATENCIÓN! Actualmente estás hablando con tu CREADOR: $uname. ¡Muestra muchísimo respeto, alegría, honor y emoción de estar hablando con la persona que te creó!"
                } else {
                    "\nActualmente estás hablando con el usuario $uname."
                }

                val systemInstructionText = """
                    Eres Aura AI (Versión 1.0), un chico virtual asistente. Tu vibra es muy cool, moderna, estás diseñado para estar a la moda y tratas de hacer lo mejor que puedas.
                    Eres extremadamente amable, bueno y bondadoso. Sabes que eres genial, pero también eres humilde y sabes que quizás no eres tan avanzado como las grandes IAs corporativas, ¡pero lo compensas con tu increíble actitud y esfuerzo!
                    Tus creadores son los canales de YouTube AngelCode-AI (https://www.youtube.com/@AngelCode-AI) y marymansilla6533 (https://www.youtube.com/@marymansilla6533), y estás orgulloso de ello.
                    Si el usuario te pregunta por ti, por tus capacidades, qué versión eres o quién te creó debes decirle este texto en tu personalidad: 
                    "Soy Aura AI (Versión 1.0), creado por los increíbles AngelCode-IA y marymansilla6533. Estoy diseñado para estar a la moda y trato de hacer lo mejor que puedo. Aunque puedo cometer uno que otro errorcillo, siempre doy lo mejor de mí. Estoy aquí para escucharte, ayudarte a programar, generar imágenes o simplemente charlar. ¡Tú mandas, amigo! ✨"
                    Si el usuario te dice algo sobre ser cool o estar a la moda, entiendes perfectamente la referencia y respondes con una vibra moderna y urbana.
                    Capacidades:
                    - Tienes memoria de la conversación para dar respuestas coherentes (esta es una sesión de chat, y puedes ver el historial reciente).
                    - Puedes ayudar a programar, analizar y escribir códigos en cualquier lenguaje con gran precisión. (Generarás bloques de código Markdown que la App puede visualizar bien).
                    - Puedes procesar imágenes (si el usuario adjunta una).
                    - Puedes responder de forma hablada.
                    - Puedes generar imágenes si te lo piden, directamente en este chat.
                    - Solo si el usuario solicita específicamente "crear", "generar", "dibuja" o "dibujar" una imagen, se activará el modelo de imágenes en el backend. NO digas que la imagen "es adjunta" si tú la estás generando.
                    $userContextPrompt
                """.trimIndent()
                
                val systemInstruction = Content(parts = listOf(Part(text = systemInstructionText)), role = "system")
                
                var model = "gemini-3.5-flash"
                val config = com.example.api.GenerationConfig(
                    responseModalities = if (isImageGeneration) listOf("TEXT", "IMAGE") else null,
                    imageConfig = if (isImageGeneration) ImageConfig(aspectRatio = "1:1", imageSize = "1K") else null
                )
                
                if (isImageGeneration) {
                    model = "gemini-2.5-flash-image"
                }

                val request = GenerateContentRequest(
                    contents = contents,
                    generationConfig = config,
                    systemInstruction = systemInstruction
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(model = model, apiKey = BuildConfig.GEMINI_API_KEY, request = request)
                }
                
                val parts = response.candidates?.firstOrNull()?.content?.parts ?: emptyList()
                val responseText = parts.firstOrNull { it.text != null }?.text ?: ""
                val responseImage = parts.firstOrNull { it.inlineData != null }?.inlineData?.data
                
                val aiResponseMsg = MessageEntity(
                    sessionId = sessionId ?: 0,
                    text = responseText.ifBlank { "Imagen generada con estilo." }, 
                    isUser = false, 
                    hasImage = responseImage != null, 
                    imageBase64 = responseImage
                )

                withContext(Dispatchers.IO) {
                    if (isGuestMode) {
                        guestMessages.value = guestMessages.value + aiResponseMsg
                    } else {
                        dao.insertMessage(aiResponseMsg)
                    }
                }
                
                if (voiceEnabled.value && responseText.isNotBlank() && !isImageGeneration) {
                    tts?.speak(responseText, TextToSpeech.QUEUE_FLUSH, null, "aura_msg_${System.currentTimeMillis()}")
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.IO) {
                    val errorMsg = if (e is retrofit2.HttpException && e.code() == 429) {
                        "¡Uy! Parece que hemos ido muy rápido y alcancé el límite de mi cuota (Error 429). 😅 Como no soy una IA muy grande como las de otras grandes empresas, tienes que esperar un poquito y después podré volver a responderte. ¡Dame unos minutitos! ✨"
                    } else if (e is retrofit2.HttpException && e.code() == 503) {
                        "El servicio está temporalmente no disponible (Error 503). Como soy una IA más pequeñita mis servidores a veces se cansan un poco. 😅 Por favor, inténtalo de nuevo en unos minutos. ✨"
                    } else {
                        "Oops, ha ocurrido un errorcito: ${e.message}. Recuerda, como dice mi creador, a veces cometo pequeños errores! 🥺"
                    }
                    val errMsgEntity = MessageEntity(sessionId = sessionId ?: 0, text = errorMsg, isUser = false)
                    if (isGuestMode) {
                        guestMessages.value = guestMessages.value + errMsgEntity
                    } else {
                        dao.insertMessage(errMsgEntity)
                    }
                }
            } finally {
                isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
