package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE isSuspended = 0 AND isVisibleInSparks = 1 ORDER BY name ASC")
    fun getAllActiveUsersFlow(): Flow<List<UserEntity>>

    @Query("UPDATE users SET isVisibleInSparks = :visible WHERE id = :userId")
    suspend fun setSparksVisibility(userId: String, visible: Boolean)

    @Query("UPDATE users SET showBadges = :showBadges WHERE id = :userId")
    suspend fun setShowBadges(userId: String, showBadges: Boolean)

    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsersForAdminFlow(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isSuspended = :isSuspended WHERE id = :userId")
    suspend fun setSuspension(userId: String, isSuspended: Boolean)

    @Query("UPDATE users SET isVerified = :isVerified WHERE id = :userId")
    suspend fun setVerification(userId: String, isVerified: Boolean)

    @Query("UPDATE users SET warningCount = warningCount + 1 WHERE id = :userId")
    suspend fun incrementWarning(userId: String)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}
