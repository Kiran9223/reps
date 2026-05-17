package com.reps.app.core.network.api

import com.reps.app.core.network.dto.WgerExerciseInfoDto
import com.reps.app.core.network.dto.WgerPagedResponse
import com.reps.app.core.network.dto.WgerSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WgerApiService {
    @GET("api/v2/exerciseinfo/")
    suspend fun getExercises(
        @Query("language") language: Int = 2,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("format") format: String = "json"
    ): WgerPagedResponse<WgerExerciseInfoDto>

    @GET("api/v2/exercise/search/")
    suspend fun searchExercises(
        @Query("term") term: String,
        @Query("language") language: String = "english",
        @Query("format") format: String = "json"
    ): WgerSearchResponse

    @GET("api/v2/exerciseinfo/{id}/")
    suspend fun getExerciseInfo(
        @Path("id") id: Int,
        @Query("format") format: String = "json"
    ): WgerExerciseInfoDto
}
