# Spec Delta

## Purpose

Defines the daily study loop that controls how many new words are introduced each day and how due reviews are presented alongside them, keeping learning pace sustainable and user-configurable.

## ADDED Requirements

### Requirement: Configurable new words per day
The system SHALL let the learner configure how many new words are introduced per day, defaulting to 15.

#### Scenario: Default new-word count
- **WHEN** a learner has not changed the new-words-per-day setting
- **THEN** the daily session introduces up to 15 new words

#### Scenario: Learner changes the new-word count
- **WHEN** a learner sets a different new-words-per-day value
- **THEN** subsequent daily sessions introduce up to that configured number of new words

### Requirement: No catch-up for skipped days
The system SHALL NOT accumulate a backlog of new words from days the learner did not study. Each day's new-word introduction SHALL be limited to that day's configured count only.

#### Scenario: Learner skips one or more days
- **WHEN** a learner returns after skipping one or more days without studying
- **THEN** the next session introduces only that day's configured new-word count, not an accumulated total from the skipped days

### Requirement: New word selection order
The system SHALL introduce new words in ascending frequency-rank order, selecting the next not-yet-introduced words from the vocabulary dataset.

#### Scenario: Selecting the next batch of new words
- **WHEN** the daily session selects new words to introduce
- **THEN** it selects the lowest-ranked (most frequent) words from the dataset that have not yet been introduced to the learner

### Requirement: Configurable daily review cap
The system SHALL let the learner configure a maximum number of due reviews presented per day. When due reviews exceed this cap, the system SHALL defer the overflow to a later day rather than discarding it.

#### Scenario: Due reviews exceed the configured cap
- **WHEN** the number of cards due on a given day exceeds the configured daily review cap
- **THEN** only up to the cap is presented that day, and the remaining due cards remain due and are presented in a later session

### Requirement: Combined daily session composition
The system SHALL present a learner's daily session as a combination of due reviews (up to the review cap) and new words (up to the daily new-word count).

#### Scenario: Both new words and reviews are available
- **WHEN** a learner starts a daily session and both due reviews and not-yet-introduced words exist
- **THEN** the session includes both due review cards and new word cards, each bounded by their own configured limit
