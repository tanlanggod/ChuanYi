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
  background: #f4f5f7;
  padding: 24rpx;
  display: flex;
  flex-direction: column;
}

.rating {
  align-self: flex-end;
  margin-top: 18rpx;
  background: #fff;
  border-radius: 999rpx;
  border: 1rpx solid #eceff3;
  padding: 8rpx 18rpx;
  font-size: 24rpx;
  color: #2f3a4b;
}

.hero {
  flex: 1;
  display: grid;
  place-items: center;
}

.stone-wrap {
  width: 420rpx;
  height: 420rpx;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, #ffffff 0%, #eef1f5 65%, #d9dfe8 100%);
  display: grid;
  place-items: center;
  box-shadow: 0 20rpx 46rpx rgba(37, 50, 77, 0.16);
}

.stone {
  width: 250rpx;
  height: 250rpx;
  border-radius: 50%;
}

.slogan {
  text-align: center;
  font-size: 30rpx;
  font-weight: 700;
  color: #232a35;
  margin-bottom: 80rpx;
}

.skip {
  align-self: flex-end;
  margin-bottom: 30rpx;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  border: 1rpx solid #d8dce3;
  color: #7a8392;
  font-size: 22rpx;
}
</style>
