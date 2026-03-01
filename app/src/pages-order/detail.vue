<template>
  <view class="container">
    <view class="card loading-card" v-if="loading">
      <view class="muted">订单信息加载中...</view>
    </view>

    <view class="card loading-card" v-else-if="!order.orderNo">
      <view class="muted">未找到该订单</view>
    </view>

    <template v-else>
      <view class="card status-card">
        <view class="status-top">
          <view class="status-title">{{ formatOrderStatus(order.orderStatus) }}</view>
          <view class="status-chip" :class="statusClass(order.orderStatus)">{{ formatPayStatus(order.payStatus) }}</view>
        </view>
        <view class="status-desc">{{ statusDescription(order.orderStatus) }}</view>
        <view class="amount-line">实付 ￥<text class="amount">{{ formatAmount(order.payAmount) }}</text></view>
        <view class="status-meta">订单号：{{ order.orderNo }}</view>
        <view class="status-meta">下单时间：{{ formatTime(order.createdAt) }}</view>
        <view class="status-meta" v-if="order.paidAt">支付时间：{{ formatTime(order.paidAt) }}</view>
      </view>

      <view class="card address-card">
        <view class="block-title">收货地址</view>
        <view class="address-name">
          {{ addressSnapshot.receiverName || "-" }} {{ addressSnapshot.receiverPhone || "" }}
        </view>
        <view class="address-detail">
          {{ addressSnapshot.province || "" }} {{ addressSnapshot.city || "" }} {{ addressSnapshot.district || "" }}
          {{ addressSnapshot.detailAddress || "" }}
        </view>
      </view>

      <view class="block-title section-gap">商品明细</view>
      <view class="list" v-if="order.items && order.items.length > 0">
        <view class="card item" v-for="(item, idx) in order.items" :key="idx">
          <image class="item-image" :src="resolveImage(item.itemImageUrl)" mode="aspectFill"></image>
          <view class="item-main">
            <view class="item-name">{{ item.itemName || "商品" }}</view>
            <view class="item-type">{{ formatItemType(item.itemType) }}</view>
            <view class="item-bottom">
              <view class="item-qty">x{{ item.qty || 1 }}</view>
              <view class="item-price">￥{{ formatAmount(item.amount || item.unitPrice) }}</view>
            </view>
          </view>
        </view>
      </view>
      <view class="card" v-else>
        <view class="muted">暂无商品信息</view>
      </view>

      <view class="card amount-card">
        <view class="amount-row">
          <view class="label">商品总额</view>
          <view class="value">￥{{ formatAmount(order.totalAmount) }}</view>
        </view>
        <view class="amount-row">
          <view class="label">实付金额</view>
          <view class="value pay">￥{{ formatAmount(order.payAmount) }}</view>
        </view>
        <view class="amount-row" v-if="order.remark">
          <view class="label">订单备注</view>
          <view class="value note">{{ order.remark }}</view>
        </view>
      </view>

      <view class="block-title section-gap" v-if="order.shipping && order.shipping.trackingNo">物流轨迹</view>
      <view class="card" v-if="order.shipping && order.shipping.trackingNo">
        <view class="track-head">
          <view class="track-no">物流单号：{{ order.shipping.trackingNo }}</view>
          <view class="track-refresh" @click="loadLogistics">刷新</view>
        </view>
        <view class="muted" v-if="logisticsLoading">物流信息加载中...</view>
        <view class="track-list" v-else-if="logistics.nodes && logistics.nodes.length > 0">
          <view class="track-item" v-for="(node, idx) in logistics.nodes" :key="idx">
            <view class="track-dot" :class="{ active: idx === 0 }"></view>
            <view class="track-main">
              <view class="track-title">{{ formatTrackStatus(node.status) }}</view>
              <view class="track-desc">{{ node.location }} {{ node.description }}</view>
              <view class="track-time">{{ formatTime(node.time) }}</view>
            </view>
          </view>
        </view>
        <view class="muted" v-else>暂无物流轨迹</view>
      </view>

      <view class="block-title section-gap">订单日志</view>
      <view class="list" v-if="order.logs && order.logs.length > 0">
        <view class="card log" v-for="(log, idx) in order.logs" :key="idx">
          <view class="log-title">{{ formatLogAction(log.action) }}</view>
          <view class="log-status">状态：{{ formatOrderStatus(log.toStatus) }}</view>
          <view class="log-time">{{ formatTime(log.createdAt) }}</view>
        </view>
      </view>
      <view class="card" v-else>
        <view class="muted">暂无日志</view>
      </view>
    </template>

    <view class="bottom-space"></view>
    <view class="footer" v-if="order.orderNo">
      <view class="btn-ghost" @click="goOrders">订单列表</view>
      <view class="btn-danger-outline" v-if="order.orderStatus === 'PENDING_PAY'" @click="cancelNow">取消订单</view>
      <view class="btn-primary" v-if="order.orderStatus === 'PENDING_PAY'" @click="mockPayNow">
        {{ submitting ? "处理中..." : "去支付" }}
      </view>
      <view class="btn-primary" v-else-if="order.orderStatus === 'SHIPPED_WAIT_RECEIVE'" @click="confirmNow">
        {{ submitting ? "处理中..." : "确认收货" }}
      </view>
      <view class="btn-primary" v-else @click="goGoods">再去逛逛</view>
    </view>
  </view>
