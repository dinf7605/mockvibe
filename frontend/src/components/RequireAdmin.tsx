import { Navigate } from "react-router-dom";
import { useAuthStore } from "../store/authStore";

interface Props {
  children: React.ReactNode;
}

/**
 * 관리자(ADMIN role) 전용 라우트 가드.
 * 권한 없으면 대시보드로 리다이렉트.
 */
export function RequireAdmin({ children }: Props) {
  const isAdmin = useAuthStore((s) => s.isAdmin());
  if (!isAdmin) return <Navigate to="/dashboard" replace />;
  return <>{children}</>;
}
