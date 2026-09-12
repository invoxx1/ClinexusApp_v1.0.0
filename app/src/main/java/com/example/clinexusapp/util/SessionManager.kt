package com.example.clinexusapp.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.clinexusapp.model.PatientInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL


@Suppress("DEPRECATION") // EncryptedSharedPreferences is deprecated in favor of Jetpack DataStore + Tink
object SessionManager {
    private const val TAG = "SessionManager"
    private const val PREF_NAME = "secure_session_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_PATIENT_INFO = "patient_info"
    private const val KEY_SAVED_ACCOUNTS = "saved_accounts"
    private const val KEY_PASSWORD_CONSENT_MIGRATED = "password_consent_migrated_v1"

    data class SavedAccount(
        val patient: PatientInfo,
        val token: String,
        val password: String? = null,
        val cachedProfilePicture: String? = null,
    )

    data class PendingPasswordSave(
        val patientID: Int,
        val password: String,
    )

    private val _currentUser = MutableStateFlow<PatientInfo?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _savedAccounts = MutableStateFlow<List<SavedAccount>>(emptyList())
    val savedAccounts = _savedAccounts.asStateFlow()

    private val _pendingPasswordSave = MutableStateFlow<PendingPasswordSave?>(null)
    val pendingPasswordSave = _pendingPasswordSave.asStateFlow()

    private var requestedAccountLoginPatientID: Int? = null

    private val _isInitialized = MutableStateFlow(value = false)
    val isInitialized = _isInitialized.asStateFlow()

    private val _sessionExpired = MutableStateFlow(value = false)
    val sessionExpired = _sessionExpired.asStateFlow()

    private var _token: String? = null
    val token: String? get() = _token

