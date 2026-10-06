import unittest

from disambiguate_dictionary_entries import select_primary_entry


class SelectPrimaryEntryTests(unittest.TestCase):
    def test_prefers_function_word_part_of_speech_and_first_source_sense(self) -> None:
        entries = [
            {
                "rank": 1,
                "word": "a",
                "frequency": 100,
                "dictionary_word": "a",
                "part_of_speech": "character",
                "senses": [{"glosses": ["letter a"]}],
            },
            {
                "rank": 1,
                "word": "a",
                "frequency": 100,
                "dictionary_word": "a",
                "part_of_speech": "noun",
                "senses": [{"glosses": ["name of the letter a"]}],
            },
            {
                "rank": 1,
                "word": "a",
                "frequency": 100,
                "dictionary_word": "a",
                "part_of_speech": "prep",
                "senses": [
                    {
                        "glosses": ["to"],
                        "example_sentence": "Voy a casa.",
                    },
                    {"glosses": ["at"]},
                ],
            },
        ]

        selected = select_primary_entry(entries)

        self.assertEqual(selected["part_of_speech"], "prep")
        self.assertEqual(selected["gloss"], "to")
        self.assertEqual(selected["example_sentence"], "Voy a casa.")
        self.assertEqual(set(selected), {
            "rank",
            "word",
            "frequency",
            "dictionary_word",
            "part_of_speech",
            "gloss",
            "example_sentence",
        })

    def test_falls_back_to_source_order_for_unranked_parts_of_speech(self) -> None:
        entries = [
            {
                "rank": 8,
                "word": "xyz",
                "frequency": 5,
                "dictionary_word": "xyz",
                "part_of_speech": "unlisted-pos",
                "senses": [{"glosses": ["first source entry"]}],
            },
            {
                "rank": 8,
                "word": "xyz",
                "frequency": 5,
                "dictionary_word": "xyz",
                "part_of_speech": "another-unlisted-pos",
                "senses": [{"glosses": ["second source entry"]}],
            },
        ]

        selected = select_primary_entry(entries)

        self.assertEqual(selected["part_of_speech"], "unlisted-pos")
        self.assertEqual(selected["gloss"], "first source entry")

    def test_omits_example_when_selected_sense_has_none(self) -> None:
        entry = {
            "rank": 9,
            "word": "por",
            "frequency": 4,
            "dictionary_word": "por",
            "part_of_speech": "prep",
            "senses": [{"glosses": ["for"]}],
        }

        selected = select_primary_entry([entry])

        self.assertNotIn("example_sentence", selected)

    def test_preserves_noun_gender(self) -> None:
        entry = {
            "rank": 10,
            "word": "casa",
            "frequency": 3,
            "dictionary_word": "casa",
            "part_of_speech": "noun",
            "grammatical_gender": ["feminine"],
            "senses": [{"glosses": ["house"]}],
        }

        selected = select_primary_entry([entry])

        self.assertEqual(selected["grammatical_gender"], ["feminine"])


if __name__ == "__main__":
    unittest.main()
