package com.turki.bot

import com.turki.bot.service.LessonService
import org.koin.java.KoinJavaComponent.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("SeedData")
private val lessonService: LessonService by inject(LessonService::class.java)

/**
 * Seeds initial lesson data from CSV files if no lessons exist yet.
 *
 * Delegates to [ImportData.importAll] which reads `data/lessons.csv`,
 * `data/vocabulary.csv`, and `data/homework.csv`.
 */
suspend fun seedInitialData() {
    val existingLessons = lessonService.getAllLessons()

    if (existingLessons.isEmpty()) {
        logger.info("No lessons found — importing initial data from CSV files")
        ImportData.importAll(skipDbInit = true)
        return
    }

    logger.info("Found ${existingLessons.size} existing lessons — skipping seed")
}
