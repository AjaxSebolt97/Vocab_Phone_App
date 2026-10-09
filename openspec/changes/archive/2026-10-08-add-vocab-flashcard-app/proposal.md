# Proposal

## Why

Learning Spanish vocabulary by rote with no structure leads to either overload (too many new words at once) or stagnation (no mechanism to retain what's learned). This change builds a focused, offline Android flashcard app that introduces a sustainable, configurable number of new Spanish words per day, grounded in real-world word-usage frequency and verified dictionary definitions, and uses spaced repetition so previously learned words are retained over time without manual tracking.

## What Changes

- New Android app (Kotlin, Jetpack Compose, Room) that runs fully offline — no network dependency for daily use.
- A repeatable, offline content-build pipeline that scans the FrequencyWords Spanish list in rank order, skips words without a non-empty English gloss in the Spanish dictionary extracted from English Wiktionary by Kaikki.org, and selects the first 5,000 glossed words. Entries include part of speech, an example sentence when available, and grammatical gender for nouns; the result is packaged as a bundled SQLite asset.
- A spaced-repetition scheduler (SM-2 algorithm) that tracks per-card ease, interval, and due-date, graded via a 4-button scale (Again / Hard / Good / Easy).
- Bidirectional flashcards: each vocabulary word produces two cards — Spanish-to-English recognition and English-to-Spanish production.
- A daily study session that introduces a configurable number of new words per day (default 15; skipped days are simply skipped, with no backlog/catch-up) interleaved with due reviews, capped at a configurable daily review limit (overflow rolls to the next day).

## Capabilities

### New Capabilities
- `vocab-dataset`: The offline Spanish vocabulary dataset — the first 5,000 frequency-ranked words with non-empty dictionary glosses, with optional example sentences and noun gender, built via a repeatable FrequencyWords + kaikki.org pipeline and packaged as a bundled, queryable local dataset. Owns requirements about dataset content, provenance, and extensibility (adding more words later).
- `spaced-repetition`: The SM-2-based scheduling behavior — per-card ease/interval/due-date tracking, 4-button grading, and bidirectional card generation per vocabulary word. Owns requirements about how review state evolves over time.
- `daily-study-session`: The daily study loop — selecting which new words and due reviews to present each day, respecting the configurable new-words-per-day count (with no catch-up for skipped days) and the configurable daily review cap. Owns requirements about session composition and user-configurable pacing settings.

### Modified Capabilities
(none — greenfield project, no existing specs)

## Impact

- New Android application project (Kotlin, Jetpack Compose, Room/SQLite), targeting Android 16.
- New offline content-build pipeline/tooling (not part of the installed app) that produces the bundled dataset asset from FrequencyWords (frequency ranking) and English Wiktionary's Spanish-language entries published by Kaikki.org (English glosses). Both are free/openly licensed; Wiktionary data carries a CC-BY-SA/GFDL attribution requirement.
- No existing systems, specs, or code affected.
