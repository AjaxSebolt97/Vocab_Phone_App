import argparse
import hashlib
import json
import sqlite3
from contextlib import closing
from pathlib import Path
from typing import TextIO


DEFAULT_INPUT = Path("data/intermediate/es_primary_words.jsonl")
DEFAULT_OUTPUT = Path("app/src/main/assets/es_vocabulary.sqlite")
# Must equal the Room database version declared in the Android app.
ROOM_DATABASE_VERSION = 4
DATASET_SOURCES = (
    (
        "FrequencyWords",
        "https://github.com/hermitdave/FrequencyWords",
        "CC BY-SA 4.0 (frequency-list content)",
        "Spanish word-frequency data derived from OpenSubtitles 2018.",
    ),
    (
        "English Wiktionary via Kaikki.org and Wiktextract",
        "https://kaikki.org/dictionary/Spanish/",
        "CC BY-SA 4.0 and GFDL (Wiktionary content)",
        "Spanish lexical entries with English glosses, examples, and grammatical metadata.",
    ),
    (
        "Spanish Wiktionary translation overrides",
        "https://es.wiktionary.org/",
        "CC BY-SA 4.0 and GFDL (Wiktionary content)",
        "English translations of 44 Spanish Wiktionary definitions not covered by English Wiktionary.",
    ),
)


def stable_word_id(word: str) -> int:
    normalized_word = word.casefold().strip()
    if not normalized_word:
        raise ValueError("Cannot assign an ID to an empty word")

    digest = hashlib.sha256(normalized_word.encode("utf-8")).digest()
    return int.from_bytes(digest[:8], "big") & ((1 << 63) - 1)


def load_entries(source: TextIO) -> list[dict[str, object]]:
    entries: list[dict[str, object]] = []
    seen_words: set[str] = set()
    previous_rank = 0

    for line_number, line in enumerate(source, start=1):
        try:
            entry = json.loads(line)
        except json.JSONDecodeError as error:
            raise ValueError(f"Invalid JSON on primary-word line {line_number}") from error
        if not isinstance(entry, dict):
            raise ValueError(f"Primary-word line {line_number} is not a JSON object")

        word = entry.get("word")
        rank = entry.get("rank")
        if not isinstance(word, str) or not word.strip():
            raise ValueError(f"Primary-word line {line_number} has no word")
        if not isinstance(rank, int) or rank <= previous_rank:
            raise ValueError(f"Primary-word ranks must be strictly increasing at line {line_number}")
        if word.casefold() in seen_words:
            raise ValueError(f"Duplicate primary word: {word}")
        if not isinstance(entry.get("part_of_speech"), str) or not entry["part_of_speech"].strip():
            raise ValueError(f"Primary word {word} has no part of speech")
        if not isinstance(entry.get("gloss"), str) or not entry["gloss"].strip():
            raise ValueError(f"Primary word {word} has no gloss")

        seen_words.add(word.casefold())
        previous_rank = rank
        entries.append(entry)

    if not entries:
        raise ValueError("Primary-word input is empty")
    return entries


def create_database(entries: list[dict[str, object]], database_path: Path) -> None:
    words_by_id: dict[int, str] = {}
    rows: list[tuple[object, ...]] = []

    for entry in entries:
        word = str(entry["word"]).strip()
        normalized_word = word.casefold()
        word_id = stable_word_id(word)
        existing_word = words_by_id.get(word_id)
        if existing_word is not None and existing_word != normalized_word:
            raise ValueError(f"Stable ID collision between {existing_word!r} and {normalized_word!r}")
        words_by_id[word_id] = normalized_word

        gender = entry.get("grammatical_gender")
        gender_json = json.dumps(gender, ensure_ascii=False) if gender else None
        rows.append(
            (
                word_id,
                int(entry["rank"]),
                word,
                normalized_word,
                int(entry["frequency"]),
                str(entry["part_of_speech"]),
                str(entry["gloss"]).strip(),
                entry.get("example_sentence"),
                gender_json,
            )
        )

    database_path.parent.mkdir(parents=True, exist_ok=True)
    with closing(sqlite3.connect(database_path)) as connection:
        with connection:
            connection.execute("DROP TABLE IF EXISTS cards")
            connection.execute("DROP TABLE IF EXISTS settings")
            connection.execute("DROP TABLE IF EXISTS words")
            connection.execute("DROP TABLE IF EXISTS dataset_sources")
            connection.execute(
                """
                CREATE TABLE words (
                    id INTEGER NOT NULL PRIMARY KEY,
                    rank INTEGER NOT NULL,
                    spanish_text TEXT NOT NULL,
                    normalized_word TEXT NOT NULL,
                    frequency INTEGER NOT NULL,
                    part_of_speech TEXT NOT NULL,
                    gloss TEXT NOT NULL,
                    example_sentence TEXT,
                    grammatical_gender TEXT
                )
                """
            )
            connection.executemany(
                """
                INSERT INTO words (
                    id, rank, spanish_text, normalized_word, frequency,
                    part_of_speech, gloss, example_sentence, grammatical_gender
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                rows,
            )
            connection.execute("CREATE UNIQUE INDEX index_words_rank ON words(rank)")
            connection.execute(
                "CREATE UNIQUE INDEX index_words_normalized_word ON words(normalized_word)"
            )
            connection.execute(
                """
                CREATE TABLE cards (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    word_id INTEGER NOT NULL,
                    direction TEXT NOT NULL,
                    ease_factor REAL NOT NULL,
                    repetitions INTEGER NOT NULL DEFAULT 0,
                    interval_days INTEGER NOT NULL,
                    due_date INTEGER NOT NULL,
                    introduced_date INTEGER NOT NULL,
                    FOREIGN KEY(word_id) REFERENCES words(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """
            )
            connection.execute("CREATE INDEX index_cards_word_id ON cards(word_id)")
            connection.execute(
                "CREATE UNIQUE INDEX index_cards_word_id_direction ON cards(word_id, direction)"
            )
            connection.execute(
                """
                CREATE TABLE settings (
                    id INTEGER NOT NULL PRIMARY KEY,
                    new_words_per_day INTEGER NOT NULL,
                    daily_review_cap INTEGER NOT NULL
                )
                """
            )
            connection.execute(
                """
                CREATE TABLE dataset_sources (
                    source_name TEXT PRIMARY KEY,
                    source_url TEXT NOT NULL,
                    license TEXT NOT NULL,
                    attribution TEXT NOT NULL
                )
                """
            )
            connection.executemany(
                """
                INSERT INTO dataset_sources (source_name, source_url, license, attribution)
                VALUES (?, ?, ?, ?)
                """,
                DATASET_SOURCES,
            )
        connection.execute(f"PRAGMA user_version = {ROOM_DATABASE_VERSION}")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Build a bundled SQLite vocabulary database with stable word IDs."
    )
    parser.add_argument("--input", type=Path, default=DEFAULT_INPUT)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args()

    with args.input.open(encoding="utf-8") as source:
        entries = load_entries(source)
    create_database(entries, args.output)
    print(f"Wrote {len(entries)} vocabulary words to {args.output}")


if __name__ == "__main__":
    main()
