"""FinMATE 보조 텍스트 모델 학습·평가·내보내기.

    python3 ml/train.py

1. 문장 조각 라이브러리로 라벨 균형 학습 문장을 생성한다(seed 고정, 재현 가능).
2. 자유 서술 성향 모델의 하이퍼파라미터(epoch, L2, 온도, 보류 기준)를 개발셋(style_dev, 사람이 쓴 40문장)으로만 고른다.
3. 최종 모델을 생성 문장 + 개발셋으로 학습하고, 학습에 쓰지 않은 보류셋(style_holdout, 115문장)으로 한 번 평가한다.
4. 용어 검색 모델의 IDF를 247개 승인 용어로 만들고, 필드 가중치·가산점을 dev 질문으로 탐색한 뒤 test 질문으로 평가한다.
5. Spring이 읽는 모델 JSON, 모델 카드, 평가 보고서, Java 일치성 테스트용 정답 파일을 쓴다.

외부 패키지·네트워크·개인 데이터를 쓰지 않는다.
"""

from __future__ import annotations

from collections import Counter
import hashlib
import json
import math
from pathlib import Path
import sys
import time

sys.path.insert(0, str(Path(__file__).resolve().parent))

from finmate_ml import LABELS  # noqa: E402
from finmate_ml import corpus, metrics, retrieval, style  # noqa: E402
from finmate_ml.features import grams, normalize  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
ML = ROOT / "ml"
DATA = ML / "data"
REPORTS = ML / "reports"
TERMS = ROOT / "backend/src/main/resources/glossary/terms"
EXPORT = ROOT / "backend/src/main/resources/ml"
TEST_EXPORT = ROOT / "backend/src/test/resources/ml"

STYLE_VERSION = "2.0.0"
TERM_VERSION = "2.0.0"
SEED = 20261015
PER_LABEL = 300
DEV_SAMPLE_WEIGHT = 3.0
PRUNE_FLOOR = 0.05
MIN_RECOGNIZED = 4
TAG_THRESHOLD = 0.5


def read_jsonl(path: Path) -> list[dict]:
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]


def load_terms() -> list[dict]:
    terms = []
    for path in sorted(TERMS.glob("*.json")):
        terms.extend(json.loads(path.read_text(encoding="utf-8"))["terms"])
    if len({term["slug"] for term in terms}) != len(terms):
        raise ValueError("중복된 용어 slug가 있습니다.")
    return terms


def source_digest() -> str:
    return hashlib.sha256(b"".join(path.read_bytes() for path in sorted(TERMS.glob("*.json")))).hexdigest()


def max_overlap(texts: list[str], references: list[str]) -> float:
    """보류셋 문장이 학습 재료와 얼마나 겹치는지(문자 4그램 자카드). 누수 점검용."""
    reference_sets = [set(grams(text, (4,))) for text in references]
    worst = 0.0
    for text in texts:
        own = set(grams(text, (4,)))
        for other in reference_sets:
            union = own | other
            if union:
                worst = max(worst, len(own & other) / len(union))
    return round(worst, 4)


# ---------------------------------------------------------------- 자유 서술 성향

def to_samples(rows: list[dict], weight: float = 1.0) -> list[dict]:
    return [{"x": style.featurize(row["text"]), "y": row["label"], "tags": row.get("tags", []), "w": weight}
            for row in rows]


def provisional(softmax: dict, temperature: float = 1.0, floor: float = 0.0,
                informative: float = 0.0, min_recognized: int = MIN_RECOGNIZED) -> dict:
    return style.build_model(softmax, {}, {}, version="tuning", temperature=temperature, confidence_floor=floor,
                             min_recognized=min_recognized, tag_threshold=TAG_THRESHOLD, prune_floor=PRUNE_FLOOR,
                             training={}, informative_weight=informative)


def negative_log_likelihood(model: dict, rows: list[dict], temperature: float) -> float:
    total = 0.0
    for row in rows:
        probability = style.probabilities(model, row["text"], temperature)[row["label"]]
        total -= math.log(max(probability, 1e-12))
    return total / len(rows)


