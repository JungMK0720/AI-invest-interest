import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Header from "../components/Header";
import { api } from "../api/client";
import type { ProductResponse } from "../api/types";

export default function ProductDetailPage() {
  const { productId } = useParams<{ productId: string }>();
  const navigate = useNavigate();
  const [product, setProduct] = useState<ProductResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!productId) return;
    api
      .getProduct(productId)
      .then(setProduct)
      .catch(() => setError("상품 정보를 찾을 수 없습니다."));
  }, [productId]);

  return (
    <div style={{ minHeight: "100vh", background: "var(--bg)", display: "flex", flexDirection: "column" }}>
      <Header />

      <div style={{ padding: "22px 40px 0" }}>
        <div style={{ fontSize: 12.5, color: "var(--text-faint)" }}>
          분석 결과 &nbsp;›&nbsp; 탐색 카테고리 &nbsp;›&nbsp; <span style={{ color: "var(--text)", fontWeight: 600 }}>상품 상세</span>
        </div>
        <a
          href="#"
          style={{ display: "flex", alignItems: "center", gap: 6, fontSize: 13, fontWeight: 600, marginTop: 14, width: "fit-content" }}
          onClick={(e) => {
            e.preventDefault();
            navigate(-1);
          }}
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
            <path d="M15 6l-6 6 6 6" stroke="#2F55E8" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
          목록으로
        </a>
      </div>

      <div style={{ flex: 1, display: "flex", justifyContent: "center", padding: "22px 40px 34px" }}>
        {error ? (
          <div style={{ color: "var(--danger)", fontSize: 14 }}>{error}</div>
        ) : !product ? (
          <div style={{ color: "var(--text-muted)", fontSize: 14 }}>불러오는 중...</div>
        ) : (
          <div style={{ width: 640, display: "flex", flexDirection: "column", gap: 20 }}>
            <div className="card" style={{ padding: 28, display: "flex", flexDirection: "column", gap: 16 }}>
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <span className="badge badge-primary">{product.categoryName}</span>
                <span className="badge badge-warn">위험등급 {product.riskGrade}등급</span>
              </div>
              <div style={{ fontWeight: 700, fontSize: 22, lineHeight: 1.4 }}>{product.name}</div>
              <div style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13, color: "var(--text-muted)" }}>
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="9" stroke="#5C6685" strokeWidth="1.7" />
                  <path d="M12 7v5l3.2 2" stroke="#5C6685" strokeWidth="1.7" strokeLinecap="round" />
                </svg>
                예상 투자 기간: {product.expectedPeriod}
              </div>
            </div>

            <div className="card" style={{ padding: 28, display: "flex", flexDirection: "column", gap: 10 }}>
              <div style={{ fontWeight: 700, fontSize: 14.5 }}>상품 설명</div>
              <div style={{ fontSize: 13.5, color: "var(--text-muted)", lineHeight: 1.7 }}>{product.description}</div>
            </div>

            <div
              style={{
                background: "var(--warn-bg)",
                border: "1.5px solid var(--warn-border)",
                borderRadius: 14,
                padding: "20px 22px",
                display: "flex",
                gap: 12,
                alignItems: "flex-start",
              }}
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" style={{ flexShrink: 0, marginTop: 1 }}>
                <path d="M12 3l10 18H2L12 3z" stroke="#8A5A00" strokeWidth="1.8" strokeLinejoin="round" />
                <line x1="12" y1="10" x2="12" y2="14" stroke="#8A5A00" strokeWidth="1.8" strokeLinecap="round" />
                <circle cx="12" cy="17" r="1" fill="#8A5A00" />
              </svg>
              <div style={{ fontSize: 12.5, color: "var(--warn-text)", lineHeight: 1.7 }}>{product.disclaimer}</div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
