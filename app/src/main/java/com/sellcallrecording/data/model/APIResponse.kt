package com.sellcallrecording.data.model

import java.io.Serializable

data class APIResponse(
    val status: String,
    val msg: String,
    val token: String,
    val category_list: List<Category>?,
    val calls: List<CallResponse>?,
    val status_list: List<StatusList>?
) : Serializable

data class CallResponse(
    val call_id: String,
    val user_id: String,
    val user_name: String,
    val M_no: String,
    val category_id: String,
    val category: String,
    val convert_status: String,
    val updated_date: String,
    val feedback: String?,
    val call_history: List<CallHistory>,
    val call_recording: List<CallRecording>
)

data class CallHistory(
    val call_history_id: String,
    val call_id: String,
    val call_status: String,
    val user_id: String,
    val feedback: String?,
    val call_start_time: String,
    val call_end_time: String,
    val duration: String?,
    val date: String
)

data class CallRecording(
    val call_recording_id: String,
    val call_id: String,
    val user_id: String,
    val call_history_id: String,
    val call_recording: String,
    val file_name: String?,
    val date: String
)

data class Category (
    val category_id : String,
    val category: String,
    val tcnt: String,
)

data class StatusList(
    val status_id : String,
    val status: String,
    val complete_stage: String,
)