def tune_style(generated: list[dict], dev: list[dict], off_topic: list[dict]) -> dict:
    samples = to_samples(generated)
    candidates = []
    for epochs in (5, 8, 14):
        for l2 in (1e-4, 1e-3, 3e-3, 1e-2):
            softmax = style.train_softmax(samples, epochs=epochs, l2=l2, seed=SEED)
            model = provisional(softmax)
            predicted = [max(LABELS, key=lambda label, p=style.probabilities(model, row["text"]): p[label]) for row in dev]
            accuracy = sum(p == row["label"] for p, row in zip(predicted, dev)) / len(dev)
            candidates.append({"epochs": epochs, "l2": l2, "devAccuracy": round(accuracy, 4), "_softmax": softmax})
    best = max(candidates, key=lambda c: (c["devAccuracy"], -c["epochs"], -c["l2"]))
    model = provisional(best["_softmax"])
    temperatures = {t: round(negative_log_likelihood(model, dev, t), 4) for t in (0.2, 0.3, 0.4, 0.5, 0.75, 1.0, 1.5)}
    temperature = min(temperatures, key=lambda t: (temperatures[t], t))
    # 보류 규칙: 주제와 무관한 문장(off-topic dev)은 90% 이상 보류하면서, 투자 서술(dev)의 판단 비율은 75% 이상 유지한다.
    expected = [row["label"] for row in dev]
    abstention = []
    for informative in (0.1, 0.2, 0.3, 0.4):
        for min_recognized in (2, 4, 6, 8):
            for floor in (0.3, 0.35, 0.4, 0.45, 0.5, 0.55, 0.6):
                tuned = provisional(best["_softmax"], temperature, floor, informative, min_recognized)
                predicted = [style.predict(tuned, row["text"])["label"] for row in dev]
                rejected = sum(style.predict(tuned, row["text"])["status"] == "INSUFFICIENT" for row in off_topic)
                abstention.append({
                    "informativeWeight": informative, "minRecognized": min_recognized, "confidenceFloor": floor,
                    "devAccuracy": metrics.classification(expected, predicted)["accuracy"],
                    "devCoverage": metrics.selective(expected, predicted)["coverage"],
                    "offTopicRejection": round(rejected / len(off_topic), 4),
                })
    eligible = [c for c in abstention if c["offTopicRejection"] >= 0.9 and c["devCoverage"] >= 0.75]
    pool = eligible or abstention
    chosen = max(pool, key=lambda c: (c["devAccuracy"], c["offTopicRejection"], -c["confidenceFloor"],
                                      -c["minRecognized"], -c["informativeWeight"]) if eligible else
                 (c["offTopicRejection"], c["devAccuracy"]))
    return {
        "epochs": best["epochs"], "l2": best["l2"], "temperature": temperature,
        "confidenceFloor": chosen["confidenceFloor"], "informativeWeight": chosen["informativeWeight"],
        "minRecognized": chosen["minRecognized"], "constraintSatisfied": bool(eligible),
        "devAtChoice": {k: chosen[k] for k in ("devAccuracy", "devCoverage", "offTopicRejection")},
        "search": {
            "epochsL2": [{k: v for k, v in c.items() if not k.startswith("_")} for c in candidates],
            "temperatureNll": {str(k): v for k, v in temperatures.items()},
            "abstentionCandidates": len(abstention),
        },
    }


def legacy_naive_bayes(train_rows: list[dict]):
    """v1 기준선: 40문장 다항 나이브 베이즈."""
    by_label = {label: Counter() for label in LABELS}
    priors = Counter(row["label"] for row in train_rows)
    for row in train_rows:
        by_label[row["label"]].update(grams(row["text"]))
    vocabulary = set().union(*by_label.values())
    totals = {label: sum(by_label[label].values()) for label in LABELS}

    def classify(text: str) -> str:
        features = {g: c for g, c in grams(text).items() if g in vocabulary}
        return max(LABELS, key=lambda label: math.log(priors[label] / len(train_rows)) + sum(
            c * math.log((by_label[label][g] + 1) / (totals[label] + len(vocabulary))) for g, c in features.items()))

    return classify


