package com.sellcallrecording.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RecordingDao {
    @Insert
    suspend fun insertRecording(recording: Recording)

    @Query("SELECT * FROM recordings")
    suspend fun getAllRecordings(): List<Recording>

    @Delete
    suspend fun deleteRecording(recording: Recording)
}
