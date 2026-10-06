import argparse
import csv
from pathlib import Path
from urllib.request import urlopen


SOURCE_URL = (
    "https://raw.githubusercontent.com/hermitdave/FrequencyWords/"
    "master/content/2018/es/es_50k.txt"
)
DEFAULT_OUTPUT = Path("data/frequency/es_frequency_candidates.tsv")


def load_source(input_path: Path | None) -> str:
    if input_path is not None:
        return input_path.read_text(encoding="utf-8")

    with urlopen(SOURCE_URL, timeout=30) as response:
        return response.read().decode("utf-8")


def extract_words(lines: str, limit: int) -> list[tuple[int, str, int]]:
    words: list[tuple[int, str, int]] = []
    seen: set[str] = set()

    for line_number, line in enumerate(lines.splitlines(), start=1):
        fields = line.split()
        if len(fields) != 2:
            raise ValueError(f"Expected word and count on source line {line_number}")

        word, raw_count = fields
        try:
            count = int(raw_count)
        except ValueError as error:
            raise ValueError(f"Invalid occurrence count on source line {line_number}") from error
        if count < 0:
            raise ValueError(f"Negative occurrence count on source line {line_number}")

        normalized_word = word.casefold()
        if normalized_word in seen:
            continue

        seen.add(normalized_word)
        words.append((len(words) + 1, word, count))
        if len(words) == limit:
            break

    if len(words) != limit:
        raise ValueError(f"Source contains only {len(words)} unique words; need {limit}")

    return words


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Extract the most frequent unique Spanish words from FrequencyWords."
    )
    parser.add_argument("--input", type=Path, help="Local FrequencyWords text file; downloads if omitted")
    parser.add_argument("--limit", type=int, default=50000, help="Number of unique words to extract")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT, help="Output TSV path")
    args = parser.parse_args()

    if args.limit < 1:
        parser.error("--limit must be greater than zero")

    words = extract_words(load_source(args.input), args.limit)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8", newline="") as output_file:
        writer = csv.writer(output_file, delimiter="\t", lineterminator="\n")
        writer.writerow(("rank", "word", "frequency"))
        writer.writerows(words)

    print(f"Wrote {len(words)} unique words to {args.output}")


if __name__ == "__main__":
    main()
