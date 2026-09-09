package com.example.clinexusapp.model

import com.google.gson.annotations.SerializedName

data class Region(
    val code: String,
    val name: String,
    val regionName: String,
)

data class Province(
    val code: String,
    val name: String,
    @SerializedName("provinceName") val provinceName: String? = null,
    val regionCode: String
) {
    val displayName: String get() = provinceName ?: name
}

data class City(
    val code: String,
    val name: String,
    @SerializedName("cityName") val cityName: String? = null,
    val provinceCode: String? = null,
    val regionCode: String? = null
) {
    val displayName: String get() = cityName ?: name
}

data class Barangay(
    val code: String,
    val name: String,
    @SerializedName("barangayName") val barangayName: String? = null,
    val cityCode: String? = null,
    val municipalityCode: String? = null
) {
    val displayName: String get() = barangayName ?: name
}
