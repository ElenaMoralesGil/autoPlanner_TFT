package com.elena.autoplanner.domain.utils

interface DataSeeder {

    suspend fun seedTasks(count: Int = 15)

    suspend fun clearAll()
    fun isEnabled(): Boolean
}

class NoOpDataSeeder : DataSeeder {
    override suspend fun seedTasks(count: Int) {}

    override suspend fun clearAll() {}

    override fun isEnabled(): Boolean = false
}