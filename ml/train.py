"""Train and evaluate two small, reproducible Korean text models.

No network, API key, or third-party Python package is needed. The exported JSON
is loaded by Spring Boot; Python is never invoked by a user request.
"""

from __future__ import annotations

from collections import Counter
import hashlib
import json
import math
from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[1]
ML = ROOT / "ml"
TERMS = ROOT / "backend/src/main/resources/glossary/terms"
EXPORT = ROOT / "backend/src/main/resources/ml"
TEST_EXPORT = ROOT / "backend/src/test/resources/ml"
LABELS = ("AGGRESSIVE", "ACTIVE", "NEUTRAL", "CONSERVATIVE", "STABLE")
VERSION = "1.0.0"


def normalized(text: str) -> str:
    """Keep Hangul and ASCII letters/digits; Java inference uses the same rule."""
    return re.sub(r"[^a-z0-9가-힣]", "", text.lower())


def grams(text: str) -> Counter[str]:
    value = normalized(text)
    return Counter(value[i : i + width] for width in (2, 3, 4)
                   for i in range(len(value) - width + 1))


def read_jsonl(path: Path) -> list[dict]:
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]


def load_terms() -> list[dict]:
    terms = []
    for path in sorted(TERMS.glob("*.json")):
        terms.extend(json.loads(path.read_text(encoding="utf-8"))["terms"])
    slugs = [term["slug"] for term in terms]
    if len(slugs) != len(set(slugs)):
        raise ValueError("Duplicate glossary slug")
    return terms


def train_style(samples: list[dict]) -> dict:
    by_label: dict[str, Counter[str]] = {label: Counter() for label in LABELS}
    class_counts = Counter()
    for item in samples:
        label = item["label"]
        if label not in by_label:
            raise ValueError(f"Unknown investment style: {label}")
        class_counts[label] += 1
        by_label[label].update(grams(item["text"]))
    vocab = set().union(*(set(counts) for counts in by_label.values()))
    return {
        "version": VERSION,
        "algorithm": "multinomial-naive-bayes-char-2-4",
        "labels": list(LABELS),
        "sampleCount": len(samples),
        "vocabSize": len(vocab),
        "classCounts": dict(class_counts),
        "featureTotals": {label: sum(by_label[label].values()) for label in LABELS},
        "featureCounts": {label: dict(sorted(by_label[label].items())) for label in LABELS},
        "notice": "보조 신호입니다. 설문 점수와 투자 성향 등급을 변경하지 않습니다.",
    }


def predict_style(model: dict, text: str) -> tuple[str | None, float]:
    features = grams(text)
    vocab = set().union(*(set(counts) for counts in model["featureCounts"].values()))
    recognized = {gram: count for gram, count in features.items() if gram in vocab}
    if len(normalized(text)) < 8 or sum(recognized.values()) < 4:
        return None, 0.0
    total_examples = sum(model["classCounts"].values())
    scores = {}
    for label in model["labels"]:
        score = math.log(model["classCounts"][label] / total_examples)
        counts = model["featureCounts"][label]
        denominator = model["featureTotals"][label] + model["vocabSize"]
        score += sum(n * math.log((counts.get(gram, 0) + 1) / denominator)
                     for gram, n in recognized.items())
        scores[label] = score
    best = max(scores, key=scores.get)
    # A tempered posterior avoids reporting false precision from correlated n-grams.
    peak = max(scores.values())
    weights = {label: math.exp((score - peak) / 8) for label, score in scores.items()}
    return best, weights[best] / sum(weights.values())


def term_text(term: dict) -> str:
    title = " ".join(filter(None, [term["term"], term.get("fullName", ""), term.get("english", "")]))
    aliases = " ".join(term.get("aliases", []))
    return " ".join([title] * 5 + [aliases] * 3 +
                    [term.get("definition", ""), term.get("explanation", "")[:180]])


def train_terms(terms: list[dict]) -> dict:
    docs = [{"slug": item["slug"], "term": item["term"],
             "aliases": item.get("aliases", []), "text": term_text(item)} for item in terms]
    document_frequency = Counter()
    for doc in docs:
        document_frequency.update(set(grams(doc["text"])))
    document_count = len(docs)
    idf = {gram: round(math.log((1 + document_count) / (1 + df)) + 1, 8)
           for gram, df in sorted(document_frequency.items())}
    sources = b"".join(path.read_bytes() for path in sorted(TERMS.glob("*.json")))
    return {
        "version": VERSION,
        "algorithm": "tfidf-cosine-char-2-4-with-exact-name-boost",
        "documentCount": document_count,
        "sourceSha256": hashlib.sha256(sources).hexdigest(),
        "idf": idf,
        "documents": docs,
    }


