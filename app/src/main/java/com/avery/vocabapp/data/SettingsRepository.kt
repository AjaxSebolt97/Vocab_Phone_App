package com.avery.vocabapp.data

data class StudySettings(
    val newWordsPerDay: Int = DEFAULT_NEW_WORDS_PER_DAY,
    val dailyReviewCap: Int = DEFAULT_DAILY_REVIEW_CAP,
) {
    companion object {
        const val DEFAULT_NEW_WORDS_PER_DAY = 15
        const val DEFAULT_DAILY_REVIEW_CAP = 50
    }
}

class SettingsRepository(private val settingsDao: SettingsDao) {
    suspend fun load(): StudySettings {
        val stored = settingsDao.findById() ?: return StudySettings()
        return StudySettings(
            newWordsPerDay = stored.newWordsPerDay,
            dailyReviewCap = stored.dailyReviewCap,
        )
    }

    suspend fun save(settings: StudySettings) {
        require(settings.newWordsPerDay >= 0) { "New-words-per-day cannot be negative" }
        require(settings.dailyReviewCap >= 0) { "Daily review cap cannot be negative" }
        settingsDao.save(
            SettingsEntity(
                newWordsPerDay = settings.newWordsPerDay,
                dailyReviewCap = settings.dailyReviewCap,
            ),
        )
    }
}
