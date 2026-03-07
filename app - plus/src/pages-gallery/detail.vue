<template>
  <view class="container">
    <view class="card loading-card" v-if="loading">
      <view class="muted">作品信息加载中...</view>
    </view>

    <view class="card loading-card" v-else-if="!detail.id">
      <view class="muted">未找到该作品</view>
    </view>

    <template v-else>
      <view class="card cover-card">
        <image class="cover" :src="resolveImage(detail.coverImageUrl)" mode="aspectFill"></image>
        <view class="cover-mask">
          <view class="cover-title">{{ detail.title || "设计作品" }}</view>
        </view>
      </view>

      <view class="card main-card">
        <view class="meta-row">
          <view class="meta-chip">作者：{{ detail.authorNickname || "匿名用户" }}</view>
          <view class="meta-chip">发布时间：{{ formatDate(detail.publishedAt) }}</view>
        </view>
        <view class="meta-row count-row">
          <view class="meta-chip">点赞 {{ Number(detail.likeCount || 0) }}</view>
          <view class="meta-chip">浏览 {{ Number(detail.viewCount || 0) }}</view>
          <view class="meta-chip">热度 {{ Number(detail.hotScore || 0) }}</view>
        </view>
        <view class="tags" v-if="detail.tags && detail.tags.length > 0">
          <view class="tag" v-for="tag in detail.tags" :key="tag">{{ tag }}</view>
        </view>
        <view
          class="like-btn"
          :class="{ active: detail.isLiked, disabled: likeLoading }"
          @click="toggleLike"
        >
          {{ likeLoading ? "处理中..." : detail.isLiked ? "已点赞，点击取消" : "点赞作品" }}
        </view>
      </view>

      <view class="card note-card">
        <view class="note-title">灵感说明</view>
        <view class="note-line">1. 该作品来自设计广场，适合作为 DIY 配色和材质参考。</view>
        <view class="note-line">2. 你可以进入定制页按类似顺序选择珠子并微调手围。</view>
        <view class="note-line">3. 若喜欢该风格，建议先保存草稿再进行细节优化。</view>
      </view>

      <view class="section-title">相关推荐</view>
      <view class="card loading-card" v-if="recommendLoading">
        <view class="muted">正在加载推荐作品...</view>
      </view>
      <view class="card" v-else-if="recommendList.length === 0">
        <view class="muted">暂无更多推荐</view>
      </view>
      <view class="recommend-list" v-else>
        <view class="card recommend-item" v-for="item in recommendList" :key="item.id" @click="openRecommend(item)">
          <image class="recommend-image" :src="resolveImage(item.coverImageUrl)" mode="aspectFill"></image>
          <view class="recommend-main">
            <view class="recommend-title">{{ item.title || "未命名作品" }}</view>
            <view class="muted">@{{ item.authorNickname || "匿名用户" }}</view>
            <view class="recommend-link">查看详情</view>
          </view>
        </view>
      </view>
    </template>

    <view class="bottom-space"></view>
    <view class="footer">
      <view class="btn-outline" @click="goHome">返回广场</view>
      <view class="btn-primary" @click="goDIY">照着搭配</view>
    </view>
  </view>
</template>

<script>
import {
  fetchGallery,
  fetchGalleryDetail,
  likeGallery,
  unlikeGallery,
} from "../common/api";
import { resolveAssetUrl } from "../common/http";
import { loginGuestIfNeeded } from "../common/session";

