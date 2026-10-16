"""질문형 용어 검색 모델.

용어마다 이름(용어·정식 명칭·영문·별칭), 정의, 본문(설명·주의) 세 필드를 따로 벡터화하고
질문과의 코사인 유사도를 필드 가중합으로 더한다. 질문에 용어명이 그대로 들어 있으면 가산점을 준다.

학습 대상은 IDF 표와 필드 가중치·가산점이다. 가중치는 개발용 질문(dev)만으로 격자 탐색하고,
평가용 질문(test)은 마지막에 한 번만 본다. 문서 벡터는 Java가 DB의 공개 용어로 매번 다시 만들기 때문에
관리자가 게시한 수정본도 재학습 없이 바로 검색된다.
"""

from __future__ import annotations

from collections import Counter
import itertools
import math

from .features import grams, name_matches, normalize

FIELDS = ("name", "definition", "body")


def term_fields(term: dict) -> dict[str, str]:
    names = [term.get("term", ""), term.get("fullName") or "", term.get("english") or "", *term.get("aliases", [])]
    return {
        "name": " ".join(filter(None, names)),
        "definition": term.get("definition", ""),
        "body": " ".join(filter(None, [term.get("explanation", ""), term.get("caution") or ""])),
    }


def fit_idf(terms: list[dict]) -> tuple[dict[str, float], float]:
    document_frequency: Counter[str] = Counter()
    for term in terms:
        seen: set[str] = set()
        for text in term_fields(term).values():
            seen.update(grams(text))
        document_frequency.update(seen)
    count = len(terms)
    idf = {gram: round(math.log((1 + count) / (1 + df)) + 1, 8) for gram, df in sorted(document_frequency.items())}
    idf_max = round(math.log((1 + count) / 2) + 1, 8)
    return idf, idf_max


def tfidf(text: str, idf: dict[str, float], idf_max: float) -> tuple[dict[str, float], float]:
    weights = {gram: (1 + math.log(count)) * idf.get(gram, idf_max) for gram, count in grams(text).items()}
    return weights, math.sqrt(sum(value * value for value in weights.values()))


def cosine(query: tuple[dict[str, float], float], document: tuple[dict[str, float], float]) -> float:
    q, q_norm = query
    d, d_norm = document
    if not q_norm or not d_norm:
        return 0.0
    if len(q) > len(d):
        q, d = d, q
    return sum(value * d.get(gram, 0.0) for gram, value in q.items()) / (q_norm * d_norm)


class Index:
    def __init__(self, terms: list[dict], idf: dict[str, float], idf_max: float):
        self.idf, self.idf_max = idf, idf_max
        self.terms = terms
        self.vectors = [{field: tfidf(text, idf, idf_max) for field, text in term_fields(term).items()} for term in terms]

    def features(self, question: str) -> list[dict]:
        """질문 하나에 대한 용어별 필드 유사도와 이름 일치 여부. 격자 탐색에서 재사용한다."""
        query = tfidf(question, self.idf, self.idf_max)
        rows = []
        for term, vectors in zip(self.terms, self.vectors):
            others = [term.get("fullName") or "", term.get("english") or "", *term.get("aliases", [])]
            rows.append({
                "slug": term["slug"],
                "term": term["term"],
                "cos": {field: cosine(query, vectors[field]) for field in FIELDS},
                "termMatch": name_matches(term["term"], question),
                "aliasMatch": any(name_matches(name, question) for name in others if name),
            })
        return rows


def score(row: dict, params: dict) -> float:
    value = sum(params["fieldWeights"][field] * row["cos"][field] for field in FIELDS)
    if row["termMatch"]:
        value += params["termBoost"]
    elif row["aliasMatch"]:
        value += params["termBoost"] * params["aliasFactor"]
    return value


def rank(rows: list[dict], params: dict, limit: int = 5) -> list[dict]:
    scored = [{"slug": row["slug"], "term": row["term"], "score": score(row, params)} for row in rows]
    scored = [item for item in scored if item["score"] > 0]
    scored.sort(key=lambda item: (-item["score"], item["slug"]))
    return scored[:limit]


def evaluate(index: Index, cases: list[dict], params: dict, cached: dict[str, list[dict]] | None = None) -> dict:
    top1 = top5 = 0
    reciprocal = 0.0
    errors = []
    for case in cases:
        rows = cached[case["question"]] if cached else index.features(case["question"])
        ranked = rank(rows, params, limit=10)
        slugs = [item["slug"] for item in ranked]
        if slugs[:1] == [case["slug"]]:
            top1 += 1
        else:
            errors.append({"question": case["question"], "expected": case["slug"], "predicted": slugs[:5]})
        if case["slug"] in slugs[:5]:
            top5 += 1
        if case["slug"] in slugs:
            reciprocal += 1.0 / (slugs.index(case["slug"]) + 1)
    n = len(cases)
    return {"count": n, "top1": round(top1 / n, 4), "top5": round(top5 / n, 4), "mrr": round(reciprocal / n, 4),
            "errors": errors}


def grid_search(index: Index, dev: list[dict]) -> tuple[dict, dict]:
    cached = {case["question"]: index.features(case["question"]) for case in dev}
    best, best_key = None, None
    for w_name, w_def, w_body, boost, alias in itertools.product(
            (0.5, 1.0, 1.5, 2.0), (0.5, 1.0, 1.5), (0.0, 0.25, 0.5, 1.0), (0.0, 0.2, 0.4), (0.5, 0.75, 1.0)):
        params = {"fieldWeights": {"name": w_name, "definition": w_def, "body": w_body},
                  "termBoost": boost, "aliasFactor": alias}
        result = evaluate(index, dev, params, cached)
        # MRR을 우선하고, 같으면 가중치 합이 작은(단순한) 설정을 고른다.
        key = (result["mrr"], result["top1"], -(w_name + w_def + w_body + boost))
        if best_key is None or key > best_key:
            best, best_key = (params, result), key
    return best


# ---------------------------------------------------------------- 비교 기준선

def legacy_score_fn(terms: list[dict]):
    """v1 방식: 필드를 반복해 이어 붙인 문서 하나 + 이름 포함 시 0.35 가산."""
    def text(term: dict) -> str:
        title = " ".join(filter(None, [term["term"], term.get("fullName") or "", term.get("english") or ""]))
        aliases = " ".join(term.get("aliases", []))
        return " ".join([title] * 5 + [aliases] * 3 + [term.get("definition", ""), term.get("explanation", "")[:180]])

    df: Counter[str] = Counter()
    docs = [text(term) for term in terms]
    for doc in docs:
        df.update(set(grams(doc)))
    idf = {gram: math.log((1 + len(docs)) / (1 + count)) + 1 for gram, count in df.items()}
    vectors = []
    for doc in docs:
        weights = {g: (1 + math.log(c)) * idf[g] for g, c in grams(doc).items()}
        vectors.append((weights, math.sqrt(sum(v * v for v in weights.values()))))

    def search(question: str, limit: int = 10) -> list[str]:
        q = {g: (1 + math.log(c)) * idf[g] for g, c in grams(question).items() if g in idf}
        q_vec = (q, math.sqrt(sum(v * v for v in q.values())))
        compact = normalize(question)
        ranked = []
        for term, vector in zip(terms, vectors):
            value = cosine(q_vec, vector)
            names = [term["term"], *term.get("aliases", [])]
            if any(len(normalize(name)) >= 2 and normalize(name) in compact for name in names):
                value += 0.35
            if value > 0:
                ranked.append((value, term["slug"]))
        ranked.sort(key=lambda item: (-item[0], item[1]))
        return [slug for _, slug in ranked[:limit]]

    return search
