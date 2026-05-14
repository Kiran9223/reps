package com.reps.app.core.network.api

import com.reps.app.core.network.dto.OffProductResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApiService {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): OffProductResponseDto
}
