package com.ancient.wenyan.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.fsrs.CardFsrsState
import com.ancient.wenyan.domain.fsrs.Rating
import com.ancient.wenyan.domain.model.ActiveSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FlipCardViewModel @Inject constructor(
    private val repository: WenYanRepository
) : ViewModel() {

    fun submitRating(cardId: String, rating: Rating, nowMillis: Long = System.currentTimeMillis()): CardFsrsState {
        return repository.submitRating(cardId, rating, nowMillis)
    }

    fun saveActiveSession(session: ActiveSession) {
        repository.saveActiveSession(session)
    }

    fun clearActiveSession() {
        repository.clearActiveSession()
    }

    fun getRepository(): WenYanRepository = repository
}
