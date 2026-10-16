import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import Header from "../components/Header";
import AdminTabs from "../components/AdminTabs";
import { api, ApiError } from "../api/client";
import type { AdminGlossaryDraft, AdminGlossaryTerm, AdminOverview, AdminRevision, GlossaryCategory } from "../api/types";

type StatusFilter = "ALL" | "PUBLISHED" | "DRAFT" | "PENDING";

const ACTION_LABEL: Record<AdminRevision["action"], string> = { CREATED: "생성", PUBLISHED: "게시", UNPUBLISHED: "공개 내림" };

const EMPTY_DRAFT: AdminGlossaryDraft = {
  categoryId: "market", term: "", fullName: null, english: "", aliases: [], level: "BASIC",
  definition: "", explanation: "", formula: null, example: null, caution: null, related: [],
  conceptSlug: "stocks", sourceName: "", sourceUrl: "https://",
};

function toDraft(term: AdminGlossaryTerm): AdminGlossaryDraft {
  return {
    categoryId: term.categoryId, term: term.term, fullName: term.fullName, english: term.english,
    aliases: [...term.aliases], level: term.level, definition: term.definition, explanation: term.explanation,
    formula: term.formula, example: term.example, caution: term.caution, related: [...term.related],
    conceptSlug: term.conceptSlug, sourceName: term.sourceName, sourceUrl: term.sourceUrl,
  };
}

function errorText(reason: unknown, fallback: string) {
  return reason instanceof ApiError ? reason.message : reason instanceof Error ? reason.message : fallback;
}

/**
 * 콘텐츠 운영 콘솔. 공개 중인 용어는 초안 → 검증 게시로만 바뀌고, 새 용어는 비공개로 만들어진다.
 * 게시·공개 내리기는 모두 감사 기록으로 남고 검색 인덱스에 즉시 반영된다.
 */
