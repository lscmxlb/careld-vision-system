package com.careld.vision.ui.child

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.careld.vision.data.local.AppDatabase
import com.careld.vision.data.local.entity.ChildProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Child search view model
 */
@HiltViewModel
class ChildSearchViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<ChildDisplay>>(emptyList())
    val searchResults: StateFlow<List<ChildDisplay>> = _searchResults.asStateFlow()

    init {
        // Load all children initially
        viewModelScope.launch {
            database.childProfileDao().getAll().collect { entities ->
                _searchResults.value = entities.map { it.toDisplay() }
            }
        }
    }

    fun search(keyword: String) {
        viewModelScope.launch {
            if (keyword.isEmpty()) {
                // Show all
                database.childProfileDao().getAll().collect { entities ->
                    _searchResults.value = entities.map { it.toDisplay() }
                }
            } else {
                // Search by name
                database.childProfileDao().searchByName(keyword).collect { entities ->
                    _searchResults.value = entities.map { it.toDisplay() }
                }
            }
        }
    }

    private fun ChildProfileEntity.toDisplay(): ChildDisplay {
        return ChildDisplay(
            id = id,
            childId = childId,
            name = name,
            phone = phone,
            birthDate = birthDate,
            gender = when (gender) {
                0 -> "女"
                1 -> "男"
                else -> "未知"
            },
            age = birthDate?.let { 
                com.careld.vision.core.utils.DateUtils.calculateAge(it) 
            } ?: 0
        )
    }

    /**
     * Child display data
     */
    data class ChildDisplay(
        val id: Long,
        val childId: String,
        val name: String,
        val phone: String?,
        val birthDate: String?,
        val gender: String,
        val age: Int
    )
}
