package com.sellcallrecording.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val startTime: String,
    val endTime: String,
    val duration: Int,
    val token: String,
    val d_id: String,
    val date: String,
    val status: String,
    val remark: String
)