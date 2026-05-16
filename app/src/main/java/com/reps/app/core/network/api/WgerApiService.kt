package com.reps.app.core.network.api

import com.reps.app.core.network.dto.WgerExerciseInfoDto
import com.reps.app.core.network.dto.WgerPagedResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WgerApiService {
    @GET("api/v2/exerciseinfo/")
    suspend fun getExercises(
        @Query("language") language: Int = 2,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("format") format: String = "json"
    ): WgerPagedResponse<WgerExerciseInfoDto>
}
