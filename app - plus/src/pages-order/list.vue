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
  min-height: 100vh;
  box-sizing: border-box;
  padding: 0 24rpx 24rpx;
}

.status-tabs {
  position: sticky;
  top: 0;
  z-index: 8;
  white-space: nowrap;
  margin-bottom: 12rpx;
  padding-top: 20rpx;
  background: rgba(245, 241, 232, 0.92);
  backdrop-filter: blur(8px);
}

.tab-row {
  display: inline-flex;
  gap: 20rpx;
  padding-bottom: 10rpx;
}

.tab {
  padding: 10rpx 24rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.2);
  background: rgba(255, 252, 246, 0.88);
  color: #708278;
  font-size: 24rpx;
}

.tab.active {
  border-color: transparent;
  background: linear-gradient(135deg, #5a8c72 0%, #3e6f57 100%);
  color: #fff;
  font-weight: 600;
}

.summary-card,
.loading-card,
.order {
  border-radius: 24rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.2);
  background: linear-gradient(160deg, #fffdfa 0%, #f8f1e6 100%);
  box-shadow: 0 10rpx 22rpx rgba(35, 56, 45, 0.08);
}

.summary-card {
  margin-top: 10rpx;
  padding: 24rpx 28rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary-left {
  color: #1f2f27;
  font-size: 26rpx;
  font-weight: 600;
}

.summary-right {
  color: #839188;
  font-size: 24rpx;
}

.loading-card {
  margin-top: 20rpx;
  min-height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;
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
  margin-bottom: 10rpx;
  color: #1f2f27;
  font-size: 32rpx;
  font-weight: 700;
}

.empty-sub {
  color: #85938a;
  font-size: 24rpx;
}

.list {
  margin-top: 20rpx;
  display: grid;
  gap: 20rpx;
}

.order {
  padding: 28rpx;
}

.order:active {
  transform: scale(0.985);
}

.order-head {
  margin-bottom: 18rpx;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid rgba(185, 152, 100, 0.2);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14rpx;
}

.order-no {
  flex: 1;
  min-width: 0;
  color: #1f2f27;
  font-size: 26rpx;
  font-weight: 600;
  word-break: break-all;
}

.status-pill {
  flex-shrink: 0;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 600;
}

.status-pill.pending {
  color: #a68450;
  background: #fff3e1;
}

.status-pill.ship {
  color: #3e6f57;
  background: #edf4ef;
}

.status-pill.receive {
  color: #486357;
  background: #e7efe9;
}

.status-pill.done {
  color: #74837a;
  background: #f4efe5;
}

.status-pill.cancel {
  color: #8a968e;
  border: 1rpx solid rgba(185, 152, 100, 0.24);
  background: rgba(255, 252, 246, 0.86);
}

.meta-line {
  margin-top: 8rpx;
  color: #5d7265;
  font-size: 24rpx;
  display: flex;
  align-items: center;
}

.price {
  margin-left: 6rpx;
  color: #b38d58;
  font-size: 30rpx;
  font-weight: 700;
}

.ops {
  margin-top: 22rpx;
  display: flex;
  justify-content: flex-end;
  gap: 12rpx;
  flex-wrap: wrap;
}

.btn-mini {
  padding: 12rpx 22rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.26);
  background: rgba(255, 252, 246, 0.9);
  color: #5a7265;
  font-size: 24rpx;
  font-weight: 600;
}

.btn-mini.warn {
  color: #a68450;
  background: #fff3e1;
}

.btn-mini.primary {
  border-color: transparent;
  color: #fff;
  background: linear-gradient(135deg, #5a8c72 0%, #3e6f57 100%);
  box-shadow: 0 6rpx 12rpx rgba(35, 56, 45, 0.16);
}

.btn-mini:active {
  transform: scale(0.96);
}
</style>
