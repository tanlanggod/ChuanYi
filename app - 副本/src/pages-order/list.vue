<template>
  <view class="container">
    <scroll-view class="status-tabs" scroll-x="true">
      <view class="tab-row">
        <view
          class="tab"
          :class="{ active: activeStatus === item.value }"
          v-for="item in statuses"
          :key="item.value || 'ALL'"
          @click="changeStatus(item.value)"
        >
          {{ item.label }}
        </view>
      </view>
    </scroll-view>

    <view class="card summary-card" v-if="!loading">
      <view class="summary-left">当前筛选：{{ activeStatusLabel }}</view>
      <view class="summary-right">共 {{ orders.length }} 笔</view>
    </view>

    <view class="card loading-card" v-if="loading">
      <view class="muted">订单加载中...</view>
    </view>

    <view v-else-if="orders.length === 0" class="empty-wrap">
      <image class="empty-image" src="/static/logo.png" mode="aspectFit"></image>
      <view class="empty-title">暂无订单数据</view>
      <view class="empty-sub">去设计或好物页下单后会在这里显示</view>
    </view>

    <view class="list" v-else>
      <view class="card order" v-for="item in orders" :key="item.orderNo" @click="openDetail(item.orderNo)">
        <view class="order-head">
          <view class="order-no">{{ item.orderNo }}</view>
          <view class="status-pill" :class="statusClass(item.orderStatus)">
            {{ formatOrderStatus(item.orderStatus) }}
          </view>
        </view>

        <view class="meta-line">实付金额：<text class="price">￥{{ formatAmount(item.payAmount) }}</text></view>
        <view class="meta-line">支付状态：{{ formatPayStatus(item.payStatus) }}</view>
        <view class="meta-line">下单时间：{{ formatTime(item.createdAt) }}</view>

        <view class="ops">
          <view class="btn-mini" @click.stop="openDetail(item.orderNo)">查看详情</view>
          <view class="btn-mini warn" v-if="item.orderStatus === 'PENDING_PAY'" @click.stop="cancelNow(item.orderNo)">
            取消订单
          </view>
          <view class="btn-mini primary" v-if="item.orderStatus === 'PENDING_PAY'" @click.stop="goPay(item.orderNo)">
            去支付
          </view>
          <view class="btn-mini primary" v-if="item.orderStatus === 'SHIPPED_WAIT_RECEIVE'" @click.stop="confirmNow(item.orderNo)">
            确认收货
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { cancelOrder, confirmReceipt, fetchOrderList } from "../common/api";
import { loginGuestIfNeeded } from "../common/session";

