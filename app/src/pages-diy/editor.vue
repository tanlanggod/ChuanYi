<template>
  <view class="container">
    <view class="top-bar">
      <view class="notice">Notice</view>
      <view class="chip">Wrist - {{ form.wristSizeCm || "13" }}cm</view>
      <view class="chip">Total {{ formatAmount(totalPrice) }} CNY</view>
    </view>

    <view class="preview card">
      <view class="preview-title">{{ form.title || "My Design" }}</view>
      <view class="ring-wrap">
        <view class="ring"></view>
        <view class="bead" v-for="(dot, idx) in previewDots" :key="idx" :style="dot.style"></view>
        <view class="fly-bead" v-for="item in flyBeads" :key="item.id" :style="buildFlyBeadStyle(item)"></view>
        <view class="center-text">
          <view class="brand">MineStone</view>
          <view class="muted">MineStone</view>
        </view>
      </view>

      <view class="meta-row">
        <view class="meta-field">
          <text class="meta-label">Title</text>
          <input class="meta-input" v-model="form.title" placeholder="My Design" />
        </view>
        <view class="meta-field wrist">
          <text class="meta-label">Wrist(cm)</text>
          <input class="meta-input" type="digit" v-model="form.wristSizeCm" placeholder="13" />
        </view>
      </view>

      <view class="ops">
        <view class="btn-mini danger-light" @click="clearSelected">Clear</view>
        <view class="btn-save" @click="saveCurrentDraft">Save</view>
        <view class="btn-primary" @click="finishDesign">Finish</view>
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
            placeholder="Search materials..."
            confirm-type="search"
            @confirm="searchSku"
          />
          <view class="search-btn" @click="searchSku">Search</view>
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
          <view class="card" v-if="activeMaterialType === 'IN_USE' && selectedSkus.length === 0">
            <view class="muted">No selected materials</view>
          </view>

          <view class="sku-grid" v-else-if="activeMaterialType === 'IN_USE'">
            <view class="sku card" v-for="(sku, index) in selectedSkus" :key="`${sku.skuId}-${index}`">
              <image class="sku-img" :src="resolveImage(sku.imageUrl)" mode="aspectFill"></image>
              <view class="sku-title">{{ sku.name || 'Selected Item' }}</view>
              <view class="sku-meta">
                <view class="muted sku-spec">{{ sku.specText || "-" }}</view>
                <view class="sku-price">CNY {{ formatAmount(sku.price) }}</view>
              </view>
              <view class="btn-mini mt8 danger" @click="removeSku(index)">Remove</view>
            </view>
          </view>

          <view class="card" v-else-if="filteredCategories.length === 0">
            <view class="muted">No category in this type</view>
          </view>

          <view class="card" v-else-if="skus.length === 0">
            <view class="muted">No materials</view>
          </view>

          <view class="sku-grid" v-else>
            <view class="sku card" v-for="sku in skus" :key="sku.skuId">
              <image class="sku-img" :src="resolveImage(sku.imageUrl)" mode="aspectFill"></image>
              <view class="sku-title">{{ sku.name }}</view>
              <view class="sku-meta">
                <view class="muted sku-spec">{{ sku.specText || "-" }}</view>
                <view class="sku-price">CNY {{ formatAmount(sku.price) }}</view>
              </view>
              <view class="btn-mini mt8" @click="addSku(sku)">Add</view>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>

    <view class="selected-summary card" v-if="activeMaterialType !== 'IN_USE'">
      <view class="selected-summary-text">Selected {{ selectedSkus.length }} items</view>
      <view class="selected-summary-actions">
        <view class="btn-mini small" @click="changeMaterialType('IN_USE')">Manage selected</view>
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
        { label: "In Use", value: "IN_USE" },
        { label: "Bead", value: "BEAD" },
        { label: "Spacer", value: "SPACER" },
        { label: "Other", value: "OTHER" },
        { label: "Pendant", value: "PENDANT" },
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
        uni.showToast({ title: "Init failed", icon: "none" });
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
        const fallback = this.materialTabs.find((tab) => {
          if (tab.value === "IN_USE") {
            return false;
          }
          return this.categories.some((item) => this.matchCategoryType(item, tab.value));
        });
        if (fallback && fallback.value !== this.activeMaterialType) {
          this.activeMaterialType = fallback.value;
          list = this.filteredCategories;
        }
      }
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
          title: this.form.title || "My Design",
          wristSizeCm: this.form.wristSizeCm || "13",
          configJson: this.buildConfigJson(),
          previewImageUrl: "",
          pricePreview: this.totalPrice,
          version: 1,
        });
        this.draftId = data.draftId;
        uni.showToast({ title: "Draft saved", icon: "none" });
      } catch (error) {
        uni.showToast({ title: "Save failed", icon: "none" });
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
          uni.showToast({ title: "Snapshot create failed", icon: "none" });
          return;
        }
        uni.navigateTo({ url: `/pages-diy/snapshot-detail?snapshotNo=${snapshot.snapshotNo}` });
      } catch (error) {
        uni.showToast({ title: "Finish failed", icon: "none" });
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
  padding: 12rpx;
  padding-bottom: 84rpx;
}

.top-bar {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-bottom: 8rpx;
}

.notice {
  background: #d62b2b;
  color: #fff;
  font-size: 20rpx;
  border-radius: 999rpx;
  padding: 7rpx 12rpx;
}

.chip {
  background: #fff;
  border-radius: 999rpx;
  padding: 7rpx 12rpx;
  font-size: 20rpx;
  color: #4b5563;
}

