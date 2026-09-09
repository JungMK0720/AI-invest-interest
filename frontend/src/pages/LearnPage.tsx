import { useCallback, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import Header from "../components/Header";
import ConceptTree from "../components/ConceptTree";
import ConceptDiagram from "../components/ConceptDiagram";
import QuizPanel from "../components/QuizPanel";
import SimulatorPanel from "../components/SimulatorPanel";
import { api } from "../api/client";
import type { LearningConceptDetail, LearningTreeNode, LearningTreeResponse } from "../api/types";

type Tab = "concept" | "quiz" | "sim";

const START_SLUG = "risk-return";
const DEEP_KEY = "finmate.learn.showDeep";

/**
 * 상품 학습 화면.
 *
 * 좌측은 개념 트리, 우측은 선택한 개념의 본문이다. 하위 개념은 두 단계로 제어한다.
 * 상단 토글로 세부 개념(3단계) 전체를 켜고 끄고, 노드별 캐럿으로 가지 하나씩 펼친다.
 * 사용자가 트리 깊이를 통제할 수 있어야 59개 개념이 목록이 아니라 지도처럼 읽힌다.
 *
 * 진단 등급을 바꾸거나 상품 가입을 유도하지 않는다. 이 화면은 재진단 제한과 무관하게 열려 있다(docs/09).
 */
export default function LearnPage() {
  const [tree, setTree] = useState<LearningTreeResponse | null>(null);
  const [treeError, setTreeError] = useState(false);
  const [slug, setSlug] = useState(START_SLUG);
  const [detail, setDetail] = useState<LearningConceptDetail | null>(null);
  const [detailError, setDetailError] = useState(false);
  const [tab, setTab] = useState<Tab>("concept");
  const [query, setQuery] = useState("");
  const [expanded, setExpanded] = useState<Set<string>>(new Set());
  const [showDeep, setShowDeep] = useState(() => {
    try {
      return localStorage.getItem(DEEP_KEY) !== "false";
    } catch {
      return true;
    }
  });
  const [navOpen, setNavOpen] = useState(false);
  const [detailRetry, setDetailRetry] = useState(0);

  const loadTree = useCallback(() => {
    setTreeError(false);
    api
      .getLearningTree()
      .then(data => {
        setTree(data);
        // 첫 진입에서 선택된 개념까지의 경로는 펼쳐 둔다. 어디에 있는지 보이지 않으면 트리가 소용없다.
        setExpanded(new Set(data.roots.map(root => root.slug)));
      })
      .catch(() => setTreeError(true));
  }, []);

  useEffect(loadTree, [loadTree]);

  useEffect(() => {
    let alive = true;
    setDetail(null);
    setDetailError(false);
    api
      .getLearningConcept(slug)
      .then(data => {
        if (!alive) return;
        setDetail(data);
        // 선택한 개념의 조상을 펼쳐 트리에서 현재 위치가 보이게 한다.
        setExpanded(prev => {
          const next = new Set(prev);
          data.breadcrumb.forEach(crumb => next.add(crumb.slug));
          return next;
        });
      })
      .catch(() => alive && setDetailError(true));
    return () => {
      alive = false;
    };
  }, [slug, detailRetry]);

  useEffect(() => {
    try {
      localStorage.setItem(DEEP_KEY, String(showDeep));
    } catch {
      // 저장에 실패해도 학습 자체에는 영향이 없으므로 조용히 넘어간다.
    }
  }, [showDeep]);

  const titleBySlug = useMemo(() => {
    const map = new Map<string, string>();
    const walk = (node: LearningTreeNode) => {
      map.set(node.slug, node.title);
      node.children.forEach(walk);
    };
    tree?.roots.forEach(walk);
    return map;
  }, [tree]);

  function select(next: string) {
    setSlug(next);
    setTab("concept");
    setNavOpen(false);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function toggle(target: string) {
    setExpanded(prev => {
      const next = new Set(prev);
      if (next.has(target)) next.delete(target);
      else next.add(target);
      return next;
    });
  }

  return (
    <div className="app-page">
      <Header />
      <main className="learn-main">
        <section className="learn-intro">
          <span className="eyebrow">상품 기초 학습 · 출처 연결 콘텐츠 · 모의투자</span>
          <h1>상품을 보기 전에, 구조를 읽는 법부터.</h1>
          <p className="intro-copy">
            진단 등급을 바꾸거나 가입을 권유하는 페이지가 아닙니다. 개념을 고르면 구조·확인 포인트·확인 문제·모의투자가
            함께 열립니다. 왼쪽 목록에서 하위 개념을 펼쳐 원하는 깊이까지 내려갈 수 있습니다.
          </p>
          {tree && (
            <p className="subtle">
              개념 {tree.conceptCount}개 · 콘텐츠 버전 {tree.contentVersion}
            </p>
          )}
        </section>

        <div className="learn-layout">
          <aside className={`learn-aside${navOpen ? " is-open" : ""}`}>
            <div className="aside-controls">
              <label className="field-label" htmlFor="concept-search">
                개념 찾기
              </label>
              <input
                id="concept-search"
                className="field-input"
                type="search"
                placeholder="예: 듀레이션, 스프레드, 레버리지"
                value={query}
                onChange={event => setQuery(event.target.value)}
              />
              <button
                type="button"
                className={`deep-toggle${showDeep ? " is-on" : ""}`}
                role="switch"
                aria-checked={showDeep}
                onClick={() => setShowDeep(value => !value)}
              >
                <span className="deep-track" aria-hidden="true">
                  <span className="deep-thumb" />
                </span>
                <span>세부 개념까지 보기</span>
              </button>
              <p className="aside-hint">
                {showDeep ? "3단계 세부 개념까지 표시합니다." : "대분류와 핵심 개념만 표시합니다."}
              </p>
            </div>

            {treeError ? (
              <div role="alert" className="panel-error">
                개념 목록을 불러오지 못했습니다.{" "}
                <button className="btn btn-secondary" onClick={loadTree}>
                  다시 시도
                </button>
              </div>
            ) : !tree ? (
              <p role="status">개념 목록을 불러오는 중…</p>
            ) : (
              <ConceptTree
                roots={tree.roots}
                selected={slug}
                expanded={expanded}
                showDeep={showDeep}
                query={query}
                onSelect={select}
                onToggle={toggle}
              />
            )}
          </aside>

          <div className="learn-content">
            <button className="btn btn-secondary nav-toggle" onClick={() => setNavOpen(open => !open)}>
              {navOpen ? "개념 목록 닫기" : "개념 목록 열기"}
            </button>

            {detailError && (
              <div role="alert" className="panel-error">
                개념을 불러오지 못했습니다.{" "}
                <button className="btn btn-secondary" onClick={() => setDetailRetry(count => count + 1)}>
                  다시 시도
                </button>
              </div>
            )}
            {!detail && !detailError && <p role="status">개념을 불러오는 중…</p>}

            {detail && (
              <>
                <nav className="breadcrumb" aria-label="현재 위치">
                  {detail.breadcrumb.map((crumb, index) => (
                    <span key={crumb.slug}>
                      {index > 0 && <span aria-hidden="true"> › </span>}
                      {crumb.slug === detail.slug ? (
                        <strong>{crumb.title}</strong>
                      ) : (
                        <button className="crumb-link" onClick={() => select(crumb.slug)}>
                          {crumb.title}
                        </button>
                      )}
                    </span>
                  ))}
                </nav>

                <header className="concept-head">
                  <h2>{detail.title}</h2>
                  <p className="concept-oneliner">{detail.oneLiner}</p>
                </header>

                <div className="tab-row" role="tablist" aria-label="학습 단계">
                  <TabButton current={tab} value="concept" onSelect={setTab}>
                    개념과 구조
                  </TabButton>
                  <TabButton current={tab} value="quiz" onSelect={setTab}>
                    확인 문제{detail.quizCount > 0 ? ` ${detail.quizCount}` : ""}
                  </TabButton>
                  <TabButton current={tab} value="sim" onSelect={setTab}>
                    모의투자
                  </TabButton>
                </div>

                {tab === "concept" && (
                  <article className="card concept-card">
                    <ConceptDiagram kind={detail.diagramKey} />
                    {detail.explanation.map(paragraph => (
                      <p key={paragraph}>{paragraph}</p>
                    ))}

                    {detail.keyPoints.length > 0 && (
                      <section className="point-block">
                        <h3>확인 포인트</h3>
                        <ul className="point-list">
                          {detail.keyPoints.map(point => (
                            <li key={point}>{point}</li>
                          ))}
                        </ul>
                      </section>
                    )}

                    {detail.misconception && (
                      <p className="misconception">
                        <strong>자주 하는 오해</strong> {detail.misconception}
                      </p>
                    )}

                    {detail.checklist.length > 0 && (
                      <section className="point-block">
                        <h3>이 상품에서 확인할 것</h3>
                        <ul className="check-list">
                          {detail.checklist.map(item => (
                            <li key={item}>{item}</li>
                          ))}
                        </ul>
                      </section>
                    )}

                    {detail.children.length > 0 && (
                      <section className="point-block">
                        <h3>하위 개념 {detail.children.length}개</h3>
                        <div className="child-grid">
                          {detail.children.map(child => (
                            <button className="child-card" key={child.slug} onClick={() => select(child.slug)}>
                              <strong>{child.title}</strong>
                              <span>{child.summary}</span>
                            </button>
                          ))}
                        </div>
                      </section>
                    )}

                    {detail.relatedSlugs.length > 0 && (
                      <section className="point-block">
                        <h3>함께 보면 좋은 개념</h3>
                        <div className="related-row">
                          {detail.relatedSlugs.map(related => (
                            <button className="btn btn-secondary" key={related} onClick={() => select(related)}>
                              {titleBySlug.get(related) ?? related}
                            </button>
                          ))}
                        </div>
                      </section>
                    )}

                    <a className="lesson-source" href={detail.sourceUrl} target="_blank" rel="noreferrer">
                      원문 출처 열기 · {detail.sourceName} ↗
                    </a>
                    <p className="notice-line">{detail.notice}</p>
                  </article>
                )}

                {tab === "quiz" && (
                  <article className="card concept-card">
                    <QuizPanel slug={detail.slug} />
                  </article>
                )}

                {tab === "sim" && (
                  <article className="card concept-card">
                    <SimulatorPanel slug={detail.slug} />
                  </article>
                )}
              </>
            )}

            <div className="action-row">
              <Link className="btn btn-secondary" to="/start">
                진단 시작 화면
              </Link>
              <Link className="btn btn-secondary" to="/history">
                진단 이력 보기
              </Link>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

function TabButton({
  current,
  value,
  onSelect,
  children,
}: {
  current: Tab;
  value: Tab;
  onSelect: (tab: Tab) => void;
  children: ReactNode;
}) {
  return (
    <button
      role="tab"
      aria-selected={current === value}
      className={`tab-button${current === value ? " is-active" : ""}`}
      onClick={() => onSelect(value)}
    >
      {children}
    </button>
  );
}
