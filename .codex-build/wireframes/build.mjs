import fs from "node:fs/promises";
import path from "node:path";
import crypto from "node:crypto";
import { Presentation, PresentationFile } from "@oai/artifact-tool";

const workspaceDir = "/Users/minkyu/Lecture/August/week9";
const buildDir = path.join(workspaceDir, ".codex-build", "wireframes");
const skillDir = "/Users/minkyu/.codex/plugins/cache/openai-primary-runtime/presentations/26.905.11957/skills/presentations";
const finalPath = path.join(workspaceDir, "presentation", "FinMATE-플로우차트-와이어프레임.pptx");
const candidatePath = path.join(buildDir, "candidate.pptx");
const family = "Apple SD Gothic Neo";

const C = {
  navy: "#131B2D",
  blue: "#3157E8",
  blue2: "#5775F3",
  paleBlue: "#EFF3FF",
  paleBlue2: "#F6F8FF",
  text: "#182132",
  muted: "#627089",
  line: "#DCE3F0",
  green: "#1D8B6A",
  paleGreen: "#EAF8F2",
  amber: "#B7791F",
  paleAmber: "#FFF7E5",
  red: "#C84C5A",
  paleRed: "#FFF0F1",
  white: "#FFFFFF",
};

const pos = (left, top, width, height) => ({ left, top, width, height });
function addShape(slide, geometry, position, fill = C.white, lineFill = C.line, width = 1, name) {
  const shape = slide.shapes.add({
    geometry,
    name,
    position,
    fill,
    line: { style: lineFill === "none" ? "solid" : "solid", fill: lineFill, width },
    borderRadius: geometry === "roundRect" ? "rounded-2xl" : undefined,
  });
  return shape;
}
function addText(slide, text, position, style = {}, name) {
  const shape = slide.shapes.add({
    geometry: "textbox",
    name,
    position,
    fill: "none",
    line: { style: "solid", fill: "none", width: 0 },
  });
  shape.text = text;
  shape.text.style = {
    typeface: family,
    fontSize: style.fontSize ?? 18,
    color: style.color ?? C.text,
    bold: style.bold ?? false,
    alignment: style.alignment ?? "left",
    verticalAlignment: style.verticalAlignment ?? "top",
    autoFit: style.autoFit ?? "shrinkText",
    wrap: "square",
    insets: style.insets ?? { top: 2, right: 4, bottom: 2, left: 4 },
  };
  return shape;
}
function addTitle(slide, number, title, subtitle = "") {
  addShape(slide, "rect", pos(0, 0, 1280, 720), C.white, "none", 0, "background");
  addShape(slide, "rect", pos(52, 48, 8, 40), C.blue, "none", 0, "accent");
  addText(slide, number, pos(76, 45, 100, 26), { fontSize: 16, bold: true, color: C.blue }, "section-number");
  addText(slide, title, pos(76, 74, 1030, 54), { fontSize: 30, bold: true, color: C.navy }, "slide-title");
  if (subtitle) addText(slide, subtitle, pos(78, 128, 1050, 30), { fontSize: 15, color: C.muted }, "slide-subtitle");
  addText(slide, "FinMATE  ·  flowchart & wireframe appendix", pos(78, 686, 450, 18), { fontSize: 11, color: C.muted }, "footer");
}
function addLabel(slide, text, x, y, w, color = C.blue, fill = C.paleBlue) {
  const s = addShape(slide, "roundRect", pos(x, y, w, 28), fill, fill, 0, `label-${text}`);
  s.borderRadius = "rounded-full";
  addText(slide, text, pos(x + 4, y + 3, w - 8, 22), { fontSize: 12, bold: true, color, alignment: "center", verticalAlignment: "middle" });
  return s;
}
function addFlowNode(slide, label, detail, x, y, w, h, fill = C.white, accent = C.blue, name = label) {
  const box = addShape(slide, "roundRect", pos(x, y, w, h), fill, C.line, 1, name);
  box.shadow = "shadow-sm";
  addShape(slide, "rect", pos(x, y, 5, h), accent, accent, 0);
  addText(slide, label, pos(x + 16, y + 12, w - 24, 28), { fontSize: 17, bold: true, color: C.navy });
  if (detail) addText(slide, detail, pos(x + 16, y + 44, w - 24, h - 50), { fontSize: 13, color: C.muted });
  return box;
}
function addWireframe(slide, x, y, w, h, title, state = "default") {
  const shell = addShape(slide, "roundRect", pos(x, y, w, h), C.white, C.line, 1, `wire-${title}`);
  shell.shadow = "shadow-sm";
  addShape(slide, "rect", pos(x, y, w, 30), state === "error" ? C.paleRed : C.paleBlue2, "none", 0);
  addShape(slide, "ellipse", pos(x + 12, y + 10, 8, 8), C.red, "none", 0);
  addShape(slide, "ellipse", pos(x + 25, y + 10, 8, 8), C.amber, "none", 0);
  addShape(slide, "ellipse", pos(x + 38, y + 10, 8, 8), C.green, "none", 0);
  addText(slide, title, pos(x + 58, y + 7, w - 70, 18), { fontSize: 11, bold: true, color: C.muted });
  return shell;
}
function addButton(slide, text, x, y, w, fill = C.blue, color = C.white) {
  const b = addShape(slide, "roundRect", pos(x, y, w, 30), fill, fill, 0);
  b.borderRadius = "rounded-full";
  addText(slide, text, pos(x + 4, y + 5, w - 8, 20), { fontSize: 12, bold: true, color, alignment: "center", verticalAlignment: "middle" });
  return b;
}
function addRule(slide, x, y, w, color = C.line) {
  return addShape(slide, "rect", pos(x, y, w, 1), color, color, 0);
}
function addNote(slide, text) {
  slide.speakerNotes.textFrame.setText(text);
}

