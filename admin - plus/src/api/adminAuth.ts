import http from "./http";

export const ADMIN_TOKEN_KEY = "admin_token";
export const ADMIN_PROFILE_KEY = "admin_profile";

export interface AdminProfile {
  id: number;
  username: string;
  nickname: string;
  status: string;
}

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

interface AdminLoginData {
  token: string;
  tokenType: string;
  expiresIn: number;
  admin: AdminProfile;
}

function unwrap<T>(response: ApiResponse<T>): T {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || "请求失败");
  }
  return response.data;
}

export async function adminLogin(username: string, password: string): Promise<AdminLoginData> {
  const response: any = await http.post("/api/admin/auth/login", { username, password });
  return unwrap<AdminLoginData>(response);
}

export async function fetchAdminMe(): Promise<AdminProfile> {
  const response: any = await http.get("/api/admin/auth/me");
  return unwrap<AdminProfile>(response);
}

export async function adminLogout(): Promise<boolean> {
  const response: any = await http.post("/api/admin/auth/logout");
  return unwrap<boolean>(response);
}

export function saveAdminSession(token: string, admin: AdminProfile): void {
  localStorage.setItem(ADMIN_TOKEN_KEY, token);
  localStorage.setItem(ADMIN_PROFILE_KEY, JSON.stringify(admin));
}

export function clearAdminSession(): void {
  localStorage.removeItem(ADMIN_TOKEN_KEY);
  localStorage.removeItem(ADMIN_PROFILE_KEY);
}

export function loadAdminProfile(): AdminProfile | null {
  const raw = localStorage.getItem(ADMIN_PROFILE_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as AdminProfile;
  } catch (_error) {
    return null;
  }
}

