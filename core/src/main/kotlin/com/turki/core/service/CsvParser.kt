package com.turki.core.service

interface CsvParser {
    fun parse(content: String): List<List<String>>
}

class DefaultCsvParser : CsvParser {
    override fun parse(content: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        val currentRecord = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < content.length) {
            val char = content[i]
            when {
                char == '"' -> {
                    if (inQuotes && i + 1 < content.length && content[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    currentRecord.add(currentField.toString())
                    currentField.clear()
                }
                (char == '\n' || char == '\r') && !inQuotes -> {
                    currentRecord.add(currentField.toString())
                    currentField.clear()
                    if (currentRecord.isNotEmpty()) {
                        records.add(currentRecord.toList())
                        currentRecord.clear()
                    }
                    if (char == '\r' && i + 1 < content.length && content[i + 1] == '\n') {
                        i++
                    }
                }
                else -> currentField.append(char)
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRecord.isNotEmpty()) {
            currentRecord.add(currentField.toString())
            records.add(currentRecord.toList())
        }

        return records
    }
}
