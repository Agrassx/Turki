package com.turki.bot.service

import com.turki.core.domain.Homework
import com.turki.core.domain.HomeworkQuestion
import com.turki.core.domain.QuestionType
import com.turki.core.repository.HomeworkRepository
import com.turki.core.repository.UserRepository
import com.turki.bot.testutil.TestClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeworkServiceTest {
    private val homeworkRepository = mockk<HomeworkRepository>()
    private val userRepository = mockk<UserRepository>()
    private val fixedInstant = Instant.fromEpochMilliseconds(1_000)
    private val clock = TestClock(fixedInstant)
    private val service = HomeworkService(homeworkRepository, userRepository, clock)

    @Test
    fun `isAnswerCorrect handles exact and normalized matches`() {
        val question = HomeworkQuestion(
            id = 1,
            homeworkId = 1,
            questionType = QuestionType.MULTIPLE_CHOICE,
            questionText = "Переведите: Привет",
            options = listOf("Merhaba"),
            correctAnswer = "Merhaba"
        )

        assertTrue(service.isAnswerCorrect(question, "Merhaba"))
        assertTrue(service.isAnswerCorrect(question, "  merhaba!  "))
        assertFalse(service.isAnswerCorrect(question, ""))
        assertFalse(service.isAnswerCorrect(question, null))
    }

    @Test
    fun `isAnswerCorrect allows flexible patterns for text input`() {
        val question = HomeworkQuestion(
            id = 2,
            homeworkId = 1,
            questionType = QuestionType.TEXT_INPUT,
            questionText = "Как по-турецки: Меня зовут ...",
            options = emptyList(),
            correctAnswer = "Adım Maria"
        )

        assertTrue(service.isAnswerCorrect(question, "Benim adım Ali"))
        assertTrue(service.isAnswerCorrect(question, "Adım Ahmet"))
    }

    @Test
    fun `isAnswerCorrect allows prefix when prompt says start with`() {
        val question = HomeworkQuestion(
            id = 3,
            homeworkId = 1,
            questionType = QuestionType.TRANSLATION,
            questionText = "Начните с: Merhaba",
            options = emptyList(),
            correctAnswer = "Merhaba"
        )

        assertTrue(service.isAnswerCorrect(question, "Merhaba! Ben Ali"))
    }

    @Test
    fun `submitHomework calculates score and advances lesson on perfect score`() = runTest {
        val questions = listOf(
            HomeworkQuestion(1, 1, QuestionType.MULTIPLE_CHOICE, "Q1", listOf("A"), "A"),
            HomeworkQuestion(2, 1, QuestionType.TEXT_INPUT, "Как вас зовут?", emptyList(), "Adım Ali")
        )
        val homework = Homework(id = 1, lessonId = 5, questions = questions)

        coEvery { homeworkRepository.findById(1) } returns homework
        val submissionSlot = slot<com.turki.core.domain.HomeworkSubmission>()
        coEvery { homeworkRepository.createSubmission(capture(submissionSlot)) } answers { submissionSlot.captured.copy(id = 10) }
        coEvery { userRepository.findById(7) } returns com.turki.core.domain.User(
            id = 7,
            telegramId = 100L,
            username = null,
            firstName = "Test",
            lastName = null,
            language = com.turki.core.domain.Language.TURKISH,
            subscriptionActive = false,
            subscriptionExpiresAt = null,
            currentLessonId = 5,
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now()
        )
        coEvery { userRepository.updateCurrentLesson(7, 6) } returns true

        val result = service.submitHomework(
            userId = 7,
            homeworkId = 1,
            answers = mapOf(1 to "A", 2 to "Adım Mehmet")
        )

        assertEquals(10, result.id)
        assertEquals(2, submissionSlot.captured.score)
        assertEquals(2, submissionSlot.captured.maxScore)
        assertEquals(fixedInstant, submissionSlot.captured.submittedAt)
        coVerify { userRepository.updateCurrentLesson(7, 6) }
    }
}
