package com.example.clinexusapp

import com.example.clinexusapp.model.ClinicalHistoryResponse
import com.google.gson.Gson
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalHistoryParsingTest {
    @Test
    fun `null medical history is accepted as an empty record`() {
        val response = Gson().fromJson(
            """{"success":true,"clinicalHistory":{"medicalHistory":null,"consultations":[],"treatmentPlans":[],"treatmentSessions":[],"serviceSessions":[],"prescriptions":[]}}""",
            ClinicalHistoryResponse::class.java,
        )

        assertTrue(response.success)
        assertFalse(response.clinicalHistory.medicalHistory?.isJsonObject == true)
    }
}
