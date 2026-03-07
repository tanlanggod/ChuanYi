<template>
  <view class="container">
    <view class="grid">
      <view class="card add-card" @click="goEditor()">
        <view class="plus">+</view>
        <view class="muted">添加新设计</view>
      </view>

      <view class="card draft-card" v-for="item in list" :key="item.id">
        <image
          v-if="item.previewImageUrl"
          class="preview-image"
          :src="resolveImage(item.previewImageUrl)"
          mode="aspectFill"
        ></image>
        <view v-else class="preview-ring-wrap">
          <view class="preview-ring-track"></view>
          <view
            class="preview-bead"
            v-for="(dot, index) in item.previewDots || []"
            :key="`${item.id}-dot-${index}`"
            :style="dot.style"
          ></view>
          <view class="preview-empty" v-if="!item.previewDots || item.previewDots.length === 0">未配置</view>
        </view>
        <view class="draft-title">{{ item.title || "未命名草稿" }}</view>
        <view class="muted">上次更新：{{ formatDate(item.updatedAt || item.createdAt) }}</view>
        <view class="muted">预览价：￥{{ formatAmount(item.pricePreview) }}</view>
        <view class="actions">
          <view class="btn-mini" @click="goEditor(item.id)">继续设计</view>
          <view class="btn-mini" @click="createSnapshotAndPreview(item.id)">完成设计</view>
          <view class="btn-mini accent" @click="publishFromDraft(item)">发布广场</view>
        </view>
        <view class="remove" @click="remove(item.id)">×</view>
      </view>
    </view>

    <view v-if="list.length === 0" class="card empty-card">
      <view class="muted">还没有设计草稿，点击左上角新建设计。</view>
    </view>

    <gallery-publish-dialog
      :visible="publishVisible"
      :submitting="publishLoading"
      :snapshot-no="publishSeed.snapshotNo"
      :default-title="publishSeed.title"
      :default-tags="publishSeed.tags"
      :default-cover-image-url="publishSeed.coverImageUrl"
      @cancel="publishVisible = false"
      @submit="submitPublish"
    />
  </view>
</template>

<script>
import {
  createSnapshotFromDraft,
  deleteDraft,
  fetchDraftList,
  publishGallery,
} from "../common/api";
import { resolveAssetUrl } from "../common/http";
import { loginGuestIfNeeded } from "../common/session";
import GalleryPublishDialog from "../components/GalleryPublishDialog.vue";

