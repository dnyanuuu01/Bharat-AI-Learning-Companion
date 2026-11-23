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

@Composable
fun BharatAITeacherApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("BharatAIPrefs", Context.MODE_PRIVATE) }

    var currentTab by remember { mutableStateOf("Profile") }
    var studentName by remember { mutableStateOf(prefs.getString("name", "") ?: "") }
    var studentState by remember { mutableStateOf(prefs.getString("state", "Meghalaya / All India") ?: "Meghalaya / All India") }
    var selectedExam by remember { mutableStateOf(prefs.getString("exam", "UG: B.Tech / Engineering") ?: "UG: B.Tech / Engineering") }
    var selectedLang by remember { mutableStateOf(prefs.getString("lang", "Hindi (हिंदी)") ?: "Hindi (हिंदी)") }
    var studyHours by remember { mutableStateOf(prefs.getString("hours", "4") ?: "4") }
    var weakSubject by remember { mutableStateOf(prefs.getString("weak", "Core Subjects") ?: "Core Subjects") }
    var quizScore by remember { mutableStateOf(prefs.getInt("score", 0)) }

    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Text("Bharat AI Guru (School • UG • PG)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("100% Offline • Voice & Image • 22 Languages • Study, Health & Sports", fontSize = 11.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Profile", "AI Chat", "Syllabus", "Health", "Timetable", "Scholarships", "Sports", "Colleges", "Formulas", "Quiz", "Progress").forEach { tab ->
                Button(
                    onClick = { currentTab = tab },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (currentTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                ) { Text(tab, fontSize = 12.sp) }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (currentTab) {
                "Profile" -> ProfileScreen(studentName, studentState, selectedExam, selectedLang, studyHours, weakSubject) { n, st, e, l, h, w ->
                    studentName = n; studentState = st; selectedExam = e; selectedLang = l; studyHours = h; weakSubject = w
                    prefs.edit().putString("name", n).putString("state", st).putString("exam", e).putString("lang", l).putString("hours", h).putString("weak", w).apply()
                }
                "AI Chat" -> ChatScreen(selectedExam, selectedLang, weakSubject, studentState)
                "Syllabus" -> SyllabusGuideScreen(selectedExam)
                "Health" -> HealthAndWellnessScreen(selectedExam)
                "Timetable" -> TimetableScreen(studentName, selectedExam, selectedLang, studyHours, weakSubject)
                "Scholarships" -> ScholarshipScreen(selectedExam, studentState)
                "Sports" -> SportsGuideScreen(studentState)
                "Colleges" -> CollegeGuideScreen(selectedExam)
                "Formulas" -> FormulaBankScreen(selectedExam)
                "Quiz" -> QuizAndPyqScreen(selectedExam) { s -> quizScore = s; prefs.edit().putInt("score", s).apply() }
                "Progress" -> ProgressScreen(studentName, studentState, selectedExam, selectedLang, studyHours, weakSubject, quizScore)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(name: String, state: String, exam: String, lang: String, hours: String, weak: String, onSave: (String, String, String, String, String, String) -> Unit) {
    var tempName by remember { mutableStateOf(name) }
    var tempState by remember { mutableStateOf(state) }
    var tempExam by remember { mutableStateOf(exam) }
    var tempLang by remember { mutableStateOf(lang) }
    var tempHours by remember { mutableStateOf(hours) }
    var tempWeak by remember { mutableStateOf(weak) }
    var examExpanded by remember { mutableStateOf(false) }
    var langExpanded by remember { mutableStateOf(false) }
    var savedMsg by remember { mutableStateOf(false) }

    val allCourses = listOf(
        "School: Class 7th–8th (Board / NMMS)", "School: Class 9th–10th Board", "School: Class 11th–12th Board",
        "Entrance: JEE Main & Advanced", "Entrance: NEET UG (MBBS/BAMS/BHMS)", "Entrance: IISER / NISER / CUET",
        "UG: B.Tech / B.E. / BCA", "UG: MBBS / BAMS / BHMS / Nursing", "UG: BA LLB / LLB (Law)",
        "UG: B.Sc / BA / B.Com / BBA", "PG: M.Tech / MCA / M.Sc / MA / MBA", "PG: MD / MS / LLM / Judiciary",
        "Govt & Research: UPSC / SSC / NDA / GATE / UGC-CSIR NET", "Sports & Skills: Khelo India / Coding"
    )
    val languages = listOf(
        "Hindi (हिंदी)", "English", "Assamese (অসমীয়া)", "Bengali (বাংলা)", "Bodo (बड़ो)", "Dogri (डोगरी)",
        "Gujarati (ગુજરાતી)", "Kannada (ಕನ್ನಡ)", "Kashmiri (कॉशुर)", "Konkani (कोंकणी)", "Maithili (मैथिली)",
        "Malayalam (മലയാളം)", "Manipuri (মৈতৈলোন্)", "Marathi (मराठी)", "Nepali (नेपाली)", "Odia (ଓଡ଼ିଆ)",
        "Punjabi (ਪੰਜਾਬੀ)", "Sanskrit (संस्कृतम्)", "Santali (ᱥᱟᱱᱛᱟᱲᱤ)", "Sindhi (सिन्धी)", "Tamil (தமிழ்)",
        "Telugu (తెలుగు)", "Urdu (اردو)"
    )

    Text("Student Profile (School • UG • PG • Govt Exams)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(value = tempName, onValueChange = { tempName = it; savedMsg = false }, label = { Text("Student Name") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(value = tempState, onValueChange = { tempState = it; savedMsg = false }, label = { Text("State / UT (e.g. Meghalaya, UP, Bihar)") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(6.dp))

    ExposedDropdownMenuBox(expanded = examExpanded, onExpandedChange = { examExpanded = !examExpanded }) {
        OutlinedTextField(value = tempExam, onValueChange = {}, readOnly = true, label = { Text("Class / UG / PG Course / Exam") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = examExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = examExpanded, onDismissRequest = { examExpanded = false }) {
            allCourses.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { tempExam = item; examExpanded = false }) }
        }
    }
    Spacer(modifier = Modifier.height(6.dp))

    ExposedDropdownMenuBox(expanded = langExpanded, onExpandedChange = { langExpanded = !langExpanded }) {
        OutlinedTextField(value = tempLang, onValueChange = {}, readOnly = true, label = { Text("Language (संविधान की 22 भाषाएं)") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = langExpanded, onDismissRequest = { langExpanded = false }) {
            languages.forEach { l -> DropdownMenuItem(text = { Text(l) }, onClick = { tempLang = l; langExpanded = false }) }
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(value = tempWeak, onValueChange = { tempWeak = it }, label = { Text("Weak Subject / Sport") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(value = tempHours, onValueChange = { tempHours = it }, label = { Text("Daily Hours") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(10.dp))
    Button(onClick = { onSave(tempName, tempState, tempExam, tempLang, tempHours, tempWeak); savedMsg = true }, modifier = Modifier.fillMaxWidth()) { Text("Save Profile Offline") }
    if (savedMsg) Text("✅ Saved! Open 'Syllabus', 'AI Chat', 'Health' & 'Scholarships'.", fontWeight = FontWeight.Bold)
}

@Composable
fun ChatScreen(exam: String, lang: String, weak: String, state: String) {
    val context = LocalContext.current
    var question by remember { mutableStateOf("") }
    var simpleMode by remember { mutableStateOf(true) }
    var hasImage by remember { mutableStateOf(false) }
    var lastReply by remember { mutableStateOf("नमस्ते! मैं आपका ऑफ़लाइन AI गुरु हूँ। बोलकर, लिखकर या फोटो चुनकर सवाल पूछें।") }
    var chatHistory by remember { mutableStateOf("AI Guru ($lang): $lastReply") }

    val tts = remember {
        var e: TextToSpeech? = null
