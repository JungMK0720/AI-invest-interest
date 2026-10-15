import hashlib
import json
from pathlib import Path
import unittest

import train


class PipelineTest(unittest.TestCase):
    def test_holdout_examples_are_distinct_and_retrieval_labels_exist(self):
        training = train.read_jsonl(train.ML / "style_train.jsonl")
        testing = train.read_jsonl(train.ML / "style_test.jsonl")
        self.assertTrue({train.normalized(x["text"]) for x in training}.isdisjoint(
            {train.normalized(x["text"]) for x in testing}))
        self.assertEqual({x["label"] for x in training}, set(train.LABELS))
        slugs = {term["slug"] for term in train.load_terms()}
        self.assertTrue({x["slug"] for x in train.read_jsonl(train.ML / "term_test.jsonl")} <= slugs)

    def test_export_matches_source_and_meets_small_holdout_baseline(self):
        style = json.loads((train.EXPORT / "style-model.json").read_text(encoding="utf-8"))
        term = json.loads((train.EXPORT / "term-model.json").read_text(encoding="utf-8"))
        sources = b"".join(path.read_bytes() for path in sorted(train.TERMS.glob("*.json")))
        self.assertEqual(term["sourceSha256"], hashlib.sha256(sources).hexdigest())
        report = train.evaluate(style, term)
        self.assertGreaterEqual(report["style"]["accuracy"], 0.8)
        self.assertGreaterEqual(report["termRetrieval"]["top1Accuracy"], 0.9)
        self.assertEqual(report["termRetrieval"]["top5Recall"], 1.0)

    def test_short_or_unknown_text_abstains(self):
        style = json.loads((train.EXPORT / "style-model.json").read_text(encoding="utf-8"))
        self.assertEqual(train.predict_style(style, "안녕"), (None, 0.0))
        self.assertEqual(train.predict_style(style, "!!!!!"), (None, 0.0))


if __name__ == "__main__":
    unittest.main()
