package com.vacum.proyectovademecum

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface GoogleTranslateApi {
    @POST("language/translate/v2")
    @FormUrlEncoded
    suspend fun traducirTexto(
        @Field("q") texto: String,
        @Field("target") idiomaDestino: String,
        @Field("format") formato: String = "text",
        @Field("key") apiKey: String
    ): Response<TraduccionResponse>
}
