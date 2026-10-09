# Spec Delta

## Purpose

Governs how review scheduling state evolves for each flashcard over time using spaced repetition, so that retention compounds automatically as words age without any manual grouping by time period.

## ADDED Requirements

### Requirement: Bidirectional card generation
The system SHALL generate two independently scheduled cards for each vocabulary word: one presenting the Spanish word for English recall, and one presenting the English gloss for Spanish recall.

#### Scenario: Word introduced for the first time
- **WHEN** a vocabulary word is introduced to a learner for the first time
- **THEN** two cards are created for that word, each with its own independent review schedule

### Requirement: Spaced repetition scheduling
The system SHALL track, per card, an ease factor, a review interval, and a due date, and SHALL recalculate these using the SM-2 algorithm each time the card is graded.

#### Scenario: Card graded
- **WHEN** a learner grades a card
- **THEN** the card's ease factor, interval, and due date are recalculated according to SM-2 rules and persisted

### Requirement: Four-button grading scale
The system SHALL let the learner grade each reviewed card as Again, Hard, Good, or Easy, with each grade producing a different scheduling outcome.

#### Scenario: Grading a card as Again
- **WHEN** a learner grades a card as "Again"
- **THEN** the card's interval resets to a short relearning interval and the card becomes due again soon

#### Scenario: Grading a card as Easy
- **WHEN** a learner grades a card as "Easy"
- **THEN** the card's next interval is longer than it would be for a "Good" grade on the same card

### Requirement: Interleaved review queue
The system SHALL present all cards that are due on a given day together, regardless of how long ago each card was introduced, without grouping or filtering by time period.

#### Scenario: Cards of different ages are due the same day
- **WHEN** cards introduced on different past dates all have a due date on or before today
- **THEN** all of them are eligible to appear together in today's set of due reviews
