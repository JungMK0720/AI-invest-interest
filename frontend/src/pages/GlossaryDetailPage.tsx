import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import Header from "../components/Header";
import { useAuth } from "../context/AuthContext";
import { api, ApiError } from "../api/client";
import type { GlossaryTerm } from "../api/types";

const levelName: Record<string, string> = { BASIC: "기초", INTERMEDIATE: "중급", ADVANCED: "심화" };

export default function GlossaryDetailPage() {
  const { slug = "" } = useParams();
  const { isAuthenticated } = useAuth();
  const [term, setTerm] = useState<GlossaryTerm | null>(null);
  const [bookmarked, setBookmarked] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    let current = true;
    setLoading(true);
    setError(null);
    setTerm(null);
    Promise.all([api.getGlossaryTerm(slug), isAuthenticated ? api.getGlossaryBookmarks() : Promise.resolve({ terms: [] })])
      .then(([detail, saved]) => {
        if (!current) return;
        setTerm(detail);
        setBookmarked(saved.terms.some(item => item.slug === slug));
      })
      .catch(reason => {
        if (current) setError(reason instanceof ApiError ? reason.message : "용어를 불러오지 못했습니다.");
      })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [slug, retry, isAuthenticated]);

  async function toggleBookmark() {
    if (!term || saving) return;
    setSaving(true);
    setError(null);
    try {
      if (bookmarked) await api.removeGlossaryBookmark(term.slug);
      else await api.addGlossaryBookmark(term.slug);
      setBookmarked(value => !value);
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "저장 상태를 바꾸지 못했습니다.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="app-page">
      <Header />
      <main className="glossary-detail-main">
        <Link className="glossary-back" to="/glossary">← 용어 목록으로</Link>
        {loading && <p role="status" className="subtle">용어를 불러오는 중…</p>}
        {error && <div className="card glossary-message" role="alert">{error} {!term && <button className="btn btn-secondary" onClick={() => setRetry(value => value + 1)}>다시 시도</button>}</div>}
        {term && (
          <>
            <div className="glossary-detail-head">
              <div>
                <div className="glossary-detail-meta"><span className="badge badge-primary">{levelName[term.level] ?? term.level}</span><span>{term.categoryId}</span><span>버전 {term.contentVersion}</span></div>
                <h1>{term.term}</h1>
                {term.english && <p className="glossary-english">{term.english}{term.fullName && term.fullName !== term.english ? ` · ${term.fullName}` : ""}</p>}
              </div>
              {isAuthenticated ? <button className="btn btn-secondary" onClick={toggleBookmark} disabled={saving} aria-pressed={bookmarked}>{bookmarked ? "★ 저장됨" : "☆ 저장하기"}</button> : <Link className="btn btn-secondary" to="/login">로그인하고 저장하기</Link>}
            </div>
            <div className="glossary-detail-layout">
              <article className="card glossary-article">
                <section><h2>한 줄 정의</h2><p className="glossary-definition">{term.definition}</p></section>
                <section><h2>이렇게 이해하세요</h2><p>{term.explanation}</p></section>
                {term.formula && <section><h2>계산식</h2><pre className="glossary-formula">{term.formula}</pre></section>}
                {term.example && <section><h2>예시</h2><p>{term.example}</p></section>}
                {term.caution && <section className="glossary-caution"><h2>주의할 점</h2><p>{term.caution}</p></section>}
              </article>
              <aside className="glossary-detail-aside">
                <div className="card">
                  <h2>이어 읽기</h2>
                  {term.related.length > 0 ? <div className="glossary-related">{term.related.map(related => <Link key={related} to={`/glossary/${related}`}>{related} →</Link>)}</div> : <p className="subtle">연결된 용어가 없습니다.</p>}
                  {term.conceptSlug && <Link className="glossary-concept-link" to={`/learn?concept=${encodeURIComponent(term.conceptSlug)}`}>관련 학습 개념 보기 →</Link>}
                </div>
                <div className="card">
                  <h2>출처</h2>
                  <a href={term.sourceUrl} target="_blank" rel="noopener noreferrer">{term.sourceName} ↗</a>
                  <p className="subtle">{term.notice}</p>
                </div>
              </aside>
            </div>
          </>
        )}
      </main>
    </div>
  );
}
