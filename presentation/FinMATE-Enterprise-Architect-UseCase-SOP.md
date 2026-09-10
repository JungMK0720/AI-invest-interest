# FinMATE Enterprise Architect Use Case Diagram 작성 SOP

> 대상 도구: Sparx Systems Enterprise Architect 17.x 기준  
> 목적: FinMATE의 액터·Use Case·기능 요구사항·비기능 요구사항을 Enterprise Architect에서 직접 작성하고, 발표용 이미지로 내보내는 절차  
> 기준 문서: [07-usecase.puml](../docs/07-usecase.puml), [13-functional-nonfunctional-usecases.md](../docs/13-functional-nonfunctional-usecases.md)

Enterprise Architect에서 Use Case Diagram은 시스템 경계 안의 Use Case와 경계 밖의 Actor가 어떻게 연결되는지 표현한다. 기능 요구사항을 시각화하는 데 사용하고, 비기능 요구사항은 별도 Requirements Diagram과 추적 관계로 연결한다. 공식 가이드도 Use Case Diagram을 기능 요구사항과 외부 Actor의 상호작용을 표현하는 도구로 설명한다. [Sparx Use Case Diagram 가이드](https://www.sparxsystems.com/enterprise_architect_user_guide/17.2/guide_books/tools_ba_use_case_diagram.html)

## 1. 완성해야 하는 파일

최종적으로 다음 세 개의 EA 다이어그램과 세 개의 EA 캡처 이미지를 만든다. API 캡처 이미지는 별도 시각화 SOP의 파일명을 따른다.

| 산출물 | EA 다이어그램 종류 | 발표에서의 용도 |
|---|---|---|
| `UC-01-FinMATE-Overview` | UML Behavioral → Use Case | 전체 액터와 P0/P1 기능 소개 |
| `UC-02-FinMATE-Learning-Practice` | UML Behavioral → Use Case | 개념 학습·퀴즈·시뮬레이션·포트폴리오 고도화 |
| `REQ-01-FR-NFR-Traceability` | Requirements Diagram | 기능/비기능 요구사항과 Use Case 추적 |
| `presentation/captures/04-usecase-overview.png` | 내보낸 이미지 | 발표 슬라이드 삽입 |
| `presentation/captures/05-usecase-learning.png` | 내보낸 이미지 | 발표 슬라이드 삽입 |
| `presentation/captures/06-requirements-traceability.png` | 내보낸 이미지 | FR/NFR 설명 |

한 장에 모든 20개 Use Case를 억지로 넣지 않는다. Overview는 범위를 보여 주고, Learning-Practice는 고도화 기능을 읽게 하며, Requirements Diagram은 추적성을 보여 준다.

## 2. EA 프로젝트와 모델 구조 만들기

### 2-1. 프로젝트 생성

1. Enterprise Architect를 실행한다.
2. `File > New Project`를 선택한다.
3. 프로젝트 파일명을 `FinMATE-EA.eapx`로 저장한다.
4. 모델 루트 또는 `Model` 아래에 다음 패키지를 만든다.

```text
FinMATE
├── 01_Requirements
│   ├── Functional_Requirements
│   └── NonFunctional_Requirements
├── 02_UseCases
│   ├── Actors
│   ├── P0_Diagnosis_Product
│   ├── P0_Learning_Practice
│   └── P1_Data_Operations
└── 03_Traceability
```

5. 패키지는 Browser 창에서 부모를 선택한 뒤 `Design > Package > Add Package`로 만든다. 패키지 생성 시 `Package Only`를 선택하고, 다이어그램은 아래 절차에서 별도로 만든다. [Sparx Add a Package](https://www.sparxsystems.com/enterprise_architect_user_guide/17.2/modeling_fundamentals/addpackage.html)

### 2-2. 이름 규칙

| 모델 요소 | 작성 규칙 | 예시 |
|---|---|---|
| Actor | 역할명만 작성 | `일반 사용자` |
| Use Case | `UC-ID. 동작` | `UC-04. 답변 제출·진단 생성` |
| Functional Requirement | `FR-ID. 기능명` | `FR-05. 성향·탐색 범위 판정` |
| Non-functional Requirement | `NFR-ID. 품질·제약` | `NFR-02. 결과 재현성` |
| Diagram | 목적과 범위 포함 | `UC-01-FinMATE-Overview` |

동작은 “조회한다”, “제출한다”, “분석한다”처럼 동사로 끝낸다. 상품명이나 화면 이름을 Use Case 이름으로 쓰지 않는다.

## 3. UC-01 전체 Use Case Diagram 만들기

### 3-1. 다이어그램 생성

1. Browser에서 `02_UseCases` 패키지를 선택한다.
2. `Design > Diagram > Add Diagram`을 선택한다.
3. `UML Behavioral > Use Case`를 선택한다.
4. 이름을 `UC-01-FinMATE-Overview`로 입력하고 생성한다.
5. Diagram Toolbox를 연다. Use Case Diagram은 Actor, Use Case, System Boundary와 Association, Include, Extend를 사용한다. [Sparx Use Case Diagram Toolbox](https://www.sparxsystems.com/enterprise_architect_user_guide/17.0/modeling_languages/usecasediagram.html)

### 3-2. 시스템 경계 배치

1. Toolbox에서 `System Boundary`를 선택한다.
2. 다이어그램 중앙에 가로로 넓게 배치한다.
3. 이름을 `FinMATE`로 바꾼다.
4. 경계 밖에 Actor를 배치하고, Use Case는 반드시 경계 안에 둔다.
5. 경계 안에는 다음 세 개의 시각적 패키지를 배치한다.

```text
발표용 P0 · 진단과 상품 탐색
발표용 P0 · 학습과 모의투자
배포형 P1 · 이력·데이터·운영
```

패키지는 범위를 보여 주는 그룹이다. 실제 UML 의미가 필요한 관계는 Use Case 사이의 Include/Extend로 표현한다.

### 3-3. Actor 추가

Toolbox의 `Actor`를 선택해 경계 바깥에 다음 요소를 만든다.

| Actor | 위치 | 연결할 대표 Use Case |
|---|---|---|
| 일반 사용자 | 왼쪽 | UC-01~04, UC-07~16, UC-18 |
| 운영자/콘텐츠 검수자 | 오른쪽 | UC-18~20 |
| 규칙 엔진 | 아래쪽 | UC-05, UC-11~13 |
| AI 설명 생성기 | 아래쪽 | UC-06 |
| 외부 데이터 공급자 | 오른쪽 아래 | UC-17 |
| 인증·관측 인프라 | 아래쪽 | UC-01, UC-17, UC-20 |

Actor가 시스템 안에 들어가면 외부 역할과 내부 기능의 경계가 흐려진다. 규칙 엔진과 AI 설명 생성기는 내부 시스템이지만, 다른 시스템과의 협력 관계를 설명하기 위해 Actor 표기로 둔다.

### 3-4. Overview에 배치할 Use Case

Overview에는 아래 14개를 넣는다. P1의 세부 Use Case는 별도 다이어그램에서 확대한다.

#### P0 · 진단과 상품 탐색

- `UC-01. 회원가입·로그인`
- `UC-02. 진단 가능 여부 확인`
- `UC-03. 질문 조회·진단 시작`
- `UC-04. 답변 제출·진단 생성`
- `UC-05. 성향·탐색 범위 판정`
- `UC-06. 근거 생성·폴백`
- `UC-07. 결과·고지 조회`
- `UC-08. 카테고리·상품 탐색`

#### P0 · 학습과 모의투자

- `UC-10. 개념 트리·그림·출처 학습`
- `UC-11. 개념별 문제·오답 교정`
- `UC-12. 메커니즘 시뮬레이션`
- `UC-13. 포트폴리오 실험·분석`
- `UC-14. 전후 결과 비교`
- `UC-15. 방법론·가정·기준시각 확인`

### 3-5. Actor와 Use Case 연결

1. Toolbox에서 `Association` 또는 `Use` 관계를 선택한다.
2. Actor를 클릭하고 연결할 Use Case를 클릭한다.
3. 일반 Actor-Use Case 선에는 화살표를 붙이지 않는다.
4. 선이 겹치면 Actor를 좌우로 이동하고, Use Case를 기능 흐름 순서대로 위에서 아래로 정렬한다.

### 3-6. Include와 Extend 연결

Quick Linker 또는 Toolbox에서 관계를 만든 뒤 관계 타입을 지정한다.

| 시작 Use Case | 관계 | 대상 Use Case | 의미 |
|---|---|---|---|
| UC-04 답변 제출·진단 생성 | `<<include>>` | UC-05 성향·탐색 범위 판정 | 답변 제출 때 항상 수행 |
| UC-04 답변 제출·진단 생성 | `<<include>>` | UC-06 근거 생성·폴백 | 결과 생성 과정에 포함 |
| UC-07 결과·고지 조회 | `<<include>>` | UC-15 방법론·가정·기준시각 확인 | 결과의 가정과 한계를 함께 제공 |
| UC-10 개념 학습 | `<<include>>` | UC-15 방법론·가정·기준시각 확인 | 출처와 기준을 함께 제공 |
| UC-12 시뮬레이션 | `<<include>>` | UC-15 방법론·가정·기준시각 확인 | 계산 결과의 가정 표시 |
| UC-13 포트폴리오 실험·분석 | `<<include>>` | UC-14 전후 결과 비교 | 비교 기능을 포함 |

`extend`는 항상 수행되는 과정이 아니라 특정 조건에서만 추가되는 흐름에 사용한다. 예를 들어 `UC-20. 장애·지연·만료 운영`이 `UC-17. 외부 자료 수집·스냅샷`의 장애 상황을 확장하는 경우에만 `<<extend>>`를 사용한다.

## 4. UC-02 학습·모의투자 상세 다이어그램

1. `02_UseCases/P0_Learning_Practice` 패키지를 선택한다.
2. `Design > Diagram > Add Diagram > UML Behavioral > Use Case`로 새 다이어그램을 만든다.
3. 이름을 `UC-02-FinMATE-Learning-Practice`로 지정한다.
4. 일반 사용자 Actor를 왼쪽에 둔다.
5. 아래 Use Case를 가로 흐름으로 배치한다.

```text
UC-10 개념 트리·그림·출처 학습
        ↓
UC-11 개념별 문제·오답 교정
        ↓
UC-12 메커니즘 시뮬레이션
        ↓
UC-13 포트폴리오 실험·분석
        ↓
UC-14 전후 결과 비교
```

6. 규칙 엔진을 UC-11에 연결한다.
7. 계산 엔진을 별도 Actor `교육용 계산 엔진`으로 만들고 UC-12, UC-13에 연결해도 된다. 발표 시간이 짧으면 규칙 엔진 Actor에 통합한다.
8. `UC-15 방법론·가정·기준시각 확인`을 아래쪽에 두고 UC-10, UC-12, UC-13에서 `<<include>>`로 연결한다.
9. 각 Use Case의 Notes 창에 다음 세 줄을 적는다.

```text
입력: 사용자가 입력하는 값
처리: 규칙 또는 계산 방식
출력: 사용자가 확인하는 의미와 한계
```

예시:

```text
입력: 원금, 자산별 금액, 금리·가격·환율 충격
처리: portfolio-shock-v1 계산식
출력: 손익, 노출, 자산별 기여도, limitations
```

## 5. 기능 요구사항(FR) 모델링

### 5-1. Functional Requirements 패키지 만들기

1. `01_Requirements/Functional_Requirements` 패키지를 선택한다.
2. `Design > Diagram > Add Diagram`에서 `Requirements` 다이어그램을 만든다. 버전에 따라 Browser의 `Add Diagram > Manage > Show All Perspectives > Extended > Requirements`에서 선택한다. [Sparx Requirements Diagram](https://www.sparxsystems.com/enterprise_architect_user_guide/17.2/guide_books/tools_ba_requirements_diagram.html)
3. Toolbox에서 `Requirement` 요소를 선택한다.
4. 대표 FR을 다음과 같이 만든다.

```text
FR-01 회원가입·로그인
FR-04 답변 제출·진단 생성
FR-05 성향·탐색 범위 판정
FR-06 근거 생성·폴백
FR-08 카테고리·상품 탐색
FR-10 개념 트리·그림·출처 학습
FR-11 개념별 문제·오답 교정
FR-13 포트폴리오 실험·분석
```

5. Requirement를 더블클릭하고 다음 값을 입력한다.

| 필드 | 입력 예시 |
|---|---|
| Name | `FR-05 성향·탐색 범위 판정` |
| Stereotype | `requirement` |
| Notes | `규칙 엔진이 점수·5단계 성향·탐색 카테고리를 결정한다.` |
| Status | `Approved` 또는 `Proposed` |
| Priority | `P0` 또는 `P1` |

6. Requirement를 다이어그램에 배치한다.
7. Browser에서 기존 Use Case를 Requirement Diagram으로 끌어온다.
8. Use Case에서 Requirement로 `Realize` 관계를 연결한다. EA는 Requirement와 Use Case를 함께 사용해 요구사항이 어떤 기능으로 충족되는지 추적할 수 있다. [Sparx UML Requirements](https://www.sparxsystems.com/enterprise_architect_user_guide/17.2/modeling_domains/uml_requirements.html)

## 6. 비기능 요구사항(NFR) 모델링

### 6-1. NFR은 Use Case와 같은 방식으로 그리지 않는다

NFR은 사용자 목표가 아니라 시스템 품질·제약이다. 따라서 Use Case Diagram의 타원으로 만들지 않고, Requirements Diagram에서 별도 패키지와 Requirement 요소로 관리한다.

### 6-2. NFR 요소 만들기

1. `01_Requirements/NonFunctional_Requirements` 패키지를 선택한다.
2. Requirements Diagram을 새로 만들거나 `REQ-01-FR-NFR-Traceability` 다이어그램에 추가한다.
3. 다음 네 개의 그룹을 먼저 만든다.

```text
NFR-Security-Safety
NFR-Data-Traceability
NFR-Performance-Availability
NFR-Maintainability-Test
```

4. Requirement 요소를 만들고 다음 대표 항목을 입력한다.

| 그룹 | ID | Requirement 이름 | 검증 방법 |
|---|---|---|---|
| 보안·안전 | NFR-01 | 인증·권한 분리 | 권한 테스트 |
| 보안·안전 | NFR-03 | 투자 권유 오해 방지 | 문구·UI 검수 |
| 데이터 | NFR-02 | 결과 재현성 | 동일 입력·seed 반복 |
| 데이터 | NFR-05 | 출처·기준일·hash 추적 | 스냅샷 점검 |
| 성능 | NFR-06 | 일반 API p95 1초 목표 | 부하 테스트 |
| 가용성 | NFR-07 | 외부 장애 시 폴백 | 장애 주입 테스트 |
| 유지보수 | NFR-12 | 콘텐츠·계산식 버전 관리 | 버전 검증 |
| 검증 | NFR-13 | 자동 테스트 가능성 | CI 테스트 |

5. NFR에서 관련 Use Case 또는 시스템 Boundary로 `Trace` 관계를 연결한다.
6. NFR 관계선에는 `<<include>>`를 사용하지 않는다.
7. Notes에 수치 또는 검증 방법을 반드시 적는다.

예시:

```text
NFR-06 일반 API p95 1초 목표
목표: 일반 조회 API p95 < 1초
예외: 외부 데이터·AI 호출 지연은 별도 표시
검증: 부하 테스트와 관측 지표
```

## 7. 화면 정리와 이미지 내보내기

### 7-1. 발표용 레이아웃

1. 시스템 Boundary는 가운데에 크게 둔다.
2. Actor는 왼쪽과 오른쪽에 분리한다.
3. P0는 위쪽, P1은 아래쪽에 배치한다.
4. Include/Extend 선이 Actor 연결선과 겹치지 않게 한다.
5. 긴 설명은 Use Case 이름에 넣지 말고 Notes로 이동한다.
6. 다이어그램 배경과 글자 대비를 확인한다.
7. 발표 캡처에서는 Browser 창·속성 창·선택 핸들을 숨긴다.

### 7-2. 이미지 저장

1. 완성한 다이어그램을 연다.
2. Diagram Image Panel 또는 Publish 도구의 `Save Diagram Image to File`을 선택한다.
3. PNG 또는 SVG로 저장한다. EA 공식 가이드도 현재 다이어그램 이미지를 파일 또는 클립보드로 저장할 수 있다고 안내한다. [Sparx Diagram Image Panel](https://www.sparxsystems.com/enterprise_architect_user_guide/17.2/the_application_desktop/pub_diag_panel.html)
4. 파일을 다음 위치에 저장한다.

```text
presentation/captures/04-usecase-overview.png
presentation/captures/05-usecase-learning.png
presentation/captures/06-requirements-traceability.png
```

5. PowerPoint에서 `삽입 > 그림`으로 불러온다.
6. 슬라이드 하단에 다음 캡션을 붙인다.

```text
FinMATE Use Case Diagram — 일반 사용자·규칙 엔진·AI 설명 생성기·외부 데이터 공급자 간 상호작용
```

## 8. 발표 슬라이드 연결

| 슬라이드 | 삽입 이미지 | 설명 순서 |
|---|---|---|
| Use Case 개요 | `04-usecase-overview.png` | Actor → 시스템 경계 → P0/P1 → Include |
| 고도화 기능 | `05-usecase-learning.png` | 개념 학습 → 퀴즈 → 시뮬레이션 → 포트폴리오 |
| 요구사항 품질 | `06-requirements-traceability.png` | FR/NFR → Use Case → 검증 방법 |
| API 설계 | `01-api-overview.png` | Use Case를 구현하는 API 그룹 |

발표 멘트는 다음 순서로 말한다.

```text
1. 일반 사용자는 진단과 학습을 이용합니다.
2. 답변 제출 과정에서 규칙 엔진이 성향을 결정하고 AI는 근거를 설명합니다.
3. 진단 등급과 별개로 개념 학습과 가정 실험은 열려 있습니다.
4. 기능 요구사항은 Use Case로, 품질과 제약은 NFR로 분리했습니다.
5. API는 각 Use Case를 구현하는 계약이며, 실데이터·검수는 P1로 확장합니다.
```

## 9. 마지막 검수 체크리스트

### 모델 구조

- [ ] 모든 Actor가 경계 밖에 있다.
- [ ] 모든 Use Case가 `UC-ID`를 가진다.
- [ ] 모든 FR/NFR이 `FR-ID`/`NFR-ID`를 가진다.
- [ ] P0와 P1 패키지가 구분된다.
- [ ] 실제 주문·계좌 연동은 제외 범위로 남아 있다.

### 관계

- [ ] Actor-Use Case는 Association/Use 관계다.
- [ ] 항상 수행되는 하위 동작만 `<<include>>`다.
- [ ] 조건부 확장만 `<<extend>>`다.
- [ ] FR은 Use Case와 Realize로 추적된다.
- [ ] NFR은 Trace 또는 적절한 의존 관계로 연결된다.

### 발표 이미지

- [ ] PNG/SVG를 16:9 슬라이드에서 읽을 수 있다.
- [ ] 선 교차와 겹치는 라벨이 없다.
- [ ] 캡처 이미지에 개인 정보와 토큰이 없다.
- [ ] API 캡처와 Use Case가 같은 ID·이름을 사용한다.
- [ ] 슬라이드마다 다이어그램을 해석하는 한 문장이 있다.
