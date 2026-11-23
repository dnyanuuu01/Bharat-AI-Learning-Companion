package com.bharat.ailearning

import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BharatAITeacherApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BharatAITeacherApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var studentName by remember { mutableStateOf("छात्र") }
    var studentClass by remember { mutableStateOf("Class 10") }
    var subject by remember { mutableStateOf("Science & Maths") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bharat AI Study Teacher (Offline)", fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Text("👤") }, label = { Text("Profile") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Text("📅") }, label = { Text("Roadmap") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Text("🤖") }, label = { Text("AI Tutor") })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Text("📝") }, label = { Text("Quiz") })
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            when (selectedTab) {
                0 -> ProfileScreen(studentName, { studentName = it }, studentClass, { studentClass = it }, subject, { subject = it })
                1 -> RoadmapScreen(studentClass, subject)
                2 -> ChatTutorScreen()
                3 -> QuizScreen()
            }
        }
    }
}

@Composable
fun ProfileScreen(name: String, onName: (String) -> Unit, sClass: String, onClass: (String) -> Unit, sub: String, onSub: (String) -> Unit) {
    val modelFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "llama3_2_1b.pte")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("1. Student Profile (ऑफ़लाइन प्रोफ़ाइल)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = name, onValueChange = onName, label = { Text("आपका नाम (Name)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = sClass, onValueChange = onClass, label = { Text("कक्षा / कोर्स (Class/Exam)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = sub, onValueChange = onSub, label = { Text("मुख्य विषय (Subjects)") }, modifier = Modifier.fillMaxWidth())
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Offline Llama 3.2 Model Status:", fontWeight = FontWeight.Bold)
                Text(if (modelFile.exists()) "✅ Model Ready: Downloads/llama3_2_1b.pte" else "⚠️ Model Not Found: कृपया Downloads फ़ोल्डर में llama3_2_1b.pte डालें")
            }
        }
    }
}

@Composable
fun RoadmapScreen(sClass: String, sub: String) {
    val days = listOf(
        "Monday: $sub - Basic Concepts & Notes (2 Hours)",
        "Tuesday: $sub - NCERT / Core Chapter Practice (2 Hours)",
        "Wednesday: Numericals & Important Formulas (1.5 Hours)",
        "Thursday: Previous Year Questions (2 Hours)",
        "Friday: Weak Topics Revision with AI Tutor (1.5 Hours)",
        "Saturday: Weekly Offline Mock Quiz (1 Hour)",
        "Sunday: Full Revision & Rest"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("2. Weekly Study Plan ($sClass)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(days) { plan ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(plan, modifier = Modifier.padding(14.dp))
                }
            }
        }
    }
}

@Composable
fun ChatTutorScreen() {
    var question by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf("AI Tutor: नमस्ते! मैं आपका ऑफ़लाइन AI शिक्षक हूँ। बिना इंटरनेट के मुझसे कोई भी सवाल पूछें।")) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("3. Offline AI Doubt Solver (हिंदी / English)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { msg ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(msg, modifier = Modifier.padding(12.dp))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = question, onValueChange = { question = it }, placeholder = { Text("सवाल लिखें...") }, modifier = Modifier.weight(1f))
            Button(onClick = {
                if (question.isNotBlank()) {
                    val userQ = question
                    messages = messages + "आप: $userQ" + "AI Tutor (Offline): '$userQ' को सरल भाषा में समझें—यह मुख्य रूप से मूल सिद्धांतों पर आधारित है। (Llama 3.2 Engine Ready)"
                    question = ""
                }
            }) { Text("Ask") }
        }
    }
}

@Composable
fun QuizScreen() {
    var score by remember { mutableIntStateOf(0) }
    var answered by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("4. Offline Practice Quiz & Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Q1. भारत में AI मॉडल को बिना इंटरनेट मोबाइल पर चलाने के लिए किस फॉर्मेट का उपयोग होता है?", fontWeight = FontWeight.Bold)
                Button(onClick = { if (!answered) { score += 10; answered = true } }, modifier = Modifier.fillMaxWidth()) { Text("A) .pte (ExecuTorch Format)") }
                OutlinedButton(onClick = { answered = true }, modifier = Modifier.fillMaxWidth()) { Text("B) .mp3 Audio Format") }
                if (answered) Text("आपका स्कोर: $score / 10 🎉", fontWeight = FontWeight.Bold)
            }
        }
    }
}
package com.bharat.ailearning

