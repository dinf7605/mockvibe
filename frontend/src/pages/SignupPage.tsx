import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { AxiosError } from "axios";
import { login, signup } from "../api/auth";
import { useAuthStore } from "../store/authStore";

interface ApiFieldError { field: string; message: string }
interface ApiError { code?: string; message?: string; fields?: ApiFieldError[] }

export default function SignupPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);

  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setFieldErrors({});
    setLoading(true);
    try {
      await signup({ username, email, password });
      // 가입 직후 자동 로그인
      const { accessToken, user } = await login({ email, password });
      setAuth({ accessToken, user });
      navigate("/dashboard", { replace: true });
    } catch (err) {
      const ax = err as AxiosError<ApiError>;
      const data = ax.response?.data;
      if (data?.fields && data.fields.length > 0) {
        const fe: Record<string, string> = {};
        data.fields.forEach((f) => { fe[f.field] = f.message; });
        setFieldErrors(fe);
        setError(data.message ?? "입력 값을 확인하세요.");
      } else {
        setError(data?.message ?? "회원가입에 실패했습니다.");
      }
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
        <h1 style={styles.title}>회원가입</h1>
        <p style={styles.subtitle}>가입 시 시드머니 <strong>1,000만원</strong>이 자동 지급됩니다.</p>

        <label style={styles.label}>
          사용자명
          <input
            required minLength={2} maxLength={50} autoFocus
            value={username} onChange={(e) => setUsername(e.target.value)}
            style={styles.input}
          />
          {fieldErrors.username && <span style={styles.fieldErr}>{fieldErrors.username}</span>}
        </label>

        <label style={styles.label}>
          이메일
          <input
            type="email" required autoComplete="email"
            value={email} onChange={(e) => setEmail(e.target.value)}
            style={styles.input}
          />
          {fieldErrors.email && <span style={styles.fieldErr}>{fieldErrors.email}</span>}
        </label>

        <label style={styles.label}>
          비밀번호 (영문+숫자 8자 이상)
          <input
            type="password" required minLength={8} autoComplete="new-password"
            value={password} onChange={(e) => setPassword(e.target.value)}
            style={styles.input}
          />
          {fieldErrors.password && <span style={styles.fieldErr}>{fieldErrors.password}</span>}
        </label>

        {error && <div style={styles.error}>{error}</div>}

        <button type="submit" disabled={loading} style={styles.primaryBtn}>
          {loading ? "처리 중..." : "가입하고 시작하기"}
        </button>

        <div style={styles.helper}>
          이미 계정이 있으신가요? <Link to="/login" style={styles.link}>로그인</Link>
        </div>
      </form>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  shell: { minHeight: "100vh", display: "grid", placeItems: "center", padding: 20 },
  card: {
    width: 380, maxWidth: "100%",
    background: "var(--bg-panel)",
    border: "1px solid var(--border-subtle)",
    borderRadius: "var(--radius-lg)",
    boxShadow: "var(--shadow-md)",
    padding: 32, display: "flex", flexDirection: "column", gap: 14,
  },
  brand: { display: "flex", alignItems: "center", gap: 8, fontWeight: 700, fontSize: 14 },
  brandDot: { width: 8, height: 8, borderRadius: 999, background: "var(--color-up)" },
  title: { margin: "8px 0 0", fontSize: 22, fontWeight: 700 },
  subtitle: { margin: 0, fontSize: 13, color: "var(--text-secondary)" },
  label: { display: "flex", flexDirection: "column", gap: 6, fontSize: 12, color: "var(--text-secondary)" },
  input: {
    height: 40, padding: "0 12px", fontSize: 14,
    background: "var(--bg-base)", color: "var(--text-primary)",
    border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)", outline: "none",
  },
  fieldErr: { fontSize: 12, color: "var(--color-danger)" },
  error: {
    fontSize: 13, color: "var(--color-danger)",
    background: "var(--color-up-bg)", padding: "8px 12px", borderRadius: "var(--radius-sm)",
  },
  primaryBtn: {
    height: 44, background: "var(--color-primary)", color: "#fff",
    border: "none", borderRadius: "var(--radius-sm)", fontSize: 14, fontWeight: 600,
  },
  helper: { fontSize: 13, color: "var(--text-secondary)", textAlign: "center" },
  link: { color: "var(--color-primary)", fontWeight: 600 },
};
