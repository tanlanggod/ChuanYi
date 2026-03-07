<template>
  <view class="container">
    <view class="hero card">
      <view class="hero-badge">养个石头工作室</view>
      <view class="hero-main">
        <view>
          <view class="hero-title">养个石头</view>
          <view class="hero-sub">天然珠宝手串设计平台</view>
        </view>
        <image class="hero-logo" src="/static/logo-home.png" mode="aspectFit"></image>
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
  gap: 20rpx;
}

.hero {
  position: relative;
  overflow: hidden;
  padding: 42rpx 30rpx;
  border-radius: 28rpx;
  border: 1rpx solid rgba(62, 111, 87, 0.2);
  background: linear-gradient(140deg, #f8f2e8 0%, #efe4d0 45%, #e7dfcf 100%);
  box-shadow: 0 16rpx 34rpx rgba(37, 59, 47, 0.12);
}

.hero::after {
  content: "";
  position: absolute;
  right: -80rpx;
  top: -96rpx;
  width: 280rpx;
  height: 280rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.58) 0%, rgba(255, 255, 255, 0) 72%);
}

.hero-badge {
  display: inline-block;
  padding: 8rpx 22rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(185, 152, 100, 0.32);
  color: var(--jade-primary);
  font-size: 20rpx;
  font-weight: 700;
  letter-spacing: 1rpx;
}

.hero-main {
  margin-top: 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20rpx;
  position: relative;
  z-index: 1;
}

.hero-title {
  font-size: 52rpx;
  font-weight: 800;
  color: #1f2f27;
  letter-spacing: 2rpx;
}

.hero-sub {
  margin-top: 10rpx;
  color: #5a6d61;
  font-size: 26rpx;
}

.hero-logo {
  width: 142rpx;
  height: 142rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.82);
  background: #fff;
  box-shadow: 0 10rpx 20rpx rgba(35, 56, 45, 0.15);
}

.hero-slogan {
  margin-top: 20rpx;
  color: #7d8a80;
  font-size: 22rpx;
  letter-spacing: 1rpx;
  position: relative;
  z-index: 1;
}

.entry-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
}

.entry-card {
  position: relative;
  overflow: hidden;
  border-radius: 22rpx;
  padding: 30rpx 24rpx;
  color: #fff;
  box-shadow: 0 12rpx 24rpx rgba(34, 52, 42, 0.15);
}

.entry-card::before {
  content: "";
  position: absolute;
  right: -24rpx;
  top: -20rpx;
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.34) 0%, rgba(255, 255, 255, 0) 72%);
}

.entry-card.design {
  background: linear-gradient(145deg, #5a8c72 0%, #3e6f57 55%, #2f5a46 100%);
}

.entry-card.goods {
  background: linear-gradient(145deg, #d4b280 0%, #b99864 60%, #9f7d47 100%);
}

.entry-en {
  font-size: 20rpx;
  letter-spacing: 2rpx;
  opacity: 0.88;
}

.entry-title {
  margin-top: 12rpx;
  font-size: 36rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
}

.entry-sub {
  margin-top: 12rpx;
  font-size: 22rpx;
  line-height: 1.56;
  opacity: 0.9;
}

.banner-wrap {
  padding: 0;
  overflow: hidden;
  border-radius: 22rpx;
  box-shadow: 0 10rpx 22rpx rgba(35, 56, 45, 0.09);
}

.banner-swiper {
  width: 100%;
  height: 230rpx;
}

.banner-img {
  width: 100%;
  height: 100%;
  background: #e7decd;
}

.banner-mask {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 22rpx;
  background: linear-gradient(180deg, rgba(23, 33, 28, 0) 0%, rgba(23, 33, 28, 0.68) 100%);
}

.banner-text {
  color: #fff;
  font-size: 26rpx;
  font-weight: 600;
}

.service-bar {
  border-radius: 22rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.34);
  background: linear-gradient(135deg, #fffaf0 0%, #f4e9d8 100%);
  padding: 24rpx 28rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 8rpx 16rpx rgba(44, 60, 50, 0.08);
}

.service-main {
  flex: 1;
}

.service-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #365c49;
}

.service-sub {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #748278;
}

.service-arrow,
.plaza-arrow {
  font-size: 36rpx;
  font-weight: 300;
  color: var(--jade-gold);
}

.plaza-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 24rpx;
  border-radius: 22rpx;
  border: 1rpx solid rgba(62, 111, 87, 0.18);
  background: linear-gradient(150deg, #fffcf8 0%, #f5eee2 100%);
  box-shadow: 0 8rpx 16rpx rgba(44, 61, 50, 0.07);
}

.plaza-title {
  margin-bottom: 6rpx;
  font-size: 34rpx;
  font-weight: 700;
  color: #1f2f27;
}

.gallery-list {
  display: grid;
  gap: 16rpx;
}

.gallery-item {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 24rpx;
  border-radius: 22rpx;
}

.gallery-img {
  width: 140rpx;
  height: 140rpx;
  border-radius: 18rpx;
  background: #e8dfcf;
  border: 2rpx solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 6rpx 12rpx rgba(35, 56, 45, 0.1);
  flex-shrink: 0;
}

.gallery-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 132rpx;
}

.gallery-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #203028;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.gallery-link {
  align-self: flex-start;
  margin-top: auto;
  font-size: 24rpx;
  color: #a68450;
  border-bottom: 2rpx solid #c2a067;
  padding-bottom: 2rpx;
  font-weight: 500;
}
</style>