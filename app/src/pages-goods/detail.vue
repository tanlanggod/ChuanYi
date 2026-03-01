<template>
  <view class="container">
    <view class="card loading-card" v-if="loading">
      <view class="muted">商品信息加载中...</view>
    </view>

    <view class="card loading-card" v-else-if="!spu">
      <view class="muted">未找到商品信息</view>
    </view>

    <template v-else>
      <view class="card gallery-card">
        <swiper class="hero-swiper" indicator-dots circular :autoplay="galleryImages.length > 1" @change="onSwiperChange">
          <swiper-item v-for="(img, idx) in galleryImages" :key="idx">
            <image class="hero" :src="resolveImage(img)" mode="aspectFill"></image>
          </swiper-item>
        </swiper>
        <view class="gallery-indicator">{{ currentGalleryIndex + 1 }}/{{ galleryImages.length }}</view>
      </view>

      <view class="card info-card">
        <view class="price-line">
          <view class="price">￥{{ formatAmount(selectedSkuPrice || spu.minPrice) }}</view>
          <view class="stock-pill" :class="{ warn: selectedSku && !isSkuAvailable(selectedSku) }">
            {{ stockText }}
          </view>
        </view>
        <view class="title">{{ spu.title }}</view>
        <view class="subtitle">{{ spu.subtitle || "天然水晶手串，支持多规格下单" }}</view>
        <view class="tag-list" v-if="spu.tags && spu.tags.length > 0">
          <view class="tag" v-for="tag in spu.tags" :key="tag">{{ tag }}</view>
        </view>
        <view class="service-row">
          <view class="service-item">精选珠材</view>
          <view class="service-item">7天售后</view>
          <view class="service-item">满额包邮</view>
        </view>
      </view>

      <view class="card section-card">
        <view class="section-head">
          <view class="section-title-inline">规格选择</view>
          <view class="section-tip">请选择可售规格</view>
        </view>
        <view class="spec-grid" v-if="skus.length > 0">
          <view
            class="spec-chip"
            :class="{ active: selectedSkuId === item.skuId, disabled: !isSkuAvailable(item) }"
            v-for="item in skus"
            :key="item.skuId"
            @click="selectSku(item.skuId)"
          >
            <view class="spec-name">{{ item.specText || `规格 ${item.skuId}` }}</view>
            <view class="spec-price">￥{{ formatAmount(item.price) }}</view>
            <view class="spec-stock">{{ specStockText(item) }}</view>
          </view>
        </view>
        <view class="muted" v-else>暂无规格数据</view>
      </view>

      <view class="card qty-card" v-if="selectedSku">
        <view class="qty-label">购买数量</view>
        <view class="qty-wrap">
          <view class="qty-btn" :class="{ disabled: buyQty <= 1 }" @click="changeQty(-1)">-</view>
          <view class="qty-value">{{ buyQty }}</view>
          <view class="qty-btn" :class="{ disabled: buyQty >= selectedSkuMaxQty }" @click="changeQty(1)">+</view>
        </view>
      </view>

      <view class="card notice-card">
        <view class="notice-title">购买须知</view>
        <view class="notice-line">1. 下单前请确认手围与规格，避免尺寸不合适。</view>
        <view class="notice-line">2. 配件为手工制作，通常 4-7 天内完成发货。</view>
        <view class="notice-line">3. 现货/定制配件以页面库存与状态为准。</view>
      </view>
    </template>

    <view class="bottom-space"></view>
    <view class="footer" v-if="spu">
      <view class="tool-btn" @click="goCart">购物车</view>
      <view class="btn-outline" @click="addToCart">加入购物车</view>
      <view class="btn-primary" :class="{ disabled: submitting }" @click="buyNow">
        {{ submitting ? "提交中..." : "立即购买" }}
      </view>
    </view>
  </view>
</template>

<script>
import {
  addCartItem,
  createOrderDirectGoods,
  fetchAddressList,
  fetchGoodsDetail,
} from "../common/api";
import { resolveAssetUrl } from "../common/http";
import { loginGuestIfNeeded } from "../common/session";