export default {
  data() {
    return {
      id: null,
      detail: {},
      recommendList: [],
      loading: false,
      recommendLoading: false,
      likeLoading: false,
    };
  },
  async onLoad(options) {
    this.id = options && options.id ? Number(options.id) : null;
    await this.loadDetail();
  },
  methods: {
    async loadDetail() {
      if (!this.id) {
        return;
      }
      this.loading = true;
      try {
        await loginGuestIfNeeded();
        const data = await fetchGalleryDetail(this.id);
        this.detail = data || {};
        this.pushRecent(this.detail);
        await this.loadRecommend();
      } catch (_error) {
        this.detail = {};
        this.recommendList = [];
      } finally {
        this.loading = false;
      }
    },
    async loadRecommend() {
      this.recommendLoading = true;
      try {
        const data = await fetchGallery(1, 12, "HOT");
        const list = (data && data.list) || [];
        this.recommendList = list.filter((item) => Number(item.id) !== Number(this.id)).slice(0, 4);
      } catch (_error) {
        this.recommendList = [];
      } finally {
        this.recommendLoading = false;
      }
    },
    async toggleLike() {
      if (!this.detail || !this.detail.id || this.likeLoading) {
        return;
      }
      this.likeLoading = true;
      const currentLiked = Boolean(this.detail.isLiked);
      try {
        const data = currentLiked
          ? await unlikeGallery(this.detail.id)
          : await likeGallery(this.detail.id);
        this.detail = Object.assign({}, this.detail, {
          isLiked: Boolean(data && data.liked),
          likeCount: Number((data && data.likeCount) || 0),
        });
      } catch (error) {
        uni.showToast({ title: error.message || "点赞操作失败", icon: "none" });
      } finally {
        this.likeLoading = false;
      }
    },
    openRecommend(item) {
      if (!item || !item.id) {
        return;
      }
      this.pushRecent(item);
      uni.redirectTo({ url: `/pages-gallery/detail?id=${item.id}` });
    },
    pushRecent(item) {
      if (!item || !item.id) {
        return;
      }
      const key = "gallery_recent_views_v1";
      const stored = uni.getStorageSync(key);
      const rows = Array.isArray(stored) ? stored : [];
      const normalized = [];
      rows.forEach((row) => {
        if (!row || !row.id) {
          return;
        }
        if (Number(row.id) === Number(item.id)) {
          return;
        }
        normalized.push(row);
      });
      normalized.unshift({
        id: item.id,
        title: item.title || "",
        coverImageUrl: item.coverImageUrl || "",
      });
      uni.setStorageSync(key, normalized.slice(0, 20));
    },
    goHome() {
      uni.navigateBack({
        fail: () => {
          uni.switchTab({ url: "/pages/home/index" });
        },
      });
    },
    goDIY() {
      uni.switchTab({ url: "/pages/diy/index" });
    },
    resolveImage(url) {
      if (!url) {
        return "/static/logo.png";
      }
      return resolveAssetUrl(url);
    },
    formatDate(value) {
      if (!value) {
        return "-";
      }
      return String(value).replace("T", " ").slice(0, 16);
    },
  },
};
</script>

<style scoped>
.container {
  min-height: 100vh;
  padding: 24rpx;
  padding-bottom: 220rpx;
}

.loading-card {
  min-height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 22rpx;
}

.cover-card {
  padding: 0;
  overflow: hidden;
  position: relative;
  border-radius: 24rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.24);
  box-shadow: 0 14rpx 28rpx rgba(34, 54, 43, 0.12);
}

.cover {
  width: 100%;
  height: 620rpx;
  background: #e7decd;
}

.cover-mask {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 42rpx 30rpx 30rpx;
  background: linear-gradient(180deg, rgba(14, 20, 17, 0) 0%, rgba(14, 20, 17, 0.78) 100%);
}

.cover-title {
  color: #fff;
  font-size: 40rpx;
  font-weight: 800;
  letter-spacing: 2rpx;
}

.main-card {
  margin-top: 24rpx;
  padding: 30rpx;
  border-radius: 22rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.18);
  background: linear-gradient(160deg, #fffdf8 0%, #f6efe2 100%);
  box-shadow: 0 8rpx 18rpx rgba(35, 56, 45, 0.08);
}

.meta-row {
  display: flex;
  gap: 14rpx;
  flex-wrap: wrap;
}