export default function AdminGlossaryPage() {
  const [terms, setTerms] = useState<AdminGlossaryTerm[]>([]);
  const [categories, setCategories] = useState<GlossaryCategory[]>([]);
  const [overview, setOverview] = useState<AdminOverview | null>(null);
  const [selectedSlug, setSelectedSlug] = useState("");
  const [creating, setCreating] = useState(false);
  const [newSlug, setNewSlug] = useState("");
  const [draft, setDraft] = useState<AdminGlossaryDraft | null>(null);
  const [revisions, setRevisions] = useState<AdminRevision[]>([]);
  const [filter, setFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    let current = true;
    setLoading(true);
    setError(null);
    api.getMe().then(user => {
      if (user.role !== "ADMIN") throw new Error("관리자 권한이 필요합니다.");
      return Promise.all([api.getAdminGlossaryTerms(), api.getAdminOverview(), api.getGlossaryCategories()]);
    }).then(([data, stats, categoryData]) => {
      if (!current) return;
      setTerms(data.items);
      setOverview(stats);
      setCategories([...categoryData.categories].sort((a, b) => a.order - b.order));
      if (data.items.length > 0) {
        setSelectedSlug(data.items[0].slug);
        setDraft(toDraft(data.items[0]));
      }
    }).catch(reason => {
      if (current) setError(errorText(reason, "관리자 자료를 불러오지 못했습니다."));
    }).finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [retry]);

  useEffect(() => {
    if (!selectedSlug || creating) { setRevisions([]); return; }
    let current = true;
    api.getAdminGlossaryRevisions(selectedSlug)
      .then(data => { if (current) setRevisions(data.items); })
      .catch(() => { if (current) setRevisions([]); });
    return () => { current = false; };
  }, [selectedSlug, creating, terms]);

  const selected = creating ? null : terms.find(item => item.slug === selectedSlug) ?? null;
  const visible = useMemo(() => terms.filter(item => {
    const matchesText = `${item.term} ${item.slug} ${item.categoryId}`.toLocaleLowerCase().includes(filter.toLocaleLowerCase());
    const matchesStatus = statusFilter === "ALL" || (statusFilter === "PENDING" ? item.hasDraft : item.status === statusFilter);
    return matchesText && matchesStatus;
  }), [terms, filter, statusFilter]);

  function select(term: AdminGlossaryTerm) {
    setCreating(false);
    setSelectedSlug(term.slug);
    setDraft(toDraft(term));
    setNotice(null);
    setError(null);
  }

  function startCreate() {
    setCreating(true);
    setSelectedSlug("");
    setNewSlug("");
    setDraft({ ...EMPTY_DRAFT, categoryId: categories[0]?.id ?? "market" });
    setNotice(null);
    setError(null);
  }

  function update<K extends keyof AdminGlossaryDraft>(key: K, value: AdminGlossaryDraft[K]) {
    setDraft(current => current ? { ...current, [key]: value } : current);
  }

  function upsert(updated: AdminGlossaryTerm) {
    setTerms(current => current.some(item => item.slug === updated.slug)
      ? current.map(item => item.slug === updated.slug ? updated : item)
      : [updated, ...current]);
    setSelectedSlug(updated.slug);
    setCreating(false);
    setDraft(toDraft(updated));
    api.getAdminOverview().then(setOverview).catch(() => undefined);
  }

  async function run(label: string, action: () => Promise<AdminGlossaryTerm | void>, success: string) {
    if (busy) return;
    setBusy(label);
    setNotice(null);
    setError(null);
    try {
      const result = await action();
      if (result) upsert(result);
      setNotice(success);
    } catch (reason) {
      setError(errorText(reason, "요청을 처리하지 못했습니다."));
    } finally {
      setBusy(null);
    }
  }

  function save(event: React.FormEvent) {
    event.preventDefault();
    if (!draft) return;
    if (creating) {
      void run("create", () => api.createAdminGlossaryTerm(newSlug.trim(), draft), "새 용어를 비공개로 만들었습니다. 내용을 확인한 뒤 게시하세요.");
    } else if (selected) {
      void run("save", () => api.saveAdminGlossaryTerm(selected.slug, draft), "초안을 저장했습니다. 공개하려면 검토 후 게시를 누르세요.");
    }
  }

  async function discard() {
    if (!selected) return;
    await run("discard", async () => {
      await api.discardAdminGlossaryDraft(selected.slug);
      const fresh = (await api.getAdminGlossaryTerms()).items.find(item => item.slug === selected.slug);
      return fresh;
    }, "초안을 버리고 게시된 내용으로 되돌렸습니다.");
  }

  const canPublish = !!selected && (selected.hasDraft || selected.status === "DRAFT");

  return (
    <div className="app-page">
      <Header />
      <main className="admin-main">
        <AdminTabs />
        <span className="eyebrow">CONTENT OPERATIONS</span>
        <h1>용어 편집과 게시</h1>
        <p className="intro-copy">공개 중인 용어의 수정은 초안으로 저장되고, 검증 후 게시해야 학습 화면과 검색에 반영됩니다. 새 용어는 비공개로 만들어집니다.</p>
        {overview && <div className="admin-overview" aria-label="운영 현황">
          <div className="card"><strong>{overview.publishedTerms}</strong><span>공개 용어</span></div>
          <div className="card"><strong>{overview.hiddenTerms}</strong><span>비공개 용어</span></div>
          <div className="card"><strong>{overview.pendingDrafts}</strong><span>게시 대기 초안</span></div>
          <div className="card"><strong>{overview.users}</strong><span>가입자 (관리자 {overview.admins})</span></div>
          <div className="card"><strong>{overview.completedAssessments}</strong><span>완료된 진단</span></div>
          <div className="card"><strong>{overview.quizAttempts}</strong><span>확인 문제 시도</span></div>
          <div className="card"><strong>v{overview.styleModelVersion}</strong><span>성향 신호 모델 · 검색 v{overview.termModelVersion}</span></div>
        </div>}
        {loading ? <p role="status" className="subtle">관리자 자료를 불러오는 중…</p> : error && terms.length === 0 ? <div role="alert" className="card glossary-message">{error} <button className="btn btn-secondary" onClick={() => setRetry(value => value + 1)}>다시 시도</button></div> : (
          <div className="admin-layout">
            <aside className="card admin-list">
              <button type="button" className="btn btn-primary btn-small" onClick={startCreate}>+ 새 용어</button>
              <label className="field-label" htmlFor="admin-filter">편집할 용어</label>
              <input id="admin-filter" className="field-input" type="search" value={filter} onChange={event => setFilter(event.target.value)} placeholder="이름이나 분류로 찾기" />
              <select className="field-input" aria-label="상태로 거르기" value={statusFilter} onChange={event => setStatusFilter(event.target.value as StatusFilter)}>
                <option value="ALL">전체 상태 ({terms.length})</option>
                <option value="PUBLISHED">공개</option>
                <option value="DRAFT">비공개</option>
                <option value="PENDING">게시 대기 초안</option>
              </select>
              <div className="admin-list-items">
                {visible.map(item => <button key={item.slug} type="button" className={item.slug === selectedSlug ? "is-selected" : ""} onClick={() => select(item)}>
                  <strong>{item.term}</strong>
                  <small><span className={`status-pill ${item.status === "PUBLISHED" ? "is-published" : "is-draft"}`}>{item.status === "PUBLISHED" ? "공개" : "비공개"}</span> {item.categoryId}{item.hasDraft ? " · 초안 있음" : ""}</small>
                </button>)}
              </div>
            </aside>
            {draft && (creating || selected) && <form className="card admin-editor" onSubmit={save}>
              <div className="admin-editor-head">
                {creating ? <div><span className="eyebrow">NEW TERM</span><h2>새 용어 만들기</h2><small>저장하면 비공개 상태로 만들어집니다.</small></div>
                  : selected && <div><span className="eyebrow">{selected.slug}</span><h2>{selected.term}</h2><small><span className={`status-pill ${selected.status === "PUBLISHED" ? "is-published" : "is-draft"}`}>{selected.status === "PUBLISHED" ? "공개" : "비공개"}</span> 버전 {selected.contentVersion}{selected.hasDraft ? " · 미게시 초안 편집 중" : ""}</small></div>}
                {selected?.status === "PUBLISHED" && <Link to={`/glossary/${selected.slug}`}>게시된 내용 보기 ↗</Link>}
              </div>
              <div className="admin-fields">
                {creating && <label>slug (영문 소문자·숫자·하이픈)<input className="field-input" value={newSlug} onChange={event => setNewSlug(event.target.value.toLowerCase())} pattern="[a-z0-9]+(-[a-z0-9]+)*" required placeholder="예: earnings-yield" /></label>}
                <label>용어<input className="field-input" value={draft.term} onChange={event => update("term", event.target.value)} required /></label>
                <label>분류<select className="field-input" value={draft.categoryId} onChange={event => update("categoryId", event.target.value)} required>
                  {categories.map(category => <option key={category.id} value={category.id}>{category.name}</option>)}
                </select></label>
                <label>영문 이름<input className="field-input" value={draft.english ?? ""} onChange={event => update("english", event.target.value)} required /></label>
                <label>전체 이름<input className="field-input" value={draft.fullName ?? ""} onChange={event => update("fullName", event.target.value || null)} /></label>
                <label>난이도<select className="field-input" value={draft.level} onChange={event => update("level", event.target.value as AdminGlossaryDraft["level"])}><option value="BASIC">기초</option><option value="INTERMEDIATE">중급</option><option value="ADVANCED">심화</option></select></label>
                <label>별칭 · 쉼표로 구분<input className="field-input" value={draft.aliases.join(", ")} onChange={event => update("aliases", event.target.value.split(",").map(value => value.trim()).filter(Boolean))} /></label>
                <label className="admin-wide">한 줄 정의<textarea className="field-input" value={draft.definition} maxLength={500} onChange={event => update("definition", event.target.value)} required /></label>
                <label className="admin-wide">자세한 설명<textarea className="field-input" value={draft.explanation} maxLength={4000} onChange={event => update("explanation", event.target.value)} required /></label>
                <label className="admin-wide">계산식<textarea className="field-input" value={draft.formula ?? ""} onChange={event => update("formula", event.target.value || null)} /></label>
                <label className="admin-wide">예시<textarea className="field-input" value={draft.example ?? ""} onChange={event => update("example", event.target.value || null)} /></label>
                <label className="admin-wide">주의할 점<textarea className="field-input" value={draft.caution ?? ""} onChange={event => update("caution", event.target.value || null)} /></label>
                <label>관련 용어 slug · 쉼표로 구분<input className="field-input" value={draft.related.join(", ")} onChange={event => update("related", event.target.value.split(",").map(value => value.trim()).filter(Boolean))} /></label>
                <label>연결 개념 slug<input className="field-input" value={draft.conceptSlug ?? ""} onChange={event => update("conceptSlug", event.target.value)} required /></label>
                <label>출처 이름<input className="field-input" value={draft.sourceName} onChange={event => update("sourceName", event.target.value)} required /></label>
                <label>출처 URL<input className="field-input" type="url" value={draft.sourceUrl} onChange={event => update("sourceUrl", event.target.value)} required /></label>
              </div>
              {error && <p className="error-text" role="alert">{error}</p>}
              {notice && <p className="admin-notice" role="status">{notice}</p>}
              <div className="admin-actions">
                <button className="btn btn-secondary" type="submit" disabled={!!busy}>{busy === "save" || busy === "create" ? "저장 중…" : creating ? "비공개로 만들기" : "초안 저장"}</button>
                {selected?.hasDraft && <button className="btn btn-secondary" type="button" onClick={() => void discard()} disabled={!!busy}>초안 버리기</button>}
                {selected?.status === "PUBLISHED" && <button className="btn btn-secondary" type="button" disabled={!!busy}
                  onClick={() => void run("unpublish", () => api.unpublishAdminGlossaryTerm(selected.slug), "공개를 내렸습니다. 용어 사전과 검색에서 즉시 사라집니다.")}>
                  {busy === "unpublish" ? "처리 중…" : "공개 내리기"}</button>}
                {selected && <button className="btn btn-primary" type="button" disabled={!canPublish || !!busy}
                  onClick={() => void run("publish", () => api.publishAdminGlossaryTerm(selected.slug), "검증을 거쳐 게시했습니다. 검색에도 바로 반영됩니다.")}>
                  {busy === "publish" ? "게시 중…" : "검증하고 게시"}</button>}
              </div>
              {selected && <section aria-label="게시 이력" style={{ marginTop: 18 }}>
                <h3 style={{ fontSize: 14, margin: "0 0 8px" }}>게시 이력</h3>
                {revisions.length === 0 ? <p className="subtle" style={{ margin: 0 }}>관리자 화면에서 바뀐 기록이 없습니다. 시드 콘텐츠 그대로입니다.</p>
                  : <ul className="revision-list">{revisions.map(revision => <li key={revision.id}><span><b>{ACTION_LABEL[revision.action]}</b> · {revision.editorEmail}</span><span>{new Date(revision.createdAt).toLocaleString("ko-KR")}</span></li>)}</ul>}
              </section>}
            </form>}
          </div>
        )}
      </main>
    </div>
  );
}
