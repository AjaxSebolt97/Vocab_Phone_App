import argparse
import json
import sqlite3
from contextlib import closing
from pathlib import Path

from build_vocabulary_database import DATASET_SOURCES, ROOM_DATABASE_VERSION
from disambiguate_dictionary_entries import select_primary_entry
from match_dictionary_entries import dictionary_entry, normalized_senses, open_dictionary


DEFAULT_DATABASE = Path("app/src/main/assets/es_vocabulary.sqlite")
DEFAULT_OVERRIDES = Path("tools/vocab_dataset/english_gloss_overrides.json")
DEFAULT_DICTIONARY_INPUT = Path("data/source/kaikki.org-dictionary-Spanish.jsonl.gz")


def load_overrides(path: Path) -> dict[str, str]:
    with path.open(encoding="utf-8") as source:
        overrides = json.load(source)
    if not isinstance(overrides, dict):
        raise ValueError("English gloss overrides must be a JSON object")

    normalized: dict[str, str] = {}
    for word, gloss in overrides.items():
        if not isinstance(word, str) or not word.strip():
            raise ValueError("English gloss override has an invalid word")
        if not isinstance(gloss, str) or not gloss.strip():
            raise ValueError(f"English gloss override for {word!r} is empty")
        normalized[word.casefold()] = gloss.strip()
    return normalized


def update_english_glosses(
    database_path: Path,
    dictionary_path: Path,
    overrides_path: Path,
) -> tuple[int, int]:
    with closing(sqlite3.connect(database_path)) as connection:
        database_version = connection.execute("PRAGMA user_version").fetchone()[0]
        if database_version not in (3, ROOM_DATABASE_VERSION):
            raise ValueError(f"Cannot update vocabulary database version {database_version}")

        rows = connection.execute(
            "SELECT id, rank, spanish_text, frequency FROM words ORDER BY rank"
        ).fetchall()

    candidates: dict[str, dict[str, int | str]] = {}
    word_ids: dict[str, int] = {}
    for word_id, rank, word, frequency in rows:
        key = word.casefold()
        if key in candidates:
            raise ValueError(f"Duplicate vocabulary word: {word}")
        candidates[key] = {"rank": rank, "word": word, "frequency": frequency}
        word_ids[key] = word_id

    matched: dict[str, list[dict[str, object]]] = {}
    with open_dictionary(dictionary_path) as source:
        for line_number, line in enumerate(source, start=1):
            try:
                record = json.loads(line)
            except json.JSONDecodeError as error:
                raise ValueError(f"Invalid JSON on dictionary line {line_number}") from error
            if not isinstance(record, dict) or record.get("lang_code") != "es":
                continue

            dictionary_word = record.get("word")
            if not isinstance(dictionary_word, str):
                continue
            key = dictionary_word.casefold()
            candidate = candidates.get(key)
            if candidate is None:
                continue

            senses = normalized_senses(record)
            if senses:
                matched.setdefault(key, []).append(
                    dictionary_entry(record, candidate, senses)
                )

    glosses = {
        key: str(select_primary_entry(entries)["gloss"])
        for key, entries in matched.items()
    }
    overrides = load_overrides(overrides_path)
    fallback_count = 0
    for key in candidates:
        if key not in glosses:
            gloss = overrides.get(key)
            if gloss is None:
                raise ValueError(f"No English Wiktionary gloss or override for {key!r}")
            glosses[key] = gloss
            fallback_count += 1

    if len(glosses) != len(rows):
        raise ValueError(
            f"Expected glosses for {len(rows)} words, found {len(glosses)}"
        )

    with closing(sqlite3.connect(database_path)) as connection:
        with connection:
            for key, word_id in word_ids.items():
                cursor = connection.execute(
                    "UPDATE words SET gloss = ? WHERE id = ?",
                    (glosses[key], word_id),
                )
                if cursor.rowcount != 1:
                    raise ValueError(f"Could not update gloss for {key!r}")

            connection.execute("DELETE FROM dataset_sources")
            connection.executemany(
                """
                INSERT INTO dataset_sources (source_name, source_url, license, attribution)
                VALUES (?, ?, ?, ?)
                """,
                DATASET_SOURCES,
            )
        connection.execute(f"PRAGMA user_version = {ROOM_DATABASE_VERSION}")

    return len(glosses), fallback_count


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Replace Spanish glossary text while preserving bundled word IDs."
    )
    parser.add_argument("--database", type=Path, default=DEFAULT_DATABASE)
    parser.add_argument("--dictionary-input", type=Path, default=DEFAULT_DICTIONARY_INPUT)
    parser.add_argument("--overrides", type=Path, default=DEFAULT_OVERRIDES)
    args = parser.parse_args()

    updated, fallback_count = update_english_glosses(
        args.database,
        args.dictionary_input,
        args.overrides,
    )
    print(
        f"Updated {updated} English glosses in {args.database}; "
        f"used {fallback_count} Spanish Wiktionary translations."
    )


if __name__ == "__main__":
    main()
