import { request } from "./http";

const DEMO_PHONE = "15700000000";

export async function loginGuestIfNeeded() {
  const token = uni.getStorageSync("token");
  if (token) {
    return token;
  }
  try {
    const data = await request({
      url: "/auth/login",
      method: "POST",
      data: {
        loginType: "PHONE",
        phone: DEMO_PHONE,
        deviceId: `uni-${Date.now()}`,
      },
    });
    if (data && data.token) {
      uni.setStorageSync("token", data.token);
      return data.token;
    }
  } catch (_error) {
    // fallback to guest login
  }

  const guestData = await request({
    url: "/auth/login",
    method: "POST",
    data: {
      loginType: "GUEST",
      deviceId: `uni-${Date.now()}`,
    },
  });
  if (guestData && guestData.token) {
    uni.setStorageSync("token", guestData.token);
  }
  return guestData ? guestData.token : "";
}

export function clearSession() {
  uni.removeStorageSync("token");
}