</template>

<script>
import {
  cancelOrder,
  confirmReceipt,
  fetchLogisticsTrack,
  fetchOrderDetail,
  mockPay,
  mockPayCallback,
} from "../common/api";
import { resolveAssetUrl } from "../common/http";
import { loginGuestIfNeeded } from "../common/session";

export default {
  data() {
    return {
      orderNo: "",
      order: {},
      loading: false,
      logistics: {
        latestStatus: "",
        nodes: [],
      },
      logisticsLoading: false,
      submitting: false,
    };
  },
  computed: {
    addressSnapshot() {
      return this.order && this.order.addressSnapshot ? this.order.addressSnapshot : {};
    },
  },
  async onLoad(options) {
    this.orderNo = (options && options.orderNo) || "";
    await this.loadDetail();
  },
  async onPullDownRefresh() {
    await this.loadDetail();
    uni.stopPullDownRefresh();
  },
  methods: {
    async loadDetail() {
      if (!this.orderNo) {
        return;
      }
      try {
        this.loading = true;
        await loginGuestIfNeeded();
        const data = await fetchOrderDetail(this.orderNo);
        this.order = data || {};
        await this.loadLogistics();
      } catch (error) {
        uni.showToast({ title: "订单加载失败", icon: "none" });
      } finally {
        this.loading = false;
      }
    },
    async loadLogistics() {
      const shipping = this.order && this.order.shipping;
      const carrierCode = shipping && shipping.carrierCode;
      const trackingNo = shipping && shipping.trackingNo;
      if (!carrierCode || !trackingNo) {
        this.logistics = { latestStatus: "", nodes: [] };
        return;
      }
      this.logisticsLoading = true;
      try {
        const data = await fetchLogisticsTrack(carrierCode, trackingNo);
        const nodes = (data && data.nodes) || [];
        this.logistics = {
          latestStatus: (data && data.latestStatus) || "",
          nodes: [...nodes].sort((a, b) => String(b.time || "").localeCompare(String(a.time || ""))),
        };
      } catch (_error) {
        this.logistics = { latestStatus: "", nodes: [] };
      } finally {
        this.logisticsLoading = false;
      }
    },
    async mockPayNow() {
      if (this.submitting) {
        return;
      }
      this.submitting = true;
      try {
        const payData = await mockPay(this.orderNo);
        await mockPayCallback(payData.payNo, this.orderNo, "SUCCESS");
        uni.showToast({ title: "支付成功", icon: "none" });
        await this.loadDetail();
      } catch (error) {
        uni.showToast({ title: "支付失败", icon: "none" });
      } finally {
        this.submitting = false;
      }
    },
    async confirmNow() {
      if (this.submitting) {
        return;
      }
      this.submitting = true;
      try {
        await confirmReceipt(this.orderNo, "用户确认收货");
        uni.showToast({ title: "订单已完成", icon: "none" });
        await this.loadDetail();
      } catch (error) {
        uni.showToast({ title: "确认收货失败", icon: "none" });
      } finally {
        this.submitting = false;
      }
    },
    cancelNow() {
      if (!this.orderNo || this.submitting) {
        return;
      }
      uni.showModal({
        title: "取消订单",
        content: "确认取消该订单吗？",
        success: async (res) => {
          if (!res.confirm) {
            return;
          }
          this.submitting = true;
          try {
            await cancelOrder(this.orderNo, "用户主动取消订单");
            uni.showToast({ title: "订单已取消", icon: "none" });
            await this.loadDetail();
          } catch (error) {
            uni.showToast({ title: "取消失败", icon: "none" });
          } finally {
            this.submitting = false;
          }
        },
      });
    },
    goOrders() {
      uni.navigateTo({ url: "/pages-order/list" });
    },
    goGoods() {
      uni.switchTab({ url: "/pages/goods/index" });
    },
    statusClass(status) {
      const map = {
        PENDING_PAY: "pending",
        PAID_WAIT_SHIP: "ship",
        SHIPPED_WAIT_RECEIVE: "receive",
        COMPLETED: "done",
        CANCELLED: "cancel",
      };
      return map[status] || "pending";
    },
    statusDescription(status) {
      const map = {
        PENDING_PAY: "订单已创建，请尽快完成支付。",
        PAID_WAIT_SHIP: "订单已支付成功，等待商家发货。",
        SHIPPED_WAIT_RECEIVE: "商品已发出，请注意查收物流信息。",
        COMPLETED: "订单已完成，感谢您的购买。",
        CANCELLED: "订单已取消，如有需要可重新下单。",
      };
      return map[status] || "";
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
    formatTime(value) {
      if (!value) {
        return "-";
      }
      return String(value).replace("T", " ").slice(0, 16);
    },
    formatOrderStatus(status) {
      const map = {
        PENDING_PAY: "待支付",
        PAID_WAIT_SHIP: "待发货",
        SHIPPED_WAIT_RECEIVE: "待收货",
        COMPLETED: "已完成",
        CANCELLED: "已取消",
      };
      return map[status] || status || "-";
    },
    formatPayStatus(status) {
      const map = {
        UNPAID: "未支付",
        PAID: "已支付",
        REFUNDED: "已退款",
        PARTIAL_REFUNDED: "部分退款",
      };
      return map[status] || status || "-";
    },
    formatItemType(status) {
      const map = {
        NORMAL_GOODS: "普通商品",
        DIY_SNAPSHOT: "定制手串",
        CUSTOM_BRACELET: "定制手串",
      };
      return map[status] || status || "-";
    },
    formatLogAction(action) {
      const map = {
        CREATE_ORDER: "创建订单",
        PAY_SUCCESS: "支付成功",
        SHIP_ORDER: "订单发货",
        CONFIRM_RECEIPT: "确认收货",
        CANCEL_ORDER: "取消订单",
      };
      return map[action] || action || "-";
    },
    formatTrackStatus(status) {
      const map = {
        PENDING_PAY: "待支付",
        WAITING_FOR_SHIPMENT: "待揽件",
        IN_TRANSIT: "运输中",
        DELIVERED: "已签收",
        CANCELLED: "已取消",
        ORDER_CREATED: "订单创建",
        PAY_SUCCESS: "支付成功",
      };
      return map[status] || status || "-";
    },
  },
};
</script>

<style scoped>
.container {
  padding: 24rpx;
  padding-bottom: 190rpx;
}

.loading-card {
  min-height: 220rpx;
  display: grid;
  place-items: center;
}

.status-card {
  background: linear-gradient(140deg, #fff, #fff7f7);
  border: 1rpx solid #ffe5e5;
}

.status-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10rpx;
}

.status-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #111827;
}

