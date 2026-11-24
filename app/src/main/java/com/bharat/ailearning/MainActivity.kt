package com.bharat.ailearning

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pytorch.executorch.LlamaCallback
import org.pytorch.executorch.LlamaModule
import java.io.File

class MainActivity : AppCompatActivity() {

    private var llamaModule: LlamaModule? = null
    private var isGenerating = false

    private val modelPath = "/storage/emulated/0/BharatAI/llama3_2_1b.pte"
    private val tokenizerPath = "/storage/emulated/0/BharatAI/tokenizer.model"

    private lateinit var promptInput: EditText
    private lateinit var sendButton: Button
    private lateinit var responseText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        promptInput = findViewById(R.id.promptInput)
        sendButton = findViewById(R.id.sendButton)
        responseText = findViewById(R.id.responseText)

        checkAndRequestStoragePermission()

        sendButton.setOnClickListener {
            val userPrompt = promptInput.text.toString().trim()
            if (userPrompt.isEmpty()) {
                Toast.makeText(this, "कृपया कोई सवाल या टॉपिक लिखें", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (llamaModule == null) {
                Toast.makeText(this, "AI मॉडल अभी लोड हो रहा है या फाइल नहीं मिली!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isGenerating) {
                generateResponse(userPrompt)
            }
        }
    }

    private fun checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                responseText.text = "⚠️ कृपया अगली स्क्रीन पर 'All Files Access' की अनुमति दें ताकि ऐप AI मॉडल पढ़ सके..."
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, 101)
            } else {
                loadOfflineLlamaModel()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 102)
            } else {
                loadOfflineLlamaModel()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (llamaModule == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            loadOfflineLlamaModel()
        }
    }

    private fun loadOfflineLlamaModel() {
        responseText.text = "⏳ ऑफलाइन AI मॉडल लोड हो रहा है, कृपया प्रतीक्षा करें..."
        
        lifecycleScope.launch(Dispatchers.IO) {
            val modelFile = File(modelPath)
            val tokenizerFile = File(tokenizerPath)

            if (modelFile.exists() && tokenizerFile.exists()) {
                try {
                    llamaModule = LlamaModule(1, modelPath, tokenizerPath, 0.7f)
                    llamaModule?.load()

                    withContext(Dispatchers.Main) {
                        responseText.text = "✅ ऑफलाइन AI ट्यूटर तैयार है! (100% Offline)\nअपना सवाल या स्टडी प्लान पूछें।"
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        responseText.text = "❌ मॉडल लोड करने में त्रुटि: ${e.message}"
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    responseText.text = "❌ मॉडल फाइल नहीं मिली!\nकृपया फोन की मेमोरी में 'BharatAI' फोल्डर बनाकर उसमें 'llama3_2_1b.pte' और 'tokenizer.model' रखें।"
                }
            }
        }
    }

    private fun generateResponse(userPrompt: String) {
        isGenerating = true
        sendButton.isEnabled = false
        responseText.text = "🤖 सोच रहा हूँ...\n\n"

        lifecycleScope.launch(Dispatchers.IO) {
            val systemInstruction = "You are a helpful offline AI learning companion for Indian students. Reply clearly in the same language (Hindi or English) as the user."
            val formattedPrompt = "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n$systemInstruction<|eot_id|><|start_header_id|>user<|end_header_id|>\n\n$userPrompt<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"

            var isFirstToken = true

            llamaModule?.generate(formattedPrompt, object : LlamaCallback {
                override fun onResult(result: String) {
                    runOnUiThread {
                        if (isFirstToken) {
                            responseText.text = ""
                            isFirstToken = false
                        }
                        responseText.append(result)
                    }
                }

                override fun onStats(tps: Float) {
                    runOnUiThread {
                        responseText.append("\n\n----------------------------------\n⚡ Speed: ${String.format("%.2f", tps)} tokens/sec")
                        isGenerating = false
                        sendButton.isEnabled = true
                    }
                }
            })
        }
    }
}
