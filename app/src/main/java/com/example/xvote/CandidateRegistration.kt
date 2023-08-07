package com.example.xvote

import DatabaseHelper
import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*

class CandidateRegistration : AppCompatActivity() {

    private lateinit var manifestoEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var databaseHelper: DatabaseHelper
    private var candidateId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_candidate_registration)

        candidateId = intent.getLongExtra("candidate_id", -1)
        if (candidateId == -1L) {
            Toast.makeText(this, "Invalid candidate ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        databaseHelper = DatabaseHelper(this)
        manifestoEditText = findViewById(R.id.manifestoEditText)
        registerButton = findViewById(R.id.registerButton)

        registerButton.setOnClickListener {
            val manifesto = manifestoEditText.text.toString().trim()
            if (manifesto.isNotEmpty()) {
                val rowsUpdated = updateCandidateManifesto(candidateId, manifesto)
                if (rowsUpdated > 0) {
                    Toast.makeText(this, "Manifesto saved successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Failed to save manifesto", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a manifesto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCandidateManifesto(candidateId: Long, manifesto: String): Int {
        val db = databaseHelper.writableDatabase

        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_CANDIDATE_MANIFESTO, manifesto)
        }

        val whereClause = "${DatabaseHelper.COLUMN_CANDIDATE_ID} = ?"
        val whereArgs = arrayOf(candidateId.toString())

        val rowsUpdated = db.update(DatabaseHelper.TABLE_CANDIDATES, values, whereClause, whereArgs)

        db.close()

        return rowsUpdated
    }
}