.status-chip {
  border-radius: 999rpx;
  padding: 6rpx 14rpx;
  font-size: 22rpx;
  font-weight: 700;
}

.status-chip.pending {
  color: #b45309;
  background: #fffbeb;
}

.status-chip.ship {
  color: #1d4ed8;
  background: #eff6ff;
}

.status-chip.receive {
  color: #0f766e;
  background: #ecfeff;
}

.status-chip.done {
  color: #047857;
  background: #ecfdf5;
}

.status-chip.cancel {
  color: #b91c1c;
  background: #fef2f2;
}

.status-desc {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 23rpx;
}

.amount-line {
  margin-top: 12rpx;
  color: #374151;
  font-size: 28rpx;
}

.amount {
  color: #d62b2b;
  font-size: 40rpx;
  font-weight: 700;
}

.status-meta {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 22rpx;
}

.address-card {
  margin-top: 12rpx;
}

.block-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #111827;
}

.section-gap {
  margin: 16rpx 0 10rpx;
}

.address-name {
  margin-top: 10rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: #111827;
}

.address-detail {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 23rpx;
  line-height: 1.6;
}

.list {
  display: grid;
  gap: 12rpx;
}

.item {
  display: flex;
  gap: 12rpx;
  align-items: center;
}

.item-image {
  width: 110rpx;
  height: 110rpx;
  border-radius: 12rpx;
  background: #f3f4f6;
  flex-shrink: 0;
}

