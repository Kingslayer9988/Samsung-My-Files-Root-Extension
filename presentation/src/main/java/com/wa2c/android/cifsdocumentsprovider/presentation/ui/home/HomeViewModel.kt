package com.samsung.cifs.ui.ui.home

import androidx.lifecycle.ViewModel
import com.samsung.cifs.domain.repository.AppRepository
import com.samsung.cifs.ui.ext.MainCoroutineScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Home Screen ViewModel
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepository: AppRepository,
): ViewModel(), CoroutineScope by MainCoroutineScope() {

    val connectionListFlow = appRepository.connectionListFlow

    /**
     * Move item.
     */
    fun onItemMove(fromPosition: Int, toPosition: Int) {
        runBlocking {
            // run blocking for drag animation
            appRepository.moveConnection(fromPosition, toPosition)
        }
    }

}
