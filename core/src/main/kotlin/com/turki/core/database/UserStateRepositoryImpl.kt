package com.turki.core.database

import com.turki.core.domain.UserState
import com.turki.core.repository.UserStateRepository
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class UserStateRepositoryImpl(
    private val clock: Clock = Clock.System
) : UserStateRepository {
    override suspend fun findByUserId(userId: Long): UserState? = DatabaseFactory.dbQuery {
        UserStatesTable.selectAll()
            .where { UserStatesTable.userId eq userId }
            .map(::toUserState)
            .singleOrNull()
    }

    override suspend fun upsert(userId: Long, state: String, payload: String): UserState = DatabaseFactory.dbQuery {
        val now = clock.now()
        val updated = UserStatesTable.update({ UserStatesTable.userId eq userId }) {
            it[UserStatesTable.state] = state
            it[UserStatesTable.payload] = payload
            it[UserStatesTable.updatedAt] = now
        }

        if (updated == 0) {
            UserStatesTable.insert {
                it[UserStatesTable.userId] = userId
                it[UserStatesTable.state] = state
                it[UserStatesTable.payload] = payload
                it[UserStatesTable.updatedAt] = now
            }
        }

        UserState(userId = userId, state = state, payload = payload, updatedAt = now)
    }

    override suspend fun clear(userId: Long): Boolean = DatabaseFactory.dbQuery {
        UserStatesTable.deleteWhere { UserStatesTable.userId eq userId } > 0
    }

    private fun toUserState(row: org.jetbrains.exposed.sql.ResultRow): UserState = UserState(
        userId = row[UserStatesTable.userId].value,
        state = row[UserStatesTable.state],
        payload = row[UserStatesTable.payload],
        updatedAt = row[UserStatesTable.updatedAt]
    )
}
