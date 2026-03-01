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
  padding: 24rpx;
  display: grid;
  gap: 12rpx;
}

.hero {
  background: linear-gradient(140deg, #fff, #fff4f4);
  border: 1rpx solid #ffe6e6;
}

.hero-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #111827;
}

.hero-sub {
  margin-top: 8rpx;
  font-size: 23rpx;
  color: #6b7280;
}

.section-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #111827;
  margin-bottom: 10rpx;
}

.line {
  margin-bottom: 10rpx;
  color: #4b5563;
  line-height: 1.75;
  font-size: 23rpx;
}

.line:last-child {
  margin-bottom: 0;
}
</style>