export default {
  data() {
    return {
      spuId: null,
      spu: null,
      loading: true,
      skus: [],
      selectedSkuId: null,
      galleryImages: [],
      buyQty: 1,
      currentGalleryIndex: 0,
      submitting: false,
    };
  },
  computed: {
    selectedSku() {
      return this.skus.find((item) => Number(item.skuId) === Number(this.selectedSkuId)) || null;
    },
    selectedSkuPrice() {
      return this.selectedSku ? this.selectedSku.price : 0;
    },
    selectedSkuMaxQty() {
      return Math.max(1, Number((this.selectedSku && this.selectedSku.stockQty) || 1));
    },
    stockText() {
      if (!this.selectedSku) {
        return `可选规格 ${this.skus.length} 个`;
      }
      if (!this.isSkuAvailable(this.selectedSku)) {
        return "当前规格暂不可售";
      }
      return `库存 ${Number(this.selectedSku.stockQty || 0)}`;
    },
  },
  async onLoad(options) {
    this.spuId = options && options.spuId ? Number(options.spuId) : null;
    if (!this.spuId) {
      uni.showToast({ title: "商品参数错误", icon: "none" });
      return;
    }
    await this.loadDetail();
  },
  methods: {
    async loadDetail() {
      try {
        this.loading = true;
        await loginGuestIfNeeded();
        const data = await fetchGoodsDetail(this.spuId);
        this.spu = (data && data.spu) || null;
        this.skus = (data && data.skus) || [];
        const firstAvailableSku = this.skus.find((item) => this.isSkuAvailable(item));
        this.selectedSkuId = firstAvailableSku ? firstAvailableSku.skuId : this.skus.length > 0 ? this.skus[0].skuId : null;
        const imageUrls = (data && data.imageUrls) || [];
        const fallback = this.spu && this.spu.coverImageUrl ? [this.spu.coverImageUrl] : ["/static/logo.png"];
        this.galleryImages = Array.isArray(imageUrls) && imageUrls.length > 0 ? imageUrls : fallback;
        this.currentGalleryIndex = 0;
      } catch (error) {
        uni.showToast({ title: "详情加载失败", icon: "none" });
      } finally {
        this.loading = false;
      }
    },
    isSkuAvailable(sku) {
      return sku && sku.salesStatus === "ON_SALE" && Number(sku.stockQty || 0) > 0;
    },
    selectSku(skuId) {
      const target = this.skus.find((item) => Number(item.skuId) === Number(skuId));
      if (!this.isSkuAvailable(target)) {
        uni.showToast({ title: "该规格暂不可购买", icon: "none" });
        return;
      }
      this.selectedSkuId = skuId;
      const maxQty = Math.max(1, Number(target.stockQty || 1));
      if (this.buyQty > maxQty) {
        this.buyQty = maxQty;
      }
    },
    onSwiperChange(event) {
      const current = Number((event && event.detail && event.detail.current) || 0);
      this.currentGalleryIndex = Number.isNaN(current) ? 0 : current;
    },
    specStockText(item) {
      const stock = Number((item && item.stockQty) || 0);
      if ((item && item.salesStatus) !== "ON_SALE") {
        return "暂未上架";
      }
      if (stock <= 0) {
        return "库存不足";
      }
      return `库存 ${stock}`;
    },
    changeQty(delta) {
      const sku = this.selectedSku;
      if (!sku) {
        return;
      }
      const maxQty = Math.max(1, Number(sku.stockQty || 1));
      const nextQty = Number(this.buyQty || 1) + delta;
      if (nextQty < 1 || nextQty > maxQty) {
        return;
      }
      this.buyQty = nextQty;
    },
    async addToCart() {
      const sku = this.selectedSku;
      if (!sku) {
        uni.showToast({ title: "请先选择规格", icon: "none" });
        return;
      }
      if (!this.isSkuAvailable(sku)) {
        uni.showToast({ title: "该规格暂不可购买", icon: "none" });
        return;
      }
      try {
        await addCartItem({
          itemType: "NORMAL_GOODS",
          skuId: this.selectedSkuId,
          qty: this.buyQty,
        });
        uni.showToast({ title: "已加入购物车", icon: "none" });
      } catch (error) {
        uni.showToast({ title: "加入购物车失败", icon: "none" });
      }
    },
    async buyNow() {
      const sku = this.selectedSku;
      if (!sku) {
        uni.showToast({ title: "请先选择规格", icon: "none" });
        return;
      }
      if (!this.isSkuAvailable(sku)) {
        uni.showToast({ title: "该规格暂不可购买", icon: "none" });
        return;
      }
      if (this.submitting) {
        return;
      }
      this.submitting = true;
      try {
        const addressData = await fetchAddressList();
        const addresses = (addressData && addressData.addresses) || [];
        if (addresses.length === 0) {
          uni.showToast({ title: "请先添加收货地址", icon: "none" });
          setTimeout(() => {
            uni.navigateTo({ url: "/pages-user/address-edit?from=order-submit" });
          }, 300);
          return;
        }
        const defaultAddress = addresses.find((item) => Number(item.isDefault) === 1) || addresses[0];
        const result = await createOrderDirectGoods({
          skuId: this.selectedSkuId,
          qty: this.buyQty,
          addressId: defaultAddress.id,
          remark: "好物立即购买",
        });
        if (result && result.orderNo) {
          uni.navigateTo({ url: `/pages-order/detail?orderNo=${result.orderNo}` });
        }
      } catch (error) {
        uni.showToast({ title: "下单失败", icon: "none" });
      } finally {
        this.submitting = false;
      }
    },
    goCart() {
      uni.switchTab({ url: "/pages/cart/index" });
    },
    resolveImage(url) {
      if (!url) {
        return "/static/logo.png";
      }
      return resolveAssetUrl(url);
    },
    formatAmount(value) {
      return Number(value || 0).toFixed(2);
    },
  },
};
</script>

