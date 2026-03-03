<template>
  <view class="container">
    <view class="hero-title">开始您的手串设计之旅</view>
    <view class="muted">选择您喜欢的设计方式</view>

    <view class="design-card" @click="goDesigns">
      <view>
        <view class="design-title">我的设计</view>
        <view class="design-sub">查看保存的设计记录（{{ draftCount }}）</view>
      </view>
      <view class="arrow">›</view>
    </view>

    <view class="entry card" @click="goEditor">
      <view class="entry-icon">定</view>
      <view class="entry-title">自由定制手串</view>
      <view class="entry-sub">从零开始，选择材质和尺寸，打造专属手串</view>
    </view>

    <view class="entry card" @click="goGoods">
      <view class="entry-icon">珠</view>
      <view class="entry-title">选购单珠</view>
      <view class="entry-sub">快速挑选喜欢的单珠与配件，加入购物车</view>
    </view>

    <view class="entry card" @click="goGallery">
      <view class="entry-icon">广</view>
      <view class="entry-title">设计广场</view>
      <view class="entry-sub">浏览热门搭配，从真实作品中获取灵感</view>
    </view>
  </view>
</template>

<script>
import { fetchDraftList } from "../../common/api";
import { loginGuestIfNeeded } from "../../common/session";

export default {
  data() {
    return {
      draftCount: 0,
    };
  },
  async onShow() {
    await this.loadDraftCount();
  },
  methods: {
    async loadDraftCount() {
      try {
        await loginGuestIfNeeded();
        const data = await fetchDraftList(1, 1);
        this.draftCount = (data && data.page && data.page.total) || 0;
      } catch (error) {
        this.draftCount = 0;
      }
    },
    goEditor() {
      uni.navigateTo({ url: "/pages-diy/editor" });
    },
    goDesigns() {
      uni.navigateTo({ url: "/pages-diy/designs" });
    },
    goGoods() {
      uni.switchTab({ url: "/pages/goods/index" });
    },
    goGallery() {
      uni.switchTab({ url: "/pages/home/index" });
    },
  },
};
</script>

<style scoped>
.container {
  padding: 30rpx;
}

.hero-title {
  font-size: 46rpx;
  font-weight: 800;
  color: #1A241D;
  margin-bottom: 8rpx;
  letter-spacing: 2rpx;
  font-family: "PingFang SC", "Microsoft YaHei", serif;
}

.muted {
  font-size: 26rpx;
  color: #7A8B7C;
  margin-bottom: 30rpx;
  letter-spacing: 1rpx;
}

.design-card {
  margin-top: 20rpx;
  margin-bottom: 24rpx;
  background: linear-gradient(135deg, #4A8365 0%, #2A523D 100%);
  border-radius: 20rpx;
  padding: 36rpx 30rpx;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 10rpx 24rpx rgba(47, 54, 50, 0.15);
  position: relative;
  overflow: hidden;
}

.design-card::before {
  content: "";
  position: absolute;
  top: -20rpx;
  right: -20rpx;
  width: 140rpx;
  height: 140rpx;
  background: radial-gradient(circle, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0) 70%);
  border-radius: 50%;
}

.design-title {
  font-size: 34rpx;
  font-weight: 700;
  letter-spacing: 1rpx;
}

.design-sub {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
  letter-spacing: 1rpx;
}

.arrow {
  font-size: 42rpx;
  font-weight: 300;
  color: #C2A578;
}

.entry {
  margin-bottom: 20rpx;
  text-align: center;
  padding: 40rpx 30rpx;
  border-radius: 20rpx;
  background: #fff;
  box-shadow: 0 8rpx 20rpx rgba(47, 54, 50, 0.04);
  border: 1rpx solid rgba(232, 228, 219, 0.5);
  transition: transform 0.2s ease;
}

.entry:active {
  transform: scale(0.98);
}

.entry-icon {
  font-size: 36rpx;
  color: #3B6E53;
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  line-height: 72rpx;
  margin: 0 auto 16rpx;
  background: #F0F5F2;
  font-weight: 600;
  box-shadow: 0 4rpx 10rpx rgba(59, 110, 83, 0.08);
}

.entry-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #2F3632;
  letter-spacing: 1rpx;
}

.entry-sub {
  margin-top: 12rpx;
  color: #8C968F;
  font-size: 24rpx;
  line-height: 1.6;
  padding: 0 20rpx;
}
</style>
