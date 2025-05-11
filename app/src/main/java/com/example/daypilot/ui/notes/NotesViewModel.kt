package com.example.daypilot.ui.notes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NotesViewModel : ViewModel() {
   private  val  _text = MutableLiveData<String>().apply {
      value = "This is Notes Fragment " }
   val  text : LiveData<String> = _text
}