def train_style(fragments: dict, generated: list[dict], dev: list[dict], holdout: list[dict],
                off_topic_dev: list[dict], off_topic_holdout: list[dict]) -> tuple[dict, dict]:
    started = time.time()
    tuned = tune_style(generated, dev, off_topic_dev)
    samples = to_samples(generated) + to_samples(dev, DEV_SAMPLE_WEIGHT)
    softmax = style.train_softmax(samples, epochs=tuned["epochs"], l2=tuned["l2"], seed=SEED)
    tag_samples = to_samples(generated)
    tag_models = {tag: style.train_binary(tag_samples, tag, epochs=10, l2=1e-4, seed=SEED)
                  for tag in fragments["tags"]}
    training = {
        "seed": SEED,
        "generatedSamples": len(generated),
        "perLabel": PER_LABEL,
        "handwrittenDevSamples": len(dev),
        "devSampleWeight": DEV_SAMPLE_WEIGHT,
        "epochs": tuned["epochs"],
        "l2": tuned["l2"],
        "tagEpochs": 10,
    }
    model = style.build_model(softmax, tag_models, fragments["tags"], version=STYLE_VERSION,
                              temperature=tuned["temperature"], confidence_floor=tuned["confidenceFloor"],
                              min_recognized=tuned["minRecognized"], tag_threshold=TAG_THRESHOLD,
                              prune_floor=PRUNE_FLOOR, training=training,
                              informative_weight=tuned["informativeWeight"])

    expected = [row["label"] for row in holdout]
    results = [style.predict(model, row["text"]) for row in holdout]
    emitted = [result["label"] for result in results]
    raw = [max(LABELS, key=lambda label, p=style.probabilities(model, row["text"]): p[label]) for row in holdout]
    confidences = [max(style.probabilities(model, row["text"]).values()) for row in holdout]
    keyword = [style.keyword_baseline(row["text"]) for row in holdout]
    legacy = legacy_naive_bayes(dev)
    report = {
        "version": STYLE_VERSION,
        "trainingSeconds": round(time.time() - started, 1),
        "data": {
            "generated": len(generated),
            "generatedLabelCounts": dict(Counter(row["label"] for row in generated)),
            "dev": len(dev),
            "holdout": len(holdout),
            "holdoutLabelCounts": dict(Counter(expected)),
            "offTopicDev": len(off_topic_dev),
            "offTopicHoldout": len(off_topic_holdout),
            "maxHoldoutOverlapWithTrainingText": max_overlap(
                [row["text"] for row in holdout], corpus.fragment_texts(fragments) + [row["text"] for row in dev]),
        },
        "hyperparameters": tuned,
        "holdout": {
            "withAbstention": metrics.classification(expected, emitted),
            "selective": metrics.selective(expected, emitted),
            "rawArgmax": metrics.classification(expected, raw),
            "expectedCalibrationError": metrics.expected_calibration_error(
                confidences, [r == e for r, e in zip(raw, expected)]),
            "confusion": metrics.confusion(expected, emitted),
            "offTopicRejection": round(sum(style.predict(model, row["text"])["status"] == "INSUFFICIENT"
                                           for row in off_topic_holdout) / len(off_topic_holdout), 4),
            "tags": metrics.multilabel([row["tags"] for row in holdout], [r["tags"] for r in results],
                                       list(fragments["tags"])),
        },
        "baselines": {
            "majorityClass": metrics.classification(expected, ["NEUTRAL"] * len(expected))["accuracy"],
            "keywordRules": metrics.classification(expected, keyword)["accuracy"],
            "v1NaiveBayes40": metrics.classification(expected, [legacy(row["text"]) for row in holdout])["accuracy"],
        },
        "errors": [{"text": row["text"], "expected": row["label"], "predicted": r["label"],
                    "confidence": round(r["confidence"], 4)}
                   for row, r in zip(holdout, results) if r["label"] != row["label"]],
    }
    golden = [{"text": row["text"], "status": r["status"], "label": r["label"],
               "confidence": round(r["confidence"], 8), "tags": r["tags"]}
              for row, r in zip(holdout, results)]
    return model, {"report": report, "golden": golden}


