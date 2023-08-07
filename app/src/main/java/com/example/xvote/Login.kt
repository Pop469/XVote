package com.example.xvote

import DatabaseHelper
import DatabaseHelper.Companion.COLUMN_IS_ADMIN
import DatabaseHelper.Companion.COLUMN_PASSWORD
import DatabaseHelper.Companion.COLUMN_USERNAME
import DatabaseHelper.Companion.COLUMN_USER_ID
import DatabaseHelper.Companion.TABLE_USERS
import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class Login : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        databaseHelper = DatabaseHelper(this)

        //RESETS DATABASE, UNCOMMENT WITH CAUTION:
        //databaseHelper.resetDatabase(this)

        //POPULATES DATABASE, UNCOMMENT WITH CAUTION:
        databaseHelper.populateFacultyTable(this)
        databaseHelper.populateUserTable(this)

        val loginButton: Button = findViewById(R.id.loginButton)
        loginButton.setOnClickListener {
            login()
        }

        val registerTextView: TextView = findViewById(R.id.registerTextView)
        registerTextView.setOnClickListener {
            startActivity(Intent(this, StudentRegistration::class.java))
        }
    }

    private fun login() {
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        // Query the database to find the user with the entered username and password
        val userId = findUserByUsernameAndPassword(username, password)

        if (userId != null) {
            val isAdmin = checkIfUserIsAdmin(userId)

            if (isAdmin) {
                // Redirect to admin dashboard
                val intent = Intent(this, AdminDashboard::class.java)
                intent.putExtra("user_id", userId)
                startActivity(intent)
            } else {
                // Redirect to student dashboard
                val intent = Intent(this, StudentDashboard::class.java)
                intent.putExtra("user_id", userId)
                startActivity(intent)
            }
        } else {
            // No user found or incorrect username/password
            Toast.makeText(this, "Incorrect username or password", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("Range")
    fun findUserByUsernameAndPassword(username: String, password: String): Long? {
        val db = databaseHelper.readableDatabase

        val columns = arrayOf(COLUMN_USER_ID)

        val selection = "$COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?"
        val selectionArgs = arrayOf(username, password)

        val cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null)

        var userId: Long? = null

        if (cursor.moveToFirst()) {
            userId = cursor.getLong(cursor.getColumnIndex(COLUMN_USER_ID))
        }

        cursor.close()
        db.close()

        return userId
    }

    @SuppressLint("Range")
    fun checkIfUserIsAdmin(userId: Long): Boolean {
        val db = databaseHelper.readableDatabase

        val columns = arrayOf(COLUMN_IS_ADMIN)

        val selection = "$COLUMN_USER_ID = ?"
        val selectionArgs = arrayOf(userId.toString())

        val cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null)

        var isAdmin = false

        if (cursor.moveToFirst()) {
            isAdmin = cursor.getInt(cursor.getColumnIndex(COLUMN_IS_ADMIN)) == 1
        }

        cursor.close()
        db.close()

        return isAdmin
    }
}