import { Link } from "react-router-dom";
import { useAuthStore } from "../store/authStore";
import { useThemeStore } from "../store/themeStore";

/**
 * 공개 랜딩 페이지.
 * - 비로그인: "무료로 시작하기"(회원가입) + "로그인"
 * - 로그인:   "대시보드로 가기" 단일 CTA
 *
 * 디자인 토큰(--color-*, --bg-*) 사용. 인라인 스타일로 외부 CSS 의존 없음.
 */
export default function LandingPage() {
  const user = useAuthStore((s) => s.user);
  const mode = useThemeStore((s) => s.mode);
  const toggleTheme = useThemeStore((s) => s.toggle);

  const features: Feature[] = [
    {
      emoji: "🤖",
      title: "AI 매매 코치",
      desc: "매매 직후 자동 코멘트와 주간 회고로 투자 습관을 점검합니다. Gemini 기반.",
    },
    {
      emoji: "📈",
      title: "백테스트 엔진",
      desc: "1년치 OHLC로 BuyAndHold / MA20 / RSI14 전략을 즉시 비교, 자산곡선까지.",
    },
    {
      emoji: "📊",
      title: "리스크 대시보드",
      desc: "VaR(Historical) · Sharpe · Beta · MDD · 집중도 경고를 한 화면에.",
    },
    {
      emoji: "⚡",
      title: "이벤트 기반 지정가",
      desc: "폴링 없는 효율적 체결. per-ticker 락 + 단일 트랜잭션 정합성.",
    },
  ];

  const stats: Stat[] = [
    { value: "32.7ms", label: "매수 API p95 응답" },
    { value: "21,361", label: "k6 부하 호출 (50 VU)" },
    { value: "0.02%", label: "실패율" },
    { value: "80/80", label: "단위 테스트 통과" },
  ];

  return (
    <div style={styles.shell}>
      {/* ====== HEADER ====== */}
      <header style={styles.header}>
        <div style={styles.brand}>
          <span style={styles.brandDot} />
          fintech-simulator
        </div>
        <div style={styles.headerRight}>
          <a
            href="https://github.com/dinf7605/mockvibe"
            target="_blank"
            rel="noreferrer"
            style={styles.ghostLink}
          >
            GitHub
          </a>
          <button
            onClick={toggleTheme}
            style={styles.ghostBtn}
            aria-label="테마 전환"
          >
            {mode === "dark" ? "☀️" : "🌙"}
          </button>
          {user ? (
            <Link to="/dashboard" style={styles.primaryBtn}>
              대시보드로 →
            </Link>
          ) : (
            <Link to="/login" style={styles.ghostBtn}>
              로그인
            </Link>
          )}
        </div>
      </header>

      {/* ====== HERO ====== */}
      <section style={styles.hero}>
        <div style={styles.heroBadge}>
          <span style={styles.heroBadgeDot} />
          한국·미국 주식 통합 · 실시간 시세 · 모의투자 전용
        </div>
        <h1 style={styles.heroTitle}>
          데이터로 <span style={styles.gradient}>전략을 검증</span>하는
          <br />
          실시간 모의투자 플랫폼
        </h1>
        <p style={styles.heroDesc}>
          KIS · Finnhub 실시간 시세를 STOMP로 다중 클라이언트에 동시 공급하고,
          <br />
          AI 코치 · 백테스트 · 리스크 분석으로 매매 결정을 정량화합니다.
        </p>

        <div style={styles.ctaRow}>
          {user ? (
            <Link to="/dashboard" style={styles.primaryCta}>
              대시보드로 가기 →
            </Link>
          ) : (
            <>
              <Link to="/signup" style={styles.primaryCta}>
                무료로 시작하기 →
              </Link>
              <Link to="/login" style={styles.secondaryCta}>
                로그인
              </Link>
            </>
          )}
        </div>

        <div style={styles.heroNote}>
          가입 즉시 1,000만원 시드머니 지급 · 신용카드 불필요 · 실거래 없음
        </div>
      </section>

      {/* ====== FEATURES ====== */}
      <section style={styles.section}>
        <div style={styles.sectionHead}>
          <div style={styles.eyebrow}>차별화 4종 세트</div>
          <h2 style={styles.h2}>단순 모의매매를 넘어, 의사결정을 돕는 도구</h2>
        </div>

        <div style={styles.featureGrid}>
          {features.map((f) => (
            <div key={f.title} style={styles.featureCard}>
              <div style={styles.featureEmoji}>{f.emoji}</div>
              <div style={styles.featureTitle}>{f.title}</div>
              <div style={styles.featureDesc}>{f.desc}</div>
            </div>
          ))}
        </div>
      </section>

      {/* ====== STATS ====== */}
      <section style={styles.statsSection}>
        <div style={styles.sectionHead}>
          <div style={styles.eyebrow}>검증된 안정성</div>
          <h2 style={styles.h2}>k6 50 VU 부하 테스트 통과</h2>
        </div>
        <div style={styles.statsGrid}>
          {stats.map((s) => (
            <div key={s.label} style={styles.statCard}>
              <div style={styles.statValue}>{s.value}</div>
              <div style={styles.statLabel}>{s.label}</div>
            </div>
          ))}
        </div>
      </section>

      {/* ====== TECH ====== */}
      <section style={styles.section}>
        <div style={styles.sectionHead}>
          <div style={styles.eyebrow}>기술 스택</div>
          <h2 style={styles.h2}>운영 환경에서 검증된 조합</h2>
        </div>
        <div style={styles.techRow}>
          {[
            "Spring Boot 3.5",
            "Java 17",
            "Oracle 21c",
            "Redis 7",
            "React 19",
            "TypeScript",
            "Vite",
            "STOMP WebSocket",
            "Resilience4j",
            "Flyway",
            "Docker",
            "Nginx + HTTPS",
            "GitHub Actions",
          ].map((t) => (
            <span key={t} style={styles.techChip}>
              {t}
            </span>
          ))}
        </div>
      </section>

      {/* ====== FINAL CTA ====== */}
      <section style={styles.finalCta}>
        <h2 style={styles.h2}>지금 바로 전략을 검증해보세요</h2>
        <p style={styles.finalDesc}>
          신용카드도, 실제 자금도 필요 없습니다. 1분 회원가입으로 1,000만원 시드머니를 받으세요.
        </p>
        <div style={styles.ctaRow}>
          {user ? (
            <Link to="/dashboard" style={styles.primaryCta}>
              대시보드로 가기 →
            </Link>
          ) : (
            <Link to="/signup" style={styles.primaryCta}>
              무료로 시작하기 →
            </Link>
          )}
        </div>
      </section>

      {/* ====== FOOTER ====== */}
      <footer style={styles.footer}>
        <div style={styles.footerInner}>
          <div style={styles.footerBrand}>
            <span style={styles.brandDot} />
            fintech-simulator
          </div>
          <div style={styles.footerLinks}>
            <a
              href="https://github.com/dinf7605/mockvibe"
              target="_blank"
              rel="noreferrer"
              style={styles.footerLink}
            >
              GitHub
            </a>
            <a
              href="https://github.com/dinf7605/mockvibe/blob/main/PRD.md"
              target="_blank"
              rel="noreferrer"
              style={styles.footerLink}
            >
              PRD
            </a>
            <a
              href="https://github.com/dinf7605/mockvibe/tree/main/docs/decisions"
              target="_blank"
              rel="noreferrer"
              style={styles.footerLink}
            >
              ADR
            </a>
          </div>
          <div style={styles.footerNote}>
            본 서비스는 모의투자 전용이며 실거래 기능을 제공하지 않습니다.
            <br />
            KIS · Finnhub · Gemini · ExchangeRate-API 사용은 각 제공자 약관에 따릅니다.
          </div>
        </div>
      </footer>
    </div>
  );
}

