package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SwipeMatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SwipeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSwipe(swipe: SwipeMatchEntity)

    @Update
    suspend fun updateSwipe(swipe: SwipeMatchEntity)

    @Query("SELECT * FROM swipes WHERE fromUserId = :fromId AND toUserId = :toId LIMIT 1")
    suspend fun getSwipe(fromId: String, toId: String): SwipeMatchEntity?

    @Query("SELECT * FROM swipes WHERE (fromUserId = :userId OR toUserId = :userId) AND isMutualMatch = 1")
    fun getMutualMatchesFlow(userId: String): Flow<List<SwipeMatchEntity>>

    @Query("SELECT toUserId FROM swipes WHERE fromUserId = :userId")
    suspend fun getSwipedUserIds(userId: String): List<String>

    @Query("SELECT COUNT(*) FROM swipes WHERE isMutualMatch = 1")
    fun getTotalMutualMatchesCountFlow(): Flow<Int>

    @Query("DELETE FROM swipes WHERE fromUserId = :userId")
    suspend fun deleteSwipesForUser(userId: String)
}
