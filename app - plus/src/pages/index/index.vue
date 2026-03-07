<template>
  <view class="launch">
    <view class="rating">★ 4.8</view>
    <view class="hero">
      <view class="stone-wrap">
        <image class="stone" src="/static/logo.png" mode="aspectFill"></image>
      </view>
    </view>
    <view class="slogan">全国首家在线手串定制平台</view>
    <view class="skip" @click="goHome">跳过</view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      timer: null,
    };
  },
  onLoad() {
    this.timer = setTimeout(() => {
      this.goHome();
    }, 1600);
  },
  onUnload() {
    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = null;
    }
  },
  methods: {
    goHome() {
      if (this.timer) {
        clearTimeout(this.timer);
        this.timer = null;
      }
      uni.switchTab({ url: "/pages/home/index" });
    },
  },
};
</script>

<style scoped>
.launch {
  min-height: 100vh;
  padding: 36rpx 32rpx 56rpx;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.launch::before,
.launch::after {
  content: "";
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.launch::before {
  width: 520rpx;
  height: 520rpx;
  right: -190rpx;
  top: -180rpx;
  background: radial-gradient(circle, rgba(62, 111, 87, 0.16) 0%, rgba(62, 111, 87, 0) 70%);
}

.launch::after {
  width: 420rpx;
  height: 420rpx;
  left: -180rpx;
  bottom: -160rpx;
  background: radial-gradient(circle, rgba(185, 152, 100, 0.22) 0%, rgba(185, 152, 100, 0) 72%);
}

.rating {
  align-self: flex-end;
  margin-top: 14rpx;
  padding: 10rpx 28rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.35);
  background: rgba(255, 252, 246, 0.82);
  color: var(--jade-gold);
  font-size: 24rpx;
  font-weight: 700;
  letter-spacing: 1rpx;
  box-shadow: var(--jade-shadow-soft);
  z-index: 1;
}

.hero {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

.stone-wrap {
  width: 500rpx;
  height: 500rpx;
  border-radius: 50%;
  background: radial-gradient(circle at 32% 25%, #ffffff 0%, #f8f0e2 66%, #e9dfca 100%);
  border: 2rpx solid rgba(185, 152, 100, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 26rpx 56rpx rgba(38, 61, 49, 0.16),
    inset 0 0 0 10rpx rgba(255, 255, 255, 0.55);
}

.stone {
  width: 312rpx;
  height: 312rpx;
  border-radius: 50%;
  border: 6rpx solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 10rpx 24rpx rgba(38, 61, 49, 0.16);
}

.slogan {
  margin-bottom: 92rpx;
  text-align: center;
  color: var(--jade-text);
  font-size: 38rpx;
  font-weight: 800;
  letter-spacing: 5rpx;
  z-index: 1;
}

.skip {
  align-self: center;
  margin-bottom: 46rpx;
  padding: 14rpx 46rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(62, 111, 87, 0.4);
  background: rgba(255, 252, 248, 0.82);
  color: var(--jade-primary);
  font-size: 25rpx;
  font-weight: 600;
  letter-spacing: 1rpx;
  transition: transform 0.2s ease, background 0.2s ease;
  z-index: 1;
}

.skip:active {
  transform: scale(0.97);
  background: #f2eadc;
}
</style>
