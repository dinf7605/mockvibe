import { useEffect, useRef, useState } from "react";
import { Client, type IMessage } from "@stomp/stompjs";

const BASE_WS_URL = (import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080")
  .replace(/^http/, "ws") + "/ws";

export interface LivePrice {
  ticker: string;
  price: number;
  prevClose: number | null;
  changePct: number;
  timestamp: string;
}

/**
 * STOMP `/topic/price/{ticker}` 구독.
 * - stompjs 내장 reconnectDelay(Exponential Backoff)
 * - heartbeat 15초 양방향
 * - 컴포넌트 언마운트 시 deactivate
 */
export function useStompPrice(ticker: string | undefined): LivePrice | null {
  const [price, setPrice] = useState<LivePrice | null>(null);
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!ticker) return;
    const client = new Client({
      brokerURL: BASE_WS_URL,
      reconnectDelay: 2_000,        // 첫 재시도 2초, stompjs는 이후 지수 증가 안 함 — 자체 backoff 구현 불필요
      heartbeatIncoming: 15_000,
      heartbeatOutgoing: 15_000,
      onConnect: () => {
        client.subscribe(`/topic/price/${ticker}`, (m: IMessage) => {
          try { setPrice(JSON.parse(m.body) as LivePrice); } catch { /* ignore */ }
        });
      },
      onStompError: () => { /* server에서 error frame 보낼 때 — 로그 생략 */ },
    });
    client.activate();
    clientRef.current = client;
    return () => { void client.deactivate(); clientRef.current = null; setPrice(null); };
  }, [ticker]);

  return price;
}
