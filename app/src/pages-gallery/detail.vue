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
  padding: 24rpx;
  padding-bottom: 190rpx;
}

.loading-card {
  min-height: 220rpx;
  display: grid;
  place-items: center;
}

.cover-card {
  padding: 0;
  overflow: hidden;
  position: relative;
}

.cover {
  width: 100%;
  height: 520rpx;
  background: #f3f4f6;
}

.cover-mask {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 18rpx;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0) 0%, rgba(0, 0, 0, 0.58) 100%);
}

.cover-title {
  color: #fff;
  font-size: 32rpx;
  font-weight: 700;
}

.main-card {
  margin-top: 12rpx;
}

.meta-row {
  display: flex;
  gap: 10rpx;
  flex-wrap: wrap;
}

.count-row {
  margin-top: 10rpx;
}

.meta-chip {
  font-size: 22rpx;
  color: #6b7280;
  background: #f5f6f8;
  border-radius: 999rpx;
  padding: 6rpx 14rpx;
}

.tags {
  margin-top: 12rpx;
  display: flex;
  gap: 8rpx;
  flex-wrap: wrap;
}

.tag {
  font-size: 22rpx;
  color: #b42318;
  background: #fbe5e5;
  border-radius: 999rpx;
  padding: 6rpx 14rpx;
}

.like-btn {
  margin-top: 14rpx;
  border-radius: 999rpx;
  border: 1px solid #d62b2b;
  color: #d62b2b;
  text-align: center;
  min-height: 66rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 700;
}

.like-btn.active {
  background: #d62b2b;
  color: #fff;
}

.like-btn.disabled {
  opacity: 0.65;
}

.note-card {
  margin-top: 12rpx;
}

.note-title {
  font-size: 28rpx;
  font-weight: 700;
  margin-bottom: 8rpx;
  color: #111827;
}

.note-line {
  color: #6b7280;
  line-height: 1.7;
  font-size: 24rpx;
}

.recommend-list {
  display: grid;
  gap: 12rpx;
}

.recommend-item {
  display: flex;
  gap: 12rpx;
  align-items: center;
}

.recommend-image {
  width: 118rpx;
  height: 118rpx;
  border-radius: 50%;
  background: #f3f4f6;
  flex-shrink: 0;
}

.recommend-main {
  flex: 1;
  min-width: 0;
}

.recommend-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #111827;
}

.recommend-link {
  margin-top: 8rpx;
  color: #111827;
  font-size: 22rpx;
  border-bottom: 2rpx solid #111827;
  display: inline-block;
}

.bottom-space {
  height: 24rpx;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  border-top: 1rpx solid #e9edf2;
  padding: 14rpx 24rpx;
  padding-bottom: calc(14rpx + env(safe-area-inset-bottom));
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10rpx;
  box-shadow: 0 -8rpx 24rpx rgba(15, 23, 42, 0.06);
}

.btn-outline,
.btn-primary {
  border-radius: 999rpx;
  min-height: 70rpx;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 24rpx;
  font-weight: 700;
}

.btn-outline {
  border: 1px solid #d62b2b;
  color: #d62b2b;
  background: #fff;
}
</style>
