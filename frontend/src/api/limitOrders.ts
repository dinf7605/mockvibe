import { api } from "./client";
import type { OrderSide } from "./trades";

export type LimitOrderStatus = "PENDING" | "FILLED" | "CANCELLED" | "EXPIRED";

export interface LimitOrder {
  limitOrderId: number;
  ticker: string;
  orderType: OrderSide;
  targetPrice: number;
  quantity: number;
  status: LimitOrderStatus;
  expiresAt: string | null;
  filledAt: string | null;
  cancelledAt: string | null;
  filledOrderId: number | null;
  createdAt: string;
}

export interface LimitOrderPage {
  items: LimitOrder[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface LimitOrderInput {
  ticker: string;
  orderType: OrderSide;
  targetPrice: number;
  quantity: number;
  /** 미설정 시 백엔드 기본 30일 */
  validityDays?: number;
}

/** 지정가 주문 등록. */
export async function registerLimitOrder(input: LimitOrderInput): Promise<LimitOrder> {
  const res = await api.post<LimitOrder>("/orders/limit", input);
  return res.data;
}

/** 내 지정가 주문 목록. */
export async function getLimitOrders(page = 0, size = 20): Promise<LimitOrderPage> {
  const res = await api.get<LimitOrderPage>("/orders/limit", { params: { page, size } });
  return res.data;
}

/** 지정가 주문 취소 (PENDING 만). */
export async function cancelLimitOrder(limitOrderId: number): Promise<void> {
  await api.delete(`/orders/limit/${limitOrderId}`);
}
