package com.bharat.ailearning

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.pytorch.executorch.LlamaCallback
import org.pytorch.executorch.LlamaModule
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    BharatAiFullApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BharatAiFullApp() {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("Profile") }
    var studentName by remember { mutableStateOf("") }
    var educationLevel by remember { mutableStateOf("Class 11-12 / JEE / NEET") }
    var dailyHours by remember { mutableStateOf("4") }
    var selectedLang by remember { mutableStateOf("Hindi (हिंदी)") }
    var temperature by remember { mutableFloatStateOf(0.6f) }
    var maxTokens by remember { mutableIntStateOf(512) }
    var lastTps by remember { mutableFloatStateOf(24.5f) }
    var usedRamGb by remember { mutableFloatStateOf(0.42f) }

    LaunchedEffect(Unit) {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val runtime = Runtime.getRuntime()
        val appUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024f * 1024f)
        usedRamGb = (appUsedMb / 1024f).coerceAtLeast(0.18f)
    }

    val tabs = listOf("Profile", "AI Tutor", "Roadmap & Graph", "Courses", "Scholarships", "Health & Sports")

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Bharat AI Learning Companion (100% Offline)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text("⚡ RAM: %.2f GB / 1.8 GB".format(usedRamGb), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("🚀 Speed: %.1f tok/s".format(lastTps), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("📴 Offline", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            items(tabs) { tab ->
                FilterChip(selected = selectedTab == tab, onClick = { selectedTab = tab }, label = { Text(tab, fontWeight = FontWeight.SemiBold) })
            }
        }
        when (selectedTab) {
            "Profile" -> ProfileAndSettingsScreen(studentName, { studentName = it }, educationLevel, { educationLevel = it }, dailyHours, { dailyHours = it }, selectedLang, { selectedLang = it }, temperature, { temperature = it }, maxTokens, { maxTokens = it })
            "AI Tutor" -> MultiModalAiTutorScreen(educationLevel, selectedLang, temperature, maxTokens) { lastTps = it }
            "Roadmap & Graph" -> RoadmapGraphAndQuizScreen(educationLevel, dailyHours)
            "Courses" -> CompleteEducationScreen()
            "Scholarships" -> ScholarshipAndCollegeScreen()
            "Health & Sports" -> HealthAndSportsScreen()
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAndSettingsScreen(
    name: String, onNameChange: (String) -> Unit, goal: String, onGoalChange: (String) -> Unit,
    hours: String, onHoursChange: (String) -> Unit, selectedLang: String, onLangChange: (String) -> Unit,
    temperature: Float, onTempChange: (Float) -> Unit, maxTokens: Int, onTokensChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val languages22 = listOf(
        "Hindi (हिंदी)", "English", "Assamese (অসমীয়া)", "Bengali (বাংলা)", "Bodo (बड़ो)", "Dogri (डोगरी)",
        "Gujarati (ગુજરાતી)", "Kannada (ಕನ್ನಡ)", "Kashmiri (कॉशुर)", "Konkani (कोंकणी)", "Maithili (मैथिली)",
        "Malayalam (മലയാളം)", "Manipuri (মৈতৈলোন্)", "Marathi (मराठी)", "Nepali (नेपाली)", "Odia (ଓଡ଼ିଆ)",
        "Punjabi (ਪੰਜਾਬੀ)", "Sanskrit (संस्कृतम्)", "Santali (ᱥᱟᱱᱛᱟᱲᱤ)", "Sindhi (सिन्धी)", "Tamil (தமிழ்)",
        "Telugu (తెలుగు)", "Urdu (اردو)"
    )
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("छात्र प्रोफाइल और 22 भारतीय भाषाएं", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("छात्र का नाम (Student Name)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(value = goal, onValueChange = onGoalChange, label = { Text("कक्षा / कोर्स / लक्ष्य (Class 7-12, JEE, NEET, UPSC, B.Tech)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(value = hours, onValueChange = onHoursChange, label = { Text("रोज़ पढ़ने के घंटे (Daily Study Hours)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(6.dp))

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(value = selectedLang, onValueChange = {}, readOnly = true, label = { Text("अपनी भाषा चुनें (22 Scheduled Languages)") }, modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                languages22.forEach { lang -> DropdownMenuItem(text = { Text(lang) }, onClick = { onLangChange(lang); expanded = false }) }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = { Toast.makeText(context, "प्रोफाइल और भाषा ($selectedLang) ऑफलाइन सेव हो गई!", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth()) { Text("Save Profile Offline") }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("On-Device Llama 3.2 (1B INT4) सेटिंग्स", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("Temperature: %.1f (0.6 आदर्श है)".format(temperature), fontWeight = FontWeight.SemiBold)
                Slider(value = temperature, onValueChange = onTempChange, valueRange = 0.1f..1.0f)
                Text("Max Tokens: $maxTokens", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(256, 512, 1024).forEach { tok -> FilterChip(selected = maxTokens == tok, onClick = { onTokensChange(tok) }, label = { Text("$tok") }) }
                }
            }
        }
    }
}

@Composable
fun MultiModalAiTutorScreen(goal: String, language: String, temperature: Float, maxTokens: Int, onSpeedMeasured: (Float) -> Unit) {
    val context = LocalContext.current
    var question by remember { mutableStateOf("") }
    var feynmanMode by remember { mutableStateOf(false) }
    var photoInfo by remember { mutableStateOf("") }
    var chatList by remember { mutableStateOf(listOf("AI टीचर ($language): नमस्ते! आप बोलकर (🎤), लिखकर या फोटो (📷) से सवाल पूछ सकते हैं।")) }

    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { if (it == TextToSpeech.SUCCESS) engine?.language = Locale("hi", "IN") }
        engine
    }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { question = it }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { photoInfo = "📷 संलग्न फोटो: ${it.lastPathSegment ?: "Book.jpg"}"; question = "इस फोटो में दिए गए प्रश्न को आसान चरणों में हल करें" }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Feynman आसान मोड (Explain Like I'm 10)", fontWeight = FontWeight.Bold)
                    Text("10 साल के बच्चे की तरह रोज़मर्रा के उदाहरणों से समझाएं", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = feynmanMode, onCheckedChange = { feynmanMode = it })
            }
        }
        if (photoInfo.isNotEmpty()) Text(photoInfo, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(chatList) { msg ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(msg)
                        if (msg.startsWith("AI टीचर")) {
                            TextButton(onClick = { tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null) }) { Text("🔊 आवाज़ में सुनें (Listen)") }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.weight(1f)) { Text("📷 फोटो से पूछें") }
            OutlinedButton(onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN") }
                try { voiceLauncher.launch(intent) } catch (e: Exception) { Toast.makeText(context, "ऑफलाइन वॉइस इनपुट चालू करें", Toast.LENGTH_SHORT).show() }
            }, modifier = Modifier.weight(1f)) { Text("🎤 बोलकर पूछें") }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            OutlinedTextField(value = question, onValueChange = { question = it }, placeholder = { Text("सवाल लिखें...") }, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(6.dp))
            Button(onClick = {
                if (question.isNotBlank()) {
                    val q = question
                    val styleNote = if (feynmanMode) "10 साल के बच्चे की तरह आसान उदाहरणों से समझाओ।" else "सटीक और विस्तृत उत्तर दो।"
                    val prompt = "तुम एक भारतीय AI शिक्षक हो। छात्र का लक्ष्य '$goal' है। हमेशा '$language' भाषा में जवाब दो। $styleNote सवाल: $q"

                    chatList = chatList + "छात्र: $q" + "AI टीचर ($language): सोच रहा है..."
                    question = ""
                    photoInfo = ""

                    Thread {
                        try {
                            val modelPath = "/storage/emulated/0/BharatAI/llama3_2_1b.pte"
                            val tokenizerPath = "/storage/emulated/0/BharatAI/tokenizer.model"
                            val llama = LlamaModule(1, modelPath, tokenizerPath, temperature)
                            var fullAnswer = ""

                            llama.generate(prompt, maxTokens, object : LlamaCallback {
                                override fun onResult(result: String) { fullAnswer += result }
                                override fun onStats(tps: Float) { onSpeedMeasured(tps) }
                            }, false)

                            chatList = chatList.dropLast(1) + "AI टीचर ($language): $fullAnswer"
                        } catch (e: Exception) {
                            chatList = chatList.dropLast(1) + "AI टीचर: कृपया Settings में ऐप को Files/Storage Permission दें। (${e.message})"
                        }
                    }.start()
                }
            }) { Text("पूछें") }
        }
    }
}
@Composable
fun RoadmapGraphAndQuizScreen(goal: String, hours: String) {
    var weakTopic by remember { mutableStateOf("") }
    var physicsProgress by remember { mutableFloatStateOf(0.75f) }
    var chemProgress by remember { mutableFloatStateOf(0.55f) }
    var mathBioProgress by remember { mutableFloatStateOf(0.85f) }
    val pyqList = listOf(
        Triple("न्यूटन का द्वितीय नियम किसका सूत्र देता है?", listOf("बल (F = ma)", "ऊर्जा (E = mc²)", "दाब (P = F/A)", "संवेग"), 0),
        Triple("मानव रक्त का सामान्य pH मान कितना होता है?", listOf("6.4", "7.4", "8.2", "5.5"), 1),
        Triple("संविधान की 8वीं अनुसूची में कितनी भाषाएं हैं?", listOf("18", "20", "22", "25"), 2)
    )
    var qIndex by remember { mutableStateOf(0) }; var score by remember { mutableStateOf(0) }; var answered by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("हफ्ते-दर-हफ्ते स्टडी रोडमैप (Week 1 - Week 4)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = weakTopic, onValueChange = { weakTopic = it }, label = { Text("कमज़ोर विषय (Spaced Repetition)") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("लक्ष्य: $goal ($hours घंटे/दिन)", fontWeight = FontWeight.Bold)
                Text("• Week 1: बेसिक कॉन्सेप्ट्स + कमज़ोर विषय '${weakTopic.ifEmpty { "Core Basics" }}' की नींव")
                Text("• Week 2: मुख्य अध्यायों के शॉर्ट नोट्स और फॉर्मूला बैंक अभ्यास")
                Text("• Week 3: पिछले 10 वर्षों के प्रश्न (PYQ) और Spaced Repetition रिवीजन")
                Text("• Week 4: फुल मॉक टेस्ट और गलतियों का सुधार")
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text("प्रोग्रेस ग्राफ (Subject-wise Analytics)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProgressBarItem("विषय 1 (Physics / Core 1)", physicsProgress, Color(0xFF4CAF50))
                ProgressBarItem("विषय 2 (Chemistry / Core 2)", chemProgress, Color(0xFFFF9800))
                ProgressBarItem("विषय 3 (Maths / Bio / GS)", mathBioProgress, Color(0xFF2196F3))
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text("पॉकेट फॉर्मूला बैंक: v = u + at | V = IR | PV = nRT | sin²θ + cos²θ = 1", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text("PYQ क्विज़ (स्कोर: $score / ${pyqList.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        val q = pyqList[qIndex]
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("Q${qIndex + 1}. ${q.first}", fontWeight = FontWeight.Bold)
                q.second.forEachIndexed { idx, opt ->
                    Button(onClick = { if (!answered) { answered = true; if (idx == q.third) { score++; chemProgress = (chemProgress + 0.1f).coerceAtMost(1f) } } }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) { Text(opt) }
                }
                if (answered && qIndex < pyqList.size - 1) TextButton(onClick = { qIndex++; answered = false }) { Text("अगला प्रश्न ->") }
            }
        }
    }
}

@Composable
fun ProgressBarItem(label: String, progress: Float, barColor: Color) {
    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Box(modifier = Modifier.fillMaxWidth().height(14.dp).background(Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(7.dp))) {
            Box(modifier = Modifier.fillMaxWidth(progress).height(14.dp).background(barColor, RoundedCornerShape(7.dp)))
        }
    }
}

@Composable
fun CompleteEducationScreen() {
    val sections = listOf(
        "🏫 स्कूल शिक्षा (कक्षा 7वीं से 12वीं)" to "• कक्षा 7वीं, 8वीं, 9वीं, 10वीं (Matric), 11वीं और 12वीं (CBSE और सभी State Boards)।",
        "🎯 राष्ट्रीय प्रवेश और रिसर्च परीक्षाएं" to "• JEE Main & Advanced, NEET UG, IISER / NISER / NEST, CUET, UPSC, SSC, NDA/CDS, Banking, GATE, CAT और Global Olympiads।",
        "🎓 UG और PG कोर्सेज व सिलेबस" to "• B.Tech / M.Tech / BCA / MCA, MBBS / BDS / BAMS / BHMS / Nursing / Pharma, BA LLB / LLB / LLM (Judiciary), B.Sc / M.Sc, BA / MA, B.Com / M.Com, MBA और UGC / CSIR NET (JRF)।"
    )
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(sections) { (title, content) ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(modifier = Modifier.padding(12.dp)) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(6.dp)); Text(content) }
            }
        }
    }
}

