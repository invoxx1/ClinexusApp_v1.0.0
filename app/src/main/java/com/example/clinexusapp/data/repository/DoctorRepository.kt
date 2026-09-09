package com.example.clinexusapp.data.repository

import com.example.clinexusapp.ui.screens.doctors.Doctor

class DoctorRepository {
    fun getDoctors(): List<Doctor> {
        return listOf(
            Doctor("Dr. Sarah Wilson", "Cardiologist", 4.8),
            Doctor("Dr. John Smith", "Neurologist", 4.9),
            Doctor("Dr. Emily Brown", "Dermatologist", 4.7),
            Doctor("Dr. Michael Lee", "Pediatrician", 4.6),
            Doctor("Dr. Jessica Davis", "Gynecologist", 4.8)
        )
    }
}
