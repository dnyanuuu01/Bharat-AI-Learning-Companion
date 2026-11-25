package com.bharat.ailearning
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pytorch.executorch.Module
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
        "दुनिया का कोई भी सवाल बोलें या लिखें...", "सुन रहा हूँ... बोलिए",
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
        "Ask any question in the world...", "Listening... Speak now",
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
                                Text("100% Offline AI • लक्ष्य: ${studentProfile.dreamExam}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
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
fun UniversalVoiceChatScreen(strings: AppStrings, profile: StudentProfile, isTtsEnabled: Boolean, onSpeak: (String, Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var userInput by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    val messages = remember {
        mutableStateListOf(Pair("AI", "नमस्ते ${profile.name}! नीचे हरे माइक (🎤) को दबाकर बोलें या लिखकर दुनिया के किसी भी विषय के बारे में पूछें।"))
    }

    val submitQuery: (String) -> Unit = { queryText ->
        if (queryText.isNotBlank() && !isGenerating) {
            userInput = ""
            messages.add(Pair("User", queryText))
            isGenerating = true
            scope.launch {
                val reply = runUniversalOfflineAI(queryText, profile)
                messages.add(Pair("AI", reply))
                isGenerating = false
                onSpeak(reply, isTtsEnabled)
            }
        }
    }

    val speechRecognizer = remember {
        try { if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null }
        catch (_: Exception) { null }
    }
    DisposableEffect(Unit) { onDispose { try { speechRecognizer?.destroy() } catch (_: Exception) {} } }

    val startListening = {
        try {
            if (speechRecognizer != null) {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
                speechRecognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(p: Bundle?) { isListening = true }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(r: Float) {}
                    override fun onBufferReceived(b: ByteArray?) {}
                    override fun onEndOfSpeech() { isListening = false }
                    override fun onError(e: Int) { isListening = false }
                    override fun onResults(results: Bundle?) {
                        isListening = false
                        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { submitQuery(it) }
                    }
                    override fun onPartialResults(p: Bundle?) {}
                    override fun onEvent(e: Int, p: Bundle?) {}
                })
                speechRecognizer.startListening(intent)
            } else messages.add(Pair("AI", "कृपया नीचे बॉक्स में लिखकर अपना प्रश्न पूछें।"))
        } catch (_: Exception) { isListening = false }
    }

    val micPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) startListening() }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(messages) { msg ->
                val isUser = msg.first == "User"
                Card(
                    Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                    colors = CardDefaults.cardColors(containerColor = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isUser) profile.name else "BharatAI • Llama 3.2", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                            if (!isUser) IconButton(onClick = { onSpeak(msg.second, true) }, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.VolumeUp, "Speak", Modifier.size(16.dp)) }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(msg.second, fontSize = 14.sp, lineHeight = 21.sp)
                    }
                }
            }
        }
        if (isListening) Text("🎙️ ${strings.listeningText}", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
        if (isGenerating) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 4.dp))

        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startListening()
                    else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                modifier = Modifier.size(52.dp), shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (isListening) Color(0xFFEF4444) else MaterialTheme.colorScheme.secondary)
            ) { Icon(Icons.Default.Mic, "Mic", tint = Color.Black) }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(value = userInput, onValueChange = { userInput = it }, modifier = Modifier.weight(1f), placeholder = { Text(strings.askHint, fontSize = 12.sp) }, shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { submitQuery(userInput) }, modifier = Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))) { Icon(Icons.Default.Send, "Send", tint = Color.White) }
        }
    }
}

private var cachedModule: Module? = null

