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
}

.search-hero {
  border-radius: 16rpx;
  padding: 14rpx;
  background: linear-gradient(120deg, #d62b2b 0%, #f97316 100%);
}

.search-box {
  background: #fff;
  border-radius: 999rpx;
  padding: 8rpx 10rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.search-input {
  flex: 1;
  font-size: 24rpx;
  padding: 8rpx 12rpx;
}

.search-btn {
  border-radius: 999rpx;
  background: #d62b2b;
  color: #fff;
  font-size: 22rpx;
  padding: 10rpx 18rpx;
}

.tag-scroll {
  white-space: nowrap;
  margin-top: 12rpx;
}

.tag-row {
  display: inline-flex;
  gap: 10rpx;
}

.section-row {
  margin-top: 12rpx;
  margin-bottom: 8rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title-small {
  font-size: 26rpx;
  font-weight: 700;
  color: #111827;
}

.clear-btn {
  color: #d62b2b;
  font-size: 22rpx;
}

.empty-recent {
  min-height: 120rpx;
  display: grid;
  place-items: center;
}

.recent-scroll {
  white-space: nowrap;
}

.recent-row {
  display: inline-flex;
  gap: 12rpx;
  padding-bottom: 2rpx;
}

.recent-item {
  width: 170rpx;
  background: #fff;
  border: 1rpx solid #eef1f5;
  border-radius: 12rpx;
  padding: 10rpx;
  box-sizing: border-box;
}

.recent-image {
  width: 150rpx;
  height: 150rpx;
  border-radius: 10rpx;
  background: #f3f4f6;
}

.recent-title {
  margin-top: 8rpx;
  font-size: 20rpx;
  color: #374151;
  line-height: 1.5;
  min-height: 60rpx;
}

.filter-chip {
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: #fff;
  color: #4b5563;
  font-size: 22rpx;
  border: 1rpx solid #eceff3;
}

.filter-chip.active {
  background: #d62b2b;
  color: #fff;
  border-color: #d62b2b;
}

.summary-row {
  margin-top: 12rpx;
  margin-bottom: 8rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10rpx;
}

.summary-text {
  font-size: 22rpx;
  color: #374151;
}

.sort-row {
  margin-bottom: 10rpx;
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.sort-chip {
  font-size: 22rpx;
  color: #4b5563;
  border-radius: 999rpx;
  padding: 8rpx 18rpx;
  background: #fff;
  border: 1rpx solid #e5e7eb;
}

.sort-chip.active {
  color: #fff;
  background: #d62b2b;
  border-color: #d62b2b;
}

.loading-card {
  min-height: 220rpx;
  display: grid;
  place-items: center;
}

.list {
  display: grid;
  gap: 12rpx;
}

.item {
  display: flex;
  gap: 12rpx;
  align-items: center;
}

.item-image {
  width: 118rpx;
  height: 118rpx;
  border-radius: 50%;
  background: #f3f4f6;
  flex-shrink: 0;
}

.item-main {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #111827;
}

.item-sub {
  margin-top: 6rpx;
  color: #8a8f99;
  font-size: 22rpx;
}

.item-tags {
  margin-top: 8rpx;
  display: flex;
  gap: 8rpx;
  flex-wrap: wrap;
}

.item-tag {
  font-size: 20rpx;
  color: #b42318;
  background: #fbe5e5;
  padding: 4rpx 10rpx;
  border-radius: 999rpx;
}

.metric-row {
  margin-top: 8rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.metric {
  font-size: 21rpx;
  color: #6b7280;
}

.item-time {
  margin-top: 8rpx;
  color: #9ca3af;
  font-size: 20rpx;
}

.loadmore {
  padding: 16rpx 0 10rpx;
  text-align: center;
}
</style>
