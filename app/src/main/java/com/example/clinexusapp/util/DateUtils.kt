package com.example.clinexusapp.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    /**
     * Formats a date string for the chat conversation list (iMessage style)
     * e.g., "9:51 AM", "Yesterday", "Tuesday", or "Aug 31"
     */
    fun formatChatTime(dateString: String?): String {
        if (dateString == null || dateString.isEmpty()) return ""
        
        // If it already looks like formatted time (e.g. "09:51 AM"), return it
        if (dateString.matches(Regex(".*\\d{1,2}:\\d{2}\\s*(AM|PM|am|pm).*"))) {
            return dateString
        }

        return try {
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
            )
            
            var date: Date? = null
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.getDefault())
                    if (format.contains("Z")) {
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                    }
                    date = sdf.parse(dateString)
                    if (date != null) break
                } catch (e: Exception) { continue }
            }
            
            if (date == null) return dateString
            
            val now = Calendar.getInstance()
            val chatDate = Calendar.getInstance()
            chatDate.time = date
            
            // Check if today
            if (now.get(Calendar.YEAR) == chatDate.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == chatDate.get(Calendar.DAY_OF_YEAR)) {
                return SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
            }
            
            // Check if yesterday
            val yesterday = Calendar.getInstance()
            yesterday.add(Calendar.DAY_OF_YEAR, -1)
            if (yesterday.get(Calendar.YEAR) == chatDate.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == chatDate.get(Calendar.DAY_OF_YEAR)) {
                return "Yesterday"
            }
            
            // Check if within last 7 days
            val lastWeek = Calendar.getInstance()
            lastWeek.add(Calendar.DAY_OF_YEAR, -7)
            if (chatDate.after(lastWeek)) {
                return SimpleDateFormat("EEEE", Locale.getDefault()).format(date) // Day name like "Tuesday"
            }
            
            // Otherwise show date
            return if (now.get(Calendar.YEAR) == chatDate.get(Calendar.YEAR)) {
                SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            } else {
                SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(date)
            }
            
        } catch (e: Exception) {
            dateString
        }
    }

    fun getDateOnly(dateString: String?): String {
        if (dateString == null || dateString.isEmpty()) return ""
        return try {
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
            )
            
            var date: Date? = null
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.getDefault())
                    if (format.contains("Z")) {
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                    }
                    date = sdf.parse(dateString)
                    if (date != null) break
                } catch (e: Exception) { continue }
            }
            
            if (date == null) return ""
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            ""
        }
    }

    fun formatSeparatorDate(dateString: String?): String {
        if (dateString == null || dateString.isEmpty()) return ""
        return try {
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
            )
            
            var date: Date? = null
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.getDefault())
                    if (format.contains("Z")) {
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                    }
                    date = sdf.parse(dateString)
                    if (date != null) break
                } catch (e: Exception) { continue }
            }
            
            if (date == null) return ""
            SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            ""
        }
    }

    fun formatDisplayDate(dateString: String?): String {
        if (dateString == null) return ""
        return try {
            val cleanString = if (dateString.contains("T")) {
                dateString.substringBefore("T")
            } else {
                dateString
            }
            
            val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputSdf.parse(cleanString) ?: return cleanString
            
            val outputSdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            outputSdf.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    fun formatDisplayTime(timeString: String?): String {
        if (timeString == null) return ""
        return try {
            val inputSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = inputSdf.parse(timeString) ?: return timeString
            
            val outputSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            outputSdf.format(date)
        } catch (e: Exception) {
            timeString
        }
    }
}
