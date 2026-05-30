import { Link } from "react-router-dom";
import type { ReactNode } from "react";

interface Props {
  icon?: string;
  title: string;
  desc?: ReactNode;
  /** 내부 라우트 이동 CTA */
  to?: string;
  ctaLabel?: string;
  /** 버튼형 CTA (onClick) — to 대신 사용 */
  onAction?: () => void;
}

/**
 * 공용 빈 상태 — 아이콘 + 제목 + 설명 + (선택) CTA.
 * 표 내부에서는 td colSpan 안에 넣어 사용한다.
 */
export function EmptyState({ icon = "📭", title, desc, to, ctaLabel, onAction }: Props) {
  return (
    <div className="empty-state">
      <div className="empty-ico">{icon}</div>
      <div className="empty-title">{title}</div>
      {desc && <div className="empty-desc">{desc}</div>}
      {to && ctaLabel && (
        <Link to={to} className="btn btn-primary" style={{ marginTop: 4 }}>
          {ctaLabel}
        </Link>
      )}
      {onAction && ctaLabel && (
        <button className="btn btn-primary" style={{ marginTop: 4 }} onClick={onAction}>
          {ctaLabel}
        </button>
      )}
    </div>
  );
}