interface Feature {
  emoji: string;
  title: string;
  desc: string;
}

interface Stat {
  value: string;
  label: string;
}

const styles: Record<string, React.CSSProperties> = {
  shell: {
    minHeight: "100vh",
    background: "var(--bg-base)",
    color: "var(--text-primary)",
    display: "flex",
    flexDirection: "column",
  },

  /* Header */
  header: {
    height: 64,
    padding: "0 24px",
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    borderBottom: "1px solid var(--border-subtle)",
    background: "var(--bg-panel)",
    position: "sticky",
    top: 0,
    zIndex: 10,
  },
  brand: {
    fontWeight: 700,
    fontSize: 15,
    display: "flex",
    alignItems: "center",
    gap: 8,
  },
  brandDot: {
    width: 8,
    height: 8,
    borderRadius: 999,
    background: "var(--color-up)",
    display: "inline-block",
  },
  headerRight: {
    display: "flex",
    alignItems: "center",
    gap: 8,
  },
  ghostLink: {
    fontSize: 13,
    color: "var(--text-secondary)",
    padding: "8px 12px",
    textDecoration: "none",
  },
  ghostBtn: {
    height: 36,
    padding: "0 16px",
    fontSize: 13,
    fontWeight: 500,
    background: "transparent",
    color: "var(--text-primary)",
    border: "1px solid var(--border-subtle)",
    borderRadius: "var(--radius-sm)",
    textDecoration: "none",
    display: "inline-flex",
    alignItems: "center",
    cursor: "pointer",
  },
  primaryBtn: {
    height: 36,
    padding: "0 16px",
    fontSize: 13,
    fontWeight: 600,
    background: "var(--color-primary)",
    color: "#fff",
    border: "none",
    borderRadius: "var(--radius-sm)",
    textDecoration: "none",
    display: "inline-flex",
    alignItems: "center",
  },

  /* Hero */
  hero: {
    padding: "96px 24px 80px",
    maxWidth: 1080,
    margin: "0 auto",
    textAlign: "center",
  },
  heroBadge: {
    display: "inline-flex",
    alignItems: "center",
    gap: 8,
    padding: "6px 14px",
    fontSize: 12,
    color: "var(--text-secondary)",
    background: "var(--bg-elevated)",
    border: "1px solid var(--border-subtle)",
    borderRadius: 999,
    marginBottom: 24,
  },
  heroBadgeDot: {
    width: 6,
    height: 6,
    borderRadius: 999,
    background: "var(--color-up)",
  },
  heroTitle: {
    fontSize: 56,
    fontWeight: 800,
    lineHeight: 1.15,
    letterSpacing: "-0.02em",
    margin: "0 0 20px",
  },
  gradient: {
    background: "linear-gradient(135deg, var(--color-primary), var(--color-up))",
    WebkitBackgroundClip: "text",
    WebkitTextFillColor: "transparent",
    backgroundClip: "text",
  },
  heroDesc: {
    fontSize: 18,
    lineHeight: 1.6,
    color: "var(--text-secondary)",
    margin: "0 0 32px",
  },
  ctaRow: {
    display: "flex",
    gap: 12,
    justifyContent: "center",
    flexWrap: "wrap",
  },
  primaryCta: {
    height: 52,
    padding: "0 28px",
    fontSize: 15,
    fontWeight: 700,
    background: "var(--color-primary)",
    color: "#fff",
    border: "none",
    borderRadius: "var(--radius-md)",
    textDecoration: "none",
    display: "inline-flex",
    alignItems: "center",
    boxShadow: "var(--shadow-md)",
    cursor: "pointer",
  },
  secondaryCta: {
    height: 52,
    padding: "0 28px",
    fontSize: 15,
    fontWeight: 600,
    background: "var(--bg-panel)",
    color: "var(--text-primary)",
    border: "1px solid var(--border-strong)",
    borderRadius: "var(--radius-md)",
    textDecoration: "none",
    display: "inline-flex",
    alignItems: "center",
  },
  heroNote: {
    marginTop: 24,
    fontSize: 13,
    color: "var(--text-tertiary)",
  },

  /* Section common */
  section: {
    padding: "80px 24px",
    maxWidth: 1080,
    margin: "0 auto",
    width: "100%",
    boxSizing: "border-box",
  },
  sectionHead: {
    textAlign: "center",
    marginBottom: 48,
  },
  eyebrow: {
    fontSize: 12,
    fontWeight: 700,
    color: "var(--color-primary)",
    textTransform: "uppercase",
    letterSpacing: "0.08em",
    marginBottom: 8,
  },
  h2: {
    fontSize: 36,
    fontWeight: 700,
    margin: 0,
    letterSpacing: "-0.01em",
  },

  /* Feature grid */
  featureGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))",
    gap: 16,
  },
  featureCard: {
    padding: 28,
    background: "var(--bg-panel)",
    border: "1px solid var(--border-subtle)",
    borderRadius: "var(--radius-lg)",
    transition: "transform 200ms var(--ease-emphasis), box-shadow 200ms",
  },
  featureEmoji: {
    fontSize: 32,
    marginBottom: 16,
  },
  featureTitle: {
    fontSize: 17,
    fontWeight: 700,
    marginBottom: 8,
  },
  featureDesc: {
    fontSize: 14,
    lineHeight: 1.6,
    color: "var(--text-secondary)",
  },

  /* Stats */
  statsSection: {
    padding: "80px 24px",
    background: "var(--bg-elevated)",
    borderTop: "1px solid var(--border-subtle)",
    borderBottom: "1px solid var(--border-subtle)",
  },
  statsGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
    gap: 16,
    maxWidth: 1080,
    margin: "0 auto",
  },
  statCard: {
    padding: 28,
    background: "var(--bg-panel)",
    border: "1px solid var(--border-subtle)",
    borderRadius: "var(--radius-lg)",
    textAlign: "center",
  },
  statValue: {
    fontSize: 32,
    fontWeight: 800,
    color: "var(--color-primary)",
    fontFamily: "var(--font-mono)",
    marginBottom: 8,
  },
  statLabel: {
    fontSize: 13,
    color: "var(--text-secondary)",
  },

  /* Tech chips */
  techRow: {
    display: "flex",
    flexWrap: "wrap",
    gap: 8,
    justifyContent: "center",
  },
  techChip: {
    padding: "8px 14px",
    fontSize: 13,
    fontWeight: 500,
    background: "var(--bg-panel)",
    color: "var(--text-secondary)",
    border: "1px solid var(--border-subtle)",
    borderRadius: 999,
  },

  /* Final CTA */
  finalCta: {
    padding: "100px 24px",
    textAlign: "center",
    background: "var(--bg-elevated)",
    borderTop: "1px solid var(--border-subtle)",
  },
  finalDesc: {
    fontSize: 16,
    color: "var(--text-secondary)",
    margin: "16px 0 32px",
  },

  /* Footer */
  footer: {
    padding: "40px 24px 32px",
    background: "var(--bg-panel)",
    borderTop: "1px solid var(--border-subtle)",
  },
  footerInner: {
    maxWidth: 1080,
    margin: "0 auto",
    display: "flex",
    flexDirection: "column",
    gap: 16,
    alignItems: "center",
    textAlign: "center",
  },
  footerBrand: {
    fontWeight: 700,
    fontSize: 14,
    display: "flex",
    alignItems: "center",
    gap: 8,
  },
  footerLinks: {
    display: "flex",
    gap: 24,
  },
  footerLink: {
    fontSize: 13,
    color: "var(--text-secondary)",
    textDecoration: "none",
  },
  footerNote: {
    fontSize: 12,
    color: "var(--text-tertiary)",
    lineHeight: 1.6,
  },
};
