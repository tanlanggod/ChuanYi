<template>
  <view class="container">
    <view class="hero card">
      <view class="hero-badge">养个石头工作室</view>
      <view class="hero-main">
        <view>
          <view class="hero-title">养个石头</view>
          <view class="hero-sub">天然珠宝手串设计平台</view>
        </view>
        <image class="hero-logo" src="/static/logo.png" mode="aspectFit"></image>
      </view>
      <view class="hero-slogan">从设计灵感到实物佩戴，一站完成</view>
    </view>

    <view class="entry-grid">
      <view class="entry-card design" @click="goDIY">
        <view class="entry-en">手串定制</view>
        <view class="entry-title">设计手串</view>
        <view class="entry-sub">自由搭配珠材、隔珠与吊坠</view>
      </view>
      <view class="entry-card goods" @click="goGoods">
        <view class="entry-en">精选好物</view>
        <view class="entry-title">好物</view>
        <view class="entry-sub">成品与周边，一键下单</view>
      </view>
    </view>

    <view class="banner-wrap card" v-if="banners.length > 0">
      <swiper class="banner-swiper" indicator-dots autoplay circular>
        <swiper-item v-for="item in banners" :key="item.id" @click="openBanner(item)">
          <image class="banner-img" :src="resolveImage(item.imageUrl)" mode="aspectFill"></image>
          <view class="banner-mask">
            <view class="banner-text">{{ item.title || "精选推荐" }}</view>
          </view>
        </swiper-item>
      </swiper>
    </view>

    <view class="service-bar" @click="goDIY">
      <view class="service-main">
        <view class="service-title">添加客服，查看设计实物图</view>
        <view class="service-sub">支持手围建议与配色指导</view>
      </view>
      <view class="service-arrow">→</view>
    </view>

    <view class="plaza-head card" @click="goGalleryList">
      <view>
        <view class="plaza-title">设计广场</view>
        <view class="muted">从世界各地的手串搭配中获取灵感</view>
      </view>
      <view class="plaza-arrow">→</view>
    </view>

    <view class="card" v-if="gallery.length === 0">
      <view class="muted">暂无广场数据</view>
    </view>
    <view class="gallery-list" v-else>
      <view class="card gallery-item" v-for="item in gallery" :key="item.id" @click="openGalleryDetail(item)">
        <image class="gallery-img" :src="resolveImage(item.coverImageUrl)" mode="aspectFill"></image>
        <view class="gallery-main">
          <view class="gallery-title">{{ item.title || "未命名作品" }}</view>
          <view class="muted">@{{ item.authorNickname || "匿名用户" }}</view>
          <view class="gallery-link">查看实物</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { fetchBannerList, fetchGallery } from "../../common/api";
import { resolveAssetUrl } from "../../common/http";
import { loginGuestIfNeeded } from "../../common/session";

export default {
  data() {
    return {
      banners: [],
      gallery: [],
    };
  },
  async onShow() {
    await this.loadData();
  },
  async onPullDownRefresh() {
    await this.loadData();
    uni.stopPullDownRefresh();
  },
  methods: {
    async loadData() {
      try {
        await loginGuestIfNeeded();
        const [bannerData, galleryData] = await Promise.all([fetchBannerList(), fetchGallery(1, 6)]);
        this.banners = (bannerData && bannerData.list) || [];
        this.gallery = (galleryData && galleryData.list) || [];
      } catch (error) {
        uni.showToast({ title: "加载失败", icon: "none" });
      }
    },
    goDIY() {
      uni.switchTab({ url: "/pages/diy/index" });
    },
    goGoods() {
      uni.switchTab({ url: "/pages/goods/index" });
    },
    goGalleryList() {
      uni.navigateTo({ url: "/pages-gallery/list" });
    },
    openBanner(item) {
      const jumpType = (item && item.jumpType) || "";
      const jumpValue = (item && item.jumpValue) || "";
      if (jumpType === "GOODS" && jumpValue) {
        uni.navigateTo({ url: `/pages-goods/detail?spuId=${jumpValue}` });
        return;
      }
      if (jumpType === "DIY") {
        this.goDIY();
        return;
      }
      if (jumpType === "GALLERY") {
        if (jumpValue) {
          uni.navigateTo({ url: `/pages-gallery/detail?id=${jumpValue}` });
        } else {
          this.goGalleryList();
        }
      }
    },
    openGalleryDetail(item) {
      if (!item || !item.id) {
        return;
      }
      uni.navigateTo({ url: `/pages-gallery/detail?id=${item.id}` });
    },
    resolveImage(url) {
      if (!url) {
        return "/static/logo.png";
      }
      return resolveAssetUrl(url);
    },
  },
};
</script>

