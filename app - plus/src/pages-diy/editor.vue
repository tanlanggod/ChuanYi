<template>
  <view class="container">
    <view class="top-bar">
      <view class="notice">小贴士</view>
      <view class="chip">手围 - {{ form.wristSizeCm || "13" }}cm</view>
      <view class="chip">总计 {{ formatAmount(totalPrice) }} 元</view>
    </view>

    <view class="preview card">
      <view class="preview-title">{{ form.title || "我的设计" }}</view>
      <view class="ring-wrap">
        <view class="ring"></view>
        <view class="bead" v-for="(dot, idx) in previewDots" :key="idx" :style="dot.style"></view>
        <view class="fly-bead" v-for="item in flyBeads" :key="item.id" :style="buildFlyBeadStyle(item)"></view>
        <view class="center-text">
          <view class="brand">养个石头</view>
          <view class="muted">设计台</view>
        </view>
      </view>

      <view class="meta-row">
        <view class="meta-field">
          <text class="meta-label">设计名称</text>
          <input class="meta-input" v-model="form.title" placeholder="例如: 翠竹流云" />
        </view>
        <view class="meta-field wrist">
          <text class="meta-label">净手围(cm)</text>
          <input class="meta-input" type="digit" v-model="form.wristSizeCm" placeholder="默认 13" />
        </view>
      </view>

      <view class="ops">
        <view class="btn-mini danger-light" @click="clearSelected">清空珠子</view>
        <view class="btn-save" @click="saveCurrentDraft">保存草稿</view>
        <view class="btn-primary" @click="finishDesign">完成设计</view>
      </view>
    </view>

    <view class="panel">
      <scroll-view class="left" scroll-y="true">
        <view
          class="material-tab"
          :class="{ active: activeMaterialType === item.value }"
          v-for="item in materialTabs"
          :key="item.value"
          @click="changeMaterialType(item.value)"
        >
          {{ item.label }}
        </view>
      </scroll-view>

      <view class="right">
        <view class="search-row" v-if="activeMaterialType !== 'IN_USE'">
          <input
            class="search-input"
            v-model="keywordInput"
            placeholder="搜索散珠/配件..."
            confirm-type="search"
            @confirm="searchSku"
          />
          <view class="search-btn" @click="searchSku">搜索</view>
        </view>

        <scroll-view
          class="cat-row"
          scroll-x="true"
          v-if="activeMaterialType !== 'IN_USE' && filteredCategories.length > 0"
        >
          <view class="cat-wrap">
            <view
              class="cat-chip"
              :class="{ active: activeCategoryId === item.id }"
              v-for="item in filteredCategories"
              :key="item.id"
              @click="selectCategory(item.id)"
            >
              {{ item.name }}
            </view>
          </view>
        </scroll-view>

        <scroll-view class="right-scroll" scroll-y="true">
          <view class="card empty-card" v-if="activeMaterialType === 'IN_USE' && selectedSkus.length === 0">
            <view class="muted">当前未选择任何材料</view>
          </view>

          <view class="sku-grid" v-else-if="activeMaterialType === 'IN_USE'">
            <view class="sku card" v-for="(sku, index) in selectedSkus" :key="`${sku.skuId}-${index}`">
              <image class="sku-img" :src="resolveImage(sku.imageUrl)" mode="aspectFill"></image>
              <view class="sku-title">{{ sku.name || '已选商品' }}</view>
              <view class="sku-meta">
                <view class="muted sku-spec">{{ sku.specText || "-" }}</view>
                <view class="sku-price">￥{{ formatAmount(sku.price) }}</view>
              </view>
              <view class="btn-mini mt8 danger" @click="removeSku(index)">移除</view>
            </view>
          </view>

          <view class="card empty-card" v-else-if="filteredCategories.length === 0">
            <view class="muted">该分类下暂无商品</view>
          </view>

          <view class="card empty-card" v-else-if="skus.length === 0">
            <view class="muted">未搜索到相关材料</view>
          </view>

          <view class="sku-grid" v-else>
            <view class="sku card" v-for="sku in skus" :key="sku.skuId">
              <image class="sku-img" :src="resolveImage(sku.imageUrl)" mode="aspectFill"></image>
              <view class="sku-title">{{ sku.name }}</view>
              <view class="sku-meta">
                <view class="muted sku-spec">{{ sku.specText || "-" }}</view>
                <view class="sku-price">￥{{ formatAmount(sku.price) }}</view>
              </view>
              <view class="btn-mini mt8" @click="addSku(sku)">穿入</view>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>

    <view class="selected-summary card" v-if="activeMaterialType !== 'IN_USE'">
      <view class="selected-summary-text">已选中 {{ selectedSkus.length }} 颗珠材</view>
      <view class="selected-summary-actions">
        <view class="btn-mini small" @click="changeMaterialType('IN_USE')">管理已选</view>
      </view>
    </view>
  </view>
