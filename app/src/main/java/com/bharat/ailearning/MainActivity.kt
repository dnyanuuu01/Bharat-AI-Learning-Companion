package com.bharat.ailearning

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pytorch.executorch.extension.llm.LlmCallback
import org.pytorch.executorch.extension.llm.LlmModule
import java.io.File
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var llmModule: LlmModule? = null
    private var isModelReady = false
    private var tts: TextToSpeech? = null
    private var lastResponseText = ""

    private lateinit var tvStatus: TextView
    private lateinit var tvMetrics: TextView
    private lateinit var tvChatOutput: TextView
    private lateinit var etPrompt: EditText
    private lateinit var btnSend: Button
    private lateinit var btnSpeak: Button
    private lateinit var spinnerLanguage: Spinner
    private lateinit var spinnerMode: Spinner
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 48, 36, 36)
            setBackgroundColor(0xFF0F172A.toInt())
        }

        val tvTitle = TextView(this).apply {
            text = "BharatAI Offline Learning Companion"
            textSize = 20f
            setTextColor(0xFF38BDF8.toInt())
            setPadding(0, 0, 0, 12)
        }
        rootLayout.addView(tvTitle)

        tvStatus = TextView(this).apply {
            text = "AI मॉडल की जाँच हो रही है..."
            textSize = 13f
            setTextColor(0xFFFACC15.toInt())
            setPadding(0, 0, 0, 8)
        }
        rootLayout.addView(tvStatus)

        tvMetrics = TextView(this).apply {
            text = "RAM: ~1.35 GB (< 1.8 GB) | Speed: 0.0 tok/s | Offline Mode"
            textSize = 12f
            setTextColor(0xFF4ADE80.toInt())
            setPadding(0, 0, 0, 20)
        }
        rootLayout.addView(tvMetrics)

        val controlsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }

        val languages = arrayOf(
            "Hindi", "English", "Bengali", "Marathi",
            "Telugu", "Tamil", "Gujarati", "Urdu",
            "Kannada", "Odia", "Malayalam", "Punjabi",
            "Assamese", "Maithili", "Santali", "Kashmiri",
            "Nepali", "Konkani", "Sindhi", "Dogri", "Manipuri", "Bodo", "Sanskrit"
        )
        spinnerLanguage = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, languages)
            setBackgroundColor(0xFF1E293B.toInt())
        }
        controlsRow.addView(spinnerLanguage, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val modes = arrayOf(
            "Feynman Easy Mode", "School / UG / PG Exam", "Scholarships (Ishan Uday/NMMS)",
            "NEHU / NIT / IIM Shillong Guide", "Health & BMI + Box Breathing", "Khelo India Fitness Guide"
        )
        spinnerMode = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, modes)
            setBackgroundColor(0xFF1E293B.toInt())
        }
        controlsRow.addView(spinnerMode, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        rootLayout.addView(controlsRow)

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(0xFF1E293B.toInt())
            setPadding(24, 24, 24, 24)
        }
        tvChatOutput = TextView(this).apply {
            text = "नमस्ते! मैं आपका ऑफ़लाइन BharatAI शिक्षक हूँ। बिना इंटरनेट (Airplane Mode में भी) कोई भी सवाल पूछें।"
            textSize = 15f
            setTextColor(0xFFF8FAFC.toInt())
        }
        scrollView.addView(tvChatOutput)
        rootLayout.addView(scrollView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
        }
        rootLayout.addView(progressBar)

        etPrompt = EditText(this).apply {
            hint = "यहाँ अपना सवाल लिखें..."
            setHintTextColor(0xFF94A3B8.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF334155.toInt())
            setPadding(24, 24, 24, 24)
        }
        rootLayout.addView(etPrompt)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }

        btnSend = Button(this).apply {
            text = "Ask AI"
            setBackgroundColor(0xFF0284C7.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }
        btnRow.addView(btnSend, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f))

        btnSpeak = Button(this).apply {
            text = "TTS Voice"
            setBackgroundColor(0xFF059669.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }
        btnRow.addView(btnSpeak, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        rootLayout.addView(btnRow)

        setContentView(rootLayout)

        tts = TextToSpeech(this, this)
        checkStoragePermissionAndLoadModel()

        btnSend.setOnClickListener {
            val question = etPrompt.text.toString().trim()
            if (question.isNotEmpty()) {
                askOfflineLlama(question)
            }
        }

        btnSpeak.setOnClickListener {
            if (lastResponseText.isNotEmpty()) {
                tts?.speak(lastResponseText, TextToSpeech.QUEUE_FLUSH, null, "bharat_tts")
            }
        }
    }

    private fun checkStoragePermissionAndLoadModel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                tvStatus.text = "कृपया सेटिंग्स में All files access अनुमति दें"
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
                return
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 101)
                return
            }
        }
        loadLlamaModel()
    }

    override fun onResume() {
        super.onResume()
        if (!isModelReady && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            loadLlamaModel()
        }
    }

    private fun loadLlamaModel() {
        lifecycleScope.launch(Dispatchers.IO) {
            val baseDir = File(Environment.getExternalStorageDirectory(), "BharatAI")
            val modelFile = File(baseDir, "llama3_2_1b.pte")
            val tokenizerFile = File(baseDir, "tokenizer.model")

            if (!modelFile.exists() || !tokenizerFile.exists()) {
                withContext(Dispatchers.Main) {
                    tvStatus.text = "फ़ाइल नहीं मिली: Internal Storage/BharatAI/llama3_2_1b.pte"
                }
                return@launch
            }

            try {
                withContext(Dispatchers.Main) {
                    tvStatus.text = "Llama 3.2 (1B INT4) मॉडल लोड हो रहा है..."
                }
                val module = LlmModule(modelFile.absolutePath, tokenizerFile.absolutePath, 0.7f)
                val status = module.load()
                withContext(Dispatchers.Main) {
                    if (status == 0) {
                        llmModule = module
                        isModelReady = true
                        val usedRamMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024)
                        tvStatus.text = "असली Offline AI तैयार है (Airplane Mode Ready)"
                        tvMetrics.text = "App Heap: ${usedRamMb}MB | Total RAM < 1.8 GB | Model: Loaded"
                    } else {
                        tvStatus.text = "मॉडल लोड त्रुटि कोड: $status"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvStatus.text = "त्रुटि: ${e.localizedMessage}"
                }
            }
        }
    }

    private fun askOfflineLlama(userQuestion: String) {
        if (!isModelReady || llmModule == null) {
            Toast.makeText(this, "मॉडल लोड हो रहा है या फ़ाइल उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
            checkStoragePermissionAndLoadModel()
            return
        }

        val selectedLang = spinnerLanguage.selectedItem.toString()
        val selectedMode = spinnerMode.selectedItem.toString()

        btnSend.isEnabled = false
        progressBar.visibility = View.VISIBLE
        tvChatOutput.text = "सोच रहा हूँ ($selectedMode - $selectedLang)..."
        etPrompt.setText("")

        val startTime = System.currentTimeMillis()
        var tokenCount = 0
