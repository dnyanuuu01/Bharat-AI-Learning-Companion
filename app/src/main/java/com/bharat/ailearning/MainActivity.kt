package com.bharat.ailearning

import android.Manifest
import android.app.Activity
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
                                Text("100% Offline • Llama 3.2 (INT4) • ${studentProfile.dreamExam}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
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
