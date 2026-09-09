import type { AvailabilityResponse } from "../api/types";
import { Link } from "react-router-dom";

export function koreanDate(value: string) {
  return new Intl.DateTimeFormat("ko-KR", { timeZone: "Asia/Seoul", year: "numeric", month: "long", day: "numeric", hour: "2-digit", minute: "2-digit" }).format(new Date(value));
}

export default function AssessmentAvailability({ value }: { value: AvailabilityResponse }) {
  return <section className="policy-card" aria-label="진단 가능 일정">
    <strong>{value.canSubmit ? "이번 달 진단을 진행할 수 있어요" : value.currentStatus === "PROCESSING" ? "제출한 진단을 처리하고 있어요" : "이번 달 진단을 완료했어요"}</strong>
    <p>FinMATE는 한국 시간 기준 매월 1회 진단합니다. 금융사마다 재진단 정책은 다를 수 있습니다.</p>
    {value.nextAvailableAt && <p>다음 진단: <strong>{koreanDate(value.nextAvailableAt)} (한국 시간)</strong></p>}
    <div className="action-row">
      {value.currentAssessmentId && value.currentStatus === "COMPLETED" && <Link to={`/result/${value.currentAssessmentId}`}>이번 달 결과 보기</Link>}
      <Link to="/learn">기다리는 동안 상품 기초 배우기 →</Link>
    </div>
  </section>;
}
