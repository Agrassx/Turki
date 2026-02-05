package com.turki.bot.service

import com.turki.core.domain.User
import com.turki.core.repository.UserRepository
import com.turki.bot.testutil.TestClock
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val fixedInstant = Instant.fromEpochMilliseconds(1_000)
    private val clock = TestClock(fixedInstant)
    private val service = UserService(userRepository, clock)

    @Test
    fun `findOrCreateUser returns existing`() = runTest {
        val existing = User(
            id = 1,
            telegramId = 100,
            username = "test",
            firstName = "Test",
            lastName = null,
            createdAt = fixedInstant,
            updatedAt = fixedInstant
        )
        coEvery { userRepository.findByTelegramId(100) } returns existing

        val result = service.findOrCreateUser(100, "test", "Test", null)

        assertEquals(existing, result)
    }

    @Test
    fun `findOrCreateUser creates when missing`() = runTest {
        coEvery { userRepository.findByTelegramId(200) } returns null
        val slot = slot<User>()
        coEvery { userRepository.create(capture(slot)) } answers { slot.captured.copy(id = 9) }

        val result = service.findOrCreateUser(200, "newuser", "New", "User")

        assertEquals(9, result.id)
        assertEquals(200, slot.captured.telegramId)
        assertEquals("newuser", slot.captured.username)
        assertEquals(fixedInstant, slot.captured.createdAt)
        assertEquals(fixedInstant, slot.captured.updatedAt)
        assertTrue(slot.captured.createdAt == slot.captured.updatedAt)
        assertNotNull(slot.captured.createdAt)
    }
}
