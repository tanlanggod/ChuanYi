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
  padding: 24rpx;
}

.hero-title {
  font-size: 42rpx;
  font-weight: 700;
}

.design-card {
  margin-top: 16rpx;
  margin-bottom: 14rpx;
  background: #d62b2b;
  border-radius: 14rpx;
  padding: 24rpx;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.design-title {
  font-size: 30rpx;
  font-weight: 700;
}

.design-sub {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #ffe5e5;
}

.arrow {
  font-size: 38rpx;
}

.entry {
  margin-bottom: 14rpx;
  text-align: center;
  padding-top: 34rpx;
  padding-bottom: 34rpx;
}

.entry-icon {
  font-size: 34rpx;
  color: #d62b2b;
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  line-height: 56rpx;
  margin: 0 auto 12rpx;
  background: #fff0f0;
}

.entry-title {
  font-size: 34rpx;
  font-weight: 700;
}

.entry-sub {
  margin-top: 10rpx;
  color: #8a8f99;
  font-size: 24rpx;
  line-height: 1.6;
}
</style>
