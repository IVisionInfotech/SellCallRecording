package com.sellcallrecording.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sellcallrecording.data.model.CallResponse
import com.sellcallrecording.data.model.Category
import com.sellcallrecording.data.model.StatusList
import com.sellcallrecording.data.network.RetrofitClient
import com.sellcallrecording.util.Util.LOAD_API_CALL_AGENT_URL
import com.sellcallrecording.util.Util.LOAD_API_CALL_STAUS_LIST_URL
import com.sellcallrecording.util.Util.LOAD_API_CALL_TYPE_DATA_URL
import com.sellcallrecording.util.Util.LOAD_API_CALL_WhatsappStatus_URL
import com.sellcallrecording.util.Util.LOAD_CALL_HISTORY_URL
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
    @Named("token") private val token: String
) : ViewModel() {

    val list = MutableLiveData<List<CallResponse>?>()
    val callType = MutableLiveData<List<Category>?>()
    val statusList = MutableLiveData<List<StatusList>?>()
    val errorMessage = MutableLiveData<String>()
    val errorMsg = MutableLiveData<String>()
    val successMsg = MutableLiveData<String>()
    val errorMsg1 = MutableLiveData<String>()
    val successMsg1 = MutableLiveData<String>()
    val headers = mutableMapOf<String, String>()

    private var currentJob: Job? = null

    fun fetchData(baseUrl: String, endpoint: String, communicationType: String, search: String) {
        currentJob?.cancel()
        headers["X-API-Key"] = token

        currentJob = viewModelScope.launch {
            try {
                val requestData = mapOf(
                    "category_id" to communicationType,
                )
                val response =
                    retrofitClient.getInstance(baseUrl)
                        .postGetData(endpoint, headers = headers, requestData = requestData)

                if (!response.calls.isNullOrEmpty()) {
                    list.postValue(response.calls)
                } else if (response.status == "1") {
                    errorMessage.postValue(response.msg ?: "Unknown error occurred.")
                } else {
                    errorMessage.postValue("No data found or invalid response.")
                }
            } catch (_: CancellationException) {

            } catch (e: HttpException) {
                errorMessage.postValue(
                    "Server error: ${
                        e.response()?.errorBody()?.string() ?: e.message
                    }"
                )
            } catch (e: IOException) {
                errorMessage.postValue("Network error: ${e.localizedMessage ?: "Check your connection."}")
            } catch (e: Exception) {
                errorMessage.postValue("Unexpected error: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun fetchCallTypeData(baseUrl: String) {
        viewModelScope.launch {
            try {

                val response = retrofitClient.getInstance(baseUrl)
                    .postGetData(LOAD_API_CALL_TYPE_DATA_URL, headers = headers)

                if (!response.category_list.isNullOrEmpty()) {
                    callType.postValue(response.category_list)
                }
            } catch (e: HttpException) {
                errorMsg.postValue("Server error: ${e.message}")
            } catch (e: IOException) {
                errorMsg.postValue("Network error: ${e.message}")
            } catch (e: Exception) {
                errorMsg.postValue("Unexpected error: ${e.message}")
            }
        }
    }

    fun fetchStatusListData(baseUrl: String) {
        viewModelScope.launch {
            try {
                headers["Authorization"] = token
                val response = retrofitClient.getInstance(baseUrl)
                    .postGetData(LOAD_API_CALL_STAUS_LIST_URL, headers = headers)

                if (!response.status_list.isNullOrEmpty()) {
                    statusList.postValue(response.status_list)
                }
            } catch (e: HttpException) {
                errorMsg.postValue("Server error: ${e.message}")
            } catch (e: IOException) {
                errorMsg.postValue("Network error: ${e.message}")
            } catch (e: Exception) {
                errorMsg.postValue("Unexpected error: ${e.message}")
            }
        }
    }

    fun agentWhatsappCallStatus(baseUrl: String, d_id: String, status: String) {
        viewModelScope.launch {
            try {
                headers["Authorization"] = token
                val requestData = hashMapOf(
                    "call_id" to d_id,
                    "call_type" to status
                )
                val response = retrofitClient.getInstance(baseUrl)
                    .postGetData(
                        LOAD_API_CALL_WhatsappStatus_URL,
                        requestData = requestData,
                        headers = headers
                    )

                successMsg1.postValue(response.msg)
            } catch (e: HttpException) {
                errorMsg.postValue("Server error: ${e.message}")
            } catch (e: IOException) {
                errorMsg.postValue("Network error: ${e.message}")
            } catch (e: Exception) {
                errorMsg.postValue("Unexpected error: ${e.message}")
            }
        }
    }

    fun uploadRecording(
        baseUrl: String,
        recordingFile: File,
        fileName: String,
        startTime: String,
        endTime: String,
        duration: Int,
        token: String,
        d_id: String,
        date: String,
        status: String,
        remarks: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {

                if (!recordingFile.exists()) {
                    errorMsg1.postValue("Recording file does not exist")
                    return@launch
                }
                headers["Authorization"] = token

                val startDateTime = formatDateTime("$date $startTime")
                val endDateTime = formatDateTime("$date $endTime")

                val audioFilePart = MultipartBody.Part.createFormData(
                    "rec_file", recordingFile.name,
                    RequestBody.create("audio/wav".toMediaTypeOrNull(), recordingFile)
                )

                val data = hashMapOf(
                    "call_id" to RequestBody.create("text/plain".toMediaTypeOrNull(), d_id),
                    "feedback" to RequestBody.create("text/plain".toMediaTypeOrNull(), remarks),
                    "status" to RequestBody.create("text/plain".toMediaTypeOrNull(), status),
                    "call_start_time" to RequestBody.create(
                        "text/plain".toMediaTypeOrNull(),
                        startDateTime
                    ),
                    "call_end_time" to RequestBody.create(
                        "text/plain".toMediaTypeOrNull(),
                        endDateTime
                    ),
                )

                val response = retrofitClient.getInstance(baseUrl)
                    .uploadRecording(LOAD_CALL_HISTORY_URL, audioFilePart, data, headers)

                println("File uploaded successfully: ${response.msg}")
                recordingFile.delete()
                successMsg.postValue(response.msg)
            } catch (e: HttpException) {
                errorMsg1.postValue("Server error: ${e.message}")
            } catch (e: IOException) {
                errorMsg1.postValue("Network error: ${e.message}")
            } catch (e: Exception) {
                errorMsg1.postValue("Unexpected error: ${e.message}")
            }
        }
    }

    fun formatDateTime(input: String): String {
        return try {
            val inputFormat = SimpleDateFormat("d-M-yyyy HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(input)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            input
        }
    }
}
