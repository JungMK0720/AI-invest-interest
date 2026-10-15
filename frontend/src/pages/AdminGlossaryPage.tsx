import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import Header from "../components/Header";
import { api, ApiError } from "../api/client";
import type { AdminGlossaryDraft, AdminGlossaryTerm } from "../api/types";

function toDraft(term: AdminGlossaryTerm): AdminGlossaryDraft {
  return {
    categoryId: term.categoryId,
    term: term.term,
    fullName: term.fullName,
    english: term.english,
    aliases: [...term.aliases],
    level: term.level,
    definition: term.definition,
    explanation: term.explanation,
    formula: term.formula,
    example: term.example,
    caution: term.caution,
    related: [...term.related],
    conceptSlug: term.conceptSlug,
    sourceName: term.sourceName,
    sourceUrl: term.sourceUrl,
  };
}

export default function AdminGlossaryPage() {
  const [terms, setTerms] = useState<AdminGlossaryTerm[]>([]);
  const [selectedSlug, setSelectedSlug] = useState("");
  const [draft, setDraft] = useState<AdminGlossaryDraft | null>(null);
  const [filter, setFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    let current = true;
    setLoading(true);
    setError(null);
    api.getMe().then(user => {
      if (user.role !== "ADMIN") throw new Error("관리자 권한이 필요합니다.");
      return api.getAdminGlossaryTerms();
    }).then(data => {
      if (!current) return;
      setTerms(data.items);
      if (data.items.length > 0) {
        setSelectedSlug(data.items[0].slug);
        setDraft(toDraft(data.items[0]));
      }
    }).catch(reason => {
      if (current) setError(reason instanceof ApiError ? reason.message : reason instanceof Error ? reason.message : "관리자 자료를 불러오지 못했습니다.");
    }).finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [retry]);

  const selected = terms.find(item => item.slug === selectedSlug) ?? null;
  const visible = useMemo(() => terms.filter(item => `${item.term} ${item.slug} ${item.categoryId}`.toLocaleLowerCase().includes(filter.toLocaleLowerCase())), [terms, filter]);

  function select(term: AdminGlossaryTerm) {
    setSelectedSlug(term.slug);
    setDraft(toDraft(term));
    setNotice(null);
    setError(null);
  }

  function update<K extends keyof AdminGlossaryDraft>(key: K, value: AdminGlossaryDraft[K]) {
    setDraft(current => current ? { ...current, [key]: value } : current);
  }

  function replace(updated: AdminGlossaryTerm) {
    setTerms(current => current.map(item => item.slug === updated.slug ? updated : item));
    setDraft(toDraft(updated));
  }

  async function save(event: React.FormEvent) {
    event.preventDefault();
    if (!selected || !draft || saving) return;
    setSaving(true);
    setNotice(null);
    setError(null);
    try {
      replace(await api.saveAdminGlossaryTerm(selected.slug, draft));
      setNotice("초안을 저장했습니다. 공개하려면 검토 후 게시를 누르세요.");
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "초안을 저장하지 못했습니다.");
    } finally {
      setSaving(false);
    }
  }

  async function publish() {
    if (!selected || publishing) return;
    setPublishing(true);
    setNotice(null);
    setError(null);
    try {
      replace(await api.publishAdminGlossaryTerm(selected.slug));
      setNotice("검증을 거쳐 용어를 게시했습니다.");
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "용어를 게시하지 못했습니다.");
    } finally {
      setPublishing(false);
    }
  }

  return (
    <div className="app-page">
      <Header />
      <main className="admin-main">
        <span className="eyebrow">CONTENT OPERATIONS</span>
        <h1>용어 편집과 게시</h1>
        <p className="intro-copy">수정은 초안으로 저장되고, 검증 후 게시해야 학습 화면에 반영됩니다. 출처와 설명을 함께 확인하세요.</p>
        {loading ? <p role="status" className="subtle">관리자 자료를 불러오는 중…</p> : error && terms.length === 0 ? <div role="alert" className="card glossary-message">{error} <button className="btn btn-secondary" onClick={() => setRetry(value => value + 1)}>다시 시도</button></div> : (
          <div className="admin-layout">
            <aside className="card admin-list">
              <label className="field-label" htmlFor="admin-filter">편집할 용어</label>
              <input id="admin-filter" className="field-input" type="search" value={filter} onChange={event => setFilter(event.target.value)} placeholder="이름이나 분류로 찾기" />
              <div className="admin-list-items">
                {visible.map(item => <button key={item.slug} type="button" className={item.slug === selectedSlug ? "is-selected" : ""} onClick={() => select(item)}>
                  <strong>{item.term}</strong><small>{item.categoryId}{item.hasDraft ? " · 미게시 초안" : ""}</small>
                </button>)}
              </div>
            </aside>
            {selected && draft && <form className="card admin-editor" onSubmit={save}>
              <div className="admin-editor-head"><div><span className="eyebrow">{selected.slug}</span><h2>{selected.term}</h2><small>현재 상태 {selected.status} · 버전 {selected.contentVersion}</small></div><Link to={`/glossary/${selected.slug}`}>게시된 내용 보기 ↗</Link></div>
              <div className="admin-fields">
                <label>용어<input className="field-input" value={draft.term} onChange={event => update("term", event.target.value)} required /></label>
                <label>분류 ID<input className="field-input" value={draft.categoryId} onChange={event => update("categoryId", event.target.value)} required /></label>
                <label>영문 이름<input className="field-input" value={draft.english ?? ""} onChange={event => update("english", event.target.value)} required /></label>
                <label>전체 이름<input className="field-input" value={draft.fullName ?? ""} onChange={event => update("fullName", event.target.value || null)} /></label>
                <label>난이도<select className="field-input" value={draft.level} onChange={event => update("level", event.target.value as AdminGlossaryDraft["level"])}><option value="BASIC">기초</option><option value="INTERMEDIATE">중급</option><option value="ADVANCED">심화</option></select></label>
                <label>별칭 · 쉼표로 구분<input className="field-input" value={draft.aliases.join(", ")} onChange={event => update("aliases", event.target.value.split(",").map(value => value.trim()).filter(Boolean))} /></label>
                <label className="admin-wide">한 줄 정의<textarea className="field-input" value={draft.definition} onChange={event => update("definition", event.target.value)} required /></label>
                <label className="admin-wide">자세한 설명<textarea className="field-input" value={draft.explanation} onChange={event => update("explanation", event.target.value)} required /></label>
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
              <div className="admin-actions"><button className="btn btn-secondary" type="submit" disabled={saving || publishing}>{saving ? "저장 중…" : "초안 저장"}</button><button className="btn btn-primary" type="button" onClick={publish} disabled={!selected.hasDraft || saving || publishing}>{publishing ? "게시 중…" : "검증하고 게시"}</button></div>
            </form>}
          </div>
        )}
      </main>
    </div>
  );
}
