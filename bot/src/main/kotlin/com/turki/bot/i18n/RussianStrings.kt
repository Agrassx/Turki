package com.turki.bot.i18n

object RussianStrings : Strings {
    override fun welcome(firstName: String) = """
Merhaba, $firstName! 👋

<b>Добро пожаловать в бот для изучения турецкого языка!</b> 🇹🇷

Здесь вы сможете:
📚 Изучать уроки турецкого языка
📝 Выполнять домашние задания
📖 Учить новые слова
📊 Отслеживать свой прогресс

Выберите действие ниже, чтобы начать!
    """.trim()
    override val notRegistered = "Вы ещё не зарегистрированы. Пожалуйста, отправьте команду /start"

    override val lessonNotFound = "Урок не найден. Попробуйте начать с начала командой /lesson"

    override val allLessonsCompleted = """
🎉 <b>Поздравляем!</b> Вы завершили все доступные уроки!

Следите за обновлениями — скоро появятся новые уроки.
    """.trim()

    override val homeworkStart = """
📝 <b>Домашнее задание</b>

Готовы проверить свои знания?

Ответьте на вопросы, чтобы закрепить материал урока.
Для перехода к следующему уроку нужно правильно ответить на все вопросы.
    """.trim()

    override val help = """
📚 <b>Команды бота:</b>

/start — Начать работу с ботом
/lesson — Текущий урок
/homework — Домашнее задание
/vocabulary — Словарь текущего урока
/progress — Ваш прогресс
/help — Справка

💡 <b>Как работает обучение:</b>
1. Изучите урок и словарь
2. Выполните домашнее задание
3. После успешного выполнения откроется следующий урок

Удачи в изучении турецкого языка! 🇹🇷
    """.trim()

    override val reminderSet = """
⏰ <b>Напоминание установлено!</b>

Я напомню вам о занятии через 24 часа.
    """.trim()

    override fun lessonTitle(orderIndex: Int, title: String) =
        "📚 <b>Урок $orderIndex: $title</b>"

    override val vocabularyTitle = "📖 <b>Словарь урока</b>"

    override fun vocabularyForLesson(lessonId: Int) =
        "📖 <b>Словарь урока $lessonId</b>"

    override val vocabularyEmpty = "Словарь для этого урока пока пуст."

    override fun vocabularyItem(word: String, translation: String) =
        "• <b>$word</b> — $translation"

    override fun vocabularyPronunciation(pronunciation: String) =
        "  🔊 <i>[$pronunciation]</i>"

    override fun vocabularyExample(example: String) =
        "  📝 <i>$example</i>"

    override val homeworkNotReady = "Домашнее задание для этого урока пока не готово."

    override val homeworkAlreadyCompleted = "Вы уже выполнили это задание! ✅"

    override fun questionTitle(index: Int) = "❓ <b>Вопрос $index</b>"

    override val writeYourAnswer = "Напишите ваш ответ:"
    
    override fun homeworkComplete(score: Int, maxScore: Int) = """
🎉 <b>Отлично!</b>

Вы правильно ответили на все вопросы!
Результат: $score/$maxScore ✅

Теперь вы можете перейти к следующему уроку.
    """.trim()

    override fun homeworkResult(score: Int, maxScore: Int) = """
📝 <b>Результат домашнего задания</b>

Правильных ответов: $score из $maxScore

Для перехода к следующему уроку необходимо ответить правильно на все вопросы.
Попробуйте ещё раз!
    """.trim()

    override fun progress(
        firstName: String,
        completedLessons: Int,
        totalLessons: Int,
        subscriptionActive: Boolean
    ): String {
        val progressBar = buildProgressBar(completedLessons, totalLessons)
        val subscriptionStatus = if (subscriptionActive) "✅ Активна" else "❌ Неактивна"
        return """
📊 <b>Ваш прогресс, $firstName</b>

Уроков пройдено: $completedLessons из $totalLessons
$progressBar

Подписка: $subscriptionStatus
        """.trim()
    }

    private fun buildProgressBar(completed: Int, total: Int): String {
        if (total == 0) {
            return "▱▱▱▱▱▱▱▱▱▱ 0%"
        }
        val percentage = (completed * 100) / total
        val filled = (completed * 10) / total
        val empty = 10 - filled
        return "▰".repeat(filled) + "▱".repeat(empty) + " $percentage%"
    }
    override val settingsTitle = """
⚙️ <b>Настройки</b>

Выберите действие:
    """.trim()

    override val resetProgressConfirm = """
⚠️ <b>Вы уверены?</b>

Весь ваш прогресс будет сброшен:
• Текущий урок станет первым
• Все выполненные задания будут удалены
    """.trim()

    override val progressResetSuccess = """
✅ <b>Прогресс успешно сброшен!</b>

Теперь вы можете начать обучение заново.
    """.trim()

    override val selectLevelTitle = """
🎯 <b>Выбор уровня</b>

Выберите ваш уровень владения турецким языком:

• <b>A1</b> — Начальный
• <b>A2</b> — Элементарный
• <b>B1</b> — Средний
• <b>B2</b> — Выше среднего
    """.trim()

    override val levelA1Active = "✅ Уровень A1 уже активен! Это ваш текущий уровень обучения."

    override fun levelLocked(level: String) = """
🔒 Уровень $level пока недоступен.

Сначала завершите уровень A1, чтобы разблокировать следующие уровни.
    """.trim()

    override val knowledgeTestTitle = """
📋 <b>Тест на определение уровня</b>

🚧 Эта функция находится в разработке.

Скоро вы сможете пройти тест и определить свой уровень владения турецким языком!
    """.trim()

    override val mainMenuTitle = """
🏠 <b>Главное меню</b>

Выберите действие:
    """.trim()
    override val btnStartLesson = "📚 Начать урок"
    override val btnHomework = "📝 Домашнее задание"
    override val btnProgress = "📊 Мой прогресс"
    override val btnSelectLevel = "🎯 Уровень"
    override val btnKnowledgeTest = "📋 Тест"
    override val btnSettings = "⚙️ Настройки"
    override val btnVocabulary = "📖 Словарь урока"
    override val btnGoToHomework = "📝 Перейти к заданию"
    override val btnSetReminder = "⏰ Напомнить о занятии"
    override val btnStartHomework = "📝 Начать домашнее задание"
    override val btnNextLesson = "➡️ Следующий урок"
    override val btnTryAgain = "🔄 Попробовать снова"
    override val btnResetProgress = "🔄 Сбросить прогресс"
    override val btnBackToMenu = "🔙 Назад в меню"
    override val btnConfirmReset = "✅ Да, сбросить"
    override val btnCancel = "❌ Отмена"
    override val btnBack = "🔙 Назад"
    override val btnContinueLesson = "📚 Продолжить урок"

    override fun btnLevelWithStatus(level: String, isActive: Boolean) =
        if (isActive) "$level ✅" else "$level 🔒"
    override val reminderLesson = """
⏰ <b>Напоминание о занятии!</b>

Пора продолжить изучение турецкого языка!

Отправьте /lesson чтобы продолжить обучение.
    """.trim()

    override val reminderHomework = """
📝 <b>Напоминание о домашнем задании!</b>

Не забудьте выполнить домашнее задание.

Отправьте /homework чтобы начать.
    """.trim()

    override val reminderSubscription = """
⚠️ <b>Ваша подписка скоро заканчивается!</b>

Продлите подписку, чтобы продолжить обучение.
    """.trim()
}
