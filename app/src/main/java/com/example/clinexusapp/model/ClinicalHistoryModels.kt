package com.example.clinexusapp.model

import com.google.gson.JsonObject
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class ClinicalHistoryResponse(
    val success: Boolean,
    val clinicalHistory: ClinicalHistoryDTO,
)

data class ClinicalHistoryDTO(
    val medicalHistory: JsonElement? = null,
    val consultations: List<JsonObject> = emptyList(),
    val treatmentPlans: List<JsonObject> = emptyList(),
    val treatmentSessions: List<JsonObject> = emptyList(),
    val serviceSessions: List<JsonObject> = emptyList(),
    val prescriptions: List<JsonObject> = emptyList(),
)

fun JsonObject.text(name: String): String? =
    get(name)?.takeUnless { it.isJsonNull }?.asString?.takeIf { it.isNotBlank() }

fun JsonObject.firstText(vararg names: String): String? = names.firstNotNullOfOrNull(::text)
