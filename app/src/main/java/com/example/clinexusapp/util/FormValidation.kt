package com.example.clinexusapp.util

import java.time.LocalDate

fun isValidPhilippineMobile(value: String): Boolean =
    value.filterNot(Char::isWhitespace).matches(Regex("^(?:\\+63|0)9\\d{9}$"))

fun isValidBirthDate(value: String): Boolean = runCatching {
    val date = LocalDate.parse(value)
    !date.isAfter(LocalDate.now()) && date.isAfter(LocalDate.now().minusYears(120))
}.getOrDefault(false)
