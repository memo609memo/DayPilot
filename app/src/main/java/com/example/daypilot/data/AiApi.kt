package com.example.daypilot.data

import retrofit2.http.Body
import retrofit2.http.POST

interface AiApi {
    @POST("/tasks")
    suspend fun createTask(@Body request: TaskRequest): TaskResponse
}