const presentation = Presentation.create({ slideSize: { width: 1280, height: 720 } });

// Slide 1: overview flowchart
{
  const s = presentation.slides.add();
  addTitle(s, "01. 사용자 플로우", "FinMATE의 전체 사용자 여정", "진단 결과를 학습·실험으로 확장하는 구조");
  addLabel(s, "진단·탐색", 78, 180, 112);
  const a = addFlowNode(s, "로그인", "계정과 진단 이력 확인", 78, 230, 180, 94, C.paleBlue2, C.blue);
  const b = addFlowNode(s, "진단 질문", "질문·선택지 조회", 322, 230, 180, 94, C.white, C.blue);
  const c = addFlowNode(s, "규칙 판정", "점수·5단계 성향", 566, 230, 180, 94, C.white, C.blue);
  const d = addFlowNode(s, "근거 설명", "AI 또는 템플릿", 810, 230, 180, 94, C.white, C.blue);
  const e = addFlowNode(s, "상품 탐색", "카테고리·상세·고지", 1054, 230, 148, 94, C.paleBlue2, C.blue);
  for (const [from, to] of [[a,b],[b,c],[c,d],[d,e]]) {
    s.shapes.connect(from, to, { kind: "straight", fromSide: "right", toSide: "left", line: { style: "solid", fill: C.blue2, width: 2 }, head: { type: "triangle", width: "sm", length: "sm" } });
  }
  addLabel(s, "학습·연습", 78, 410, 112, C.green, C.paleGreen);
  const f = addFlowNode(s, "개념 트리", "자산군 → 상품 구조 → 메커니즘", 78, 460, 220, 104, C.paleGreen, C.green);
  const g = addFlowNode(s, "개념별 퀴즈", "오답 이유와 다음 학습", 360, 460, 220, 104, C.white, C.green);
  const h = addFlowNode(s, "메커니즘 실험", "금리·수수료·옵션·레버리지", 642, 460, 220, 104, C.white, C.green);
  const i = addFlowNode(s, "포트폴리오 연습", "가상 상품·충격·전후 비교", 924, 460, 220, 104, C.paleGreen, C.green);
  for (const [from, to] of [[f,g],[g,h],[h,i]]) {
    s.shapes.connect(from, to, { kind: "straight", fromSide: "right", toSide: "left", line: { style: "solid", fill: C.green, width: 2 }, head: { type: "triangle", width: "sm", length: "sm" } });
  }
  addNote(s, "발표 포인트: 진단 결과와 학습 접근을 분리해, 성향 제한을 받은 사용자도 금융상품 구조를 공부하고 가정값으로 실험할 수 있다.");
}

