import http from "./http";

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface AdminGoodsCategory {
  id: number;
  parentId?: number;
  name: string;
  iconUrl?: string;
  sortNo?: number;
  status?: string;
  goodsCount?: number;
}

export interface AdminGoodsItem {
  spuId: number;
  categoryId: number;
  categoryName?: string;
  title: string;
  subtitle?: string;
  coverImageUrl?: string;
  minPrice: number;
  maxPrice: number;
  salesStatus: string;
  stockStatus: string;
  sortNo?: number;
  tags?: string[];
  skuCount: number;
  onSaleSkuCount: number;
  totalStock: number;
}

export interface AdminGoodsListResult {
  list: AdminGoodsItem[];
  page: {
    pageNo: number;
    pageSize: number;
    total: number;
  };
}

export interface AdminGoodsSkuItem {
  skuId: number;
  spuId: number;
  specText?: string;
  skuImageUrl?: string;
  price: number;
  originPrice?: number;
  stockQty: number;
  salesStatus: string;
}

export interface AdminGoodsDetailResult {
  spu: AdminGoodsItem;
  skus: AdminGoodsSkuItem[];
}

function unwrap<T>(response: ApiResponse<T>): T {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || "请求失败");
  }
  return response.data;
}

export async function fetchAdminGoodsCategories(): Promise<AdminGoodsCategory[]> {
  const response: any = await http.get("/api/goods/categories", {
    params: {
      withCount: true,
    },
  });
  const data = unwrap<{ categories: AdminGoodsCategory[] }>(response);
  return data.categories || [];
}

export async function fetchAdminGoodsList(params: {
  categoryId?: number;
  keyword?: string;
  salesStatus?: string;
  pageNo: number;
  pageSize: number;
}): Promise<AdminGoodsListResult> {
  const response: any = await http.get("/api/admin/goods/list", { params });
  return unwrap<AdminGoodsListResult>(response);
}

export async function updateAdminGoodsStatus(spuId: number, salesStatus: "ON_SALE" | "OFF_SALE"): Promise<{
  spuId: number;
  salesStatus: string;
}> {
  const response: any = await http.post(`/api/admin/goods/${spuId}/status`, { salesStatus });
  return unwrap<{ spuId: number; salesStatus: string }>(response);
}

export async function fetchAdminGoodsDetail(spuId: number): Promise<AdminGoodsDetailResult> {
  const response: any = await http.get(`/api/admin/goods/${spuId}`);
  return unwrap<AdminGoodsDetailResult>(response);
}

export async function saveAdminGoods(spuId: number, payload: {
  title?: string;
  subtitle?: string;
  coverImageUrl?: string;
  categoryId?: number;
  sortNo?: number;
  stockStatus?: string;
  salesStatus?: string;
  minPrice?: number;
  maxPrice?: number;
  tags?: string;
}): Promise<{
  spuId: number;
  title: string;
  salesStatus: string;
}> {
  const response: any = await http.post(`/api/admin/goods/${spuId}/save`, payload);
  return unwrap<{ spuId: number; title: string; salesStatus: string }>(response);
}

export async function createAdminGoodsSku(spuId: number, payload: {
  specText: string;
  skuImageUrl?: string;
  price: number;
  originPrice?: number;
  stockQty?: number;
  salesStatus?: string;
}): Promise<{
  skuId: number;
  spuId: number;
}> {
  const response: any = await http.post(`/api/admin/goods/${spuId}/sku/create`, payload);
  return unwrap<{ skuId: number; spuId: number }>(response);
}

export async function saveAdminGoodsSku(skuId: number, payload: {
  specText?: string;
  skuImageUrl?: string;
  price?: number;
  originPrice?: number;
  stockQty?: number;
  salesStatus?: string;
}): Promise<{
  skuId: number;
  spuId: number;
}> {
  const response: any = await http.post(`/api/admin/goods/sku/${skuId}/save`, payload);
  return unwrap<{ skuId: number; spuId: number }>(response);
}

export async function updateAdminGoodsSkuStatus(skuId: number, salesStatus: "ON_SALE" | "OFF_SALE"): Promise<{
  skuId: number;
  salesStatus: string;
}> {
  const response: any = await http.post(`/api/admin/goods/sku/${skuId}/status`, { salesStatus });
  return unwrap<{ skuId: number; salesStatus: string }>(response);
}

export async function deleteAdminGoodsSku(skuId: number): Promise<boolean> {
  const response: any = await http.delete(`/api/admin/goods/sku/${skuId}`);
  return unwrap<boolean>(response);
}

