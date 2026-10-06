import argparse
import csv
import gzip
import json
from pathlib import Path
from typing import TextIO


DEFAULT_FREQUENCY_INPUT = Path("data/frequency/es_frequency_candidates.tsv")
DEFAULT_DICTIONARY_INPUT = Path("data/source/es-extract.jsonl.gz")
DEFAULT_OUTPUT = Path("data/intermediate/es_dictionary_candidates.jsonl")
TARGET_WORD_COUNT = 5000
GENDER_TAGS = {"masculine", "feminine", "common-gender", "neuter"}


def read_candidates(path: Path) -> dict[str, dict[str, int | str]]:
    candidates: dict[str, dict[str, int | str]] = {}
    with path.open(encoding="utf-8", newline="") as source:
        reader = csv.DictReader(source, delimiter="\t")
        for row in reader:
            word = row.get("word", "").strip()
            if not word:
                raise ValueError("Frequency list contains an empty word")

            key = word.casefold()
            if key in candidates:
                raise ValueError(f"Frequency list contains duplicate word: {word}")

            candidates[key] = {
                "rank": int(row["rank"]),
                "word": word,
                "frequency": int(row["frequency"]),
            }

    if not candidates:
        raise ValueError("Frequency list contains no candidate words")
    return candidates


def open_dictionary(path: Path) -> TextIO:
    if path.suffix == ".gz":
        return gzip.open(path, "rt", encoding="utf-8")
    return path.open(encoding="utf-8")


def source_gender_tags(record: dict[str, object]) -> set[str]:
    tags: set[str] = set()
    raw_tags = record.get("tags")
    if isinstance(raw_tags, list):
        tags.update(
            tag.casefold()
            for tag in raw_tags
            if isinstance(tag, str) and tag.casefold() in GENDER_TAGS
        )

    raw_senses = record.get("senses", [])
    if isinstance(raw_senses, list):
        for sense in raw_senses:
            if not isinstance(sense, dict):
                continue
            sense_tags = sense.get("tags")
            if isinstance(sense_tags, list):
                tags.update(
                    tag.casefold()
                    for tag in sense_tags
                    if isinstance(tag, str) and tag.casefold() in GENDER_TAGS
                )
    return tags


def normalized_senses(record: dict[str, object]) -> list[dict[str, object]]:
    normalized: list[dict[str, object]] = []
    senses = record.get("senses", [])
    if not isinstance(senses, list):
        return normalized

    for sense in senses:
        if not isinstance(sense, dict):
            continue

        raw_glosses = sense.get("glosses", [])
        if isinstance(raw_glosses, str):
            raw_glosses = [raw_glosses]
        glosses = [
            gloss.strip()
            for gloss in raw_glosses
            if isinstance(gloss, str) and gloss.strip()
        ]
        if not glosses:
            continue

        normalized_sense: dict[str, object] = {"glosses": glosses}
        tags = sense.get("tags")
        if isinstance(tags, list):
            normalized_sense["tags"] = [tag for tag in tags if isinstance(tag, str)]

        form_of = sense.get("form_of")
        if isinstance(form_of, list):
            lemmas = [
                item["word"]
                for item in form_of
                if isinstance(item, dict)
                and isinstance(item.get("word"), str)
                and item["word"].strip()
            ]
            if lemmas:
                normalized_sense["form_of"] = lemmas

        examples = sense.get("examples", [])
        if isinstance(examples, dict):
            examples = [examples]
        if isinstance(examples, list):
            for example in examples:
                if not isinstance(example, dict):
                    continue
                text = example.get("text")
                if isinstance(text, str) and text.strip():
                    normalized_sense["example_sentence"] = text.strip()
                    break

        normalized.append(normalized_sense)

    return normalized


def dictionary_entry(
    record: dict[str, object],
    candidate: dict[str, int | str],
    senses: list[dict[str, object]],
) -> dict[str, object]:
    entry: dict[str, object] = {
        "rank": candidate["rank"],
        "word": candidate["word"],
        "frequency": candidate["frequency"],
        "dictionary_word": record.get("word"),
        "part_of_speech": record.get("pos"),
        "senses": senses,
    }

    raw_tags = record.get("tags")
    if isinstance(raw_tags, list):
        entry["tags"] = [tag for tag in raw_tags if isinstance(tag, str)]

    if record.get("pos") == "noun":
        gender_tags = source_gender_tags(record)
        if gender_tags:
            entry["grammatical_gender"] = sorted(gender_tags)
    return entry


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Match ranked Spanish frequency candidates to Wiktextract entries."
    )
    parser.add_argument("--frequency-input", type=Path, default=DEFAULT_FREQUENCY_INPUT)
    parser.add_argument("--dictionary-input", type=Path, default=DEFAULT_DICTIONARY_INPUT)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--target-count", type=int, default=TARGET_WORD_COUNT)
    args = parser.parse_args()

    if args.target_count < 1:
        parser.error("--target-count must be greater than zero")

    candidates = read_candidates(args.frequency_input)
    matched: dict[str, list[dict[str, object]]] = {}
    gender_by_word: dict[str, set[str]] = {}
    with open_dictionary(args.dictionary_input) as source:
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
            if record.get("pos") == "noun":
                gender_by_word.setdefault(key, set()).update(source_gender_tags(record))

            candidate = candidates.get(key)
            if candidate is None:
                continue

            senses = normalized_senses(record)
            if not senses:
                continue

            entry = dictionary_entry(record, candidate, senses)
            matched.setdefault(key, []).append(entry)
            if entry.get("part_of_speech") == "noun":
                gender_by_word.setdefault(key, set()).update(
                    entry.get("grammatical_gender", [])
                )

    for entries in matched.values():
        for entry in entries:
            if entry.get("part_of_speech") != "noun" or entry.get("grammatical_gender"):
                continue
            for sense in entry["senses"]:
                for lemma in sense.get("form_of", []):
                    inherited_gender = gender_by_word.get(lemma.casefold())
                    if inherited_gender:
                        entry["grammatical_gender"] = sorted(inherited_gender)
                        break
                if entry.get("grammatical_gender"):
                    break

    selected_keys = sorted(
        matched,
        key=lambda key: int(candidates[key]["rank"]),
    )[: args.target_count]
    if len(selected_keys) != args.target_count:
        raise ValueError(
            f"Found only {len(selected_keys)} glossed words; "
            f"need {args.target_count}"
        )

    args.output.parent.mkdir(parents=True, exist_ok=True)
    entry_count = 0
    with args.output.open("w", encoding="utf-8", newline="\n") as output:
        for key in selected_keys:
            for entry in matched[key]:
                output.write(json.dumps(entry, ensure_ascii=False) + "\n")
                entry_count += 1

    print(
        f"Wrote {len(selected_keys)} glossed words across {entry_count} "
        f"dictionary entries to {args.output}"
    )


if __name__ == "__main__":
    main()
