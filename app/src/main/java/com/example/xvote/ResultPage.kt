package com.example.xvote

import DatabaseHelper
import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xvote.R

class ResultPage : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var resultRecyclerView: RecyclerView
    private lateinit var resultList: List<Result>

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result_page)

        // Initialize views
        resultRecyclerView = findViewById(R.id.resultRecyclerView)

        // Initialize the DatabaseHelper
        databaseHelper = DatabaseHelper(this)

        // Set up RecyclerView
        resultList = getFinalResult()
        val resultAdapter = ResultAdapter(resultList)
        resultRecyclerView.layoutManager = LinearLayoutManager(this)
        resultRecyclerView.adapter = resultAdapter

        val finishButton: Button = findViewById(R.id.finishButton)
        finishButton.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
        }
    }

    @SuppressLint("Range")
    private fun getFinalResult(): List<Result> {
        val resultList = mutableListOf<Result>()

        val db = databaseHelper.readableDatabase

        // Subquery to get the maximum vote count for each faculty
        val subquery =
            "SELECT ${DatabaseHelper.COLUMN_CANDIDATE_FACULTY_ID_FK}, MAX(vote_count) AS max_vote_count " +
                    "FROM (SELECT ${DatabaseHelper.COLUMN_CANDIDATE_FACULTY_ID_FK}, COUNT(*) AS vote_count " +
                    "FROM ${DatabaseHelper.TABLE_VOTES} " +
                    "INNER JOIN ${DatabaseHelper.TABLE_CANDIDATES} " +
                    "ON ${DatabaseHelper.TABLE_VOTES}.${DatabaseHelper.COLUMN_VOTE_CANDIDATE_ID_FK} = " +
                    "${DatabaseHelper.TABLE_CANDIDATES}.${DatabaseHelper.COLUMN_CANDIDATE_ID} " +
                    "GROUP BY ${DatabaseHelper.COLUMN_CANDIDATE_FACULTY_ID_FK}) " +
                    "GROUP BY ${DatabaseHelper.COLUMN_CANDIDATE_FACULTY_ID_FK}"

        val cursor = db.rawQuery(subquery, null)

        val maxVotesMap = mutableMapOf<String, Int>()
        if (cursor.moveToFirst()) {
            do {
                val faculty =
                    cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CANDIDATE_FACULTY_ID_FK))
                val maxVotes = cursor.getInt(cursor.getColumnIndex("max_vote_count"))
                maxVotesMap[faculty] = maxVotes
            } while (cursor.moveToNext())
        }

        cursor.close()

        // Query to retrieve the candidate details with the maximum vote count for each faculty
        val query = "SELECT ${DatabaseHelper.COLUMN_CANDIDATE_FACULTY_ID_FK}, " +
                "${DatabaseHelper.COLUMN_CANDIDATE_NAME}, " +
                "${DatabaseHelper.COLUMN_CANDIDATE_EMAIL}, " +
                "COUNT(*) AS vote_count " +
                "FROM ${DatabaseHelper.TABLE_VOTES} " +
                "INNER JOIN ${DatabaseHelper.TABLE_CANDIDATES} " +
                "ON ${DatabaseHelper.TABLE_VOTES}.${DatabaseHelper.COLUMN_VOTE_CANDIDATE_ID_FK} = " +
                "${DatabaseHelper.TABLE_CANDIDATES}.${DatabaseHelper.COLUMN_CANDIDATE_ID} " +
                "GROUP BY ${DatabaseHelper.COLUMN_CANDIDATE_FACULTY_ID_FK}, " +
                "${DatabaseHelper.COLUMN_CANDIDATE_NAME}, " +
                "${DatabaseHelper.COLUMN_CANDIDATE_EMAIL}"

        val resultCursor = db.rawQuery(query, null)

        if (resultCursor.moveToFirst()) {
            val facultyMaxVotesMap = mutableMapOf<String, Int>()

            // Find the maximum vote count for each faculty
            do {
                val faculty =
                    resultCursor.getString(resultCursor.getColumnIndex(DatabaseHelper.COLUMN_CANDIDATE_FACULTY_ID_FK))
                val voteCount = resultCursor.getInt(resultCursor.getColumnIndex("vote_count"))

                if (!facultyMaxVotesMap.containsKey(faculty) || voteCount > facultyMaxVotesMap[faculty]!!) {
                    // Update the maximum vote count for the faculty
                    facultyMaxVotesMap[faculty] = voteCount
                }
            } while (resultCursor.moveToNext())

            resultCursor.moveToFirst()

            // Retrieve the candidate with the maximum vote count for each faculty
            do {
                val faculty =
                    resultCursor.getString(resultCursor.getColumnIndex(DatabaseHelper.COLUMN_CANDIDATE_FACULTY_ID_FK))
                val name =
                    resultCursor.getString(resultCursor.getColumnIndex(DatabaseHelper.COLUMN_CANDIDATE_NAME))
                val email =
                    resultCursor.getString(resultCursor.getColumnIndex(DatabaseHelper.COLUMN_CANDIDATE_EMAIL))
                val voteCount = resultCursor.getInt(resultCursor.getColumnIndex("vote_count"))
                val maxVotes = facultyMaxVotesMap[faculty]
                val percentage = (voteCount.toDouble() / maxVotesMap[faculty]!!) * 100

                if (voteCount == maxVotes) {
                    val result = Result(faculty, name, email, voteCount, percentage)
                    resultList.add(result)
                }
            } while (resultCursor.moveToNext())
        }

        resultCursor.close()
        db.close()

        return resultList
    }

    private data class Result(
        val faculty: String,
        val candidateName: String,
        val candidateEmail: String,
        val voteCount: Int,
        val votePercentage: Double
    )

    private inner class ResultAdapter(private val resultList: List<Result>) :
        RecyclerView.Adapter<ResultAdapter.ResultViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.result_item, parent, false)
            return ResultViewHolder(view)
        }

        override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
            val result = resultList[position]
            holder.bind(result)
        }

        override fun getItemCount(): Int {
            return resultList.size
        }

        inner class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val facultyTextView: TextView = itemView.findViewById(R.id.facultyTextView)
            private val candidateNameTextView: TextView =
                itemView.findViewById(R.id.candidateNameTextView)
            private val candidateEmailTextView: TextView =
                itemView.findViewById(R.id.candidateEmailTextView)
            private val voteCountTextView: TextView = itemView.findViewById(R.id.voteCountTextView)
            private val votePercentageTextView: TextView =
                itemView.findViewById(R.id.votePercentageTextView)

            fun bind(result: Result) {
                facultyTextView.text = getFacultyName(result.faculty.toLong())
                candidateNameTextView.text = "Name: " + result.candidateName
                candidateEmailTextView.text = "Email: " + result.candidateEmail
                voteCountTextView.text = "Votes: " + result.voteCount.toString()
                votePercentageTextView.text = "(" + String.format("%.2f", result.votePercentage) + "%)"

            }
        }
    }

    private fun getTotalVoteCount(faculty: String): Int {
        val db = databaseHelper.readableDatabase

        val query = "SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_VOTES} " +
                "INNER JOIN ${DatabaseHelper.TABLE_CANDIDATES} " +
                "ON ${DatabaseHelper.TABLE_VOTES}.${DatabaseHelper.COLUMN_VOTE_CANDIDATE_ID_FK} = " +
                "${DatabaseHelper.TABLE_CANDIDATES}.${DatabaseHelper.COLUMN_CANDIDATE_ID} " +
                "WHERE ${DatabaseHelper.TABLE_CANDIDATES}.${DatabaseHelper.COLUMN_CANDIDATE_FACULTY_ID_FK} = ?"

        val selectionArgs = arrayOf(faculty)
        val cursor = db.rawQuery(query, selectionArgs)

        var voteCount = 0
        if (cursor.moveToFirst()) {
            voteCount = cursor.getInt(0)
        }

        cursor.close()
        db.close()

        return voteCount
    }

    @SuppressLint("Range")
    private fun getCandidateDetails(candidateId: Long): Candidate? {
        val db = databaseHelper.readableDatabase

        val query = "SELECT * FROM ${DatabaseHelper.TABLE_CANDIDATES} " +
                "WHERE ${DatabaseHelper.COLUMN_CANDIDATE_ID} = $candidateId"

        val cursor = db.rawQuery(query, null)

        var candidate: Candidate? = null
        if (cursor.moveToFirst()) {
            val faculty = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CANDIDATE_FACULTY_ID_FK))
            val name = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CANDIDATE_NAME))
            val email = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CANDIDATE_EMAIL))

            candidate = Candidate(faculty, name, email)
        }

        cursor.close()
        db.close()

        return candidate
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
        val faculty: String,
        val name: String,
        val email: String
    )
}
