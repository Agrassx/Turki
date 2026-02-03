package com.turki.core.database

import com.turki.core.domain.UserCustomDictionaryEntry
import com.turki.core.repository.UserCustomDictionaryRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

class UserCustomDictionaryRepositoryImpl : UserCustomDictionaryRepository {
    override suspend fun listByUser(userId: Long): List<UserCustomDictionaryEntry> = DatabaseFactory.dbQuery {
        UserCustomDictionaryTable.selectAll()
            .where { UserCustomDictionaryTable.userId eq userId }
            .map(::toEntry)
    }

    override suspend fun create(entry: UserCustomDictionaryEntry): UserCustomDictionaryEntry = DatabaseFactory.dbQuery {
        val id = UserCustomDictionaryTable.insert {
            it[userId] = entry.userId
            it[word] = entry.word
            it[translation] = entry.translation
            it[pronunciation] = entry.pronunciation
            it[example] = entry.example
            it[addedAt] = entry.addedAt
        }[UserCustomDictionaryTable.id].value
        entry.copy(id = id)
    }

    override suspend fun deleteByUser(userId: Long): Boolean = DatabaseFactory.dbQuery {
        UserCustomDictionaryTable.deleteWhere { UserCustomDictionaryTable.userId eq userId } > 0
    }

    private fun toEntry(row: ResultRow): UserCustomDictionaryEntry = UserCustomDictionaryEntry(
        id = row[UserCustomDictionaryTable.id].value,
        userId = row[UserCustomDictionaryTable.userId].value,
        word = row[UserCustomDictionaryTable.word],
        translation = row[UserCustomDictionaryTable.translation],
        pronunciation = row[UserCustomDictionaryTable.pronunciation],
        example = row[UserCustomDictionaryTable.example],
        addedAt = row[UserCustomDictionaryTable.addedAt]
    )
}
