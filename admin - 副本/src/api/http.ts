import axios from "axios";

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "",
  timeout: 10000,
});

function isAbsoluteUrl(url: string): boolean {
  return /^https?:\/\//i.test(url) || /^data:/i.test(url);
}

function resolveApiOrigin(): string {
  if (typeof window === "undefined") {
    return "";
  }
  const rawBaseUrl = String(import.meta.env.VITE_API_BASE_URL || "").trim();
  if (!rawBaseUrl) {
    return window.location.origin;
  }
  if (isAbsoluteUrl(rawBaseUrl)) {
    try {
      return new URL(rawBaseUrl).origin;
    } catch (error) {
      return window.location.origin;
    }
  }
  if (rawBaseUrl.startsWith("//")) {
    return `${window.location.protocol}${rawBaseUrl}`;
  }
  return window.location.origin;
}

function extractErrorMessage(error: any): string {
  const responseData = error?.response?.data;
  if (responseData && typeof responseData.message === "string" && responseData.message.trim()) {
    return responseData.message.trim();
  }
  if (typeof error?.message === "string" && error.message.trim()) {
    return error.message.trim();
  }
  return "请求失败";
}

export function resolveStaticUrl(url?: string): string {
  const raw = String(url || "").trim();
  if (!raw) {
    return "";
  }
  if (raw.startsWith("//")) {
    if (typeof window === "undefined") {
      return `https:${raw}`;
    }
    return `${window.location.protocol}${raw}`;
  }
  if (isAbsoluteUrl(raw)) {
    return raw;
  }
  const origin = resolveApiOrigin();
  if (!origin) {
    return raw;
  }
  if (raw.startsWith("/")) {
    return `${origin}${raw}`;
  }
  return `${origin}/${raw}`;
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem("admin_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response.data,
  (error) => Promise.reject(new Error(extractErrorMessage(error)))
);

export default http;

