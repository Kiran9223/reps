package com.reps.app.core.network.api

import com.reps.app.core.network.dto.ApiNinjasFoodDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ApiNinjasApiService {
    @GET("v1/nutrition")
    suspend fun getNutrition(
        @Header("X-Api-Key") apiKey: String,
        @Query("query") query: String
    ): List<ApiNinjasFoodDto>
}
