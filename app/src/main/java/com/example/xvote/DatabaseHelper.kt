import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "x-vote.db"
        const val DATABASE_VERSION = 1

        // Users table and columns
        const val TABLE_USERS = "Users"
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_USERNAME = "username"
        const val COLUMN_PASSWORD = "password"
        const val COLUMN_IS_ADMIN = "is_admin"

        // Students table and columns
        const val TABLE_STUDENTS = "Students"
        const val COLUMN_STUDENT_ID = "student_id"
        const val COLUMN_USER_ID_FK = "user_id_fk"
        const val COLUMN_NAME = "name"
        const val COLUMN_STUDENT_EMAIL = "student_email"
        const val COLUMN_STUDENTS_FACULTY_ID_FK = "student_faculty"

        // Candidates table and columns
        const val TABLE_CANDIDATES = "Candidates"
        const val COLUMN_CANDIDATE_ID = "candidate_id"
        const val COLUMN_CANDIDATE_USER_ID_FK = "user_id_fk"
        const val COLUMN_CANDIDATE_NAME = "name"
        const val COLUMN_CANDIDATE_EMAIL = "candidate_email"
        const val COLUMN_CANDIDATE_MANIFESTO = "candidate_manifesto"
        const val COLUMN_CANDIDATE_FACULTY_ID_FK = "candidate_faculty"

        // Faculties table and columns
        const val TABLE_FACULTIES = "Faculties"
        const val COLUMN_FACULTY_ID = "faculty_id"
        const val COLUMN_FACULTY_NAME = "faculty_name"

        // CandidateApprovals table and columns
        const val TABLE_CANDIDATE_APPROVALS = "CandidateApprovals"
        const val COLUMN_APPROVAL_ID = "candidate_approval_id"
        const val COLUMN_CANDIDATE_ID_FK = "candidate_id_fk"
        const val COLUMN_FACULTY_ID_FK = "faculty_id_fk"

        // Votes table and columns
        const val TABLE_VOTES = "Votes"
        const val COLUMN_VOTE_ID = "vote_id"
        const val COLUMN_VOTE_STUDENT_ID_FK = "student_id_fk"
        const val COLUMN_VOTE_CANDIDATE_ID_FK = "candidate_id_fk"

    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create Users table
        val createUsersTableQuery = "CREATE TABLE $TABLE_USERS (" +
                "$COLUMN_USER_ID INTEGER PRIMARY KEY," +
                "$COLUMN_USERNAME TEXT," +
                "$COLUMN_PASSWORD TEXT," +
                "$COLUMN_IS_ADMIN INTEGER)"

        db.execSQL(createUsersTableQuery)

        // Create Students table
        val createStudentsTableQuery = "CREATE TABLE $TABLE_STUDENTS (" +
                "$COLUMN_STUDENT_ID INTEGER PRIMARY KEY," +
                "$COLUMN_USER_ID_FK INTEGER," +
                "$COLUMN_NAME TEXT," +
                "$COLUMN_STUDENT_EMAIL TEXT," +
                "$COLUMN_STUDENTS_FACULTY_ID_FK INTEGER," +
                "FOREIGN KEY($COLUMN_USER_ID_FK) REFERENCES $TABLE_USERS($COLUMN_USER_ID)," +
                "FOREIGN KEY($COLUMN_STUDENTS_FACULTY_ID_FK) REFERENCES $TABLE_FACULTIES($COLUMN_FACULTY_ID))"

        db.execSQL(createStudentsTableQuery)

        // Create Candidates table
        val createCandidatesTableQuery = "CREATE TABLE $TABLE_CANDIDATES (" +
                "$COLUMN_CANDIDATE_ID INTEGER PRIMARY KEY," +
                "$COLUMN_CANDIDATE_USER_ID_FK INTEGER," +
                "$COLUMN_CANDIDATE_NAME TEXT," +
                "$COLUMN_CANDIDATE_EMAIL TEXT," +
                "$COLUMN_CANDIDATE_MANIFESTO TEXT," +
                "$COLUMN_CANDIDATE_FACULTY_ID_FK INTEGER," +
                "FOREIGN KEY($COLUMN_CANDIDATE_USER_ID_FK) REFERENCES $TABLE_USERS($COLUMN_USER_ID)," +
                "FOREIGN KEY($COLUMN_CANDIDATE_FACULTY_ID_FK) REFERENCES $TABLE_FACULTIES($COLUMN_FACULTY_ID))"

        db.execSQL(createCandidatesTableQuery)

        // Create Faculties table
        val createFacultiesTableQuery = "CREATE TABLE $TABLE_FACULTIES (" +
                "$COLUMN_FACULTY_ID INTEGER PRIMARY KEY," +
                "$COLUMN_FACULTY_NAME TEXT)"

        db.execSQL(createFacultiesTableQuery)

        // Create CandidateApprovals table
        val createCandidateApprovalsTableQuery = "CREATE TABLE $TABLE_CANDIDATE_APPROVALS (" +
                "$COLUMN_APPROVAL_ID INTEGER PRIMARY KEY," +
                "$COLUMN_CANDIDATE_ID_FK INTEGER," +
                "$COLUMN_FACULTY_ID_FK INTEGER," +
                "FOREIGN KEY($COLUMN_CANDIDATE_ID_FK) REFERENCES $TABLE_CANDIDATES($COLUMN_CANDIDATE_ID)," +
                "FOREIGN KEY($COLUMN_FACULTY_ID_FK) REFERENCES $TABLE_FACULTIES($COLUMN_FACULTY_ID))"

        db.execSQL(createCandidateApprovalsTableQuery)

        // Create Votes table
        val createVotesTableQuery = "CREATE TABLE $TABLE_VOTES (" +
                "$COLUMN_VOTE_ID INTEGER PRIMARY KEY," +
                "$COLUMN_VOTE_STUDENT_ID_FK INTEGER," +
                "$COLUMN_VOTE_CANDIDATE_ID_FK INTEGER," +
                "FOREIGN KEY($COLUMN_VOTE_STUDENT_ID_FK) REFERENCES $TABLE_STUDENTS($COLUMN_STUDENT_ID)," +
                "FOREIGN KEY($COLUMN_VOTE_CANDIDATE_ID_FK) REFERENCES $TABLE_CANDIDATES($COLUMN_CANDIDATE_ID))"

        db.execSQL(createVotesTableQuery)

    }

    fun populateFacultyTable(intent: Context) {
        val faculties = arrayOf(
            "School of Economics and Management",
            "School of Humanities and Communication",
            "School of Energy and Chemical Engineering",
            "China-ASEAN College of Marine Sciences",
            "School of Electrical and Computer Engineering",
            "School of Traditional Chinese Medicine",
            "School of Mathematics and Physics",
            "School of Foundation Studies"
        )

        val dbHelper = DatabaseHelper(intent)
        val db = dbHelper.writableDatabase

        // Insert faculty entries into the table
        for (faculty in faculties) {
            val values = ContentValues().apply {
                put(COLUMN_FACULTY_NAME, faculty)
            }
            db.insert(TABLE_FACULTIES, null, values)
        }

        db.close()
    }

    fun populateUserTable(intent: Context) {
        val adminUsername = "admin"
        val adminPassword = "12345"

        val dbHelper = DatabaseHelper(intent)
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(COLUMN_USERNAME, adminUsername)
            put(COLUMN_PASSWORD, adminPassword)
            put(COLUMN_IS_ADMIN, 1) // Set isAdmin to 1 for admin user
        }

        db.insert(TABLE_USERS, null, values)

        db.close()
    }

    fun deleteUserFromDatabase(userId: Long): Int {
        val db = writableDatabase

        val whereClause = "$COLUMN_USER_ID = ?"
        val whereArgs = arrayOf(userId.toString())

        val deletedRows = db.delete(TABLE_USERS, whereClause, whereArgs)
        db.close()

        return deletedRows
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle any database upgrade logic here
    }

    fun resetDatabase(intent: Context) {
        val dbHelper = DatabaseHelper(intent)
        val db = dbHelper.writableDatabase

        // Perform any necessary actions to reset the database
        // For example, you can delete all tables and recreate them

        // Delete all tables
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STUDENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CANDIDATES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FACULTIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CANDIDATE_APPROVALS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_VOTES")

        // Recreate the tables by calling the onCreate method
        dbHelper.onCreate(db)

        db.close()

        // Display a message to indicate that the database has been reset
        Toast.makeText(intent, "Database reset successful", Toast.LENGTH_SHORT).show()
    }
}