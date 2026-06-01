import { useEffect } from "react";
import { Client, type IMessage } from "@stomp/stompjs";
import { useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "../store/authStore";
import { useToast } from "../components/Toast";
import type { AppNotification } from "../api/notifications";

const BASE_WS_URL = (import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080")
  .replace(/^http/, "ws") + "/ws";

/**
 * STOMP `/user/queue/notifications` 사용자별 실시간 알림 구독.
 * - CONNECT 시 JWT를 헤더로 보내 서버가 Principal(userId)을 세팅 → 본인 큐로 라우팅
 * - 수신 시 토스트 + notifications 쿼리 무효화(목록/미확인 배지 갱신)
 * 로그인 상태에서 AppLayout이 1회 활성화.
 */
export function useNotificationStream() {
  const qc = useQueryClient();
  const notify = useToast();
  const token = useAuthStore((s) => s.accessToken);

  useEffect(() => {
    if (!token) return;
    const client = new Client({
      brokerURL: BASE_WS_URL,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 3_000,
      heartbeatIncoming: 15_000,
      heartbeatOutgoing: 15_000,
      onConnect: () => {
        client.subscribe("/user/queue/notifications", (m: IMessage) => {
          try {
            const n = JSON.parse(m.body) as AppNotification;
            notify.info(n.title);
          } catch { /* ignore */ }
          qc.invalidateQueries({ queryKey: ["notifications"] });
        });
      },
      onStompError: () => { /* ignore */ },
    });
    client.activate();
    return () => { void client.deactivate(); };
  }, [token, qc, notify]);
}
