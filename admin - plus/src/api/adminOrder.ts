import http from "./http";

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface AdminOrderItem {
  id: number;
  itemType: string;
  itemName: string;
  itemImageUrl: string;
  unitPrice: number;
  qty: number;
  amount: number;
}

export interface AdminOrderLog {
  action: string;
  fromStatus?: string;
  toStatus?: string;
  operatorType: string;
  operatorId: number;
  remark?: string;
  createdAt: string;
}

export interface AdminOrderSummary {
  orderNo: string;
  userId: number;
  orderStatus: string;
  payStatus: string;
  totalAmount: number;
  payAmount: number;
  createdAt: string;
  paidAt?: string;
  shipping?: {
    carrierCode?: string;
    trackingNo?: string;
    remark?: string;
    shippedAt?: string;
  };
  itemPreview?: {
    title?: string;
    imageUrl?: string;
  };
}

export interface AdminOrderDetail extends AdminOrderSummary {
  remark?: string;
  addressSnapshot?: Record<string, string>;
  items: AdminOrderItem[];
  logs: AdminOrderLog[];
}

export interface AdminOrderListResult {
  list: AdminOrderSummary[];
  page: {
    pageNo: number;
    pageSize: number;
    total: number;
  };
}

interface ActionData {
  orderNo: string;
  orderStatus: string;
}

interface ShipData extends ActionData {
  carrierCode: string;
  trackingNo: string;
}

function unwrap<T>(response: ApiResponse<T>): T {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || "请求失败");
  }
  return response.data;
}

export async function fetchAdminOrderList(params: {
  status?: string;
  keyword?: string;
  pageNo: number;
  pageSize: number;
}): Promise<AdminOrderListResult> {
  const response: any = await http.get("/api/admin/order/list", { params });
  return unwrap<AdminOrderListResult>(response);
}

export async function fetchAdminOrderDetail(orderNo: string): Promise<AdminOrderDetail> {
  const response: any = await http.get(`/api/admin/order/${orderNo}`);
  return unwrap<AdminOrderDetail>(response);
}

export async function cancelAdminOrder(orderNo: string, remark?: string): Promise<ActionData> {
  const response: any = await http.post(`/api/admin/order/${orderNo}/cancel`, { remark: remark || "" });
  return unwrap<ActionData>(response);
}

export async function shipAdminOrder(orderNo: string, payload: {
  carrierCode: string;
  trackingNo: string;
  remark?: string;
}): Promise<ShipData> {
  const response: any = await http.post(`/api/admin/order/${orderNo}/ship`, payload);
  return unwrap<ShipData>(response);
}

export async function completeAdminOrder(orderNo: string, remark?: string): Promise<ActionData> {
  const response: any = await http.post(`/api/admin/order/${orderNo}/complete`, { remark: remark || "" });
  return unwrap<ActionData>(response);
}

