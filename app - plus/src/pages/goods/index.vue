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
  min-height: 100vh;
  padding: 24rpx;
}

.search-hero {
  margin-bottom: 20rpx;
  padding: 22rpx;
  border-radius: 24rpx;
  background: linear-gradient(145deg, #5b8e73 0%, #3e6f57 58%, #2f5b46 100%);
  box-shadow: 0 14rpx 26rpx rgba(35, 57, 45, 0.16);
}

.search-box {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 10rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.95);
  border: 1rpx solid rgba(255, 255, 255, 0.85);
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
  background: #365f4b;
  color: #fff;
  font-size: 24rpx;
  font-weight: 600;
}

.category-scroll,
.sort-scroll {
  white-space: nowrap;
}

.category-scroll {
  margin: 8rpx 0 14rpx;
}

.category-row {
  display: inline-flex;
  gap: 14rpx;
  padding: 2rpx 4rpx 10rpx;
}

.category-card {
  width: 226rpx;
  padding: 20rpx 16rpx;
  box-sizing: border-box;
  border-radius: 18rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.2);
  background: linear-gradient(155deg, #fffdf9 0%, #f8f1e6 100%);
  box-shadow: 0 6rpx 14rpx rgba(35, 56, 45, 0.06);
}

.category-card.active {
  border-color: rgba(62, 111, 87, 0.58);
  background: linear-gradient(150deg, #eef5f1 0%, #e1ece6 100%);
  box-shadow: 0 8rpx 16rpx rgba(35, 56, 45, 0.09);
}

.category-name {
  font-size: 28rpx;
  font-weight: 700;
  color: #213129;
}

.category-count {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #86948a;
}

.sort-scroll {
  margin-bottom: 18rpx;
}

.sort-row {
  display: inline-flex;
  gap: 14rpx;
  padding: 0 2rpx 4rpx;
}

.sort-chip {
  padding: 10rpx 24rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.25);
  background: rgba(255, 252, 246, 0.92);
  color: #5d7265;
  font-size: 24rpx;
}

.sort-chip.active {
  color: #fff;
  font-weight: 600;
  border-color: transparent;
  background: linear-gradient(135deg, #5a8c72 0%, #3e6f57 100%);
}

.list {
  display: grid;
  gap: 20rpx;
  padding-bottom: 30rpx;
}

.item {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 24rpx;
  border-radius: 22rpx;
}

.cover {
  width: 180rpx;
  height: 180rpx;
  border-radius: 18rpx;
  flex-shrink: 0;
  background: #e8decd;
  border: 1rpx solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 8rpx 14rpx rgba(35, 56, 45, 0.1);
}

.main {
  flex: 1;
  min-width: 0;
  height: 172rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.item-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1f2f27;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.two-line {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #77867c;
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
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  border: 1rpx solid rgba(62, 111, 87, 0.24);
  background: #edf4ef;
  color: #3e6f57;
  font-size: 20rpx;
}

.row {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.price {
  font-size: 34rpx;
  font-weight: 700;
  color: #b08d58;
  font-family: -apple-system, BlinkMacSystemFont, "Helvetica Neue", Helvetica, Arial, sans-serif;
}

.detail-btn {
  padding: 8rpx 24rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(62, 111, 87, 0.52);
  background: rgba(255, 252, 246, 0.9);
  color: #3b6751;
  font-size: 22rpx;
  font-weight: 600;
}

.bottom-tip {
  margin: 30rpx 0 40rpx;
  text-align: center;
  color: #8b978d;
  font-size: 24rpx;
  letter-spacing: 1rpx;
}
</style>
