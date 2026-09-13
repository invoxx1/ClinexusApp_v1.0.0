package com.example.clinexusapp.api

import com.example.clinexusapp.model.*
import com.example.clinexusapp.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddressRepository @Inject constructor(private val apiService: AddressApiService) {

    private var regionsCache: List<Region>? = null
    private var allProvincesCache: List<Province>? = null
    private val provincesCache = mutableMapOf<String, List<Province>>()
    private val citiesCache = mutableMapOf<String, List<City>>()
    private val barangaysCache = mutableMapOf<String, List<Barangay>>()

    suspend fun getRegions(): Resource<List<Region>> {
        regionsCache?.let { return Resource.Success(it) }
        return try {
            val response = apiService.getRegions()
            if ((response.isSuccessful) && (response.body() != null)) {
                Resource.Success(response.body()!!.also { regionsCache = it })
            } else {
                Resource.Error("Failed to fetch regions")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }

    suspend fun getProvinces(regionCode: String): Resource<List<Province>> {
        provincesCache[regionCode]?.let { return Resource.Success(it) }
        return try {
            val response = apiService.getProvinces(regionCode)
            if ((response.isSuccessful) && (response.body() != null)) {
                Resource.Success(response.body()!!.also { provincesCache[regionCode] = it })
            } else {
                Resource.Error("Failed to fetch provinces")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }

    suspend fun getAllProvinces(): Resource<List<Province>> {
        allProvincesCache?.let { return Resource.Success(it) }
        return try {
            val response = apiService.getAllProvinces()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.also { allProvincesCache = it })
            } else {
                Resource.Error("Failed to fetch provinces")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }

    suspend fun getCities(provinceCode: String): Resource<List<City>> {
        citiesCache[provinceCode]?.let { return Resource.Success(it) }
        return try {
            val response = apiService.getCitiesInProvince(provinceCode)
            if ((response.isSuccessful) && (response.body() != null)) {
                Resource.Success(response.body()!!.also { citiesCache[provinceCode] = it })
            } else {
                Resource.Error("Failed to fetch cities")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }

    suspend fun getBarangays(cityCode: String): Resource<List<Barangay>> {
        barangaysCache[cityCode]?.let { return Resource.Success(it) }
        return try {
            val response = apiService.getBarangays(cityCode)
            if ((response.isSuccessful) && (response.body() != null)) {
                Resource.Success(response.body()!!.also { barangaysCache[cityCode] = it })
            } else {
                Resource.Error("Failed to fetch barangays")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }
}