const CARD_PREVIEW_COLORS = [
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
      list: [],
      publishVisible: false,
      publishLoading: false,
      publishSeed: {
        snapshotNo: "",
        title: "",
        coverImageUrl: "",
        tags: [],
      },
      convertingDraftId: 0,
    };
  },
  async onShow() {
    await this.loadDrafts();
  },
  async onPullDownRefresh() {
    await this.loadDrafts();
    uni.stopPullDownRefresh();
  },
  methods: {
    async loadDrafts() {
      try {
        await loginGuestIfNeeded();
        const data = await fetchDraftList(1, 50);
        const rows = (data && data.list) || [];
        this.list = rows.map((item) => {
          const previewDots = this.buildDraftPreviewDots(item);
          return Object.assign({}, item, { previewDots });
        });
      } catch (_error) {
        uni.showToast({ title: "草稿加载失败", icon: "none" });
      }
    },
    goEditor(id) {
      const suffix = id ? `?draftId=${id}` : "";
      uni.navigateTo({ url: `/pages-diy/editor${suffix}` });
    },
    async createSnapshotAndPreview(draftId) {
      try {
        const data = await createSnapshotFromDraft(draftId);
        if (data && data.snapshotNo) {
          uni.navigateTo({ url: `/pages-diy/snapshot-detail?snapshotNo=${data.snapshotNo}` });
        }
      } catch (_error) {
        uni.showToast({ title: "生成快照失败", icon: "none" });
      }
    },
    async publishFromDraft(item) {
      if (!item || !item.id) {
        return;
      }
      if (this.convertingDraftId) {
        return;
      }
      this.convertingDraftId = Number(item.id);
      uni.showLoading({ title: "正在准备发布..." });
      try {
        const data = await createSnapshotFromDraft(item.id);
        const snapshotNo = String((data && data.snapshotNo) || "");
        if (!snapshotNo) {
          uni.showToast({ title: "生成快照失败", icon: "none" });
          return;
        }
        this.publishSeed = {
          snapshotNo,
          title: String(item.title || "我的设计"),
          coverImageUrl: String(item.previewImageUrl || ""),
          tags: this.buildTagsFromDraft(item),
        };
        this.publishVisible = true;
      } catch (_error) {
        uni.showToast({ title: "发布准备失败", icon: "none" });
      } finally {
        uni.hideLoading();
        this.convertingDraftId = 0;
      }
    },
    buildTagsFromDraft(item) {
      const map = {};
      const result = [];
      const list = this.parseDraftSkus(item && item.configJson);
      list.forEach((row) => {
        const raw = String(row.name || "").trim();
        if (!raw) {
          return;
        }
        const tag = raw.length > 10 ? raw.slice(0, 10) : raw;
        if (map[tag]) {
          return;
        }
        map[tag] = true;
        result.push(tag);
      });
      return result.slice(0, 5);
    },
    parseDraftSkus(configJson) {
      if (!configJson) {
        return [];
      }
      try {
        const parsed = typeof configJson === "string" ? JSON.parse(configJson) : configJson;
        const list = parsed && parsed.selectedSkus;
        return Array.isArray(list) ? list : [];
      } catch (_error) {
        return [];
      }
    },
    buildDraftPreviewDots(item) {
      const beads = this.parseDraftSkus(item && item.configJson).slice(0, 16);
      if (beads.length === 0) {
        return [];
      }
      const center = 70;
      const radius = 60;
      const beadSize = 20;
      const count = beads.length;
      const dots = [];
      for (let i = 0; i < count; i += 1) {
        const angle = (Math.PI * 2 * i) / count - Math.PI / 2;
        const left = center + Math.cos(angle) * radius - beadSize / 2;
        const top = center + Math.sin(angle) * radius - beadSize / 2;
        const bead = beads[i] || {};
        const style = {
          left: `${left}rpx`,
          top: `${top}rpx`,
          background: this.buildPreviewColor(bead, i),
        };
        if (bead.imageUrl) {
          style.backgroundImage = `url(${this.resolveImage(bead.imageUrl)})`;
          style.backgroundSize = "cover";
          style.backgroundPosition = "center";
        }
        dots.push({ style });
      }
      return dots;
    },
    buildPreviewColor(bead, index) {
      const raw = String((bead && bead.previewColor) || "").trim();
      if (raw) {
        return raw;
      }
      return CARD_PREVIEW_COLORS[index % CARD_PREVIEW_COLORS.length];
    },
    async submitPublish(payload) {
      if (this.publishLoading) {
        return;
      }
      this.publishLoading = true;
      try {
        const data = await publishGallery(payload || {});
        const galleryId = Number((data && data.id) || 0);
        this.publishVisible = false;
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
    remove(id) {
      uni.showModal({
        title: "删除草稿",
        content: "确认删除该草稿吗？",
        success: async (res) => {
          if (!res.confirm) {
            return;
          }
          try {
            await deleteDraft(id);
            uni.showToast({ title: "已删除", icon: "none" });
            await this.loadDrafts();
          } catch (_error) {
            uni.showToast({ title: "删除失败", icon: "none" });
          }
        },
      });
    },
    formatAmount(value) {
      return Number(value || 0).toFixed(2);
    },
    formatDate(value) {
      if (!value) {
        return "-";
      }
      return String(value).replace("T", " ").slice(0, 16);
    },
    resolveImage(url) {
      if (!url) {
        return "/static/logo.png";
      }
      return resolveAssetUrl(url);
    },
  },
};
</script>

<style scoped>
.container {
  min-height: 100vh;
  padding: 24rpx;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20rpx;
}

.card {
  border-radius: 24rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.2);
  background: linear-gradient(160deg, #fffdfa 0%, #f8f1e6 100%);
  box-shadow: 0 10rpx 22rpx rgba(35, 56, 45, 0.08);
  padding: 26rpx 22rpx;
  display: flex;
  flex-direction: column;
}

.add-card {
  position: relative;
  min-height: 420rpx;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  border: 2rpx dashed rgba(62, 111, 87, 0.28);
  background: linear-gradient(150deg, #f8fcf9 0%, #edf5f0 100%);
}

.add-card::after {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: radial-gradient(circle at 86% 12%, rgba(255, 255, 255, 0.45), rgba(255, 255, 255, 0));
  pointer-events: none;
}

.add-card:active {
  transform: scale(0.985);
}

.plus {
  position: relative;
  z-index: 1;
  font-size: 88rpx;
  line-height: 1;
  font-weight: 300;
  color: #3e6f57;
}

.draft-card {
  min-height: 420rpx;
  position: relative;
  overflow: hidden;
}

.draft-card::before {
  content: "";
  position: absolute;
  right: -44rpx;
  top: -44rpx;
  width: 156rpx;
  height: 156rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.62) 0%, rgba(255, 255, 255, 0) 72%);
}

