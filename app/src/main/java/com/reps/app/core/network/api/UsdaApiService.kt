package com.reps.app.core.network.api

import com.reps.app.core.network.dto.UsdaSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface UsdaApiService {
    @GET("fdc/v1/foods/search")
    suspend fun searchFoods(
        @Query("query") query: String,
        @Query("api_key") apiKey: String,
        @Query("pageSize") pageSize: Int,
        @Query("dataType") dataType: List<String>
    ): UsdaSearchResponseDto
}
