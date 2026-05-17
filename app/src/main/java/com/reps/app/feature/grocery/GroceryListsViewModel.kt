package com.reps.app.feature.grocery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.core.di.IoDispatcher
import com.reps.app.core.domain.model.GroceryListSummary
import com.reps.app.core.domain.repository.GroceryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroceryListsViewModel @Inject constructor(
    private val groceryRepository: GroceryRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    val lists: StateFlow<List<GroceryListSummary>> = groceryRepository.getLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createList(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch(ioDispatcher) {
            groceryRepository.createList(name.trim())
        }
    }

    fun deleteList(listId: Long) {
        viewModelScope.launch(ioDispatcher) {
            groceryRepository.deleteList(listId)
        }
    }
}
