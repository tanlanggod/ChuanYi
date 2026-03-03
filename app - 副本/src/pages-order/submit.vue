<template>
  <view class="container">
    <view class="card loading-card" v-if="loading">
      <view class="muted">订单信息加载中...</view>
    </view>

    <view class="card loading-card" v-else-if="!snapshotNo || !snapshot.id">
      <view class="muted">快照信息异常，请返回重新生成</view>
    </view>

    <template v-else>
      <view class="card address-card" @click="goAddressList">
        <view v-if="selectedAddress" class="address-main">
          <view class="address-line1">
            <view class="address-name">{{ selectedAddress.receiverName }}</view>
            <view class="address-phone">{{ selectedAddress.receiverPhone }}</view>
            <view class="address-tag" v-if="Number(selectedAddress.isDefault) === 1">默认</view>
          </view>
          <view class="address-detail">
            {{ selectedAddress.province }} {{ selectedAddress.city }} {{ selectedAddress.district }}
            {{ selectedAddress.detailAddress }}
          </view>
        </view>
        <view v-else class="muted">暂无收货地址，点击新增</view>
        <view class="arrow">›</view>
      </view>

      <view class="card item-card">
        <image class="item-image" :src="resolveImage(snapshot.previewImageUrl)" mode="aspectFill"></image>
        <view class="item-main">
          <view class="item-title">定制商品</view>
          <view class="item-sub">手围 {{ formatWristSize(snapshot.wristSizeCm) }}cm</view>
          <view class="item-price">￥{{ formatAmount(snapshot.priceSnapshot) }}</view>
        </view>
        <view class="qty">x1</view>
      </view>

      <view class="card remark-card">
        <view class="remark-head">
          <view class="remark-title">备注</view>
          <view class="muted">{{ remark.length }}/45</view>
        </view>
        <textarea class="remark" v-model="remark" maxlength="45" placeholder="选填，给商家留言，如需加急请备注“加急”二字" />
      </view>
    </template>

    <view class="bottom-space"></view>
    <view class="bottom-bar" v-if="snapshotNo && snapshot.id">
      <view class="pay-line">实付：<text class="pay">￥{{ formatAmount(snapshot.priceSnapshot) }}</text></view>
      <view class="submit-btn" :class="{ disabled: submitting }" @click="submitOrder">
        {{ submitting ? "提交中..." : "提交订单" }}
      </view>
    </view>
  </view>
</template>

<script>
import { createOrderFromSnapshot, fetchAddressList, fetchSnapshotDetail } from "../common/api";
import { resolveAssetUrl } from "../common/http";
import { loginGuestIfNeeded } from "../common/session";

