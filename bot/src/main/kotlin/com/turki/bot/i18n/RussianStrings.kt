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
/menu — Главное меню
/lessons — Список уроков
/practice — Практика
/dictionary — Поиск слов
/review — Повторение
/lesson — Текущий урок
/homework — Домашнее задание
/vocabulary — Словарь текущего урока
/progress — Ваш прогресс
/reminders — Напоминания
/help — Справка

💡 <b>Как работает обучение:</b>
1. Изучите урок и словарь
2. Выполните домашнее задание
3. После успешного выполнения откроется следующий урок

📬 <b>Поддержка:</b>
/support — Написать в поддержку

🔐 <b>Управление данными:</b>
/export — Экспортировать ваши данные
/delete — Удалить все ваши данные

📄 <a href="https://turki.bot/privacy">Политика конфиденциальности</a>

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
        subscriptionActive: Boolean,
        currentLevel: String,
        streakDays: Int
    ): String {
        val progressBar = buildProgressBar(completedLessons, totalLessons)
        val subscriptionStatus = if (subscriptionActive) "✅ Активна" else "ℹ️ Можно улучшить"
        return """
📊 <b>Ваш прогресс, $firstName</b>

Уроков пройдено: $completedLessons из $totalLessons
$progressBar

Уровень: $currentLevel
Серия дней: $streakDays 🔥
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

Можно начать заново с урока 1.
    """.trim()

    override val deleteDataConfirm = """
⚠️ <b>Удалить все данные?</b>

Это действие удалит ваш прогресс, словарь, повторения и историю занятий.
    """.trim()

    override val deleteDataSuccess = """
✅ <b>Данные удалены.</b>

Чтобы начать заново, отправьте /start.
    """.trim()

    override val exportDataPreparing = "⏳ Подготавливаю экспорт ваших данных..."

    override val exportDataReady = """
📦 <b>Ваши данные готовы!</b>

Файл содержит:
• Профиль и настройки
• Прогресс по урокам
• Словарь
• Результаты домашних заданий
• Статистику занятий

Файл в формате JSON — его можно открыть в любом текстовом редакторе.
    """.trim()

    override val exportDataEmpty = "У вас пока нет данных для экспорта. Начните с /start"

    override val homeworkFeedbackPerfect = "✨ Отлично! Ошибок нет."

    override fun homeworkFeedbackSummary(details: String, wrongCount: Int) = """
Вот что можно улучшить:
$details

Ошибок: $wrongCount
    """.trim()

    override fun homeworkCorrectAnswer(answer: String) = "Правильно: <b>$answer</b>"

    override val homeworkNoNext = "Пока нет следующей домашки — вы на последнем уроке."

    override val homeworkContinue = "Продолжим домашку. Ответьте на следующий вопрос."

    override val lessonIntro = "Начинаем урок!"

    override fun lessonIntroTitle(orderIndex: Int, title: String) =
        "👋 <b>Урок $orderIndex: $title</b>\n\nСначала теория, затем упражнение. Домашка — отдельная проверка."

    override val lessonsTitle = "📚 <b>Уроки</b>"

    override val practiceIntro = "🧩 <b>Практика</b>\n\nНебольшая разминка перед уроком."

    override val practicePrompt = "Готовы? Жмите «Начать практику»."

    override val exerciseNotReady = "Для этого урока пока нет упражнений."

    override fun exercisePrompt(word: String) = "🧩 <b>Упражнение</b>\nПереведи слово: <b>$word</b>"

    override val exerciseCorrect = "✅ Верно!"

    override val exerciseIncorrect = "❌ Неверно."

    override val exerciseComplete = "Готово на сегодня! Продолжим?"

    override val reviewIntro = "🔁 <b>Повторение</b>\n\nКороткая ежедневная серия карточек."

    override val reviewEmpty = "Пока нечего повторять. Добавьте слова в словарь."

    override val reviewDone = "🎉 <b>Отлично!</b>\n\nПовторение завершено. До следующего раза! 👋"

    override val reviewSelectDifficulty = """
🔁 <b>Повторение</b>

Выберите режим тренировки:
    """.trim()

    override val reviewDifficultyWarmup = "☕ Разминка (10 вопросов)"
    override val reviewDifficultyTraining = "💪 Тренировка (20 вопросов)"
    override val reviewDifficultyMarathon = "🔥 Марафон (30 вопросов)"

    override fun reviewProgress(current: Int, total: Int) = "Вопрос $current из $total"

    override fun reviewCardTitle(word: String) = "Карточка: <b>$word</b>"

    override fun reviewCardTranslation(translation: String) = "Перевод: $translation"

    override val reviewTranslateToTurkish = "🇹🇷 Переведите на турецкий:"
    override val reviewTranslateToRussian = "🇷🇺 Переведите на русский:"

    override val dictionaryPrompt = "Введите слово или перевод для поиска."

    override val dictionaryEmpty = "Словарь пуст. Добавьте слова, чтобы было что повторять."

    override val dictionaryAddPrompt = "Введите слово и перевод через тире, например: Merhaba - Привет"

    override val dictionaryAddFormatError = "Не понял формат. Напишите так: слово - перевод"

    override val dictionaryNoResults = "Пока не нашёл это слово. Попробуйте другой запрос."

    override fun dictionaryCardTitle(word: String, translation: String) =
        "📖 <b>$word</b> — $translation"

    override fun dictionaryPronunciation(pronunciation: String) =
        "  🔊 <i>[$pronunciation]</i>"

    override fun dictionaryExample(example: String) =
        "  📝 <i>$example</i>"

    override fun dictionaryTags(tags: String) = "Теги: $tags"

    override val dictionaryTagsEmpty = "нет тегов"

    override val dictionaryTagPrompt = "Выберите теги для слова:"

    override fun dictionaryTagsUpdated(tags: String) = "Теги обновлены: $tags"

    override val dictionaryFavorited = "Сохранено в словарь ⭐️"

    override val dictionaryUnfavorited = "Удалено из избранного"

    override fun dictionaryAddedAll(count: Int) = "Добавлено в словарь: $count"

    override val reminderStatusOff = "Напоминания выключены."

    override fun reminderStatusOn(days: String, time: String) =
        "Напоминания включены: $days в $time"

    override fun reminderEnabled(days: String, time: String) =
        "Готово! Буду напоминать: $days в $time."

    override val reminderDisabled = "Ок, напоминания выключены."

    override val reminderSelectFrequency = """
⏰ <b>Настройка напоминаний</b>

Как часто напоминать о занятиях?
    """.trim()

    override val reminderFrequencyDaily = "🌟 Каждый день"
    override val reminderFrequency1x = "1️⃣ Раз в неделю"
    override val reminderFrequency2x = "2️⃣ Два раза в неделю"
    override val reminderFrequency3x = "3️⃣ Три раза в неделю"
    override val reminderFrequency4x = "4️⃣ Четыре раза в неделю"

    override val reminderSelectDays = """
📅 <b>Выберите дни недели</b>

Нажмите на дни, когда хотите получать напоминания.
Выбранные дни отмечены ✅
    """.trim()

    override val reminderSelectTime = """
🕐 <b>Выберите время</b>

В какое время дня напоминать?
    """.trim()

    override val reminderTimeMorning = "🌅 Утро (08:00)"
    override val reminderTimeDay = "☀️ День (14:00)"
    override val reminderTimeEvening = "🌆 Вечер (20:00)"
    override val reminderTimeNight = "🌙 Ночь (00:00)"

    override fun reminderDaysSelected(count: Int, needed: Int) =
        "Выбрано: $count из $needed"

    override fun weeklyReport(lessons: Int, practice: Int, review: Int, homework: Int) = """
📈 <b>Недельный отчёт</b>

Уроки: $lessons
Практика: $practice
Повторение: $review
Домашки: $homework

Продолжим?
    """.trim()

    override val menuTitle = "🏠 <b>Меню</b>"

    override val continueNothing = "Пока нет активного занятия. Выберите пункт меню."

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
    override val btnLesson = "Урок"
    override val btnContinue = "▶️ Продолжить"
    override val btnHomework = "📝 Домашнее задание"
    override val btnProgress = "📊 Мой прогресс"
    override val btnLessons = "📚 Уроки"
    override val btnPractice = "🧩 Практика"
    override val btnDictionary = "📖 Словарь"
    override val btnReview = "🔁 Повторение"
    override val btnReminders = "⏰ Напоминания"
    override val btnHelp = "❓ Помощь"
    override val btnSelectLevel = "🎯 Уровень"
    override val btnKnowledgeTest = "📋 Тест"
    override val btnSettings = "⚙️ Настройки"
    override val btnVocabulary = "📖 Словарь урока"
    override val btnGoToHomework = "📝 Перейти к заданию"
    override val btnStartPractice = "🧩 Начать практику"
    override val btnStartReview = "🔁 Начать повторение"
    override val btnSetReminder = "⏰ Напомнить о занятии"
    override val btnStartHomework = "📝 Начать домашнее задание"
    override val btnNextLesson = "➡️ Следующий урок"
    override val btnNext = "Дальше"
    override val btnRemember = "Помню"
    override val btnAgain = "Повторить"
    override val btnRepeatTopic = "Повторить тему"
    override val btnNextHomework = "Следующая домашка"
    override val btnEditTags = "Теги"
    override val btnAddToDictionary = "➕ В словарь"
    override val btnAddCustomWord = "➕ Добавить своё слово"
    override val btnAddAllToDictionary = "➕ Добавить все слова"
    override val btnRemoveFromDictionary = "🗑️ Удалить из словаря"
    override val btnEnableWeekdays = "Включить будние дни 19:00"
    override val btnDisableReminders = "Выключить"
    override val btnConfigureReminders = "⚙️ Настроить"
    override val btnMon = "Понедельник"
    override val btnTue = "Вторник"
    override val btnWed = "Среда"
    override val btnThu = "Четверг"
    override val btnFri = "Пятница"
    override val btnSat = "Суббота"
    override val btnSun = "Воскресенье"
    override val btnConfirmDays = "✅ Готово"
    override val btnTryAgain = "🔄 Попробовать снова"
    override val btnResetProgress = "🔄 Сбросить прогресс"
    override val btnBackToMenu = "🔙 Назад в меню"
    override val btnConfirmReset = "✅ Да, сбросить"
    override val btnCancel = "❌ Отмена"
    override val btnConfirmDelete = "🗑️ Да, удалить"
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

    // Support
    override val supportPrompt = """
📬 <b>Служба поддержки</b>

Напишите ваш вопрос или предложение — я передам его разработчику.

Отправьте сообщение:
    """.trim()

    override val supportSent = """
✅ <b>Сообщение отправлено!</b>

Спасибо за обращение. Мы ответим вам в ближайшее время.
    """.trim()

    override val supportReply = """
📬 <b>Ответ от поддержки:</b>

    """.trim()

    override fun supportMessageToAdmin(userId: Long, username: String?, firstName: String, message: String): String {
        val userInfo = if (username != null) "@$username" else "ID: $userId"
        return """
📬 <b>Новое обращение в поддержку</b>

👤 <b>От:</b> $firstName ($userInfo)
🆔 <code>$userId</code>

💬 <b>Сообщение:</b>
$message

<i>Ответьте на это сообщение, чтобы отправить ответ пользователю.</i>
        """.trim()
    }
}
