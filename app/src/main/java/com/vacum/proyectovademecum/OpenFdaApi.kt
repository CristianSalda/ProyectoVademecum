package com.vacum.proyectovademecum

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenFdaApi {
    @GET("drug/label.json")
    suspend fun getMedicamentos(
        @Query("search") search: String,
        @Query("limit") limit: Int = 1
    ): Response<OpenFdaResponse>
}