<style scoped>
.container {
  padding: 24rpx;
  display: grid;
  gap: 14rpx;
}

.hero {
  position: relative;
  overflow: hidden;
  background: linear-gradient(130deg, #fff5f5 0%, #ffe4e6 45%, #fff 100%);
  border: 1rpx solid #ffe1e1;
}

.hero::after {
  content: "";
  position: absolute;
  right: -40rpx;
  top: -40rpx;
  width: 180rpx;
  height: 180rpx;
  border-radius: 50%;
  background: rgba(214, 43, 43, 0.08);
}

.hero-badge {
  display: inline-block;
  border-radius: 999rpx;
  padding: 8rpx 16rpx;
  font-size: 20rpx;
  color: #b42318;
  background: #fff;
}

.hero-main {
  margin-top: 12rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hero-title {
  font-size: 46rpx;
  font-weight: 800;
  letter-spacing: 1rpx;
  color: #111827;
}

.hero-sub {
  margin-top: 6rpx;
  color: #4b5563;
  font-size: 24rpx;
}

.hero-logo {
  width: 130rpx;
  height: 130rpx;
  border-radius: 50%;
  background: #fff;
}

.hero-slogan {
  margin-top: 10rpx;
  color: #7f1d1d;
  font-size: 22rpx;
}

.entry-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12rpx;
}

.entry-card {
  border-radius: 18rpx;
  padding: 20rpx;
  color: #fff;
  box-shadow: 0 10rpx 24rpx rgba(17, 24, 39, 0.12);
}

.entry-card.design {
  background: linear-gradient(130deg, #d62b2b 0%, #f65c5c 100%);
}

.entry-card.goods {
  background: linear-gradient(130deg, #111827 0%, #334155 100%);
}

.entry-en {
  font-size: 20rpx;
  opacity: 0.9;
}

.entry-title {
  margin-top: 8rpx;
  font-size: 34rpx;
  font-weight: 700;
}

.entry-sub {
  margin-top: 10rpx;
  font-size: 22rpx;
  line-height: 1.5;
  opacity: 0.95;
}

.banner-wrap {
  padding: 0;
  overflow: hidden;
}

.banner-swiper {
  width: 100%;
  height: 190rpx;
}

.banner-img {
  width: 100%;
  height: 100%;
  background: #f3f4f6;
}

.banner-mask {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 14rpx 16rpx;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0) 0%, rgba(0, 0, 0, 0.45) 100%);
}

.banner-text {
  color: #fff;
  font-size: 24rpx;
  font-weight: 600;
}

.service-bar {
  border-radius: 16rpx;
  background: linear-gradient(95deg, #f59e0b 0%, #ec4899 50%, #0ea5e9 100%);
  color: #fff;
  padding: 16rpx 18rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.service-main {
  flex: 1;
}

.service-title {
  font-size: 24rpx;
  font-weight: 700;
}

.service-sub {
  margin-top: 4rpx;
  font-size: 20rpx;
  opacity: 0.9;
}

.service-arrow,
.plaza-arrow {
  font-size: 32rpx;
}

.plaza-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.plaza-title {
  font-size: 32rpx;
  font-weight: 700;
}

.gallery-list {
  display: grid;
  gap: 12rpx;
}

.gallery-item {
  display: flex;
  gap: 14rpx;
  align-items: center;
}

.gallery-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: #f3f4f6;
  flex-shrink: 0;
}

.gallery-main {
  flex: 1;
  min-width: 0;
}

.gallery-title {
  font-size: 30rpx;
  font-weight: 700;
}

.gallery-link {
  margin-top: 10rpx;
  display: inline-block;
  font-size: 22rpx;
  color: #111827;
  border-bottom: 2rpx solid #111827;
}
</style>
