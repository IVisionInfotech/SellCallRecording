package com.sellcallrecording.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sellcallrecording.data.model.CallType
import com.sellcallrecording.data.network.RetrofitClient
import com.sellcallrecording.util.Util.LOAD_API_CALL_AGENT_URL
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
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.cancellation.CancellationException


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
    @Named("token") private val token: String
) : ViewModel() {

    val list = MutableLiveData<List<CallType>?>()
    val callType = MutableLiveData<List<CallType>?>()
    val errorMessage = MutableLiveData<String>()
    val errorMsg = MutableLiveData<String>()
    val successMsg = MutableLiveData<String>()
    val errorMsg1 = MutableLiveData<String>()
    val successMsg1 = MutableLiveData<String>()
    val headers = mutableMapOf<String, String>()

    private var currentJob: Job? = null

    fun fetchData(baseUrl: String, endpoint: String, communicationType: String, search: String) {
        currentJob?.cancel()
        headers["Authorization"] = token

        currentJob = viewModelScope.launch {
            try {

                val response =
                    retrofitClient.getInstance(baseUrl).postGetData(endpoint, headers = headers)

                if (!response.ctype.isNullOrEmpty()) {
                    list.postValue(response.ctype)
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

                if (!response.ctype.isNullOrEmpty()) {
                    callType.postValue(response.ctype)
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

    fun agentCallStatus(baseUrl: String) {
        viewModelScope.launch {
            try {
                val requestData = hashMapOf("token" to token)
                val response = retrofitClient.getInstance(baseUrl)
                    .postGetData(LOAD_API_CALL_AGENT_URL, requestData)

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

    fun agentWhatsappCallStatus(baseUrl: String, d_id: String, date: String) {
        viewModelScope.launch {
            try {
                val requestData = hashMapOf(
                    "token" to token,
                    "d_id" to d_id,
                    "date" to date
                )
                val response = retrofitClient.getInstance(baseUrl)
                    .postGetData(LOAD_API_CALL_WhatsappStatus_URL, requestData)

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
        date: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {

                if (!recordingFile.exists()) {
                    errorMsg1.postValue("Recording file does not exist")
                    return@launch
                }

                val audioFilePart = MultipartBody.Part.createFormData(
                    "rec_file", recordingFile.name,
                    RequestBody.create("audio/wav".toMediaTypeOrNull(), recordingFile)
                )

                val data = hashMapOf(
                    "file_name" to RequestBody.create("text/plain".toMediaTypeOrNull(), fileName),
                    "start_time" to RequestBody.create("text/plain".toMediaTypeOrNull(), startTime),
                    "end_time" to RequestBody.create("text/plain".toMediaTypeOrNull(), endTime),
                    "duration" to RequestBody.create(
                        "text/plain".toMediaTypeOrNull(),
                        duration.toString()
                    ),
                    "token" to RequestBody.create("text/plain".toMediaTypeOrNull(), token),
                    "d_id" to RequestBody.create("text/plain".toMediaTypeOrNull(), d_id),
                    "date" to RequestBody.create("text/plain".toMediaTypeOrNull(), date)
                )

                val response = retrofitClient.getInstance(baseUrl)
                    .uploadRecording(LOAD_CALL_HISTORY_URL, audioFilePart, data)


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
}
