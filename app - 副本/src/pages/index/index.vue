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
  background: #F9F7F3;
  padding: 30rpx;
  display: flex;
  flex-direction: column;
}

.rating {
  align-self: flex-end;
  margin-top: 20rpx;
  background: #fff;
  border-radius: 999rpx;
  border: 1rpx solid #E8E4DB;
  padding: 10rpx 24rpx;
  font-size: 26rpx;
  color: #C2A578;
  font-weight: 600;
  box-shadow: 0 4rpx 10rpx rgba(47, 54, 50, 0.03);
}

.hero {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stone-wrap {
  width: 480rpx;
  height: 480rpx;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, #ffffff 0%, #FAF8F5 65%, #F0EDE6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 20rpx 50rpx rgba(47, 54, 50, 0.1);
  border: 2rpx solid rgba(232, 228, 219, 0.6);
}

.stone {
  width: 300rpx;
  height: 300rpx;
  border-radius: 50%;
}

.slogan {
  text-align: center;
  font-size: 36rpx;
  font-weight: 800;
  color: #1A241D;
  margin-bottom: 100rpx;
  letter-spacing: 4rpx;
  font-family: "PingFang SC", "Microsoft YaHei", serif;
}

.skip {
  align-self: center;
  margin-bottom: 60rpx;
  padding: 12rpx 40rpx;
  border-radius: 999rpx;
  border: 1rpx solid #D1E0D7;
  color: #7A8B7C;
  font-size: 26rpx;
  background: #F0F5F2;
  transition: all 0.2s;
}

.skip:active {
  background: #E3EBE6;
}
</style>
