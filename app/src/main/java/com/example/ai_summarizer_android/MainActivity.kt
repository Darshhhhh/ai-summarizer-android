package com.example.ai_summarizer_android

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.ai_summarizer_android.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Use Documents/Ollama instead of root sdcard
    private val ollamaDir by lazy {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "Ollama"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Runtime permissions for pre-Android 11
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ), 1
            )
        }

        // Dropdown for summary style
        val styles = listOf("Short", "Medium", "Detailed", "Bullet Points")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, styles)
        binding.styleDropdown.setAdapter(adapter)

        // Word counter
        binding.inputText.addTextChangedListener {
            val words = it.toString().trim().split("\\s+".toRegex()).filter { w -> w.isNotEmpty() }
            binding.wordCount.text = "${words.size} words"
        }

        // Summarize button
        binding.summarizeButton.setOnClickListener {
            val text = binding.inputText.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "Please enter text", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val style = binding.styleDropdown.text.toString().ifEmpty { "Medium" }

            binding.progressBar.visibility = View.VISIBLE
            binding.outputCard.visibility = View.GONE

            sendToOllama("Summarize in $style style:\n$text")
            pollForOutput()
        }

        // Copy summary
        binding.copyButton.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "Summary",
                    binding.outputText.text
                )
            )
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        // Share summary
        binding.shareButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, binding.outputText.text.toString())
            startActivity(Intent.createChooser(intent, "Share Summary"))
        }
    }

    private fun sendToOllama(text: String) {
        if (!ollamaDir.exists()) {
            ollamaDir.mkdirs()
        }

        try {
            val inputFile = File(ollamaDir, "input.txt")
            inputFile.writeText(text)
            Toast.makeText(this, "Wrote to: ${inputFile.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error writing file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun pollForOutput() {
        Thread {
            val outputFile = File(ollamaDir, "output.txt")
            var waited = 0
            while (!outputFile.exists() && waited < 60) { // wait max 60s
                Thread.sleep(1000)
                waited++
            }

            val summary = if (outputFile.exists()) {
                val result = outputFile.readText()
                outputFile.delete()
                result
            } else {
                "Error: No response from Ollama"
            }

            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                binding.outputCard.visibility = View.VISIBLE
                binding.outputText.text = summary
            }
        }.start()
    }
}
