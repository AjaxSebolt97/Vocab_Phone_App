import json
import sqlite3
import tempfile
import unittest
from contextlib import closing
from pathlib import Path

from build_vocabulary_database import create_database, stable_word_id
from update_english_glosses import update_english_glosses


class UpdateEnglishGlossesTests(unittest.TestCase):
    def test_uses_english_dictionary_and_fallbacks_without_changing_word_ids(self) -> None:
        entries = [
            {
                "rank": 1,
                "word": "de",
                "frequency": 100,
                "part_of_speech": "prep",
                "gloss": "Indica pertenencia.",
            },
            {
                "rank": 2,
                "word": "hola",
                "frequency": 50,
                "part_of_speech": "interj",
                "gloss": "Saludo.",
            },
        ]
        english_entry = {
            "word": "de",
            "lang_code": "es",
            "pos": "prep",
            "senses": [{"glosses": ["of"]}],
        }
        wrong_language_entry = {
            "word": "de",
            "lang_code": "en",
            "pos": "prep",
            "senses": [{"glosses": ["Spanish definition must not be used"]}],
        }

        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            database_path = folder / "vocabulary.sqlite"
            dictionary_path = folder / "dictionary.jsonl"
            overrides_path = folder / "overrides.json"
            create_database(entries, database_path)
            dictionary_path.write_text(
                "\n".join(
                    json.dumps(item)
                    for item in (wrong_language_entry, english_entry)
                )
                + "\n",
                encoding="utf-8",
            )
            overrides_path.write_text(
                json.dumps({"hola": "Hello."}),
                encoding="utf-8",
            )

            updated, fallback_count = update_english_glosses(
                database_path,
                dictionary_path,
                overrides_path,
            )

            with closing(sqlite3.connect(database_path)) as connection:
                glosses = dict(
                    connection.execute(
                        "SELECT normalized_word, gloss FROM words ORDER BY rank"
                    )
                )
                ids = dict(connection.execute("SELECT normalized_word, id FROM words"))

        self.assertEqual(updated, 2)
        self.assertEqual(fallback_count, 1)
        self.assertEqual(glosses, {"de": "of", "hola": "Hello."})
        self.assertEqual(ids, {"de": stable_word_id("de"), "hola": stable_word_id("hola")})


if __name__ == "__main__":
    unittest.main()
