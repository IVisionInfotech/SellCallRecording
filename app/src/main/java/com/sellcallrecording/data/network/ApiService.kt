package com.sellcallrecording.data.network

import com.sellcallrecording.data.model.APIResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {

    @FormUrlEncoded
    @POST
    suspend fun postGetData(
        @Url url: String,
        @FieldMap requestData: Map<String, @JvmSuppressWildcards Any> = emptyMap(),
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): APIResponse

    @Multipart
    @POST
    suspend fun uploadRecording(
        @Url url: String,
        @Part audioFile: MultipartBody.Part,
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): APIResponse
}

