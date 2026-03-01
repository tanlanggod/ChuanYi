<template>
  <view class="container">
    <view class="hero card">
      <view class="hero-top">
        <view>
          <view class="nickname">刺猬大王</view>
          <view class="hero-sub">您好，欢迎来到养个石头</view>
        </view>
        <view class="avatar">我</view>
      </view>
      <view class="hero-actions">
        <view class="design-entry" @click="goDesigns">
          <view>
            <view class="design-title">我的设计</view>
            <view class="design-sub">查看已保存的设计记录</view>
          </view>
          <view class="arrow">›</view>
        </view>
      </view>
    </view>

    <view class="card quick-order">
      <view class="section-head">
        <view class="section-title">快捷订单</view>
        <view class="section-link" @click="goOrders('')">查看全部</view>
      </view>
      <view class="quick-grid">
        <view class="quick-item" @click="goOrders('PAID_WAIT_SHIP')">
          <view class="quick-num">待</view>
          <view class="quick-name">待发货</view>
        </view>
        <view class="quick-item" @click="goOrders('SHIPPED_WAIT_RECEIVE')">
          <view class="quick-num">收</view>
          <view class="quick-name">待收货</view>
        </view>
        <view class="quick-item" @click="goOrders('COMPLETED')">
          <view class="quick-num">完</view>
          <view class="quick-name">已完成</view>
        </view>
        <view class="quick-item" @click="goOrders('CANCELLED')">
          <view class="quick-num">售</view>
          <view class="quick-name">退款/售后</view>
        </view>
      </view>
    </view>

    <view class="menu card">
      <view class="menu-item" @click="goOrders('')">
        <view class="menu-left">
          <view class="icon-dot">单</view>
          <view>
            <view class="menu-title">我的订单</view>
            <view class="menu-sub">定制记录、购买记录</view>
          </view>
        </view>
        <view class="menu-arrow">›</view>
      </view>
      <view class="menu-item" @click="goAddress">
        <view class="menu-left">
          <view class="icon-dot">址</view>
          <view>
            <view class="menu-title">收货地址</view>
            <view class="menu-sub">完善地址，方便下单</view>
          </view>
        </view>
        <view class="menu-arrow">›</view>
      </view>
      <view class="menu-item" @click="goHelp">
        <view class="menu-left">
          <view class="icon-dot">助</view>
          <view>
            <view class="menu-title">帮助中心</view>
            <view class="menu-sub">有疑问可联系在线客服</view>
          </view>
        </view>
        <view class="menu-arrow">›</view>
      </view>
      <view class="menu-item last" @click="goTerms">
        <view class="menu-left">
          <view class="icon-dot">款</view>
          <view>
            <view class="menu-title">条款和条件</view>
            <view class="menu-sub">平台服务说明</view>
          </view>
        </view>
        <view class="menu-arrow">›</view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  methods: {
    goOrders(status) {
      const query = status ? `?status=${status}` : "";
      uni.navigateTo({ url: `/pages-order/list${query}` });
    },
    goAddress() {
      uni.navigateTo({ url: "/pages-user/address-list" });
    },
    goDesigns() {
      uni.navigateTo({ url: "/pages-diy/designs" });
    },
    goHelp() {
      uni.navigateTo({ url: "/pages-user/help" });
    },
    goTerms() {
      uni.navigateTo({ url: "/pages-user/terms" });
    },
  },
};
</script>

<style scoped>
.container {
  padding: 24rpx;
  display: grid;
  gap: 14rpx;
}

.hero {
  background: linear-gradient(140deg, #fff, #fff4f4);
  border: 1rpx solid #ffe6e6;
}

.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nickname {
  font-size: 42rpx;
  font-weight: 700;
  color: #111827;
}

.hero-sub {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 24rpx;
}

.avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  background: #d62b2b;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 30rpx;
}

.hero-actions {
  margin-top: 16rpx;
}

.design-entry {
  border-radius: 14rpx;
  background: #d62b2b;
  color: #fff;
  padding: 20rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.design-title {
  font-size: 30rpx;
  font-weight: 700;
}

.design-sub {
  margin-top: 8rpx;
  color: #ffe5e5;
  font-size: 22rpx;
}

.arrow {
  font-size: 38rpx;
  line-height: 1;
}

.quick-order {
  padding: 20rpx;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #111827;
}

.section-link {
  color: #d62b2b;
  font-size: 22rpx;
}

.quick-grid {
  margin-top: 12rpx;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10rpx;
}

.quick-item {
  text-align: center;
  padding: 14rpx 6rpx;
  border-radius: 12rpx;
  background: #f8fafc;
}

.quick-num {
  margin: 0 auto 6rpx;
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: #fee2e2;
  color: #b91c1c;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 700;
}

.quick-name {
  font-size: 22rpx;
  color: #374151;
}

.menu {
  padding: 0 20rpx;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22rpx 0;
  border-bottom: 1px solid #f0f1f3;
}

.menu-item.last {
  border-bottom: none;
}

.menu-left {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.icon-dot {
  width: 52rpx;
  height: 52rpx;
  border-radius: 14rpx;
  background: #fef2f2;
  color: #b91c1c;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 700;
}

.menu-title {
  font-size: 29rpx;
  font-weight: 600;
  color: #111827;
}

.menu-sub {
  margin-top: 6rpx;
  color: #8a8f99;
  font-size: 22rpx;
}

.menu-arrow {
  color: #adb2bc;
  font-size: 36rpx;
}
</style>
