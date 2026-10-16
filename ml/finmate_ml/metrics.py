"""분류·보정 지표. 외부 라이브러리 없이 계산해 결과를 그대로 재현할 수 있게 한다."""

from __future__ import annotations

from . import GRADE, LABELS


def confusion(expected: list[str], predicted: list[str | None]) -> dict[str, dict[str, int]]:
    columns = list(LABELS) + ["ABSTAIN"]
    matrix = {label: {column: 0 for column in columns} for label in LABELS}
    for truth, guess in zip(expected, predicted):
        matrix[truth][guess or "ABSTAIN"] += 1
    return matrix


def classification(expected: list[str], predicted: list[str | None]) -> dict:
    """보류(None)는 오답으로 센다. 보류를 많이 해서 정확도를 부풀리지 못하게 하기 위해서다."""
    n = len(expected)
    correct = sum(truth == guess for truth, guess in zip(expected, predicted))
    adjacent = sum(guess is not None and abs(GRADE[truth] - GRADE[guess]) <= 1 for truth, guess in zip(expected, predicted))
    per_label = {}
    f1_values = []
    for label in LABELS:
        tp = sum(truth == label and guess == label for truth, guess in zip(expected, predicted))
        fp = sum(truth != label and guess == label for truth, guess in zip(expected, predicted))
        fn = sum(truth == label and guess != label for truth, guess in zip(expected, predicted))
        precision = tp / (tp + fp) if tp + fp else 0.0
        recall = tp / (tp + fn) if tp + fn else 0.0
        f1 = 2 * precision * recall / (precision + recall) if precision + recall else 0.0
        per_label[label] = {"precision": round(precision, 4), "recall": round(recall, 4), "f1": round(f1, 4),
                            "support": sum(truth == label for truth in expected)}
        f1_values.append(f1)
    return {
        "count": n,
        "accuracy": round(correct / n, 4),
        "macroF1": round(sum(f1_values) / len(f1_values), 4),
        "withinOneGrade": round(adjacent / n, 4),
        "perLabel": per_label,
    }


def selective(expected: list[str], predicted: list[str | None]) -> dict:
    """모델이 판단을 낸 문장만 놓고 본 정확도와 판단 비율(coverage)."""
    emitted = [(truth, guess) for truth, guess in zip(expected, predicted) if guess is not None]
    return {
        "coverage": round(len(emitted) / len(expected), 4),
        "accuracyWhenEmitted": round(sum(t == g for t, g in emitted) / len(emitted), 4) if emitted else 0.0,
    }


def expected_calibration_error(confidences: list[float], hits: list[bool], bins: int = 5) -> float:
    total = len(confidences)
    error = 0.0
    for b in range(bins):
        low, high = b / bins, (b + 1) / bins
        members = [(c, h) for c, h in zip(confidences, hits) if low < c <= high or (b == 0 and c == 0)]
        if members:
            mean_confidence = sum(c for c, _ in members) / len(members)
            accuracy = sum(h for _, h in members) / len(members)
            error += len(members) / total * abs(mean_confidence - accuracy)
    return round(error, 4)


def multilabel(expected: list[list[str]], predicted: list[list[str]], tags: list[str]) -> dict:
    tp = fp = fn = 0
    per_tag = {}
    for tag in tags:
        t_tp = sum(tag in e and tag in p for e, p in zip(expected, predicted))
        t_fp = sum(tag not in e and tag in p for e, p in zip(expected, predicted))
        t_fn = sum(tag in e and tag not in p for e, p in zip(expected, predicted))
        tp, fp, fn = tp + t_tp, fp + t_fp, fn + t_fn
        precision = t_tp / (t_tp + t_fp) if t_tp + t_fp else 0.0
        recall = t_tp / (t_tp + t_fn) if t_tp + t_fn else 0.0
        per_tag[tag] = {"precision": round(precision, 4), "recall": round(recall, 4),
                        "support": sum(tag in e for e in expected)}
    precision = tp / (tp + fp) if tp + fp else 0.0
    recall = tp / (tp + fn) if tp + fn else 0.0
    f1 = 2 * precision * recall / (precision + recall) if precision + recall else 0.0
    return {"microPrecision": round(precision, 4), "microRecall": round(recall, 4), "microF1": round(f1, 4),
            "perTag": per_tag}