</template>
<script>
import {
  createSnapshotFromDraft,
  fetchDraftDetail,
  fetchSkuCategories,
  fetchSkuList,
  saveDraft,
} from "../common/api";
import { resolveAssetUrl } from "../common/http";
import { loginGuestIfNeeded } from "../common/session";

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
const RING_SIZE_RPX = 560;
const BEAD_SIZE_RPX = 48;
const RING_RADIUS_RPX = 245;
const RING_CENTER_RPX = 280;
const MAX_PREVIEW_DOTS = 20;
const FLY_DURATION_MS = 460;
const DEFAULT_BEAD_DIAMETER_MM = 8;
const BEAD_LIMIT_TOAST_TEXT = "Reached wrist-size limit, cannot add more beads.";

export default {
  data() {
    return {
      draftId: null,
      categories: [],
      skus: [],
      activeCategoryId: null,
      activeMaterialType: "BEAD",
      keywordInput: "",
      keyword: "",
      selectedSkus: [],
      flyBeads: [],
      flySeq: 0,
      flyTimerIds: [],
      beadLimitToastAt: 0,
      saving: false,
      materialTabs: [
        { label: "已选", value: "IN_USE" },
        { label: "散珠", value: "BEAD" },
        { label: "隔珠", value: "SPACER" },
        { label: "其他", value: "OTHER" },
        { label: "吊坠", value: "PENDANT" },
      ],
      form: {
        title: "",
        wristSizeCm: "13",
      },
    };
  },
  computed: {
    totalPrice() {
      return this.selectedSkus.reduce((sum, item) => sum + Number(item.price || 0), 0);
    },
    previewSource() {
      if (this.selectedSkus.length === 0 && this.flyBeads.length === 0) {
        return PREVIEW_COLORS.map((color) => ({
          previewColor: color,
          __pending: false,
        }));
      }
      const settled = this.selectedSkus.map((item) => ({
        ...item,
        __pending: false,
      }));
      const pending = this.flyBeads
        .slice()
        .sort((a, b) => Number(a.id || 0) - Number(b.id || 0))
        .map((item) => ({
          skuId: item.skuId,
          name: item.name || "",
          specText: item.specText || "",
          price: Number(item.price || 0),
          imageUrl: item.imageUrl || "",
          previewColor: item.previewColor || "",
          __pending: true,
        }));
      return settled.concat(pending).slice(0, MAX_PREVIEW_DOTS);
    },
    previewDots() {
      const source = this.previewSource;
      const count = Math.min(source.length, MAX_PREVIEW_DOTS);
      const dots = [];
      for (let i = 0; i < count; i += 1) {
        const position = this.getPreviewDotPosition(i, count);
        const bead = source[i];
        const style = {
          left: `${position.x}rpx`,
          top: `${position.y}rpx`,
          background: bead.previewColor || PREVIEW_COLORS[i % PREVIEW_COLORS.length],
          opacity: bead.__pending ? "0" : "1",
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
    filteredCategories() {
      if (this.activeMaterialType === "IN_USE") {
        return [];
      }
      return this.categories.filter((item) => this.matchCategoryType(item, this.activeMaterialType));
    },
  },
  async onLoad(options) {
    this.draftId = options && options.draftId ? Number(options.draftId) : null;
    await this.initData();
  },
  onUnload() {
    this.clearFlyTimers();
  },
  methods: {
    async initData() {
      try {
        await loginGuestIfNeeded();
        await this.loadCategories();
        if (this.draftId) {
          await this.loadDraftDetail(this.draftId);
        }
      } catch (error) {
        uni.showToast({ title: "初始化失败", icon: "none" });
      }
    },
    async loadCategories() {
      const data = await fetchSkuCategories();
      this.categories = (data && data.categories) || [];
      this.ensureActiveCategory();
      await this.loadSkus();
    },
    matchCategoryType(item, type) {
      const current = String((item && item.categoryType) || "").toUpperCase();
      return current === type;
    },
    ensureActiveCategory() {
      if (this.activeMaterialType === "IN_USE") {
        this.activeCategoryId = null;
        return;
      }
      let list = this.filteredCategories;
      if (list.length === 0) {
        this.activeCategoryId = null;
        return;
      }
      const current = list.find((item) => Number(item.id) === Number(this.activeCategoryId));
      if (current) {
        return;
      }
      this.activeCategoryId = list[0].id;
    },
    async loadSkus() {
      if (this.activeMaterialType === "IN_USE") {
        this.skus = [];
        return;
      }
      if (!this.activeCategoryId) {
        this.skus = [];
        return;
      }
      const data = await fetchSkuList({
        categoryId: this.activeCategoryId,
        keyword: this.keyword || undefined,
        pageNo: 1,
        pageSize: 50,
      });
      this.skus = (data && data.list) || [];
    },
    async changeMaterialType(materialType) {
      this.activeMaterialType = materialType;
      this.ensureActiveCategory();
      await this.loadSkus();
    },
    async selectCategory(categoryId) {
      this.activeCategoryId = categoryId;
      await this.loadSkus();
    },
    async searchSku() {
      this.keyword = (this.keywordInput || "").trim();
      await this.loadSkus();
    },
    addSku(sku) {
      const selectedSku = {
        skuId: sku.skuId,
        name: sku.name,
        specText: sku.specText,
        price: Number(sku.price || 0),
        beadDiameterMm: Number(sku.beadDiameterMm || 0),
        imageUrl: sku.imageUrl || "",
      };
      this.animateAddSku(selectedSku);
    },
    animateAddSku(sku) {
      const pendingCount = this.flyBeads.length;
      const currentCount = this.selectedSkus.length + pendingCount;
      if (!this.canAddSkuByWrist(sku)) {
        this.showBeadLimitToast();
        return;
      }
      if (currentCount >= MAX_PREVIEW_DOTS) {
        this.showBeadLimitToast();
        return;
      }

      const targetCount = currentCount + 1;
      const targetPosition = this.getPreviewDotPosition(currentCount, targetCount);
      const startPosition = this.getFlyStartPosition();
      const flyId = this.flySeq + 1;
      this.flySeq = flyId;

      this.flyBeads.push({
        id: flyId,
        active: false,
        skuId: sku.skuId,
        name: sku.name || "",
        specText: sku.specText || "",
        price: Number(sku.price || 0),
        beadDiameterMm: Number(sku.beadDiameterMm || 0),
        startX: startPosition.x,
        startY: startPosition.y,
        targetX: targetPosition.x,
        targetY: targetPosition.y,
        previewColor: PREVIEW_COLORS[currentCount % PREVIEW_COLORS.length],
        imageUrl: sku.imageUrl || "",
      });

      this.$nextTick(() => {
        const activateTimer = setTimeout(() => {
          const target = this.flyBeads.find((item) => Number(item.id) === Number(flyId));
          if (target) {
            target.active = true;
          }
          this.flyTimerIds = this.flyTimerIds.filter((timerId) => Number(timerId) !== Number(activateTimer));
        }, 20);
        this.flyTimerIds.push(activateTimer);
      });

      const settleTimer = setTimeout(() => {
        this.selectedSkus.push({
          skuId: sku.skuId,
          name: sku.name || "",
          specText: sku.specText || "",
          price: Number(sku.price || 0),
          beadDiameterMm: Number(sku.beadDiameterMm || 0),
          imageUrl: sku.imageUrl || "",
        });
        this.flyBeads = this.flyBeads.filter((item) => Number(item.id) !== Number(flyId));
        this.flyTimerIds = this.flyTimerIds.filter((timerId) => Number(timerId) !== Number(settleTimer));
      }, FLY_DURATION_MS + 40);
      this.flyTimerIds.push(settleTimer);
    },
    getPreviewDotPosition(index, count) {
      const safeCount = Math.max(1, Number(count || 1));
      const safeIndex = Math.max(0, Number(index || 0));
      const angle = (Math.PI * 2 * safeIndex) / safeCount - Math.PI / 2;
      const x = RING_CENTER_RPX + Math.cos(angle) * RING_RADIUS_RPX - BEAD_SIZE_RPX / 2;
      const y = RING_CENTER_RPX + Math.sin(angle) * RING_RADIUS_RPX - BEAD_SIZE_RPX / 2;
      return { x, y };
    },
    getFlyStartPosition() {
      const margin = 8;
      return {
        x: RING_SIZE_RPX - BEAD_SIZE_RPX - margin,
        y: RING_SIZE_RPX - BEAD_SIZE_RPX - margin,
      };
    },
    buildFlyBeadStyle(item) {
      const style = {
        left: `${item.active ? item.targetX : item.startX}rpx`,
        top: `${item.active ? item.targetY : item.startY}rpx`,
        opacity: item.active ? 1 : 0.2,
        transform: item.active ? "scale(1)" : "scale(0.64)",
        transition: `left ${FLY_DURATION_MS}ms cubic-bezier(0.22, 1, 0.36, 1), top ${FLY_DURATION_MS}ms cubic-bezier(0.22, 1, 0.36, 1), transform ${FLY_DURATION_MS}ms ease, opacity ${FLY_DURATION_MS}ms ease`,
        background: item.previewColor || PREVIEW_COLORS[0],
      };
      if (item.imageUrl) {
        style.backgroundImage = `url(${this.resolveImage(item.imageUrl)})`;
        style.backgroundSize = "cover";
        style.backgroundPosition = "center";
      }
      return style;
    },
    canAddSkuByWrist(sku) {
      const wristLimitMm = this.getWristLimitMm();
      if (wristLimitMm <= 0) {
        return true;
      }
      const nextLengthMm = this.getCurrentBeadLengthMm() + this.getSkuDiameterMm(sku);
      return nextLengthMm <= wristLimitMm;
    },
    getWristLimitMm() {
      const wrist = Number(this.form.wristSizeCm || 0);
      if (!Number.isFinite(wrist) || wrist <= 0) {
        return 130;
      }
      return wrist * 10;
    },
    getCurrentBeadLengthMm() {
      let total = 0;
      this.selectedSkus.forEach((item) => {
        total += this.getSkuDiameterMm(item);
      });
      this.flyBeads.forEach((item) => {
        total += this.getSkuDiameterMm(item);
      });
      return total;
    },
    getSkuDiameterMm(sku) {
      const direct = Number((sku && sku.beadDiameterMm) || 0);
      if (Number.isFinite(direct) && direct > 0) {
        return direct;
      }
      const spec = String((sku && sku.specText) || "");
      const matched = spec.match(/(\d+(?:\.\d+)?)\s*mm/i);
      if (matched && matched[1]) {
        const parsed = Number(matched[1]);
        if (Number.isFinite(parsed) && parsed > 0) {
          return parsed;
        }
      }
      return DEFAULT_BEAD_DIAMETER_MM;
    },
    showBeadLimitToast() {
      const now = Date.now();
      if (now - this.beadLimitToastAt < 700) {
        return;
      }
      this.beadLimitToastAt = now;
      uni.showToast({ title: BEAD_LIMIT_TOAST_TEXT, icon: "none" });
    },
    clearFlyTimers() {
      this.flyTimerIds.forEach((timerId) => {
        clearTimeout(timerId);
      });
      this.flyTimerIds = [];
    },
    removeSku(index) {
      this.selectedSkus.splice(index, 1);
    },
    moveSku(index, offset) {
      const target = index + offset;
      if (index < 0 || target < 0 || target >= this.selectedSkus.length) {
        return;
      }
      const next = this.selectedSkus.slice();
      const current = next[index];
      next.splice(index, 1);
      next.splice(target, 0, current);
      this.selectedSkus = next;
    },
    clearSelected() {
      this.selectedSkus = [];
    },
    async loadDraftDetail(draftId) {
      const data = await fetchDraftDetail(draftId);
      if (!data) {
        return;
      }
      this.form.title = data.title || "";
      this.form.wristSizeCm = String(data.wristSizeCm || "13");
      this.selectedSkus = this.parseSelectedSkus(data.configJson);
    },
    parseSelectedSkus(configJson) {
      if (!configJson) {
        return [];
      }
      try {
        const parsed = typeof configJson === "string" ? JSON.parse(configJson) : configJson;
        const list = parsed && parsed.selectedSkus;
        if (!Array.isArray(list)) {
          return [];
        }
        return list.map((item) => ({
          skuId: item.skuId,
          name: item.name || "",
          specText: item.specText || "",
          price: Number(item.price || 0),
          beadDiameterMm: Number(item.beadDiameterMm || 0),
          imageUrl: item.imageUrl || "",
        }));
      } catch (_error) {
        return [];
      }
    },
    buildConfigJson() {
      return JSON.stringify({
        selectedSkus: this.selectedSkus,
        selectedCount: this.selectedSkus.length,
      });
    },

    async saveCurrentDraft() {
      if (this.saving) {
        return;
      }
      this.saving = true;
      try {
        const data = await saveDraft({
          draftId: this.draftId || undefined,
          title: this.form.title || "我的设计",
          wristSizeCm: this.form.wristSizeCm || "13",
          configJson: this.buildConfigJson(),
          previewImageUrl: "",
          pricePreview: this.totalPrice,
          version: 1,
        });
        this.draftId = data.draftId;
        uni.showToast({ title: "草稿已保存", icon: "none" });
      } catch (error) {
        uni.showToast({ title: "保存失败", icon: "none" });
      } finally {
        this.saving = false;
      }
    },
    async finishDesign() {
      try {
        if (!this.draftId) {
          await this.saveCurrentDraft();
        }
        if (!this.draftId) {
          return;
        }
        const snapshot = await createSnapshotFromDraft(this.draftId);
        if (!snapshot || !snapshot.snapshotNo) {
          uni.showToast({ title: "快照生成失败", icon: "none" });
          return;
        }
        uni.navigateTo({ url: `/pages-diy/snapshot-detail?snapshotNo=${snapshot.snapshotNo}` });
      } catch (error) {
        uni.showToast({ title: "完成设计失败", icon: "none" });
      }
    },
    formatAmount(value) {
      const text = Number(value || 0).toFixed(2);
      return text.replace(/\.00$/, "").replace(/(\.\d)0$/, "$1");
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
  box-sizing: border-box;
  padding: 16rpx;
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
  display: flex;
  flex-direction: column;
}

.top-bar {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 12rpx;
}

.notice {
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #c8a26b 0%, #b38d58 100%);
  color: #fff;
  font-size: 20rpx;
  font-weight: 600;
}

.chip {
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.24);
  background: rgba(255, 252, 246, 0.9);
  color: #5e7266;
  font-size: 20rpx;
  font-weight: 500;
}

.preview,
.left,
.right,
.sku,
.selected-summary,
.empty-card {
  border-radius: 22rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.2);
  background: linear-gradient(160deg, #fffdfa 0%, #f8f1e6 100%);
  box-shadow: 0 10rpx 20rpx rgba(35, 56, 45, 0.08);
}

.preview {
  margin-bottom: 12rpx;
  padding: 22rpx;
}

.preview-title {
  color: #1f2f27;
  font-size: 30rpx;
  font-weight: 700;
  text-align: center;
}

.ring-wrap {
  margin: 20rpx auto;
  width: 560rpx;
  height: 560rpx;
  position: relative;
}

.ring {
  position: absolute;
  left: 35rpx;
  top: 35rpx;
  width: 490rpx;
  height: 490rpx;
  border-radius: 50%;
  border: 2rpx dashed rgba(185, 152, 100, 0.62);
  background: radial-gradient(circle at center, #ffffff 0%, #faf7f1 72%, #f0ebdf 100%);
  opacity: 0.7;
}

.bead {
  position: absolute;
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  box-shadow: 0 4rpx 8rpx rgba(35, 56, 45, 0.18);
  transition: left 460ms cubic-bezier(0.22, 1, 0.36, 1), top 460ms cubic-bezier(0.22, 1, 0.36, 1), opacity 220ms ease;
  will-change: left, top, opacity;
}

.fly-bead {
  position: absolute;
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  box-shadow: 0 8rpx 16rpx rgba(35, 56, 45, 0.24);
  pointer-events: none;
  z-index: 3;
}

.center-text {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.brand {
  color: #3e6f57;
  font-size: 34rpx;
  font-weight: 800;
  letter-spacing: 2rpx;
}

.muted {
  margin-top: 4rpx;
  color: #a68450;
  font-size: 20rpx;
}

.meta-row {
  margin-top: 10rpx;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
}

.meta-field {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.meta-field.wrist {
  align-items: flex-start;
}

.meta-label {
  color: #7a887f;
  font-size: 24rpx;
  font-weight: 500;
}

.meta-input {
  width: 100%;
  height: 64rpx;
  line-height: 32rpx;
  box-sizing: border-box;
  padding: 16rpx 20rpx;
  border-radius: 12rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.22);
  background: rgba(255, 252, 246, 0.9);
  color: #2f463b;
  font-size: 26rpx;
}

.meta-input:focus {
  border-color: rgba(62, 111, 87, 0.6);
  background: #fff;
}

.ops {
  margin-top: 24rpx;
  margin-bottom: 4rpx;
  display: grid;
  grid-template-columns: 1fr 1fr 1.5fr;
  gap: 16rpx;
}

.ops .btn-mini,
.ops .btn-save,
.ops .btn-primary {
  border-radius: 999rpx;
  font-size: 26rpx;
  padding: 16rpx 10rpx;
  text-align: center;
  font-weight: 600;
}

.ops .btn-primary {
  color: #fff;
  border: 1rpx solid transparent;
  background: linear-gradient(135deg, #5a8c72 0%, #3e6f57 100%);
  box-shadow: 0 6rpx 12rpx rgba(35, 56, 45, 0.16);
}

.ops .btn-save {
  border: 1rpx solid rgba(62, 111, 87, 0.38);
  background: #edf4ef;
  color: #3e6f57;
}

.btn-mini.danger-light {
  border: 1rpx solid rgba(185, 152, 100, 0.3);
  background: #fff3e1;
  color: #a68450;
}

.panel {
  flex: 1;
  min-height: 0;
  margin-bottom: 12rpx;
  display: flex;
  gap: 12rpx;
}

.left {
  width: 140rpx;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.material-tab {
  position: relative;
  padding: 22rpx 10rpx;
  color: #7a887f;
  text-align: center;
  font-size: 24rpx;
}

.material-tab.active {
  color: #3e6f57;
  font-weight: 700;
  background: #edf4ef;
}

.material-tab.active::before {
  content: "";
  position: absolute;
  left: 0;
  top: 20%;
  bottom: 20%;
  width: 6rpx;
  border-radius: 0 6rpx 6rpx 0;
  background: #3e6f57;
}

.right {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.search-row {
  display: flex;
  gap: 12rpx;
  padding: 16rpx;
  border-bottom: 1rpx solid rgba(185, 152, 100, 0.2);
}

.search-input {
  flex: 1;
  padding: 10rpx 20rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.24);
  background: rgba(255, 252, 246, 0.9);
  color: #2f463b;
  font-size: 24rpx;
}

.search-btn {
  padding: 10rpx 24rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #5a8c72 0%, #3e6f57 100%);
  color: #fff;
  font-size: 24rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cat-row {
  white-space: nowrap;
  padding: 12rpx 16rpx;
  border-bottom: 1rpx solid rgba(185, 152, 100, 0.2);
}

.cat-wrap {
  display: inline-flex;
  gap: 12rpx;
}

.cat-chip {
  padding: 8rpx 20rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.24);
  background: rgba(255, 252, 246, 0.9);
  color: #7c8a80;
  font-size: 22rpx;
}

.cat-chip.active {
  border-color: transparent;
  background: linear-gradient(135deg, #5a8c72 0%, #3e6f57 100%);
  color: #fff;
  font-weight: 600;
}

.right-scroll {
  flex: 1;
  height: 100%;
  min-height: 0;
  box-sizing: border-box;
  padding: 16rpx;
  overflow: hidden;
  background: #fcf7ed;
}

.empty-card {
  min-height: 170rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sku-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14rpx;
}

.sku {
  padding: 12rpx;
  display: flex;
  flex-direction: column;
}

.sku-title {
  margin-top: 8rpx;
  color: #1f2f27;
  font-size: 24rpx;
  font-weight: 600;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sku-meta {
  margin-top: 8rpx;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8rpx;
}

.sku-spec {
  color: #8a968d;
  font-size: 20rpx;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sku-img {
  width: 100%;
  height: 120rpx;
  border-radius: 12rpx;
  background: #e8decd;
}

.sku-price {
  color: #b38d58;
  font-size: 24rpx;
  font-weight: 700;
}

.mt8 {
  margin-top: 12rpx;
}

.btn-mini {
  border-radius: 999rpx;
  border: 1rpx solid rgba(62, 111, 87, 0.26);
  background: #edf4ef;
  color: #3e6f57;
  padding: 10rpx 0;
  font-size: 22rpx;
  text-align: center;
  font-weight: 600;
}

.btn-mini:active {
  background: #dfece5;
}

.btn-mini.danger {
  border-color: rgba(185, 152, 100, 0.3);
  background: #fff3e1;
  color: #a68450;
}

.selected-summary {
  position: fixed;
  z-index: 100;
  left: 24rpx;
  right: 24rpx;
  bottom: calc(24rpx + env(safe-area-inset-bottom));
  padding: 20rpx 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.selected-summary-text {
  color: #5a7265;
  font-size: 24rpx;
  font-weight: 600;
}

.selected-summary-actions {
  display: flex;
  align-items: center;
}

.btn-mini.small {
  padding: 8rpx 20rpx;
  font-size: 22rpx;
}
</style>
