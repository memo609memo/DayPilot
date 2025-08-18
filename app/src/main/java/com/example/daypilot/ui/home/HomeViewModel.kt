package com.example.daypilot.ui.home

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daypilot.data.TaskRepo
import com.example.daypilot.data.TaskResponse
import kotlinx.coroutines.launch

class HomeViewModel(private val repo: TaskRepo) : ViewModel() {


    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text

    // the speech
    private val _speech = MutableLiveData<String>()
    val speech: LiveData<String> = _speech

    // the ai
    private val _aiResponse = MutableLiveData<TaskResponse>()
    val aiResponse: LiveData<TaskResponse> = _aiResponse

    fun onNewSpeechText(text: String) {
        _speech.value = text
    }

    fun sendTextToAi(text: String) {
        viewModelScope.launch {
            val response = repo.postToPythonModel(text)
            _aiResponse.postValue(response)
        }
    }


}