// Slide 2: diagnosis flow with exceptions
{
  const s = presentation.slides.add();
  addTitle(s, "02. 진단 흐름", "답변 제출부터 결과 확인까지", "정상 경로와 예외 경로를 함께 설계");
  addLabel(s, "정상 경로", 78, 178, 110);
  const nodes = [
    addFlowNode(s, "진단 가능 여부", "GET /assessments/availability", 78, 230, 190, 100, C.paleBlue2, C.blue),
    addFlowNode(s, "질문 응답", "GET /questions", 314, 230, 190, 100, C.white, C.blue),
    addFlowNode(s, "답변 제출", "POST /assessments", 550, 230, 190, 100, C.white, C.blue),
    addFlowNode(s, "규칙 판정", "score · profileGrade", 786, 230, 190, 100, C.white, C.blue),
    addFlowNode(s, "결과·탐색", "GET result + recommendations", 1022, 230, 180, 100, C.paleBlue2, C.blue),
  ];
  for (let i = 0; i < nodes.length - 1; i++) s.shapes.connect(nodes[i], nodes[i + 1], { kind: "straight", fromSide: "right", toSide: "left", line: { style: "solid", fill: C.blue2, width: 2 }, head: { type: "triangle", width: "sm", length: "sm" } });
  addLabel(s, "예외 경로", 78, 390, 110, C.red, C.paleRed);
  const ex1 = addFlowNode(s, "미응답 문항", "제출 버튼 비활성화", 78, 442, 250, 90, C.paleRed, C.red);
  const ex2 = addFlowNode(s, "AI 설명 실패", "TEMPLATE로 결과 완료", 378, 442, 250, 90, C.paleAmber, C.amber);
  const ex3 = addFlowNode(s, "데이터 준비 실패", "503 + 답변 유지 재시도", 678, 442, 250, 90, C.paleRed, C.red);
  const ex4 = addFlowNode(s, "권한 오류", "401/403 안내", 978, 442, 224, 90, C.paleRed, C.red);
  addText(s, "화면은 실패를 숨기지 않고 사용자가 다음 행동을 선택하게 한다.", pos(78, 590, 750, 34), { fontSize: 17, bold: true, color: C.navy });
  addText(s, "FR-02~07  ·  /assessments  ·  reasoningSource  ·  고지 문구", pos(78, 625, 900, 24), { fontSize: 14, color: C.muted });
  addNote(s, "발표 포인트: 규칙 엔진이 성향과 탐색 범위를 결정하며, AI는 설명만 담당한다. AI 실패는 진단 실패가 아니라 TEMPLATE 폴백이다.");
}

// Slide 3: learning/practice flow
{
  const s = presentation.slides.add();
  addTitle(s, "03. 학습·연습 흐름", "상품명을 외우는 화면에서 메커니즘을 실험하는 화면으로", "각 단계가 다음 API와 화면으로 연결된다");
  const left = addShape(s, "roundRect", pos(78, 205, 510, 380), C.paleBlue2, C.line, 1, "learning-lane");
  const right = addShape(s, "roundRect", pos(652, 205, 550, 380), C.paleGreen, C.line, 1, "practice-lane");
  addText(s, "개념 학습", pos(106, 225, 220, 30), { fontSize: 22, bold: true, color: C.blue });
  addText(s, "가상 포트폴리오 연습", pos(682, 225, 300, 30), { fontSize: 22, bold: true, color: C.green });
  const l1 = addFlowNode(s, "1. 개념 트리", "주식 · 채권 · 펀드 · ETF · 파생", 112, 278, 430, 58, C.white, C.blue);
  const l2 = addFlowNode(s, "2. 상품 학습 카드", "돈의 흐름 그림 · 오해 · 공식 출처", 112, 362, 430, 58, C.white, C.blue);
  const l3 = addFlowNode(s, "3. 개념별 퀴즈", "선택지별 오답 해설", 112, 446, 430, 58, C.white, C.blue);
  s.shapes.connect(l1, l2, { kind: "straight", fromSide: "bottom", toSide: "top", line: { style: "solid", fill: C.blue2, width: 2 }, head: { type: "triangle", width: "sm", length: "sm" } });
  s.shapes.connect(l2, l3, { kind: "straight", fromSide: "bottom", toSide: "top", line: { style: "solid", fill: C.blue2, width: 2 }, head: { type: "triangle", width: "sm", length: "sm" } });
  const r1 = addFlowNode(s, "1. 가상 상품 선택", "주식 · 채권 · ETF · 금 · 리츠 · 선물 · 옵션", 686, 278, 480, 58, C.white, C.green);
  const r2 = addFlowNode(s, "2. 충격 입력", "가격 · 금리 · 환율 · 비용 · 기간", 686, 362, 480, 58, C.white, C.green);
  const r3 = addFlowNode(s, "3. 결과 비교", "손익 · 낙폭 · 노출 · 자산별 기여도", 686, 446, 480, 58, C.white, C.green);
  s.shapes.connect(r1, r2, { kind: "straight", fromSide: "bottom", toSide: "top", line: { style: "solid", fill: C.green, width: 2 }, head: { type: "triangle", width: "sm", length: "sm" } });
  s.shapes.connect(r2, r3, { kind: "straight", fromSide: "bottom", toSide: "top", line: { style: "solid", fill: C.green, width: 2 }, head: { type: "triangle", width: "sm", length: "sm" } });
  addText(s, "학습 점수는 투자 가능 여부가 아니라 다음 확인 행동을 정하는 신호다.", pos(78, 622, 1080, 28), { fontSize: 16, bold: true, color: C.navy });
  addNote(s, "발표 포인트: 성향 제한과 지식 접근을 분리한다. 모든 실험 결과에는 입력·공식·seed·가정·한계를 함께 남긴다.");
}

