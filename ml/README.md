# FinMATE 보조 텍스트 모델

두 모델은 교육용 보조 기능이며 금융상품의 적합성이나 설문 투자 성향을 판정하지 않습니다. 실제 거래도 실행하지 않습니다.

## 재학습과 평가

프로젝트 루트에서 `python3 ml/train.py`를 실행합니다. Python 표준 라이브러리만 사용하고 외부 API나 개인 데이터는 필요하지 않습니다. 산출물은 `backend/src/main/resources/ml/`의 JSON 두 개, `ml/evaluation.json`, 그리고 Java 평가용 `backend/src/test/resources/ml/evaluation-cases.json`입니다. 이어서 `python3 -m unittest discover -s ml -p 'test_*.py'`와 `cd backend && mvn test`로 확인합니다.

| 모델 | 학습 데이터 | 방법 | 별도 수작업 평가셋 |
| --- | --- | --- | --- |
| 자유 서술 성향 | `style_train.jsonl`의 예시 40개 | 한글·영문 문자 2~4그램 다항 나이브 베이즈 | `style_test.jsonl` 15개 중 13개 원시 분류 정답. 운영 임계값 적용 시 12개 출력, 3개 보류 |
| 질문→용어 검색 | 승인 용어 JSON 247개 | 문자 2~4그램 TF-IDF 코사인 유사도와 명칭 일치 보정 | `term_test.jsonl` 47개 중 1순위 43개, 상위 5개 47개 포함 |

학습용 성향 문장과 평가 문장은 실제 사용자 기록이 아닌 사람이 작성한 작은 예시입니다. 숫자는 이 평가셋에만 해당하며, 실제 고객이나 새로운 문장에 대한 정확도로 해석할 수 없습니다. 특히 짧거나 모호한 문장은 `INSUFFICIENT`를 반환하고, 설문 등급과 2단계 이상 차이나며 신뢰도가 0.55 이상일 때만 `mismatch=true`를 표시합니다. 설문 등급은 서버의 규칙 엔진이 계속 소유합니다.

## Java API

- `POST /api/ml/style-check` — `{"text":"원금 손실은 싫어요", "questionnaireGrade":5}`. `status`, `estimatedProfileType`, `estimatedProfileLabel`, `estimatedGrade`, `confidence`, `questionnaireGrade`, `mismatch`, `modelVersion`, `notice`를 반환합니다.
- `POST /api/ml/term-search` — `{"question":"ETF가 뭔가요?", "limit":5}`. 공개 상태인 용어만 `results:[{slug,term,score}]`로 반환합니다.

용어 모델은 배포 시점의 JSON에서 학습됩니다. 관리자 화면에서 기존 용어를 비공개로 바꾸면 검색에서 즉시 제외됩니다. 본문·별칭을 수정하거나 새 용어를 공개한 경우에는 `python3 ml/train.py`로 모델을 재생성하고 서버를 재배포해야 검색 인덱스에도 반영됩니다. `sourceSha256`과 평가셋은 이 과정을 확인하기 위한 것입니다.
