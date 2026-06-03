package day.azimuth.observer.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import day.azimuth.observer.data.local.AzimuthPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class AzimuthNavViewModel @Inject constructor(
    prefs: AzimuthPreferences,
) : ViewModel() {
    val isRegistered: Flow<Boolean> = prefs.isRegistered
}
