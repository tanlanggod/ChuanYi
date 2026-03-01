import http from "./http";

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface AdminOverview {
  totalOrders: number;
  pendingPayCount: number;
  paidWaitShipCount: number;
  shippedWaitReceiveCount: number;
  completedCount: number;
  cancelledCount: number;
  todayOrderCount: number;
  todayRevenue: number;
}

function unwrap<T>(response: ApiResponse<T>): T {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || "请求失败");
  }
  return response.data;
}

export async function fetchAdminOverview(): Promise<AdminOverview> {
  const response: any = await http.get("/api/admin/dashboard/overview");
  return unwrap<AdminOverview>(response);
}

