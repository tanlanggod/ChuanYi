<template>
  <view class="container">
    <view class="search-hero">
      <view class="search-box">
        <input
          v-model="keywordInput"
          class="search-input"
          placeholder="搜索产品"
          confirm-type="search"
          @confirm="doSearch"
        />
        <view class="search-btn" @click="doSearch">搜索</view>
      </view>
    </view>

    <scroll-view class="category-scroll" scroll-x="true">
      <view class="category-row">
        <view class="category-card" :class="{ active: activeCategory === null }" @click="selectCategory(null)">
          <view class="category-name">全部好物</view>
          <view class="category-count">{{ totalCategoryCount }} 个商品</view>
        </view>
        <view
          class="category-card"
          :class="{ active: activeCategory === item.id }"
          v-for="item in categories"
          :key="item.id"
          @click="selectCategory(item.id)"
        >
          <view class="category-name">{{ item.name }}</view>
          <view class="category-count">{{ item.goodsCount || 0 }} 个商品</view>
        </view>
      </view>
    </scroll-view>

    <scroll-view class="sort-scroll" scroll-x="true">
      <view class="sort-row">
        <view
          class="sort-chip"
          :class="{ active: sortType === item.value }"
          v-for="item in sortOptions"
          :key="item.value"
          @click="changeSort(item.value)"
        >
          {{ item.label }}
        </view>
      </view>
    </scroll-view>

    <view class="section-title">商品列表</view>
    <view v-if="goods.length === 0" class="card">
      <view class="muted">暂无好物数据</view>
    </view>
    <view v-else class="list">
      <view class="card item" v-for="item in goods" :key="item.spuId" @click="openDetail(item.spuId)">
        <image class="cover" :src="resolveImage(item.coverImageUrl)" mode="aspectFill"></image>
        <view class="main">
          <view class="item-title">{{ item.title }}</view>
          <view class="muted two-line">{{ item.subtitle || "-" }}</view>
          <view class="tags" v-if="item.tags && item.tags.length > 0">
            <view class="tag" v-for="tag in item.tags" :key="tag">{{ tag }}</view>
          </view>
          <view class="row">
            <view class="price">￥{{ formatAmount(item.minPrice) }}</view>
            <view class="detail-btn">查看详情</view>
          </view>
        </view>
      </view>
    </view>

    <view class="bottom-tip">已加载全部数据</view>
  </view>
</template>

<script>
import { fetchGoodsCategories, fetchGoodsList } from "../../common/api";
import { resolveAssetUrl } from "../../common/http";
import { loginGuestIfNeeded } from "../../common/session";

