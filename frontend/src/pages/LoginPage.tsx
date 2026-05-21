import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { AxiosError } from "axios";
import { login } from "../api/auth";
import { useAuthStore } from "../store/authStore";

interface LocationState { from?: string }

interface ApiError { code?: string; message?: string }

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const setAuth = useAuthStore((s) => s.setAuth);
  const from = (location.state as LocationState | null)?.from ?? "/";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const { accessToken, user } = await login({ email, password });
      setAuth({ accessToken, user });
      navigate(from, { replace: true });
    } catch (err) {
      const ax = err as AxiosError<ApiError>;
      setError(ax.response?.data?.message ?? "로그인에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={styles.shell}>
      <form onSubmit={onSubmit} style={styles.card}>
        <div style={styles.brand}>
          <span style={styles.brandDot} />
          fintech-simulator
        </div>
        <h1 style={styles.title}>로그인</h1>

        <label style={styles.label}>
          이메일
          <input
            type="email"
            required
            autoFocus
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            style={styles.input}
          />
        </label>

        <label style={styles.label}>
          비밀번호
          <input
            type="password"
            required
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            style={styles.input}
          />
        </label>

        {error && <div style={styles.error}>{error}</div>}

        <button type="submit" disabled={loading} style={styles.primaryBtn}>
          {loading ? "로그인 중..." : "로그인"}
        </button>

        <div style={styles.helper}>
          계정이 없으신가요? <Link to="/signup" style={styles.link}>회원가입</Link>
        </div>
      </form>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  shell: {
    minHeight: "100vh",
    display: "grid",
    placeItems: "center",
    padding: 20,
  },
  card: {
    width: 360,
    maxWidth: "100%",
    background: "var(--bg-panel)",
    border: "1px solid var(--border-subtle)",
    borderRadius: "var(--radius-lg)",
    boxShadow: "var(--shadow-md)",
    padding: 32,
    display: "flex",
    flexDirection: "column",
    gap: 16,
  },
  brand: { display: "flex", alignItems: "center", gap: 8, fontWeight: 700, fontSize: 14 },
  brandDot: { width: 8, height: 8, borderRadius: 999, background: "var(--color-up)" },
  title: { margin: "8px 0 0", fontSize: 22, fontWeight: 700 },
  label: { display: "flex", flexDirection: "column", gap: 6, fontSize: 12, color: "var(--text-secondary)" },
  input: {
    height: 40,
    padding: "0 12px",
    fontSize: 14,
    background: "var(--bg-base)",
    color: "var(--text-primary)",
    border: "1px solid var(--border-subtle)",
    borderRadius: "var(--radius-sm)",
    outline: "none",
  },
  error: {
    fontSize: 13,
    color: "var(--color-danger)",
    background: "var(--color-up-bg)",
    padding: "8px 12px",
    borderRadius: "var(--radius-sm)",
  },
  primaryBtn: {
    height: 44,
    background: "var(--color-primary)",
    color: "#fff",
    border: "none",
    borderRadius: "var(--radius-sm)",
    fontSize: 14,
    fontWeight: 600,
  },
  helper: { fontSize: 13, color: "var(--text-secondary)", textAlign: "center" },
  link: { color: "var(--color-primary)", fontWeight: 600 },
};
