import sqlite3
import tempfile
import unittest
from contextlib import closing
from pathlib import Path

from build_vocabulary_database import create_database, stable_word_id


class StableWordIdTests(unittest.TestCase):
    def test_id_is_stable_across_case_and_content_changes(self) -> None:
        original = stable_word_id("Casa")
        regenerated = stable_word_id("casa")

        self.assertEqual(original, regenerated)
        self.assertGreaterEqual(original, 0)
        self.assertLess(original, 1 << 63)


class VocabularyDatabaseTests(unittest.TestCase):
    def test_database_persists_word_fields_and_stable_ids(self) -> None:
        entries = [
            {
                "rank": 1,
                "word": "casa",
                "frequency": 500,
                "part_of_speech": "noun",
                "gloss": "house",
                "example_sentence": "La casa es grande.",
                "grammatical_gender": ["feminine"],
            },
            {
                "rank": 2,
                "word": "ser",
                "frequency": 450,
                "part_of_speech": "verb",
                "gloss": "to be",
            },
        ]

        with tempfile.TemporaryDirectory() as directory:
            database_path = Path(directory) / "vocabulary.sqlite"
            create_database(entries, database_path)
            with closing(sqlite3.connect(database_path)) as connection:
                first_ids = dict(connection.execute("SELECT normalized_word, id FROM words"))
                casa = connection.execute(
                    "SELECT rank, spanish_text, part_of_speech, gloss, example_sentence, grammatical_gender "
                    "FROM words WHERE normalized_word = 'casa'"
                ).fetchone()
                ser = connection.execute(
                    "SELECT example_sentence, grammatical_gender FROM words WHERE normalized_word = 'ser'"
                ).fetchone()
                sources = connection.execute(
                    "SELECT source_name, source_url, license, attribution "
                    "FROM dataset_sources ORDER BY source_name"
                ).fetchall()

            create_database(entries, database_path)
            with closing(sqlite3.connect(database_path)) as connection:
                second_ids = dict(connection.execute("SELECT normalized_word, id FROM words"))
                count = connection.execute("SELECT COUNT(*) FROM words").fetchone()[0]

        self.assertEqual(first_ids, second_ids)
        self.assertEqual(count, 2)
        self.assertEqual(casa, (1, "casa", "noun", "house", "La casa es grande.", '["feminine"]'))
        self.assertEqual(ser, (None, None))
        self.assertEqual(first_ids["casa"], stable_word_id("casa"))
        self.assertEqual(len(sources), 2)
        self.assertEqual(
            {source[0] for source in sources},
            {"FrequencyWords", "Wiktionary via Kaikki.org and Wiktextract"},
        )
        self.assertTrue(all(source[1] and source[2] and source[3] for source in sources))

    def test_empty_word_id_is_rejected(self) -> None:
        with self.assertRaises(ValueError):
            stable_word_id("")


if __name__ == "__main__":
    unittest.main()
