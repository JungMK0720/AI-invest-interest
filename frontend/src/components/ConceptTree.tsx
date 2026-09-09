import { useMemo } from "react";
import type { LearningTreeNode } from "../api/types";

/**
 * 좌측 개념 트리.
 *
 * 두 가지 방식으로 하위를 제어한다.
 * 1) 노드별 캐럿: 그 가지만 펼치고 접는다.
 * 2) 상단 토글: 3단계 세부 개념을 트리 전체에서 보이거나 감춘다.
 *
 * 검색어가 있으면 일치하는 노드와 그 조상만 남기고, 이때는 접힘 상태를 무시해 전부 펼친다.
 * 검색 결과가 접혀 있어 보이지 않는 것이 가장 흔한 트리 UI 실패이기 때문이다.
 */
export default function ConceptTree({
  roots,
  selected,
  expanded,
  showDeep,
  query,
  onSelect,
  onToggle,
}: {
  roots: LearningTreeNode[];
  selected: string;
  expanded: Set<string>;
  showDeep: boolean;
  query: string;
  onSelect: (slug: string) => void;
  onToggle: (slug: string) => void;
}) {
  const keyword = query.trim().toLowerCase();

  // 검색어에 걸리는 노드의 조상까지 모아 둔다. 트리를 그릴 때 이 집합으로 가지치기한다.
  const visible = useMemo(() => {
    if (!keyword) return null;
    const keep = new Set<string>();
    const walk = (node: LearningTreeNode, ancestors: string[]): boolean => {
      const selfHit =
        node.title.toLowerCase().includes(keyword) || node.summary.toLowerCase().includes(keyword);
      const childHit = node.children.map(child => walk(child, [...ancestors, node.slug])).some(Boolean);
      if (selfHit || childHit) {
        keep.add(node.slug);
        ancestors.forEach(slug => keep.add(slug));
        return true;
      }
      return false;
    };
    roots.forEach(root => walk(root, []));
    return keep;
  }, [roots, keyword]);

  const empty = visible !== null && visible.size === 0;

  return (
    <nav className="concept-tree" aria-label="학습 개념 목록">
      {empty && <p className="tree-empty">‘{query}’와 일치하는 개념이 없습니다.</p>}
      <ul className="tree-list">
        {roots.map(root => (
          <TreeBranch
            key={root.slug}
            node={root}
            selected={selected}
            expanded={expanded}
            showDeep={showDeep}
            visible={visible}
            searching={Boolean(keyword)}
            onSelect={onSelect}
            onToggle={onToggle}
          />
        ))}
      </ul>
    </nav>
  );
}

function TreeBranch({
  node,
  selected,
  expanded,
  showDeep,
  visible,
  searching,
  onSelect,
  onToggle,
}: {
  node: LearningTreeNode;
  selected: string;
  expanded: Set<string>;
  showDeep: boolean;
  visible: Set<string> | null;
  searching: boolean;
  onSelect: (slug: string) => void;
  onToggle: (slug: string) => void;
}) {
  if (visible && !visible.has(node.slug)) return null;

  // 3단계는 상단 토글이 켜져 있을 때만 노출한다. 검색 중에는 토글과 무관하게 보여준다.
  const children = node.children.filter(child => showDeep || searching || child.level <= 2);
  const hasChildren = children.length > 0;
  const open = searching || expanded.has(node.slug);
  const isSelected = node.slug === selected;

  return (
    <li className={`tree-item level-${node.level}`}>
      <div className={`tree-row${isSelected ? " is-selected" : ""}`}>
        {hasChildren ? (
          <button
            type="button"
            className="tree-caret"
            aria-expanded={open}
            aria-label={`${node.title} 하위 개념 ${open ? "접기" : "펼치기"}`}
            onClick={() => onToggle(node.slug)}
          >
            <span aria-hidden="true">{open ? "▾" : "▸"}</span>
          </button>
        ) : (
          <span className="tree-caret tree-caret-empty" aria-hidden="true" />
        )}
        <button
          type="button"
          className="tree-label"
          aria-current={isSelected ? "true" : undefined}
          onClick={() => onSelect(node.slug)}
        >
          <span className="tree-title">{node.title}</span>
          <span className="tree-meta">
            {node.descendantCount > 0 && <span className="tree-count">{node.descendantCount}</span>}
            {node.hasSimulation && (
              <span className="tree-dot" title="모의투자 있음" aria-label="모의투자 있음">
                ◈
              </span>
            )}
          </span>
        </button>
      </div>
      {hasChildren && open && (
        <ul className="tree-list">
          {children.map(child => (
            <TreeBranch
              key={child.slug}
              node={child}
              selected={selected}
              expanded={expanded}
              showDeep={showDeep}
              visible={visible}
              searching={searching}
              onSelect={onSelect}
              onToggle={onToggle}
            />
          ))}
        </ul>
      )}
    </li>
  );
}
