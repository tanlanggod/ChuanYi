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
