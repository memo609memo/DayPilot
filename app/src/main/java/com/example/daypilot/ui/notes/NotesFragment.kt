package com.example.daypilot.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

import com.example.daypilot.databinding.FragmentNotesBinding

class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(

        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
      val notesViewModel = ViewModelProvider(this).get(NotesViewModel::class.java)
        _binding = FragmentNotesBinding.inflate(inflater,container,false)
        val root : View = binding.root

        /*val textView: TextView = binding.textNotes
        notesViewModel.text.observe(viewLifecycleOwner){
            textView.text = it
        }*/
      return  root


    }




    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }



}