def vector(counts: Counter[str], idf: dict[str, float]) -> tuple[dict[str, float], float]:
    weights = {gram: (1 + math.log(count)) * idf[gram] for gram, count in counts.items()
               if gram in idf and count > 0}
    norm = math.sqrt(sum(weight * weight for weight in weights.values()))
    return weights, norm


def retrieve(model: dict, question: str, limit: int = 5) -> list[dict]:
    query, q_norm = vector(grams(question), model["idf"])
    if not q_norm:
        return []
    query_compact = normalized(question)
    ranked = []
    for doc in model["documents"]:
        doc_vector, d_norm = vector(grams(doc["text"]), model["idf"])
        score = (sum(weight * doc_vector.get(gram, 0) for gram, weight in query.items())
                 / (q_norm * d_norm)) if d_norm else 0
        names = [doc["term"], *doc["aliases"]]
        if any(len(name_normalized := normalized(name)) >= 2 and name_normalized in query_compact
               for name in names):
            score += 0.35
        if score > 0:
            ranked.append({"slug": doc["slug"], "term": doc["term"],
                           "score": round(min(score, 1.0), 4)})
    return sorted(ranked, key=lambda item: (-item["score"], item["slug"]))[:limit]


def evaluate(style: dict, terms: dict) -> dict:
    style_test = read_jsonl(ML / "style_test.jsonl")
    term_test = read_jsonl(ML / "term_test.jsonl")
    slugs = {doc["slug"] for doc in terms["documents"]}
    for case in term_test:
        if case["slug"] not in slugs:
            raise ValueError(f"Unknown evaluation term: {case['slug']}")
    style_inference = [predict_style(style, case["text"]) for case in style_test]
    style_predictions = [prediction for prediction, _ in style_inference]
    emitted = [(prediction, case) for (prediction, confidence), case in zip(style_inference, style_test)
               if prediction is not None and confidence >= 0.35]
    term_predictions = [retrieve(terms, case["question"], 5) for case in term_test]
    style_correct = sum(pred == case["label"] for pred, case in zip(style_predictions, style_test))
    top1 = sum(bool(pred) and pred[0]["slug"] == case["slug"]
               for pred, case in zip(term_predictions, term_test))
    top5 = sum(any(item["slug"] == case["slug"] for item in pred)
               for pred, case in zip(term_predictions, term_test))
    return {
        "modelVersion": VERSION,
        "style": {"trainCount": style["sampleCount"], "testCount": len(style_test),
                  "correct": style_correct, "accuracy": round(style_correct / len(style_test), 4),
                  "emittedCount": len(emitted),
                  "emittedCorrect": sum(prediction == case["label"] for prediction, case in emitted),
                  "errors": [{"text": case["text"], "expected": case["label"], "predicted": pred}
                             for pred, case in zip(style_predictions, style_test)
                             if pred != case["label"]]},
        "termRetrieval": {"documentCount": terms["documentCount"], "testCount": len(term_test),
                          "top1Correct": top1, "top1Accuracy": round(top1 / len(term_test), 4),
                          "top5Correct": top5, "top5Recall": round(top5 / len(term_test), 4),
                          "errors": [{"question": case["question"], "expected": case["slug"],
                                      "predicted": [item["slug"] for item in pred]}
                                     for pred, case in zip(term_predictions, term_test)
                                     if not pred or pred[0]["slug"] != case["slug"]]},
        "limitations": "작은 수작업 평가셋이며, 투자 적합성 판정 또는 실제 고객 성능을 입증하지 않습니다.",
    }


def main() -> None:
    style = train_style(read_jsonl(ML / "style_train.jsonl"))
    term = train_terms(load_terms())
    report = evaluate(style, term)
    EXPORT.mkdir(parents=True, exist_ok=True)
    for filename, payload in (("style-model.json", style), ("term-model.json", term)):
        (EXPORT / filename).write_text(json.dumps(payload, ensure_ascii=False, separators=(",", ":")) + "\n",
                                       encoding="utf-8")
    (ML / "evaluation.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n",
                                         encoding="utf-8")
    TEST_EXPORT.mkdir(parents=True, exist_ok=True)
    (TEST_EXPORT / "evaluation-cases.json").write_text(json.dumps({
        "style": read_jsonl(ML / "style_test.jsonl"),
        "terms": read_jsonl(ML / "term_test.jsonl"),
    }, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"styleAccuracy": report["style"]["accuracy"],
                      "termTop1": report["termRetrieval"]["top1Accuracy"],
                      "termTop5": report["termRetrieval"]["top5Recall"]}))


if __name__ == "__main__":
    main()
