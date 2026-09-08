import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Header from "../components/Header";
import { api } from "../api/client";
import type { CategoryRecommendationResponse, ProductResponse } from "../api/types";

const GRADE_DOT_COLORS: Record<number, string> = {
  1: "#E5484D",
  2: "#ED7D4C",
  3: "#F0A93B",
  4: "#A7C43D",
  5: "#4FB36B",
  6: "#2D8F6B",
};

export default function ExplorePage() {
  const { assessmentId } = useParams<{ assessmentId: string }>();
  const navigate = useNavigate();
  const [recommendations, setRecommendations] = useState<CategoryRecommendationResponse[]>([]);
  const [selectedCategoryId, setSelectedCategoryId] = useState<string | null>(null);
  const [products, setProducts] = useState<ProductResponse[] | null>(null);
  const [profileLabel, setProfileLabel] = useState<string>("");

  useEffect(() => {
    if (!assessmentId) return;
    api.getAssessment(assessmentId).then((res) => setProfileLabel(res.profile?.profileLabel ?? ""));
    api.getRecommendations(assessmentId).then((recs) => {
      setRecommendations(recs);
      if (recs.length > 0) setSelectedCategoryId(recs[0].categoryId);
    });
  }, [assessmentId]);

  useEffect(() => {
    if (!selectedCategoryId) return;
    setProducts(null);
    api.getProducts(selectedCategoryId).then(setProducts);
  }, [selectedCategoryId]);

  const selected = recommendations.find((r) => r.categoryId === selectedCategoryId);

  return (
    <div style={{ minHeight: "100vh", background: "var(--bg)", display: "flex", flexDirection: "column" }}>
      <Header />

      <div style={{ padding: "22px 40px 0", display: "flex", flexDirection: "column", gap: 16 }}>
        <div style={{ fontSize: 12.5, color: "var(--text-faint)" }}>
          분석 결과 &nbsp;›&nbsp; <span style={{ color: "var(--text)", fontWeight: 600 }}>탐색 카테고리</span>
        </div>
        <div style={{ fontWeight: 700, fontSize: 19 }}>{profileLabel} 고객님께 안내되는 탐색 카테고리</div>

        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          {recommendations.map((rec) => {
            const active = rec.categoryId === selectedCategoryId;
            return (
              <button
                key={rec.categoryId}
                type="button"
                aria-pressed={active}
                onClick={() => setSelectedCategoryId(rec.categoryId)}
                style={{
                  padding: "10px 18px",
                  borderRadius: 999,
                  border: `1.5px solid ${active ? "var(--primary)" : "var(--border)"}`,
                  background: active ? "var(--primary-tint)" : "#fff",
                  color: active ? "var(--primary)" : "var(--text-muted)",
                  fontWeight: active ? 700 : 400,
                  fontSize: 13.5,
                  display: "flex",
                  alignItems: "center",
                  gap: 8,
                  cursor: "pointer",
                  whiteSpace: "nowrap",
                }}
              >
                <span style={{ width: 8, height: 8, borderRadius: "50%", background: GRADE_DOT_COLORS[rec.riskGrade] ?? "#999" }} />
                {rec.categoryName} · {rec.riskGrade}등급
              </button>
            );
          })}
        </div>

        {selected && (
          <div
            style={{
              fontSize: 12.5,
              color: "var(--text-muted)",
              background: "#fff",
              border: "1px solid var(--border)",
              borderRadius: 10,
              padding: "12px 16px",
              lineHeight: 1.6,
            }}
          >
            {selected.reason}
          </div>
        )}
      </div>

      <div style={{ flex: 1, padding: "20px 40px", display: "flex", flexDirection: "column", gap: 14 }}>
        {products === null ? (
          <div style={{ textAlign: "center", color: "var(--text-muted)", fontSize: 14 }}>불러오는 중...</div>
        ) : products.length === 0 ? (
          <EmptyProducts />
        ) : (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(2, minmax(0,1fr))", gap: 16 }}>
            {products.map((p) => (
              <div key={p.id} className="card" style={{ padding: 22, display: "flex", flexDirection: "column", gap: 10 }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                  <div style={{ fontWeight: 700, fontSize: 15, lineHeight: 1.4 }}>{p.name}</div>
                  <span className="badge badge-warn" style={{ flexShrink: 0 }}>
                    {p.riskGrade}등급
                  </span>
                </div>
                <div style={{ fontSize: 12.5, color: "var(--text-faint)" }}>예상 투자 기간 {p.expectedPeriod}</div>
                <div style={{ fontSize: 13, color: "var(--text-muted)", lineHeight: 1.6 }}>{p.description}</div>
                <a
                  href="#"
                  style={{ fontSize: 13, fontWeight: 700, marginTop: 4 }}
                  onClick={(e) => {
                    e.preventDefault();
                    navigate(`/product/${p.id}`);
                  }}
                >
                  상세보기 →
                </a>
              </div>
            ))}
          </div>
        )}
      </div>

      <div
        style={{
          padding: "14px 40px",
          background: "#fff",
          borderTop: "1px solid var(--border)",
          fontSize: 11.5,
          color: "var(--text-faint)",
          textAlign: "center",
        }}
      >
        본 정보는 투자 참고용이며 투자 자문이나 투자 권유가 아닙니다.
      </div>
    </div>
  );
}

function EmptyProducts() {
  return (
    <div style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center" }}>
      <div style={{ width: 400, display: "flex", flexDirection: "column", alignItems: "center", textAlign: "center", gap: 16 }}>
        <div
          style={{
            width: 64,
            height: 64,
            borderRadius: 16,
            background: "#eff1f6",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
            <rect x="3" y="8" width="18" height="12" rx="1.6" stroke="#8B95B3" strokeWidth="1.8" />
            <path d="M3 8l3-4h12l3 4" stroke="#8B95B3" strokeWidth="1.8" strokeLinejoin="round" />
            <line x1="9" y1="12" x2="15" y2="12" stroke="#8B95B3" strokeWidth="1.8" strokeLinecap="round" />
          </svg>
        </div>
        <div style={{ fontWeight: 700, fontSize: 16 }}>조건에 맞는 상품이 없습니다</div>
        <div style={{ fontSize: 13, color: "var(--text-muted)", lineHeight: 1.6 }}>
          이 카테고리에는 현재 안내 가능한 예시 상품이 없습니다.
          <br />
          다른 카테고리를 확인해보세요.
        </div>
      </div>
    </div>
  );
}
