<template>
  <view class="container">
    <view class="search-hero">
      <view class="search-box">
        <input
          v-model="keywordInput"
          class="search-input"
          placeholder="搜索作品/作者"
          confirm-type="search"
          @confirm="doSearch"
        />
        <view class="search-btn" @click="doSearch">搜索</view>
      </view>
    </view>

    <scroll-view class="tag-scroll" scroll-x="true">
      <view class="tag-row">
        <view class="filter-chip" :class="{ active: activeTag === '' }" @click="changeTag('')">全部</view>
        <view
          class="filter-chip"
          :class="{ active: activeTag === tag }"
          v-for="tag in tagOptions"
          :key="tag"
          @click="changeTag(tag)"
        >
          {{ tag }}
        </view>
      </view>
    </scroll-view>

    <view class="section-row">
      <view class="section-title-small">最近浏览</view>
      <view class="clear-btn" v-if="recentList.length > 0" @click="clearRecent">清空</view>
    </view>
    <view class="card empty-recent" v-if="recentList.length === 0">
      <view class="muted">还没有浏览记录</view>
    </view>
    <scroll-view class="recent-scroll" scroll-x="true" v-else>
      <view class="recent-row">
        <view class="recent-item" v-for="item in recentList" :key="item.id" @click="openRecent(item)">
          <image class="recent-image" :src="resolveImage(item.coverImageUrl)" mode="aspectFill"></image>
          <view class="recent-title">{{ item.title || "未命名作品" }}</view>
        </view>
      </view>
    </scroll-view>

    <view class="summary-row">
      <view class="summary-text">当前展示 {{ displayList.length }} / {{ list.length }} 条</view>
      <view class="summary-text muted" v-if="hasMore">继续上滑加载更多</view>
    </view>

    <view class="sort-row">
      <view class="sort-chip" :class="{ active: sortType === 'PUBLISH_TIME' }" @click="changeSort('PUBLISH_TIME')">
        按发布时间
      </view>
      <view class="sort-chip" :class="{ active: sortType === 'HOT' }" @click="changeSort('HOT')">按热度</view>
    </view>

    <view class="card loading-card" v-if="loading">
      <view class="muted">广场作品加载中...</view>
    </view>

    <view class="card loading-card" v-else-if="displayList.length === 0">
      <view class="muted">暂无符合条件的作品</view>
    </view>

    <view class="list" v-else>
      <view class="card item" v-for="item in displayList" :key="item.id" @click="openDetail(item)">
        <image class="item-image" :src="resolveImage(item.coverImageUrl)" mode="aspectFill"></image>
        <view class="item-main">
          <view class="item-title">{{ item.title || "未命名作品" }}</view>
          <view class="item-sub">@{{ item.authorNickname || "匿名用户" }}</view>
          <view class="item-tags" v-if="item.tags && item.tags.length > 0">
            <view class="item-tag" v-for="tag in item.tags.slice(0, 3)" :key="tag">{{ tag }}</view>
          </view>
          <view class="metric-row">
            <view class="metric">点赞 {{ Number(item.likeCount || 0) }}</view>
            <view class="metric">浏览 {{ Number(item.viewCount || 0) }}</view>
            <view class="metric" v-if="sortType === 'HOT'">热度 {{ Number(item.hotScore || 0) }}</view>
          </view>
          <view class="item-time">{{ formatDate(item.publishedAt) }}</view>
        </view>
      </view>
    </view>

    <view class="loadmore" v-if="!loading">
      <view class="muted" v-if="loadingMore">正在加载更多...</view>
      <view class="muted" v-else-if="hasMore">上滑可加载更多</view>
      <view class="muted" v-else>已加载全部数据</view>
    </view>
  </view>
</template>

<script>
import { fetchGallery } from "../common/api";
import { resolveAssetUrl } from "../common/http";
import { loginGuestIfNeeded } from "../common/session";

