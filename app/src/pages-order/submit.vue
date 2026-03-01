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
  padding-bottom: 170rpx;
}

.loading-card {
  min-height: 220rpx;
  display: grid;
  place-items: center;
}

.address-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  border: 1rpx solid #ebeff4;
}

.address-main {
  flex: 1;
  min-width: 0;
}

.address-line1 {
  display: flex;
  gap: 12rpx;
  align-items: center;
  margin-bottom: 8rpx;
}

.address-name,
.address-phone {
  font-size: 30rpx;
  color: #111827;
}

.address-name {
  font-weight: 700;
}

.address-tag {
  background: #fee2e2;
  color: #b91c1c;
  border-radius: 999rpx;
  padding: 4rpx 12rpx;
  font-size: 20rpx;
}

.address-detail {
  color: #6b7280;
  font-size: 22rpx;
  line-height: 1.6;
}

.arrow {
  color: #9aa0aa;
  font-size: 36rpx;
}

.item-card {
  margin-top: 12rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.item-image {
  width: 110rpx;
  height: 110rpx;
  border-radius: 50%;
  background: #f3f4f6;
  flex-shrink: 0;
}

.item-main {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #111827;
}

.item-sub {
  margin-top: 6rpx;
  color: #8a8f99;
  font-size: 22rpx;
}

.item-price {
  margin-top: 8rpx;
  color: #d62b2b;
  font-size: 34rpx;
  font-weight: 700;
}

.qty {
  color: #8a8f99;
  font-size: 24rpx;
}

.remark-card {
  margin-top: 12rpx;
}

.remark-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;
}

.remark-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #111827;
}

.remark {
  width: 100%;
  min-height: 140rpx;
}

.bottom-space {
  height: 20rpx;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  border-top: 1rpx solid #eceff3;
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14rpx;
  box-shadow: 0 -8rpx 26rpx rgba(15, 23, 42, 0.06);
}

.pay-line {
  font-size: 28rpx;
  color: #374151;
}

.pay {
  color: #d62b2b;
  font-size: 40rpx;
  font-weight: 700;
}

.submit-btn {
  min-width: 260rpx;
  text-align: center;
  background: #d62b2b;
  color: #fff;
  font-size: 30rpx;
  border-radius: 999rpx;
  padding: 18rpx 24rpx;
  font-weight: 700;
}

.submit-btn.disabled {
  background: #c5ccd6;
}
</style>
