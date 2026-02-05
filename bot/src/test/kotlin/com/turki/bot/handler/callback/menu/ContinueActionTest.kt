package com.turki.bot.handler.callback.menu

import com.turki.bot.util.sendHtml
import com.turki.bot.util.editOrSendHtml

import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.handler.coreUser
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.LessonService
import com.turki.bot.service.UserFlowState
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.core.domain.UserState
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ContinueActionTest {
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
    fun `user missing returns`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val lessonService = mockk<LessonService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 60L)

        coEvery { userService.findByTelegramId(60L) } returns null

        val action = ContinueAction(userService, userStateService, lessonService, analytics)
        action(context, query, listOf("continue"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `no state shows nothing to continue`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val lessonService = mockk<LessonService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val user = coreUser(id = 1L, telegramId = 61L)
        val query = mockCallbackQuery(telegramId = 61L)

        coEvery { userService.findByTelegramId(61L) } returns user
        coEvery { userStateService.get(user.id) } returns null

        val action = ContinueAction(userService, userStateService, lessonService, analytics)
        action(context, query, listOf("continue"))

        coVerify { context.editOrSendHtml(query, S.continueNothing, any()) }
    }

    @Test
    fun `dict search state sends prompt`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val lessonService = mockk<LessonService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val user = coreUser(id = 2L, telegramId = 62L)
        val query = mockCallbackQuery(telegramId = 62L)
        val state = UserState(
            userId = user.id,
            state = UserFlowState.DICT_SEARCH.name,
            payload = "{}",
            updatedAt = Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(62L) } returns user
        coEvery { userStateService.get(user.id) } returns state

        val action = ContinueAction(userService, userStateService, lessonService, analytics)
        action(context, query, listOf("continue"))

        coVerify { context.sendHtml(any(), S.dictionaryPrompt) }
    }

    @Test
    fun `homework text state sends prompt`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val lessonService = mockk<LessonService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val user = coreUser(id = 3L, telegramId = 63L)
        val query = mockCallbackQuery(telegramId = 63L)
        val state = UserState(
            userId = user.id,
            state = UserFlowState.HOMEWORK_TEXT.name,
            payload = "{}",
            updatedAt = Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(63L) } returns user
        coEvery { userStateService.get(user.id) } returns state

        val action = ContinueAction(userService, userStateService, lessonService, analytics)
        action(context, query, listOf("continue"))

        coVerify { context.sendHtml(any(), S.homeworkContinue) }
    }

    @Test
    fun `support state sends prompt`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val lessonService = mockk<LessonService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val user = coreUser(id = 4L, telegramId = 64L)
        val query = mockCallbackQuery(telegramId = 64L)
        val state = UserState(
            userId = user.id,
            state = UserFlowState.SUPPORT_MESSAGE.name,
            payload = "{}",
            updatedAt = Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(64L) } returns user
        coEvery { userStateService.get(user.id) } returns state

        val action = ContinueAction(userService, userStateService, lessonService, analytics)
        action(context, query, listOf("continue"))

        coVerify { context.sendHtml(any(), S.supportPrompt) }
    }

    @Test
    fun `unknown state clears and returns menu`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val lessonService = mockk<LessonService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val user = coreUser(id = 5L, telegramId = 65L)
        val query = mockCallbackQuery(telegramId = 65L)
        val state = UserState(
            userId = user.id,
            state = "UNKNOWN",
            payload = "{}",
            updatedAt = Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(65L) } returns user
        coEvery { userStateService.get(user.id) } returns state
        coEvery { userStateService.clear(user.id) } returns true

        val action = ContinueAction(userService, userStateService, lessonService, analytics)
        action(context, query, listOf("continue"))

        coVerify { context.editOrSendHtml(query, S.menuTitle, any()) }
    }
}
