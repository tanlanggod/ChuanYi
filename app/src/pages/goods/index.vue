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

.category-scroll {
  white-space: nowrap;
  margin-top: 12rpx;
}

.category-row {
  display: inline-flex;
  gap: 10rpx;
  padding-bottom: 2rpx;
}

.category-card {
  width: 210rpx;
  border-radius: 14rpx;
  background: #fff;
  border: 2rpx solid #eef2f7;
  padding: 16rpx 14rpx;
  box-sizing: border-box;
}

.category-card.active {
  border-color: #fca5a5;
  background: #fff7f7;
}

.category-name {
  font-size: 28rpx;
  font-weight: 700;
  color: #111827;
}

.category-count {
  margin-top: 6rpx;
  color: #8a8f99;
  font-size: 22rpx;
}

.sort-scroll {
  white-space: nowrap;
  margin-top: 12rpx;
  margin-bottom: 8rpx;
}

.sort-row {
  display: inline-flex;
  gap: 10rpx;
}

.sort-chip {
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: #fff;
  color: #4b5563;
  font-size: 22rpx;
  border: 1rpx solid #eceff3;
}

.sort-chip.active {
  background: #d62b2b;
  color: #fff;
  border-color: #d62b2b;
}

.list {
  display: grid;
  gap: 12rpx;
}

.item {
  display: flex;
  gap: 14rpx;
}

.cover {
  width: 170rpx;
  height: 170rpx;
  border-radius: 12rpx;
  background: #f3f4f6;
  flex-shrink: 0;
}

.main {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 30rpx;
  font-weight: 700;
}

.two-line {
  margin-top: 8rpx;
  line-height: 1.5;
}

.tags {
  margin-top: 8rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
}

.tag {
  font-size: 20rpx;
  color: #b42318;
  background: #fef2f2;
  border: 1rpx solid #fecaca;
  padding: 4rpx 10rpx;
  border-radius: 999rpx;
}

.row {
  margin-top: 12rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  color: #d62b2b;
  font-weight: 700;
  font-size: 30rpx;
}

.detail-btn {
  background: #111827;
  color: #fff;
  border-radius: 999rpx;
  padding: 8rpx 18rpx;
  font-size: 22rpx;
}

.bottom-tip {
  margin-top: 18rpx;
  text-align: center;
  color: #9ca3af;
  font-size: 22rpx;
}
</style>
