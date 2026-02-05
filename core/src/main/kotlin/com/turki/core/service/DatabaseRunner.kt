package com.turki.core.service

import com.turki.core.database.DatabaseFactory

interface DatabaseRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

class ExposedDatabaseRunner : DatabaseRunner {
    override suspend fun <T> run(block: suspend () -> T): T = DatabaseFactory.dbQuery(block)
}
