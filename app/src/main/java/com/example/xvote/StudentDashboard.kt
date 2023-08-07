package com.example.xvote

import DatabaseHelper
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog

class StudentDashboard : AppCompatActivity() {

    private lateinit var voteLayout: LinearLayout
    private lateinit var resultLayout: LinearLayout
    private lateinit var titleTextView: TextView
    private lateinit var candidateListView: ListView
    private lateinit var resultTitleTextView: TextView
    private lateinit var databaseHelper: DatabaseHelper
    private var hasVoted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_dashboard)

        // Initialize views
        voteLayout = findViewById(R.id.voteLayout)
        titleTextView = findViewById(R.id.titleTextView)
        candidateListView = findViewById(R.id.candidateListView)

        // Initialize DatabaseHelper
        databaseHelper = DatabaseHelper(this)

        if (isVotingEnded()) {
            Toast.makeText(this, "Voting has ended. Redirecting to Result...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, ResultPage::class.java)
            startActivity(intent)
            finish() // Finish the current activity to prevent going back to it
        }

        populateCandidateByFaculty()
    }

    @SuppressLint("Range")
    private fun isVotingEnded(): Boolean {
        val db = databaseHelper.readableDatabase

        // Query the last entry in the Vote Table
        val query = "SELECT * FROM ${DatabaseHelper.TABLE_VOTES} ORDER BY ${DatabaseHelper.COLUMN_VOTE_ID} DESC LIMIT 1"
        val cursor = db.rawQuery(query, null)

        val isEnded = if (cursor.moveToFirst()) {
            val voteStudentId = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_VOTE_STUDENT_ID_FK))
            val voteCandidateId = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_VOTE_CANDIDATE_ID_FK))
            voteStudentId == 999L && voteCandidateId == 999L
        } else {
            // Vote Table is empty, voting has not ended
            false
        }

        cursor.close()
        db.close()

        return isEnded
    }

    private fun populateCandidateByFaculty(){
        val facultyId = getFacultyId()
        val candidateList = getCandidates(facultyId)
        val adapter = CandidatesAdapter(this, candidateList)
        candidateListView.adapter = adapter
    }

    @SuppressLint("Range")
    private fun getFacultyId(): String {
        val userId = getCurrentUserId().toString()

        var faculty = ""
        val databaseHelper = DatabaseHelper(this)
        val db = databaseHelper.readableDatabase

        val queryStudents = "SELECT ${DatabaseHelper.COLUMN_STUDENTS_FACULTY_ID_FK} " +
                "FROM ${DatabaseHelper.TABLE_STUDENTS} " +
                "WHERE ${DatabaseHelper.COLUMN_USER_ID_FK} = ?"

        val queryCandidates = "SELECT ${DatabaseHelper.COLUMN_CANDIDATE_FACULTY_ID_FK} " +
                "FROM ${DatabaseHelper.TABLE_CANDIDATES} " +
                "WHERE ${DatabaseHelper.COLUMN_CANDIDATE_USER_ID_FK} = ?"

        val selectionArgs = arrayOf(userId)

        val cursorStudents = db.rawQuery(queryStudents, selectionArgs)
        if (cursorStudents.moveToFirst()) {
            faculty = cursorStudents.getString(cursorStudents.getColumnIndex(DatabaseHelper.COLUMN_STUDENTS_FACULTY_ID_FK))
        }
        cursorStudents.close()

        if (faculty == "") {
            val cursorCandidates = db.rawQuery(queryCandidates, selectionArgs)
            if (cursorCandidates.moveToFirst()) {
                faculty = cursorCandidates.getString(cursorCandidates.getColumnIndex(DatabaseHelper.COLUMN_CANDIDATE_FACULTY_ID_FK))
            }
            cursorCandidates.close()
        }

        db.close()

        return faculty
    }


    @SuppressLint("Range")
    private fun getCandidates(faculty: String): List<Candidate> {
        val db = databaseHelper.readableDatabase
        val candidatesList = mutableListOf<Candidate>()

        val query = "SELECT * FROM ${DatabaseHelper.TABLE_CANDIDATES} " +
                "WHERE ${DatabaseHelper.TABLE_CANDIDATES}.${DatabaseHelper.COLUMN_CANDIDATE_ID} IN " +
                "(SELECT ${DatabaseHelper.COLUMN_CANDIDATE_ID_FK} FROM ${DatabaseHelper.TABLE_CANDIDATE_APPROVALS}) " +
                "AND ${DatabaseHelper.TABLE_CANDIDATES}.${DatabaseHelper.COLUMN_CANDIDATE_FACULTY_ID_FK} = ?"

        val selectionArgs = arrayOf(faculty)
        val cursor = db.rawQuery(query, selectionArgs)

        if (cursor.moveToFirst()) {
            do {
                val candidateId = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_CANDIDATE_ID))
                val name = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CANDIDATE_NAME))
                val facultyId = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CANDIDATE_FACULTY_ID_FK))
                val manifesto = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CANDIDATE_MANIFESTO))

                val candidate = Candidate(candidateId, name, facultyId, manifesto)
                candidatesList.add(candidate)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return candidatesList
    }


    inner class CandidatesAdapter(context: Context, private val candidates: List<StudentDashboard.Candidate>) :
        ArrayAdapter<Candidate>(context, 0, candidates) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val itemView = convertView ?: LayoutInflater.from(context).inflate(
                R.layout.candidate_vote_list_item,
                parent,
                false
            )

            val candidate = candidates[position]

            val candidateNameTextView = itemView.findViewById<TextView>(R.id.candidateNameTextView)
            val candidateFacultyTextView = itemView.findViewById<TextView>(R.id.candidateFacultyTextView)
            val candidateManifestoTextView = itemView.findViewById<TextView>(R.id.candidateManifestoTextView)
            val voteButton = itemView.findViewById<Button>(R.id.voteButton)

            candidateNameTextView.text = candidate.name
            candidateFacultyTextView.text = getFacultyName(candidate.facultyId.toLong())
            candidateManifestoTextView.text = candidate.manifesto

            voteButton.setOnClickListener {
                if(hasVoteRecord(getCurrentUserId()))
                {
                    showVotedDialog()
                }
                else {
                    showConfirmationDialog(candidate)
                }
            }
            return itemView
        }
    }

    private fun showVotedDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
            .setMessage("You have voted previously. A student can only vote once.")
            .setNegativeButton("Understand", null)
            .create()
            .show()
    }

    private fun showConfirmationDialog(candidate: Candidate) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Vote")
            .setMessage("Are you sure you want to vote for ${candidate.name}?")
            .setPositiveButton("Vote") { dialog, which -> castVote(candidate) }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun castVote(candidate: Candidate) {
        val userId = getCurrentUserId() // Replace with your own method to get the current user ID
        val candidateId = candidate.candidateId
        val voteId = saveVoteToDatabase(userId, candidateId)

        if (voteId != -1L) {
            hasVoted = true
            //showResultLayout()
            Toast.makeText(this, "Vote casted successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to cast vote. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveVoteToDatabase(userId: Long, candidateId: Long): Long {
        val db = databaseHelper.writableDatabase

        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_VOTE_STUDENT_ID_FK, userId)
            put(DatabaseHelper.COLUMN_VOTE_CANDIDATE_ID_FK, candidateId)
        }

        return db.insert(DatabaseHelper.TABLE_VOTES, null, values)
    }

    private fun hasVoteRecord(userId: Long): Boolean {
        val db = databaseHelper.readableDatabase

        val selection =
            "${DatabaseHelper.COLUMN_VOTE_STUDENT_ID_FK} = ? AND ${DatabaseHelper.COLUMN_VOTE_CANDIDATE_ID_FK} IS NOT NULL"
        val selectionArgs = arrayOf(userId.toString())

        val cursor = db.query(
            DatabaseHelper.TABLE_VOTES,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        val hasVote = cursor.count > 0

        cursor.close()
        db.close()

        return hasVote
    }

    private fun getCurrentUserId(): Long {
        return intent.getLongExtra("user_id", -1L)
    }

    @SuppressLint("Range")
    private fun getFacultyName(facultyId: Long): String {
        val db = databaseHelper.readableDatabase

        val selection = "${DatabaseHelper.COLUMN_FACULTY_ID} = ?"
        val selectionArgs = arrayOf(facultyId.toString())
        val projection = arrayOf(DatabaseHelper.COLUMN_FACULTY_NAME)

        val cursor = db.query(
            DatabaseHelper.TABLE_FACULTIES,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        val facultyName: String? = if (cursor.moveToFirst()) {
            cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_FACULTY_NAME))
        } else {
            null
        }

        cursor.close()
        db.close()

        return facultyName.toString()
    }

    data class Candidate(
        val candidateId: Long,
        val name: String,
        val facultyId: String,
        val manifesto: String
    )
}