function drawLogin(slide, x, y, w, h) {
  addWireframe(slide, x, y, w, h, "S1 로그인");
  addText(slide, "FinMATE", pos(x + 22, y + 58, w - 44, 32), { fontSize: 25, bold: true, color: C.navy, alignment: "center" });
  addText(slide, "AI 투자 성향 진단과 금융상품 학습", pos(x + 20, y + 92, w - 40, 22), { fontSize: 12, color: C.muted, alignment: "center" });
  addText(slide, "이메일", pos(x + 28, y + 143, w - 56, 18), { fontSize: 12, bold: true, color: C.text });
  addShape(slide, "roundRect", pos(x + 28, y + 165, w - 56, 34), C.white, C.line, 1);
  addText(slide, "learner@example.com", pos(x + 38, y + 172, w - 76, 18), { fontSize: 12, color: C.muted });
  addText(slide, "비밀번호", pos(x + 28, y + 216, w - 56, 18), { fontSize: 12, bold: true, color: C.text });
  addShape(slide, "roundRect", pos(x + 28, y + 238, w - 56, 34), C.white, C.line, 1);
  addText(slide, "••••••••", pos(x + 38, y + 245, w - 76, 18), { fontSize: 12, color: C.muted });
  addButton(slide, "로그인", x + 28, y + 296, w - 56);
  addText(slide, "계정이 없나요? 회원가입", pos(x + 20, y + 343, w - 40, 20), { fontSize: 12, color: C.blue, alignment: "center" });
}
function drawStart(slide, x, y, w, h) {
  addWireframe(slide, x, y, w, h, "S2 진단 시작");
  addText(slide, "오늘의 학습 경로", pos(x + 22, y + 52, w - 44, 28), { fontSize: 19, bold: true, color: C.navy });
  addText(slide, "현재 성향과 학습 목적을 분리해 선택하세요.", pos(x + 22, y + 84, w - 44, 22), { fontSize: 12, color: C.muted });
  const state = addShape(slide, "roundRect", pos(x + 22, y + 122, w - 44, 72), C.paleBlue, C.paleBlue, 0);
  addText(slide, "진단 가능", pos(x + 36, y + 136, 100, 20), { fontSize: 14, bold: true, color: C.blue });
  addText(slide, "다음 진단 가능일: 2026.10.10", pos(x + 36, y + 162, w - 70, 18), { fontSize: 11, color: C.muted });
  addButton(slide, "진단 시작", x + 22, y + 222, w - 44);
  const learn = addShape(slide, "roundRect", pos(x + 22, y + 274, w - 44, 62), C.white, C.line, 1);
  addText(slide, "상품 학습으로 바로 이동", pos(x + 36, y + 288, w - 70, 22), { fontSize: 14, bold: true, color: C.navy });
  addText(slide, "등급과 상관없이 개념을 공부할 수 있어요", pos(x + 36, y + 315, w - 70, 16), { fontSize: 11, color: C.muted });
}
function drawDiagnosis(slide, x, y, w, h) {
  addWireframe(slide, x, y, w, h, "S3 진단 진행");
  addText(slide, "투자 성향 진단", pos(x + 22, y + 52, w - 44, 28), { fontSize: 19, bold: true, color: C.navy });
  addText(slide, "3 / 5", pos(x + w - 72, y + 58, 48, 18), { fontSize: 12, bold: true, color: C.blue, alignment: "right" });
  addRule(slide, x + 22, y + 94, w - 44, C.line);
  addText(slide, "원금 손실 가능성을 어느 정도 감수할 수 있나요?", pos(x + 22, y + 118, w - 44, 44), { fontSize: 15, bold: true, color: C.navy });
  const choices = ["손실을 감수하기 어렵다", "일부 손실은 이해할 수 있다", "높은 변동성도 감수할 수 있다"];
  choices.forEach((c, i) => {
    const yy = y + 190 + i * 46;
    addShape(slide, "roundRect", pos(x + 22, yy, w - 44, 34), i === 1 ? C.paleBlue : C.white, i === 1 ? C.blue : C.line, i === 1 ? 2 : 1);
    addShape(slide, "ellipse", pos(x + 34, yy + 10, 14, 14), i === 1 ? C.blue : C.white, i === 1 ? C.blue : C.line, 1);
    addText(slide, c, pos(x + 58, yy + 8, w - 84, 18), { fontSize: 12, color: C.text });
  });
  addButton(slide, "다음", x + 22, y + h - 58, w - 44);
}
function drawResult(slide, x, y, w, h) {
  addWireframe(slide, x, y, w, h, "S4 분석 결과");
  addText(slide, "분석 결과", pos(x + 22, y + 52, w - 44, 28), { fontSize: 19, bold: true, color: C.navy });
  addLabel(slide, "TEMPLATE 설명", x + 22, y + 94, 120, C.amber, C.paleAmber);
  addText(slide, "위험중립형", pos(x + 22, y + 138, w - 44, 36), { fontSize: 25, bold: true, color: C.blue });
  addText(slide, "점수 51 / 100", pos(x + 22, y + 178, w - 44, 22), { fontSize: 14, color: C.muted });
  addRule(slide, x + 22, y + 220, w - 44, C.line);
  addText(slide, "판정 근거", pos(x + 22, y + 238, w - 44, 18), { fontSize: 13, bold: true, color: C.navy });
  addText(slide, "위험과 손실 가능성을 이해하고 분산을 고려하는 응답이 많았습니다.", pos(x + 22, y + 264, w - 44, 44), { fontSize: 12, color: C.text });
  addButton(slide, "탐색 카테고리 보기", x + 22, y + h - 58, w - 44);
}
function drawExplore(slide, x, y, w, h) {
  addWireframe(slide, x, y, w, h, "S5 상품 탐색");
  addText(slide, "탐색 카테고리", pos(x + 22, y + 52, w - 44, 26), { fontSize: 18, bold: true, color: C.navy });
  addLabel(slide, "채권", x + 22, y + 96, 64, C.blue, C.paleBlue);
  addLabel(slide, "ETF", x + 94, y + 96, 58, C.muted, C.paleBlue2);
  addLabel(slide, "금", x + 160, y + 96, 48, C.muted, C.paleBlue2);
  ["단기 채권 예시", "채권형 펀드 예시", "글로벌 채권 ETF 예시"].forEach((t, i) => {
    const yy = y + 146 + i * 60;
    addShape(slide, "roundRect", pos(x + 22, yy, w - 44, 46), C.white, C.line, 1);
    addText(slide, t, pos(x + 34, yy + 8, w - 110, 18), { fontSize: 13, bold: true, color: C.navy });
    addText(slide, i === 0 ? "낮은 위험 · 1~3년" : "중간 위험 · 3년 이상", pos(x + 34, yy + 27, w - 110, 14), { fontSize: 10, color: C.muted });
    addText(slide, "›", pos(x + w - 54, yy + 12, 24, 20), { fontSize: 22, color: C.blue, alignment: "center" });
  });
  addText(slide, "내부 탐색 기준 · 투자 권유 아님", pos(x + 22, y + h - 34, w - 44, 18), { fontSize: 10, color: C.muted, alignment: "center" });
}
function drawProduct(slide, x, y, w, h) {
  addWireframe(slide, x, y, w, h, "S6 상품 상세");
  addText(slide, "단기 채권 예시", pos(x + 22, y + 52, w - 44, 26), { fontSize: 18, bold: true, color: C.navy });
  addLabel(slide, "위험등급 3", x + 22, y + 94, 94, C.amber, C.paleAmber);
  addText(slide, "예상 투자기간 1~3년", pos(x + 128, y + 99, w - 150, 18), { fontSize: 12, color: C.muted });
  addRule(slide, x + 22, y + 136, w - 44, C.line);
  addText(slide, "무엇이 결과를 바꾸나요?", pos(x + 22, y + 154, w - 44, 18), { fontSize: 13, bold: true, color: C.navy });
  addText(slide, "시장금리 · 듀레이션 · 신용등급 · 중도매도 가격", pos(x + 22, y + 180, w - 44, 36), { fontSize: 12, color: C.text });
  addText(slide, "가입 전 확인", pos(x + 22, y + 238, w - 44, 18), { fontSize: 13, bold: true, color: C.navy });
  addText(slide, "설명서 · 수수료 · 위험등급 · 유동성", pos(x + 22, y + 264, w - 44, 36), { fontSize: 12, color: C.text });
  addText(slide, "교육용 예시이며 투자 권유나 수익을 보장하지 않습니다.", pos(x + 22, y + h - 44, w - 44, 24), { fontSize: 10, color: C.red, alignment: "center" });
}