export default {
  data() {
    return {
      activeStatus: "",
      orders: [],
      loading: false,
      submittingMap: {},
      statuses: [
        { label: "全部", value: "" },
        { label: "待发货", value: "PAID_WAIT_SHIP" },
        { label: "待收货", value: "SHIPPED_WAIT_RECEIVE" },
        { label: "已完成", value: "COMPLETED" },
        { label: "退款/售后", value: "CANCELLED" },
      ],
    };
  },
  computed: {
    activeStatusLabel() {
      const current = this.statuses.find((item) => item.value === this.activeStatus);
      return current ? current.label : "全部";
    },
  },
  async onLoad(options) {
    const status = (options && options.status) || "";
    this.activeStatus = status;
  },
  async onShow() {
    await this.loadOrders();
  },
  async onPullDownRefresh() {
    await this.loadOrders();
    uni.stopPullDownRefresh();
  },
  methods: {
    async loadOrders() {
      try {
        this.loading = true;
        await loginGuestIfNeeded();
        const data = await fetchOrderList(this.activeStatus, 1, 50);
        this.orders = (data && data.list) || [];
      } catch (error) {
        uni.showToast({ title: "订单加载失败", icon: "none" });
      } finally {
        this.loading = false;
      }
    },
    async changeStatus(status) {
      if (this.activeStatus === status) {
        return;
      }
      this.activeStatus = status;
      await this.loadOrders();
    },
    openDetail(orderNo) {
      uni.navigateTo({ url: `/pages-order/detail?orderNo=${orderNo}` });
    },
    goPay(orderNo) {
      this.openDetail(orderNo);
    },
    isSubmitting(orderNo) {
      return !!this.submittingMap[orderNo];
    },
    setSubmitting(orderNo, value) {
      this.submittingMap = Object.assign({}, this.submittingMap, { [orderNo]: !!value });
    },
    cancelNow(orderNo) {
      if (this.isSubmitting(orderNo)) {
        return;
      }
      uni.showModal({
        title: "取消订单",
        content: "确认取消该订单吗？",
        success: async (res) => {
          if (!res.confirm) {
            return;
          }
          this.setSubmitting(orderNo, true);
          try {
            await cancelOrder(orderNo, "用户主动取消订单");
            uni.showToast({ title: "订单已取消", icon: "none" });
            await this.loadOrders();
          } catch (error) {
            uni.showToast({ title: "取消失败", icon: "none" });
          } finally {
            this.setSubmitting(orderNo, false);
          }
        },
      });
    },
    async confirmNow(orderNo) {
      if (this.isSubmitting(orderNo)) {
        return;
      }
      this.setSubmitting(orderNo, true);
      try {
        await confirmReceipt(orderNo, "用户确认收货");
        uni.showToast({ title: "订单已完成", icon: "none" });
        await this.loadOrders();
      } catch (error) {
        uni.showToast({ title: "确认收货失败", icon: "none" });
      } finally {
        this.setSubmitting(orderNo, false);
      }
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
  },
};
</script>

<style scoped>
.container {
  padding: 0 24rpx 24rpx;
  min-height: 100vh;
  box-sizing: border-box;
  background: #F9F7F3;
}

.status-tabs {
  white-space: nowrap;
  position: sticky;
  top: 0;
  z-index: 8;
  background: #F9F7F3;
  padding-top: 20rpx;
  margin-bottom: 12rpx;
}

.tab-row {
  display: inline-flex;
  gap: 32rpx;
  padding-bottom: 12rpx;
}

.tab {
  position: relative;
  padding: 6rpx 0 16rpx;
  color: #7A8B7C;
  font-size: 28rpx;
  transition: all 0.2s;
}

.tab.active {
  color: #1A241D;
  font-weight: 700;
}

.tab.active::after {
  content: "";
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: 0;
  width: 32rpx;
  height: 6rpx;
  background: #3B6E53;
  border-radius: 999rpx;
}

.summary-card {
  margin-top: 10rpx;
  padding: 24rpx 30rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: 20rpx;
  background: #fff;
  box-shadow: 0 4rpx 12rpx rgba(47, 54, 50, 0.04);
  border: 1rpx solid rgba(232, 228, 219, 0.6);
}

.summary-left {
  font-size: 26rpx;
  color: #1A241D;
  font-weight: 600;
}

.summary-right {
  font-size: 24rpx;
  color: #8C968F;
}

.loading-card {
  margin-top: 20rpx;
  min-height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(47, 54, 50, 0.04);
}

.empty-wrap {
  min-height: 66vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.empty-image {
  width: 240rpx;
  height: 240rpx;
  margin-bottom: 20rpx;
  opacity: 0.6;
}

.empty-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1A241D;
  margin-bottom: 12rpx;
}

.empty-sub {
  font-size: 24rpx;
  color: #8C968F;
}

.list {
  display: grid;
  gap: 20rpx;
  margin-top: 20rpx;
}

.order {
  padding: 30rpx;
  border-radius: 20rpx;
  background: #fff;
  box-shadow: 0 6rpx 16rpx rgba(47, 54, 50, 0.04);
  border: 1rpx solid rgba(232, 228, 219, 0.6);
  transition: transform 0.2s;
}

.order:active {
  transform: scale(0.98);
}

.order-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
  gap: 16rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid rgba(232, 228, 219, 0.4);
}

.order-no {
  flex: 1;
  min-width: 0;
  font-size: 26rpx;
  font-weight: 600;
  word-break: break-all;
  color: #1A241D;
}

.status-pill {
  border-radius: 8rpx;
  padding: 8rpx 16rpx;
  font-size: 22rpx;
  font-weight: 600;
  flex-shrink: 0;
}

.status-pill.pending {
  color: #A68754;
  background: #FDFBF7;
  border: 1rpx solid #E8E4DB;
}

.status-pill.ship {
  color: #3B6E53;
  background: #F0F5F2;
  border: 1rpx solid #D1E0D7;
}

.status-pill.receive {
  color: #556B5D;
  background: #E8E4DB;
}

.status-pill.done {
  color: #7A8B7C;
  background: #F9F7F3;
}

.status-pill.cancel {
  color: #8C968F;
  background: transparent;
  border: 1rpx solid rgba(232, 228, 219, 0.6);
}

.meta-line {
  margin-top: 10rpx;
  color: #556B5D;
  font-size: 24rpx;
  display: flex;
  align-items: center;
}

.price {
  color: #C2A578;
  font-weight: 700;
  font-size: 30rpx;
  font-family: -apple-system, BlinkMacSystemFont, "Helvetica Neue", Helvetica, Arial, sans-serif;
  margin-left: 6rpx;
}

.ops {
  margin-top: 24rpx;
  display: flex;
  justify-content: flex-end;
  gap: 16rpx;
}

.btn-mini {
  border-radius: 999rpx;
  background: #F9F7F3;
  color: #556B5D;
  padding: 12rpx 24rpx;
  font-size: 24rpx;
  font-weight: 500;
  border: 1rpx solid #E8E4DB;
  transition: all 0.2s;
}

.btn-mini.warn {
  color: #A68754;
  background: #FDFBF7;
  border-color: #E8E4DB;
}

.btn-mini.primary {
  color: #fff;
  background: linear-gradient(135deg, #4A8365 0%, #3B6E53 100%);
  border: none;
  box-shadow: 0 4rpx 10rpx rgba(59, 110, 83, 0.2);
}

.btn-mini:active {
  transform: scale(0.95);
}
</style>
