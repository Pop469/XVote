package com.example.xvote

import DatabaseHelper
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.xvote.CandidateRegistration
import com.example.xvote.Login
import com.example.xvote.R

class StudentRegistration : AppCompatActivity() {
    // Define the views
    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var facultySpinner: Spinner
    private lateinit var registerButton: Button
    private lateinit var registerCandidateButton: Button
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_registration)

        // Initialize the views
        firstNameEditText = findViewById(R.id.firstNameEditText)
        lastNameEditText = findViewById(R.id.lastNameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        facultySpinner = findViewById(R.id.facultySpinner)
        val facultyAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            resources.getStringArray(R.array.faculty_array)
        )
        facultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        facultySpinner.adapter = facultyAdapter
        registerButton = findViewById(R.id.registerButton)
        registerCandidateButton = findViewById(R.id.registerCandidateButton)

        // Initialize the DatabaseHelper
        databaseHelper = DatabaseHelper(this)

        // Set click listener for Register button
        registerButton.setOnClickListener {
            registerUser(false)
        }

        // Set click listener for Register as Candidate button
        registerCandidateButton.setOnClickListener {
            registerUser(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        databaseHelper.close()
    }

    private fun registerUser(isCandidate: Boolean) {
        val firstName = firstNameEditText.text.toString().trim()
        val lastName = lastNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val faculty = facultySpinner.selectedItem.toString()

        // Check if any fields are empty
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = saveUserToDatabase(username, password, false)
        if (userId != -1L) {
            val registrationId = if (isCandidate) {
                saveCandidateToDatabase(userId, firstName, lastName, email, faculty)
            } else {
                saveStudentToDatabase(userId, firstName, lastName, email, faculty)
            }

            if (registrationId != -1L) {
                val message = if (isCandidate) "Candidate registration successful" else "Student registration successful"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                if(isCandidate){
                    val intent = Intent(this, CandidateRegistration::class.java)
                    intent.putExtra("candidate_id", registrationId)
                    startActivity(intent)
                }
                else{
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                }
            } else {
                databaseHelper.deleteUserFromDatabase(userId)
                val errorMessage = if (isCandidate) "Failed to save candidate information" else "Failed to save student information"
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Failed to save user information", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUserToDatabase(username: String, password: String, isAdmin: Boolean): Long {
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_USERNAME, username)
            put(DatabaseHelper.COLUMN_PASSWORD, password)
            put(DatabaseHelper.COLUMN_IS_ADMIN, if (isAdmin) 1 else 0)
        }

        return databaseHelper.writableDatabase.insert(DatabaseHelper.TABLE_USERS, null, values)
    }

    private fun saveStudentToDatabase(
        userId: Long,
        firstName: String,
        lastName: String,
        email: String,
        faculty: String
    ): Long {
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_USER_ID_FK, userId)
            put(DatabaseHelper.COLUMN_NAME, "$firstName $lastName")
            put(DatabaseHelper.COLUMN_STUDENT_EMAIL, email)
            put(DatabaseHelper.COLUMN_STUDENTS_FACULTY_ID_FK, getFacultyId(faculty))
        }

        return databaseHelper.writableDatabase.insert(DatabaseHelper.TABLE_STUDENTS, null, values)
    }

    private fun saveCandidateToDatabase(
        userId: Long,
        firstName: String,
        lastName: String,
        email: String,
        faculty: String
    ): Long {
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_CANDIDATE_USER_ID_FK, userId)
            put(DatabaseHelper.COLUMN_CANDIDATE_NAME, "$firstName $lastName")
            put(DatabaseHelper.COLUMN_CANDIDATE_EMAIL, email)
            put(DatabaseHelper.COLUMN_CANDIDATE_FACULTY_ID_FK, getFacultyId(faculty))
        }

        return databaseHelper.writableDatabase.insert(DatabaseHelper.TABLE_CANDIDATES, null, values)
    }

    @SuppressLint("Range")
    private fun getFacultyId(facultyName: String): Long {
        val selection = "${DatabaseHelper.COLUMN_FACULTY_NAME} = ?"
        val selectionArgs = arrayOf(facultyName)
        val projection = arrayOf(DatabaseHelper.COLUMN_FACULTY_ID)

        val cursor = databaseHelper.readableDatabase.query(
            DatabaseHelper.TABLE_FACULTIES,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        val facultyId: Long = if (cursor.moveToFirst()) {
            cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_FACULTY_ID))
        } else {
            -1L // Invalid faculty ID
        }

        cursor.close()

        return facultyId
    }
}