// Slide 4: wireframes S1/S2
{
  const s = presentation.slides.add();
  addTitle(s, "04. 와이어프레임", "시작 화면", "로그인 이후 진단과 학습으로 분기");
  drawLogin(s, 104, 190, 430, 420);
  drawStart(s, 746, 190, 430, 420);
  addText(s, "S1  회원가입·로그인", pos(104, 625, 430, 24), { fontSize: 15, bold: true, color: C.navy, alignment: "center" });
  addText(s, "S2  진단 시작 / 학습 바로가기", pos(746, 625, 430, 24), { fontSize: 15, bold: true, color: C.navy, alignment: "center" });
  addNote(s, "화면 연결: S1 → S2. S2에서는 진단 가능 여부와 다음 가능일을 보여 주며, 등급과 무관하게 상품 학습으로 이동할 수 있다.");
}

// Slide 5: wireframes S3/S4
{
  const s = presentation.slides.add();
  addTitle(s, "05. 와이어프레임", "진단과 결과", "입력 상태·판정 결과·AI 폴백 상태를 한 흐름으로 확인");
  drawDiagnosis(s, 104, 190, 430, 420);
  drawResult(s, 746, 190, 430, 420);
  addText(s, "S3  진단 진행", pos(104, 625, 430, 24), { fontSize: 15, bold: true, color: C.navy, alignment: "center" });
  addText(s, "S4  분석 결과·근거·탐색", pos(746, 625, 430, 24), { fontSize: 15, bold: true, color: C.navy, alignment: "center" });
  addNote(s, "화면 연결: S3에서 POST /assessments를 호출하고, S4에서 reasoningSource와 결과 근거를 보여 준다. AI 실패도 TEMPLATE 상태로 완료한다.");
}

