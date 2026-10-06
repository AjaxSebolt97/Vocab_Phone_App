import argparse
import json
from pathlib import Path
from typing import TextIO


DEFAULT_INPUT = Path("data/intermediate/es_dictionary_candidates.jsonl")
DEFAULT_OUTPUT = Path("data/intermediate/es_primary_words.jsonl")
PART_OF_SPEECH_PRIORITY = {
    part_of_speech: priority
    for priority, part_of_speech in enumerate(
        (
            "prep",
            "conj",
            "pron",
            "det",
            "article",
            "adv",
            "verb",
            "noun",
            "adj",
            "intj",
            "num",
            "phrase",
        )
    )
}


def read_entries(source: TextIO) -> dict[str, list[dict[str, object]]]:
    words: dict[str, list[dict[str, object]]] = {}
    for line_number, line in enumerate(source, start=1):
        try:
            entry = json.loads(line)
        except json.JSONDecodeError as error:
            raise ValueError(f"Invalid JSON on candidate line {line_number}") from error
        if not isinstance(entry, dict):
            raise ValueError(f"Candidate line {line_number} is not a JSON object")

        word = entry.get("word")
        if not isinstance(word, str) or not word.strip():
            raise ValueError(f"Candidate line {line_number} has no word")
        words.setdefault(word.casefold(), []).append(entry)

    return words


def select_primary_entry(entries: list[dict[str, object]]) -> dict[str, object]:
    if not entries:
        raise ValueError("Cannot select a primary entry from an empty group")

    _, selected = min(
        enumerate(entries),
        key=lambda item: (
            PART_OF_SPEECH_PRIORITY.get(
                str(item[1].get("part_of_speech", "")),
                len(PART_OF_SPEECH_PRIORITY),
            ),
            item[0],
        ),
    )

    senses = selected.get("senses")
    if not isinstance(senses, list):
        raise ValueError("Selected dictionary entry has no senses")

    for sense in senses:
        if not isinstance(sense, dict):
            continue
        glosses = sense.get("glosses")
        if not isinstance(glosses, list):
            continue
        primary_gloss = next(
            (gloss.strip() for gloss in glosses if isinstance(gloss, str) and gloss.strip()),
            None,
        )
        if primary_gloss is None:
            continue

        primary: dict[str, object] = {
            "rank": selected["rank"],
            "word": selected["word"],
            "frequency": selected["frequency"],
            "dictionary_word": selected["dictionary_word"],
            "part_of_speech": selected["part_of_speech"],
            "gloss": primary_gloss,
        }
        example = sense.get("example_sentence")
        if isinstance(example, str) and example.strip():
            primary["example_sentence"] = example.strip()

        gender = selected.get("grammatical_gender")
        if selected.get("part_of_speech") == "noun" and gender:
            primary["grammatical_gender"] = gender
        return primary

    raise ValueError(f"Selected entry for {selected.get('word')} has no usable gloss")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Reduce dictionary candidates to one primary gloss per word."
    )
    parser.add_argument("--input", type=Path, default=DEFAULT_INPUT)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args()

    with args.input.open(encoding="utf-8") as source:
        words = read_entries(source)

    primary_entries = [select_primary_entry(entries) for entries in words.values()]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8", newline="\n") as output:
        for entry in primary_entries:
            output.write(json.dumps(entry, ensure_ascii=False) + "\n")

    print(f"Wrote {len(primary_entries)} primary word entries to {args.output}")


if __name__ == "__main__":
    main()
