package com.example.clinexusapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinexusapp.api.AuthRepository
import com.example.clinexusapp.model.ClinicalHistoryResponse
import com.example.clinexusapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClinicalHistoryViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<Resource<ClinicalHistoryResponse>>(Resource.Loading)
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = Resource.Loading
            _state.value = repository.getMyClinicalHistory()
        }
    }
}