// Slide 6: wireframes S5/S6
{
  const s = presentation.slides.add();
  addTitle(s, "06. 와이어프레임", "상품 탐색", "상품명보다 위험 변수와 확인 문서를 먼저 보여 주는 구조");
  drawExplore(s, 104, 190, 430, 420);
  drawProduct(s, 746, 190, 430, 420);
  addText(s, "S5  카테고리·상품 목록", pos(104, 625, 430, 24), { fontSize: 15, bold: true, color: C.navy, alignment: "center" });
  addText(s, "S6  상품 상세·확인 항목", pos(746, 625, 430, 24), { fontSize: 15, bold: true, color: C.navy, alignment: "center" });
  addNote(s, "화면 연결: S4 → S5 → S6. 상품 상세는 위험도, 기간, 결과 변수를 설명하고 교육용 고지를 항상 노출한다.");
}

// Slide 7: learning and quiz wireframes
{
  const s = presentation.slides.add();
  addTitle(s, "07. 와이어프레임", "개념 학습과 확인 문제", "개념마다 다른 문항과 구조 그림을 제공");
  const x = 78, y = 190, w = 1124, h = 420;
  addWireframe(s, x, y, w, h, "S7 학습 트리 + S8 개념별 퀴즈");
  addShape(s, "rect", pos(x + 18, y + 48, 250, h - 70), C.paleBlue2, C.line, 1);
  addText(s, "개념 트리", pos(x + 36, y + 64, 200, 24), { fontSize: 17, bold: true, color: C.navy });
  ["투자의 기초 원리", "  위험과 수익", "  변동성", "자산군과 상품 구조", "  채권", "  ETF", "파생상품과 레버리지"].forEach((t, i) => addText(s, t, pos(x + 38, y + 104 + i * 32, 220, 22), { fontSize: i === 0 || i === 4 ? 13 : 12, bold: i === 0 || i === 4, color: i === 4 ? C.blue : C.text }));
  addText(s, "개념 상세", pos(x + 300, y + 66, 360, 24), { fontSize: 17, bold: true, color: C.navy });
  addText(s, "금리가 오르면 채권 가격은 왜 내려갈까?", pos(x + 300, y + 104, 380, 40), { fontSize: 19, bold: true, color: C.blue });
  addShape(s, "roundRect", pos(x + 300, y + 164, 360, 98), C.paleGreen, C.paleGreen, 0);
  addText(s, "구조 그림\n채권 현금흐름 → 할인율 → 현재가격", pos(x + 320, y + 188, 320, 48), { fontSize: 15, bold: true, color: C.green, alignment: "center", verticalAlignment: "middle" });
  addText(s, "공식 출처 · 자주 하는 오해 · 가입 전 확인", pos(x + 300, y + 286, 360, 24), { fontSize: 12, color: C.muted });
  addShape(s, "rect", pos(x + 700, y + 48, 1, h - 70), C.line, C.line, 0);
  addText(s, "확인 문제", pos(x + 744, y + 66, 320, 24), { fontSize: 17, bold: true, color: C.navy });
  addText(s, "금리 상승 시 채권 가격은 어떻게 변하나요?", pos(x + 744, y + 110, 350, 42), { fontSize: 15, bold: true, color: C.text });
  ["상승한다", "하락한다", "항상 같다"].forEach((t, i) => {
    const yy = y + 180 + i * 48;
    addShape(s, "roundRect", pos(x + 744, yy, 340, 34), i === 1 ? C.paleBlue : C.white, i === 1 ? C.blue : C.line, i === 1 ? 2 : 1);
    addText(s, t, pos(x + 762, yy + 7, 300, 20), { fontSize: 13, color: C.text });
  });
  addButton(s, "채점하고 오답 이유 보기", x + 744, y + 340, 340);
  addNote(s, "화면 연결: /learn. 개념마다 quiz를 분리하고, 정답은 서버에서 채점한다. 화면에는 그림·오해·출처·다음 학습 행동을 함께 둔다.");
}