export default {
  data() {
    return {
      snapshotNo: "",
      snapshot: {},
      addresses: [],
      selectedAddressId: null,
      remark: "",
      loading: false,
      submitting: false,
    };
  },
  computed: {
    selectedAddress() {
      if (!this.selectedAddressId) {
        return null;
      }
      return this.addresses.find((item) => Number(item.id) === Number(this.selectedAddressId)) || null;
    },
  },
  async onLoad(options) {
    this.snapshotNo = (options && options.snapshotNo) || "";
    await this.loadData();
  },
  async onShow() {
    if (this.snapshotNo && !this.loading) {
      await this.loadAddressOnly();
    }
  },
  methods: {
    async loadData() {
      if (!this.snapshotNo) {
        uni.showToast({ title: "快照参数无效", icon: "none" });
        return;
      }
      try {
        this.loading = true;
        await loginGuestIfNeeded();
        const [snapshotData, addressData] = await Promise.all([fetchSnapshotDetail(this.snapshotNo), fetchAddressList()]);
        this.snapshot = snapshotData || {};
        this.applyAddresses((addressData && addressData.addresses) || []);
      } catch (error) {
        uni.showToast({ title: "加载失败", icon: "none" });
      } finally {
        this.loading = false;
      }
    },
    async loadAddressOnly() {
      try {
        await loginGuestIfNeeded();
        const data = await fetchAddressList();
        this.applyAddresses((data && data.addresses) || []);
      } catch (error) {
        uni.showToast({ title: "地址加载失败", icon: "none" });
      }
    },
    applyAddresses(addresses) {
      this.addresses = addresses || [];
      const recentAddressId = this.consumeRecentAddressId();
      if (recentAddressId) {
        const recentAddress = this.addresses.find((item) => Number(item.id) === Number(recentAddressId));
        if (recentAddress) {
          this.selectedAddressId = recentAddress.id;
          return;
        }
      }
      const current = this.addresses.find((item) => Number(item.id) === Number(this.selectedAddressId));
      if (current) {
        return;
      }
      const defaultAddress = this.addresses.find((item) => Number(item.isDefault) === 1) || this.addresses[0];
      this.selectedAddressId = defaultAddress ? defaultAddress.id : null;
    },
    consumeRecentAddressId() {
      const key = "order_submit_recent_address_id";
      const value = uni.getStorageSync(key);
      if (!value) {
        return null;
      }
      uni.removeStorageSync(key);
      const id = Number(value);
      return Number.isNaN(id) ? null : id;
    },
    goAddressList() {
      if (this.selectedAddressId) {
        uni.navigateTo({ url: "/pages-user/address-list?from=order-submit" });
        return;
      }
      uni.navigateTo({ url: "/pages-user/address-edit?from=order-submit" });
    },
    async submitOrder() {
      if (this.submitting || this.loading) {
        return;
      }
      if (!this.snapshotNo || !this.snapshot.id) {
        uni.showToast({ title: "快照信息异常", icon: "none" });
        return;
      }
      if (!this.selectedAddressId) {
        if (this.addresses.length === 0) {
          uni.showToast({ title: "请先新增收货地址", icon: "none" });
          uni.navigateTo({ url: "/pages-user/address-edit?from=order-submit" });
        } else {
          uni.showToast({ title: "请先选择收货地址", icon: "none" });
          uni.navigateTo({ url: "/pages-user/address-list?from=order-submit" });
        }
        return;
      }
      this.submitting = true;
      try {
        const result = await createOrderFromSnapshot({
          snapshotNo: this.snapshotNo,
          addressId: this.selectedAddressId,
          remark: this.remark || "",
        });
        if (result && result.orderNo) {
          uni.redirectTo({ url: `/pages-order/detail?orderNo=${result.orderNo}` });
        }
      } catch (error) {
        uni.showToast({ title: "提交失败", icon: "none" });
      } finally {
        this.submitting = false;
      }
    },
    resolveImage(url) {
      if (!url) {
        return "/static/logo.png";
      }
      return resolveAssetUrl(url);
    },
    formatAmount(value) {
      const text = Number(value || 0).toFixed(2);
      return text.replace(/\.00$/, "").replace(/(\.\d)0$/, "$1");
    },
    formatWristSize(value) {
      return Number(value || 13).toFixed(0);
    },
  },
};
</script>

<style scoped>
.container {
  padding: 24rpx;
  padding-bottom: 200rpx;
  background: #F9F7F3;
  min-height: 100vh;
}

.loading-card {
  min-height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(47, 54, 50, 0.04);
}

.address-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 6rpx 16rpx rgba(47, 54, 50, 0.04);
  border: 1rpx solid rgba(232, 228, 219, 0.6);
  position: relative;
  overflow: hidden;
}

