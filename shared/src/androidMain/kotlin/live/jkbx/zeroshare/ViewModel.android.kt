package live.jkbx.zeroshare

import androidx.lifecycle.ViewModel as AndroidXViewModel


actual abstract class ViewModel actual constructor() : AndroidXViewModel() {
    actual override fun onCleared() {
        super.onCleared()
    }
}