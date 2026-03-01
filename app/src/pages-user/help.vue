<template>
  <view class="container">
    <view class="hero card">
      <view class="hero-title">帮助中心</view>
      <view class="hero-sub">常见问题与处理建议</view>
    </view>

    <view class="card section">
      <view class="section-title">设计与保存</view>
      <view class="line">1. 离开手串编辑页前请先点击保存，避免草稿丢失。</view>
      <view class="line">2. 完成设计后请先生成快照，再进行提交订单。</view>
      <view class="line">3. 手围建议先测量手腕周长，再选择对应尺寸。</view>
    </view>

    <view class="card section">
      <view class="section-title">下单与支付</view>
      <view class="line">1. 提交订单前请确认收货地址、商品规格和数量。</view>
      <view class="line">2. 待支付订单可在订单详情页完成支付。</view>
      <view class="line">3. 如支付失败，请稍后重试或重新下单。</view>
    </view>

    <view class="card section">
      <view class="section-title">售后与物流</view>
      <view class="line">1. 发货后可在订单详情页查看物流轨迹。</view>
      <view class="line">2. 收到货后请及时确认收货并检查商品状态。</view>
      <view class="line">3. 售后问题请携带订单号联系在线客服处理。</view>
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
