package com.turki.bot

import com.turki.bot.service.HomeworkService
import com.turki.bot.service.LessonService
import com.turki.core.domain.Homework
import com.turki.core.domain.HomeworkQuestion
import com.turki.core.domain.Language
import com.turki.core.domain.Lesson
import com.turki.core.domain.QuestionType
import com.turki.core.domain.VocabularyItem
import org.koin.java.KoinJavaComponent.inject

private val lessonService: LessonService by inject(LessonService::class.java)
private val homeworkService: HomeworkService by inject(HomeworkService::class.java)

suspend fun seedInitialData() {
    val existingLessons = lessonService.getAllLessons()
    if (existingLessons.isNotEmpty()) return

    val lessons = listOf(
        createLesson1(),
        createLesson2(),
        createLesson3()
    )

    lessons.forEach { (lesson, homework) ->
        val createdLesson = lessonService.createLesson(lesson)
        val homeworkWithLessonId = homework.copy(lessonId = createdLesson.id)
        homeworkService.createHomework(homeworkWithLessonId)
    }
}

private fun createLesson1(): Pair<Lesson, Homework> {
    val lesson = Lesson(
        id = 0,
        orderIndex = 1,
        targetLanguage = Language.TURKISH,
        title = "Merhaba! - Приветствие и знакомство",
        description = """
            |В этом уроке вы научитесь:
            |• Приветствовать людей по-турецки
            |• Представляться
            |• Спрашивать имя и отвечать на этот вопрос
        """.trimMargin(),
        content = """
            |🗣️ *Приветствия*
            |
            |*Merhaba!* — Привет! Здравствуйте!
            |*Selam!* — Привет! (неформально)
            |*Günaydın!* — Доброе утро!
            |*İyi akşamlar!* — Добрый вечер!
            |*İyi geceler!* — Спокойной ночи!
            |
            |🤝 *Знакомство*
            |
            |*Benim adım...* — Меня зовут...
            |*Senin adın ne?* — Как тебя зовут? (неформально)
            |*Sizin adınız ne?* — Как вас зовут? (формально)
            |
            |*Memnun oldum!* — Приятно познакомиться!
            |*Ben de memnun oldum!* — Мне тоже приятно!
            |
            |📝 *Пример диалога*
            |
            |— Merhaba! Benim adım Ali. Senin adın ne?
            |— Merhaba Ali! Benim adım Maria. Memnun oldum!
            |— Ben de memnun oldum, Maria!
        """.trimMargin(),
        vocabularyItems = listOf(
            VocabularyItem(0, 0, "Merhaba", "Привет, Здравствуйте", "мэр-ха-БА", "Merhaba, nasılsın?"),
            VocabularyItem(0, 0, "Selam", "Привет", "сэ-ЛЯМ", "Selam, ne yapıyorsun?"),
            VocabularyItem(0, 0, "Günaydın", "Доброе утро", "гю-най-ДЫН", "Günaydın, iyi uykular mı?"),
            VocabularyItem(0, 0, "İyi akşamlar", "Добрый вечер", "и-ЙИ ак-шам-ЛАР"),
            VocabularyItem(0, 0, "ad", "имя", "ад", "Benim adım Ali."),
            VocabularyItem(0, 0, "Memnun oldum", "Приятно познакомиться", "мэм-НУН ол-ДУМ")
        )
    )

    val homework = Homework(
        id = 0,
        lessonId = 0,
        questions = listOf(
            HomeworkQuestion(
                id = 0,
                homeworkId = 0,
                questionType = QuestionType.MULTIPLE_CHOICE,
                questionText = "Как сказать 'Привет' по-турецки?",
                options = listOf("Günaydın", "Merhaba", "İyi geceler", "Teşekkürler"),
                correctAnswer = "Merhaba"
            ),
            HomeworkQuestion(
                id = 0,
                homeworkId = 0,
                questionType = QuestionType.MULTIPLE_CHOICE,
                questionText = "Что означает 'Günaydın'?",
                options = listOf("Спокойной ночи", "Добрый вечер", "Доброе утро", "До свидания"),
                correctAnswer = "Доброе утро"
            ),
            HomeworkQuestion(
                id = 0,
                homeworkId = 0,
                questionType = QuestionType.TRANSLATION,
                questionText = "Переведите на турецкий: 'Меня зовут...' (начните с 'Benim')",
                correctAnswer = "Benim adım"
            )
        )
    )

    return lesson to homework
}

