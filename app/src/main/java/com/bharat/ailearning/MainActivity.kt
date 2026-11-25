package com.bharat.ailearning

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            tts = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale("hi", "IN")
                    tts?.setSpeechRate(0.95f)
                }
            }
        } catch (_: Exception) {}

        setContent {
            BharatAIApp(
                onSpeak = { text, enabled ->
                    try {
                        if (enabled) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                        else tts?.stop()
                    } catch (_: Exception) {}
                },
                onStopSpeak = { try { tts?.stop() } catch (_: Exception) {} }
            )
        }
    }

    override fun onDestroy() {
        try { tts?.stop(); tts?.shutdown() } catch (_: Exception) {}
        super.onDestroy()
    }
}

data class ChatMessage(val text: String, val isUser: Boolean)

data class StudentProfile(
    val name: String,
    val age: String,
    val studentClass: String,
    val dreamExam: String,
    val dreamCollege: String
)

data class AppStrings(
    val appTitle: String, val tabAi: String, val tabRoadmap: String, val tabProfile: String,
    val askHint: String, val listeningText: String, val darkModeLabel: String,
    val languageLabel: String, val autoVoiceLabel: String
)

fun getStrings(lang: String): AppStrings = when (lang) {
    "हिन्दी" -> AppStrings(
        "BharatAI • ऑफलाइन गुरु", "बोलकर पूछें", "रोडमैप & क्विज़", "प्रोफाइल & सेटिंग",
        "कोई भी सवाल बोलें या लिखें...", "सुन रहा हूँ... बोलिए",
        "डार्क मोड (Dark Mode)", "ऐप की भाषा चुनें", "AI जवाब बोलकर सुनाएं (Auto-Voice)"
    )
    "অসমীয়া" -> AppStrings(
        "BharatAI • অফলাইন গুৰু", "কথা পাতক", "ৰোডমেপ & কুইজ", "প্ৰফাইল & ছেটিংছ",
        "যিকোনো প্ৰশ্ন সোধক...", "শুনি আছো...",
        "ডাৰ্ক মোড", "ভাষা নিৰ্বাচন কৰক", "অটো ভয়েচ (Auto-TTS)"
    )
    "বাংলা" -> AppStrings(
        "BharatAI • অফলাইন গুরু", "কথা বলুন", "রোডম্যাপ & কুইজ", "প্রোফাইল & সেটিংস",
        "যেকোনো প্রশ্ন জিজ্ঞাসা করুন...", "শুনছি...",
        "ডার্ক মোড", "ভাষা নির্বাচন করুন", "অটো ভয়েস (Auto-TTS)"
    )
    else -> AppStrings(
        "BharatAI • Offline AI Tutor", "Voice Chat", "Roadmap & Quiz", "Profile & Settings",
        "Ask any study question...", "Listening... Speak now",
        "Dark Mode", "Select App Language", "AI Auto-Voice Reply (TTS)"
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BharatAIApp(onSpeak: (String, Boolean) -> Unit, onStopSpeak: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("BharatAI_Prefs", Context.MODE_PRIVATE) }
    var isDarkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", true)) }
    var selectedLanguage by remember { mutableStateOf(prefs.getString("language", "हिन्दी") ?: "हिन्दी") }
    var isAutoTtsEnabled by remember { mutableStateOf(prefs.getBoolean("auto_tts", true)) }
    var selectedTab by remember { mutableIntStateOf(0) }

    var studentProfile by remember {
        mutableStateOf(
            StudentProfile(
                prefs.getString("p_name", "छात्र (Student)") ?: "छात्र (Student)",
                prefs.getString("p_age", "17") ?: "17",
                prefs.getString("p_class", "Class 12th") ?: "Class 12th",
                prefs.getString("p_exam", "JEE / NEET / CUET") ?: "JEE / NEET / CUET",
                prefs.getString("p_college", "IIT / NEHU Shillong") ?: "IIT / NEHU Shillong"
            )
        )
    }

    val strings = getStrings(selectedLanguage)
    val colorScheme = if (isDarkMode) darkColorScheme(
        primary = Color(0xFF38BDF8), secondary = Color(0xFF4ADE80),
        background = Color(0xFF0B1120), surface = Color(0xFF1E293B),
        onSurface = Color(0xFFF8FAFC), outline = Color(0xFF334155)
    ) else lightColorScheme(
        primary = Color(0xFF0284C7), secondary = Color(0xFF16A34A),
        background = Color(0xFFF1F5F9), surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF0F172A), outline = Color(0xFFCBD5E1)
    )

    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(strings.appTitle, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text("100% Offline • On-Device LLM • ${studentProfile.dreamExam}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        },
                        actions = {
                            IconButton(onClick = onStopSpeak) { Icon(Icons.Default.VolumeOff, "Mute") }
                            IconButton(onClick = {
                                isDarkMode = !isDarkMode
                                prefs.edit().putBoolean("dark_mode", isDarkMode).apply()
                            }) { Icon(if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, "Theme") }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
                }
            },
            bottomBar = {
                Column {
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        listOf(
                            Pair(strings.tabAi, Icons.Default.Mic),
                            Pair(strings.tabRoadmap, Icons.Default.Timeline),
                            Pair(strings.tabProfile, Icons.Default.Person)
                        ).forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = { Icon(item.second, item.first) },
                                label = { Text(item.first, fontSize = 12.sp, maxLines = 1) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
                when (selectedTab) {
                    0 -> UniversalVoiceChatScreen(strings, studentProfile, isAutoTtsEnabled, onSpeak)
                    1 -> RoadmapAndQuizScreen(studentProfile)
                    2 -> ProfileAndSettingsScreen(
                        strings, studentProfile,
                        onSaveProfile = { u ->
                            studentProfile = u
                            prefs.edit().putString("p_name", u.name).putString("p_age", u.age)
                                .putString("p_class", u.studentClass).putString("p_exam", u.dreamExam)
                                .putString("p_college", u.dreamCollege).apply()
                        },
                        isDarkMode, { isDarkMode = it; prefs.edit().putBoolean("dark_mode", it).apply() },
                        selectedLanguage, { selectedLanguage = it; prefs.edit().putString("language", it).apply() },
                        isAutoTtsEnabled, { isAutoTtsEnabled = it; prefs.edit().putBoolean("auto_tts", it).apply() }
                    )
                }
            }
        }
    }
}
@Composable
fun UniversalVoiceChatScreen(
    strings: AppStrings,
    profile: StudentProfile,
    isTtsEnabled: Boolean,
    onSpeak: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var userInput by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var llmInference by remember { mutableStateOf<LlmInference?>(null) }
    var modelStatus by remember { mutableStateOf("AI मॉडल लोड हो रहा है...") }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                "नमस्ते ${profile.name}! नीचे हरे माइक को दबाकर बोलें या लिखकर दुनिया के किसी भी विषय के बारे में पूछें।",
                isUser = false
            )
        )
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val possibleDirs = listOf(
                    context.filesDir,
                    context.getExternalFilesDir(null),
                    File("/data/local/tmp"),
                    File("/sdcard/Download")
                )
                var foundModel: File? = null
                for (dir in possibleDirs) {
                    if (dir != null && dir.exists()) {
                        dir.listFiles()?.forEach { file ->
                            if (file.name.endsWith(".bin") || file.name.endsWith(".task")) {
                                foundModel = file
                            }
                        }
                    }
                }

                if (foundModel != null) {
                    val options = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(foundModel!!.absolutePath)
                       .setMaxTokens(256)
                       .setPreferredBackend(LlmInference.Backend.GPU)


                        .build()
                    val engine = LlmInference.createFromOptions(context, options)
                    withContext(Dispatchers.Main) {
                        llmInference = engine
                        modelStatus = "BharatAI LLM तैयार है (${foundModel!!.name})"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        modelStatus = "मॉडल फाइल (.bin / .task) नहीं मिली! Download या Android/data/com.bharat.ailearning/files/ में रखें"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    modelStatus = "Error: ${e.message}"
                }
            }
        }
    }

    fun sendMessage(questionText: String) {
        val q = questionText.trim()
        if (q.isEmpty() || isGenerating) return

        messages.add(ChatMessage(q, isUser = true))
        userInput = ""
        isGenerating = true

        val aiMsgIndex = messages.size
        messages.add(ChatMessage("सोच रहा हूँ...", isUser = false))

        scope.launch { listState.animateScrollToItem(messages.size - 1) }

        scope.launch(Dispatchers.IO) {
            val replyText = if (llmInference != null) {
                try {
                    val prompt = "You are BharatAI, a smart offline study tutor for ${profile.studentClass} (${profile.dreamExam}). Answer clearly and concisely: $q"
                    llmInference!!.generateResponse(prompt)
                } catch (e: Exception) {
                    "AI Error: ${e.message}"
                }
            } else {
                "⚠️ $modelStatus"
            }

            withContext(Dispatchers.Main) {
                val cleanReply = replyText.trim()
                messages[aiMsgIndex] = ChatMessage(cleanReply, isUser = false)
                onSpeak(cleanReply, isTtsEnabled)
                isGenerating = false
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            }
            speechRecognizer.startListening(intent)
        } else {
            Toast.makeText(context, "माइक की अनुमति दें", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onResults(results: Bundle?) {
                isListening = false
                val spokenText = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!spokenText.isNullOrBlank()) sendMessage(spokenText)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose { speechRecognizer.destroy() }
    }

    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Status: $modelStatus",
                fontSize = 11.sp,
                color = if (llmInference != null) MaterialTheme.colorScheme.secondary else Color(0xFFF59E0B),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages) { msg ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                    colors = CardDefaults.cardColors(
                        containerColor = if (msg.isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (msg.isUser) profile.name else "BharatAI • Offline LLM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                            if (!msg.isUser) {
                                IconButton(
                                    onClick = { onSpeak(msg.text, true) },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(Icons.Default.VolumeUp, "Speak", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(msg.text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                        }
                        speechRecognizer.startListening(intent)
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                shape = CircleShape,
                containerColor = if (isListening) Color.Red else Color(0xFF16A34A),
                modifier = Modifier.size(50.dp)
            ) {
                Icon(Icons.Default.Mic, "Voice", tint = Color.White)
            }

            OutlinedTextField(
                value = userInput,
                onValueChange = { userInput = it },
                placeholder = { Text(if (isListening) strings.listeningText else strings.askHint, fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

                IconButton(
                onClick = { sendMessage(userInput) },
                enabled = !isGenerating && userInput.isNotBlank(),
                modifier = Modifier.size(50.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.White)
            }
        }
    }
}

@Composable
fun RoadmapAndQuizScreen(profile: StudentProfile) {
    var selectedAnswer by remember { mutableIntStateOf(-1) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("🎯 ${profile.dreamExam} Study Roadmap", fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp)) {
                Text("Weekly Skill Mastery Graph", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Canvas(Modifier.fillMaxWidth().height(90.dp)) {
                    val points = listOf(0.25f, 0.45f, 0.40f, 0.65f, 0.80f, 0.92f)
                    val path = Path()
                    points.forEachIndexed { i, p ->
                        val x = (size.width / (points.size - 1)) * i
                        val y = size.height * (1f - p)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        drawCircle(Color(0xFF38BDF8), radius = 8f, center = Offset(x, y))
                    }
                    drawPath(path, color = Color(0xFF4ADE80), style = Stroke(width = 5f))
                }
            }
        }

        listOf(
            "Week 1: Core Concepts & Formula Revision (${profile.studentClass})",
            "Week 2: Weak Topic Identification & Daily Practice",
            "Week 3: Full-Length Offline Mock Tests & Analysis"
        ).forEach { step ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Text(step, modifier = Modifier.padding(14.dp), fontSize = 14.sp)
            }
        }

        Text("📝 Quick Practice Quiz", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Q1. SI unit of Electric Current is:", fontWeight = FontWeight.SemiBold)
                listOf("Volt", "Ampere", "Ohm", "Watt").forEachIndexed { idx, opt ->
                    OutlinedButton(
                        onClick = { selectedAnswer = idx },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = when {
                                selectedAnswer == idx && idx == 1 -> Color(0xFF16A34A).copy(alpha = 0.25f)
                                selectedAnswer == idx -> Color.Red.copy(alpha = 0.25f)
                                else -> Color.Transparent
                            }
                        )
                    ) { Text(opt) }
                }
            }
        }
    }
}

@Composable
fun ProfileAndSettingsScreen(
    strings: AppStrings,
    profile: StudentProfile,
    onSaveProfile: (StudentProfile) -> Unit,
    isDarkMode: Boolean,
    onToggleDark: (Boolean) -> Unit,
    selectedLanguage: String,
    onSelectLanguage: (String) -> Unit,
    isAutoTts: Boolean,
    onToggleTts: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(profile.name) }
    var studentClass by remember { mutableStateOf(profile.studentClass) }
    var exam by remember { mutableStateOf(profile.dreamExam) }
    var college by remember { mutableStateOf(profile.dreamCollege) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("👤 Student Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = studentClass, onValueChange = { studentClass = it }, label = { Text("Class") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = exam, onValueChange = { exam = it }, label = { Text("Target Exam") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = college, onValueChange = { college = it }, label = { Text("Dream College") }, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                onSaveProfile(profile.copy(name = name, studentClass = studentClass, dreamExam = exam, dreamCollege = college))
                Toast.makeText(context, "Profile Saved!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save Profile") }

        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(strings.darkModeLabel)
            Switch(checked = isDarkMode, onCheckedChange = onToggleDark)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(strings.autoVoiceLabel)
            Switch(checked = isAutoTts, onCheckedChange = onToggleTts)
        }

        Text(strings.languageLabel, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("हिन्दी", "English", "অসমীয়া", "বাংলা").forEach { lang ->
                FilterChip(selected = selectedLanguage == lang, onClick = { onSelectLanguage(lang) }, label = { Text(lang) })
            }
        }
    }
}
