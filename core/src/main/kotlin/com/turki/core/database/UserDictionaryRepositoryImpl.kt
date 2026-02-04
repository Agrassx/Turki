package com.turki.core.database

import com.turki.core.domain.UserDictionaryEntry
import com.turki.core.repository.UserDictionaryRepository
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class UserDictionaryRepositoryImpl : UserDictionaryRepository {
    override suspend fun findByUserAndVocabulary(userId: Long, vocabularyId: Int): UserDictionaryEntry? =
        DatabaseFactory.dbQuery {
            UserDictionaryTable.selectAll()
                .where {
                    (UserDictionaryTable.userId eq userId) and
                        (UserDictionaryTable.vocabularyId eq vocabularyId)
                }
                .map(::toEntry)
                .singleOrNull()
        }

    override suspend fun findByUser(userId: Long): List<UserDictionaryEntry> = DatabaseFactory.dbQuery {
        UserDictionaryTable.selectAll()
            .where { UserDictionaryTable.userId eq userId }
            .map(::toEntry)
    }

    override suspend fun upsert(entry: UserDictionaryEntry): UserDictionaryEntry = DatabaseFactory.dbQuery {
        val now = Clock.System.now()
        val updated = UserDictionaryTable.update({
            (UserDictionaryTable.userId eq entry.userId) and
                (UserDictionaryTable.vocabularyId eq entry.vocabularyId)
        }) {
            it[isFavorite] = entry.isFavorite
            it[tags] = entry.tags
            it[addedAt] = entry.addedAt
        }

        if (updated == 0) {
            UserDictionaryTable.insert {
                it[userId] = entry.userId
                it[vocabularyId] = entry.vocabularyId
                it[isFavorite] = entry.isFavorite
                it[tags] = entry.tags
                it[addedAt] = now
            }
        }

        entry.copy(addedAt = now)
    }

    override suspend fun listFavorites(userId: Long, limit: Int): List<UserDictionaryEntry> = DatabaseFactory.dbQuery {
        UserDictionaryTable.selectAll()
            .where { (UserDictionaryTable.userId eq userId) and (UserDictionaryTable.isFavorite eq true) }
            .limit(limit)
            .map(::toEntry)
    }

    override suspend fun deleteByUser(userId: Long): Boolean = DatabaseFactory.dbQuery {
        UserDictionaryTable.deleteWhere { UserDictionaryTable.userId eq userId } > 0
    }

    private fun toEntry(row: org.jetbrains.exposed.sql.ResultRow): UserDictionaryEntry = UserDictionaryEntry(
        userId = row[UserDictionaryTable.userId].value,
        vocabularyId = row[UserDictionaryTable.vocabularyId].value,
        isFavorite = row[UserDictionaryTable.isFavorite],
        tags = row[UserDictionaryTable.tags],
        addedAt = row[UserDictionaryTable.addedAt]
    )
}
