"""학습 파이프라인 회귀 테스트. 내보낸 모델이 소스·데이터와 맞는지, 평가 기준을 지키는지 확인한다.

    python3 -m unittest discover -s ml -p 'test_*.py'
"""

import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

import train
from finmate_ml import LABELS, corpus, label_for, retrieval, style
from finmate_ml.features import grams, name_matches, normalize


def load(name):
    return json.loads((train.EXPORT / name).read_text(encoding="utf-8"))


class FeatureTest(unittest.TestCase):
    def test_source_digest_is_independent_of_checkout_line_endings(self):
        with TemporaryDirectory() as directory:
            terms = Path(directory)
            source = terms / "01-example.json"
            with patch.object(train, "TERMS", terms):
                source.write_bytes(b'{"terms": []}\n')
                expected = train.source_digest()
                source.write_bytes(b'{"terms": []}\r\n')
                self.assertEqual(train.source_digest(), expected)
                source.write_bytes(b'{"terms": [1]}\n')
                self.assertNotEqual(train.source_digest(), expected)

    def test_normalize_keeps_hangul_ascii_digits_only(self):
        self.assertEqual(normalize("PER이 10배, ETF(합성)!"), "per이10배etf합성")
        self.assertEqual(sum(grams("abcd").values()), 3 + 2 + 1)

    def test_ascii_names_match_whole_tokens_only(self):
        self.assertTrue(name_matches("PER", "PER이 낮으면 싼가요?"))
        self.assertFalse(name_matches("IV", "derivative 상품"))
        self.assertTrue(name_matches("공매도", "공 매도는 뭔가요"))

    def test_label_thresholds_match_rule_engine(self):
        self.assertEqual([label_for(s) for s in (81, 80, 66, 65, 46, 45, 26, 25)],
                         ["AGGRESSIVE", "ACTIVE", "ACTIVE", "NEUTRAL", "NEUTRAL", "CONSERVATIVE", "CONSERVATIVE", "STABLE"])


class DataTest(unittest.TestCase):
    def test_holdout_is_separate_from_training_material(self):
        dev = train.read_jsonl(train.DATA / "style_dev.jsonl")
        holdout = train.read_jsonl(train.DATA / "style_holdout.jsonl")
        fragments = corpus.load_fragments(train.DATA / "style_fragments.json")
        self.assertTrue({normalize(x["text"]) for x in dev}.isdisjoint({normalize(x["text"]) for x in holdout}))
        overlap = train.max_overlap([x["text"] for x in holdout],
                                    corpus.fragment_texts(fragments) + [x["text"] for x in dev])
        self.assertLess(overlap, 0.5)
        self.assertEqual({x["label"] for x in holdout}, set(LABELS))

    def test_generator_is_deterministic_and_balanced(self):
        fragments = corpus.load_fragments(train.DATA / "style_fragments.json")
        first = corpus.generate(fragments, 20, seed=7)
        second = corpus.generate(fragments, 20, seed=7)
        self.assertEqual(first, second)
        self.assertEqual({label: sum(x["label"] == label for x in first) for label in LABELS},
                         {label: 20 for label in LABELS})

    def test_query_labels_exist_and_splits_do_not_overlap(self):
        queries = train.read_jsonl(train.DATA / "term_queries.jsonl")
        slugs = {term["slug"] for term in train.load_terms()}
        self.assertTrue({q["slug"] for q in queries} <= slugs)
        dev = {q["question"] for q in queries if q["split"] == "dev"}
        test = {q["question"] for q in queries if q["split"] == "test"}
        self.assertTrue(dev and test and dev.isdisjoint(test))


class ExportTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.style_model = load("style-model.json")
        cls.term_model = load("term-model.json")
        cls.report = json.loads((train.REPORTS / "evaluation.json").read_text(encoding="utf-8"))

    def test_term_model_matches_current_glossary_source(self):
        self.assertEqual(self.term_model["sourceSha256"], train.source_digest())
        self.assertEqual(self.term_model["documentCount"], len(train.load_terms()))

    def test_style_model_beats_baselines_on_holdout(self):
        holdout = self.report["style"]["holdout"]
        baselines = self.report["style"]["baselines"]
        self.assertGreaterEqual(holdout["withAbstention"]["accuracy"], 0.55)
        self.assertGreaterEqual(holdout["withAbstention"]["withinOneGrade"], 0.85)
        self.assertGreater(holdout["withAbstention"]["accuracy"], baselines["keywordRules"])
        self.assertGreater(holdout["withAbstention"]["accuracy"], baselines["v1NaiveBayes40"])

    def test_term_retrieval_beats_v1_and_keeps_top5_recall(self):
        result = self.report["termRetrieval"]
        self.assertGreaterEqual(result["test"]["top1"], 0.8)
        self.assertGreaterEqual(result["test"]["top5"], 0.95)
        self.assertGreater(result["testParaphrase"]["top1"], result["baselines"]["v1ConcatenatedParaphraseTop1"])

    def test_short_or_unknown_text_abstains(self):
        for text in ("안녕", "!!!!!", "ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ"):
            self.assertEqual(style.predict(self.style_model, text)["status"], "INSUFFICIENT")

    def test_exported_model_reproduces_golden_predictions(self):
        golden = json.loads((train.TEST_EXPORT / "golden-predictions.json").read_text(encoding="utf-8"))
        for case in golden["style"][:20]:
            result = style.predict(self.style_model, case["text"])
            self.assertEqual(result["label"], case["label"])
            self.assertAlmostEqual(result["confidence"], case["confidence"], places=6)
        terms = train.load_terms()
        index = retrieval.Index(terms, self.term_model["idf"], self.term_model["idfMax"])
        params = {key: self.term_model[key] for key in ("fieldWeights", "termBoost", "aliasFactor")}
        for case in golden["terms"][:10]:
            ranked = retrieval.rank(index.features(case["question"]), params, limit=5)
            self.assertEqual([item["slug"] for item in ranked], [item["slug"] for item in case["top"]])


if __name__ == "__main__":
    unittest.main()
