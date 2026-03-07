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
  min-height: 100vh;
  padding: 24rpx;
}

.search-hero {
  margin-bottom: 20rpx;
  padding: 22rpx;
  border-radius: 24rpx;
  background: linear-gradient(145deg, #5b8d73 0%, #3f7058 58%, #2f5a46 100%);
  box-shadow: 0 14rpx 26rpx rgba(34, 56, 45, 0.18);
}

.search-box {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 10rpx 14rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.85);
  background: rgba(255, 255, 255, 0.95);
}

.search-input {
  flex: 1;
  padding: 8rpx 12rpx;
  font-size: 26rpx;
  color: #1f2f27;
}

.search-btn {
  padding: 12rpx 24rpx;
  border-radius: 999rpx;
  background: #355f4b;
  color: #fff;
  font-size: 24rpx;
  font-weight: 600;
}

.tag-scroll,
.recent-scroll {
  white-space: nowrap;
}

.tag-scroll {
  margin-bottom: 20rpx;
}

.tag-row,
.recent-row {
  display: inline-flex;
}

.tag-row {
  gap: 14rpx;
  padding-bottom: 4rpx;
}

.filter-chip,
.sort-chip {
  padding: 10rpx 24rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.25);
  background: rgba(255, 252, 246, 0.92);
  color: #5d7265;
  font-size: 24rpx;
}

.filter-chip.active,
.sort-chip.active {
  color: #fff;
  font-weight: 600;
  border-color: transparent;
  background: linear-gradient(135deg, #5a8c72 0%, #3e6f57 100%);
}

.section-row {
  margin: 8rpx 0 16rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 8rpx;
}

.section-title-small {
  font-size: 28rpx;
  font-weight: 700;
  color: #1f2f27;
}

.clear-btn {
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.3);
  background: #fbf4e8;
  color: #a68450;
  font-size: 24rpx;
}

.empty-recent {
  min-height: 160rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 22rpx;
}

.recent-scroll {
  margin-bottom: 24rpx;
}

.recent-row {
  gap: 14rpx;
  padding-bottom: 10rpx;
}

.recent-item {
  width: 204rpx;
  box-sizing: border-box;
  padding: 16rpx;
  border-radius: 16rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.2);
  background: linear-gradient(160deg, #fffdf9 0%, #f7f0e3 100%);
  box-shadow: 0 6rpx 12rpx rgba(35, 56, 45, 0.08);
}

.recent-item:active {
  transform: scale(0.98);
}

.recent-image {
  width: 168rpx;
  height: 168rpx;
  border-radius: 14rpx;
  background: #e7decd;
}

.recent-title {
  margin-top: 12rpx;
  height: 66rpx;
  font-size: 24rpx;
  line-height: 1.4;
  color: #2a3a32;
  font-weight: 500;
  white-space: normal;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.summary-row {
  margin: 20rpx 0 14rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 8rpx;
}

.summary-text {
  font-size: 24rpx;
  color: #7b8980;
}

.sort-row {
  margin-bottom: 20rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.loading-card {
  min-height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 22rpx;
}

.list {
  display: grid;
  gap: 20rpx;
  padding-bottom: 20rpx;
}

.item {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 24rpx;
  border-radius: 22rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.2);
  background: linear-gradient(160deg, #fffdf8 0%, #f6efe2 100%);
  box-shadow: 0 8rpx 16rpx rgba(35, 56, 45, 0.08);
}

.item-image {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background: #e8decd;
  border: 2rpx solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 6rpx 12rpx rgba(35, 56, 45, 0.1);
  flex-shrink: 0;
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
  color: #1f2f27;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-sub {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #76847a;
}

.item-tags {
  margin-top: 10rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
}

.item-tag {
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
  border: 1rpx solid rgba(62, 111, 87, 0.25);
  background: #edf4ef;
  color: #3e6f57;
  font-size: 20rpx;
}

.metric-row {
  margin-top: 12rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.metric {
  font-size: 22rpx;
  color: #8b978e;
}

.item-time {
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #a58350;
}

.loadmore {
  padding: 30rpx 0;
  text-align: center;
}
</style>
