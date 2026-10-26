import { useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import Header from "../components/Header";
import AdminTabs from "../components/AdminTabs";
import ConceptDiagram from "../components/ConceptDiagram";
import { api, ApiError } from "../api/client";
import type { AdminConcept, AdminConceptContent, AdminConceptSummary, AdminOverview, AdminQuizItem, AdminQuizOption, AdminRevision, SimulationScenario } from "../api/types";

type StatusFilter = "ALL" | "PUBLISHED" | "DRAFT" | "PENDING";

const ACTION_LABEL: Record<AdminRevision["action"], string> = { CREATED: "생성", PUBLISHED: "게시", UNPUBLISHED: "공개 내림" };
const DIFFICULTY_LABEL: Record<AdminQuizItem["difficulty"], string> = { BASIC: "기초", INTERMEDIATE: "중급", ADVANCED: "심화" };
const MAX_OPTIONS = 6;
const MIN_OPTIONS = 3;

/** 문항 하나의 편집 상태. key는 화면 목록용, isNew면 문항 id를 고칠 수 있다(기존 id는 복습 기록의 키라 고정). */
interface QuizForm extends AdminQuizItem {
  key: string;
  isNew: boolean;
}

/**
 * 목록 필드는 입력 중인 줄바꿈이 사라지지 않도록 원문 그대로 들고 있다가 저장할 때 나눈다.
 * 설명은 빈 줄로 문단을, 확인 포인트·체크리스트는 줄 단위로 항목을 나눈다.
 */
interface ConceptForm {
  parent: string;
  level: number;
  order: number;
  title: string;
  summary: string;
  oneLiner: string;
  explanationText: string;
  keyPointsText: string;
  checklistText: string;
  misconception: string;
  diagramKey: string;
  sourceName: string;
  sourceUrl: string;
  simulationId: string;
  quiz: QuizForm[];
}

let keySeed = 0;
const nextKey = () => `quiz-${++keySeed}`;

function toForm(content: AdminConceptContent): ConceptForm {
  return {
    parent: content.parent ?? "", level: content.level, order: content.order, title: content.title,
    summary: content.summary, oneLiner: content.oneLiner, explanationText: content.explanation.join("\n\n"),
    keyPointsText: content.keyPoints.join("\n"), checklistText: content.checklist.join("\n"),
    misconception: content.misconception, diagramKey: content.diagramKey, sourceName: content.sourceName,
    sourceUrl: content.sourceUrl, simulationId: content.simulationId ?? "",
    quiz: content.quiz.map(item => ({ ...item, options: item.options.map(option => ({ ...option })), key: nextKey(), isNew: false })),
  };
}

function lines(text: string) {
  return text.split("\n").map(value => value.trim()).filter(Boolean);
}

function toContent(form: ConceptForm): AdminConceptContent {
  return {
    parent: form.level === 1 ? null : form.parent || null, level: form.level, order: form.order,
    title: form.title, summary: form.summary, oneLiner: form.oneLiner,
    explanation: form.explanationText.split(/\n\s*\n/).map(value => value.trim()).filter(Boolean),
    keyPoints: lines(form.keyPointsText), checklist: lines(form.checklistText),
    misconception: form.misconception, diagramKey: form.diagramKey.trim(), sourceName: form.sourceName,
    sourceUrl: form.sourceUrl, simulationId: form.simulationId || null,
    quiz: form.quiz.map(item => ({ id: item.id.trim(), prompt: item.prompt, difficulty: item.difficulty, options: item.options, takeaway: item.takeaway })),
  };
}

function errorText(reason: unknown, fallback: string) {
  return reason instanceof ApiError ? reason.message : reason instanceof Error ? reason.message : fallback;
}

function statusPill(status: AdminConceptSummary["status"]) {
  return <span className={`status-pill ${status === "PUBLISHED" ? "is-published" : "is-draft"}`}>{status === "PUBLISHED" ? "공개" : "비공개"}</span>;
}

/**
 * 학습 개념·확인 문제 운영 화면. 개념 본문과 그 문항들은 한 초안으로 저장되고, 검증 게시를 거쳐야
 * 개념 학습 화면과 채점에 반영된다. 게시하면 학습 콘텐츠 버전이 바뀌어 이전에 불러 둔 문제는 다시 불러오게 된다.
 */
export default function AdminLearningPage() {
  const [concepts, setConcepts] = useState<AdminConceptSummary[]>([]);
  const [contentVersion, setContentVersion] = useState("");
  const [overview, setOverview] = useState<AdminOverview | null>(null);
  const [scenarios, setScenarios] = useState<SimulationScenario[]>([]);
  const [selected, setSelected] = useState<AdminConcept | null>(null);
  const [form, setForm] = useState<ConceptForm | null>(null);
  const [dirty, setDirty] = useState(false);
  const [revisions, setRevisions] = useState<AdminRevision[]>([]);
  const [filter, setFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [retry, setRetry] = useState(0);
  const requested = useRef("");

  useEffect(() => {
    let current = true;
    setLoading(true);
    setError(null);
    api.getMe().then(user => {
      if (user.role !== "ADMIN") throw new Error("관리자 권한이 필요합니다.");
      return Promise.all([api.getAdminConcepts(), api.getAdminOverview(), api.getSimulations()]);
    }).then(([data, stats, simulationData]) => {
      if (!current) return;
      setConcepts(data.items);
      setContentVersion(data.learningContentVersion);
      setOverview(stats);
      setScenarios(simulationData.scenarios);
      if (data.items.length > 0) void open(data.items[0].slug);
    }).catch(reason => {
      if (current) setError(errorText(reason, "관리자 자료를 불러오지 못했습니다."));
    }).finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [retry]);

  useEffect(() => {
    if (!selected) { setRevisions([]); return; }
    let current = true;
    api.getAdminConceptRevisions(selected.slug)
      .then(data => { if (current) setRevisions(data.items); })
      .catch(() => { if (current) setRevisions([]); });
    return () => { current = false; };
  }, [selected]);

  const visible = useMemo(() => concepts.filter(item => {
    const matchesText = `${item.title} ${item.slug}`.toLocaleLowerCase().includes(filter.toLocaleLowerCase());
    const matchesStatus = statusFilter === "ALL" || (statusFilter === "PENDING" ? item.hasDraft : item.status === statusFilter);
    return matchesText && matchesStatus;
  }), [concepts, filter, statusFilter]);

  const parentOptions = useMemo(() => form ? concepts.filter(item => item.level === form.level - 1 && item.slug !== selected?.slug) : [],
    [concepts, form, selected]);

  async function open(slug: string) {
    requested.current = slug;
    setNotice(null);
    setError(null);
    try {
      const concept = await api.getAdminConcept(slug);
      if (requested.current !== slug) return;
      setSelected(concept);
      setForm(toForm(concept.content));
      setDirty(false);
    } catch (reason) {
      setError(errorText(reason, "개념을 불러오지 못했습니다."));
    }
  }

  function select(slug: string) {
    if (slug === selected?.slug) return;
    if (dirty && !window.confirm("저장하지 않은 변경이 있습니다. 다른 개념으로 이동할까요?")) return;
    void open(slug);
  }

  function update<K extends keyof ConceptForm>(key: K, value: ConceptForm[K]) {
    setForm(current => current ? { ...current, [key]: value } : current);
    setDirty(true);
  }

  function updateQuiz(key: string, change: (item: QuizForm) => QuizForm) {
    setForm(current => current ? { ...current, quiz: current.quiz.map(item => item.key === key ? change(item) : item) } : current);
    setDirty(true);
  }

  function updateOption(key: string, index: number, change: Partial<AdminQuizOption>) {
    updateQuiz(key, item => ({ ...item, options: item.options.map((option, i) => i === index ? { ...option, ...change } : option) }));
  }

  function markCorrect(key: string, index: number) {
    updateQuiz(key, item => ({ ...item, options: item.options.map((option, i) => ({ ...option, correct: i === index })) }));
  }

  function addQuiz() {
    if (!form || !selected) return;
    const used = new Set(form.quiz.map(item => item.id));
    let n = form.quiz.length + 1;
    while (used.has(`${selected.slug}-q${n}`)) n++;
    const blank: AdminQuizOption = { label: "", correct: false, rationale: "" };
    update("quiz", [...form.quiz, {
      key: nextKey(), isNew: true, id: `${selected.slug}-q${n}`, prompt: "", difficulty: "BASIC", takeaway: "",
      options: [{ ...blank, correct: true }, { ...blank }, { ...blank }],
    }]);
  }

  function changeLevel(level: number) {
    setForm(current => {
      if (!current) return current;
      const parentStillFits = concepts.some(item => item.slug === current.parent && item.level === level - 1);
      return { ...current, level, parent: level === 1 || !parentStillFits ? "" : current.parent };
    });
    setDirty(true);
  }

  async function refresh(updated: AdminConcept) {
    setSelected(updated);
    setForm(toForm(updated.content));
    setDirty(false);
    const [data, stats] = await Promise.all([api.getAdminConcepts(), api.getAdminOverview()]);
    setConcepts(data.items);
    setContentVersion(data.learningContentVersion);
    setOverview(stats);
  }

  async function run(label: string, action: () => Promise<AdminConcept>, success: string) {
    if (busy) return;
    setBusy(label);
    setNotice(null);
    setError(null);
    try {
      await refresh(await action());
      setNotice(success);
    } catch (reason) {
      setError(errorText(reason, "요청을 처리하지 못했습니다."));
    } finally {
      setBusy(null);
    }
  }

  function save(event: React.FormEvent) {
    event.preventDefault();
    if (!form || !selected) return;
    void run("save", () => api.saveAdminConcept(selected.slug, toContent(form)), "초안을 저장했습니다. 공개하려면 검토 후 게시를 누르세요.");
  }

  function discard() {
    if (!selected) return;
    void run("discard", async () => {
      await api.discardAdminConceptDraft(selected.slug);
      return api.getAdminConcept(selected.slug);
    }, "초안을 버리고 게시된 내용으로 되돌렸습니다.");
  }

  const canPublish = !!selected && !dirty && (selected.hasDraft || selected.status === "DRAFT");

  return (
    <div className="app-page">
      <Header />
      <main className="admin-main">
        <AdminTabs />
        <span className="eyebrow">CONTENT OPERATIONS</span>
        <h1>학습 개념·확인 문제 편집과 게시</h1>
        <p className="intro-copy">개념 본문과 확인 문제는 한 초안으로 저장되고, 트리·정답·해설·출처 검증을 통과해야 게시됩니다. 게시하면 개념 학습 화면과 채점에 재시작 없이 바로 반영됩니다.</p>
        {overview && <div className="admin-overview" aria-label="운영 현황">
          <div className="card"><strong>{overview.publishedConcepts}</strong><span>공개 개념</span></div>
          <div className="card"><strong>{overview.hiddenConcepts}</strong><span>비공개 개념</span></div>
          <div className="card"><strong>{overview.pendingConceptDrafts}</strong><span>게시 대기 초안</span></div>
          <div className="card"><strong>{overview.quizAttempts}</strong><span>확인 문제 시도</span></div>
          <div className="card admin-version"><strong title={contentVersion}>{contentVersion}</strong><span>학습 콘텐츠 버전 · 게시하면 바뀝니다</span></div>
        </div>}
        {loading ? <p role="status" className="subtle">관리자 자료를 불러오는 중…</p> : error && concepts.length === 0 ? <div role="alert" className="card glossary-message">{error} <button className="btn btn-secondary" onClick={() => setRetry(value => value + 1)}>다시 시도</button></div> : (
          <div className="admin-layout">
            <aside className="card admin-list">
              <label className="field-label" htmlFor="admin-concept-filter">편집할 개념</label>
              <input id="admin-concept-filter" className="field-input" type="search" value={filter} onChange={event => setFilter(event.target.value)} placeholder="제목이나 slug로 찾기" />
              <select className="field-input" aria-label="상태로 거르기" value={statusFilter} onChange={event => setStatusFilter(event.target.value as StatusFilter)}>
                <option value="ALL">전체 상태 ({concepts.length})</option>
                <option value="PUBLISHED">공개</option>
                <option value="DRAFT">비공개</option>
                <option value="PENDING">게시 대기 초안</option>
              </select>
              <div className="admin-list-items">
                {visible.map(item => <button key={item.slug} type="button" className={item.slug === selected?.slug ? "is-selected" : ""}
                  style={{ paddingLeft: 10 + (item.level - 1) * 14 }} onClick={() => select(item.slug)}>
                  <strong>{item.title}</strong>
                  <small>{statusPill(item.status)} 문제 {item.quizCount}개{item.hasDraft ? " · 초안 있음" : ""}</small>
                </button>)}
              </div>
            </aside>
            {form && selected && <form className="card admin-editor" onSubmit={save}>
              <div className="admin-editor-head">
                <div><span className="eyebrow">{selected.slug}</span><h2>{selected.content.title}</h2>
                  <small>{statusPill(selected.status)} 버전 {selected.contentVersion}{selected.hasDraft ? " · 미게시 초안 편집 중" : ""}{dirty ? " · 저장하지 않은 변경" : ""}</small></div>
                {selected.status === "PUBLISHED" && <Link to={`/learn?concept=${encodeURIComponent(selected.slug)}`}>게시된 내용 보기 ↗</Link>}
              </div>
              <div className="admin-fields">
                <label>제목<input className="field-input" value={form.title} maxLength={160} onChange={event => update("title", event.target.value)} required /></label>
                <label>요약<input className="field-input" value={form.summary} maxLength={300} onChange={event => update("summary", event.target.value)} required /></label>
                <label>레벨<select className="field-input" value={form.level} onChange={event => changeLevel(Number(event.target.value))}>
                  <option value={1}>1 · 대분류</option><option value={2}>2 · 상품군/핵심 개념</option><option value={3}>3 · 세부 개념</option>
                </select></label>
                <label>상위 개념<select className="field-input" value={form.parent} disabled={form.level === 1} onChange={event => update("parent", event.target.value)} required={form.level > 1}>
                  <option value="">{form.level === 1 ? "없음 (최상위)" : "선택하세요"}</option>
                  {parentOptions.map(item => <option key={item.slug} value={item.slug}>{item.title} ({item.slug}){item.status === "DRAFT" ? " · 비공개" : ""}</option>)}
                </select></label>
                <label>같은 층 안의 순서<input className="field-input" type="number" min={0} max={999} value={form.order} onChange={event => update("order", Number(event.target.value))} required /></label>
                <label>모의투자 시나리오<select className="field-input" value={form.simulationId} onChange={event => update("simulationId", event.target.value)}>
                  <option value="">연결 안 함</option>
                  {scenarios.map(scenario => <option key={scenario.id} value={scenario.id}>{scenario.title}</option>)}
                </select></label>
                <label className="admin-wide">한 줄 정의<textarea className="field-input" value={form.oneLiner} maxLength={500} onChange={event => update("oneLiner", event.target.value)} required /></label>
                <label className="admin-wide">설명 · 빈 줄로 문단 구분<textarea className="field-input admin-tall" value={form.explanationText} onChange={event => update("explanationText", event.target.value)} required /></label>
                <label className="admin-wide">확인 포인트 · 한 줄에 하나<textarea className="field-input" value={form.keyPointsText} onChange={event => update("keyPointsText", event.target.value)} required /></label>
                <label className="admin-wide">상품을 볼 때 확인할 문서·항목 · 한 줄에 하나<textarea className="field-input" value={form.checklistText} onChange={event => update("checklistText", event.target.value)} required /></label>
                <label className="admin-wide">자주 하는 오해<textarea className="field-input" value={form.misconception} maxLength={1000} onChange={event => update("misconception", event.target.value)} required /></label>
                <label>구조 그림 키 · 영문 대문자<input className="field-input" value={form.diagramKey} pattern="[A-Z][A-Z_]*" onChange={event => update("diagramKey", event.target.value.toUpperCase())} required /></label>
                <div className="admin-diagram-preview" aria-label="구조 그림 미리보기"><ConceptDiagram kind={form.diagramKey} /></div>
                <label>출처 이름<input className="field-input" value={form.sourceName} maxLength={200} onChange={event => update("sourceName", event.target.value)} required /></label>
                <label>출처 URL<input className="field-input" type="url" value={form.sourceUrl} onChange={event => update("sourceUrl", event.target.value)} required /></label>
              </div>

              <section className="admin-quiz" aria-label="확인 문제">
                <div className="admin-quiz-head">
                  <h3>확인 문제 ({form.quiz.length})</h3>
                  <button type="button" className="btn btn-secondary btn-small" onClick={addQuiz} disabled={form.quiz.length >= 10}>+ 문항 추가</button>
                </div>
                {form.quiz.length === 0 && <p className="subtle">문항이 없으면 학습 화면에 “확인 문제가 아직 없습니다”로 보입니다.</p>}
                {form.quiz.map((item, itemIndex) => <fieldset key={item.key} className="admin-quiz-item">
                  <legend>문항 {itemIndex + 1}</legend>
                  <div className="admin-fields">
                    <label>문항 id{item.isNew ? " · 새 문항" : " · 복습 기록의 키라 고정"}<input className="field-input" value={item.id} readOnly={!item.isNew}
                      pattern="[a-z0-9]+(-[a-z0-9]+)*" onChange={event => updateQuiz(item.key, current => ({ ...current, id: event.target.value.toLowerCase() }))} required /></label>
                    <label>난이도<select className="field-input" value={item.difficulty} onChange={event => updateQuiz(item.key, current => ({ ...current, difficulty: event.target.value as AdminQuizItem["difficulty"] }))}>
                      {Object.entries(DIFFICULTY_LABEL).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                    </select></label>
                    <label className="admin-wide">질문<textarea className="field-input" value={item.prompt} maxLength={500} onChange={event => updateQuiz(item.key, current => ({ ...current, prompt: event.target.value }))} required /></label>
                  </div>
                  <div className="admin-options" role="radiogroup" aria-label={`문항 ${itemIndex + 1} 정답`}>
                    {item.options.map((option, optionIndex) => <div key={optionIndex} className={`admin-option${option.correct ? " is-correct" : ""}`}>
                      <label className="admin-option-correct"><input type="radio" name={`correct-${item.key}`} checked={option.correct} onChange={() => markCorrect(item.key, optionIndex)} /> 정답</label>
                      <div className="admin-option-body">
                        <input className="field-input" aria-label={`선택지 ${optionIndex + 1}`} placeholder={`선택지 ${optionIndex + 1}`} value={option.label} maxLength={300}
                          onChange={event => updateOption(item.key, optionIndex, { label: event.target.value })} required />
                        <textarea className="field-input" aria-label={`선택지 ${optionIndex + 1} 해설`} placeholder={option.correct ? "왜 맞는지" : "왜 틀렸는지"} value={option.rationale} maxLength={1000}
                          onChange={event => updateOption(item.key, optionIndex, { rationale: event.target.value })} required />
                      </div>
                      <button type="button" className="btn btn-secondary btn-small" aria-label={`선택지 ${optionIndex + 1} 삭제`} disabled={item.options.length <= MIN_OPTIONS}
                        onClick={() => updateQuiz(item.key, current => {
                          const options = current.options.filter((_, i) => i !== optionIndex);
                          return { ...current, options: options.some(value => value.correct) ? options : options.map((value, i) => ({ ...value, correct: i === 0 })) };
                        })}>삭제</button>
                    </div>)}
                  </div>
                  <div className="admin-quiz-tools">
                    <button type="button" className="btn btn-secondary btn-small" disabled={item.options.length >= MAX_OPTIONS}
                      onClick={() => updateQuiz(item.key, current => ({ ...current, options: [...current.options, { label: "", correct: false, rationale: "" }] }))}>+ 선택지 추가</button>
                    <button type="button" className="btn btn-secondary btn-small" onClick={() => update("quiz", form.quiz.filter(value => value.key !== item.key))}>문항 삭제</button>
                  </div>
                  <label className="admin-takeaway">정답과 무관하게 남길 한 줄 결론<textarea className="field-input" value={item.takeaway} maxLength={1000} onChange={event => updateQuiz(item.key, current => ({ ...current, takeaway: event.target.value }))} required /></label>
                </fieldset>)}
              </section>

              {error && <p className="error-text" role="alert">{error}</p>}
              {notice && <p className="admin-notice" role="status">{notice}</p>}
              {dirty && (selected.hasDraft || selected.status === "DRAFT") && <p className="subtle admin-hint">게시는 저장된 초안을 기준으로 합니다. 먼저 초안을 저장하세요.</p>}
              <div className="admin-actions">
                <button className="btn btn-secondary" type="submit" disabled={!!busy}>{busy === "save" ? "저장 중…" : "초안 저장"}</button>
                {selected.hasDraft && <button className="btn btn-secondary" type="button" onClick={discard} disabled={!!busy}>초안 버리기</button>}
                {selected.status === "PUBLISHED" && <button className="btn btn-secondary" type="button" disabled={!!busy}
                  onClick={() => void run("unpublish", () => api.unpublishAdminConcept(selected.slug), "공개를 내렸습니다. 개념 학습 화면과 추천에서 즉시 사라집니다.")}>
                  {busy === "unpublish" ? "처리 중…" : "공개 내리기"}</button>}
                <button className="btn btn-primary" type="button" disabled={!canPublish || !!busy}
                  onClick={() => void run("publish", () => api.publishAdminConcept(selected.slug), "검증을 거쳐 게시했습니다. 개념 학습 화면과 채점에 바로 반영됩니다.")}>
                  {busy === "publish" ? "게시 중…" : "검증하고 게시"}</button>
              </div>
              <section aria-label="게시 이력" style={{ marginTop: 18 }}>
                <h3 style={{ fontSize: 14, margin: "0 0 8px" }}>게시 이력</h3>
                {revisions.length === 0 ? <p className="subtle" style={{ margin: 0 }}>관리자 화면에서 바뀐 기록이 없습니다. 시드 콘텐츠 그대로입니다.</p>
                  : <ul className="revision-list">{revisions.map(revision => <li key={revision.id}><span><b>{ACTION_LABEL[revision.action]}</b> · {revision.editorEmail}</span><span>{new Date(revision.createdAt).toLocaleString("ko-KR")}</span></li>)}</ul>}
              </section>
            </form>}
          </div>
        )}
      </main>
    </div>
  );
}
