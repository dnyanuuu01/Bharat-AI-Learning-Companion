package com.bharat.ailearning

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BharatAITeacherApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BharatAITeacherApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("BharatAIPrefs", Context.MODE_PRIVATE) }

    var currentTab by remember { mutableStateOf("Profile") }
    var studentName by remember { mutableStateOf(prefs.getString("name", "") ?: "") }
    var studentState by remember { mutableStateOf(prefs.getString("state", "Meghalaya / All India") ?: "Meghalaya / All India") }
    var selectedExam by remember { mutableStateOf(prefs.getString("exam", "UG: B.Tech / B.E. (Engineering)") ?: "UG: B.Tech / B.E. (Engineering)") }
    var selectedLang by remember { mutableStateOf(prefs.getString("lang", "Hindi (हिंदी)") ?: "Hindi (हिंदी)") }
    var studyHours by remember { mutableStateOf(prefs.getString("hours", "4") ?: "4") }
    var weakSubject by remember { mutableStateOf(prefs.getString("weak", "Core Subjects / Coding") ?: "Core Subjects / Coding") }
    var quizScore by remember { mutableStateOf(prefs.getInt("score", 0)) }

    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Text(
            text = "Bharat AI Guru (School • UG • PG)",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "100% Offline • Voice & Image • 22 Languages • Study, Health & Sports",
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Profile", "AI Chat", "UG/PG Syllabus", "Health & Mind", "Timetable", "Scholarships", "Sports", "Colleges", "Formulas", "PYQ & Quiz", "Progress").forEach { tab ->
                Button(
                    onClick = { currentTab = tab },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(tab, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (currentTab) {
                "Profile" -> ProfileScreen(
                    name = studentName,
                    state = studentState,
                    exam = selectedExam,
                    lang = selectedLang,
                    hours = studyHours,
                    weak = weakSubject,
                    onSave = { n, st, e, l, h, w ->
                        studentName = n
                        studentState = st
                        selectedExam = e
                        selectedLang = l
                        studyHours = h
                        weakSubject = w
                        prefs.edit()
                            .putString("name", n)
                            .putString("state", st)
                            .putString("exam", e)
                            .putString("lang", l)
                            .putString("hours", h)
                            .putString("weak", w)
                            .apply()
                    }
                )
                "AI Chat" -> ChatScreen(selectedExam, selectedLang, weakSubject, studentState)
                "UG/PG Syllabus" -> SyllabusGuideScreen(selectedExam)
                "Health & Mind" -> HealthAndWellnessScreen(selectedExam)
                "Timetable" -> TimetableScreen(studentName, selectedExam, selectedLang, studyHours, weakSubject)
                "Scholarships" -> ScholarshipScreen(selectedExam, studentState)
                "Sports" -> SportsGuideScreen(studentState)
                "Colleges" -> CollegeGuideScreen(selectedExam)
                "Formulas" -> FormulaBankScreen(selectedExam)
                "PYQ & Quiz" -> QuizAndPyqScreen(selectedExam, onScoreUpdate = { newScore ->
                    quizScore = newScore
                    prefs.edit().putInt("score", newScore).apply()
                })
                "Progress" -> ProgressScreen(studentName, studentState, selectedExam, selectedLang, studyHours, weakSubject, quizScore)
            }
        }
    }
}

@Composable
fun ChatScreen(exam: String, lang: String, weak: String, state: String) {
    val context = LocalContext.current
    var question by remember { mutableStateOf("") }
    var simpleMode by remember { mutableStateOf(true) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var lastAiReply by remember {
        mutableStateOf("नमस्ते! मैं आपका ऑफ़लाइन AI गुरु हूँ। आप बोलकर, लिखकर या किताब/सवाल की फोटो चुनकर अपना डाउट पूछ सकते हैं।")
    }
    var chatHistory by remember { mutableStateOf("AI Guru ($lang): $lastAiReply") }

    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = when {
                    lang.contains("English") -> Locale.ENGLISH
                    lang.contains("Bengali") -> Locale("bn", "IN")
                    lang.contains("Marathi") -> Locale("mr", "IN")
                    lang.contains("Tamil") -> Locale("ta", "IN")
                    lang.contains("Telugu") -> Locale("te", "IN")
                    lang.contains("Gujarati") -> Locale("gu", "IN")
                    else -> Locale("hi", "IN")
                }
                engine?.language = locale
            }
        }
        engine
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                question = matches[0]
            }
        }
    }

    // Offline Gallery Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            chatHistory += "\n\n📷 [Offline Image Attached]: सवाल की फोटो लोड हो गई है। कृपया नीचे इस सवाल का टॉपिक या मुख्य शब्द लिखें/बोलें ताकि AI हल दे सके।"
        }
    }

    Text("Offline AI Voice, Image & Text Tutor", fontWeight = FontWeight.Bold, fontSize = 15.sp)
    Text("Language: $lang | Course: $exam", fontSize = 12.sp)

    Row(modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = simpleMode, onCheckedChange = { simpleMode = it })
        Text("Explain Like I'm 10 (आसान भाषा में समझाएं)", modifier = Modifier.padding(top = 12.dp), fontSize = 12.sp)
    }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = chatHistory, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    tts?.speak(lastAiReply, TextToSpeech.QUEUE_FLUSH, null, null)
                }) {
                    Text("🔊 सुनें", fontSize = 12.sp)
                }
                OutlinedButton(onClick = { tts?.stop() }) {
                    Text("⏹️ रोकें", fontSize = 12.sp)
                }
                OutlinedButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Text("📷 फोटो चुनें", fontSize = 12.sp)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = question,
        onValueChange = { question = it },
        label = { Text("बोलें या अपना सवाल लिखें ($lang)...") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