    private var sharedPreferences: android.content.SharedPreferences? = null
    private var applicationContext: Context? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val startTime = System.currentTimeMillis()
            try {
                sharedPreferences = createSharedPreferences(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize EncryptedSharedPreferences, attempting recovery", e)
                try {
                    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit(commit = true) {
                        clear()
                    }
                    sharedPreferences = createSharedPreferences(context)
                } catch (recoveryException: Exception) {
                    Log.e(TAG, "Recovery failed, session management will be unavailable", recoveryException)
                }
            }

            sharedPreferences?.let { prefs ->
                _token = prefs.getString(KEY_TOKEN, null)
                val patientJson = prefs.getString(KEY_PATIENT_INFO, null)
                val savedAccountsJson = prefs.getString(KEY_SAVED_ACCOUNTS, null)
                val savedAccountType = object : TypeToken<List<SavedAccount>>() {}.type
                val parsedAccounts = try {
                    if (savedAccountsJson.isNullOrBlank()) emptyList() else Gson().fromJson<List<SavedAccount>>(savedAccountsJson, savedAccountType)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing saved accounts from prefs", e)
                    emptyList()
                }
                // Passwords written before explicit consent was introduced must not be retained.
                val hasMigratedPasswordConsent = prefs.getBoolean(KEY_PASSWORD_CONSENT_MIGRATED, false)
                val storedAccounts = if (hasMigratedPasswordConsent) {
                    parsedAccounts
                } else {
                    parsedAccounts.map { it.copy(password = null) }.also { sanitizedAccounts ->
                        prefs.edit {
                            putString(KEY_SAVED_ACCOUNTS, Gson().toJson(sanitizedAccounts))
                            putBoolean(KEY_PASSWORD_CONSENT_MIGRATED, true)
                        }
                    }
                }
                if (patientJson != null) {
                    try {
                        val patient = Gson().fromJson(patientJson, PatientInfo::class.java)
                        withContext(Dispatchers.Main) {
                            _currentUser.value = patient
                            _savedAccounts.value = mergeAccount(storedAccounts, _token, patient, password = null)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing patient info from prefs", e)
                    }
                } else {
                    _savedAccounts.value = storedAccounts
                }
            }
            
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "SessionManager initialized in ${duration}ms")
            _isInitialized.value = true
        }
    }

    private fun createSharedPreferences(context: Context): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun saveSession(
        token: String,
        patient: PatientInfo,
        rememberAccount: Boolean = true,
        password: String? = null,
    ) {
        val existingPatient = _savedAccounts.value
            .firstOrNull { it.patient.patientID == patient.patientID }
            ?.patient
        val sessionPatient = mergePatientDetails(existingPatient, patient)
        _token = token
        _currentUser.value = sessionPatient
        if (rememberAccount) {
            _savedAccounts.value = mergeAccount(_savedAccounts.value, token, sessionPatient, password)
        }

        sharedPreferences?.edit {
            putString(KEY_TOKEN, token)
            putString(KEY_PATIENT_INFO, Gson().toJson(sessionPatient))
            if (rememberAccount) putString(KEY_SAVED_ACCOUNTS, Gson().toJson(_savedAccounts.value))
            Log.d(TAG, "Session saved successfully for patient ID: ${sessionPatient.patientID}")
        } ?: Log.w(TAG, "saveSession called but sharedPreferences is null")
        cacheProfilePicture(sessionPatient)
    }

    fun updateProfile(patient: PatientInfo) {
        _currentUser.value = patient
        _savedAccounts.value = _savedAccounts.value.map { account ->
            if (account.patient.patientID == patient.patientID) account.copy(patient = patient) else account
        }
        sharedPreferences?.edit {
            putString(KEY_PATIENT_INFO, Gson().toJson(patient))
            putString(KEY_SAVED_ACCOUNTS, Gson().toJson(_savedAccounts.value))
            Log.d(TAG, "Profile updated successfully for patient ID: ${patient.patientID}")
        }
        cacheProfilePicture(patient)
    }

    fun logout() {
        _sessionExpired.value = false
        clearActiveSession()
    }

    @Synchronized
    fun expireSession() {
        if (_token.isNullOrBlank() || _sessionExpired.value) return
        clearActiveSession()
        _sessionExpired.value = true
    }

    fun acknowledgeSessionExpiry() {
        _sessionExpired.value = false
    }

    private fun clearActiveSession() {
        _pendingPasswordSave.value = null
        _token = null
        _currentUser.value = null
        sharedPreferences?.edit {
            remove(KEY_TOKEN)
            remove(KEY_PATIENT_INFO)
            Log.d(TAG, "Session cleared successfully")
        } ?: Log.w(TAG, "logout called but sharedPreferences is null")
    }

    fun switchAccount(patientID: Int): Boolean {
        val account = _savedAccounts.value.firstOrNull { it.patient.patientID == patientID } ?: return false
        _token = account.token
        _currentUser.value = account.patient
        sharedPreferences?.edit {
            putString(KEY_TOKEN, account.token)
            putString(KEY_PATIENT_INFO, Gson().toJson(account.patient))
        }
        return true
    }

    fun requestPasswordSave(patientID: Int, password: String) {
        _pendingPasswordSave.value = PendingPasswordSave(patientID, password)
    }

    fun confirmPasswordSave() {
        val pending = _pendingPasswordSave.value ?: return
        _savedAccounts.value = _savedAccounts.value.map { account ->
            if (account.patient.patientID == pending.patientID) {
                account.copy(password = pending.password)
            } else {
                account
            }
        }
        sharedPreferences?.edit {
            putString(KEY_SAVED_ACCOUNTS, Gson().toJson(_savedAccounts.value))
        }
        _pendingPasswordSave.value = null
    }

    fun dismissPasswordSave() {
        _pendingPasswordSave.value = null
    }

    fun removeSavedAccount(patientID: Int) {
        val removedAccount = _savedAccounts.value.firstOrNull { it.patient.patientID == patientID }
        _savedAccounts.value = _savedAccounts.value.filterNot { it.patient.patientID == patientID }
        sharedPreferences?.edit {
            putString(KEY_SAVED_ACCOUNTS, Gson().toJson(_savedAccounts.value))
        }
        removedAccount?.cachedProfilePicture?.let { pictureUri ->
            runCatching {
                val cachedFile = pictureUri.toUri().path?.let(::File) ?: return@runCatching
                val filesDirectory = applicationContext?.filesDir?.canonicalFile ?: return@runCatching
                if (cachedFile.canonicalFile.parentFile == filesDirectory) cachedFile.delete()
            }.onFailure { Log.w(TAG, "Unable to remove cached account photo", it) }
        }
    }

    fun requestAccountLogin(patientID: Int) {
        requestedAccountLoginPatientID = patientID
    }

    fun consumeRequestedAccountLogin(): Int? {
        return requestedAccountLoginPatientID.also { requestedAccountLoginPatientID = null }
    }

    private fun mergeAccount(
        accounts: List<SavedAccount>,
        token: String?,
        patient: PatientInfo,
        password: String?,
    ): List<SavedAccount> {
        if (token.isNullOrBlank()) return accounts
        val existingPassword = accounts.firstOrNull { it.patient.patientID == patient.patientID }?.password
        val existingPicture = accounts.firstOrNull { it.patient.patientID == patient.patientID }?.cachedProfilePicture
        return listOf(SavedAccount(patient, token, password ?: existingPassword, existingPicture)) +
            accounts.filterNot { it.patient.patientID == patient.patientID }
    }

    private fun mergePatientDetails(previous: PatientInfo?, incoming: PatientInfo): PatientInfo {
        if (previous == null) return incoming
        fun preferIncoming(value: String?, fallback: String?): String? = value?.takeIf { it.isNotBlank() } ?: fallback
        return incoming.copy(
            role = incoming.role.ifBlank { previous.role },
            email = preferIncoming(incoming.email, previous.email),
            firstName = preferIncoming(incoming.firstName, previous.firstName),
            middleName = preferIncoming(incoming.middleName, previous.middleName),
            lastName = preferIncoming(incoming.lastName, previous.lastName),
            phoneNumber = preferIncoming(incoming.phoneNumber, previous.phoneNumber),
            dateOfBirth = preferIncoming(incoming.dateOfBirth, previous.dateOfBirth),
            streetAddress = preferIncoming(incoming.streetAddress, previous.streetAddress),
            province = preferIncoming(incoming.province, previous.province),
            city = preferIncoming(incoming.city, previous.city),
            barangay = preferIncoming(incoming.barangay, previous.barangay),
            profilePicture = preferIncoming(incoming.profilePicture, previous.profilePicture),
        )
    }

    private fun cacheProfilePicture(patient: PatientInfo) {
        val imageUrl = patient.profilePicture?.takeIf { it.isNotBlank() } ?: return
        val context = applicationContext ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val avatarFile = File(context.filesDir, "saved_account_avatar_${patient.patientID}.jpg")
                URL(imageUrl).openStream().use { input ->
                    avatarFile.outputStream().use { output -> input.copyTo(output) }
                }
                val localUri = Uri.fromFile(avatarFile).toString()
                _savedAccounts.value = _savedAccounts.value.map { account ->
                    if (account.patient.patientID == patient.patientID) {
                        account.copy(cachedProfilePicture = localUri)
                    } else {
                        account
                    }
                }
                sharedPreferences?.edit {
                    putString(KEY_SAVED_ACCOUNTS, Gson().toJson(_savedAccounts.value))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Unable to cache profile picture for patient ID: ${patient.patientID}", e)
            }
        }
    }

    val isLoggedIn: Boolean get() = _token != null
}