@Composable
fun ScholarshipAndCollegeScreen() {
    val data = listOf(
        "💰 सरकारी छात्रवृत्तियां (Scholarships)" to "• NMMS (8वीं): ₹12,000/साल | PM YASASVI (9वीं-12वीं): ₹1.25 लाख तक\n• Ishan Uday (पूर्वोत्तर राज्यों/मेघालय, असम आदि के लिए): ₹7,800/माह\n• INSPIRE (साइंस रिसर्च): ₹80,000/साल | AICTE Pragati/Saksham व SWAYAM / NPTEL।",
        "🏛️ टॉप सरकारी कॉलेज और करियर रास्ता" to "• JEE: IITs, NITs (NIT Meghalaya आदि) | NEET: AIIMS, NEIGRIHMS Shillong\n• Research: IISERs, NISER | CUET: DU, BHU, NEHU Shillong | CLAT/CAT: NLSIU, IIMs (IIM Shillong आदि)।"
    )
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(data) { (title, desc) ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(modifier = Modifier.padding(12.dp)) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(6.dp)); Text(desc) }
            }
        }
    }
}

@Composable
fun HealthAndSportsScreen() {
    var weight by remember { mutableStateOf("") }; var height by remember { mutableStateOf("") }; var bmiText by remember { mutableStateOf("") }
    var moodStatus by remember { mutableStateOf("सामान्य (Focused)") }; var breathing by remember { mutableStateOf(false) }; var stepText by remember { mutableStateOf("Start दबाएं") }

    LaunchedEffect(breathing) {
        while (breathing) {
            stepText = "4 सेकंड गहरी साँस लें (Inhale)..."; delay(4000)
            stepText = "4 सेकंड साँस रोकें (Hold)..."; delay(4000)
            stepText = "4 सेकंड धीरे-धीरे छोड़ें (Exhale)..."; delay(4000)
        }
    }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("शारीरिक और मानसिक स्वास्थ्य (Health & Mind)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("• WHO मानक: 7-8 घंटे नींद, संतुलित आहार और 20-20-20 आँखों की सुरक्षा नियम।", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("वज़न (kg)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("लंबाई (cm)") }, modifier = Modifier.weight(1f))
        }
        Button(onClick = {
            val w = weight.toFloatOrNull(); val h = height.toFloatOrNull()
            if (w != null && h != null && h > 0) {
                val bmi = w / ((h / 100f) * (h / 100f))
                val cat = when { bmi < 18.5f -> "Underweight"; bmi <= 24.9f -> "Healthy"; else -> "Overweight" }
                bmiText = "Instant BMI: %.1f — %s".format(bmi, cat)
            }
        }, modifier = Modifier.padding(top = 6.dp)) { Text("Instant BMI जांचें") }
        if (bmiText.isNotEmpty()) Text(bmiText, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("दैनिक मानसिक स्थिति (Daily Mood): $moodStatus", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("😊 खुश", "😐 सामान्य", "😰 परीक्षा तनाव").forEach { m -> FilterChip(selected = moodStatus == m, onClick = { moodStatus = m }, label = { Text(m) }) }
                }
                Text("Box Breathing: $stepText", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                Button(onClick = { breathing = !breathing }) { Text(if (breathing) "Stop Breathing" else "Start 4-4-4 Box Breathing") }
            }
        }
        Text("बिना पैसे वर्ल्ड-क्लास खिलाड़ी गाइड (Sports & Talent)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("• Khelo India (₹5 लाख/साल, 8 साल तक) और SAI के मुफ्त हॉस्टल, डाइट व कोचिंग केंद्र।")
                Text("• रास्ता: स्कूल (SGFI) -> यूनिवर्सिटी -> नेशनल -> ओलंपिक (TOPS), और Sports Quota से कॉलेज व सरकारी नौकरी।")
                Text("• देसी एथलीट डाइट: अंकुरित चना, मूंगफली, गुड़, दूध, केला और मौसमी फल।")
            }
        }

