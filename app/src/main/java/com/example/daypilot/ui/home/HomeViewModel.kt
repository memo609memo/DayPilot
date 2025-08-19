package com.example.daypilot.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daypilot.data.TaskRepo
import com.example.daypilot.data.TaskResponse
import kotlinx.coroutines.launch

class HomeViewModel(private val repo: TaskRepo) : ViewModel() {

    private val _text = MutableLiveData("This is home Fragment")
    val text: LiveData<String> = _text

    // speech text from the mic overlay
    private val _speech = MutableLiveData<String>()
    val speech: LiveData<String> = _speech

    // nullable so we can "consume" it afterward
    private val _aiResponse = MutableLiveData<TaskResponse?>()
    val aiResponse: LiveData<TaskResponse?> = _aiResponse

    fun onNewSpeechText(text: String) {
        _speech.value = text
    }

    fun sendTextToAi(text: String) {
        viewModelScope.launch {
            try {
                val response = repo.postToPythonModel(text)
                _aiResponse.postValue(response)
            } catch (_: Exception) {
                // null observer so data doesn't become stuck there
                _aiResponse.postValue(null)
            }
        }
    }

    fun clearAiResponse() {
        _aiResponse.value = null
    }
}
