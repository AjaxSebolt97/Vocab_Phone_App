# vocab-dataset Specification

## Purpose

Provides the offline Spanish vocabulary content the app studies from: a frequency-ranked, dictionary-grounded word dataset bundled with the app and built by a repeatable pipeline so the vocabulary pool can grow over time.

## Requirements

### Requirement: Dataset content and size
The system SHALL ship a bundled dataset of 5,000 Spanish words selected in descending usage-frequency rank from candidates with a non-empty English gloss. Each selected entry SHALL include its English gloss and part of speech, an example sentence when one is available in the source data, and grammatical gender when the part of speech is a noun.

#### Scenario: Word entry has required fields
- **WHEN** a word from the bundled dataset is loaded
- **THEN** it has a frequency rank, an English gloss, and a part of speech

#### Scenario: Source provides an example sentence
- **WHEN** the selected dictionary sense includes an example sentence
- **THEN** the word entry includes that example sentence

#### Scenario: Source has no example sentence
- **WHEN** the selected dictionary sense has no example sentence
- **THEN** the word entry omits the example sentence rather than fabricating one

#### Scenario: Noun entry includes gender
- **WHEN** a word in the dataset has part of speech "noun"
- **THEN** its entry includes a grammatical gender value

### Requirement: Gloss-qualified ranked selection
The system SHALL scan FrequencyWords candidates in descending frequency rank, skip candidates without a non-empty English gloss in the dictionary source, and continue until 5,000 eligible words are selected.

#### Scenario: Candidate has no dictionary gloss
- **WHEN** a frequency-ranked candidate has no matching dictionary entry with a non-empty English gloss
- **THEN** the candidate is omitted and the next-ranked candidate is considered

#### Scenario: Selection reaches the configured dataset size
- **WHEN** at least 5,000 gloss-qualified candidates exist in the frequency list
- **THEN** the dataset contains the first 5,000 such candidates in frequency order

### Requirement: Build-time content pipeline
The system SHALL provide a repeatable, offline-runnable pipeline that selects ranked words from the FrequencyWords Spanish frequency list and enriches each with English definition data looked up from the Spanish dictionary extracted from English Wiktionary by Kaikki.org, producing the bundled dataset asset.

#### Scenario: Pipeline produces a dataset asset
- **WHEN** the content pipeline is run against the frequency list and dictionary source
- **THEN** it produces a dataset asset containing the required fields for each selected word, without requiring network access when the app itself runs

### Requirement: Offline availability at runtime
The system SHALL make all vocabulary content available on-device without any network call once the app is installed.

#### Scenario: App used with no network connection
- **WHEN** the device has no internet connectivity
- **THEN** the app can still read word entries, definitions, and any available example sentences from the bundled dataset

### Requirement: Dataset extensibility
The system SHALL allow the vocabulary pool to be extended beyond the initial 5,000 words by re-running the content pipeline with a larger gloss-qualified word target, without altering the identifiers or content of previously included words.

#### Scenario: Re-running the pipeline with a larger word count
- **WHEN** the content pipeline is re-run with a higher gloss-qualified word target than before
- **THEN** previously included words keep their existing identifiers and field values, and newly included words are appended to the dataset

### Requirement: Stable word identity
The system SHALL assign each dataset word a stable identifier that does not change when the dataset is regenerated, so that a learner's review history for that word remains valid across dataset updates.

#### Scenario: Dataset regenerated after a review history exists
- **WHEN** the content pipeline is re-run and a word already has recorded review history
- **THEN** the word's identifier is unchanged and its existing review history still applies to it
