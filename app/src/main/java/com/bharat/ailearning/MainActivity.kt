package com.example.bharatailearningcompanion

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
    var studentGoal by remember { mutableStateOf(prefs.getString("goal", "Python Programming") ?: "Python Programming") }
    var studyHours by remember { mutableStateOf(prefs.getString("hours", "2") ?: "2") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Bharat AI Study Teacher (Offline)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 4 Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Profile", "Plan", "AI Chat", "Quiz").forEach { tab ->
                Button(
                    onClick = { currentTab = tab },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(tab, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Screen Content
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (currentTab) {
                "Profile" -> ProfileScreen(
                    name = studentName,
                    goal = studentGoal,
                    hours = studyHours,
                    onSave = { n, g, h ->
                        studentName = n
                        studentGoal = g
                        studyHours = h
                        prefs.edit().putString("name", n).putString("goal", g).putString("hours", h).apply()
                    }
                )
                "Plan" -> PlanScreen(studentName, studentGoal, studyHours)
                "AI Chat" -> ChatScreen(studentGoal)
                "Quiz" -> QuizScreen(studentGoal)
            }
        }
    }
}

@Composable
fun ProfileScreen(name: String, goal: String, hours: String, onSave: (String, String, String) -> Unit) {
    var tempName by remember { mutableStateOf(name) }
    var tempGoal by remember { mutableStateOf(goal) }
    var tempHours by remember { mutableStateOf(hours) }
    var savedMsg by remember { mutableStateOf(false) }

    Text("Student Profile (Offline Storage)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = tempName,
        onValueChange = { tempName = it; savedMsg = false },
        label = { Text("Student Name") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = tempGoal,
        onValueChange = { tempGoal = it; savedMsg = false },
        label = { Text("Study Goal (e.g. Python, GATE)") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = tempHours,
        onValueChange = { tempHours = it; savedMsg = false },
        label = { Text("Daily Study Hours") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))

    Button(onClick = {
        onSave(tempName, tempGoal, tempHours)
        savedMsg = true
    }, modifier = Modifier.fillMaxWidth()) {
        Text("Save Profile Offline")
    }

    if (savedMsg) {
        Spacer(modifier = Modifier.height(8.dp))
        Text("✅ Profile Saved Successfully in Phone!", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun PlanScreen(name: String, goal: String, hours: String) {
    Text("Study Roadmap for ${if (name.isBlank()) "Student" else name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    Text("Goal: $goal ($hours hrs/day)", fontSize = 14.sp)
    Spacer(modifier = Modifier.height(12.dp))

    val weeks = listOf(
        "Week 1: Basics & Core Concepts of $goal",
        "Week 2: Practical Exercises & Problem Solving",
        "Week 3: Advanced Topics & Mini Project",
        "Week 4: Revision & Full Mock Tests"
    )
    weeks.forEach { week ->
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(text = week, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
fun ChatScreen(goal: String) {
    var question by remember { mutableStateOf("") }
    var chatHistory by remember { mutableStateOf("AI Teacher: नमस्ते! मैं आपका ऑफ़लाइन AI टीचर हूँ। $goal से जुड़ा कोई भी सवाल हिंदी या अंग्रेजी में पूछें।") }

    Text("Offline AI Chat Tutor", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    Spacer(modifier = Modifier.height(8.dp))

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = chatHistory, modifier = Modifier.padding(12.dp), fontSize = 14.sp)
    }
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = question,
        onValueChange = { question = it },
        label = { Text("अपना सवाल लिखें...") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))

    Button(onClick = {
        if (question.isNotBlank()) {
            val reply = "\n\nYou: $question\nAI Teacher: [Llama 3.2 Engine Ready] '$question' को समझने के लिए पहले इसके बेसिक सिद्धांतों को देखें। (असली .pte मॉडल जुड़ते ही यहाँ पूरा उत्तर जनरेट होगा)।"
            chatHistory += reply
            question = ""
        }
    }, modifier = Modifier.fillMaxWidth()) {
        Text("Ask AI (Offline)")
    }
}

@Composable
fun QuizScreen(goal: String) {
    var score by remember { mutableStateOf<Int?>(null) }
    var selectedOption by remember { mutableStateOf(-1) }

    Text("Weekly AI Quiz ($goal)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    Spacer(modifier = Modifier.height(8.dp))
    Text("Q1. ऑफ़लाइन AI मॉडल चलाने के लिए कौन-सी फाइल फॉर्मेट इस्तेमाल होती है?")
    Spacer(modifier = Modifier.height(8.dp))

    val options = listOf("1. .mp4", "2. .pte (ExecuTorch)", "3. .jpg", "4. .pdf")
    options.forEachIndexed { index, text ->
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            RadioButton(selected = selectedOption == index, onClick = { selectedOption = index })
            Text(text = text, modifier = Modifier.padding(top = 12.dp))
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = {
        score = if (selectedOption == 1) 1 else 0
    }, modifier = Modifier.fillMaxWidth()) {
        Text("Submit Quiz")
    }

    score?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (it == 1) "🎉 सही जवाब! आपका स्कोर: 1/1" else "❌ गलत जवाब। सही उत्तर: .pte (ExecuTorch)",
            fontWeight = FontWeight.Bold
        )
    }
}
