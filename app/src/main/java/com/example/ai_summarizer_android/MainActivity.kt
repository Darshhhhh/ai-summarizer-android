package com.example.ai_summarizer_android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.ai_summarizer_android.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val ollamaDir = File(Environment.getExternalStorageDirectory(), "Ollama")

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "Storage permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ask permission if not granted
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        binding.summarizeButton.setOnClickListener {
            val text = binding.inputText.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "Enter text first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.outputCard.visibility = View.GONE

            sendToOllama(text)
            pollForOutput()
        }
    }

    private fun sendToOllama(text: String) {
        ollamaDir.mkdirs()
        val inputFile = File(ollamaDir, "input.txt")
        inputFile.writeText(text)
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
