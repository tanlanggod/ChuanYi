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
  background: #F9F7F3;
}

.empty-wrap {
  min-height: 66vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.empty-icon {
  width: 200rpx;
  height: 200rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #F0F5F2 0%, #E3EBE6 100%);
  border: 4rpx solid #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 60rpx;
  font-weight: 800;
  color: #3B6E53;
  box-shadow: 0 10rpx 30rpx rgba(59, 110, 83, 0.08);
  font-family: "PingFang SC", "Microsoft YaHei", serif;
  letter-spacing: 4rpx;
}

.empty-title {
  margin-top: 40rpx;
  color: #1A241D;
  font-size: 32rpx;
  font-weight: 700;
  letter-spacing: 1rpx;
}

.empty-sub {
  margin-top: 12rpx;
  color: #8C968F;
  font-size: 24rpx;
  letter-spacing: 1rpx;
}

.empty-actions {
  margin-top: 40rpx;
  display: flex;
  gap: 20rpx;
}

.btn-outline {
  border: 2rpx solid #3B6E53;
  color: #3B6E53;
  border-radius: 999rpx;
  padding: 16rpx 40rpx;
  font-size: 26rpx;
  background: transparent;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-outline:active {
  background: #F0F5F2;
}

.list-wrap {
  display: grid;
  gap: 20rpx;
  padding-bottom: 20rpx;
}

.select-all-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 30rpx;
  border-radius: 20rpx;
}

.select-info {
  flex: 1;
}

.select-title {
  font-size: 30rpx;
  color: #1A241D;
  font-weight: 600;
}

.select-sub {
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #8C968F;
}

.check-btn {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  border: 2rpx solid #D1E0D7;
  color: transparent;
  text-align: center;
  line-height: 40rpx;
  font-size: 24rpx;
  font-weight: 700;
  flex-shrink: 0;
  transition: all 0.2s ease;
  background: #fff;
}

.check-btn.checked {
  border-color: #3B6E53;
  background: #3B6E53;
  color: #fff;
}

.item-list {
  display: grid;
  gap: 20rpx;
}

.item {
  padding: 30rpx;
  border-radius: 20rpx;
}

.item-main {
  display: flex;
  gap: 20rpx;
  align-items: flex-start;
}

.item-image {
  width: 160rpx;
  height: 160rpx;
  border-radius: 16rpx;
  background: #E8E4DB;
  flex-shrink: 0;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.04);
  border: 1rpx solid rgba(232, 228, 219, 0.5);
}

.item-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.item-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1A241D;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-sub {
  margin-top: 8rpx;
  color: #7A8B7C;
  font-size: 24rpx;
}

.status-tag {
  margin-top: 12rpx;
  display: inline-block;
  font-size: 20rpx;
  border-radius: 8rpx;
  padding: 4rpx 12rpx;
  align-self: flex-start;
}

.status-valid {
  color: #3B6E53;
  background: #F0F5F2;
  border: 1rpx solid #D1E0D7;
}

.status-invalid {
  color: #A68754;
  background: #FDFBF7;
  border: 1rpx solid #E8E4DB;
}

.invalid-tip {
  margin-top: 8rpx;
  color: #A68754;
  font-size: 22rpx;
}

.item-footer {
  margin-top: 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.qty-wrap {
  display: flex;
  align-items: center;
  gap: 16rpx;
  background: #F9F7F3;
  border-radius: 10rpx;
  padding: 4rpx;
  border: 1rpx solid rgba(232, 228, 219, 0.6);
}

.qty-btn {
  width: 44rpx;
  height: 44rpx;
  border-radius: 8rpx;
  background: #fff;
  text-align: center;
  line-height: 44rpx;
  color: #556B5D;
  font-weight: 500;
  box-shadow: 0 2rpx 6rpx rgba(0,0,0,0.02);
}

.qty-value {
  min-width: 40rpx;
  text-align: center;
  font-weight: 600;
  color: #2F3632;
  font-size: 26rpx;
}

.price {
  color: #C2A578;
  font-size: 34rpx;
  font-weight: 700;
  font-family: -apple-system, BlinkMacSystemFont, "Helvetica Neue", Helvetica, Arial, sans-serif;
}

.item-ops {
  margin-top: 16rpx;
  display: flex;
  justify-content: flex-end;
}

.op-delete {
  color: #A68754;
  font-size: 24rpx;
  padding: 10rpx 20rpx;
  background: #FDFBF7;
  border-radius: 999rpx;
  border: 1rpx solid #E8E4DB;
}

.bottom-space {
  height: 160rpx;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-top: 1rpx solid rgba(232, 228, 219, 0.6);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  box-shadow: 0 -10rpx 30rpx rgba(47, 54, 50, 0.04);
}

.total-line {
  font-size: 28rpx;
  color: #556B5D;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.total-price {
  color: #C2A578;
  font-weight: 700;
  font-size: 40rpx;
  font-family: -apple-system, BlinkMacSystemFont, "Helvetica Neue", Helvetica, Arial, sans-serif;
}

.checkout-btn {
  min-width: 280rpx;
  text-align: center;
  background: linear-gradient(135deg, #4A8365 0%, #3B6E53 100%);
  color: #fff;
  border-radius: 999rpx;
  padding: 20rpx 32rpx;
  font-size: 28rpx;
  font-weight: 600;
  box-shadow: 0 8rpx 20rpx rgba(59, 110, 83, 0.2);
  transition: all 0.2s ease;
}

.checkout-btn:active {
  transform: scale(0.98);
  box-shadow: 0 4rpx 10rpx rgba(59, 110, 83, 0.15);
}

.checkout-btn.disabled {
  background: #D1E0D7;
  color: #F0F5F2;
  box-shadow: none;
  transform: none;
}
</style>