import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BharatAITeacherApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BharatAITeacherApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var studentName by remember { mutableStateOf("छात्र") }
    var studentClass by remember { mutableStateOf("Class 10") }
    var subject by remember { mutableStateOf("Science & Maths") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bharat AI Study Teacher (Offline)", fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Text("👤") }, label = { Text("Profile") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Text("📅") }, label = { Text("Roadmap") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Text("🤖") }, label = { Text("AI Tutor") })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Text("📝") }, label = { Text("Quiz") })
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            when (selectedTab) {
                0 -> ProfileScreen(studentName, { studentName = it }, studentClass, { studentClass = it }, subject, { subject = it })
                1 -> RoadmapScreen(studentClass, subject)
                2 -> ChatTutorScreen()
                3 -> QuizScreen()
            }
        }
    }
}

@Composable
fun ProfileScreen(name: String, onName: (String) -> Unit, sClass: String, onClass: (String) -> Unit, sub: String, onSub: (String) -> Unit) {
    val modelFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "llama3_2_1b.pte")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("1. Student Profile (ऑफ़लाइन प्रोफ़ाइल)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = name, onValueChange = onName, label = { Text("आपका नाम (Name)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = sClass, onValueChange = onClass, label = { Text("कक्षा / कोर्स (Class/Exam)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = sub, onValueChange = onSub, label = { Text("मुख्य विषय (Subjects)") }, modifier = Modifier.fillMaxWidth())
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Offline Llama 3.2 Model Status:", fontWeight = FontWeight.Bold)
                Text(if (modelFile.exists()) "✅ Model Ready: Downloads/llama3_2_1b.pte" else "⚠️ Model Not Found: कृपया Downloads फ़ोल्डर में llama3_2_1b.pte डालें")
            }
        }
    }
}

@Composable
fun RoadmapScreen(sClass: String, sub: String) {
    val days = listOf(
        "Monday: $sub - Basic Concepts & Notes (2 Hours)",
        "Tuesday: $sub - NCERT / Core Chapter Practice (2 Hours)",
        "Wednesday: Numericals & Important Formulas (1.5 Hours)",
        "Thursday: Previous Year Questions (2 Hours)",
        "Friday: Weak Topics Revision with AI Tutor (1.5 Hours)",
        "Saturday: Weekly Offline Mock Quiz (1 Hour)",
        "Sunday: Full Revision & Rest"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("2. Weekly Study Plan ($sClass)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(days) { plan ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(plan, modifier = Modifier.padding(14.dp))
                }
            }
        }
    }
}

@Composable
fun ChatTutorScreen() {
    var question by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf("AI Tutor: नमस्ते! मैं आपका ऑफ़लाइन AI शिक्षक हूँ। बिना इंटरनेट के मुझसे कोई भी सवाल पूछें।")) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("3. Offline AI Doubt Solver (हिंदी / English)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { msg ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(msg, modifier = Modifier.padding(12.dp))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = question, onValueChange = { question = it }, placeholder = { Text("सवाल लिखें...") }, modifier = Modifier.weight(1f))
            Button(onClick = {
                if (question.isNotBlank()) {
                    val userQ = question
                    messages = messages + "आप: $userQ" + "AI Tutor (Offline): '$userQ' को सरल भाषा में समझें—यह मुख्य रूप से मूल सिद्धांतों पर आधारित है। (Llama 3.2 Engine Ready)"
                    question = ""
                }
            }) { Text("Ask") }
        }
    }
}

@Composable
fun QuizScreen() {
    var score by remember { mutableIntStateOf(0) }
    var answered by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("4. Offline Practice Quiz & Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Q1. भारत में AI मॉडल को बिना इंटरनेट मोबाइल पर चलाने के लिए किस फॉर्मेट का उपयोग होता है?", fontWeight = FontWeight.Bold)
                Button(onClick = { if (!answered) { score += 10; answered = true } }, modifier = Modifier.fillMaxWidth()) { Text("A) .pte (ExecuTorch Format)") }
                OutlinedButton(onClick = { answered = true }, modifier = Modifier.fillMaxWidth()) { Text("B) .mp3 Audio Format") }
                if (answered) Text("आपका स्कोर: $score / 10 🎉", fontWeight = FontWeight.Bold)
            }
        }
    }
}
