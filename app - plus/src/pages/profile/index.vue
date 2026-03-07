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
  padding: 30rpx 24rpx;
  display: grid;
  gap: 20rpx;
}

.hero {
  position: relative;
  overflow: hidden;
  border-radius: 24rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.26);
  background: linear-gradient(150deg, #fff9f0 0%, #f3e8d6 100%);
  box-shadow: 0 14rpx 28rpx rgba(33, 53, 43, 0.1);
}

.hero::after {
  content: "";
  position: absolute;
  right: -50rpx;
  top: -60rpx;
  width: 220rpx;
  height: 220rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.6) 0%, rgba(255, 255, 255, 0) 72%);
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
  color: #1f2f27;
  letter-spacing: 2rpx;
}

.hero-sub {
  margin-top: 10rpx;
  font-size: 26rpx;
  color: #6f7f75;
}

.avatar {
  width: 102rpx;
  height: 102rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.82);
  background: linear-gradient(145deg, #5a8c72 0%, #3e6f57 100%);
  box-shadow: 0 10rpx 20rpx rgba(33, 53, 43, 0.2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  font-weight: 700;
}

.hero-actions {
  margin-top: 28rpx;
  position: relative;
  z-index: 1;
}

.design-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 30rpx;
  border-radius: 20rpx;
  color: #fff;
  background: linear-gradient(140deg, #5b8d73 0%, #3f7058 60%, #2f5a46 100%);
  box-shadow: 0 10rpx 20rpx rgba(34, 55, 44, 0.2);
}

.design-title {
  font-size: 32rpx;
  font-weight: 700;
}

.design-sub {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.88);
}

.arrow {
  font-size: 42rpx;
  font-weight: 300;
  color: #ead7b5;
}

.quick-order {
  padding: 30rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.section-title {
  margin: 0;
  font-size: 32rpx;
  font-weight: 700;
  color: #1f2f27;
}

.section-title::before {
  display: none;
}

.section-link {
  font-size: 24rpx;
  font-weight: 600;
  color: #a88552;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14rpx;
}

.quick-item {
  text-align: center;
  padding: 20rpx 8rpx;
  border-radius: 16rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.2);
  background: linear-gradient(150deg, #fffdf9 0%, #f6efe2 100%);
}

.quick-item:active {
  transform: scale(0.98);
}

.quick-num {
  width: 60rpx;
  height: 60rpx;
  margin: 0 auto 12rpx;
  border-radius: 50%;
  border: 1rpx solid rgba(62, 111, 87, 0.2);
  background: #f0f5f2;
  color: #3e6f57;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 700;
}

.quick-name {
  font-size: 24rpx;
  font-weight: 500;
  color: #5a6f63;
}

.menu {
  padding: 10rpx 30rpx;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx 0;
  border-bottom: 1rpx solid rgba(185, 152, 100, 0.15);
}

.menu-item:active {
  opacity: 0.78;
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
  background: linear-gradient(150deg, #edf4ef 0%, #ddebe3 100%);
  color: #3e6f57;
  box-shadow: 0 5rpx 10rpx rgba(62, 111, 87, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 700;
}

.menu-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1f2f27;
}

.menu-sub {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #87938a;
}

.menu-arrow {
  font-size: 40rpx;
  font-weight: 300;
  color: #b99864;
}
</style>
