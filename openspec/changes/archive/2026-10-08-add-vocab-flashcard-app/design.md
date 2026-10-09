# Design

## Context

Greenfield project — no existing code, specs, or architecture to integrate with. Target is a single-user, single-device Android app (Android 16) with no backend service. See [proposal.md](proposal.md#why) for motivation. Full behavior contracts are in the specs for `vocab-dataset`, `spaced-repetition`, and `daily-study-session`.

## Goals / Non-Goals

**Goals:**
- Native Android app (Kotlin, Jetpack Compose) with Room over a bundled SQLite dataset — zero network dependency at runtime.
- A reusable, scriptable content pipeline (run on a developer machine, not on-device) that produces the bundled dataset from two external sources.
- SM-2 spaced repetition with bidirectional cards and 4-button grading.

**Non-Goals:**
- No backend/cloud sync, no multi-device support, no user accounts.
- No live dictionary API calls at runtime (all content is pre-baked).
- No audio/pronunciation playback in v1.
- No languages other than Spanish, and no UI localization work beyond what's needed to display Spanish text.

## Decisions

**Content pipeline as an offline, standalone script (not part of the Android app build).**
The pipeline reads the complete 50,000-entry FrequencyWords `es` frequency list in rank order and looks candidates up in Kaikki.org's Spanish dictionary, which is extracted from English Wiktionary and provides English glosses. It skips candidates without a non-empty English gloss and stops after selecting 5,000 glossed words; an example sentence is stored when present, and noun gender is required. For the existing 5,000-word set, 44 entries absent from English Wiktionary use English translations of Spanish Wiktionary definitions. Output is a SQLite database file checked into the app's assets and bundled at build time via Room's pre-populated database support.
- *Alternative considered*: fetch and enrich data on-device at first launch. Rejected — reintroduces a network dependency and API/ToS surface the offline-first decision was meant to avoid, and conflicts with the "no constant internet connection" requirement.
- Each word is assigned a stable surrogate ID (not row position) so re-running the pipeline with a larger N never changes existing IDs — this is what the `vocab-dataset` extensibility and stable-identity requirements depend on.
- kaikki.org's raw Wiktionary extract contains many senses/parts of speech per word form; the pipeline needs disambiguation logic (e.g., picking the most common/primary sense) to produce one clean entry per word. This is pipeline-internal logic, not a spec-level behavior.

**SM-2 for scheduling, implemented in-app (not a library dependency).**
SM-2 is a well-documented, simple-to-implement algorithm (ease factor, interval, repetition count per card) and matches the "Again/Hard/Good/Easy" grading the user wants. Implementing it directly (rather than pulling in a third-party SRS library) keeps the dependency surface small and the behavior fully auditable/testable against the spec's scenarios.
- *Alternative considered*: FSRS (a newer, ML-derived scheduler used by recent Anki versions). Rejected for v1 — more complex to implement/tune correctly, and SM-2's behavior is simpler to reason about and verify against the spec scenarios.

**Bidirectional cards as two independent rows, not one row with a "direction" flag read at render time.**
Each word produces two card records (ES→EN, EN→ES) each with its own ease/interval/due-date, because the two directions genuinely have independent difficulty and should schedule independently (per the `spaced-repetition` spec).

**Room schema sketch (illustrative, not binding on tasks/implementation):**
```
words(id, rank, spanish_text, part_of_speech, gloss, example_sentence_nullable, gender_nullable)
cards(id, word_id FK, direction[ES_TO_EN|EN_TO_ES], ease_factor, repetitions, interval_days, due_date, introduced_date)
settings(new_words_per_day, daily_review_cap)
```
The repetition count is persisted because SM-2 uses it to distinguish the first and second successful reviews from later interval calculations.

## Risks / Trade-offs

- [Risk] Kaikki.org's English Wiktionary Spanish dataset is ~1GB raw and will require a build-time filtering/parsing step rather than being shippable as-is → Mitigation: pipeline runs once per dataset version on a developer machine and only the small filtered output (≈5,000 words) is bundled in the APK.
- [Risk] Word-sense disambiguation (picking the right gloss among many Wiktionary senses) is inherently imperfect and may need manual review/correction for some of the 5,000 words → Mitigation: pipeline output is a plain SQLite file that can be hand-edited/corrected before bundling; this is a data-quality concern, not a blocking architectural one.
- [Risk] Attribution requirement: kaikki.org/Wiktionary content is CC-BY-SA/GFDL, which requires attribution in the app → Mitigation: include an attribution/credits screen or section referencing Wiktionary and the FrequencyWords project.
- [Risk] SM-2, implemented from scratch, risks subtle scheduling bugs that are hard to notice because incorrect intervals still "look" plausible → Mitigation: the `spaced-repetition` spec's scenarios (Again resets short, Easy > Good interval, etc.) are concrete, testable cases to validate the implementation against.

## Migration Plan

Not applicable — this is a new, standalone application with no prior version or existing data to migrate.
