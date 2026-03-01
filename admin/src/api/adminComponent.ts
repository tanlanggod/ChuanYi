import http from "./http";

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface AdminComponentCategoryItem {
  id: number;
  parentId?: number;
  categoryType?: string;
  name: string;
  sortNo?: number;
  status?: string;
  skuCount?: number;
}

export interface AdminComponentSkuItem {
  skuId: number;
  skuCode?: string;
  categoryId: number;
  categoryName?: string;
  categoryType?: string;
  name: string;
  specText?: string;
  beadDiameterMm?: number;
  price: number;
  stockQty?: number;
  stockWarnQty?: number;
  salesStatus: string;
  imageUrl?: string;
}

export interface AdminComponentSkuListResult {
  list: AdminComponentSkuItem[];
  page: {
    pageNo: number;
    pageSize: number;
    total: number;
  };
}

function unwrap<T>(response: ApiResponse<T>): T {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || "请求失败");
  }
  return response.data;
}

export async function fetchAdminComponentCategories(withCount = true): Promise<AdminComponentCategoryItem[]> {
  const response: any = await http.get("/api/admin/component/categories", { params: { withCount } });
  const data = unwrap<{ categories: AdminComponentCategoryItem[] }>(response);
  return data.categories || [];
}

export async function saveAdminComponentCategory(payload: {
  id?: number;
  parentId?: number;
  categoryType?: string;
  name: string;
  sortNo?: number;
  status?: string;
}): Promise<{ id: number; name: string; status: string }> {
  const response: any = await http.post("/api/admin/component/category/save", payload);
  return unwrap<{ id: number; name: string; status: string }>(response);
}

export async function updateAdminComponentCategoryStatus(id: number, status: "ENABLED" | "DISABLED"): Promise<{
  id: number;
  status: string;
}> {
  const response: any = await http.post(`/api/admin/component/category/${id}/status`, { status });
  return unwrap<{ id: number; status: string }>(response);
}

export async function deleteAdminComponentCategory(id: number): Promise<boolean> {
  const response: any = await http.delete(`/api/admin/component/category/${id}`);
  return unwrap<boolean>(response);
}

export async function fetchAdminComponentSkuList(params: {
  categoryId?: number;
  keyword?: string;
  salesStatus?: string;
  pageNo: number;
  pageSize: number;
}): Promise<AdminComponentSkuListResult> {
  const response: any = await http.get("/api/admin/component/sku/list", { params });
  return unwrap<AdminComponentSkuListResult>(response);
}

export async function createAdminComponentSku(payload: {
  skuCode?: string;
  categoryId: number;
  name: string;
  specText?: string;
  beadDiameterMm?: number;
  price: number;
  stockQty?: number;
  stockWarnQty?: number;
  salesStatus?: string;
  imageUrl?: string;
}): Promise<{ skuId: number; skuCode: string }> {
  const response: any = await http.post("/api/admin/component/sku/create", payload);
  return unwrap<{ skuId: number; skuCode: string }>(response);
}

export async function saveAdminComponentSku(skuId: number, payload: {
  skuCode?: string;
  categoryId?: number;
  name?: string;
  specText?: string;
  beadDiameterMm?: number;
  price?: number;
  stockQty?: number;
  stockWarnQty?: number;
  salesStatus?: string;
  imageUrl?: string;
}): Promise<{ skuId: number; skuCode: string }> {
  const response: any = await http.post(`/api/admin/component/sku/${skuId}/save`, payload);
  return unwrap<{ skuId: number; skuCode: string }>(response);
}

export async function updateAdminComponentSkuStatus(skuId: number, salesStatus: "ON_SALE" | "OFF_SALE"): Promise<{
  skuId: number;
  salesStatus: string;
}> {
  const response: any = await http.post(`/api/admin/component/sku/${skuId}/status`, { salesStatus });
  return unwrap<{ skuId: number; salesStatus: string }>(response);
}

export async function batchUpdateAdminComponentSkuStatus(payload: {
  skuIds: number[];
  salesStatus: "ON_SALE" | "OFF_SALE";
}): Promise<{ updated: number; salesStatus: string; skuIds: number[] }> {
  const response: any = await http.post("/api/admin/component/sku/batch-status", payload);
  return unwrap<{ updated: number; salesStatus: string; skuIds: number[] }>(response);
}

export async function deleteAdminComponentSku(skuId: number): Promise<boolean> {
  const response: any = await http.delete(`/api/admin/component/sku/${skuId}`);
  return unwrap<boolean>(response);
}

