<template>
  <view class="container">
    <view class="empty-wrap" v-if="items.length === 0">
      <view class="empty-icon">购物</view>
      <view class="empty-title">购物车暂无商品</view>
      <view class="empty-sub">先去挑选喜欢的手串和配件吧</view>
      <view class="empty-actions">
        <view class="btn-outline" @click="goDIY">去定制</view>
        <view class="btn-outline" @click="goGoods">找好物</view>
      </view>
    </view>

    <view class="list-wrap" v-else>
      <view class="card select-all-row" @click="toggleSelectAll">
        <view class="check-btn" :class="{ checked: allSelected }">{{ allSelected ? "√" : "" }}</view>
        <view class="select-info">
          <view class="select-title">{{ allSelected ? "已全选，可点击取消" : "全选商品" }}</view>
          <view class="select-sub">共 {{ items.length }} 件，已选 {{ selectedCount }} 件</view>
        </view>
      </view>

      <view class="item-list">
        <view class="card item" v-for="item in items" :key="item.cartItemId">
          <view class="item-main">
            <view class="check-btn" :class="{ checked: item.selected }" @click="toggleSelect(item)">
              {{ item.selected ? "√" : "" }}
            </view>
            <image class="item-image" :src="resolveImage(item.imageUrl)" mode="aspectFill"></image>

            <view class="item-body">
              <view class="item-title">{{ item.title || "未命名商品" }}</view>
              <view class="item-sub">{{ formatItemType(item.itemType) }}</view>

              <view class="status-tag" :class="statusClass(item.validityStatus)">
                {{ formatValidityStatus(item.validityStatus) }}
              </view>
              <view class="invalid-tip" v-if="item.invalidReason">{{ item.invalidReason }}</view>

              <view class="item-footer">
                <view class="qty-wrap">
                  <view class="qty-btn" @click="changeQty(item, -1)">-</view>
                  <view class="qty-value">{{ item.qty }}</view>
                  <view class="qty-btn" @click="changeQty(item, 1)">+</view>
                </view>
                <view class="price">￥{{ formatAmount(item.amount) }}</view>
              </view>

              <view class="item-ops">
                <view class="op-delete" @click="removeItem(item)">删除</view>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="bottom-space"></view>
    <view class="bottom-bar">
      <view class="total-line">
        合计: <text class="total-price">￥{{ formatAmount(summary.payAmount) }}</text>
      </view>
      <view class="checkout-btn" :class="{ disabled: checkoutDisabled }" @click="submitCartOrder">
        去结算（{{ selectedCount }}件）
      </view>
    </view>
  </view>
</template>

<script>
import {
  createOrderFromCart,
  deleteCartItem,
  fetchAddressList,
  fetchCartDetail,
  selectAllCartItems,
  selectCartItem,
  updateCartItemQty,
} from "../../common/api";
import { resolveAssetUrl } from "../../common/http";
import { loginGuestIfNeeded } from "../../common/session";

