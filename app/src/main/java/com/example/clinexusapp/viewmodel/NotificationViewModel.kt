package com.example.clinexusapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinexusapp.api.AuthRepository
import com.example.clinexusapp.model.NotificationDTO
import com.example.clinexusapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<Resource<List<NotificationDTO>>>(Resource.Loading)
    val notifications = _notifications.asStateFlow()

    fun loadNotifications() {
        viewModelScope.launch {
            _notifications.value = Resource.Loading
            _notifications.value = repository.getNotifications()
        }
    }

    fun markAsRead(notification: NotificationDTO) {
        if (notification.isRead != 0) return

        viewModelScope.launch {
            if (repository.markNotificationAsRead(notification.notificationId) is Resource.Success) {
                val current = _notifications.value
                if (current is Resource.Success) {
                    _notifications.value = Resource.Success(
                        current.data.map {
                            if (it.notificationId == notification.notificationId) {
                                it.copy(isRead = 1)
                            } else {
                                it
                            }
                        }
                    )
                }
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            if (repository.markAllNotificationsAsRead() is Resource.Success) {
                val current = _notifications.value
                if (current is Resource.Success) {
                    _notifications.value = Resource.Success(
                        current.data.map { it.copy(isRead = 1) }
                    )
                }
            }
        }
    }
}