import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pytorch.executorch.LlmModule
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var llmModule: LlmModule? = null
    private var tts: TextToSpeech? = null
    private val modelPath = "/storage/emulated/0/BharatAI/llama3_2_1b.pte"
    private val tokenizerPath = "/storage/emulated/0/BharatAI/tokenizer.model"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        requestStoragePermission()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                BharatAIApp()
            }
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("hi", "IN")
        }
    }

    @Composable
    fun BharatAIApp() {
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("AI Chat", "Edu & Exam", "Scholarships", "Health", "Khelo India", "Status")
        var modelStatus by remember { mutableStateOf("मॉडल लोड हो रहा है...") }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                try {
                    val mFile = File(modelPath)
                    val tFile = File(tokenizerPath)
                    if (mFile.exists() && tFile.exists()) {
                        llmModule = LlmModule(modelPath, tokenizerPath, 0.7f)
                        modelStatus = "✅ असली Llama AI तैयार (100% ऑफलाइन)"
                    } else {
                        modelStatus = "⚠️ फ़ाइल नहीं मिली: Internal Storage -> BharatAI फोल्डर चेक करें"
                    }
                } catch (e: Exception) {
                    modelStatus = "❌ मॉडल एरर: ${e.message}"
                }
            }
        }

        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(Color(0xFF1E1E2E)).padding(12.dp)) {
                    Text("🇮🇳 BharatAI (Offline Edge AI)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(modelStatus, fontSize = 12.sp, color = Color(0xFFA6E3A1))
                }
            },
            bottomBar = {
                ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 4.dp) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 12.sp) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                when (selectedTab) {
                    0 -> ChatScreen(scope)
                    1 -> EduScreen()
                    2 -> ScholarshipScreen()
                    3 -> HealthScreen()
                    4 -> KheloIndiaScreen()
                    5 -> StatusScreen()
                }
            }
        }
    }

    @Composable
    fun ChatScreen(scope: kotlinx.coroutines.CoroutineScope) {
        var prompt by remember { mutableStateOf("") }
        var response by remember { mutableStateOf("नमस्ते! मैं BharatAI हूँ। Airplane Mode में भी मुझसे कोई भी सवाल पूछें।") }
        var feynmanMode by remember { mutableStateOf(false) }
        var isGenerating by remember { mutableStateOf(false) }
        var tokSpeed by remember { mutableStateOf("0.0 tok/s") }

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Feynman आसान मोड: ", fontWeight = FontWeight.Bold)
                Switch(checked = feynmanMode, onCheckedChange = { feynmanMode = it })
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("यहाँ अपना सवाल लिखें...") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(
                    onClick = {
                        if (prompt.isNotBlank() && !isGenerating) {
                            isGenerating = true
                            response = "सोच रहा हूँ (Offline AI)..."
                            val finalPrompt = if (feymanMode) {
                                "Explain in very simple terms like to a 10 year old: $prompt"
                            } else {
                                prompt
                            }
                            scope.launch(Dispatchers.IO) {
                                val startTime = System.currentTimeMillis()
                                val sb = StringBuilder()
                                try {
                                    if (llmModule != null) {
                                        var tokenCount = 0
                                        llmModule?.generate(finalPrompt, 128) { token ->
                                            sb.append(token)
                                            tokenCount++
                                            response = sb.toString()
                                        }
                                        val sec = (System.currentTimeMillis() - startTime) / 1000.0
                                        if (sec > 0) tokSpeed = String.format("%.1f tok/s", tokenCount / sec)
                                    } else {
                                        response = "कृपया सुनिश्चित करें कि BharatAI फोल्डर में मॉडल फाइलें मौजूद हैं।"
                                    }
                                } catch (e: Exception) {
                                    response = "Error: ${e.message}"
                                }
                                isGenerating = false
                            }
                        }
                    },
                    enabled = !isGenerating
                ) {
                    Text(if (isGenerating) "जवाब बन रहा है..." else "पूछें (Ask AI)")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = { tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, null) }) {
                    Text("🔊 सुनें (TTS)")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("स्पीड: $tokSpeed", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(response, fontSize = 16.sp)
                }
            }
        }
    }

    @Composable
    fun EduScreen() {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text("📚 School / UG / PG / Competitive Exams", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("• Class 10 & 12 NCERT Quick Revision\n• CUET, JEE, NEET, GATE & UPSC Notes\n• AI Chat टैब में 'Feynman Mode' ऑन करके कठिन टॉपिक को कहानी की तरह समझें।")
        }
    }

    @Composable
    fun ScholarshipScreen() {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text("🎓 छात्रवृत्तियाँ व सरकारी कॉलेज (North-East & India)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("प्रमुख छात्रवृत्तियाँ:\n• Ishan Uday Special Scholarship (NER छात्रों के लिए)\n• NMMS (National Means-cum-Merit Scholarship)\n• NSP Post-Matric & Pre-Matric Schemes\n\nप्रमुख संस्थान:\n• NEHU Shillong (North-Eastern Hill University)\n• NIT Meghalaya & IIM Shillong\n• NEIGRIHMS Shillong")
        }
    }

    @Composable
    fun HealthScreen() {
        var weight by remember { mutableStateOf("") }
        var height by remember { mutableStateOf("") }
        var bmiResult by remember { mutableStateOf("") }


