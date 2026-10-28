package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters
import com.example.data.dao.AuditDao
import com.example.data.dao.ChatDao
import com.example.data.dao.ReportDao
import com.example.data.dao.SwipeDao
import com.example.data.dao.UserDao
import com.example.data.dao.UserProfileDao
import com.example.data.model.AuditLogEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ReportEntity
import com.example.data.model.SwipeMatchEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        UserProfile::class,
        SwipeMatchEntity::class,
        ChatMessageEntity::class,
        ReportEntity::class,
        AuditLogEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun swipeDao(): SwipeDao
    abstract fun chatDao(): ChatDao
    abstract fun reportDao(): ReportDao
    abstract fun auditDao(): AuditDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rset_sparks_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseInitCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseInitCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val instance = INSTANCE ?: return@launch
                    // Initialize Campus Safety Directorate Administrator account for u2504040@rajagiri.edu.in
                    val adminOfficer = UserEntity(
                        id = "admin_safety_officer",
                        email = "u2504040@rajagiri.edu.in",
                        name = "Mohammed Mishal M",
                        age = 22,
                        department = "Campus Safety Directorate",
                        year = 2024,
                        bio = "Official Rajagiri Campus Moderation and Student Welfare Coordinator.",
                        interests = "Campus Safety, Student Affairs, Ethics",
                        avatarColorIndex = 8,
                        avatarEmoji = "🎓",
                        isVerified = true,
                        isAdmin = true,
                        isVisibleInSparks = false,
                        showBadges = false
                    )
                    instance.userDao().insertUser(adminOfficer)

                    instance.auditDao().insertAuditLog(
                        AuditLogEntity(
                            id = "audit_init",
                            adminEmail = "u2504040@rajagiri.edu.in",
                            action = "PLATFORM_INITIALIZED",
                            targetUserId = "system",
                            targetUserEmail = "campus@rajagiri.edu.in",
                            reason = "RSETSparks production campus network initialized with verified @rajagiri.edu.in domain policy.",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }
}
