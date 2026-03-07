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
  min-height: 100vh;
  padding: 24rpx;
  padding-bottom: 214rpx;
}

.loading-card {
  min-height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 24rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.2);
  background: linear-gradient(160deg, #fffdfa 0%, #f8f1e6 100%);
  box-shadow: 0 8rpx 18rpx rgba(35, 56, 45, 0.08);
}

.gallery-card,
.info-card,
.section-card,
.qty-card,
.notice-card {
  border-radius: 24rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.2);
  background: linear-gradient(160deg, #fffdfa 0%, #f8f1e6 100%);
  box-shadow: 0 10rpx 22rpx rgba(35, 56, 45, 0.08);
}

.gallery-card {
  position: relative;
  padding: 0;
  overflow: hidden;
}

.hero-swiper {
  width: 100%;
  height: 640rpx;
}

.hero {
  width: 100%;
  height: 100%;
  background: #e8decd;
}

.gallery-indicator {
  position: absolute;
  right: 24rpx;
  bottom: 24rpx;
  border-radius: 999rpx;
  padding: 8rpx 24rpx;
  color: #fff;
  font-size: 22rpx;
  font-weight: 500;
  background: rgba(31, 47, 39, 0.56);
  backdrop-filter: blur(4px);
}

.info-card {
  margin-top: 24rpx;
  padding: 38rpx 30rpx;
}

.price-line {
  margin-bottom: 18rpx;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.price {
  color: #b38d58;
  font-weight: 700;
  font-size: 48rpx;
  line-height: 1;
  white-space: nowrap;
}

.stock-pill {
  padding: 7rpx 18rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(62, 111, 87, 0.26);
  background: #edf4ef;
  color: #3e6f57;
  font-size: 22rpx;
}

.stock-pill.warn {
  border-color: rgba(185, 152, 100, 0.28);
  background: #fff3e1;
  color: #a68450;
}

.title {
  margin-bottom: 10rpx;
  color: #1f2f27;
  font-size: 36rpx;
  font-weight: 700;
  line-height: 1.42;
}

.subtitle {
  margin-bottom: 18rpx;
  color: #74837a;
  font-size: 26rpx;
  line-height: 1.6;
}

.tag-list {
  margin-bottom: 22rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
}

.tag {
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(62, 111, 87, 0.24);
  background: #edf4ef;
  color: #3e6f57;
  font-size: 22rpx;
}

.service-row {
  padding-top: 22rpx;
  border-top: 1rpx solid rgba(185, 152, 100, 0.2);
  display: flex;
  gap: 16rpx;
}

.service-item {
  color: #5d7265;
  font-size: 22rpx;
  display: flex;
  align-items: center;
}

.service-item::before {
  content: "·";
  margin-right: 6rpx;
  color: #c39e67;
  font-weight: 700;
}

.section-card,
.qty-card,
.notice-card {
  margin-top: 24rpx;
  padding: 34rpx 30rpx;
}

.section-head {
  margin-bottom: 22rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title-inline,
.notice-title {
  position: relative;
  padding-left: 20rpx;
  color: #1f2f27;
  font-size: 30rpx;
  font-weight: 700;
}

.section-title-inline::before,
.notice-title::before {
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

.section-tip {
  color: #839188;
  font-size: 24rpx;
}

.spec-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14rpx;
}

.spec-chip {
  min-height: 164rpx;
  box-sizing: border-box;
  padding: 22rpx;
  border-radius: 18rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.2);
  background: rgba(255, 252, 246, 0.86);
  display: flex;
  flex-direction: column;
}

.spec-chip.active {
  border-color: rgba(62, 111, 87, 0.5);
  background: #edf4ef;
}

.spec-chip.disabled {
  opacity: 0.54;
}

.spec-name {
  margin-bottom: 8rpx;
  color: #2c4137;
  font-size: 28rpx;
  font-weight: 600;
  line-height: 1.3;
}

.spec-price {
  margin-top: auto;
  color: #b38d58;
  font-size: 28rpx;
  font-weight: 700;
}

.spec-stock {
  margin-top: 6rpx;
  color: #87938c;
  font-size: 22rpx;
}

.qty-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.qty-label {
  color: #1f2f27;
  font-size: 30rpx;
  font-weight: 700;
}

.qty-wrap {
  padding: 6rpx;
  border-radius: 12rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.22);
  background: rgba(255, 252, 246, 0.86);
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.qty-btn {
  width: 60rpx;
  height: 60rpx;
  border-radius: 10rpx;
  background: #fff;
  color: #4a6457;
  text-align: center;
  line-height: 60rpx;
  font-size: 32rpx;
  box-shadow: 0 2rpx 8rpx rgba(35, 56, 45, 0.08);
}

.qty-btn:active {
  background: #edf4ef;
}

.qty-btn.disabled {
  color: #c6d2cb;
  box-shadow: none;
}

.qty-value {
  min-width: 56rpx;
  text-align: center;
  color: #2f463b;
  font-size: 30rpx;
  font-weight: 700;
}

.notice-title {
  margin-bottom: 16rpx;
}

.notice-line {
  margin-bottom: 8rpx;
  color: #5d7265;
  font-size: 26rpx;
  line-height: 1.6;
}

.bottom-space {
  height: 160rpx;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 20;
  display: grid;
  grid-template-columns: 140rpx 1fr 1fr;
  gap: 16rpx;
  padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid rgba(185, 152, 100, 0.2);
  background: rgba(252, 247, 238, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 -8rpx 22rpx rgba(35, 56, 45, 0.08);
}

.tool-btn,
.btn-outline,
.btn-primary {
  min-height: 88rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 600;
}

.tool-btn {
  border: 1rpx solid rgba(185, 152, 100, 0.3);
  background: rgba(255, 252, 246, 0.9);
  color: #896f44;
}

.btn-outline {
  border: 1rpx solid rgba(62, 111, 87, 0.34);
  background: #edf4ef;
  color: #3e6f57;
}

.btn-primary {
  background: linear-gradient(135deg, #5a8c72 0%, #3e6f57 100%);
  color: #fff;
  box-shadow: 0 8rpx 16rpx rgba(35, 56, 45, 0.16);
}

.btn-primary.disabled {
  background: #c5d3cc;
  color: #eef3f0;
  box-shadow: none;
}

.tool-btn:active,
.btn-outline:active,
.btn-primary:active {
  transform: scale(0.98);
}

.btn-primary.disabled:active {
  transform: none;
}
</style>
