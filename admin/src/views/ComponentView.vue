<template>
  <el-card>
    <template #header>
      <div class="header">
        <span>素材库管理</span>
        <el-tag type="info">分类 + SKU</el-tag>
      </div>
    </template>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="分类管理" name="category">
        <div class="toolbar">
          <el-button type="success" @click="openCategoryDialog()">新增分类</el-button>
        </div>

        <el-table :data="categories" border v-loading="categoryLoading">
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="name" label="分类名称" min-width="180" />
          <el-table-column prop="categoryType" label="类型" width="120" />
          <el-table-column prop="sortNo" label="排序" width="90" />
          <el-table-column label="SKU数" width="100">
            <template #default="{ row }">{{ row.skuCount || 0 }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">
                {{ row.status === "ENABLED" ? "启用" : "禁用" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="300" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openCategoryDialog(row)">编辑</el-button>
              <el-button
                v-if="row.status === 'ENABLED'"
                link
                type="danger"
                @click="changeCategoryStatus(row.id, 'DISABLED')"
              >
                禁用
              </el-button>
              <el-button v-else link type="primary" @click="changeCategoryStatus(row.id, 'ENABLED')">启用</el-button>
              <el-button link type="danger" @click="removeCategory(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="SKU 管理" name="sku">
        <div class="toolbar">
          <el-input
            v-model="skuQuery.keyword"
            placeholder="名称/规格/SKU编码"
            clearable
            class="w220"
            @keyup.enter="searchSku"
          />
          <el-select v-model="skuQuery.categoryId" placeholder="全部分类" clearable class="w180">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-select v-model="skuQuery.salesStatus" placeholder="全部状态" clearable class="w160">
            <el-option label="上架中" value="ON_SALE" />
            <el-option label="已下架" value="OFF_SALE" />
          </el-select>
          <el-button type="primary" :loading="skuLoading" @click="searchSku">查询</el-button>
          <el-button @click="resetSku">重置</el-button>
          <el-button type="success" @click="openSkuDialog()">新增 SKU</el-button>
          <el-button
            type="success"
            plain
            :disabled="selectedSkuIds.length === 0"
            @click="batchChangeSkuStatus('ON_SALE')"
          >
            批量上架
          </el-button>
          <el-button
            type="warning"
            plain
            :disabled="selectedSkuIds.length === 0"
            @click="batchChangeSkuStatus('OFF_SALE')"
          >
            批量下架
          </el-button>
        </div>

        <el-table :data="skuRows" border v-loading="skuLoading" row-key="skuId" @selection-change="onSkuSelectionChange">
          <el-table-column type="selection" width="46" />
          <el-table-column prop="skuId" label="SKU ID" width="90" />
          <el-table-column prop="skuCode" label="SKU编码" min-width="140" />
          <el-table-column prop="name" label="名称" min-width="180" />
          <el-table-column label="图片" width="110">
            <template #default="{ row }">
              <el-image
                v-if="row.imageUrl"
                :src="resolveImageUrl(row.imageUrl)"
                fit="cover"
                style="width: 60px; height: 60px; border-radius: 6px"
              />
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="categoryName" label="分类" width="120" />
          <el-table-column prop="specText" label="规格" width="120" />
          <el-table-column label="价格" width="110">
            <template #default="{ row }">￥{{ formatAmount(row.price) }}</template>
          </el-table-column>
          <el-table-column prop="stockQty" label="库存" width="90" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.salesStatus === 'ON_SALE' ? 'success' : 'info'">
                {{ row.salesStatus === "ON_SALE" ? "上架中" : "已下架" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="230" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openSkuDialog(row)">编辑</el-button>
              <el-button
                v-if="row.salesStatus === 'ON_SALE'"
                link
                type="warning"
                @click="changeSkuStatus(row.skuId, 'OFF_SALE')"
              >
                下架
              </el-button>
              <el-button v-else link type="success" @click="changeSkuStatus(row.skuId, 'ON_SALE')">上架</el-button>
              <el-button link type="danger" @click="removeSku(row.skuId)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager">
          <el-pagination
            background
            layout="total, sizes, prev, pager, next"
            :total="skuTotal"
            :page-sizes="[10, 20, 50]"
            :page-size="skuQuery.pageSize"
            :current-page="skuQuery.pageNo"
            @size-change="onSkuSizeChange"
            @current-change="onSkuPageChange"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </el-card>

  <el-dialog v-model="categoryDialogVisible" :title="categoryForm.id ? '编辑分类' : '新增分类'" width="460px">
    <el-form label-position="top">
      <el-form-item label="分类名称">
        <el-input v-model="categoryForm.name" maxlength="64" show-word-limit />
      </el-form-item>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="分类类型">
            <el-select v-model="categoryForm.categoryType" class="full">
              <el-option label="珠子(BEAD)" value="BEAD" />
              <el-option label="隔珠(SPACER)" value="SPACER" />
              <el-option label="吊坠(PENDANT)" value="PENDANT" />
              <el-option label="其他(OTHER)" value="OTHER" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="排序">
            <el-input-number v-model="categoryForm.sortNo" :min="0" :max="999999" class="full" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="状态">
        <el-select v-model="categoryForm.status" class="full">
          <el-option label="启用" value="ENABLED" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="categoryDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="categorySaving" @click="submitCategory">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="skuDialogVisible" :title="skuForm.skuId ? '编辑 SKU' : '新增 SKU'" width="560px">
    <el-form label-position="top">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="SKU 编码">
            <el-input v-model="skuForm.skuCode" maxlength="64" show-word-limit placeholder="留空自动生成" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="分类">
            <el-select v-model="skuForm.categoryId" class="full">
              <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="名称">
        <el-input v-model="skuForm.name" maxlength="128" show-word-limit />
      </el-form-item>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="规格">
            <el-input v-model="skuForm.specText" maxlength="64" show-word-limit />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="珠径(mm)">
            <el-input-number v-model="skuForm.beadDiameterMm" :min="0" :precision="2" :step="0.5" class="full" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="售价">
            <el-input-number v-model="skuForm.price" :min="0" :precision="2" :step="0.1" class="full" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="库存">
            <el-input-number v-model="skuForm.stockQty" :min="0" :max="999999" class="full" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="预警库存">
            <el-input-number v-model="skuForm.stockWarnQty" :min="0" :max="999999" class="full" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="图片 URL">
        <el-input v-model="skuForm.imageUrl" maxlength="255" show-word-limit />
        <div class="upload-line">
          <el-upload
            :show-file-list="false"
            :http-request="uploadSkuImage"
            :before-upload="beforeImageUpload"
            accept=".jpg,.jpeg,.png,.webp,.gif"
          >
            <el-button size="small" :loading="skuUploadLoading" :disabled="skuUploadLoading">上传图片</el-button>
          </el-upload>
          <el-image
            v-if="skuForm.imageUrl"
            :src="resolveImageUrl(skuForm.imageUrl)"
            fit="cover"
            style="width: 52px; height: 52px; border-radius: 6px"
          />
        </div>
      </el-form-item>
      <el-form-item label="销售状态">
        <el-select v-model="skuForm.salesStatus" class="full">
          <el-option label="上架中" value="ON_SALE" />
          <el-option label="已下架" value="OFF_SALE" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="skuDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="skuSaving" @click="submitSku">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { UploadRawFile, UploadRequestOptions } from "element-plus";
import {
  batchUpdateAdminComponentSkuStatus,
  createAdminComponentSku,
  deleteAdminComponentCategory,
  deleteAdminComponentSku,
  fetchAdminComponentCategories,
  fetchAdminComponentSkuList,
  saveAdminComponentCategory,
  saveAdminComponentSku,
  updateAdminComponentCategoryStatus,
  updateAdminComponentSkuStatus,
  type AdminComponentCategoryItem,
  type AdminComponentSkuItem,
} from "../api/adminComponent";
import { uploadAdminContentImage } from "../api/adminContent";
import { resolveStaticUrl } from "../api/http";

const activeTab = ref("category");

const categoryLoading = ref(false);
const categories = ref<AdminComponentCategoryItem[]>([]);
const categoryDialogVisible = ref(false);
const categorySaving = ref(false);
const categoryForm = reactive({
  id: undefined as number | undefined,
  name: "",
  categoryType: "BEAD",
  sortNo: 0,
  status: "ENABLED",
});

const skuLoading = ref(false);
const skuRows = ref<AdminComponentSkuItem[]>([]);
const skuTotal = ref(0);
const selectedSkuIds = ref<number[]>([]);
const skuDialogVisible = ref(false);
const skuSaving = ref(false);
const skuUploadLoading = ref(false);
const skuQuery = reactive({
  keyword: "",
  categoryId: undefined as number | undefined,
  salesStatus: "",
  pageNo: 1,
  pageSize: 20,
});
const skuForm = reactive({
  skuId: undefined as number | undefined,
  skuCode: "",
  categoryId: undefined as number | undefined,
  name: "",
  specText: "",
  beadDiameterMm: 0,
  price: 0,
  stockQty: 0,
  stockWarnQty: 0,
  imageUrl: "",
  salesStatus: "ON_SALE",
});

function formatAmount(value?: number): string {
  return Number(value || 0).toFixed(2);
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

function resetCategoryForm() {
  categoryForm.id = undefined;
  categoryForm.name = "";
  categoryForm.categoryType = "BEAD";
  categoryForm.sortNo = 0;
  categoryForm.status = "ENABLED";
}

function resetSkuForm() {
  skuForm.skuId = undefined;
  skuForm.skuCode = "";
  skuForm.categoryId = undefined;
  skuForm.name = "";
  skuForm.specText = "";
  skuForm.beadDiameterMm = 0;
  skuForm.price = 0;
  skuForm.stockQty = 0;
  skuForm.stockWarnQty = 0;
  skuForm.imageUrl = "";
  skuForm.salesStatus = "ON_SALE";
}

async function loadCategories() {
  categoryLoading.value = true;
  try {
    categories.value = await fetchAdminComponentCategories(true);
  } catch (error: any) {
    ElMessage.error(error?.message || "分类加载失败");
  } finally {
    categoryLoading.value = false;
  }
}

async function loadSkus() {
  skuLoading.value = true;
  try {
    const data = await fetchAdminComponentSkuList({
      keyword: skuQuery.keyword || undefined,
      categoryId: skuQuery.categoryId,
      salesStatus: skuQuery.salesStatus || undefined,
      pageNo: skuQuery.pageNo,
      pageSize: skuQuery.pageSize,
    });
    skuRows.value = data.list || [];
    const validIdMap: Record<number, boolean> = {};
    skuRows.value.forEach((item) => {
      validIdMap[item.skuId] = true;
    });
    selectedSkuIds.value = selectedSkuIds.value.filter((id) => Boolean(validIdMap[id]));
    skuTotal.value = data.page?.total || 0;
  } catch (error: any) {
    ElMessage.error(error?.message || "SKU 加载失败");
  } finally {
    skuLoading.value = false;
  }
}

function openCategoryDialog(row?: AdminComponentCategoryItem) {
  resetCategoryForm();
  if (row) {
    categoryForm.id = row.id;
    categoryForm.name = row.name || "";
    categoryForm.categoryType = row.categoryType || "BEAD";
    categoryForm.sortNo = Number(row.sortNo || 0);
    categoryForm.status = row.status || "ENABLED";
  }
  categoryDialogVisible.value = true;
}

async function submitCategory() {
  if (!categoryForm.name.trim()) {
    ElMessage.warning("分类名称不能为空");
    return;
  }
  categorySaving.value = true;
  try {
    await saveAdminComponentCategory({
      id: categoryForm.id,
      name: categoryForm.name.trim(),
      categoryType: categoryForm.categoryType,
      sortNo: Number(categoryForm.sortNo || 0),
      status: categoryForm.status,
    });
    ElMessage.success("保存成功");
    categoryDialogVisible.value = false;
    await loadCategories();
  } catch (error: any) {
    ElMessage.error(error?.message || "保存失败");
  } finally {
    categorySaving.value = false;
  }
}

async function changeCategoryStatus(id: number, status: "ENABLED" | "DISABLED") {
  const text = status === "ENABLED" ? "启用" : "禁用";
  try {
    await ElMessageBox.confirm(`确认${text}该分类吗？`, "提示", { type: "warning" });
    await updateAdminComponentCategoryStatus(id, status);
    ElMessage.success(`已${text}`);
    await loadCategories();
  } catch (error: any) {
    if (error === "cancel") {
      return;
    }
    ElMessage.error(error?.message || `${text}失败`);
  }
}

async function removeCategory(row: AdminComponentCategoryItem) {
  try {
    await ElMessageBox.confirm(
      `确认删除分类「${row.name}」吗？有子分类或SKU时会被拒绝。`,
      "提示",
      { type: "warning" }
    );
    await deleteAdminComponentCategory(row.id);
    if (skuQuery.categoryId === row.id) {
      skuQuery.categoryId = undefined;
    }
    ElMessage.success("删除成功");
    await loadCategories();
    await loadSkus();
  } catch (error: any) {
    if (error === "cancel") {
      return;
    }
    ElMessage.error(error?.message || "删除失败");
  }
}

function openSkuDialog(row?: AdminComponentSkuItem) {
  resetSkuForm();
  if (row) {
    skuForm.skuId = row.skuId;
    skuForm.skuCode = row.skuCode || "";
    skuForm.categoryId = row.categoryId;
    skuForm.name = row.name || "";
    skuForm.specText = row.specText || "";
    skuForm.beadDiameterMm = Number(row.beadDiameterMm || 0);
    skuForm.price = Number(row.price || 0);
    skuForm.stockQty = Number(row.stockQty || 0);
    skuForm.stockWarnQty = Number(row.stockWarnQty || 0);
    skuForm.imageUrl = row.imageUrl || "";
    skuForm.salesStatus = row.salesStatus || "ON_SALE";
  } else if (categories.value.length > 0) {
    skuForm.categoryId = categories.value[0].id;
  }
  skuDialogVisible.value = true;
}

async function submitSku() {
  if (!skuForm.categoryId) {
    ElMessage.warning("请选择分类");
    return;
  }
  if (!skuForm.name.trim()) {
    ElMessage.warning("名称不能为空");
    return;
  }
  skuSaving.value = true;
  try {
    const payload = {
      skuCode: skuForm.skuCode.trim() || undefined,
      categoryId: skuForm.categoryId,
      name: skuForm.name.trim(),
      specText: skuForm.specText.trim(),
      beadDiameterMm: Number(skuForm.beadDiameterMm || 0),
      price: Number(skuForm.price || 0),
      stockQty: Number(skuForm.stockQty || 0),
      stockWarnQty: Number(skuForm.stockWarnQty || 0),
      imageUrl: skuForm.imageUrl.trim(),
      salesStatus: skuForm.salesStatus,
    };
    if (skuForm.skuId) {
      await saveAdminComponentSku(skuForm.skuId, payload);
    } else {
      await createAdminComponentSku(payload as any);
    }
    ElMessage.success("SKU 保存成功");
    skuDialogVisible.value = false;
    await loadSkus();
    await loadCategories();
  } catch (error: any) {
    ElMessage.error(error?.message || "SKU 保存失败");
  } finally {
    skuSaving.value = false;
  }
}

async function changeSkuStatus(skuId: number, status: "ON_SALE" | "OFF_SALE") {
  const text = status === "ON_SALE" ? "上架" : "下架";
  try {
    await ElMessageBox.confirm(`确认${text}该 SKU 吗？`, "提示", { type: "warning" });
    await updateAdminComponentSkuStatus(skuId, status);
    ElMessage.success(`已${text}`);
    await loadSkus();
  } catch (error: any) {
    if (error === "cancel") {
      return;
    }
    ElMessage.error(error?.message || `${text}失败`);
  }
}

function onSkuSelectionChange(rows: AdminComponentSkuItem[]) {
  selectedSkuIds.value = (rows || []).map((item) => item.skuId);
}

async function batchChangeSkuStatus(status: "ON_SALE" | "OFF_SALE") {
  if (selectedSkuIds.value.length === 0) {
    ElMessage.warning("请先勾选 SKU");
    return;
  }
  const text = status === "ON_SALE" ? "上架" : "下架";
  try {
    await ElMessageBox.confirm(`确认批量${text}已勾选的 ${selectedSkuIds.value.length} 个 SKU 吗？`, "提示", {
      type: "warning",
    });
    const result = await batchUpdateAdminComponentSkuStatus({
      skuIds: selectedSkuIds.value,
      salesStatus: status,
    });
    ElMessage.success(`已${text}，更新 ${result.updated} 条`);
    await loadSkus();
  } catch (error: any) {
    if (error === "cancel") {
      return;
    }
    ElMessage.error(error?.message || `批量${text}失败`);
  }
}

async function removeSku(skuId: number) {
  try {
    await ElMessageBox.confirm("确认删除该 SKU 吗？", "提示", { type: "warning" });
    await deleteAdminComponentSku(skuId);
    ElMessage.success("删除成功");
    await loadSkus();
    await loadCategories();
  } catch (error: any) {
    if (error === "cancel") {
      return;
    }
    ElMessage.error(error?.message || "删除失败");
  }
}

async function uploadSkuImage(options: UploadRequestOptions) {
  skuUploadLoading.value = true;
  try {
    const data = await uploadAdminContentImage(options.file as File);
    skuForm.imageUrl = data.url;
    options.onSuccess(data as any);
    ElMessage.success("图片上传成功");
  } catch (error: any) {
    options.onError(error);
    ElMessage.error(error?.message || "图片上传失败");
  } finally {
    skuUploadLoading.value = false;
  }
}

function searchSku() {
  skuQuery.pageNo = 1;
  loadSkus();
}

function resetSku() {
  skuQuery.keyword = "";
  skuQuery.categoryId = undefined;
  skuQuery.salesStatus = "";
  skuQuery.pageNo = 1;
  skuQuery.pageSize = 20;
  loadSkus();
}

function onSkuPageChange(pageNo: number) {
  skuQuery.pageNo = pageNo;
  loadSkus();
}

function onSkuSizeChange(pageSize: number) {
  skuQuery.pageSize = pageSize;
  skuQuery.pageNo = 1;
  loadSkus();
}

onMounted(async () => {
  await loadCategories();
  await loadSkus();
});
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.w220 {
  width: 220px;
}

.w180 {
  width: 180px;
}

.w160 {
  width: 160px;
}

.full {
  width: 100%;
}

.upload-line {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
