package com.jholachhapdevs.pdfjuggler.home.domain.usecase

import com.jholachhapdevs.pdfjuggler.home.data.repository.HomeRepository


// Use case to get update information from the repository
class GetUpdatesUseCase(private val repository: HomeRepository) {

    // Invoke the use case to fetch update data and convert it to domain model

    // the invoke operator allows instances of the class to be called as functions
    // e.g., getUpdatesUseCase()
    // This makes the code more readable and expressive


    suspend operator fun invoke() = repository.getUpdateData()?.toDomain()

    // The use of suspend indicates that this function is designed to be called from a coroutine or another suspend function
    // coroutines are a way to handle asynchronous programming in Kotlin (these are lightweight threads)
}