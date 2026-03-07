<template>
  <view class="container">
    <view class="card preview-card">
      <image
        v-if="snapshot.previewImageUrl"
        class="preview-image"
        :src="resolveImage(snapshot.previewImageUrl)"
        mode="aspectFill"
      ></image>
      <view v-else class="ring-wrap">
        <view class="ring"></view>
        <view class="bead" v-for="(dot, idx) in previewDots" :key="idx" :style="dot.style"></view>
        <view class="center-text">
          <view class="brand">养个石头</view>
          <view class="muted">MineStone</view>
        </view>
      </view>
    </view>

    <view class="card info-card">
      <view class="shop">刺猬大王</view>
      <view class="title-line">
        <view class="title">{{ snapshot.title || "独一无二的定制" }}</view>
        <view class="price">￥{{ formatAmount(snapshot.priceSnapshot) }}</view>
      </view>
      <view class="chip-row">
        <view class="chip">手围 {{ formatWristSize(snapshot.wristSizeCm) }}cm</view>
        <view class="chip-action" @click="checkWrist">检查手围</view>
      </view>

      <view class="notice-title">购前须知</view>
      <view class="notice-line">1. 定制商品支持签收后 48 小时鉴赏期，具体规则以门店说明为准。</view>
      <view class="notice-line">2. 下单前请确认手围尺寸，不确定可联系在线客服协助确认。</view>
      <view class="notice-line">3. 常规制作周期约 4-7 天，节假日可能顺延。</view>
    </view>

    <view class="bottom-space"></view>
    <view class="footer">
      <view class="btn-ghost" @click="contactService">客服</view>
      <view class="btn-outline" @click="openPublishDialog">发布广场</view>
      <view class="btn-primary buy-btn" @click="goSubmitOrder">立即购买</view>
    </view>

    <gallery-publish-dialog
      :visible="publishVisible"
      :submitting="publishLoading"
      :snapshot-no="snapshotNo"
      :default-title="snapshot.title || '我的设计'"
      :default-tags="defaultTags"
      :default-cover-image-url="snapshot.previewImageUrl || ''"
      @cancel="publishVisible = false"
      @submit="submitPublish"
    />
  </view>
</template>

<script>
import { fetchSnapshotDetail, publishGallery } from "../common/api";
import { resolveAssetUrl } from "../common/http";
import { loginGuestIfNeeded } from "../common/session";
import GalleryPublishDialog from "../components/GalleryPublishDialog.vue";

const PREVIEW_COLORS = [
  "#b47dfd",
  "#8a5cff",
  "#5a7bff",
  "#5cc4ff",
  "#a4e5ff",
  "#e7f0ff",
  "#d8f7d2",
  "#f7efb1",
  "#ffc483",
  "#ff9a9a",
  "#d27cc8",
  "#8a66f0",
];

