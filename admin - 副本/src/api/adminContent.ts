import http from "./http";

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

interface PageResult<T> {
  list: T[];
  page: {
    pageNo: number;
    pageSize: number;
    total: number;
  };
}

export interface AdminBannerItem {
  id: number;
  title: string;
  imageUrl?: string;
  jumpType?: string;
  jumpValue?: string;
  sortNo?: number;
  status: string;
}

export interface AdminGalleryItem {
  id: number;
  snapshotId?: number;
  userId?: number;
  title: string;
  coverImageUrl?: string;
  tags?: string[];
  displayStatus: string;
  publishedAt?: string;
}

function unwrap<T>(response: ApiResponse<T>): T {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || "请求失败");
  }
  return response.data;
}

export async function fetchAdminBannerList(params: {
  keyword?: string;
  status?: string;
  pageNo: number;
  pageSize: number;
}): Promise<PageResult<AdminBannerItem>> {
  const response: any = await http.get("/api/admin/content/banners", { params });
  return unwrap<PageResult<AdminBannerItem>>(response);
}

export async function updateAdminBannerStatus(id: number, status: "ENABLED" | "DISABLED"): Promise<{
  id: number;
  status: string;
}> {
  const response: any = await http.post(`/api/admin/content/banners/${id}/status`, { status });
  return unwrap<{ id: number; status: string }>(response);
}

export async function saveAdminBanner(payload: {
  id?: number;
  title: string;
  imageUrl?: string;
  jumpType?: string;
  jumpValue?: string;
  sortNo?: number;
  status?: string;
}): Promise<{
  id: number;
  title: string;
  status: string;
}> {
  const response: any = await http.post("/api/admin/content/banners/save", payload);
  return unwrap<{ id: number; title: string; status: string }>(response);
}

export async function fetchAdminGalleryList(params: {
  keyword?: string;
  displayStatus?: string;
  pageNo: number;
  pageSize: number;
}): Promise<PageResult<AdminGalleryItem>> {
  const response: any = await http.get("/api/admin/content/gallery", { params });
  return unwrap<PageResult<AdminGalleryItem>>(response);
}

export async function updateAdminGalleryStatus(id: number, displayStatus: "ENABLED" | "DISABLED" | "HIDDEN"): Promise<{
  id: number;
  displayStatus: string;
}> {
  const response: any = await http.post(`/api/admin/content/gallery/${id}/status`, { displayStatus });
  return unwrap<{ id: number; displayStatus: string }>(response);
}

export async function saveAdminGallery(id: number, payload: {
  title?: string;
  coverImageUrl?: string;
  tags?: string;
  displayStatus?: string;
}): Promise<{
  id: number;
  title: string;
  displayStatus: string;
}> {
  const response: any = await http.post(`/api/admin/content/gallery/${id}/save`, payload);
  return unwrap<{ id: number; title: string; displayStatus: string }>(response);
}

export async function uploadAdminContentImage(file: File): Promise<{
  url: string;
}> {
  const formData = new FormData();
  formData.append("file", file);
  const response: any = await http.post("/api/admin/content/upload", formData);
  return unwrap<{ url: string }>(response);
}

