"""자유 서술 성향 신호 모델.

- 성향: 문자 2~4그램 특징의 다항 로지스틱 회귀(softmax), SGD + L2
- 관심사 태그: 같은 특징의 일대다(one-vs-rest) 로지스틱 회귀
- 확신도: 온도(temperature)로 보정한 softmax 최댓값. 기준 미만이면 판단을 보류한다.

모델은 설문 점수·등급을 바꾸지 않는다. 설문 결과와 다른 신호가 있는지 알려 주는 보조 정보다.
"""

from __future__ import annotations

import math
import random

from . import LABELS
from .features import grams, normalize, unit_vector

MIN_CHARACTERS = 8


def featurize(text: str) -> dict[str, float]:
    return unit_vector(grams(text))


def _softmax(scores: list[float], temperature: float = 1.0) -> list[float]:
    scaled = [score / temperature for score in scores]
    peak = max(scaled)
    exps = [math.exp(value - peak) for value in scaled]
    total = sum(exps)
    return [value / total for value in exps]


def _sigmoid(value: float) -> float:
    if value >= 0:
        return 1.0 / (1.0 + math.exp(-value))
    exp = math.exp(value)
    return exp / (1.0 + exp)


def train_softmax(samples: list[dict], epochs: int, l2: float, seed: int, learning_rate: float = 0.5) -> dict:
    """samples: {"x": 특징 dict, "y": 라벨, "w": 표본 가중치(선택)}."""
    rng = random.Random(seed)
    weights = [dict() for _ in LABELS]
    bias = [0.0] * len(LABELS)
    index_of = {label: i for i, label in enumerate(LABELS)}
    order = list(range(len(samples)))
    for epoch in range(epochs):
        rng.shuffle(order)
        rate = learning_rate / (1.0 + epoch * 0.5)
        for i in order:
            sample = samples[i]
            x, target, sample_weight = sample["x"], index_of[sample["y"]], sample.get("w", 1.0)
            scores = [bias[c] + sum(weights[c].get(g, 0.0) * v for g, v in x.items()) for c in range(len(LABELS))]
            probabilities = _softmax(scores)
            for c in range(len(LABELS)):
                gradient = (probabilities[c] - (1.0 if c == target else 0.0)) * sample_weight
                bias[c] -= rate * gradient
                row = weights[c]
                for g, v in x.items():
                    # 등장한 특징에만 L2 감쇠를 적용하는 지연 정규화(lazy L2)
                    row[g] = row.get(g, 0.0) * (1.0 - rate * l2) - rate * gradient * v
    return {"weights": weights, "bias": bias}


def train_binary(samples: list[dict], tag: str, epochs: int, l2: float, seed: int, learning_rate: float = 0.5) -> dict:
    rng = random.Random(seed)
    weights: dict[str, float] = {}
    bias = 0.0
    positives = sum(tag in sample["tags"] for sample in samples)
    # 양성 표본이 적은 태그도 학습되도록 클래스 가중치를 맞춘다.
    positive_weight = (len(samples) - positives) / max(positives, 1)
    order = list(range(len(samples)))
    for epoch in range(epochs):
        rng.shuffle(order)
        rate = learning_rate / (1.0 + epoch * 0.5)
        for i in order:
            sample = samples[i]
            label = 1.0 if tag in sample["tags"] else 0.0
            weight = positive_weight if label else 1.0
            score = bias + sum(weights.get(g, 0.0) * v for g, v in sample["x"].items())
            gradient = (_sigmoid(score) - label) * weight
            bias -= rate * gradient
            for g, v in sample["x"].items():
                weights[g] = weights.get(g, 0.0) * (1.0 - rate * l2) - rate * gradient * v
    return {"weights": weights, "bias": bias}


def prune(weights: dict[str, float], floor: float) -> dict[str, float]:
    return {g: round(w, 6) for g, w in sorted(weights.items()) if abs(w) >= floor}


