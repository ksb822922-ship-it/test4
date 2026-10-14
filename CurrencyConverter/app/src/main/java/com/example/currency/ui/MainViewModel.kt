package com.example.currency.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.currency.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class UiState(
    val from: String = "USD", val to: String = "EUR", val amount: String = "100",
    val rates: Map<String, Double> = emptyMap(), val offline: Boolean = false,
    val updatedAt: Long = 0, val loading: Boolean = false
) {
    val value: Double? get() = amount.replace(',', '.').toDoubleOrNull()
    val result: Double? get() = value?.let { v -> rates[to]?.let { v * it } }
}

@OptIn(FlowPreview::class)
@HiltViewModel
class MainViewModel @Inject constructor(private val repo: Repository, private val prefs: Prefs) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()
    val shake = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val history = repo.history().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val theme = prefs.theme.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val offlineMode = prefs.offlineMode.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val defFrom = prefs.from.stateIn(viewModelScope, SharingStarted.Eagerly, "USD")
    val defTo = prefs.to.stateIn(viewModelScope, SharingStarted.Eagerly, "EUR")
    private val commit = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var job: Job? = null

    init {
        viewModelScope.launch {
            _state.update { it.copy(from = prefs.from.first(), to = prefs.to.first()) }
            refresh()
        }
        // Save a history entry once the user stops typing.
        viewModelScope.launch {
            commit.debounce(1500).collect {
                val s = _state.value
                val v = s.value; val r = s.result
                if (v != null && v > 0 && r != null) repo.addHistory(HistoryEntity(from = s.from, to = s.to, amount = v, result = r, ts = System.currentTimeMillis()))
            }
        }
    }

    fun refresh() {
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val res = repo.rates(_state.value.from, offlineMode.value)
            delay(600) // let the shimmer be visible
            _state.update {
                if (res == null) it.copy(loading = false, rates = emptyMap(), offline = true)
                else it.copy(loading = false, rates = res.rates, offline = res.offline, updatedAt = res.ts)
            }
            commit.tryEmit(Unit)
        }
    }

    fun setAmount(t: String) {
        if (t.length <= 12 && t.all { it.isDigit() || it == '.' || it == ',' }) {
            _state.update { it.copy(amount = t) }
            if (_state.value.value == null && t.isNotEmpty()) shake.tryEmit(Unit) else commit.tryEmit(Unit)
        }
    }
    fun setFrom(c: String) { _state.update { it.copy(from = c) }; refresh() }
    fun setTo(c: String) { _state.update { it.copy(to = c) }; commit.tryEmit(Unit) }
    fun setPair(f: String, t: String) { _state.update { it.copy(from = f, to = t) }; refresh() }
    fun swap() = setPair(_state.value.to, _state.value.from)
    fun validate() { if (_state.value.value == null) shake.tryEmit(Unit) }

    fun deleteHistory(id: Long) = viewModelScope.launch { repo.deleteHistory(id) }
    fun setTheme(v: Int) = viewModelScope.launch { prefs.setTheme(v) }
    fun setOffline(v: Boolean) = viewModelScope.launch { prefs.setOffline(v) }
    fun setDefaults(f: String, t: String) = viewModelScope.launch { prefs.setPair(f, t) }
}
