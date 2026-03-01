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
      const radius = 148;
      const center = 180;
      const dots = [];
      for (let i = 0; i < count; i += 1) {
        const angle = (Math.PI * 2 * i) / count - Math.PI / 2;
        const x = center + Math.cos(angle) * radius - 22;
        const y = center + Math.sin(angle) * radius - 22;
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
  padding: 24rpx;
  padding-bottom: 170rpx;
}

.preview-card {
  padding: 24rpx;
}

.preview-image {
  width: 100%;
  height: 520rpx;
  border-radius: 14rpx;
  background: #f3f4f6;
}

.ring-wrap {
  width: 360rpx;
  height: 360rpx;
  margin: 10rpx auto;
  position: relative;
}

.ring {
  position: absolute;
  left: 30rpx;
  top: 30rpx;
  width: 300rpx;
  height: 300rpx;
  border-radius: 50%;
  border: 2rpx dashed #e5e7eb;
}

.bead {
  position: absolute;
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  box-shadow: 0 4rpx 8rpx rgba(0, 0, 0, 0.12);
}

.center-text {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  text-align: center;
}

.brand {
  font-size: 24rpx;
  color: #b25b7a;
  font-weight: 700;
}

.info-card {
  margin-top: 16rpx;
}

.shop {
  color: #8a8f99;
  font-size: 24rpx;
}

.title-line {
  margin-top: 8rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12rpx;
}

.title {
  flex: 1;
  min-width: 0;
  font-size: 40rpx;
  font-weight: 700;
}

.price {
  color: #111827;
  font-size: 38rpx;
  font-weight: 700;
}

.chip-row {
  margin-top: 14rpx;
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.chip {
  background: #fef2f2;
  border: 1rpx solid #fecaca;
  border-radius: 999rpx;
  color: #b42318;
  font-size: 24rpx;
  padding: 8rpx 16rpx;
}

.chip-action {
  border-radius: 999rpx;
  border: 1rpx solid #fecaca;
  color: #b42318;
  font-size: 22rpx;
  padding: 8rpx 16rpx;
}

.notice-title {
  margin-top: 20rpx;
  color: #d62b2b;
  font-size: 28rpx;
  font-weight: 700;
}

.notice-line {
  margin-top: 8rpx;
  color: #4b5563;
  font-size: 24rpx;
  line-height: 1.6;
}

.bottom-space {
  height: 20rpx;
}

.footer {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: 30rpx;
  display: grid;
  grid-template-columns: 1fr 1.2fr 1.5fr;
  gap: 12rpx;
}

.btn-ghost,
.btn-outline,
.buy-btn {
  border-radius: 999rpx;
  padding: 18rpx 10rpx;
  text-align: center;
  font-size: 24rpx;
}

.btn-ghost {
  border: 1px solid #d62b2b;
  color: #d62b2b;
  background: #fff;
}

.btn-outline {
  border: 1px solid #d62b2b;
  color: #d62b2b;
  background: #fff7f7;
}

.buy-btn {
  font-weight: 700;
}
</style>