.item-main {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 28rpx;
  font-weight: 700;
  color: #111827;
}

.item-type {
  margin-top: 6rpx;
  color: #8a8f99;
  font-size: 22rpx;
}

.item-bottom {
  margin-top: 10rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-qty {
  color: #8a8f99;
  font-size: 22rpx;
}

.item-price {
  color: #d62b2b;
  font-weight: 700;
  font-size: 30rpx;
}

.amount-card {
  margin-top: 12rpx;
}

.amount-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14rpx;
  margin-bottom: 10rpx;
}

.amount-row:last-child {
  margin-bottom: 0;
}

.label {
  color: #6b7280;
  font-size: 23rpx;
}

.value {
  color: #111827;
  font-size: 24rpx;
  text-align: right;
}

.value.pay {
  color: #d62b2b;
  font-weight: 700;
}

.value.note {
  color: #4b5563;
  max-width: 460rpx;
  line-height: 1.6;
}

.track-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;
  gap: 10rpx;
}

.track-no {
  flex: 1;
  min-width: 0;
  color: #6b7280;
  font-size: 22rpx;
  word-break: break-all;
}

.track-refresh {
  color: #d62b2b;
  font-size: 22rpx;
}

.track-list {
  display: grid;
  gap: 10rpx;
}

.track-item {
  display: flex;
  gap: 12rpx;
}

.track-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #d1d5db;
  margin-top: 10rpx;
  flex-shrink: 0;
}

.track-dot.active {
  background: #d62b2b;
}

.track-main {
  flex: 1;
  padding-bottom: 12rpx;
  border-bottom: 1rpx solid #f1f3f5;
}

.track-title {
  font-size: 25rpx;
  font-weight: 700;
  color: #1f2937;
}

.track-desc,
.track-time {
  margin-top: 6rpx;
  color: #6b7280;
  font-size: 22rpx;
}

.log-title {
  font-size: 26rpx;
  font-weight: 700;
  color: #111827;
}

.log-status,
.log-time {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 22rpx;
}

.bottom-space {
  height: 40rpx;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  border-top: 1rpx solid #eceff3;
  padding: 14rpx 20rpx;
  padding-bottom: calc(14rpx + env(safe-area-inset-bottom));
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10rpx;
  box-shadow: 0 -8rpx 26rpx rgba(15, 23, 42, 0.06);
}

.btn-ghost,
.btn-danger-outline,
.btn-primary {
  border-radius: 999rpx;
  min-height: 70rpx;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 24rpx;
  font-weight: 700;
  box-sizing: border-box;
}

.btn-ghost {
  border: 1px solid #d1d5db;
  color: #374151;
  background: #fff;
}

.btn-danger-outline {
  border: 1px solid #ef4444;
  color: #ef4444;
  background: #fff;
}

.btn-primary {
  background: #d62b2b;
  color: #fff;
  border: 1px solid #d62b2b;
}
</style>
