package com.turki.bot.handler.command.menu

import com.turki.bot.util.sendHtml

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockMessage
import com.turki.bot.handler.mockTelegramUser
import com.turki.bot.handler.stubSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.ProgressSummary
import com.turki.bot.service.UserService
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProgressCommandTest {
    private val context = mockk<BehaviourContext>(relaxed = true)

    @BeforeEach
    fun setUp() {
        mockkStatic("com.turki.bot.util.BotExtensionsKt")
        mockkStatic("dev.inmo.tgbotapi.extensions.utils.extensions.raw.MessageKt")
        stubSendHtml(context)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `user not registered sends warning`() = runBlocking {
        val userService = mockk<UserService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 12L
        val message = mockMessage("/progress", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns null

        val action = ProgressCommand(userService, progressService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.notRegistered) }
    }

    @Test
    fun `progress uses subscription state`() = runBlocking {
        val userService = mockk<UserService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 13L
        val user = coreUser(id = 4L, telegramId = telegramId, subscriptionActive = true)
        val message = mockMessage("/progress", mockTelegramUser(telegramId))
        val summary = ProgressSummary(completedLessons = 1, totalLessons = 10, currentLevel = "A1", currentStreak = 2)

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { progressService.getProgressSummary(user.id) } returns summary

        val action = ProgressCommand(userService, progressService, analytics)
        action(context, message)

        val expected = S.progress(
            firstName = user.firstName,
            completedLessons = summary.completedLessons,
            totalLessons = summary.totalLessons,
            subscriptionActive = true,
            currentLevel = summary.currentLevel,
            streakDays = summary.currentStreak
        )

        coVerify { context.sendHtml(any(), expected) }
    }
}
