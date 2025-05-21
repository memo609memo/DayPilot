package com.example.daypilot.ui.notes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.applandeo.materialcalendarview.EventDay
import java.util.Collections.emptyList

class NotesViewModel : ViewModel() {
   private  val  _selectedDate = MutableLiveData<String>()
   val selectedDate: LiveData<String> = _selectedDate

   private val _eventList = MutableLiveData<List<EventDay>>(emptyList())
   val eventList: LiveData<List<EventDay>> = _eventList
   fun selectedDate(dateString: String)
   {
      _selectedDate.value = dateString
   }

   fun addEvent(event:EventDay){

      val current = _eventList.value?.toMutableList() ?: mutableListOf()
      current.add(event)
      _eventList.value = current
   }

}