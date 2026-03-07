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
  padding: 30rpx 24rpx;
}

.hero-title {
  font-size: 48rpx;
  font-weight: 800;
  color: #1f2f27;
  letter-spacing: 3rpx;
}

.muted {
  margin-top: 8rpx;
  margin-bottom: 28rpx;
  font-size: 25rpx;
  color: #7b887f;
}

.design-card {
  margin-bottom: 24rpx;
  padding: 36rpx 30rpx;
  border-radius: 24rpx;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(140deg, #5b8d73 0%, #3f7058 55%, #2f5a46 100%);
  box-shadow: 0 14rpx 28rpx rgba(34, 55, 44, 0.2);
}

.design-card::before {
  content: "";
  position: absolute;
  right: -44rpx;
  top: -56rpx;
  width: 180rpx;
  height: 180rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.3) 0%, rgba(255, 255, 255, 0) 72%);
}

.design-title {
  font-size: 34rpx;
  font-weight: 700;
  letter-spacing: 1rpx;
}

.design-sub {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.9);
}

.arrow {
  font-size: 42rpx;
  font-weight: 300;
  color: #e4cfab;
}

.entry {
  margin-bottom: 18rpx;
  padding: 38rpx 28rpx;
  text-align: center;
  border-radius: 22rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.22);
  background: linear-gradient(160deg, #fffdf8 0%, #f6efe2 100%);
  box-shadow: 0 10rpx 20rpx rgba(35, 56, 45, 0.08);
}

.entry:active {
  transform: scale(0.985);
}

.entry-icon {
  width: 76rpx;
  height: 76rpx;
  margin: 0 auto 16rpx;
  border-radius: 50%;
  line-height: 76rpx;
  font-size: 36rpx;
  font-weight: 700;
  color: #3e6f57;
  background: linear-gradient(145deg, #edf4ef 0%, #dfece4 100%);
  box-shadow: 0 6rpx 12rpx rgba(62, 111, 87, 0.14);
}

.entry-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #1f2f27;
  letter-spacing: 1rpx;
}

.entry-sub {
  margin-top: 12rpx;
  padding: 0 18rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #87938a;
}
</style>