.count-row {
  margin-top: 16rpx;
}

.meta-chip {
  padding: 8rpx 20rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.2);
  background: rgba(255, 252, 246, 0.9);
  color: #74857a;
  font-size: 24rpx;
}

.tags {
  margin-top: 20rpx;
  display: flex;
  gap: 10rpx;
  flex-wrap: wrap;
}

.tag {
  padding: 8rpx 20rpx;
  border-radius: 8rpx;
  border: 1rpx solid rgba(62, 111, 87, 0.24);
  background: #edf4ef;
  color: #3e6f57;
  font-size: 24rpx;
}

.like-btn {
  margin-top: 30rpx;
  min-height: 82rpx;
  border-radius: 999rpx;
  border: 2rpx solid rgba(62, 111, 87, 0.62);
  background: rgba(255, 252, 248, 0.86);
  color: #3b6852;
  font-size: 28rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.like-btn.active {
  color: #fff;
  border-color: transparent;
  background: linear-gradient(135deg, #5a8c72 0%, #3e6f57 100%);
  box-shadow: 0 8rpx 16rpx rgba(62, 111, 87, 0.2);
}

.like-btn.disabled {
  opacity: 0.65;
  pointer-events: none;
}

.like-btn:active {
  transform: scale(0.98);
}

.note-card {
  margin-top: 24rpx;
  padding: 30rpx;
  border-radius: 22rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.18);
  background: linear-gradient(160deg, #fffdf9 0%, #f7f0e3 100%);
  box-shadow: 0 8rpx 18rpx rgba(35, 56, 45, 0.08);
}

.note-title {
  margin-bottom: 16rpx;
  padding-left: 20rpx;
  position: relative;
  color: #1f2f27;
  font-size: 30rpx;
  font-weight: 700;
}

.note-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 7rpx;
  height: 24rpx;
  border-radius: 8rpx;
  background: linear-gradient(180deg, #c39e67 0%, #3e6f57 100%);
}

.note-line {
  margin-bottom: 8rpx;
  color: #5e7266;
  line-height: 1.65;
  font-size: 26rpx;
}

.recommend-list {
  display: grid;
  gap: 20rpx;
  padding-bottom: 20rpx;
}

.recommend-item {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 24rpx;
  border-radius: 22rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.2);
  background: linear-gradient(160deg, #fffdf8 0%, #f6efe2 100%);
  box-shadow: 0 8rpx 16rpx rgba(35, 56, 45, 0.08);
}

.recommend-image {
  width: 140rpx;
  height: 140rpx;
  border-radius: 16rpx;
  background: #e8decd;
  border: 2rpx solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 6rpx 12rpx rgba(35, 56, 45, 0.1);
  flex-shrink: 0;
}

.recommend-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.recommend-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1f2f27;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recommend-link {
  margin-top: 16rpx;
  align-self: flex-start;
  padding-bottom: 2rpx;
  border-bottom: 2rpx solid #be9960;
  color: #a58350;
  font-size: 24rpx;
  font-weight: 600;
}

.bottom-space {
  height: 40rpx;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: grid;
  grid-template-columns: 1fr 1.5fr;
  gap: 20rpx;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid rgba(185, 152, 100, 0.2);
  background: rgba(255, 252, 247, 0.95);
  backdrop-filter: blur(12px);
}

.btn-outline,
.btn-primary {
  min-height: 84rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 700;
}

.btn-outline {
  border: 2rpx solid rgba(62, 111, 87, 0.35);
  background: rgba(255, 252, 248, 0.92);
  color: #4f6458;
}

.btn-primary {
  color: #fff;
  border: none;
  background: linear-gradient(135deg, #5a8c72 0%, #3e6f57 100%);
  box-shadow: 0 8rpx 16rpx rgba(62, 111, 87, 0.2);
}

.btn-outline:active,
.btn-primary:active {
  transform: scale(0.98);
}
</style>
