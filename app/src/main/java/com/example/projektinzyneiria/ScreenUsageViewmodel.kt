package com.example.projektinzyneiria
import androidx.compose.runtime.key
import com.example.projektinzyneiria.Data.AppUsageRepository
import com.example.projektinzyneiria.Data.AppUsage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
// 2. ViewModel przyjmujący repozytorium w konstruktorze
class UsageViewModel(
    private val repository: AppUsageRepository
) : ViewModel() {

    val allUsages = repository.getAllUsages()

    fun addUsage(usage: AppUsage) = viewModelScope.launch {
        repository.upsertUsage(usage)
    }

    fun clearAll() = viewModelScope.launch {
        repository.clearAllUsages()
    }

    fun deleteForPackage(pkg: String) = viewModelScope.launch {
        repository.deleteUsagesForPackage(pkg)
    }
    fun getUsagesByDate(date: String) = viewModelScope.launch {
        repository.getUsagesByDate(kotlinx.datetime.LocalDate.parse(date))
    }
    fun deleteOlderThan(date: String) = viewModelScope.launch {
        repository.deleteOlderThan(kotlinx.datetime.LocalDate.parse(date))
    }
    fun isAppMonitored(pkg: String) = viewModelScope.launch {
        repository.isAppMonitored(pkg)
    }
    // inne metody analogicznie...
}