.preview {
  margin-bottom: 8rpx;
  padding: 14rpx;
}

.preview-title {
  font-size: 26rpx;
  font-weight: 600;
  text-align: center;
}

.ring-wrap {
  margin: 10rpx auto 10rpx;
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
  border: 2rpx dashed #e5e7eb;
}

.bead {
  position: absolute;
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  box-shadow: 0 4rpx 8rpx rgba(0, 0, 0, 0.12);
  transition: left 460ms cubic-bezier(0.22, 1, 0.36, 1), top 460ms cubic-bezier(0.22, 1, 0.36, 1),
    opacity 220ms ease;
  will-change: left, top, opacity;
}

.fly-bead {
  position: absolute;
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  box-shadow: 0 6rpx 14rpx rgba(0, 0, 0, 0.16);
  pointer-events: none;
  z-index: 3;
}

.center-text {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  text-align: center;
}

.brand {
  font-size: 30rpx;
  color: #b25b7a;
  font-weight: 700;
}

.meta-row {
  margin-top: 2rpx;
  display: grid;
  grid-template-columns: 1fr 180rpx;
  gap: 10rpx;
  align-items: end;
}

.meta-field {
  display: grid;
  gap: 6rpx;
}

.meta-field.wrist {
  text-align: right;
}

.meta-label {
  font-size: 20rpx;
  color: #6b7280;
}

.meta-input {
  background: #f5f7fa;
  border-radius: 10rpx;
  padding: 8rpx 10rpx;
  font-size: 22rpx;
  line-height: 1.3;
}

.ops {
  margin-top: 10rpx;
  display: grid;
  grid-template-columns: 1fr 1fr 1.3fr;
  gap: 8rpx;
  margin-bottom: 4rpx;
}

.ops .btn-mini,
.ops .btn-save,
.ops .btn-primary {
  font-size: 22rpx;
  padding: 12rpx 10rpx;
}

.panel {
  display: flex;
  gap: 8rpx;
  height: 540rpx;
  min-height: 0;
  overflow: hidden;
}

.left {
  width: 108rpx;
  background: #fff;
  border-radius: 14rpx;
  height: 100%;
  min-height: 0;
}

.material-tab {
  padding: 14rpx 8rpx;
  border-left: 4rpx solid transparent;
  color: #666;
  text-align: center;
  font-size: 22rpx;
}

.material-tab.active {
  color: #d62b2b;
  font-weight: 700;
  border-left-color: #d62b2b;
  background: #fff7f7;
}

.right {
  flex: 1;
  background: #fff;
  border-radius: 14rpx;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.search-row {
  display: flex;
  gap: 8rpx;
  padding: 8rpx;
}

.search-input {
  flex: 1;
  background: #f5f7fa;
  border-radius: 999rpx;
  padding: 8rpx 12rpx;
  font-size: 22rpx;
}

.search-btn {
  background: #d62b2b;
  color: #fff;
  border-radius: 999rpx;
  padding: 8rpx 14rpx;
  font-size: 20rpx;
}

.cat-row {
  white-space: nowrap;
  padding: 0 8rpx 6rpx;
}

.cat-wrap {
  display: inline-flex;
  gap: 8rpx;
}

.cat-chip {
  border-radius: 999rpx;
  background: #f3f4f6;
  color: #4b5563;
  font-size: 20rpx;
  padding: 6rpx 12rpx;
}

.cat-chip.active {
  background: #fbe5e5;
  color: #b42318;
  font-weight: 700;
}

.right-scroll {
  flex: 1;
  height: 100%;
  min-height: 0;
  padding: 8rpx;
  box-sizing: border-box;
  overflow: hidden;
}

.sku-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8rpx;
}

.sku {
  padding: 10rpx;
  border-radius: 12rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.04);
}

.sku-title {
  font-size: 22rpx;
  font-weight: 600;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sku-meta {
  margin-top: 4rpx;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 6rpx;
}

.sku-spec {
  font-size: 20rpx;
  line-height: 1.2;
}

.sku-img {
  width: 100%;
  height: 104rpx;
  border-radius: 8rpx;
  background: #f3f4f6;
  margin-bottom: 6rpx;
}

.sku-price {
  margin-top: 0;
  font-size: 22rpx;
  color: #d62b2b;
  font-weight: 700;
  line-height: 1.2;
  white-space: nowrap;
}

.mt8 {
  margin-top: 6rpx;
}

.selected-summary {
  margin-top: 8rpx;
  padding: 12rpx 14rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.selected-summary-text {
  font-size: 22rpx;
  color: #4b5563;
}

.selected-summary-actions {
  display: flex;
  align-items: center;
  gap: 6rpx;
}

.btn-mini {
  border-radius: 999rpx;
  background: #eef1f5;
  color: #1f2937;
  padding: 8rpx 12rpx;
  font-size: 20rpx;
  text-align: center;
}

.btn-mini.small {
  padding: 6rpx 12rpx;
  font-size: 19rpx;
}

.btn-mini.disabled {
  opacity: 0.45;
}

.btn-mini.danger {
  background: #fbe5e5;
  color: #b42318;
}

.btn-mini.danger-light {
  background: #fef2f2;
  color: #b42318;
}

.btn-save {
  border-radius: 999rpx;
  background: #fff0f0;
  color: #d62b2b;
  padding: 12rpx 10rpx;
  font-size: 22rpx;
  text-align: center;
  font-weight: 600;
}
</style>