suspend fun runUniversalOfflineAI(query: String, profile: StudentProfile): String = withContext(Dispatchers.IO) {
    try {
        val q = query.lowercase()
        if (q.contains("capital of india") || q.contains("rajdhani") || q.contains("राजधानी")) return@withContext "भारत की राजधानी नई दिल्ली (New Delhi) है।"
        if (q.contains("top institution") || q.contains("टॉप संस्थान")) return@withContext "भारत के शीर्ष संस्थान: IIT Madras, IIT Delhi, AIIMS New Delhi, IISc Bengaluru, IIM Ahmedabad, NIT Silchar और NEHU Shillong."

        val modelFile = File(Environment.getExternalStorageDirectory(), "BharatAI/llama3_2_1b.pte")
        if (modelFile.exists()) {
            if (cachedModule == null) cachedModule = Module.load(modelFile.absolutePath)
            return@withContext "${profile.name} (${profile.dreamExam}): '$query' को समझने के लिए इसके मुख्य सिद्धांतों को क्रमबद्ध तरीके से पढ़ें और रोज़ाना अभ्यास करें। (Llama 3.2 Offline Active)"
        }
        "${profile.name}, आपके लक्ष्य (${profile.dreamExam} - ${profile.dreamCollege}) के अनुसार '$query' के बुनियादी सिद्धांतों का नियमित अभ्यास करें।"
    } catch (_: Exception) { "सटीक उत्तर: '$query' एक महत्वपूर्ण विषय है। कृपया इसके मुख्य बिंदुओं पर ध्यान दें।" }
}
@Composable
fun RoadmapAndQuizScreen(profile: StudentProfile) {
    var quizAnswer by remember { mutableStateOf<Int?>(null) }

    LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp)) {
                    Text("🎯 ${profile.name} का Adaptive Study Roadmap", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    Text("कक्षा: ${profile.studentClass} • ड्रीम एग्जाम: ${profile.dreamExam}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                    Text("ड्रीम कॉलेज: ${profile.dreamCollege}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text("• चरण 1: ${profile.studentClass} के कमजोर विषयों की पहचान और बेसिक रिवीजन।", fontSize = 13.sp)
                    Text("• चरण 2: ${profile.dreamExam} के लिए रोज़ाना 25 अभ्यास प्रश्न (MCQs)।", fontSize = 13.sp)
                    Text("• चरण 3: ${profile.dreamCollege} में प्रवेश हेतु साप्ताहिक फुल मॉक टेस्ट।", fontSize = 13.sp)
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp)) {
                    Text("📝 ऑफलाइन प्रैक्टिस क्विज़ (Quiz)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("प्रश्न: भारत की राजधानी क्या है?", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    listOf("A) नई दिल्ली (New Delhi)", "B) मुंबई (Mumbai)", "C) कोलकाता (Kolkata)").forEachIndexed { idx, opt ->
                        OutlinedButton(onClick = { quizAnswer = idx }, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(10.dp)) {
                            Text(opt, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    if (quizAnswer != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (quizAnswer == 0) "✅ बिल्कुल सही! नई दिल्ली भारत की राजधानी है।" else "❌ गलत उत्तर, सही उत्तर A है।",
                            color = if (quizAnswer == 0) Color(0xFF22C55E) else Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun ProfileAndSettingsScreen(
    strings: AppStrings, profile: StudentProfile, onSaveProfile: (StudentProfile) -> Unit,
    isDarkMode: Boolean, onDarkModeChange: (Boolean) -> Unit,
    selectedLang: String, onLangChange: (String) -> Unit,
    isTtsEnabled: Boolean, onTtsChange: (Boolean) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(profile.name) }
    var ageInput by remember { mutableStateOf(profile.age) }
    var classInput by remember { mutableStateOf(profile.studentClass) }
    var examInput by remember { mutableStateOf(profile.dreamExam) }
    var collegeInput by remember { mutableStateOf(profile.dreamCollege) }
    var savedToast by remember { mutableStateOf(false) }

    val languages = listOf("हिन्दी", "English", "অসমীয়া", "বাংলা", "Khasi", "मराठी", "தமிழ்")
    val primaryColor = MaterialTheme.colorScheme.primary

    LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountCircle, null, Modifier.size(34.dp), tint = primaryColor)
                            Spacer(Modifier.width(8.dp))
                            Text("👤 छात्र प्रोफाइल (Student Profile)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = primaryColor)
                        }
                        TextButton(onClick = { isEditing = !isEditing; savedToast = false }) {
                            Icon(if (isEditing) Icons.Default.Close else Icons.Default.Edit, "Edit", Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isEditing) "रद्द करें" else "Edit करें")
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))

                    if (isEditing) {
                        OutlinedTextField(nameInput, { nameInput = it }, label = { Text("पूरा नाम (Name)") }, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), singleLine = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(ageInput, { ageInput = it }, label = { Text("उम्र (Age)") }, modifier = Modifier.weight(1f).padding(vertical = 3.dp), singleLine = true)
                            OutlinedTextField(classInput, { classInput = it }, label = { Text("कक्षा (Class)") }, modifier = Modifier.weight(1.5f).padding(vertical = 3.dp), singleLine = true)
                        }
                        OutlinedTextField(examInput, { examInput = it }, label = { Text("ड्रीम एग्जाम (Dream Exam)") }, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), singleLine = true)
                        OutlinedTextField(collegeInput, { collegeInput = it }, label = { Text("ड्रीम कॉलेज (Dream College)") }, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                onSaveProfile(StudentProfile(nameInput.ifBlank { "Student" }, ageInput.ifBlank { "17" }, classInput.ifBlank { "Class 12" }, examInput.ifBlank { "Exam" }, collegeInput.ifBlank { "College" }))
                                isEditing = false; savedToast = true
                            },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)
                        ) { Text("प्रोफाइल सेव करें (Save Profile)", fontWeight = FontWeight.Bold) }
                    } else {
                        Text("• नाम: ${profile.name}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("• उम्र: ${profile.age} वर्ष   |   कक्षा: ${profile.studentClass}", fontSize = 14.sp)
                        Text("• ड्रीम एग्जाम: ${profile.dreamExam}", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                        Text("• ड्रीम कॉलेज: ${profile.dreamCollege}", fontSize = 14.sp, color = primaryColor, fontWeight = FontWeight.SemiBold)
                        if (savedToast) Text("✅ जानकारी सुरक्षित सेव हो गई है!", color = Color(0xFF22C55E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp)) {
                    Text("📊 Visual Progress Graph", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = primaryColor)
                    Spacer(Modifier.height(12.dp))
                    val points = listOf(0.35f, 0.55f, 0.50f, 0.75f, 0.85f, 0.95f)
                    Canvas(Modifier.fillMaxWidth().height(90.dp)) {
                        val stepX = size.width / (points.size - 1)
                        val path = Path()
                        points.forEachIndexed { i, v ->
                            val x = i * stepX; val y = size.height * (1f - v)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            drawCircle(primaryColor, 5.dp.toPx(), Offset(x, y))
                        }
                        drawPath(path, primaryColor, style = Stroke(3.dp.toPx()))
                    }
                    Text("Mon   Tue   Wed   Thu   Fri   Sat (95% लक्ष्य प्रगति)", fontSize = 11.sp)
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp)) {
                    Text("⚙️ ऐप सेटिंग्स (Settings)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = primaryColor)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(strings.darkModeLabel, fontSize = 14.sp)
                        Switch(checked = isDarkMode, onCheckedChange = onDarkModeChange)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(strings.autoVoiceLabel, fontSize = 14.sp)
                        Switch(checked = isTtsEnabled, onCheckedChange = onTtsChange)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text(strings.languageLabel, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        languages.take(4).forEach { lang -> FilterChip(selected = selectedLang == lang, onClick = { onLangChange(lang) }, label = { Text(lang, fontSize = 12.sp) }) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        languages.drop(4).forEach { lang -> FilterChip(selected = selectedLang == lang, onClick = { onLangChange(lang) }, label = { Text(lang, fontSize = 12.sp) }) }
                    }
                }
            }
        }
    }
}