export default {
  components: {
    GalleryPublishDialog,
  },
  data() {
    return {
      snapshotNo: "",
      snapshot: {},
      selectedSkus: [],
      publishVisible: false,
      publishLoading: false,
    };
  },
  computed: {
    previewDots() {
      const source =
        this.selectedSkus.length > 0
          ? this.selectedSkus
          : PREVIEW_COLORS.map((color) => ({
              previewColor: color,
            }));
      const count = Math.min(source.length, 20);
      const radius = 180;
      const center = 220;
      const dots = [];
      for (let i = 0; i < count; i += 1) {
        const angle = (Math.PI * 2 * i) / count - Math.PI / 2;
        const x = center + Math.cos(angle) * radius - 25;
        const y = center + Math.sin(angle) * radius - 25;
        const bead = source[i];
        const style = {
          left: `${x}rpx`,
          top: `${y}rpx`,
          background: bead.previewColor || PREVIEW_COLORS[i % PREVIEW_COLORS.length],
        };
        if (bead && bead.imageUrl) {
          style.backgroundImage = `url(${this.resolveImage(bead.imageUrl)})`;
          style.backgroundSize = "cover";
          style.backgroundPosition = "center";
        }
        dots.push({ style });
      }
      return dots;
    },
    defaultTags() {
      const map = {};
      const list = [];
      this.selectedSkus.forEach((item) => {
        const raw = String(item.name || "").trim();
        if (!raw) {
          return;
        }
        const tag = raw.length > 10 ? raw.slice(0, 10) : raw;
        if (map[tag]) {
          return;
        }
        map[tag] = true;
        list.push(tag);
      });
      return list.slice(0, 5);
    },
  },
  async onLoad(options) {
    this.snapshotNo = (options && options.snapshotNo) || "";
    await this.loadSnapshot();
  },
  methods: {
    async loadSnapshot() {
      if (!this.snapshotNo) {
        uni.showToast({ title: "快照参数无效", icon: "none" });
        return;
      }
      try {
        await loginGuestIfNeeded();
        const data = await fetchSnapshotDetail(this.snapshotNo);
        this.snapshot = data || {};
        this.selectedSkus = this.parseSelectedSkus(data && data.snapshotJson);
      } catch (_error) {
        uni.showToast({ title: "加载失败", icon: "none" });
      }
    },
    parseSelectedSkus(snapshotJson) {
      if (!snapshotJson) {
        return [];
      }
      try {
        const parsed = typeof snapshotJson === "string" ? JSON.parse(snapshotJson) : snapshotJson;
        const list = parsed && parsed.selectedSkus;
        if (!Array.isArray(list)) {
          return [];
        }
        return list.map((item) => ({
          skuId: item.skuId,
          imageUrl: item.imageUrl || "",
          previewColor: item.previewColor || "",
          name: item.name || "",
        }));
      } catch (_error) {
        return [];
      }
    },
    goSubmitOrder() {
      if (!this.snapshotNo) {
        return;
      }
      uni.navigateTo({ url: `/pages-order/submit?snapshotNo=${this.snapshotNo}` });
    },
    checkWrist() {
      uni.showToast({ title: "可回到设计页进一步调整手围", icon: "none" });
    },
    contactService() {
      uni.showToast({ title: "请前往帮助中心联系客服", icon: "none" });
    },
    openPublishDialog() {
      if (!this.snapshotNo) {
        uni.showToast({ title: "快照未生成，暂不可发布", icon: "none" });
        return;
      }
      this.publishVisible = true;
    },
    async submitPublish(payload) {
      if (this.publishLoading) {
        return;
      }
      this.publishLoading = true;
      try {
        const data = await publishGallery(payload || {});
        this.publishVisible = false;
        const galleryId = data && data.id ? Number(data.id) : 0;
        uni.showModal({
          title: "发布成功",
          content: "作品已发布到设计广场，是否立即查看？",
          confirmText: "去查看",
          success: (res) => {
            if (res.confirm && galleryId > 0) {
              uni.navigateTo({ url: `/pages-gallery/detail?id=${galleryId}` });
            }
          },
        });
      } catch (error) {
        uni.showToast({ title: error.message || "发布失败", icon: "none" });
      } finally {
        this.publishLoading = false;
      }
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
    formatWristSize(value) {
      return Number(value || 13).toFixed(0);
    },
  },
};
</script>

<style scoped>
.container {
  min-height: 100vh;
  padding: 24rpx;
  padding-bottom: 228rpx;
}

.preview-card,
.info-card {
  border-radius: 24rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.2);
  background: linear-gradient(160deg, #fffdfa 0%, #f8f1e6 100%);
  box-shadow: 0 10rpx 22rpx rgba(35, 56, 45, 0.08);
}

.preview-card {
  padding: 26rpx;
}

.preview-image {
  width: 100%;
  height: 600rpx;
  border-radius: 18rpx;
  background: #e8decd;
  box-shadow: 0 8rpx 16rpx rgba(35, 56, 45, 0.12);
}

.ring-wrap {
  width: 460rpx;
  height: 460rpx;
  margin: 32rpx auto;
  position: relative;
}

.ring {
  position: absolute;
  left: 40rpx;
  top: 40rpx;
  width: 380rpx;
  height: 380rpx;
  border-radius: 50%;
  border: 2rpx dashed rgba(185, 152, 100, 0.64);
  background: radial-gradient(circle at center, #ffffff 0%, #faf7f1 72%, #f0ebdf 100%);
  opacity: 0.65;
}

.bead {
  position: absolute;
  width: 50rpx;
  height: 50rpx;
  border-radius: 50%;
  border: 2rpx solid rgba(255, 255, 255, 0.82);
  box-shadow: 0 6rpx 12rpx rgba(35, 56, 45, 0.2);
}

.center-text {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  pointer-events: none;
}

.brand {
  color: #3e6f57;
  font-size: 30rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
}

.muted {
  margin-top: 6rpx;
  color: #a68450;
  font-size: 21rpx;
  letter-spacing: 1rpx;
}

.info-card {
  margin-top: 24rpx;
  padding: 38rpx 30rpx;
}

.shop {
  margin-bottom: 12rpx;
  color: #7d8d82;
  font-size: 26rpx;
}

.title-line {
  margin-bottom: 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18rpx;
}

.title {
  flex: 1;
  min-width: 0;
  color: #1f2f27;
  font-size: 40rpx;
  font-weight: 800;
  line-height: 1.3;
}

.price {
  color: #b38d58;
  font-size: 44rpx;
  font-weight: 700;
}

.chip-row {
  margin-bottom: 30rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.chip,
.chip-action {
  padding: 9rpx 20rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
}

.chip {
  border: 1rpx solid rgba(62, 111, 87, 0.26);
  background: #edf4ef;
  color: #3e6f57;
}

.chip-action {
  border: 1rpx solid rgba(185, 152, 100, 0.32);
  background: rgba(255, 252, 246, 0.9);
  color: #8a7247;
}

.chip-action:active {
  background: #f3e9d8;
}

.notice-title {
  margin: 34rpx 0 14rpx;
  padding-left: 20rpx;
  position: relative;
  color: #1f2f27;
  font-size: 30rpx;
  font-weight: 700;
}

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

.notice-line {
  margin-top: 10rpx;
  color: #5d7265;
  font-size: 26rpx;
  line-height: 1.66;
}

.bottom-space {
  height: 40rpx;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 22rpx 24rpx calc(22rpx + env(safe-area-inset-bottom));
  background: rgba(252, 247, 238, 0.95);
  backdrop-filter: blur(10px);
  border-top: 1rpx solid rgba(185, 152, 100, 0.22);
  box-shadow: 0 -8rpx 24rpx rgba(35, 56, 45, 0.08);
}

.btn-ghost,
.btn-outline,
.buy-btn {
  border-radius: 999rpx;
  padding: 20rpx 0;
  text-align: center;
  font-size: 28rpx;
  font-weight: 600;
}

.btn-ghost,
.btn-outline {
  flex: 1;
}

.buy-btn {
  flex: 2;
  color: #fff;
  background: linear-gradient(135deg, #5a8c72 0%, #3e6f57 100%);
  box-shadow: 0 8rpx 16rpx rgba(35, 56, 45, 0.16);
}

.btn-ghost {
  border: 1rpx solid rgba(185, 152, 100, 0.32);
  background: rgba(255, 252, 246, 0.9);
  color: #8d7347;
}

.btn-outline {
  border: 1rpx solid rgba(62, 111, 87, 0.36);
  background: #edf4ef;
  color: #3e6f57;
}

.btn-ghost:active,
.btn-outline:active,
.buy-btn:active {
  transform: scale(0.98);
}
</style>