# ---------------------------------------------------------------- 질문형 용어 검색

def train_terms(terms: list[dict], queries: list[dict]) -> tuple[dict, dict]:
    started = time.time()
    idf, idf_max = retrieval.fit_idf(terms)
    index = retrieval.Index(terms, idf, idf_max)
    slugs = {term["slug"] for term in terms}
    unknown = {case["slug"] for case in queries} - slugs
    if unknown:
        raise ValueError(f"평가 질문의 정답 용어가 없습니다: {unknown}")
    dev = [case for case in queries if case["split"] == "dev"]
    test = [case for case in queries if case["split"] == "test"]
    params, dev_result = retrieval.grid_search(index, dev)

    by_slug = {term["slug"]: term for term in terms}
    for case in test:
        term = by_slug[case["slug"]]
        names = [term["term"], term.get("fullName") or "", term.get("english") or "", *term.get("aliases", [])]
        case["namePresent"] = any(retrieval.name_matches(name, case["question"]) for name in names if name)
    test_result = retrieval.evaluate(index, test, params)
    named = retrieval.evaluate(index, [c for c in test if c["namePresent"]], params)
    paraphrase = retrieval.evaluate(index, [c for c in test if not c["namePresent"]], params)
    legacy = retrieval.legacy_score_fn(terms)
    legacy_top1 = sum(legacy(case["question"])[:1] == [case["slug"]] for case in test) / len(test)
    legacy_paraphrase = [c for c in test if not c["namePresent"]]
    legacy_paraphrase_top1 = (sum(legacy(c["question"])[:1] == [c["slug"]] for c in legacy_paraphrase)
                              / len(legacy_paraphrase)) if legacy_paraphrase else 0.0

    model = {
        "version": TERM_VERSION,
        "algorithm": "field-weighted-tfidf-cosine-char-2-4 + exact-name-boost",
        "features": {"normalize": "lowercase, keep [a-z0-9가-힣]", "ngrams": [2, 3, 4], "tf": "1+log(tf)",
                     "idf": "ln((1+N)/(1+df))+1, unseen=idfMax"},
        "fields": {"name": "term + fullName + english + aliases", "definition": "definition",
                   "body": "explanation + caution"},
        "fieldWeights": params["fieldWeights"],
        "termBoost": params["termBoost"],
        "aliasFactor": params["aliasFactor"],
        "documentCount": len(terms),
        "sourceSha256": source_digest(),
        "idfMax": idf_max,
        "idf": idf,
    }
    report = {
        "version": TERM_VERSION,
        "trainingSeconds": round(time.time() - started, 1),
        "data": {"terms": len(terms), "devQueries": len(dev), "testQueries": len(test),
                 "testNamePresent": named["count"], "testParaphrase": paraphrase["count"]},
        "selectedParameters": params,
        "dev": {k: v for k, v in dev_result.items() if k != "errors"},
        "test": {k: v for k, v in test_result.items() if k != "errors"},
        "testNamePresent": {k: v for k, v in named.items() if k != "errors"},
        "testParaphrase": {k: v for k, v in paraphrase.items() if k != "errors"},
        "baselines": {"v1ConcatenatedTop1": round(legacy_top1, 4),
                      "v1ConcatenatedParaphraseTop1": round(legacy_paraphrase_top1, 4)},
        "errors": test_result["errors"],
    }
    golden = []
    for case in test:
        ranked = retrieval.rank(index.features(case["question"]), params, limit=5)
        golden.append({"question": case["question"], "expected": case["slug"],
                       "top": [{"slug": item["slug"], "score": round(item["score"], 10)} for item in ranked]})
    return model, {"report": report, "golden": golden}