<style scoped>
.container {
  padding: 24rpx;
  padding-bottom: 210rpx;
}

.loading-card {
  display: grid;
  place-items: center;
  min-height: 220rpx;
}

.gallery-card {
  padding: 0;
  overflow: hidden;
  position: relative;
}

.hero-swiper {
  width: 100%;
  height: 560rpx;
}

.hero {
  width: 100%;
  height: 100%;
  background: #f3f4f6;
}

.gallery-indicator {
  position: absolute;
  right: 16rpx;
  bottom: 16rpx;
  background: rgba(17, 24, 39, 0.48);
  color: #fff;
  border-radius: 999rpx;
  padding: 8rpx 16rpx;
  font-size: 20rpx;
}

.info-card {
  margin-top: 12rpx;
}

.price-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  color: #d62b2b;
  font-weight: 700;
  font-size: 42rpx;
  white-space: nowrap;
}

.stock-pill {
  border-radius: 999rpx;
  padding: 8rpx 16rpx;
  font-size: 22rpx;
  color: #047857;
  background: #ecfdf5;
}

.stock-pill.warn {
  color: #b45309;
  background: #fffbeb;
}

.title {
  margin-top: 10rpx;
  font-size: 34rpx;
  font-weight: 700;
  color: #111827;
}

.subtitle {
  margin-top: 8rpx;
  color: #6b7280;
  font-size: 24rpx;
  line-height: 1.6;
}

.tag-list {
  margin-top: 10rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
}

.tag {
  font-size: 20rpx;
  color: #b42318;
  background: #fbe5e5;
  padding: 4rpx 10rpx;
  border-radius: 999rpx;
}

.service-row {
  margin-top: 16rpx;
  display: flex;
  gap: 12rpx;
}

.service-item {
  font-size: 22rpx;
  color: #4b5563;
  background: #f5f6f8;
  border-radius: 999rpx;
  padding: 6rpx 14rpx;
}

.section-card {
  margin-top: 12rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.section-title-inline {
  font-size: 30rpx;
  font-weight: 700;
}

.section-tip {
  font-size: 22rpx;
  color: #9ca3af;
}

.spec-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10rpx;
}

.spec-chip {
  background: #fff;
  border-radius: 14rpx;
  padding: 16rpx;
  border: 2rpx solid #eef1f5;
  min-height: 146rpx;
  box-sizing: border-box;
}

.spec-chip.active {
  border-color: #d62b2b;
  background: #fff6f6;
}

.spec-chip.disabled {
  opacity: 0.5;
}

.spec-name {
  font-size: 26rpx;
  font-weight: 600;
  color: #1f2937;
}

.spec-price {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #d62b2b;
}

.spec-stock {
  margin-top: 4rpx;
  font-size: 22rpx;
  color: #8a8f99;
}

.qty-card {
  margin-top: 10rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.qty-label {
  font-size: 28rpx;
  font-weight: 600;
}

.qty-wrap {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.qty-btn {
  width: 54rpx;
  height: 54rpx;
  border-radius: 12rpx;
  background: #f3f4f6;
  text-align: center;
  line-height: 54rpx;
  font-size: 30rpx;
  color: #374151;
}

.qty-btn.disabled {
  color: #c4c9d2;
}

.qty-value {
  min-width: 48rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 600;
}

.notice-card {
  margin-top: 12rpx;
}

.notice-title {
  font-size: 28rpx;
  font-weight: 700;
  margin-bottom: 8rpx;
}

.notice-line {
  color: #6b7280;
  font-size: 23rpx;
  line-height: 1.7;
}

.bottom-space {
  height: 128rpx;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 14rpx 18rpx;
  padding-bottom: calc(14rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #e9edf2;
  display: grid;
  grid-template-columns: 124rpx 1fr 1fr;
  gap: 10rpx;
  min-height: 98rpx;
  box-sizing: border-box;
  box-shadow: 0 -8rpx 26rpx rgba(15, 23, 42, 0.06);
  z-index: 20;
}

.tool-btn,
.btn-outline,
.btn-primary {
  min-height: 70rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: 700;
  box-sizing: border-box;
}

.tool-btn {
  background: #f7f8fa;
  color: #374151;
  border: 1px solid #edf0f4;
}

.btn-outline {
  border: 1px solid #d62b2b;
  color: #d62b2b;
  background: #fff;
}

.btn-primary {
  background: #d62b2b;
  color: #fff;
}

.btn-primary.disabled {
  background: #c5ccd6;
}

.btn-outline:active,
.btn-primary:active,
.tool-btn:active {
  transform: scale(0.99);
}

.btn-primary.disabled:active {
  transform: none;
}
</style>
