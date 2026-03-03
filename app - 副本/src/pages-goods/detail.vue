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
  background: #F9F7F3;
  min-height: 100vh;
}

.loading-card {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300rpx;
  background: #fff;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(47, 54, 50, 0.04);
}

.gallery-card {
  padding: 0;
  overflow: hidden;
  position: relative;
  border-radius: 20rpx;
  box-shadow: 0 8rpx 20rpx rgba(47, 54, 50, 0.04);
  border: 1rpx solid rgba(232, 228, 219, 0.6);
  background: #fff;
}

.hero-swiper {
  width: 100%;
  height: 640rpx;
}

.hero {
  width: 100%;
  height: 100%;
  background: #E8E4DB;
}

.gallery-indicator {
  position: absolute;
  right: 24rpx;
  bottom: 24rpx;
  background: rgba(47, 54, 50, 0.6);
  backdrop-filter: blur(4px);
  color: #fff;
  border-radius: 999rpx;
  padding: 8rpx 24rpx;
  font-size: 22rpx;
  font-weight: 500;
}

.info-card {
  margin-top: 24rpx;
  padding: 40rpx 30rpx;
  border-radius: 20rpx;
  background: #fff;
  box-shadow: 0 8rpx 20rpx rgba(47, 54, 50, 0.04);
  border: 1rpx solid rgba(232, 228, 219, 0.6);
}

.price-line {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 20rpx;
}

.price {
  color: #C2A578;
  font-weight: 700;
  font-size: 48rpx;
  white-space: nowrap;
  font-family: -apple-system, BlinkMacSystemFont, "Helvetica Neue", Helvetica, Arial, sans-serif;
  line-height: 1;
}

.stock-pill {
  border-radius: 8rpx;
  padding: 6rpx 16rpx;
  font-size: 22rpx;
  color: #3B6E53;
  background: #F0F5F2;
  border: 1rpx solid #D1E0D7;
}

.stock-pill.warn {
  color: #A68754;
  background: #FDFBF7;
  border-color: #E8E4DB;
}

.title {
  font-size: 36rpx;
  font-weight: 700;
  color: #1A241D;
  line-height: 1.4;
  margin-bottom: 12rpx;
}

.subtitle {
  color: #7A8B7C;
  font-size: 26rpx;
  line-height: 1.6;
  margin-bottom: 20rpx;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 24rpx;
}

.tag {
  font-size: 22rpx;
  color: #3B6E53;
  background: #F0F5F2;
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
}

.service-row {
  display: flex;
  gap: 16rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid rgba(232, 228, 219, 0.4);
}

.service-item {
  font-size: 22rpx;
  color: #556B5D;
  display: flex;
  align-items: center;
}

.service-item::before {
  content: "·";
  margin-right: 6rpx;
  color: #C2A578;
  font-weight: 700;
}

.section-card {
  margin-top: 24rpx;
  padding: 40rpx 30rpx;
  border-radius: 20rpx;
  background: #fff;
  box-shadow: 0 8rpx 20rpx rgba(47, 54, 50, 0.04);
  border: 1rpx solid rgba(232, 228, 219, 0.6);
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.section-title-inline {
  font-size: 30rpx;
  font-weight: 600;
  color: #1A241D;
  position: relative;
  padding-left: 20rpx;
}

.section-title-inline::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 6rpx;
  height: 24rpx;
  background: #C2A578;
  border-radius: 4rpx;
}

.section-tip {
  font-size: 24rpx;
  color: #8C968F;
}

.spec-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
}

.spec-chip {
  background: #F9F7F3;
  border-radius: 16rpx;
  padding: 24rpx;
  border: 2rpx solid transparent;
  min-height: 160rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  transition: all 0.2s;
}

.spec-chip.active {
  border-color: #3B6E53;
  background: #F0F5F2;
}

.spec-chip.disabled {
  opacity: 0.5;
  background: #F9F7F3;
  border-color: #E8E4DB;
}

.spec-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #2F3632;
  margin-bottom: 8rpx;
  line-height: 1.3;
}

.spec-price {
  margin-top: auto;
  font-size: 28rpx;
  color: #C2A578;
  font-weight: 600;
  font-family: -apple-system, BlinkMacSystemFont, "Helvetica Neue", Helvetica, Arial, sans-serif;
}

.spec-stock {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #8C968F;
}

.qty-card {
  margin-top: 24rpx;
  padding: 30rpx;
  border-radius: 20rpx;
  background: #fff;
  box-shadow: 0 8rpx 20rpx rgba(47, 54, 50, 0.04);
  border: 1rpx solid rgba(232, 228, 219, 0.6);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.qty-label {
  font-size: 30rpx;
  font-weight: 600;
  color: #1A241D;
}

.qty-wrap {
  display: flex;
  align-items: center;
  gap: 16rpx;
  background: #F9F7F3;
  border-radius: 12rpx;
  padding: 6rpx;
  border: 1rpx solid rgba(232, 228, 219, 0.6);
}

.qty-btn {
  width: 60rpx;
  height: 60rpx;
  border-radius: 8rpx;
  background: #fff;
  text-align: center;
  line-height: 60rpx;
  font-size: 32rpx;
  color: #556B5D;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.02);
  transition: all 0.2s;
}

.qty-btn:active {
  background: #F0F5F2;
}

.qty-btn.disabled {
  color: #D1E0D7;
  box-shadow: none;
}

.qty-value {
  min-width: 60rpx;
  text-align: center;
  font-size: 30rpx;
  font-weight: 600;
  color: #2F3632;
}

.notice-card {
  margin-top: 24rpx;
  padding: 30rpx;
  border-radius: 20rpx;
  background: #fff;
  box-shadow: 0 8rpx 20rpx rgba(47, 54, 50, 0.04);
  border: 1rpx solid rgba(232, 228, 219, 0.6);
}

.notice-title {
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 16rpx;
  color: #1A241D;
  position: relative;
  padding-left: 20rpx;
}

.notice-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 6rpx;
  height: 24rpx;
  background: #C2A578;
  border-radius: 4rpx;
}

.notice-line {
  color: #556B5D;
  font-size: 26rpx;
  line-height: 1.6;
  margin-bottom: 8rpx;
}

.bottom-space {
  height: 160rpx;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-top: 1rpx solid rgba(232, 228, 219, 0.6);
  display: grid;
  grid-template-columns: 140rpx 1fr 1fr;
  gap: 20rpx;
  box-shadow: 0 -10rpx 30rpx rgba(47, 54, 50, 0.04);
  z-index: 20;
}

.tool-btn,
.btn-outline,
.btn-primary {
  min-height: 88rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: 600;
  transition: all 0.2s ease;
}

.tool-btn {
  background: #F9F7F3;
  color: #556B5D;
  border: 1px solid rgba(232, 228, 219, 0.8);
}

.btn-outline {
  border: 2rpx solid #3B6E53;
  color: #3B6E53;
  background: #fff;
}

.btn-primary {
  background: linear-gradient(135deg, #4A8365 0%, #3B6E53 100%);
  color: #fff;
  box-shadow: 0 8rpx 20rpx rgba(59, 110, 83, 0.2);
}

.btn-primary.disabled {
  background: #D1E0D7;
  color: #F0F5F2;
  box-shadow: none;
}

.btn-outline:active,
.btn-primary:active,
.tool-btn:active {
  transform: scale(0.98);
}

.btn-primary.disabled:active {
  transform: none;
}
</style>