# ---------------------------------------------------------------- 내보내기

def write_json(path: Path, payload: dict, compact: bool = False) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {k: v for k, v in payload.items() if not k.startswith("_")}
    text = json.dumps(payload, ensure_ascii=False, separators=(",", ":")) if compact else \
        json.dumps(payload, ensure_ascii=False, indent=2)
    path.write_text(text + "\n", encoding="utf-8")


def model_card(style_report: dict, term_report: dict) -> dict:
    s, t = style_report, term_report
    return {
        "generatedBy": "ml/train.py",
        "models": [
            {
                "id": "style-signal",
                "name": "자유 서술 투자 성향 신호",
                "version": s["version"],
                "task": "사용자가 쓴 투자 생각에서 5단계 성향과 관심사 태그를 추정해 설문 결과와의 차이를 알린다.",
                "algorithm": "문자 2~4그램 다항 로지스틱 회귀 + 태그별 로지스틱 회귀",
                "trainingData": f"문장 조각 라이브러리로 생성한 라벨 균형 문장 {s['data']['generated']}개 + 사람이 쓴 개발 문장 {s['data']['dev']}개",
                "evaluationData": f"학습에 쓰지 않은 사람이 쓴 보류 문장 {s['data']['holdout']}개 + 주제와 무관한 문장 {s['data']['offTopicHoldout']}개",
                "metrics": {
                    "accuracy": s["holdout"]["withAbstention"]["accuracy"],
                    "macroF1": s["holdout"]["withAbstention"]["macroF1"],
                    "withinOneGrade": s["holdout"]["withAbstention"]["withinOneGrade"],
                    "coverage": s["holdout"]["selective"]["coverage"],
                    "accuracyWhenEmitted": s["holdout"]["selective"]["accuracyWhenEmitted"],
                    "tagMicroF1": s["holdout"]["tags"]["microF1"],
                    "expectedCalibrationError": s["holdout"]["expectedCalibrationError"],
                    "offTopicRejection": s["holdout"]["offTopicRejection"],
                },
                "baselines": s["baselines"],
                "intendedUse": "설문과 다른 신호가 있을 때 답변을 다시 확인하도록 돕고, 관심사에 맞는 학습 개념을 추천한다.",
                "notIntendedUse": "투자 성향 등급 판정, 금융상품 적합성·적정성 판단, 투자 권유",
                "limitations": [
                    "학습 문장 대부분은 실제 사용자 기록이 아닌 생성 문장이다. 실제 사용자 문장에서는 정확도가 다를 수 있다.",
                    "보류 문장 115개는 한 사람이 작성해 문체가 제한적이다.",
                    "반어·부정('손실이 무섭지 않다고 하면 거짓말')처럼 문맥이 필요한 표현에 약하다.",
                    "'~하고 싶어요'처럼 흔한 어미가 많은 짧은 잡담은 일부 성향으로 분류될 수 있다(주제 무관 개발 문장 30개 중 3개).",
                    "원문 자유 서술은 저장하지 않고 추정 결과만 진단 스냅샷에 남긴다.",
                ],
            },
            {
                "id": "term-retrieval",
                "name": "질문형 주식 용어 검색",
                "version": t["version"],
                "task": "용어 이름을 몰라도 설명형 질문으로 관련 용어를 찾는다.",
                "algorithm": "필드별 문자 2~4그램 TF-IDF 코사인 가중합 + 용어명 일치 가산점",
                "trainingData": f"승인된 용어 {t['data']['terms']}개(IDF), 개발 질문 {t['data']['devQueries']}개(가중치 탐색)",
                "evaluationData": f"평가 질문 {t['data']['testQueries']}개(이름 포함 {t['data']['testNamePresent']}, 설명형 {t['data']['testParaphrase']})",
                "metrics": {
                    "top1": t["test"]["top1"], "top5": t["test"]["top5"], "mrr": t["test"]["mrr"],
                    "paraphraseTop1": t["testParaphrase"]["top1"], "paraphraseTop5": t["testParaphrase"]["top5"],
                },
                "baselines": t["baselines"],
                "intendedUse": "용어 사전 검색, 학습 화면에서 모르는 표현 찾기",
                "notIntendedUse": "종목 추천, 시세·수익률 예측",
                "limitations": [
                    "의미 임베딩이 아니라 문자 조각 일치에 기반하므로 전혀 다른 단어로 말한 질문은 놓칠 수 있다.",
                    "관리자가 새 용어를 추가하면 IDF에 없는 조각은 최댓값으로 취급하므로 재학습 전까지 가중치가 근사값이다.",
                ],
            },
        ],
    }


