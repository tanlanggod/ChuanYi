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
  gap: 20rpx;
}

.hero {
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #F0F5F2 0%, #E3EBE6 100%); /* 淡雅玉色 */
  border: none;
  border-radius: 24rpx;
  box-shadow: 0 12rpx 30rpx rgba(59, 110, 83, 0.08);
  padding: 40rpx 30rpx;
}

.hero::after {
  content: "";
  position: absolute;
  right: -60rpx;
  top: -60rpx;
  width: 240rpx;
  height: 240rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255,255,255,0.6) 0%, rgba(255,255,255,0) 70%);
  opacity: 0.8;
}

.hero-badge {
  display: inline-block;
  border-radius: 999rpx;
  padding: 8rpx 20rpx;
  font-size: 20rpx;
  color: #3B6E53;
  background: rgba(255, 255, 255, 0.9);
  font-weight: 500;
  box-shadow: 0 4rpx 10rpx rgba(59, 110, 83, 0.05);
}

.hero-main {
  margin-top: 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 1;
}

.hero-title {
  font-size: 52rpx;
  font-weight: 800;
  letter-spacing: 2rpx;
  color: #1A241D;
  font-family: "PingFang SC", "Microsoft YaHei", serif;
}

.hero-sub {
  margin-top: 10rpx;
  color: #556B5D;
  font-size: 26rpx;
  letter-spacing: 1rpx;
}

.hero-logo {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 8rpx 20rpx rgba(0, 0, 0, 0.06);
  border: 4rpx solid #FFF;
}

.hero-slogan {
  margin-top: 20rpx;
  color: #7A8B7C;
  font-size: 22rpx;
  letter-spacing: 1rpx;
}

.entry-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
}

.entry-card {
  border-radius: 20rpx;
  padding: 28rpx 24rpx;
  color: #fff;
  box-shadow: 0 10rpx 24rpx rgba(47, 54, 50, 0.1);
  position: relative;
  overflow: hidden;
}

.entry-card::before {
  content: "";
  position: absolute;
  top: 0;
  right: 0;
  width: 100rpx;
  height: 100rpx;
  background: linear-gradient(135deg, rgba(255,255,255,0.2) 0%, rgba(255,255,255,0) 100%);
  border-radius: 50%;
  transform: translate(30%, -30%);
}

.entry-card.design {
  background: linear-gradient(135deg, #4A8365 0%, #2A523D 100%); /* 深玉绿 */
}

.entry-card.goods {
  background: linear-gradient(135deg, #C2A578 0%, #A68754 100%); /* 鎏金色 */
}

.entry-en {
  font-size: 20rpx;
  opacity: 0.8;
  letter-spacing: 2rpx;
  text-transform: uppercase;
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
  line-height: 1.5;
  opacity: 0.9;
}

.banner-wrap {
  padding: 0;
  overflow: hidden;
  border-radius: 20rpx;
  box-shadow: 0 8rpx 20rpx rgba(47, 54, 50, 0.05);
}

.banner-swiper {
  width: 100%;
  height: 220rpx;
}

.banner-img {
  width: 100%;
  height: 100%;
  background: #E8E4DB;
}

.banner-mask {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0) 0%, rgba(0, 0, 0, 0.6) 100%);
}

.banner-text {
  color: #fff;
  font-size: 26rpx;
  font-weight: 500;
  letter-spacing: 1rpx;
}

.service-bar {
  border-radius: 20rpx;
  background: linear-gradient(135deg, #FAF8F5 0%, #F0EDE6 100%); /* 温润白渐变 */
  border: 1rpx solid #E8E4DB;
  color: #2F3632;
  padding: 24rpx 30rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 6rpx 16rpx rgba(47, 54, 50, 0.03);
}

.service-main {
  flex: 1;
}

.service-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #3B6E53;
}

.service-sub {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #7A8B7C;
}

.service-arrow,
.plaza-arrow {
  font-size: 32rpx;
  color: #C2A578;
  font-weight: 300;
}

.plaza-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 24rpx;
  background: #fff;
  border-radius: 20rpx;
}

.plaza-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #1A241D;
  margin-bottom: 6rpx;
}

.gallery-list {
  display: grid;
  gap: 16rpx;
}

.gallery-item {
  display: flex;
  gap: 24rpx;
  align-items: center;
  padding: 24rpx;
}

.gallery-img {
  width: 140rpx;
  height: 140rpx;
  border-radius: 16rpx;
  background: #E8E4DB;
  flex-shrink: 0;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
  border: 2rpx solid #FAF8F5;
}

.gallery-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 130rpx;
}

.gallery-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #2F3632;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.gallery-link {
  align-self: flex-start;
  margin-top: auto;
  font-size: 24rpx;
  color: #C2A578;
  border-bottom: 2rpx solid #C2A578;
  padding-bottom: 2rpx;
  font-weight: 500;
}
</style>
