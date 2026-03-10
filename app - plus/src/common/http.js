const API_BASE_URL = "/api";
const H5_ASSET_BASE_URL = String(import.meta.env.VITE_ASSET_BASE_URL || "").replace(/\/$/, "");

function isAbsoluteUrl(url) {
  return /^https?:\/\//i.test(url) || /^data:/i.test(url);
}

export function resolveAssetUrl(url) {
  const raw = (url || "").trim();
  if (!raw) {
    return "";
  }
  if (raw.startsWith("//")) {
    return `https:${raw}`;
  }
  if (isAbsoluteUrl(raw)) {
    return raw;
  }
  // H5 环境下将相对路径转为后端地址，避免 /uploads 资源走到前端服务。
  if (typeof window !== "undefined") {
    if (raw.startsWith("/")) {
      return `${H5_ASSET_BASE_URL}${raw}`;
    }
    return `${H5_ASSET_BASE_URL}/${raw}`;
  }
  return raw;
}

export function request(options) {
  const token = uni.getStorageSync("token");
  const headers = Object.assign({}, options.header || {});
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: `${API_BASE_URL}${options.url}`,
      method: options.method || "GET",
      data: options.data || {},
      header: headers,
      success: (res) => {
        const body = res.data || {};
        if (body.code === 0) {
          resolve(body.data);
          return;
        }
        if (body.code === 401) {
          uni.removeStorageSync("token");
        }
        reject(new Error(body.message || "请求失败"));
      },
      fail: (err) => {
        reject(err);
      },
    });
  });
}