def main() -> None:
    fragments = corpus.load_fragments(DATA / "style_fragments.json")
    generated = corpus.generate(fragments, PER_LABEL, SEED)
    dev = read_jsonl(DATA / "style_dev.jsonl")
    holdout = read_jsonl(DATA / "style_holdout.jsonl")
    if {normalize(row["text"]) for row in dev} & {normalize(row["text"]) for row in holdout}:
        raise ValueError("개발셋과 보류셋에 같은 문장이 있습니다.")

    off_topic_dev = read_jsonl(DATA / "style_offtopic_dev.jsonl")
    off_topic_holdout = read_jsonl(DATA / "style_offtopic_holdout.jsonl")
    style_model, style_out = train_style(fragments, generated, dev, holdout, off_topic_dev, off_topic_holdout)
    term_model, term_out = train_terms(load_terms(), read_jsonl(DATA / "term_queries.jsonl"))

    write_json(EXPORT / "style-model.json", style_model, compact=True)
    write_json(EXPORT / "term-model.json", term_model, compact=True)
    write_json(EXPORT / "model-card.json", model_card(style_out["report"], term_out["report"]))
    write_json(REPORTS / "evaluation.json", {"style": style_out["report"], "termRetrieval": term_out["report"]})
    (REPORTS / "style_corpus_sample.jsonl").write_text(
        "".join(json.dumps(row, ensure_ascii=False) + "\n" for row in generated[:60]), encoding="utf-8")
    write_json(TEST_EXPORT / "golden-predictions.json", {
        "styleVersion": STYLE_VERSION, "termVersion": TERM_VERSION,
        "style": style_out["golden"], "terms": term_out["golden"],
    })
    stale = TEST_EXPORT / "evaluation-cases.json"
    if stale.exists():
        stale.unlink()

    s, t = style_out["report"], term_out["report"]
    print(json.dumps({
        "style": {"accuracy": s["holdout"]["withAbstention"]["accuracy"],
                  "macroF1": s["holdout"]["withAbstention"]["macroF1"],
                  "coverage": s["holdout"]["selective"]["coverage"],
                  "accuracyWhenEmitted": s["holdout"]["selective"]["accuracyWhenEmitted"],
                  "rawArgmax": s["holdout"]["rawArgmax"]["accuracy"],
                  "withinOneGrade": s["holdout"]["withAbstention"]["withinOneGrade"],
                  "offTopicRejection": s["holdout"]["offTopicRejection"],
                  "tagMicroF1": s["holdout"]["tags"]["microF1"],
                  "baselines": s["baselines"], "seconds": s["trainingSeconds"],
                  "hyper": {k: v for k, v in s["hyperparameters"].items() if k != "search"}},
        "terms": {"test": {k: t["test"][k] for k in ("top1", "top5", "mrr")},
                  "paraphrase": {k: t["testParaphrase"][k] for k in ("top1", "top5", "mrr")},
                  "baselines": t["baselines"], "params": t["selectedParameters"]},
    }, ensure_ascii=False, indent=1))


if __name__ == "__main__":
    main()
