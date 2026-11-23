package com.bharat.ailearning

import android.app.Activity
import android.content.Context
import android.content.Intent
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
            text = "100% Offline • Voice • 22 Languages • School, UG/PG, Health & Sports",
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Scrollable 11 Complete Tabs
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
fun SyllabusGuideScreen(selectedCourse: String) {
    Text("School, UG & PG Complete Syllabus Guide", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    Text("Active Course/Goal: $selectedCourse", fontSize = 12.sp)
    Spacer(modifier = Modifier.height(8.dp))

    val syllabusList = listOf(
        "💻 1. UG/PG Engineering & IT (B.Tech / BCA / M.Tech / MCA):\n• Core Syllabus: Engineering Maths, Data Structures & Algorithms (DSA), Operating Systems, DBMS, Computer Networks, AI/ML, Thermodynamics/Circuits.\n• Next Step: GATE, ISRO/DRDO Scientist, PSU Jobs & Software Placements.",
        "🩺 2. UG/PG Medical & AYUSH (MBBS / BAMS / BHMS / BDS / Nursing / Pharma):\n• MBBS/BDS Syllabus: Anatomy, Physiology, Biochemistry, Pathology, Pharmacology, Microbiology, Medicine & Surgery.\n• BAMS/BHMS Syllabus: Rachana Sharir, Kriya Sharir, Dravyaguna, Kayachikitsa, Organon of Medicine & Materia Medica.\n• Next Step: NEET PG / NEXT, INI-CET, AIAPGET & Govt Medical Officer.",
        "⚖️ 3. UG/PG Law & Judiciary (BA LLB / LLB / LLM):\n• Core Syllabus: Constitutional Law of India, Bharatiya Nyaya Sanhita (BNS), Contract Act, Jurisprudence, Family Law, Property & Corporate Law.\n• Next Step: CLAT PG, Judiciary Exam (PCS-J), UPSC Law & Supreme/High Court Practice.",
        "🔬 4. UG/PG Science, Arts & Commerce (B.Sc/M.Sc, BA/MA, ://b.com, BBA/MBA):\n• Science/Research: Physics, Chemistry, Maths, Life Sciences, Agriculture & Data Science.\n• Arts/Commerce/Mgmt: Economics, Polity, History, Accounting, Finance, Marketing & HR.\n• Next Step: CSIR NET / UGC NET (JRF & Professor), UPSC, CAT, CA/CS & Ph.D.",
        "📚 5. School & Entrance (Class 7th–12th, JEE, NEET, CUET):\n• Core Syllabus: Complete NCERT (Physics, Chemistry, Maths, Biology, Social Science, English & Reasoning) + Previous Year Questions (PYQs)."
    )

    syllabusList.forEach { item ->
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(text = item, modifier = Modifier.padding(10.dp), fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    name: String,
    state: String,
    exam: String,
    lang: String,
    hours: String,
    weak: String,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var tempName by remember { mutableStateOf(name) }
    var tempState by remember { mutableStateOf(state) }
    var tempExam by remember { mutableStateOf(exam) }
    var tempLang by remember { mutableStateOf(lang) }
    var tempHours by remember { mutableStateOf(hours) }
    var tempWeak by remember { mutableStateOf(weak) }
    var examExpanded by remember { mutableStateOf(false) }
    var langExpanded by remember { mutableStateOf(false) }
    var savedMsg by remember { mutableStateOf(false) }

    val allCoursesAndExams = listOf(
        // School
        "School: Class 7th – 8th (CBSE / State Board / NMMS)",
        "School: Class 9th – 10th Board Exam (Matric)",
        "School: Class 11th – 12th (Science / Commerce / Arts)",
        // Entrance Exams
        "Entrance: JEE Main & Advanced (Engineering)",
        "Entrance: NEET UG (MBBS / BAMS / BHMS / BDS)",
        "Entrance: IISER / NISER / NEST (Research)",
        "Entrance: CUET UG / PG (Central Universities)",
        // UG Courses
        "UG: B.Tech / B.E. (Engineering & Technology)",
        "UG: MBBS / BDS (Allopathic Medical)",
        "UG: BAMS / BHMS / BUMS (AYUSH Medical)",
        "UG: B.Sc. Nursing / B.Pharm / Paramedical",
        "UG: BA LLB / LLB (Law)",
        "UG: B.Sc. / B.Sc. Agriculture / BCA",
        "UG: BA / B.Com / BBA",
        // PG Courses
        "PG: M.Tech / MS / MCA (Tech & Engineering)",
        "PG: MD / MS / MDS / AIAPGET (Medical PG)",
        "PG: LLM / Judiciary (PCS-J)",
        "PG: M.Sc. / MA / M.Com / MBA",
        // Govt & Research Exams
        "Govt Exam: UPSC Civil Services (IAS / IPS)",
        "Govt Exam: SSC CGL / CHSL / Banking / NDA",
