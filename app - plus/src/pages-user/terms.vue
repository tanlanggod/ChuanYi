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
  min-height: 100vh;
  box-sizing: border-box;
  padding: 28rpx 24rpx;
  display: grid;
  gap: 18rpx;
}

.hero {
  position: relative;
  overflow: hidden;
  border-radius: 24rpx;
  padding: 40rpx 30rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.25);
  background: linear-gradient(145deg, #fff9ee 0%, #f3e8d7 100%);
  box-shadow: 0 12rpx 24rpx rgba(34, 54, 43, 0.1);
}

.hero::after {
  content: "";
  position: absolute;
  right: -56rpx;
  top: -56rpx;
  width: 200rpx;
  height: 200rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.6) 0%, rgba(255, 255, 255, 0) 72%);
}

.hero-title {
  position: relative;
  z-index: 1;
  color: #1f2f27;
  font-size: 40rpx;
  font-weight: 800;
  letter-spacing: 2rpx;
}

.hero-sub {
  margin-top: 12rpx;
  position: relative;
  z-index: 1;
  color: #728177;
  font-size: 26rpx;
}

.section {
  border-radius: 22rpx;
  padding: 30rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.18);
  background: linear-gradient(160deg, #fffdf9 0%, #f7f0e3 100%);
  box-shadow: 0 8rpx 16rpx rgba(34, 54, 43, 0.08);
}

.section-title {
  margin-bottom: 20rpx;
  padding-left: 20rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2f27;
  position: relative;
}

.section-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 7rpx;
  height: 24rpx;
  border-radius: 8rpx;
  background: linear-gradient(180deg, #c39e67 0%, #3e6f57 100%);
}

.line {
  margin-bottom: 12rpx;
  color: #5e7266;
  line-height: 1.72;
  font-size: 26rpx;
}

.line:last-child {
  margin-bottom: 0;
}
</style>