export default {
  data() {
    return {
      items: [],
      summary: {},
      submitting: false,
    };
  },
  computed: {
    allSelected() {
      return this.items.length > 0 && this.items.every((item) => !!item.selected);
    },
    selectedCount() {
      const count = Number(this.summary.selectedCount || 0);
      if (count > 0) {
        return count;
      }
      return this.items.filter((item) => item.selected).length;
    },
    checkoutDisabled() {
      if (this.items.length === 0 || this.selectedCount <= 0) {
        return true;
      }
      return !!this.summary.checkoutStatus && this.summary.checkoutStatus !== "READY";
    },
  },
  async onShow() {
    await this.loadCart();
  },
  async onPullDownRefresh() {
    await this.loadCart();
    uni.stopPullDownRefresh();
  },
  methods: {
    async loadCart() {
      try {
        await loginGuestIfNeeded();
        const data = await fetchCartDetail();
        this.items = (data && data.items) || [];
        this.summary = (data && data.summary) || {};
      } catch (error) {
        uni.showToast({ title: "购物车加载失败", icon: "none" });
      }
    },
    async changeQty(item, delta) {
      const nextQty = Number(item.qty || 1) + delta;
      if (nextQty < 1) {
        return;
      }
      try {
        await updateCartItemQty(item.cartItemId, nextQty);
        await this.loadCart();
      } catch (error) {
        uni.showToast({ title: "数量更新失败", icon: "none" });
      }
    },
    async removeItem(item) {
      try {
        await deleteCartItem(item.cartItemId);
        await this.loadCart();
      } catch (error) {
        uni.showToast({ title: "删除失败", icon: "none" });
      }
    },
    async toggleSelect(item) {
      try {
        await selectCartItem(item.cartItemId, !item.selected);
        await this.loadCart();
      } catch (error) {
        uni.showToast({ title: "选择失败", icon: "none" });
      }
    },
    async toggleSelectAll() {
      if (this.items.length === 0) {
        return;
      }
      try {
        await selectAllCartItems(!this.allSelected);
        await this.loadCart();
      } catch (error) {
        uni.showToast({ title: "全选失败", icon: "none" });
      }
    },
    async submitCartOrder() {
      if (this.submitting || this.checkoutDisabled) {
        if (this.summary.checkoutStatus && this.summary.checkoutStatus !== "READY") {
          uni.showToast({ title: this.formatCheckoutStatus(this.summary.checkoutStatus), icon: "none" });
        }
        return;
      }
      try {
        const addressData = await fetchAddressList();
        const addresses = (addressData && addressData.addresses) || [];
        if (addresses.length === 0) {
          uni.showToast({ title: "请先添加收货地址", icon: "none" });
          setTimeout(() => {
            uni.navigateTo({ url: "/pages-user/address-edit?from=order-submit" });
          }, 280);
          return;
        }
        const defaultAddress = addresses.find((item) => Number(item.isDefault) === 1) || addresses[0];
        const selectedIds = this.items.filter((item) => item.selected).map((item) => item.cartItemId);
        if (selectedIds.length === 0) {
          uni.showToast({ title: "请先选择商品", icon: "none" });
          return;
        }
        this.submitting = true;
        const result = await createOrderFromCart({
          addressId: defaultAddress.id,
          cartItemIds: selectedIds,
          remark: "购物车下单",
        });
        if (result && result.orderNo) {
          uni.navigateTo({ url: `/pages-order/detail?orderNo=${result.orderNo}` });
        }
      } catch (error) {
        uni.showToast({ title: "提交订单失败", icon: "none" });
      } finally {
        this.submitting = false;
      }
    },
    goGoods() {
      uni.switchTab({ url: "/pages/goods/index" });
    },
    goDIY() {
      uni.switchTab({ url: "/pages/diy/index" });
    },
    formatAmount(value) {
      const text = Number(value || 0).toFixed(2);
      return text.replace(/\.00$/, "").replace(/(\.\d)0$/, "$1");
    },
    formatItemType(value) {
      const map = {
        NORMAL_GOODS: "普通商品",
        DIY_SNAPSHOT: "定制手串",
        CUSTOM_BRACELET: "定制手串",
      };
      return map[value] || value || "-";
    },
    statusClass(value) {
      if (value === "VALID") {
        return "status-valid";
      }
      return "status-invalid";
    },
    formatValidityStatus(value) {
      const map = {
        VALID: "有效",
        INVALID_OFF_SALE: "已下架",
        INVALID_OUT_OF_STOCK: "库存不足",
        INVALID_PRICE_CHANGED: "价格变更",
        INVALID_NOT_FOUND: "商品不存在",
      };
      return map[value] || value || "-";
    },
    formatCheckoutStatus(value) {
      const map = {
        READY: "可结算",
        BLOCKED_EMPTY: "购物车为空",
        BLOCKED_NO_SELECTION: "未选择商品",
        BLOCKED_INVALID_ITEMS: "存在失效商品",
        BLOCKED_NO_ADDRESS: "未设置地址",
        BLOCKED_STOCK_CHANGED: "库存发生变化",
      };
      return map[value] || value || "-";
    },
    resolveImage(url) {
      if (!url) {
        return "/static/logo.png";
      }
      return resolveAssetUrl(url);
    },
  },
};
</script>

