package com.example.clinexusapp.api

import com.example.clinexusapp.util.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/** Ends the local patient session when an authenticated request is rejected. */
object SessionExpiryInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code == 401 && request.header("Authorization") != null) {
            SessionManager.expireSession()
        }

        return response
    }
}
