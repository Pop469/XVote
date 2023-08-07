package com.example.xvote

import DatabaseHelper
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog

class AdminDashboard : AppCompatActivity() {

    private lateinit var voteCountTextView: TextView
    private lateinit var candidateListView: ListView
    private lateinit var generateResultsButton: Button
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        // Initialize views
        voteCountTextView = findViewById(R.id.voteCountTextView)
        candidateListView = findViewById(R.id.candidateListView)
        generateResultsButton = findViewById(R.id.generateResultsButton)

        // Initialize the DatabaseHelper
        databaseHelper = DatabaseHelper(this)

        // Set click listener for the Generate Results button
        generateResultsButton.setOnClickListener {
            showConfirmationDialog()
        }

        updateVoteCount()
        // Populate the list of pending candidates
        populatePendingCandidatesList()
    }

    private fun updateVoteCount() {
        val voteCount = getVoteCount()
        voteCountTextView.text = "Votes Casted: $voteCount"
    }

    private fun getVoteCount(): Int {
        val db = databaseHelper.readableDatabase

        val query = "SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_VOTES}"
        val cursor = db.rawQuery(query, null)

        var voteCount = 0
        if (cursor.moveToFirst()) {
            voteCount = cursor.getInt(0)
        }

        cursor.close()
        db.close()

        return voteCount
    }

    private fun populatePendingCandidatesList() {
        val pendingCandidatesList = getPendingCandidates()
        val adapter = PendingCandidatesAdapter(this, pendingCandidatesList)
        candidateListView.adapter = adapter
    }

    @SuppressLint("Range")
    private fun getPendingCandidates(): List<Candidate> {
        val db = databaseHelper.readableDatabase
        val pendingCandidatesList = mutableListOf<Candidate>()

        val query = "SELECT * FROM ${DatabaseHelper.TABLE_CANDIDATES} " +
                "LEFT JOIN ${DatabaseHelper.TABLE_CANDIDATE_APPROVALS} " +
                "ON ${DatabaseHelper.TABLE_CANDIDATES}.${DatabaseHelper.COLUMN_CANDIDATE_ID} = " +
                "${DatabaseHelper.TABLE_CANDIDATE_APPROVALS}.${DatabaseHelper.COLUMN_CANDIDATE_ID_FK} " +
                "WHERE ${DatabaseHelper.TABLE_CANDIDATE_APPROVALS}.${DatabaseHelper.COLUMN_CANDIDATE_ID_FK} IS NULL"

        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                val candidateId = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_CANDIDATE_ID))
                val name = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CANDIDATE_NAME))
                val facultyId = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CANDIDATE_FACULTY_ID_FK))
                val manifesto = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CANDIDATE_MANIFESTO))

                val candidate = Candidate(candidateId, name, facultyId, manifesto)
                pendingCandidatesList.add(candidate)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return pendingCandidatesList
    }

    private fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Vote End")
            .setMessage("Are you sure you want to end vote?")
            .setPositiveButton("Yes") { dialog, which -> endVoting() }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun endVoting() {
        val endIndicator = 999
        val userId = endIndicator.toLong()
        val candidateId = endIndicator.toLong()

        val db = databaseHelper.writableDatabase

        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_VOTE_STUDENT_ID_FK, userId)
            put(DatabaseHelper.COLUMN_VOTE_CANDIDATE_ID_FK, candidateId)
        }

        db.insert(DatabaseHelper.TABLE_VOTES, null, values)
    }

    inner class PendingCandidatesAdapter(context: Context, private val candidates: List<Candidate>) :
        ArrayAdapter<Candidate>(context, 0, candidates) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val itemView = convertView ?: LayoutInflater.from(context).inflate(
                R.layout.candidate_list_item,
                parent,
                false
            )

            val candidate = candidates[position]

            val candidateNameTextView = itemView.findViewById<TextView>(R.id.candidateNameTextView)
            val candidateFacultyTextView = itemView.findViewById<TextView>(R.id.candidateFacultyTextView)
            val candidateManifestoTextView = itemView.findViewById<TextView>(R.id.candidateManifestoTextView)
            val approveButton = itemView.findViewById<Button>(R.id.approveButton)
            val rejectButton = itemView.findViewById<Button>(R.id.rejectButton)

            candidateNameTextView.text = candidate.name
            candidateFacultyTextView.text = getFacultyName(candidate.facultyId.toLong())
            candidateManifestoTextView.text = candidate.manifesto

            approveButton.setOnClickListener {
                approveCandidate(candidate)
            }

            rejectButton.setOnClickListener {
                rejectCandidate(candidate)
            }

            return itemView
        }
    }

    private fun approveCandidate(candidate: Candidate) {
        val db = databaseHelper.writableDatabase

        // Insert the approval record in the CandidateApprovals table
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_CANDIDATE_ID_FK, candidate.candidateId)
            put(DatabaseHelper.COLUMN_FACULTY_ID_FK, candidate.facultyId)
        }
        db.insert(DatabaseHelper.TABLE_CANDIDATE_APPROVALS, null, values)

        Toast.makeText(this, "Candidate ${candidate.name} has been approved.", Toast.LENGTH_SHORT).show()

        // Refresh the list of pending candidates
        populatePendingCandidatesList()
    }

    private fun rejectCandidate(candidate: Candidate) {
        val db = databaseHelper.writableDatabase

        // Delete the candidate's account and related records from the database
        db.delete(
            DatabaseHelper.TABLE_CANDIDATES,
            "${DatabaseHelper.COLUMN_CANDIDATE_ID} = ?",
            arrayOf(candidate.candidateId.toString())
        )

        Toast.makeText(this, "Candidate ${candidate.name} has been rejected.", Toast.LENGTH_SHORT).show()

        // Refresh the list of pending candidates
        populatePendingCandidatesList()
    }

    @SuppressLint("Range")
    private fun getFacultyName(facultyId: Long): String? {
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
            null // Faculty name not found or invalid faculty ID
        }

        cursor.close()
        db.close()

        return facultyName
    }

    data class Candidate(
        val candidateId: Long,
        val name: String,
        val facultyId: String,
        val manifesto: String
    )
}
