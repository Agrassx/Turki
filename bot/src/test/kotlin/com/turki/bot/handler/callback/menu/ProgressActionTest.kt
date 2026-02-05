package com.turki.bot.handler.callback.menu

import com.turki.bot.util.sendHtml
import com.turki.bot.util.editOrSendHtml

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
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

class ProgressActionTest {
    private val context = mockk<BehaviourContext>(relaxed = true)

    @BeforeEach
    fun setUp() {
        mockkStatic("com.turki.bot.util.BotExtensionsKt")
        stubEditAndSendHtml(context)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `user missing sends warning`() = runBlocking {
        val userService = mockk<UserService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 90L)

        coEvery { userService.findByTelegramId(90L) } returns null

        val action = ProgressAction(userService, progressService, analytics)
        action(context, query, listOf("progress"))

        coVerify { context.sendHtml(any(), S.notRegistered) }
    }

    @Test
    fun `user exists sends progress`() = runBlocking {
        val userService = mockk<UserService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val user = coreUser(id = 1L, telegramId = 91L, subscriptionActive = true)
        val query = mockCallbackQuery(telegramId = 91L)
        val summary = ProgressSummary(completedLessons = 1, totalLessons = 10, currentLevel = "A1", currentStreak = 2)

        coEvery { userService.findByTelegramId(91L) } returns user
        coEvery { progressService.getProgressSummary(user.id) } returns summary

        val action = ProgressAction(userService, progressService, analytics)
        action(context, query, listOf("progress"))

        val expected = S.progress(
            firstName = user.firstName,
            completedLessons = summary.completedLessons,
            totalLessons = summary.totalLessons,
            subscriptionActive = true,
            currentLevel = summary.currentLevel,
            streakDays = summary.currentStreak
        )

        coVerify { context.editOrSendHtml(query, expected, any()) }
    }
}