export default {
  data() {
    return {
      categories: [],
      goods: [],
      activeCategory: null,
      keywordInput: "",
      keyword: "",
      sortType: "DEFAULT",
      sortOptions: [
        { label: "默认排序", value: "DEFAULT" },
        { label: "价格从低到高", value: "PRICE_ASC" },
        { label: "价格从高到低", value: "PRICE_DESC" },
        { label: "最新上架", value: "NEWEST" },
      ],
    };
  },
  computed: {
    totalCategoryCount() {
      return this.categories.reduce((sum, item) => sum + Number(item.goodsCount || 0), 0);
    },
  },
  async onShow() {
    await this.loadCategories();
    await this.loadGoods();
  },
  async onPullDownRefresh() {
    await this.loadGoods();
    uni.stopPullDownRefresh();
  },
  methods: {
    async loadCategories() {
      try {
        await loginGuestIfNeeded();
        const data = await fetchGoodsCategories(true);
        this.categories = (data && data.categories) || [];
      } catch (error) {
        uni.showToast({ title: "分类加载失败", icon: "none" });
      }
    },
    async loadGoods() {
      try {
        const data = await fetchGoodsList({
          categoryId: this.activeCategory || undefined,
          keyword: this.keyword || undefined,
          sortType: this.sortType || "DEFAULT",
          pageNo: 1,
          pageSize: 20,
        });
        this.goods = (data && data.list) || [];
      } catch (error) {
        uni.showToast({ title: "好物加载失败", icon: "none" });
      }
    },
    async selectCategory(categoryId) {
      this.activeCategory = categoryId;
      await this.loadGoods();
    },
    async changeSort(sortType) {
      this.sortType = sortType;
      await this.loadGoods();
    },
    async doSearch() {
      this.keyword = (this.keywordInput || "").trim();
      await this.loadGoods();
    },
    openDetail(spuId) {
      uni.navigateTo({ url: `/pages-goods/detail?spuId=${spuId}` });
    },
    resolveImage(url) {
      if (!url) {
        return "/static/logo.png";
      }
      return resolveAssetUrl(url);
    },
    formatAmount(value) {
      const text = Number(value || 0).toFixed(2);
      return text.replace(/\.00$/, "").replace(/(\.\d)0$/, "$1");
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

.category-scroll {
  white-space: nowrap;
  margin-top: 10rpx;
  margin-bottom: 16rpx;
}

.category-row {
  display: inline-flex;
  gap: 16rpx;
  padding-bottom: 10rpx;
  padding-left: 4rpx;
  padding-right: 4rpx;
}

.category-card {
  width: 220rpx;
  border-radius: 16rpx;
  background: #fff;
  border: 2rpx solid transparent;
  padding: 20rpx 16rpx;
  box-sizing: border-box;
  box-shadow: 0 4rpx 12rpx rgba(47, 54, 50, 0.04);
  transition: all 0.2s ease;
}

.category-card.active {
  border-color: #3B6E53;
  background: #F0F5F2;
}

.category-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A241D;
}

.category-count {
  margin-top: 8rpx;
  color: #8C968F;
  font-size: 22rpx;
}

.sort-scroll {
  white-space: nowrap;
  margin-bottom: 16rpx;
}

.sort-row {
  display: inline-flex;
  gap: 16rpx;
  padding-bottom: 4rpx;
}

.sort-chip {
  padding: 10rpx 24rpx;
  border-radius: 999rpx;
  background: #fff;
  color: #556B5D;
  font-size: 24rpx;
  border: 1rpx solid #E8E4DB;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.02);
  transition: all 0.2s ease;
}

.sort-chip.active {
  background: #3B6E53;
  color: #fff;
  border-color: #3B6E53;
  font-weight: 500;
}

.list {
  display: grid;
  gap: 20rpx;
  padding-bottom: 30rpx;
}

.item {
  display: flex;
  gap: 24rpx;
  padding: 24rpx;
  align-items: center;
}

.cover {
  width: 180rpx;
  height: 180rpx;
  border-radius: 16rpx;
  background: #E8E4DB;
  flex-shrink: 0;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.04);
  border: 1rpx solid rgba(232, 228, 219, 0.5);
}

.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 170rpx;
}

.item-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1A241D;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.two-line {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #7A8B7C;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tags {
  margin-top: 10rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
}

.tag {
  font-size: 20rpx;
  color: #3B6E53;
  background: #F0F5F2;
  border: 1rpx solid #D1E0D7;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}

.row {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  color: #C2A578;
  font-weight: 700;
  font-size: 34rpx;
  font-family: -apple-system, BlinkMacSystemFont, "Helvetica Neue", Helvetica, Arial, sans-serif;
}

.detail-btn {
  background: #FAF8F5;
  color: #3B6E53;
  border: 1rpx solid #3B6E53;
  border-radius: 999rpx;
  padding: 8rpx 24rpx;
  font-size: 22rpx;
  font-weight: 500;
}

.bottom-tip {
  margin-top: 30rpx;
  margin-bottom: 40rpx;
  text-align: center;
  color: #8C968F;
  font-size: 24rpx;
  letter-spacing: 1rpx;
}
</style>
