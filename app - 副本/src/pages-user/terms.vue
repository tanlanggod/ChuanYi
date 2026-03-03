<template>
  <view class="container">
    <view class="hero card">
      <view class="hero-title">条款和条件</view>
      <view class="hero-sub">请在下单前阅读以下服务说明</view>
    </view>

    <view class="card section">
      <view class="section-title">使用约定</view>
      <view class="line">1. 平台商品与定制设计仅供个人消费使用，不得用于违法用途。</view>
      <view class="line">2. 用户需保证账号与下单信息真实、完整、有效。</view>
      <view class="line">3. 平台保留在法律允许范围内调整功能与服务规则的权利。</view>
    </view>

    <view class="card section">
      <view class="section-title">交易与履约</view>
      <view class="line">1. 定制订单提交后即进入制作流程，非质量问题不支持无理由退换。</view>
      <view class="line">2. 地址与联系方式请如实填写，因信息错误导致的损失由用户承担。</view>
      <view class="line">3. 物流时效受地区、天气及第三方配送影响，平台会同步最新状态。</view>
    </view>

    <view class="card section">
      <view class="section-title">售后与争议</view>
      <view class="line">1. 如遇质量问题，请先在订单页提交售后说明并保留凭证。</view>
      <view class="line">2. 平台将依据订单记录、物流信息和证据材料进行处理。</view>
      <view class="line">3. 未尽事宜按相关法律法规及平台公告执行。</view>
    </view>
  </view>
</template>

<script>
export default {};
</script>

<style scoped>
.container {
  padding: 30rpx;
  display: grid;
  gap: 20rpx;
  background: #F9F7F3;
  min-height: 100vh;
  box-sizing: border-box;
}

.hero {
  background: linear-gradient(135deg, #FAF8F5 0%, #F0F5F2 100%);
  border: 1rpx solid rgba(232, 228, 219, 0.8);
  border-radius: 20rpx;
  padding: 40rpx 30rpx;
  box-shadow: 0 10rpx 30rpx rgba(47, 54, 50, 0.05);
  position: relative;
  overflow: hidden;
}

.hero::after {
  content: "";
  position: absolute;
  right: -40rpx;
  top: -40rpx;
  width: 160rpx;
  height: 160rpx;
  background: radial-gradient(circle, rgba(255,255,255,0.8) 0%, rgba(255,255,255,0) 70%);
  border-radius: 50%;
}

.hero-title {
  font-size: 40rpx;
  font-weight: 800;
  color: #1A241D;
  position: relative;
  z-index: 1;
  letter-spacing: 2rpx;
}

.hero-sub {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #7A8B7C;
  position: relative;
  z-index: 1;
  letter-spacing: 1rpx;
}

.section {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(47, 54, 50, 0.04);
  border: 1rpx solid rgba(232, 228, 219, 0.6);
}

.section-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1A241D;
  margin-bottom: 20rpx;
  position: relative;
  padding-left: 20rpx;
}

.section-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 6rpx;
  height: 24rpx;
  background: #C2A578;
  border-radius: 4rpx;
}

.line {
  margin-bottom: 12rpx;
  color: #556B5D;
  line-height: 1.7;
  font-size: 26rpx;
}

.line:last-child {
  margin-bottom: 0;
}
</style>