<style scoped>
.container {
  padding: 24rpx;
  min-height: 100vh;
  box-sizing: border-box;
}

.empty-wrap {
  min-height: 66vh;
  display: grid;
  place-items: center;
  align-content: center;
  text-align: center;
}

.empty-icon {
  width: 220rpx;
  height: 220rpx;
  border-radius: 50%;
  background: linear-gradient(140deg, #fff5f5 0%, #ffe6e6 100%);
  border: 2rpx solid #ffd5d5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 56rpx;
  font-weight: 700;
  color: #d62b2b;
}

.empty-title {
  margin-top: 20rpx;
  color: #374151;
  font-size: 30rpx;
  font-weight: 700;
}

.empty-sub {
  margin-top: 8rpx;
  color: #9ca3af;
  font-size: 24rpx;
}

.empty-actions {
  margin-top: 22rpx;
  display: flex;
  gap: 14rpx;
}

.btn-outline {
  border: 1px solid #d62b2b;
  color: #d62b2b;
  border-radius: 999rpx;
  padding: 12rpx 30rpx;
  font-size: 24rpx;
  background: #fff;
}

.list-wrap {
  display: grid;
  gap: 12rpx;
}

.select-all-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 18rpx 20rpx;
}

.select-info {
  flex: 1;
}

.select-title {
  font-size: 28rpx;
  color: #111827;
  font-weight: 700;
}

.select-sub {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #8a8f99;
}

.check-btn {
  width: 42rpx;
  height: 42rpx;
  border-radius: 50%;
  border: 2rpx solid #c7c9cf;
  color: #fff;
  text-align: center;
  line-height: 38rpx;
  font-size: 24rpx;
  font-weight: 700;
  flex-shrink: 0;
}

.check-btn.checked {
  border-color: #d62b2b;
  background: #d62b2b;
}

.item-list {
  display: grid;
  gap: 12rpx;
}

.item {
  padding: 20rpx;
}

.item-main {
  display: flex;
  gap: 12rpx;
  align-items: flex-start;
}

.item-image {
  width: 132rpx;
  height: 132rpx;
  border-radius: 12rpx;
  background: #f3f4f6;
  flex-shrink: 0;
}

.item-body {
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

.status-tag {
  margin-top: 10rpx;
  display: inline-block;
  font-size: 20rpx;
  border-radius: 999rpx;
  padding: 4rpx 12rpx;
}

.status-valid {
  color: #047857;
  background: #ecfdf5;
}

.status-invalid {
  color: #b45309;
  background: #fffbeb;
}

.invalid-tip {
  margin-top: 8rpx;
  color: #b45309;
  font-size: 22rpx;
}

.item-footer {
  margin-top: 12rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.qty-wrap {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.qty-btn {
  width: 42rpx;
  height: 42rpx;
  border-radius: 10rpx;
  background: #eef1f5;
  text-align: center;
  line-height: 42rpx;
  color: #374151;
}

.qty-value {
  min-width: 34rpx;
  text-align: center;
  font-weight: 600;
}

.price {
  color: #d62b2b;
  font-size: 32rpx;
  font-weight: 700;
}

.item-ops {
  margin-top: 8rpx;
  display: flex;
  justify-content: flex-end;
}

.op-delete {
  color: #c82a2a;
  font-size: 22rpx;
}

.bottom-space {
  height: 130rpx;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 14rpx 24rpx;
  padding-bottom: calc(14rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #ebeff3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  box-shadow: 0 -8rpx 26rpx rgba(15, 23, 42, 0.06);
}

.total-line {
  font-size: 30rpx;
  color: #1f2937;
}

.total-price {
  color: #d62b2b;
  font-weight: 700;
}

.checkout-btn {
  min-width: 290rpx;
  text-align: center;
  background: #d62b2b;
  color: #fff;
  border-radius: 999rpx;
  padding: 18rpx 24rpx;
  font-size: 28rpx;
  font-weight: 700;
}

.checkout-btn.disabled {
  background: #c5ccd6;
}
</style>