export default {
  data() {
    return {
      list: [],
      recentList: [],
      loading: false,
      loadingMore: false,
      keywordInput: "",
      keyword: "",
      activeTag: "",
      sortType: "PUBLISH_TIME",
      pageNo: 1,
      pageSize: 12,
      total: 0,
      needRefreshOnShow: false,
    };
  },
  computed: {
    tagOptions() {
      const map = {};
      this.list.forEach((item) => {
        const tags = (item && item.tags) || [];
        tags.forEach((tag) => {
          if (tag) {
            map[tag] = Number(map[tag] || 0) + 1;
          }
        });
      });
      return Object.keys(map)
        .sort((a, b) => {
          const diff = Number(map[b] || 0) - Number(map[a] || 0);
          if (diff !== 0) {
            return diff;
          }
          return String(a).localeCompare(String(b));
        })
        .slice(0, 20);
    },
    filteredList() {
      const keyword = (this.keyword || "").toLowerCase();
      return this.list.filter((item) => {
        const tags = (item && item.tags) || [];
        const passTag = !this.activeTag || tags.includes(this.activeTag);
        if (!passTag) {
          return false;
        }
        if (!keyword) {
          return true;
        }
        const title = String((item && item.title) || "").toLowerCase();
        const author = String((item && item.authorNickname) || "").toLowerCase();
        return title.includes(keyword) || author.includes(keyword);
      });
    },
    displayList() {
      return this.filteredList;
    },
    hasMore() {
      if (this.total <= 0) {
        return false;
      }
      return this.list.length < this.total;
    },
  },
  async onLoad() {
    await this.reload();
  },
  onShow() {
    this.loadRecentViews();
    if (this.needRefreshOnShow) {
      this.needRefreshOnShow = false;
      this.reload();
    }
  },
  async onPullDownRefresh() {
    await this.reload();
    uni.stopPullDownRefresh();
  },
  async onReachBottom() {
    await this.loadMore();
  },
  methods: {
    async reload() {
      this.pageNo = 1;
      this.total = 0;
      this.list = [];
      this.loading = true;
      try {
        await loginGuestIfNeeded();
        this.loadRecentViews();
        await this.loadMore(true);
      } catch (_error) {
        uni.showToast({ title: "广场加载失败", icon: "none" });
      } finally {
        this.loading = false;
      }
    },
    async loadMore(firstLoad = false) {
      if (this.loadingMore) {
        return;
      }
      if (!firstLoad && this.total > 0 && this.list.length >= this.total) {
        return;
      }
      this.loadingMore = true;
      try {
        const data = await fetchGallery(this.pageNo, this.pageSize, this.sortType);
        const rows = (data && data.list) || [];
        const page = (data && data.page) || {};
        this.total = Number(page.total || this.total || 0);
        const idMap = {};
        this.list.forEach((item) => {
          idMap[item.id] = true;
        });
        rows.forEach((item) => {
          if (!idMap[item.id]) {
            this.list.push(item);
            idMap[item.id] = true;
          }
        });
        if (rows.length > 0) {
          this.pageNo += 1;
        }
      } catch (_error) {
        if (!firstLoad) {
          uni.showToast({ title: "加载更多失败", icon: "none" });
        }
      } finally {
        this.loadingMore = false;
      }
    },
    doSearch() {
      this.keyword = (this.keywordInput || "").trim();
    },
    changeTag(tag) {
      this.activeTag = tag || "";
    },
    async changeSort(type) {
      if (type !== "PUBLISH_TIME" && type !== "HOT") {
        return;
      }
      if (this.sortType === type) {
        return;
      }
      this.sortType = type;
      await this.reload();
    },
    openDetail(item) {
      if (!item || !item.id) {
        return;
      }
      this.pushRecent(item);
      this.needRefreshOnShow = true;
      uni.navigateTo({ url: `/pages-gallery/detail?id=${item.id}` });
    },
    openRecent(item) {
      if (!item || !item.id) {
        return;
      }
      this.needRefreshOnShow = true;
      uni.navigateTo({ url: `/pages-gallery/detail?id=${item.id}` });
    },
    clearRecent() {
      const key = "gallery_recent_views_v1";
      uni.removeStorageSync(key);
      this.recentList = [];
    },
    loadRecentViews() {
      const key = "gallery_recent_views_v1";
      const rows = uni.getStorageSync(key);
      if (!Array.isArray(rows)) {
        this.recentList = [];
        return;
      }
      this.recentList = rows
        .filter((item) => item && item.id)
        .slice(0, 12);
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
      const finalRows = normalized.slice(0, 20);
      uni.setStorageSync(key, finalRows);
      this.recentList = finalRows.slice(0, 12);
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
  background: #F9F7F3;
  min-height: 100vh;
}

.search-hero {
  border-radius: 20rpx;
  padding: 20rpx;
  background: linear-gradient(135deg, #4A8365 0%, #3B6E53 100%);
  box-shadow: 0 8rpx 20rpx rgba(59, 110, 83, 0.15);
  margin-bottom: 20rpx;
}

.search-box {
  background: #fff;
  border-radius: 999rpx;
  padding: 10rpx 16rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.search-input {
  flex: 1;
  font-size: 26rpx;
  padding: 8rpx 12rpx;
  color: #2F3632;
}

.search-btn {
  border-radius: 999rpx;
  background: #3B6E53;
  color: #fff;
  font-size: 24rpx;
  padding: 12rpx 24rpx;
  font-weight: 500;
  box-shadow: 0 4rpx 10rpx rgba(59, 110, 83, 0.2);
}

.tag-scroll {
  white-space: nowrap;
  margin-bottom: 20rpx;
}

.tag-row {
  display: inline-flex;
  gap: 16rpx;
  padding-bottom: 4rpx;
}

.filter-chip {
  padding: 10rpx 24rpx;
  border-radius: 999rpx;
  background: #fff;
  color: #556B5D;
  font-size: 24rpx;
  border: 1rpx solid #E8E4DB;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.02);
  transition: all 0.2s ease;
}

.filter-chip.active {
  background: #3B6E53;
  color: #fff;
  border-color: #3B6E53;
  font-weight: 500;
}

.section-row {
  margin-top: 10rpx;
  margin-bottom: 16rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 8rpx;
}

.section-title-small {
  font-size: 28rpx;
  font-weight: 700;
  color: #1A241D;
}

.clear-btn {
  color: #A68754;
  font-size: 24rpx;
  padding: 8rpx 16rpx;
  background: #FDFBF7;
  border-radius: 999rpx;
  border: 1rpx solid #E8E4DB;
}

.empty-recent {
  min-height: 160rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(47, 54, 50, 0.04);
}

.recent-scroll {
  white-space: nowrap;
  margin-bottom: 24rpx;
}

.recent-row {
  display: inline-flex;
  gap: 16rpx;
  padding-bottom: 10rpx;
}

.recent-item {
  width: 200rpx;
  background: #fff;
  border: 1rpx solid rgba(232, 228, 219, 0.6);
  border-radius: 16rpx;
  padding: 16rpx;
  box-sizing: border-box;
  box-shadow: 0 4rpx 12rpx rgba(47, 54, 50, 0.04);
  transition: transform 0.2s;
}

.recent-item:active {
  transform: scale(0.98);
}

.recent-image {
  width: 168rpx;
  height: 168rpx;
  border-radius: 12rpx;
  background: #E8E4DB;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04);
}

.recent-title {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #2F3632;
  line-height: 1.4;
  height: 66rpx;
  white-space: normal;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  font-weight: 500;
}

.summary-row {
  margin-top: 20rpx;
  margin-bottom: 16rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 8rpx;
}

.summary-text {
  font-size: 24rpx;
  color: #7A8B7C;
}

.sort-row {
  margin-bottom: 20rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.sort-chip {
  font-size: 24rpx;
  color: #556B5D;
  border-radius: 999rpx;
  padding: 10rpx 24rpx;
  background: #fff;
  border: 1rpx solid #E8E4DB;
  transition: all 0.2s;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.02);
}

.sort-chip.active {
  color: #fff;
  background: linear-gradient(135deg, #4A8365 0%, #3B6E53 100%);
  border-color: #3B6E53;
  font-weight: 500;
  box-shadow: 0 4rpx 10rpx rgba(59, 110, 83, 0.15);
}

.loading-card {
  min-height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(47, 54, 50, 0.04);
}

.list {
  display: grid;
  gap: 20rpx;
  padding-bottom: 20rpx;
}

.item {
  display: flex;
  gap: 24rpx;
  align-items: center;
  padding: 24rpx;
  border-radius: 20rpx;
  background: #fff;
  box-shadow: 0 4rpx 16rpx rgba(47, 54, 50, 0.04);
  border: 1rpx solid rgba(232, 228, 219, 0.5);
}

.item-image {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background: #E8E4DB;
  flex-shrink: 0;
  box-shadow: 0 4rpx 10rpx rgba(0,0,0,0.06);
  border: 2rpx solid #F0F5F2;
}

.item-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.item-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1A241D;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-sub {
  margin-top: 8rpx;
  color: #7A8B7C;
  font-size: 24rpx;
}

.item-tags {
  margin-top: 10rpx;
  display: flex;
  gap: 12rpx;
  flex-wrap: wrap;
}

.item-tag {
  font-size: 20rpx;
  color: #3B6E53;
  background: #F0F5F2;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
  border: 1rpx solid #D1E0D7;
}

.metric-row {
  margin-top: 12rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.metric {
  font-size: 22rpx;
  color: #8C968F;
}

.item-time {
  margin-top: 10rpx;
  color: #A68754;
  font-size: 22rpx;
}

.loadmore {
  padding: 30rpx 0;
  text-align: center;
}
</style>
