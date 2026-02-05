package com.turki.bot.handler.command.reminder

import com.turki.bot.util.sendHtml

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockMessage
import com.turki.bot.handler.mockTelegramUser
import com.turki.bot.handler.stubSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.ReminderPreferenceService
import com.turki.bot.service.UserService
import com.turki.core.domain.ReminderPreference
import kotlinx.datetime.Instant
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

class RemindersCommandTest {
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
        val reminderService = mockk<ReminderPreferenceService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 30L
        val message = mockMessage("/reminders", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns null

        val action = RemindersCommand(userService, reminderService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.notRegistered) }
    }

    @Test
    fun `disabled reminders shows off status`() = runBlocking {
        val userService = mockk<UserService>()
        val reminderService = mockk<ReminderPreferenceService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 31L
        val user = coreUser(id = 11L, telegramId = telegramId)
        val message = mockMessage("/reminders", mockTelegramUser(telegramId))
        val pref = ReminderPreference(
            userId = user.id,
            daysOfWeek = "",
            timeLocal = "10:00",
            isEnabled = false,
            lastFiredAt = null
        )

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { reminderService.getOrDefault(user.id) } returns pref

        val action = RemindersCommand(userService, reminderService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.reminderStatusOff, any()) }
    }

    @Test
    fun `enabled reminders shows on status`() = runBlocking {
        val userService = mockk<UserService>()
        val reminderService = mockk<ReminderPreferenceService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 32L
        val user = coreUser(id = 12L, telegramId = telegramId)
        val message = mockMessage("/reminders", mockTelegramUser(telegramId))
        val pref = ReminderPreference(
            userId = user.id,
            daysOfWeek = "1,2,3",
            timeLocal = "09:00",
            isEnabled = true,
            lastFiredAt = Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { reminderService.getOrDefault(user.id) } returns pref

        val action = RemindersCommand(userService, reminderService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.reminderStatusOn(pref.daysOfWeek, pref.timeLocal), any()) }
    }
}
