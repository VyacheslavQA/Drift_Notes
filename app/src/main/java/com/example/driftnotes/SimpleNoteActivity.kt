package com.example.driftnotes

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SimpleNoteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_note)

        val editTextNote = findViewById<EditText>(R.id.editTextNote)
        val buttonSave = findViewById<Button>(R.id.buttonSave)

        buttonSave.setOnClickListener {
            val noteText = editTextNote.text.toString().trim()
            if (noteText.isNotEmpty()) {
                Toast.makeText(this, "Заметка: $noteText", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Введите текст заметки", Toast.LENGTH_SHORT).show()
            }
        }
    }
}