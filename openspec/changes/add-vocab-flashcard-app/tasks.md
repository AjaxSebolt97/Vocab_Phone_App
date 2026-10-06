# Tasks

## 1. Content pipeline

- [x] 1.1 Write a script that downloads/reads the complete 50,000-entry FrequencyWords Spanish frequency list and preserves every unique candidate in rank order, and verify it outputs exactly 50,000 unique words in rank order
- [x] 1.2 Match candidates in rank order against the kaikki.org Spanish Wiktionary JSONL extract, skip entries without a non-empty gloss, and select the first 5,000 glossed words with part of speech, example sentence when present, and source-tagged or lemma-inherited gender for nouns; verify a sample of 20 entries by hand and confirm the output has exactly 5,000 words
- [x] 1.3 Add disambiguation logic to pick one primary sense per word when multiple senses/parts of speech exist, and verify no word in the output has more than one gloss/part-of-speech pair
- [x] 1.4 Assign each word a stable surrogate ID and write the enriched dataset to a SQLite file, and verify re-running the pipeline against an unchanged frequency cutoff produces identical IDs for all existing words
- [x] 1.5 Verify the pipeline supports extensibility: re-run it with a larger gloss-qualified target (e.g. 5,010) and confirm the original 5,000 words keep their IDs/content and 10 new words are appended in frequency order
- [x] 1.6 Add an attribution note (crediting Wiktionary/kaikki.org and the FrequencyWords project) to the pipeline's output/readme, and verify it is present in the repo

## 2. Android project scaffolding

- [x] 2.1 Create the native Android app project (Kotlin, Jetpack Compose, min/target SDK for Android 16) and verify the app builds and launches to an empty screen on an emulator/device
- [ ] 2.2 Add Room and wire up the pipeline's SQLite file as a pre-populated bundled database asset, and verify the app can open the database and read a row count matching the dataset size at startup

## 3. Data layer

- [ ] 3.1 Define Room entities for `words`, `cards`, and `settings` per the design's schema sketch, and verify Room generates without schema errors and a unit test can insert/read a word and its two cards
- [ ] 3.2 Implement DAOs for querying due cards (by due date), not-yet-introduced words (by rank), and settings read/write, and verify each DAO method has a passing unit test against an in-memory database

## 4. Spaced repetition scheduler

- [ ] 4.1 Implement the SM-2 algorithm (ease factor, interval, due date) as a pure function taking a card's current state and a grade, and verify unit tests cover: initial review, consecutive "Good" grades growing the interval, "Again" resetting to a short interval, and "Easy" producing a longer interval than "Good" on the same prior state
- [ ] 4.2 Implement bidirectional card creation so introducing a word creates two independently-scheduled cards (ES→EN, EN→ES), and verify a unit test confirms both cards exist with independent due dates after introduction and after grading only one of them
- [ ] 4.3 Wire grading (Again/Hard/Good/Easy) from the review flow into the scheduler and persist updated card state, and verify an integration test confirms a graded card's new due date/ease is persisted and reloaded correctly

## 5. Daily study session logic

- [ ] 5.1 Implement new-word selection that picks up to the configured daily count of not-yet-introduced words in ascending rank order, and verify a unit test confirms the correct words are selected and no word is selected twice across sessions
- [ ] 5.2 Implement the "no catch-up" rule so each day's new-word introduction is capped at that day's configured count regardless of skipped days, and verify a unit test simulating a multi-day gap confirms no backlog accumulates
- [ ] 5.3 Implement due-review selection capped at the configured daily review limit, with overflow remaining due for later sessions, and verify a unit test confirms overflow cards are not shown today but remain due tomorrow
- [ ] 5.4 Combine new words and due reviews into a single daily session list, and verify an integration test confirms a session includes both kinds of cards when both are available, respecting both configured limits
- [ ] 5.5 Implement settings read/write for new-words-per-day (default 15) and daily review cap, and verify a unit test confirms defaults apply when unset and persisted values are used once changed

## 6. Review UI

- [ ] 6.1 Build the Compose flashcard screen showing front content, a reveal action, and back content (word, part of speech, gloss, example sentence when available, gender when present), and verify manual testing on an emulator shows correct front/back content for both card directions
- [ ] 6.2 Add the four grading buttons (Again/Hard/Good/Easy) wired to the scheduler, and verify manual testing confirms grading advances to the next card in the session and updates the card's schedule
- [ ] 6.3 Build the daily session start screen showing the day's combined queue and a "no cards due" empty state, and verify manual testing confirms the empty state appears once a session's queue is exhausted

## 7. Settings UI

- [ ] 7.1 Build a settings screen for new-words-per-day and daily review cap, and verify manual testing confirms changed values persist across app restarts and affect the next session's composition

## 8. End-to-end verification

- [ ] 8.1 Run a full manual walkthrough on a device/emulator: complete a daily session mixing new words and reviews, grade cards across all four grades, restart the app, and confirm due dates and introduced-word state persisted correctly
