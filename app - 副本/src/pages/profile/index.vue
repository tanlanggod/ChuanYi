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
  padding: 30rpx;
  display: grid;
  gap: 20rpx;
}

.hero {
  background: linear-gradient(135deg, #FAF8F5 0%, #F0F5F2 100%);
  border: 1rpx solid rgba(232, 228, 219, 0.8);
  box-shadow: 0 10rpx 30rpx rgba(47, 54, 50, 0.05);
  position: relative;
  overflow: hidden;
}

.hero::after {
  content: "";
  position: absolute;
  right: -40rpx;
  top: -40rpx;
  width: 180rpx;
  height: 180rpx;
  background: radial-gradient(circle, rgba(255,255,255,0.8) 0%, rgba(255,255,255,0) 70%);
  border-radius: 50%;
}

.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 1;
}

.nickname {
  font-size: 46rpx;
  font-weight: 800;
  color: #1A241D;
  font-family: "PingFang SC", "Microsoft YaHei", serif;
  letter-spacing: 2rpx;
}

.hero-sub {
  margin-top: 10rpx;
  color: #7A8B7C;
  font-size: 26rpx;
  letter-spacing: 1rpx;
}

.avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #4A8365 0%, #3B6E53 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 36rpx;
  box-shadow: 0 8rpx 20rpx rgba(59, 110, 83, 0.2);
  border: 4rpx solid #fff;
}

.hero-actions {
  margin-top: 30rpx;
  position: relative;
  z-index: 1;
}

.design-entry {
  border-radius: 20rpx;
  background: linear-gradient(135deg, #3B6E53 0%, #2A523D 100%);
  color: #fff;
  padding: 24rpx 30rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 8rpx 20rpx rgba(47, 54, 50, 0.15);
}

.design-title {
  font-size: 32rpx;
  font-weight: 600;
  letter-spacing: 1rpx;
}

.design-sub {
  margin-top: 10rpx;
  color: rgba(255, 255, 255, 0.85);
  font-size: 24rpx;
  letter-spacing: 1rpx;
}

.arrow {
  font-size: 42rpx;
  line-height: 1;
  color: #C2A578;
  font-weight: 300;
}

.quick-order {
  padding: 30rpx;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1A241D;
  margin: 0;
}

.section-title::before {
  display: none;
}

.section-link {
  color: #C2A578;
  font-size: 24rpx;
  font-weight: 500;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
}

.quick-item {
  text-align: center;
  padding: 20rpx 10rpx;
  border-radius: 16rpx;
  background: #F9F7F3;
  transition: all 0.2s ease;
}

.quick-item:active {
  background: #E8E4DB;
}

.quick-num {
  margin: 0 auto 12rpx;
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: #fff;
  color: #3B6E53;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 600;
  box-shadow: 0 4rpx 10rpx rgba(0,0,0,0.03);
  border: 1rpx solid #E8E4DB;
}

.quick-name {
  font-size: 24rpx;
  color: #556B5D;
  font-weight: 500;
}

.menu {
  padding: 10rpx 30rpx;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx 0;
  border-bottom: 1px solid rgba(232, 228, 219, 0.6);
  transition: opacity 0.2s;
}

.menu-item:active {
  opacity: 0.7;
}

.menu-item.last {
  border-bottom: none;
}

.menu-left {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.icon-dot {
  width: 64rpx;
  height: 64rpx;
  border-radius: 16rpx;
  background: #F0F5F2;
  color: #3B6E53;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 600;
  box-shadow: 0 4rpx 10rpx rgba(59, 110, 83, 0.05);
}

.menu-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #2F3632;
  letter-spacing: 1rpx;
}

.menu-sub {
  margin-top: 8rpx;
  color: #8C968F;
  font-size: 24rpx;
}

.menu-arrow {
  color: #C2A578;
  font-size: 40rpx;
  font-weight: 300;
}
</style>
