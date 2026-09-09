package com.example.clinexusapp.api

import com.example.clinexusapp.model.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface AddressApiService {
    @GET("regions")
    suspend fun getRegions(): Response<List<Region>>

    @GET("regions/{code}/provinces")
    suspend fun getProvinces(@Path("code") regionCode: String): Response<List<Province>>

    /*
    @GET("regions/{code}/cities-municipalities")
    suspend fun getCitiesInRegion(@Path("code") regionCode: String): Response<List<City>>
    */

    @GET("provinces/{code}/cities-municipalities")
    suspend fun getCitiesInProvince(@Path("code") provinceCode: String): Response<List<City>>

    @GET("cities-municipalities/{code}/barangays")
    suspend fun getBarangays(@Path("code") cityCode: String): Response<List<Barangay>>
}
