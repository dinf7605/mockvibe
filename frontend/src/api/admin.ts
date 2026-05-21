import { api } from "./client";

export interface AdminUserView {
  userId: string;
  username: string;
  email: string;
  role: "USER" | "ADMIN";
  status: "ACTIVE" | "SUSPENDED";
  lastLoginAt: string | null;
  createdAt: string;
}

export interface AdminUserDetail extends AdminUserView {
  cashBalance: number;
}

export interface PageResponse<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export async function listUsers(page = 0, size = 20): Promise<PageResponse<AdminUserView>> {
  return (await api.get("/admin/users", { params: { page, size } })).data;
}
export async function getUserDetail(userId: string): Promise<AdminUserDetail> {
  return (await api.get(`/admin/users/${userId}`)).data;
}
export async function suspendUser(userId: string) {
  return (await api.post(`/admin/users/${userId}/suspend`)).data;
}
export async function activateUser(userId: string) {
  return (await api.post(`/admin/users/${userId}/activate`)).data;
}
export async function issueStepUp(password: string): Promise<{ stepUpToken: string }> {
  return (await api.post("/admin/users/stepup", { password })).data;
}
export async function adjustCash(userId: string, amount: number, reason: string, stepUpToken: string) {
  return (await api.post(`/admin/users/${userId}/cash`, { amount, reason, stepUpToken })).data;
}
export async function changeRole(userId: string, role: "USER" | "ADMIN", stepUpToken: string) {
  return (await api.post(`/admin/users/${userId}/role`, { role, stepUpToken })).data;
}

// ===== Stocks =====
export interface AdminStockView {
  ticker: string; market: string; currency: string; companyName: string;
  sector: string | null; region: string | null; currentPrice: number | null;
  tickSize: number; active: boolean;
}
export async function listStocks(page = 0, size = 30): Promise<PageResponse<AdminStockView>> {
  return (await api.get("/admin/stocks", { params: { page, size } })).data;
}
export async function toggleStock(ticker: string) {
  return (await api.post(`/admin/stocks/${ticker}/toggle`)).data;
}

// ===== Trades =====
export interface AdminOrderView {
  orderId: number; userId: string; ticker: string;
  orderType: "BUY" | "SELL"; orderMethod: "MARKET" | "LIMIT";
  price: number; quantity: number; fxRate: number; fee: number;
  totalAmountKrw: number; createdAt: string;
}
export async function listTrades(page = 0, size = 50): Promise<PageResponse<AdminOrderView>> {
  return (await api.get("/admin/trades", { params: { page, size } })).data;
}

// ===== System =====
export interface CircuitBreakerInfo {
  name: string; state: string;
  failureRate: number; failedCalls: number; successCalls: number;
}
export async function listCircuitBreakers(): Promise<CircuitBreakerInfo[]> {
  return (await api.get("/admin/system/circuit-breakers")).data;
}
export async function resetCircuitBreaker(name: string) {
  return (await api.post(`/admin/system/circuit-breakers/${name}/reset`)).data;
}
export async function getCacheStats(): Promise<{ priceCacheSize: number }> {
  return (await api.get("/admin/system/cache")).data;
}

// ===== AI Usage =====
export async function getDailyAiUsage(): Promise<Record<string, number>> {
  return (await api.get("/admin/ai-usage/daily")).data;
}

// ===== Announcements =====
export interface AnnouncementView {
  announcementId: number; adminUserId: string; title: string; content: string;
  level: "INFO" | "WARNING" | "CRITICAL";
  startsAt: string | null; endsAt: string | null;
  createdAt: string;
  active: boolean;
}
export interface AnnouncementUpsert {
  title: string; content: string; level: "INFO" | "WARNING" | "CRITICAL";
  startsAt: string | null; endsAt: string | null;
}
export async function listAnnouncements(page = 0, size = 20): Promise<PageResponse<AnnouncementView>> {
  return (await api.get("/admin/announcements", { params: { page, size } })).data;
}
export async function createAnnouncement(req: AnnouncementUpsert) {
  return (await api.post("/admin/announcements", req)).data;
}
export async function updateAnnouncement(id: number, req: AnnouncementUpsert) {
  return (await api.put(`/admin/announcements/${id}`, req)).data;
}
export async function toggleAnnouncement(id: number) {
  return (await api.post(`/admin/announcements/${id}/toggle`)).data;
}
export async function deleteAnnouncement(id: number) {
  return (await api.delete(`/admin/announcements/${id}`)).data;
}

// ===== Audit =====
export interface AuditLogView {
  auditId: number; adminUserId: string; action: string;
  targetType: string; targetId: string | null;
  beforeValue: string | null; afterValue: string | null;
  reason: string | null; ipAddress: string | null; userAgent: string | null;
  createdAt: string;
}
export async function listAuditLogs(page = 0, size = 50, targetType?: string, targetId?: string): Promise<PageResponse<AuditLogView>> {
  return (await api.get("/admin/audit", { params: { page, size, targetType, targetId } })).data;
}