private fun createLesson2(): Pair<Lesson, Homework> {
    val lesson = Lesson(
        id = 0,
        orderIndex = 2,
        targetLanguage = Language.TURKISH,
        title = "Neredesin? - Где ты?",
        description = """
            |В этом уроке вы научитесь:
            |• Спрашивать и указывать местоположение
            |• Использовать указательные местоимения
            |• Числа от 1 до 10
        """.trimMargin(),
        content = """
            |📍 *Местоположение*
            |
            |*Nerede?* — Где?
            |*Burası* — Здесь, это место
            |*Şurası* — Там (недалеко)
            |*Orası* — Там (далеко)
            |
            |*Neredesin?* — Где ты?
            |*Neredesiniz?* — Где вы?
            |
            |🏫 *Места*
            |
            |*okul* — школа
            |*ev* — дом
            |*ofis* — офис
            |*cadde* — улица
            |*park* — парк
            |
            |🔢 *Числа 1-10*
            |
            |1 — bir
            |2 — iki
            |3 — üç
            |4 — dört
            |5 — beş
            |6 — altı
            |7 — yedi
            |8 — sekiz
            |9 — dokuz
            |10 — on
            |
            |📝 *Пример*
            |
            |— Neredesin?
            |— Ben okulda. Sen neredesin?
            |— Ben evdeyim.
        """.trimMargin(),
        vocabularyItems = listOf(
            VocabularyItem(0, 0, "nerede", "где", "нэ-рэ-ДЭ", "Kitap nerede?"),
            VocabularyItem(0, 0, "burası", "здесь, это место", "бу-ра-СЫ"),
            VocabularyItem(0, 0, "okul", "школа", "о-КУЛ", "Ben okulda."),
            VocabularyItem(0, 0, "ev", "дом", "эв", "Bu benim evim."),
            VocabularyItem(0, 0, "bir", "один", "бир"),
            VocabularyItem(0, 0, "iki", "два", "и-КИ"),
            VocabularyItem(0, 0, "üç", "три", "юч")
        )
    )

    val homework = Homework(
        id = 0,
        lessonId = 0,
        questions = listOf(
            HomeworkQuestion(
                id = 0,
                homeworkId = 0,
                questionType = QuestionType.MULTIPLE_CHOICE,
                questionText = "Как сказать 'Где?' по-турецки?",
                options = listOf("Ne?", "Kim?", "Nerede?", "Nasıl?"),
                correctAnswer = "Nerede?"
            ),
            HomeworkQuestion(
                id = 0,
                homeworkId = 0,
                questionType = QuestionType.MULTIPLE_CHOICE,
                questionText = "Как будет '5' по-турецки?",
                options = listOf("üç", "dört", "beş", "altı"),
                correctAnswer = "beş"
            ),
            HomeworkQuestion(
                id = 0,
                homeworkId = 0,
                questionType = QuestionType.TEXT_INPUT,
                questionText = "Напишите по-турецки число '3'",
                correctAnswer = "üç"
            )
        )
    )

    return lesson to homework
}

private fun createLesson3(): Pair<Lesson, Homework> {
    val lesson = Lesson(
        id = 0,
        orderIndex = 3,
        targetLanguage = Language.TURKISH,
        title = "Ne yapıyorsun? - Что ты делаешь?",
        description = """
            |В этом уроке вы научитесь:
            |• Спрашивать о действиях
            |• Использовать настоящее время
            |• Описывать свой день
        """.trimMargin(),
        content = """
            |⏰ *Настоящее время (Şimdiki zaman)*
            |
            |Для образования настоящего времени добавляем *-ıyor/-iyor/-uyor/-üyor* к основе глагола.
            |
            |*yapmak* (делать) → *yapıyorum* (я делаю)
            |*gelmek* (приходить) → *geliyorum* (я прихожу)
            |*okumak* (читать) → *okuyorum* (я читаю)
            |
            |❓ *Вопросы*
            |
            |*Ne yapıyorsun?* — Что ты делаешь?
            |*Ne yapıyorsunuz?* — Что вы делаете?
            |
            |🌅 *Распорядок дня*
            |
            |*kalkmak* — вставать
            |*kahvaltı yapmak* — завтракать
            |*çalışmak* — работать
            |*yemek yemek* — есть, кушать
            |*uyumak* — спать
            |
            |📝 *Пример диалога*
            |
            |— Günaydın! Ne yapıyorsun?
            |— Günaydın! Kahvaltı yapıyorum. Sen?
            |— Ben çalışıyorum.
        """.trimMargin(),
        vocabularyItems = listOf(
            VocabularyItem(0, 0, "yapmak", "делать", "яп-МАК", "Ne yapıyorsun?"),
            VocabularyItem(0, 0, "gelmek", "приходить", "гэль-МЭК", "Eve geliyorum."),
            VocabularyItem(0, 0, "okumak", "читать", "о-ку-МАК", "Kitap okuyorum."),
            VocabularyItem(0, 0, "çalışmak", "работать", "ча-лыш-МАК", "Ofiste çalışıyorum."),
            VocabularyItem(0, 0, "uyumak", "спать", "у-ю-МАК", "Gece uyuyorum."),
            VocabularyItem(0, 0, "kahvaltı", "завтрак", "ках-вал-ТЫ")
        )
    )

    val homework = Homework(
        id = 0,
        lessonId = 0,
        questions = listOf(
            HomeworkQuestion(
                id = 0,
                homeworkId = 0,
                questionType = QuestionType.MULTIPLE_CHOICE,
                questionText = "Как сказать 'Что ты делаешь?' по-турецки?",
                options = listOf("Neredesin?", "Ne yapıyorsun?", "Nasılsın?", "Kim?"),
                correctAnswer = "Ne yapıyorsun?"
            ),
            HomeworkQuestion(
                id = 0,
                homeworkId = 0,
                questionType = QuestionType.MULTIPLE_CHOICE,
                questionText = "Что означает 'okumak'?",
                options = listOf("спать", "работать", "читать", "есть"),
                correctAnswer = "читать"
            ),
            HomeworkQuestion(
                id = 0,
                homeworkId = 0,
                questionType = QuestionType.TRANSLATION,
                questionText = "Переведите: 'Я работаю' (используйте глагол çalışmak)",
                correctAnswer = "çalışıyorum"
            )
        )
    )

    return lesson to homework
}
