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
}

.status-tabs {
  white-space: nowrap;
  position: sticky;
  top: 0;
  z-index: 8;
  background: #f4f5f7;
  padding-top: 12rpx;
}

.tab-row {
  display: inline-flex;
  gap: 28rpx;
  padding-bottom: 8rpx;
}

.tab {
  position: relative;
  padding: 6rpx 0 12rpx;
  color: #6b7280;
  font-size: 26rpx;
}

.tab.active {
  color: #d62b2b;
  font-weight: 700;
}

.tab.active::after {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 4rpx;
  background: #d62b2b;
  border-radius: 999rpx;
}

.summary-card {
  margin-top: 8rpx;
  padding: 18rpx 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary-left {
  font-size: 24rpx;
  color: #4b5563;
}

.summary-right {
  font-size: 22rpx;
  color: #9ca3af;
}

.loading-card {
  margin-top: 12rpx;
  min-height: 220rpx;
  display: grid;
  place-items: center;
}

.empty-wrap {
  min-height: 66vh;
  display: grid;
  place-items: center;
  align-content: center;
}

.empty-image {
  width: 210rpx;
  height: 210rpx;
}

.empty-title {
  margin-top: 12rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: #374151;
}

.empty-sub {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #9ca3af;
}

.list {
  display: grid;
  gap: 12rpx;
  margin-top: 12rpx;
}

.order {
  padding-top: 20rpx;
  padding-bottom: 20rpx;
}

.order-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;
  gap: 10rpx;
}

.order-no {
  flex: 1;
  min-width: 0;
  font-size: 25rpx;
  font-weight: 700;
  word-break: break-all;
  color: #111827;
}

.status-pill {
  border-radius: 999rpx;
  padding: 6rpx 14rpx;
  font-size: 20rpx;
  font-weight: 700;
  flex-shrink: 0;
}

.status-pill.pending {
  color: #b45309;
  background: #fffbeb;
}

.status-pill.ship {
  color: #1d4ed8;
  background: #eff6ff;
}

.status-pill.receive {
  color: #0f766e;
  background: #ecfeff;
}

.status-pill.done {
  color: #047857;
  background: #ecfdf5;
}

.status-pill.cancel {
  color: #b91c1c;
  background: #fef2f2;
}

.meta-line {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 23rpx;
}

.price {
  color: #d62b2b;
  font-weight: 700;
}

.ops {
  margin-top: 12rpx;
  display: flex;
  justify-content: flex-end;
  gap: 10rpx;
}

.btn-mini {
  border-radius: 999rpx;
  background: #eef1f5;
  color: #1f2937;
  padding: 8rpx 18rpx;
  font-size: 22rpx;
}

.btn-mini.warn {
  color: #b42318;
  background: #fef2f2;
}

.btn-mini.primary {
  color: #fff;
  background: #d62b2b;
}
</style>
