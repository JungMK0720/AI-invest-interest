"""문자 n-그램 특징. Java의 com.finmate.ml.TextFeatures와 한 글자도 다르지 않아야 한다."""

from __future__ import annotations

from collections import Counter
import math
import re

_NON_TOKEN = re.compile(r"[^a-z0-9가-힣]")
_ASCII_SPLIT = re.compile(r"[^a-z0-9]+")
_HANGUL = re.compile(r"[가-힣]")


def normalize(text: str) -> str:
    """소문자화 후 한글 음절·영문 소문자·숫자만 남긴다. 띄어쓰기 차이를 없애기 위해 공백도 지운다."""
    return _NON_TOKEN.sub("", (text or "").lower())


def grams(text: str, widths: tuple[int, ...] = (2, 3, 4)) -> Counter[str]:
    value = normalize(text)
    return Counter(value[i:i + width] for width in widths for i in range(len(value) - width + 1))


def unit_vector(counts: Counter[str] | dict[str, int]) -> dict[str, float]:
    """1 + log(tf)로 줄인 뒤 L2 정규화한다. 긴 문장이 점수를 독식하지 않게 한다."""
    weights = {gram: 1.0 + math.log(count) for gram, count in counts.items() if count > 0}
    norm = math.sqrt(sum(value * value for value in weights.values()))
    return {gram: value / norm for gram, value in weights.items()} if norm else {}


def ascii_tokens(text: str) -> set[str]:
    return {token for token in _ASCII_SPLIT.split((text or "").lower()) if token}


def name_matches(name: str, query: str) -> bool:
    """용어명이 질문에 그대로 들어 있는지. 영문 약어는 단어 단위로만 비교해 'iv'가 'derivative'에 걸리지 않게 한다."""
    compact = normalize(name)
    if len(compact) < 2:
        return False
    if _HANGUL.search(compact):
        return compact in normalize(query)
    return compact in ascii_tokens(query)
