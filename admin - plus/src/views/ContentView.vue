<template>
  <el-card class="admin-page">
    <template #header>
      <div class="admin-page__header">
        <div class="admin-page__heading">
          <div class="admin-page__title"><el-icon><Document /></el-icon><span>内容管理</span></div>
          <div class="admin-page__subtitle">Banner 与设计广场统一版式与控件层级</div>
        </div>
        <div class="admin-page__actions"><el-tag type="info">Banner / 设计广场</el-tag></div>
      </div>
    </template>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="Banner 管理" name="banner">
        <div class="admin-page__toolbar">
          <el-input
            v-model="bannerQuery.keyword"
            placeholder="Banner 标题"
            clearable
            class="w220"
            @keyup.enter="searchBanner"
          />
          <el-select v-model="bannerQuery.status" placeholder="全部状态" clearable class="w160">
            <el-option label="启用" value="ENABLED" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
          <el-button type="primary" :loading="bannerLoading" @click="searchBanner" :icon="'Search'">查询</el-button>
          <el-button @click="resetBanner" :icon="'Refresh'">重置</el-button>
          <el-button type="success" @click="openBannerDialog()">新增 Banner</el-button>
          <el-button
            type="primary"
            plain
            :disabled="selectedBannerIds.length === 0"
            @click="batchChangeBannerStatus('ENABLED')"
          >
            批量启用
          </el-button>
          <el-button
            type="warning"
            plain
            :disabled="selectedBannerIds.length === 0"
            @click="batchChangeBannerStatus('DISABLED')"
          >
            批量禁用
          </el-button>
          <el-button
            type="success"
            plain
            :disabled="selectedBannerIds.length === 0"
            @click="openBatchBannerJumpDialog"
          >
            批量设置跳转
          </el-button>
        </div>

        <div class="summary-row">
          <el-tag type="success">启用 {{ bannerSummary.enabled }}</el-tag>
          <el-tag type="info">禁用 {{ bannerSummary.disabled }}</el-tag>
          <el-tag>已选 {{ selectedBannerIds.length }}</el-tag>
        </div>

        <div class="admin-table-shell">
          <el-table :data="banners" border v-loading="bannerLoading" row-key="id" @selection-change="onBannerSelectionChange">
          <el-table-column type="selection" width="46" />
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="title" label="标题" min-width="220" />
          <el-table-column label="图片" width="120">
            <template #default="{ row }">
              <el-image
                v-if="row.imageUrl"
                :src="resolveImageUrl(row.imageUrl)"
                fit="cover"
                style="width: 64px; height: 64px; border-radius: 6px"
              />
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="jumpType" label="跳转类型" width="140" />
          <el-table-column prop="jumpValue" label="跳转值" min-width="180" />
          <el-table-column label="排序" width="150">
            <template #default="{ row }">
              <div class="sort-cell">
                <el-input-number v-model="row.sortNo" :min="0" :max="999999" controls-position="right" />
                <el-button link type="primary" @click="saveBannerSort(row)">保存</el-button>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">
                {{ row.status === "ENABLED" ? "启用" : "禁用" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openBannerDialog(row)">编辑</el-button>
              <el-button
                v-if="row.status === 'ENABLED'"
                link
                type="danger"
                @click="changeBannerStatus(row.id, 'DISABLED')"
              >
                禁用
              </el-button>
              <el-button
                v-else
                link
                type="primary"
                @click="changeBannerStatus(row.id, 'ENABLED')"
              >
                启用
              </el-button>
            </template>
            </el-table-column>
          </el-table>

          <div class="admin-page__pager">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next"
              :total="bannerTotal"
              :page-sizes="[10, 20, 50]"
              :page-size="bannerQuery.pageSize"
              :current-page="bannerQuery.pageNo"
              @size-change="onBannerSizeChange"
              @current-change="onBannerPageChange"
            />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="设计广场管理" name="gallery">
        <div class="admin-page__toolbar">
          <el-input
            v-model="galleryQuery.keyword"
            placeholder="作品标题"
            clearable
            class="w220"
            @keyup.enter="searchGallery"
          />
          <el-select v-model="galleryQuery.displayStatus" placeholder="全部状态" clearable class="w160">
            <el-option label="展示中" value="ENABLED" />
            <el-option label="隐藏" value="DISABLED" />
            <el-option label="已下线" value="HIDDEN" />
          </el-select>
          <el-button type="primary" :loading="galleryLoading" @click="searchGallery" :icon="'Search'">查询</el-button>
          <el-button @click="resetGallery" :icon="'Refresh'">重置</el-button>
          <el-button
            type="primary"
            plain
            :disabled="selectedGalleryIds.length === 0"
            @click="batchChangeGalleryStatus('ENABLED')"
          >
            批量展示
          </el-button>
          <el-button
            type="warning"
            plain
            :disabled="selectedGalleryIds.length === 0"
            @click="batchChangeGalleryStatus('DISABLED')"
          >
            批量隐藏
          </el-button>
          <el-button
            type="danger"
            plain
            :disabled="selectedGalleryIds.length === 0"
            @click="batchChangeGalleryStatus('HIDDEN')"
          >
            批量下线
          </el-button>
        </div>

        <div class="summary-row">
          <el-tag type="success">展示 {{ gallerySummary.enabled }}</el-tag>
          <el-tag type="warning">隐藏 {{ gallerySummary.disabled }}</el-tag>
          <el-tag type="info">下线 {{ gallerySummary.hidden }}</el-tag>
          <el-tag>已选 {{ selectedGalleryIds.length }}</el-tag>
        </div>

        <div class="admin-table-shell">
          <el-table :data="galleryRows" border v-loading="galleryLoading" row-key="id" @selection-change="onGallerySelectionChange">
            <el-table-column type="selection" width="46" />
            <el-table-column prop="id" label="ID" width="90" />
            <el-table-column prop="title" label="作品标题" min-width="220" />
            <el-table-column label="封面" width="120">
              <template #default="{ row }">
                <el-image
                  v-if="row.coverImageUrl"
                  :src="resolveImageUrl(row.coverImageUrl)"
                  fit="cover"
                  style="width: 64px; height: 64px; border-radius: 6px"
                />
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="userId" label="用户ID" width="100" />
            <el-table-column label="标签" min-width="180">
              <template #default="{ row }">{{ formatTags(row.tags) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="displayStatusTag(row.displayStatus)">
                  {{ formatDisplayStatus(row.displayStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="publishedAt" label="发布时间" min-width="170" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openGalleryDialog(row)">编辑</el-button>
                <el-button
                  v-if="row.displayStatus !== 'ENABLED'"
                  link
                  type="primary"
                  @click="changeGalleryStatus(row.id, 'ENABLED')"
                >
                  展示
                </el-button>
                <el-button
                  v-if="row.displayStatus === 'ENABLED'"
                  link
                  type="warning"
                  @click="changeGalleryStatus(row.id, 'DISABLED')"
                >
                  隐藏
                </el-button>
                <el-button
                  v-if="row.displayStatus !== 'HIDDEN'"
                  link
                  type="danger"
                  @click="changeGalleryStatus(row.id, 'HIDDEN')"
                >
                  下线
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="admin-page__pager">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next"
              :total="galleryTotal"
              :page-sizes="[10, 20, 50]"
              :page-size="galleryQuery.pageSize"
              :current-page="galleryQuery.pageNo"
              @size-change="onGallerySizeChange"
              @current-change="onGalleryPageChange"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </el-card>

  <el-dialog v-model="bannerDialogVisible" :title="bannerForm.id ? '编辑 Banner' : '新增 Banner'" width="520px">
    <el-form label-position="top">
      <el-form-item label="标题">
        <el-input v-model="bannerForm.title" maxlength="128" show-word-limit />
      </el-form-item>
      <el-form-item label="图片 URL">
        <el-input v-model="bannerForm.imageUrl" maxlength="255" show-word-limit />
        <div class="upload-line">
          <el-upload
            :show-file-list="false"
            :http-request="uploadBannerImage"
            :before-upload="beforeImageUpload"
            accept=".jpg,.jpeg,.png,.webp,.gif"
          >
            <el-button size="small" :loading="bannerUploadLoading" :disabled="bannerUploadLoading">上传图片</el-button>
          </el-upload>
          <el-image
            v-if="bannerForm.imageUrl"
            :src="resolveImageUrl(bannerForm.imageUrl)"
            fit="cover"
            style="width: 52px; height: 52px; border-radius: 6px"
          />
        </div>
      </el-form-item>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="跳转类型">
            <el-select v-model="bannerForm.jumpType" class="full">
              <el-option label="无" value="NONE" />
              <el-option label="商品页" value="GOODS" />
              <el-option label="定制页" value="DIY" />
              <el-option label="设计广场" value="GALLERY" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="排序">
            <el-input-number v-model="bannerForm.sortNo" :min="0" :max="999999" class="full" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="跳转值">
        <div class="jump-picker-line">
          <el-input v-model="bannerForm.jumpValue" maxlength="255" show-word-limit />
          <el-button @click="openJumpPicker('single')">选择ID</el-button>
        </div>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="bannerForm.status" class="full">
          <el-option label="启用" value="ENABLED" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="admin-dialog-footer">
        <el-button @click="bannerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bannerSaving" @click="submitBanner">保存</el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="bannerBatchJumpVisible" title="批量设置 Banner 跳转" width="520px">
    <el-form label-position="top">
      <el-form-item label="已选条数">
        <el-tag>共 {{ selectedBannerIds.length }} 条</el-tag>
      </el-form-item>
      <el-form-item label="跳转类型">
        <el-select v-model="bannerBatchJumpForm.jumpType" class="full">
          <el-option label="无" value="NONE" />
          <el-option label="商品页" value="GOODS" />
          <el-option label="定制页" value="DIY" />
          <el-option label="设计广场" value="GALLERY" />
        </el-select>
      </el-form-item>
      <el-form-item label="跳转值">
        <div class="jump-picker-line">
          <el-input v-model="bannerBatchJumpForm.jumpValue" maxlength="255" show-word-limit />
          <el-button @click="openJumpPicker('batch')">选择ID</el-button>
        </div>
        <div class="batch-tip">提示：GOODS 填商品 SPU ID，GALLERY 填作品 ID，其它类型可留空。</div>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="admin-dialog-footer">
        <el-button @click="bannerBatchJumpVisible = false">取消</el-button>
        <el-button type="primary" :loading="bannerBatchJumpSaving" @click="submitBatchBannerJump">保存</el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="galleryDialogVisible" title="编辑广场作品" width="520px">
    <el-form label-position="top">
      <el-form-item label="作品标题">
        <el-input v-model="galleryForm.title" maxlength="128" show-word-limit />
      </el-form-item>
      <el-form-item label="封面图 URL">
        <el-input v-model="galleryForm.coverImageUrl" maxlength="255" show-word-limit />
        <div class="upload-line">
          <el-upload
            :show-file-list="false"
            :http-request="uploadGalleryImage"
            :before-upload="beforeImageUpload"
            accept=".jpg,.jpeg,.png,.webp,.gif"
          >
            <el-button size="small" :loading="galleryUploadLoading" :disabled="galleryUploadLoading">上传封面</el-button>
          </el-upload>
          <el-image
            v-if="galleryForm.coverImageUrl"
            :src="resolveImageUrl(galleryForm.coverImageUrl)"
            fit="cover"
            style="width: 52px; height: 52px; border-radius: 6px"
          />
        </div>
      </el-form-item>
      <el-form-item label="标签（逗号分隔）">
        <el-input v-model="galleryForm.tags" maxlength="255" show-word-limit />
      </el-form-item>
      <el-form-item label="展示状态">
        <el-select v-model="galleryForm.displayStatus" class="full">
          <el-option label="展示中" value="ENABLED" />
          <el-option label="隐藏" value="DISABLED" />
          <el-option label="已下线" value="HIDDEN" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="admin-dialog-footer">
        <el-button @click="galleryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="gallerySaving" @click="submitGallery">保存</el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="jumpPickerVisible" title="选择跳转目标" width="760px">
    <el-tabs v-model="jumpPickerTab" @tab-change="handleJumpPickerTabChange">
      <el-tab-pane label="商品（GOODS）" name="goods">
        <div class="picker-toolbar">
          <el-input
            v-model="jumpGoodsQuery.keyword"
            placeholder="搜索商品标题"
            clearable
            class="w220"
            @keyup.enter="searchJumpGoods"
          />
          <el-button type="primary" :loading="jumpPickerLoading" @click="searchJumpGoods" :icon="'Search'">查询</el-button>
        </div>
        <div class="recent-id-row" v-if="recentGoodsJumpIds.length > 0">
          <span class="recent-id-label">最近使用：</span>
          <el-tag
            v-for="id in recentGoodsJumpIds"
            :key="`goods_recent_${id}`"
            class="recent-id-tag"
            @click="pickJumpValue('GOODS', id)"
          >
            {{ id }}
          </el-tag>
          <el-button link type="primary" @click="clearRecentJumpIds('GOODS')">清空</el-button>
        </div>
        <el-table :data="jumpGoodsRows" border v-loading="jumpPickerLoading">
          <el-table-column prop="spuId" label="SPU ID" width="100" />
          <el-table-column prop="title" label="商品标题" min-width="220" />
          <el-table-column label="价格区间" width="160">
            <template #default="{ row }">
              ￥{{ formatMoney(row.minPrice) }} ~ ￥{{ formatMoney(row.maxPrice) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="pickJumpValue('GOODS', row.spuId)">选择</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="admin-page__pager">
          <el-pagination
            background
            layout="total, prev, pager, next"
            :total="jumpGoodsTotal"
            :page-size="jumpGoodsQuery.pageSize"
            :current-page="jumpGoodsQuery.pageNo"
            @current-change="onJumpGoodsPageChange"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="设计广场（GALLERY）" name="gallery">
        <div class="picker-toolbar">
          <el-input
            v-model="jumpGalleryQuery.keyword"
            placeholder="搜索作品标题"
            clearable
            class="w220"
            @keyup.enter="searchJumpGallery"
          />
          <el-button type="primary" :loading="jumpPickerLoading" @click="searchJumpGallery" :icon="'Search'">查询</el-button>
        </div>
        <div class="recent-id-row" v-if="recentGalleryJumpIds.length > 0">
          <span class="recent-id-label">最近使用：</span>
          <el-tag
            v-for="id in recentGalleryJumpIds"
            :key="`gallery_recent_${id}`"
            class="recent-id-tag"
            @click="pickJumpValue('GALLERY', id)"
          >
            {{ id }}
          </el-tag>
          <el-button link type="primary" @click="clearRecentJumpIds('GALLERY')">清空</el-button>
        </div>
        <el-table :data="jumpGalleryRows" border v-loading="jumpPickerLoading">
          <el-table-column prop="id" label="作品ID" width="100" />
          <el-table-column prop="title" label="作品标题" min-width="220" />
          <el-table-column prop="publishedAt" label="发布时间" min-width="170" />
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="pickJumpValue('GALLERY', row.id)">选择</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="admin-page__pager">
          <el-pagination
            background
            layout="total, prev, pager, next"
            :total="jumpGalleryTotal"
            :page-size="jumpGalleryQuery.pageSize"
            :current-page="jumpGalleryQuery.pageNo"
            @current-change="onJumpGalleryPageChange"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { UploadRawFile, UploadRequestOptions } from "element-plus";
import {
  fetchAdminBannerList,
  fetchAdminGalleryList,
  saveAdminBanner,
  saveAdminGallery,
  uploadAdminContentImage,
  updateAdminBannerStatus,
  updateAdminGalleryStatus,
  type AdminBannerItem,
  type AdminGalleryItem,
} from "../api/adminContent";
import { fetchAdminGoodsList, type AdminGoodsItem } from "../api/adminGoods";
import { resolveStaticUrl } from "../api/http";

const activeTab = ref("banner");

const bannerLoading = ref(false);
const banners = ref<AdminBannerItem[]>([]);
const bannerTotal = ref(0);
const bannerLoaded = ref(false);
const selectedBannerIds = ref<number[]>([]);
const bannerQuery = reactive({
  keyword: "",
  status: "",
  pageNo: 1,
  pageSize: 20,
});
const bannerDialogVisible = ref(false);
const bannerSaving = ref(false);
const bannerUploadLoading = ref(false);
const bannerBatchJumpVisible = ref(false);
const bannerBatchJumpSaving = ref(false);
const jumpPickerVisible = ref(false);
const jumpPickerLoading = ref(false);
const jumpPickerTab = ref<"goods" | "gallery">("goods");
const jumpPickerTarget = ref<"single" | "batch">("single");
const recentGoodsJumpIds = ref<number[]>([]);
const recentGalleryJumpIds = ref<number[]>([]);
const bannerForm = reactive({
  id: undefined as number | undefined,
  title: "",
  imageUrl: "",
  jumpType: "NONE",
  jumpValue: "",
  sortNo: 0,
  status: "ENABLED",
});
const bannerBatchJumpForm = reactive({
  jumpType: "NONE",
  jumpValue: "",
});
const jumpGoodsRows = ref<AdminGoodsItem[]>([]);
const jumpGoodsTotal = ref(0);
const jumpGoodsQuery = reactive({
  keyword: "",
  pageNo: 1,
  pageSize: 10,
});
const jumpGalleryRows = ref<AdminGalleryItem[]>([]);
const jumpGalleryTotal = ref(0);
const jumpGalleryQuery = reactive({
  keyword: "",
  pageNo: 1,
  pageSize: 10,
});

const galleryLoading = ref(false);
const galleryRows = ref<AdminGalleryItem[]>([]);
const galleryTotal = ref(0);
const galleryLoaded = ref(false);
const selectedGalleryIds = ref<number[]>([]);
const galleryQuery = reactive({
  keyword: "",
  displayStatus: "",
  pageNo: 1,
  pageSize: 20,
});
const galleryDialogVisible = ref(false);
const gallerySaving = ref(false);
const galleryUploadLoading = ref(false);
const galleryForm = reactive({
  id: undefined as number | undefined,
  title: "",
  coverImageUrl: "",
  tags: "",
  displayStatus: "ENABLED",
});
const RECENT_JUMP_IDS_KEY = "admin_banner_recent_jump_ids_v1";

const selectedBannerRows = computed(() => {
  if (selectedBannerIds.value.length === 0) {
    return [];
  }
  const idMap: Record<number, boolean> = {};
  selectedBannerIds.value.forEach((id) => {
    idMap[id] = true;
  });
  return banners.value.filter((item) => Boolean(idMap[item.id]));
});

const selectedGalleryRows = computed(() => {
  if (selectedGalleryIds.value.length === 0) {
    return [];
  }
  const idMap: Record<number, boolean> = {};
  selectedGalleryIds.value.forEach((id) => {
    idMap[id] = true;
  });
  return galleryRows.value.filter((item) => Boolean(idMap[item.id]));
});

const bannerSummary = computed(() => {
  const summary = { enabled: 0, disabled: 0 };
  banners.value.forEach((item) => {
    if (item.status === "ENABLED") {
      summary.enabled += 1;
    } else if (item.status === "DISABLED") {
      summary.disabled += 1;
    }
  });
  return summary;
});

const gallerySummary = computed(() => {
  const summary = { enabled: 0, disabled: 0, hidden: 0 };
  galleryRows.value.forEach((item) => {
    if (item.displayStatus === "ENABLED") {
      summary.enabled += 1;
    } else if (item.displayStatus === "DISABLED") {
      summary.disabled += 1;
    } else if (item.displayStatus === "HIDDEN") {
      summary.hidden += 1;
    }
  });
  return summary;
});

function formatTags(tags?: string[]): string {
  if (!tags || tags.length === 0) {
    return "-";
  }
  return tags.join(" / ");
}

function formatDisplayStatus(status?: string): string {
  const map: Record<string, string> = {
    ENABLED: "展示中",
    DISABLED: "隐藏",
    HIDDEN: "已下线",
  };
  if (!status) {
    return "-";
  }
  return map[status] || status;
}

function formatMoney(value?: number): string {
  return Number(value || 0).toFixed(2);
}

function displayStatusTag(status?: string): string {
  if (status === "ENABLED") {
    return "success";
  }
  if (status === "DISABLED") {
    return "warning";
  }
  if (status === "HIDDEN") {
    return "info";
  }
  return "";
}

function resolveImageUrl(url?: string): string {
  return resolveStaticUrl(url);
}

function beforeImageUpload(rawFile: UploadRawFile): boolean {
  const maxSizeMb = 10;
  const allowedTypes = ["image/jpeg", "image/png", "image/webp", "image/gif"];
  if (!allowedTypes.includes(rawFile.type)) {
    ElMessage.warning("仅支持 JPG/PNG/WEBP/GIF 图片");
    return false;
  }
  const sizeMb = rawFile.size / 1024 / 1024;
  if (sizeMb > maxSizeMb) {
    ElMessage.warning(`图片大小不能超过 ${maxSizeMb}MB`);
    return false;
  }
  return true;
}

function loadRecentJumpIds() {
  try {
    const raw = localStorage.getItem(RECENT_JUMP_IDS_KEY);
    if (!raw) {
      recentGoodsJumpIds.value = [];
      recentGalleryJumpIds.value = [];
      return;
    }
    const parsed = JSON.parse(raw);
    const goods = Array.isArray(parsed?.GOODS) ? parsed.GOODS : [];
    const gallery = Array.isArray(parsed?.GALLERY) ? parsed.GALLERY : [];
    recentGoodsJumpIds.value = goods.map((id: any) => Number(id)).filter((id: number) => Number.isFinite(id) && id > 0).slice(0, 8);
    recentGalleryJumpIds.value = gallery.map((id: any) => Number(id)).filter((id: number) => Number.isFinite(id) && id > 0).slice(0, 8);
  } catch (_error) {
    recentGoodsJumpIds.value = [];
    recentGalleryJumpIds.value = [];
  }
}

function saveRecentJumpIds() {
  const payload = {
    GOODS: recentGoodsJumpIds.value.slice(0, 8),
    GALLERY: recentGalleryJumpIds.value.slice(0, 8),
  };
  localStorage.setItem(RECENT_JUMP_IDS_KEY, JSON.stringify(payload));
}

function pushRecentJumpId(jumpType: "GOODS" | "GALLERY", value: number) {
  const id = Number(value || 0);
  if (!Number.isFinite(id) || id <= 0) {
    return;
  }
  const target = jumpType === "GOODS" ? recentGoodsJumpIds : recentGalleryJumpIds;
  const next = target.value.filter((item) => Number(item) !== id);
  next.unshift(id);
  target.value = next.slice(0, 8);
  saveRecentJumpIds();
}

function parseJumpId(value: string): number {
  const text = String(value || "").trim();
  if (!text) {
    return 0;
  }
  const parsed = Number(text);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return 0;
  }
  return Math.floor(parsed);
}

function rememberJumpValue(jumpType: string, jumpValue: string) {
  if (jumpType !== "GOODS" && jumpType !== "GALLERY") {
    return;
  }
  const id = parseJumpId(jumpValue);
  if (id <= 0) {
    return;
  }
  pushRecentJumpId(jumpType, id);
}

function clearRecentJumpIds(jumpType: "GOODS" | "GALLERY") {
  if (jumpType === "GOODS") {
    recentGoodsJumpIds.value = [];
  } else {
    recentGalleryJumpIds.value = [];
  }
  saveRecentJumpIds();
}

async function loadBanners() {
  bannerLoading.value = true;
  try {
    const result = await fetchAdminBannerList({
      keyword: bannerQuery.keyword || undefined,
      status: bannerQuery.status || undefined,
      pageNo: bannerQuery.pageNo,
      pageSize: bannerQuery.pageSize,
    });
    banners.value = result.list || [];
    const idMap: Record<number, boolean> = {};
    banners.value.forEach((item) => {
      idMap[item.id] = true;
    });
    selectedBannerIds.value = selectedBannerIds.value.filter((id) => Boolean(idMap[id]));
    bannerTotal.value = result.page?.total || 0;
    bannerLoaded.value = true;
  } catch (error: any) {
    ElMessage.error(error?.message || "Banner 加载失败");
  } finally {
    bannerLoading.value = false;
  }
}

async function loadGallery() {
  galleryLoading.value = true;
  try {
    const result = await fetchAdminGalleryList({
      keyword: galleryQuery.keyword || undefined,
      displayStatus: galleryQuery.displayStatus || undefined,
      pageNo: galleryQuery.pageNo,
      pageSize: galleryQuery.pageSize,
    });
    galleryRows.value = result.list || [];
    const idMap: Record<number, boolean> = {};
    galleryRows.value.forEach((item) => {
      idMap[item.id] = true;
    });
    selectedGalleryIds.value = selectedGalleryIds.value.filter((id) => Boolean(idMap[id]));
    galleryTotal.value = result.page?.total || 0;
    galleryLoaded.value = true;
  } catch (error: any) {
    ElMessage.error(error?.message || "设计广场数据加载失败");
  } finally {
    galleryLoading.value = false;
  }
}

function searchBanner() {
  bannerQuery.pageNo = 1;
  loadBanners();
}

function resetBanner() {
  bannerQuery.keyword = "";
  bannerQuery.status = "";
  bannerQuery.pageNo = 1;
  bannerQuery.pageSize = 20;
  loadBanners();
}

function onBannerPageChange(pageNo: number) {
  bannerQuery.pageNo = pageNo;
  loadBanners();
}

function onBannerSizeChange(pageSize: number) {
  bannerQuery.pageSize = pageSize;
  bannerQuery.pageNo = 1;
  loadBanners();
}

function onBannerSelectionChange(selection: AdminBannerItem[]) {
  selectedBannerIds.value = (selection || []).map((item) => item.id);
}

function searchGallery() {
  galleryQuery.pageNo = 1;
  loadGallery();
}

function resetGallery() {
  galleryQuery.keyword = "";
  galleryQuery.displayStatus = "";
  galleryQuery.pageNo = 1;
  galleryQuery.pageSize = 20;
  loadGallery();
}

function onGalleryPageChange(pageNo: number) {
  galleryQuery.pageNo = pageNo;
  loadGallery();
}

function onGallerySizeChange(pageSize: number) {
  galleryQuery.pageSize = pageSize;
  galleryQuery.pageNo = 1;
  loadGallery();
}

function onGallerySelectionChange(selection: AdminGalleryItem[]) {
  selectedGalleryIds.value = (selection || []).map((item) => item.id);
}

async function changeBannerStatus(id: number, status: "ENABLED" | "DISABLED") {
  const text = status === "ENABLED" ? "启用" : "禁用";
  try {
    await ElMessageBox.confirm(`确认${text}该 Banner 吗？`, "提示", { type: "warning" });
    await updateAdminBannerStatus(id, status);
    ElMessage.success(`已${text}`);
    await loadBanners();
  } catch (error: any) {
    if (error === "cancel") {
      return;
    }
    ElMessage.error(error?.message || `${text}失败`);
  }
}

async function saveBannerSort(row: AdminBannerItem) {
  try {
    await saveAdminBanner({
      id: row.id,
      title: (row.title || "").trim(),
      imageUrl: (row.imageUrl || "").trim(),
      jumpType: row.jumpType || "NONE",
      jumpValue: (row.jumpValue || "").trim(),
      sortNo: Number(row.sortNo || 0),
      status: row.status || "ENABLED",
    });
    ElMessage.success("排序已保存");
    await loadBanners();
  } catch (error: any) {
    ElMessage.error(error?.message || "排序保存失败");
  }
}

async function batchChangeBannerStatus(status: "ENABLED" | "DISABLED") {
  const targets = selectedBannerRows.value;
  if (targets.length === 0) {
    ElMessage.warning("请先勾选 Banner");
    return;
  }
  const text = status === "ENABLED" ? "启用" : "禁用";
  try {
    await ElMessageBox.confirm(`确认批量${text} ${targets.length} 条 Banner 吗？`, "提示", { type: "warning" });
    const results = await Promise.allSettled(targets.map((item) => updateAdminBannerStatus(item.id, status)));
    const successCount = results.filter((item) => item.status === "fulfilled").length;
    const failCount = results.length - successCount;
    if (successCount > 0 && failCount === 0) {
      ElMessage.success(`批量${text}成功，共 ${successCount} 条`);
    } else if (successCount > 0) {
      ElMessage.warning(`批量${text}完成，成功 ${successCount} 条，失败 ${failCount} 条`);
    } else {
      ElMessage.error(`批量${text}失败`);
    }
    await loadBanners();
  } catch (error: any) {
    if (error === "cancel") {
      return;
    }
    ElMessage.error(error?.message || `批量${text}失败`);
  }
}

function openBatchBannerJumpDialog() {
  const targets = selectedBannerRows.value;
  if (targets.length === 0) {
    ElMessage.warning("请先勾选 Banner");
    return;
  }
  const first = targets[0];
  bannerBatchJumpForm.jumpType = first?.jumpType || "NONE";
  bannerBatchJumpForm.jumpValue = first?.jumpValue || "";
  bannerBatchJumpVisible.value = true;
}

async function submitBatchBannerJump() {
  const targets = selectedBannerRows.value;
  if (targets.length === 0) {
    ElMessage.warning("请先勾选 Banner");
    return;
  }
  const jumpType = bannerBatchJumpForm.jumpType || "NONE";
  const jumpValue = (bannerBatchJumpForm.jumpValue || "").trim();
  bannerBatchJumpSaving.value = true;
  try {
    const results = await Promise.allSettled(
      targets.map((item) =>
        saveAdminBanner({
          id: item.id,
          title: (item.title || "").trim(),
          imageUrl: (item.imageUrl || "").trim(),
          jumpType,
          jumpValue,
          sortNo: Number(item.sortNo || 0),
          status: item.status || "ENABLED",
        })
      )
    );
    const successCount = results.filter((item) => item.status === "fulfilled").length;
    const failCount = results.length - successCount;
    if (successCount > 0 && failCount === 0) {
      ElMessage.success(`批量跳转设置成功，共 ${successCount} 条`);
    } else if (successCount > 0) {
      ElMessage.warning(`批量设置完成，成功 ${successCount} 条，失败 ${failCount} 条`);
    } else {
      ElMessage.error("批量设置失败");
    }
    rememberJumpValue(jumpType, jumpValue);
    bannerBatchJumpVisible.value = false;
    await loadBanners();
  } catch (error: any) {
    ElMessage.error(error?.message || "批量设置失败");
  } finally {
    bannerBatchJumpSaving.value = false;
  }
}

function openJumpPicker(target: "single" | "batch") {
  loadRecentJumpIds();
  jumpPickerTarget.value = target;
  const currentJumpType = target === "single" ? bannerForm.jumpType : bannerBatchJumpForm.jumpType;
  jumpPickerTab.value = currentJumpType === "GALLERY" ? "gallery" : "goods";
  jumpPickerVisible.value = true;
  if (jumpPickerTab.value === "goods" && jumpGoodsRows.value.length === 0) {
    loadJumpGoods();
    return;
  }
  if (jumpPickerTab.value === "gallery" && jumpGalleryRows.value.length === 0) {
    loadJumpGallery();
  }
}

function pickJumpValue(jumpType: "GOODS" | "GALLERY", value: number) {
  const text = String(value || "");
  if (jumpPickerTarget.value === "single") {
    bannerForm.jumpType = jumpType;
    bannerForm.jumpValue = text;
  } else {
    bannerBatchJumpForm.jumpType = jumpType;
    bannerBatchJumpForm.jumpValue = text;
  }
  pushRecentJumpId(jumpType, Number(value || 0));
  jumpPickerVisible.value = false;
}

async function loadJumpGoods() {
  jumpPickerLoading.value = true;
  try {
    const result = await fetchAdminGoodsList({
      keyword: jumpGoodsQuery.keyword || undefined,
      pageNo: jumpGoodsQuery.pageNo,
      pageSize: jumpGoodsQuery.pageSize,
    });
    jumpGoodsRows.value = result.list || [];
    jumpGoodsTotal.value = result.page?.total || 0;
  } catch (error: any) {
    ElMessage.error(error?.message || "商品列表加载失败");
  } finally {
    jumpPickerLoading.value = false;
  }
}

async function loadJumpGallery() {
  jumpPickerLoading.value = true;
  try {
    const result = await fetchAdminGalleryList({
      keyword: jumpGalleryQuery.keyword || undefined,
      displayStatus: "ENABLED",
      pageNo: jumpGalleryQuery.pageNo,
      pageSize: jumpGalleryQuery.pageSize,
    });
    jumpGalleryRows.value = result.list || [];
    jumpGalleryTotal.value = result.page?.total || 0;
  } catch (error: any) {
    ElMessage.error(error?.message || "广场作品加载失败");
  } finally {
    jumpPickerLoading.value = false;
  }
}

function handleJumpPickerTabChange(name: string | number) {
  if (name === "goods" && jumpGoodsRows.value.length === 0) {
    loadJumpGoods();
    return;
  }
  if (name === "gallery" && jumpGalleryRows.value.length === 0) {
    loadJumpGallery();
  }
}

function searchJumpGoods() {
  jumpGoodsQuery.pageNo = 1;
  loadJumpGoods();
}

function onJumpGoodsPageChange(pageNo: number) {
  jumpGoodsQuery.pageNo = pageNo;
  loadJumpGoods();
}

function searchJumpGallery() {
  jumpGalleryQuery.pageNo = 1;
  loadJumpGallery();
}

function onJumpGalleryPageChange(pageNo: number) {
  jumpGalleryQuery.pageNo = pageNo;
  loadJumpGallery();
}

async function changeGalleryStatus(id: number, displayStatus: "ENABLED" | "DISABLED" | "HIDDEN") {
  const text = displayStatus === "ENABLED" ? "展示" : displayStatus === "DISABLED" ? "隐藏" : "下线";
  try {
    await ElMessageBox.confirm(`确认${text}该作品吗？`, "提示", { type: "warning" });
    await updateAdminGalleryStatus(id, displayStatus);
    ElMessage.success(`已${text}`);
    await loadGallery();
  } catch (error: any) {
    if (error === "cancel") {
      return;
    }
    ElMessage.error(error?.message || `${text}失败`);
  }
}

async function batchChangeGalleryStatus(displayStatus: "ENABLED" | "DISABLED" | "HIDDEN") {
  const targets = selectedGalleryRows.value;
  if (targets.length === 0) {
    ElMessage.warning("请先勾选作品");
    return;
  }
  const text = displayStatus === "ENABLED" ? "展示" : displayStatus === "DISABLED" ? "隐藏" : "下线";
  try {
    await ElMessageBox.confirm(`确认批量${text} ${targets.length} 条作品吗？`, "提示", { type: "warning" });
    const results = await Promise.allSettled(targets.map((item) => updateAdminGalleryStatus(item.id, displayStatus)));
    const successCount = results.filter((item) => item.status === "fulfilled").length;
    const failCount = results.length - successCount;
    if (successCount > 0 && failCount === 0) {
      ElMessage.success(`批量${text}成功，共 ${successCount} 条`);
    } else if (successCount > 0) {
      ElMessage.warning(`批量${text}完成，成功 ${successCount} 条，失败 ${failCount} 条`);
    } else {
      ElMessage.error(`批量${text}失败`);
    }
    await loadGallery();
  } catch (error: any) {
    if (error === "cancel") {
      return;
    }
    ElMessage.error(error?.message || `批量${text}失败`);
  }
}

function openBannerDialog(row?: AdminBannerItem) {
  bannerForm.id = row?.id;
  bannerForm.title = row?.title || "";
  bannerForm.imageUrl = row?.imageUrl || "";
  bannerForm.jumpType = row?.jumpType || "NONE";
  bannerForm.jumpValue = row?.jumpValue || "";
  bannerForm.sortNo = Number(row?.sortNo || 0);
  bannerForm.status = row?.status || "ENABLED";
  bannerDialogVisible.value = true;
}

async function submitBanner() {
  if (!bannerForm.title.trim()) {
    ElMessage.warning("Banner 标题不能为空");
    return;
  }
  bannerSaving.value = true;
  try {
    await saveAdminBanner({
      id: bannerForm.id,
      title: bannerForm.title.trim(),
      imageUrl: bannerForm.imageUrl.trim(),
      jumpType: bannerForm.jumpType,
      jumpValue: bannerForm.jumpValue.trim(),
      sortNo: bannerForm.sortNo,
      status: bannerForm.status,
    });
    rememberJumpValue(bannerForm.jumpType, bannerForm.jumpValue);
    ElMessage.success("保存成功");
    bannerDialogVisible.value = false;
    await loadBanners();
  } catch (error: any) {
    ElMessage.error(error?.message || "保存失败");
  } finally {
    bannerSaving.value = false;
  }
}

function openGalleryDialog(row: AdminGalleryItem) {
  galleryForm.id = row.id;
  galleryForm.title = row.title || "";
  galleryForm.coverImageUrl = row.coverImageUrl || "";
  galleryForm.tags = (row.tags || []).join(",");
  galleryForm.displayStatus = row.displayStatus || "ENABLED";
  galleryDialogVisible.value = true;
}

async function submitGallery() {
  if (!galleryForm.id) {
    return;
  }
  gallerySaving.value = true;
  try {
    await saveAdminGallery(galleryForm.id, {
      title: galleryForm.title.trim(),
      coverImageUrl: galleryForm.coverImageUrl.trim(),
      tags: galleryForm.tags.trim(),
      displayStatus: galleryForm.displayStatus,
    });
    ElMessage.success("保存成功");
    galleryDialogVisible.value = false;
    await loadGallery();
  } catch (error: any) {
    ElMessage.error(error?.message || "保存失败");
  } finally {
    gallerySaving.value = false;
  }
}

async function uploadBannerImage(options: UploadRequestOptions) {
  bannerUploadLoading.value = true;
  try {
    const data = await uploadAdminContentImage(options.file as File);
    bannerForm.imageUrl = data.url;
    options.onSuccess(data as any);
    ElMessage.success("图片上传成功");
  } catch (error: any) {
    options.onError(error);
    ElMessage.error(error?.message || "图片上传失败");
  } finally {
    bannerUploadLoading.value = false;
  }
}

async function uploadGalleryImage(options: UploadRequestOptions) {
  galleryUploadLoading.value = true;
  try {
    const data = await uploadAdminContentImage(options.file as File);
    galleryForm.coverImageUrl = data.url;
    options.onSuccess(data as any);
    ElMessage.success("封面上传成功");
  } catch (error: any) {
    options.onError(error);
    ElMessage.error(error?.message || "封面上传失败");
  } finally {
    galleryUploadLoading.value = false;
  }
}

function handleTabChange(name: string | number) {
  if (name === "banner" && !bannerLoaded.value) {
    loadBanners();
    return;
  }
  if (name === "gallery" && !galleryLoaded.value) {
    loadGallery();
  }
}

onMounted(() => {
  loadRecentJumpIds();
  loadBanners();
});
</script>

<style scoped>
.summary-row {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.sort-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.upload-line {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.batch-tip {
  margin-top: 6px;
  color: var(--text-3);
  font-size: 12px;
}

.jump-picker-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.jump-picker-line .el-input {
  flex: 1;
}

.picker-toolbar {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.recent-id-row {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.recent-id-label {
  color: var(--text-3);
  font-size: 12px;
}

.recent-id-tag {
  cursor: pointer;
}
</style>
