package com.turki.bot

import java.io.File

object EnvLoader {

    private val env = mutableMapOf<String, String>()

    fun load() {
        val envFile = findEnvFile()
        if (envFile != null && envFile.exists()) {
            println("📄 Загружаем .env из: ${envFile.absolutePath}")
            envFile.readLines()
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .forEach { line ->
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim().removeSurrounding("\"").removeSurrounding("'")
                        env[key] = value
                    }
                }
        } else {
            println("⚠️ Файл .env не найден, используем переменные окружения системы")
        }
    }

    fun get(key: String, default: String? = null): String? {
        return env[key] ?: System.getenv(key) ?: default
    }

    fun require(key: String): String {
        return get(key)
            ?: error("❌ Переменная окружения $key не задана! Создайте .env файл или экспортируйте переменную.")
    }

    private fun findEnvFile(): File? {
        val currentDir = File(System.getProperty("user.dir"))
        val candidates = listOf(
            File(currentDir, ".env"),
            File(currentDir.parentFile, ".env"),
            File(System.getProperty("user.home"), "IdeaProjects/Turki/.env")
        )
        return candidates.firstOrNull { it.exists() }
    }
}
