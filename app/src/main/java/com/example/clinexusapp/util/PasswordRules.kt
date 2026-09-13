package com.example.clinexusapp.util

fun isValidNewPassword(password: String): Boolean =
    password.length >= 8 && password.any { it.isUpperCase() } && password.none { it.isWhitespace() }

fun passwordsMeetRules(password: String, confirmation: String): Boolean =
    isValidNewPassword(password) && password == confirmation