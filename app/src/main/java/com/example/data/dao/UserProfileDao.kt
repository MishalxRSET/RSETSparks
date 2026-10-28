package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for local caching and synchronization of student UserProfiles with Firestore.
 */
@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): UserProfile?

    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    fun getProfileFlow(id: String): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getProfileByEmail(email: String): UserProfile?

    @Query("SELECT * FROM user_profiles ORDER BY updatedAt DESC")
    fun getAllCachedProfilesFlow(): Flow<List<UserProfile>>

    @Query("SELECT * FROM user_profiles WHERE isSyncedWithFirestore = 0")
    suspend fun getPendingSyncProfiles(): List<UserProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Update
    suspend fun updateProfile(profile: UserProfile)

    @Query("UPDATE user_profiles SET isSyncedWithFirestore = :isSynced WHERE id = :id")
    suspend fun markSyncStatus(id: String, isSynced: Boolean)

    @Query("DELETE FROM user_profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)

    @Query("DELETE FROM user_profiles")
    suspend fun clearAllProfiles()
}
