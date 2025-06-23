package com.example.daypilot.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// used to see message sent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class TaskRepo {

    // creates the logger
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // creates/install in to our okHttpclient
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val api: AiApi = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:5000/")
            .client(client) // keep track of the info on the client
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AiApi::class.java)

        suspend fun postToPythonModel(text: String): TaskResponse {
            return api.createTask(TaskRequest(text))
        }
}