.address-card::before {
  content: "";
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 6rpx;
  background: repeating-linear-gradient(-45deg, #C2A578 0, #C2A578 20rpx, transparent 20rpx, transparent 40rpx, #3B6E53 40rpx, #3B6E53 60rpx, transparent 60rpx, transparent 80rpx);
  opacity: 0.5;
}

.address-main {
  flex: 1;
  min-width: 0;
}

.address-line1 {
  display: flex;
  gap: 16rpx;
  align-items: center;
  margin-bottom: 12rpx;
}

.address-name,
.address-phone {
  font-size: 32rpx;
  color: #1A241D;
  font-weight: 700;
}

.address-phone {
  font-weight: 600;
  font-family: -apple-system, BlinkMacSystemFont, "Helvetica Neue", Helvetica, Arial, sans-serif;
}

.address-tag {
  background: #F0F5F2;
  color: #3B6E53;
  border: 1rpx solid #D1E0D7;
  border-radius: 8rpx;
  padding: 4rpx 12rpx;
  font-size: 20rpx;
}

.address-detail {
  color: #7A8B7C;
  font-size: 26rpx;
  line-height: 1.6;
}

.arrow {
  color: #C2A578;
  font-size: 44rpx;
  font-weight: 300;
}

.item-card {
  margin-top: 24rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(47, 54, 50, 0.04);
  border: 1rpx solid rgba(232, 228, 219, 0.6);
}

.item-image {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background: #E8E4DB;
  flex-shrink: 0;
  box-shadow: 0 4rpx 10rpx rgba(0,0,0,0.06);
  border: 2rpx solid #F0F5F2;
}

.item-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.item-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1A241D;
}

.item-sub {
  margin-top: 8rpx;
  color: #8C968F;
  font-size: 24rpx;
}

.item-price {
  margin-top: 16rpx;
  color: #C2A578;
  font-size: 36rpx;
  font-weight: 700;
  font-family: -apple-system, BlinkMacSystemFont, "Helvetica Neue", Helvetica, Arial, sans-serif;
}

.qty {
  color: #556B5D;
  font-size: 26rpx;
  font-weight: 500;
  align-self: flex-end;
  margin-bottom: 8rpx;
}

.remark-card {
  margin-top: 24rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(47, 54, 50, 0.04);
  border: 1rpx solid rgba(232, 228, 219, 0.6);
}

.remark-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.remark-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1A241D;
  position: relative;
  padding-left: 20rpx;
}

.remark-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 6rpx;
  height: 24rpx;
  background: #C2A578;
  border-radius: 4rpx;
}

.muted {
  color: #8C968F;
  font-size: 22rpx;
}

.remark {
  width: 100%;
  min-height: 160rpx;
  background: #F9F7F3;
  border-radius: 12rpx;
  padding: 20rpx;
  box-sizing: border-box;
  font-size: 26rpx;
  color: #2F3632;
  border: 1rpx solid rgba(232, 228, 219, 0.8);
  transition: border-color 0.2s;
}

.remark:focus {
  border-color: #3B6E53;
  background: #FDFBF7;
}

.bottom-space {
  height: 60rpx;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-top: 1rpx solid rgba(232, 228, 219, 0.6);
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  box-shadow: 0 -10rpx 30rpx rgba(47, 54, 50, 0.04);
}

.pay-line {
  font-size: 28rpx;
  color: #556B5D;
  display: flex;
  align-items: center;
}

.pay {
  color: #C2A578;
  font-size: 44rpx;
  font-weight: 700;
  font-family: -apple-system, BlinkMacSystemFont, "Helvetica Neue", Helvetica, Arial, sans-serif;
}

.submit-btn {
  min-width: 280rpx;
  text-align: center;
  background: linear-gradient(135deg, #4A8365 0%, #3B6E53 100%);
  color: #fff;
  font-size: 30rpx;
  border-radius: 999rpx;
  padding: 20rpx 32rpx;
  font-weight: 600;
  box-shadow: 0 8rpx 20rpx rgba(59, 110, 83, 0.2);
  transition: all 0.2s ease;
}

.submit-btn:active {
  transform: scale(0.98);
}

.submit-btn.disabled {
  background: #D1E0D7;
  color: #F0F5F2;
  box-shadow: none;
  pointer-events: none;
}
</style>