.preview-image {
  margin: 8rpx auto 18rpx;
  width: 152rpx;
  height: 152rpx;
  border-radius: 50%;
  background: #e8decd;
  border: 4rpx solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 8rpx 16rpx rgba(35, 56, 45, 0.14);
}

.preview-ring-wrap {
  margin: 8rpx auto 18rpx;
  width: 152rpx;
  height: 152rpx;
  position: relative;
}

.preview-ring-track {
  position: absolute;
  left: 10rpx;
  top: 10rpx;
  width: 132rpx;
  height: 132rpx;
  border-radius: 50%;
  border: 2rpx dashed rgba(185, 152, 100, 0.62);
  background: radial-gradient(circle at center, #ffffff 0%, #faf7f1 72%, #f0ebdf 100%);
  opacity: 0.65;
}

.preview-bead {
  position: absolute;
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  box-shadow: 0 4rpx 8rpx rgba(35, 56, 45, 0.2);
  background-size: cover;
  background-position: center;
}

.preview-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #859287;
  font-size: 22rpx;
}

.draft-title {
  position: relative;
  z-index: 1;
  margin-bottom: 10rpx;
  color: #1f2f27;
  font-size: 30rpx;
  font-weight: 700;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.muted {
  position: relative;
  z-index: 1;
  margin-bottom: 6rpx;
  text-align: center;
  font-size: 22rpx;
  color: #77857b;
}

.actions {
  position: relative;
  z-index: 1;
  margin-top: auto;
  padding-top: 18rpx;
  display: grid;
  gap: 12rpx;
}

.btn-mini {
  border-radius: 999rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.28);
  background: rgba(255, 252, 246, 0.9);
  color: #405c4f;
  padding: 14rpx 0;
  font-size: 24rpx;
  text-align: center;
  font-weight: 600;
}

.btn-mini:active {
  background: #f1e8d8;
}

.btn-mini.accent {
  border-color: transparent;
  background: linear-gradient(135deg, #5a8c72 0%, #3e6f57 100%);
  color: #fff;
  box-shadow: 0 6rpx 12rpx rgba(35, 56, 45, 0.16);
}

.remove {
  position: absolute;
  top: 16rpx;
  right: 16rpx;
  width: 46rpx;
  height: 46rpx;
  border-radius: 50%;
  border: 1rpx solid rgba(185, 152, 100, 0.35);
  background: rgba(255, 252, 246, 0.92);
  box-shadow: 0 3rpx 8rpx rgba(35, 56, 45, 0.1);
  color: #9e7e4d;
  text-align: center;
  line-height: 42rpx;
  font-size: 32rpx;
  font-weight: 300;
  z-index: 2;
}

.remove:active {
  color: #c39a60;
  background: #fff3df;
}

.empty-card {
  margin-top: 22rpx;
  min-height: 180rpx;
  justify-content: center;
}
</style>