// Slide 8: practice wireframe and mapping
{
  const s = presentation.slides.add();
  addTitle(s, "08. 와이어프레임", "교육용 포트폴리오 연습기", "상품을 사는 화면이 아니라 변수를 바꾸고 의미를 확인하는 화면");
  const x = 78, y = 184, w = 1124, h = 420;
  addWireframe(s, x, y, w, h, "S9 모의 포트폴리오");
  addText(s, "가상 예산", pos(x + 24, y + 52, 120, 20), { fontSize: 13, bold: true, color: C.navy });
  addShape(s, "roundRect", pos(x + 24, y + 78, 172, 34), C.white, C.line, 1);
  addText(s, "₩10,000,000", pos(x + 38, y + 86, 144, 18), { fontSize: 14, bold: true, color: C.text });
  addText(s, "가상 상품 바구니", pos(x + 24, y + 142, 250, 20), { fontSize: 13, bold: true, color: C.navy });
  const rows = [["장기 채권", "₩4,000,000"], ["주식형 ETF", "₩3,000,000"], ["금", "₩2,000,000"]];
  rows.forEach((r, i) => {
    const yy = y + 170 + i * 42;
    addShape(s, "roundRect", pos(x + 24, yy, 278, 32), C.white, C.line, 1);
    addText(s, r[0], pos(x + 38, yy + 7, 140, 18), { fontSize: 12, color: C.text });
    addText(s, r[1], pos(x + 182, yy + 7, 100, 18), { fontSize: 12, bold: true, color: C.navy, alignment: "right" });
  });
  addText(s, "충격 시나리오", pos(x + 344, y + 52, 180, 20), { fontSize: 13, bold: true, color: C.navy });
  addLabel(s, "금리 +1%p", x + 344, y + 80, 100, C.blue, C.paleBlue);
  addLabel(s, "주가 -10%", x + 452, y + 80, 98, C.red, C.paleRed);
  addLabel(s, "환율 +3%", x + 558, y + 80, 98, C.green, C.paleGreen);
  addText(s, "분석 결과", pos(x + 344, y + 142, 180, 20), { fontSize: 13, bold: true, color: C.navy });
  addText(s, "-₩350,000", pos(x + 344, y + 174, 220, 36), { fontSize: 27, bold: true, color: C.red });
  addText(s, "수익률 -3.5%  ·  선형 노출 1.2배", pos(x + 344, y + 214, 280, 20), { fontSize: 12, color: C.muted });
  addShape(s, "roundRect", pos(x + 344, y + 260, 312, 106), C.paleBlue2, C.paleBlue2, 0);
  addText(s, "자산별 기여도\n채권  -₩320,000\nETF   -₩210,000\n금    +₩100,000", pos(x + 364, y + 276, 280, 76), { fontSize: 13, color: C.text });
  addShape(s, "rect", pos(x + 712, y + 48, 1, h - 70), C.line, C.line, 0);
  addText(s, "결과의 의미", pos(x + 756, y + 64, 300, 24), { fontSize: 17, bold: true, color: C.navy });
  addText(s, "금리 충격이 듀레이션이 긴 채권에 더 크게 반영되었습니다.\n\n이는 실제 수익률 예측이 아니라 입력한 가정을 계산한 교육용 결과입니다.", pos(x + 756, y + 110, 350, 110), { fontSize: 14, color: C.text });
  addButton(s, "변경 전후 비교", x + 756, y + 290, 250, C.green);
  addText(s, "API: POST /practice/analyze · POST /practice/compare", pos(x + 756, y + 354, 350, 26), { fontSize: 11, color: C.muted });
  addNote(s, "화면 연결: /practice. 실제 주문이나 특정 종목 추천 없이 가상 상품·충격·계산 결과·의미·한계를 한 화면에 보여 준다.");
}

