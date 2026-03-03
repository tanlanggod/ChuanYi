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
