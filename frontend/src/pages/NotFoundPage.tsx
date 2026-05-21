import { Link } from "react-router-dom";

export default function NotFoundPage() {
  return (
    <div style={{ textAlign: "center", padding: "80px 20px" }}>
      <div style={{ fontSize: 56, fontWeight: 700, color: "var(--text-tertiary)" }}>404</div>
      <p style={{ color: "var(--text-secondary)", marginTop: 8 }}>
        존재하지 않는 페이지입니다.
      </p>
      <Link
        to="/"
        style={{
          display: "inline-block",
          marginTop: 20,
          padding: "10px 20px",
          background: "var(--color-primary)",
          color: "#fff",
          borderRadius: "var(--radius-sm)",
          fontSize: 13,
          fontWeight: 600,
        }}
      >
        대시보드로 돌아가기
      </Link>
    </div>
  );
}