def build_model(softmax: dict, tag_models: dict[str, dict], tag_names: dict[str, str], *, version: str,
                temperature: float, confidence_floor: float, min_recognized: int, tag_threshold: float,
                prune_floor: float, training: dict, informative_weight: float = 0.0) -> dict:
    class_weights = {label: prune(softmax["weights"][i], prune_floor) for i, label in enumerate(LABELS)}
    vocabulary = sorted(set().union(*class_weights.values()))
    return {
        "version": version,
        "algorithm": "softmax-logistic-regression-char-2-4 + one-vs-rest-tags",
        "labels": list(LABELS),
        "features": {"normalize": "lowercase, keep [a-z0-9가-힣]", "ngrams": [2, 3, 4], "tf": "1+log(tf)", "norm": "l2"},
        "minCharacters": MIN_CHARACTERS,
        # 어느 성향이든 |가중치|가 이 값 이상인 조각만 '근거'로 센다. 어미·조사처럼 흔한 조각만 있으면 판단을 보류한다.
        "informativeWeight": informative_weight,
        "minRecognized": min_recognized,
        "temperature": temperature,
        "confidenceFloor": confidence_floor,
        # 설문 등급과 2단계 이상 차이 나고 확신도가 이 값 이상일 때만 불일치로 표시한다.
        "mismatchMinGap": 2,
        "mismatchMinConfidence": 0.5,
        "vocabularySize": len(vocabulary),
        "bias": {label: round(softmax["bias"][i], 6) for i, label in enumerate(LABELS)},
        "weights": class_weights,
        "tagThreshold": tag_threshold,
        "tags": {tag: {"label": tag_names[tag], "bias": round(model["bias"], 6),
                       "weights": prune(model["weights"], prune_floor)}
                 for tag, model in tag_models.items()},
        "training": training,
        "notice": "자유 서술과 설문 결과의 차이를 살피는 교육용 보조 신호입니다. 설문 점수와 투자 성향 등급은 변경되지 않습니다.",
    }


_INFORMATIVE_CACHE: dict[tuple[int, float], set[str]] = {}


def informative_grams(model: dict) -> set[str]:
    key = (id(model["weights"]), model["informativeWeight"])
    if key not in _INFORMATIVE_CACHE:
        floor = model["informativeWeight"]
        strongest: dict[str, float] = {}
        for row in model["weights"].values():
            for gram, weight in row.items():
                strongest[gram] = max(strongest.get(gram, 0.0), abs(weight))
        _INFORMATIVE_CACHE[key] = {gram for gram, weight in strongest.items() if weight >= floor}
    return _INFORMATIVE_CACHE[key]


def recognized_count(model: dict, text: str) -> int:
    vocabulary = informative_grams(model)
    return sum(count for gram, count in grams(text).items() if gram in vocabulary)


def probabilities(model: dict, text: str, temperature: float | None = None) -> dict[str, float]:
    x = featurize(text)
    scores = [model["bias"][label] + sum(model["weights"][label].get(g, 0.0) * v for g, v in x.items())
              for label in model["labels"]]
    probs = _softmax(scores, model["temperature"] if temperature is None else temperature)
    return dict(zip(model["labels"], probs))


def predict(model: dict, text: str) -> dict:
    """Java StyleInferenceService.check와 같은 판단 순서를 따른다."""
    if len(normalize(text)) < model["minCharacters"] or recognized_count(model, text) < model["minRecognized"]:
        return {"status": "INSUFFICIENT", "label": None, "confidence": 0.0, "probabilities": {}, "tags": []}
    probs = probabilities(model, text)
    best = max(model["labels"], key=lambda label: probs[label])
    tags = predict_tags(model, text)
    if probs[best] < model["confidenceFloor"]:
        return {"status": "INSUFFICIENT", "label": None, "confidence": probs[best], "probabilities": probs, "tags": tags}
    return {"status": "ESTIMATED", "label": best, "confidence": probs[best], "probabilities": probs, "tags": tags}


def tag_scores(model: dict, text: str) -> dict[str, float]:
    x = featurize(text)
    return {tag: _sigmoid(spec["bias"] + sum(spec["weights"].get(g, 0.0) * v for g, v in x.items()))
            for tag, spec in model["tags"].items()}


def predict_tags(model: dict, text: str) -> list[str]:
    scores = tag_scores(model, text)
    return sorted(tag for tag, score in scores.items() if score >= model["tagThreshold"])


# ---------------------------------------------------------------- 비교 기준선

def keyword_baseline(text: str) -> str:
    """사람이 정한 키워드 규칙. 학습 모델이 이 정도는 넘어야 의미가 있다."""
    value = normalize(text)
    if any(word in value for word in ("레버리지", "몰빵", "대박", "신용", "대출", "옵션", "선물", "몇배", "두세배", "반토막")):
        return "AGGRESSIVE"
    if any(word in value for word in ("성장주", "공격적", "적극", "높은수익", "고수익")):
        return "ACTIVE"
    if any(word in value for word in ("원금보장", "예금", "적금", "안전", "잃고싶지", "한푼")):
        return "STABLE"
    if any(word in value for word in ("채권", "안정", "소폭", "조금")):
        return "CONSERVATIVE"
    return "NEUTRAL"