await fs.mkdir(buildDir, { recursive: true });
await fs.mkdir(path.dirname(finalPath), { recursive: true });
await (await PresentationFile.exportPptx(presentation)).save(candidatePath);

const { finalizePresentation } = await import(path.join(skillDir, "container_tools", "artifact_tool_utils.mjs"));
const stagingDir = path.join(buildDir, "finalizer");
await fs.mkdir(stagingDir, { recursive: true });
const result = await finalizePresentation({
  explicitTotalSlideCount: 8,
  workspaceDir,
  candidatePath,
  finalPath,
  pythonExecutable: "/Users/minkyu/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3",
  integrityValidatorPath: path.join(skillDir, "container_tools", "inspect_presentation_package_integrity.py"),
  layoutValidatorPath: path.join(skillDir, "container_tools", "inspect_presentation_layout_geometry.py"),
  layoutArgs: ["--expected-slide-size-emu", "12192000,6858000", "--validate-bullet-geometry", "--validate-heading-fit"],
  requiredNativeTableOwnerSlides: [],
  requiredNativeChartOwnerSlides: [],
  fontPolicy: { basis: "design", families: [family] },
  verifyArtifactToolImport: true,
  receiptPath: path.join(stagingDir, "FinMATE-플로우차트-와이어프레임.validation.json"),
});
console.log(JSON.stringify(result, null, 2));
