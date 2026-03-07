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
  min-height: 100vh;
  padding: 24rpx;
  padding-bottom: 200rpx;
}

.loading-card,
.address-card,
.item-card,
.remark-card {
  border-radius: 24rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.2);
  background: linear-gradient(160deg, #fffdfa 0%, #f8f1e6 100%);
  box-shadow: 0 10rpx 22rpx rgba(35, 56, 45, 0.08);
}

.loading-card {
  min-height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.address-card {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 28rpx;
}

.address-card::before {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 6rpx;
  opacity: 0.45;
  background: repeating-linear-gradient(
    -45deg,
    #c39e67 0,
    #c39e67 20rpx,
    transparent 20rpx,
    transparent 40rpx,
    #3e6f57 40rpx,
    #3e6f57 60rpx,
    transparent 60rpx,
    transparent 80rpx
  );
}

.address-main {
  flex: 1;
  min-width: 0;
}

.address-line1 {
  margin-bottom: 10rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.address-name,
.address-phone {
  color: #1f2f27;
  font-size: 32rpx;
  font-weight: 700;
}

.address-phone {
  font-weight: 600;
}

.address-tag {
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(62, 111, 87, 0.26);
  background: #edf4ef;
  color: #3e6f57;
  font-size: 20rpx;
}

.address-detail {
  color: #5d7265;
  font-size: 26rpx;
  line-height: 1.58;
}

.arrow {
  color: #b38d58;
  font-size: 44rpx;
  font-weight: 300;
}

.item-card {
  margin-top: 24rpx;
  display: flex;
  align-items: center;
  gap: 22rpx;
  padding: 28rpx;
}

.item-image {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  border: 2rpx solid rgba(255, 255, 255, 0.88);
  background: #e8decd;
  flex-shrink: 0;
}

.item-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.item-title {
  color: #1f2f27;
  font-size: 32rpx;
  font-weight: 700;
}

.item-sub {
  margin-top: 8rpx;
  color: #7d8d82;
  font-size: 24rpx;
}

.item-price {
  margin-top: 14rpx;
  color: #b38d58;
  font-size: 36rpx;
  font-weight: 700;
}

.qty {
  align-self: flex-end;
  margin-bottom: 8rpx;
  color: #5d7265;
  font-size: 26rpx;
}

.remark-card {
  margin-top: 24rpx;
  padding: 28rpx;
}

.remark-head {
  margin-bottom: 14rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.remark-title {
  position: relative;
  padding-left: 20rpx;
  color: #1f2f27;
  font-size: 30rpx;
  font-weight: 700;
}

.remark-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 7rpx;
  height: 24rpx;
  border-radius: 8rpx;
  background: linear-gradient(180deg, #c39e67 0%, #3e6f57 100%);
}

.muted {
  color: #8a968d;
  font-size: 22rpx;
}

.remark {
  width: 100%;
  min-height: 160rpx;
  box-sizing: border-box;
  padding: 20rpx;
  border-radius: 14rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.22);
  background: rgba(255, 252, 246, 0.9);
  color: #2f463b;
  font-size: 26rpx;
}

.remark:focus {
  border-color: rgba(62, 111, 87, 0.52);
  background: #fff;
}

.bottom-space {
  height: 60rpx;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid rgba(185, 152, 100, 0.2);
  background: rgba(252, 247, 238, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 -8rpx 22rpx rgba(35, 56, 45, 0.08);
}

.pay-line {
  color: #5d7265;
  font-size: 28rpx;
  display: flex;
  align-items: center;
}

.pay {
  color: #b38d58;
  font-size: 44rpx;
  font-weight: 700;
}

.submit-btn {
  min-width: 280rpx;
  padding: 20rpx 32rpx;
  border-radius: 999rpx;
  text-align: center;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  background: linear-gradient(135deg, #5a8c72 0%, #3e6f57 100%);
  box-shadow: 0 8rpx 16rpx rgba(35, 56, 45, 0.16);
}

.submit-btn:active {
  transform: scale(0.98);
}

.submit-btn.disabled {
  background: #c5d3cc;
  color: #eef3f0;
  box-shadow: none;
  pointer-events: none;
}
</style>
