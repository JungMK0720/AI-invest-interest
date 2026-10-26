"""문장 조각 라이브러리로 라벨이 있는 자유 서술 학습 문장을 결정적으로 생성한다.

생성 방식
1. 페르소나의 목표 위험 점수(0~100)를 고른다.
2. 목적·손실 반응 중 하나 이상과 기간·경험·여력 슬롯 일부를 고르고, 각 슬롯에서 목표 점수에
   가까운 조각일수록 높은 확률로 뽑는다. 완벽히 일관된 사람은 없으므로 먼 조각도 낮은 확률로 섞인다.
3. 라벨은 목표 점수가 아니라 실제로 뽑힌 조각 점수의 가중 평균(+작은 잡음)으로 정한다.
   문장에 드러난 내용과 라벨이 어긋나지 않게 하기 위해서다.
4. 라벨별 같은 개수가 될 때까지 반복해 클래스 불균형을 없앤다.
"""

from __future__ import annotations

import json
import math
from pathlib import Path
import random

from . import LABELS, label_for
from .features import normalize

# 해요체 일부를 합쇼체로 바꿔 문체 다양성을 만든다. 긴 어미부터 검사한다.
_FORMAL_ENDINGS = (
    ("거예요", "것입니다"), ("이에요", "입니다"), ("예요", "입니다"), ("싶어요", "싶습니다"),
    ("있어요", "있습니다"), ("없어요", "없습니다"), ("좋아요", "좋습니다"), ("같아요", "같습니다"),
    ("싫어요", "싫습니다"), ("몰라요", "모릅니다"), ("돼요", "됩니다"), ("해요", "합니다"),
    ("봤어요", "봤습니다"), ("됐어요", "됐습니다"), ("많아요", "많습니다"), ("급해요", "급합니다"),
    ("막막해요", "막막합니다"), ("충분해요", "충분합니다"), ("편해요", "편합니다"), ("덤덤해요", "덤덤합니다"),
)


def load_fragments(path: Path) -> dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    for name, slot in data["slots"].items():
        if not slot["options"]:
            raise ValueError(f"빈 슬롯: {name}")
        for option in slot["options"]:
            unknown = set(option["tags"]) - set(data["tags"])
            if unknown:
                raise ValueError(f"정의되지 않은 태그 {unknown} in {name}")
    return data


def fragment_texts(fragments: dict) -> list[str]:
    texts = [text for slot in fragments["slots"].values() for option in slot["options"] for text in option["texts"]]
    texts += [text for option in fragments["extras"]["options"] for text in option["texts"]]
    return texts


def _formal(text: str) -> str:
    for casual, formal in _FORMAL_ENDINGS:
        if text.endswith(casual):
            return text[: -len(casual)] + formal
    return text


def _pick(options: list[dict], target: float, rng: random.Random, spread: float) -> dict:
    weights = [math.exp(-abs(option["score"] - target) / spread) for option in options]
    return rng.choices(options, weights=weights, k=1)[0]


def generate(fragments: dict, per_label: int, seed: int) -> list[dict]:
    rng = random.Random(seed)
    slots = fragments["slots"]
    extras = fragments["extras"]
    buckets: dict[str, list[dict]] = {label: [] for label in LABELS}
    seen: set[str] = set()
    attempts = 0
    while any(len(items) < per_label for items in buckets.values()):
        attempts += 1
        if attempts > per_label * len(LABELS) * 200:
            raise RuntimeError("생성 시도 한도를 넘었습니다. 조각 점수 분포를 확인하세요.")
        target = rng.uniform(0, 100)
        names = rng.sample(["goal", "loss"], k=rng.choice([1, 2, 2]))
        names += [name for name in ("horizon", "experience", "capacity") if rng.random() < 0.45]
        parts, tags = [], set()
        weighted = weight_sum = 0.0
        for name in names:
            option = _pick(slots[name]["options"], target, rng, spread=15)
            parts.append(rng.choice(option["texts"]))
            weighted += slots[name]["weight"] * option["score"]
            weight_sum += slots[name]["weight"]
            tags.update(option["tags"])
        if rng.random() < 0.35:
            option = _pick(extras["options"], target, rng, spread=30)
            parts.append(rng.choice(option["texts"]))
            weighted += extras["weight"] * option["score"]
            weight_sum += extras["weight"]
            tags.update(option["tags"])
        score = weighted / weight_sum + rng.gauss(0, 4)
        label = label_for(score)
        if len(buckets[label]) >= per_label:
            continue
        rng.shuffle(parts)
        sentence = []
        for index, part in enumerate(parts):
            text = _formal(part) if rng.random() < 0.35 else part
            connector = "" if index == 0 else rng.choice(fragments["connectors"])
            sentence.append(connector + text)
        text = " ".join(sentence)
        key = normalize(text)
        if key in seen:
            continue
        seen.add(key)
        buckets[label].append({"text": text, "label": label, "tags": sorted(tags), "score": round(score, 2)})
    corpus = [item for label in LABELS for item in buckets[label]]
    rng.shuffle(corpus)
    return corpus
