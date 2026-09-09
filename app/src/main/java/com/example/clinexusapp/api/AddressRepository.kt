package com.example.clinexusapp.api

import com.example.clinexusapp.model.*
import com.example.clinexusapp.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddressRepository @Inject constructor(private val apiService: AddressApiService) {

    suspend fun getRegions(): Resource<List<Region>> {
        return try {
            val response = apiService.getRegions()
            if ((response.isSuccessful) && (response.body() != null)) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Failed to fetch regions")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }

    suspend fun getProvinces(regionCode: String): Resource<List<Province>> {
        return try {
            val response = apiService.getProvinces(regionCode)
            if ((response.isSuccessful) && (response.body() != null)) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Failed to fetch provinces")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }

    suspend fun getCities(provinceCode: String): Resource<List<City>> {
        return try {
            val response = apiService.getCitiesInProvince(provinceCode)
            if ((response.isSuccessful) && (response.body() != null)) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Failed to fetch cities")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }

    suspend fun getBarangays(cityCode: String): Resource<List<Barangay>> {
        return try {
            val response = apiService.getBarangays(cityCode)
            if ((response.isSuccessful) && (response.body() != null)) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Failed to fetch barangays")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }
}
