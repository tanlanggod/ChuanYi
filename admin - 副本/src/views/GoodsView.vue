<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <div class="page-title"><el-icon><Goods /></el-icon> <span>好物管理</span></div>
        <div class="header-actions">
          <el-input
            v-model="query.keyword"
            placeholder="商品标题/副标题"
            clearable
            class="w220"
            @keyup.enter="handleSearch"
          />
          <el-select v-model="query.categoryId" placeholder="全部分类" clearable class="w180">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-select v-model="query.salesStatus" placeholder="全部状态" clearable class="w160">
            <el-option label="上架中" value="ON_SALE" />
            <el-option label="已下架" value="OFF_SALE" />
            <el-option label="已售罄" value="SOLD_OUT" />
          </el-select>
          <el-button type="primary" :loading="loading" @click="handleSearch" :icon="'Search'">查询</el-button>
          <el-button @click="handleReset" :icon="'Refresh'">重置</el-button>
        </div>
      </div>
    </template>

    <el-table :data="rows" border v-loading="loading">
      <el-table-column prop="spuId" label="SPU ID" width="110" />
      <el-table-column label="商品信息" min-width="280">
        <template #default="{ row }">
          <div class="main-line">{{ row.title }}</div>
          <div class="sub-line">{{ row.subtitle || "-" }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类" width="140" />
      <el-table-column label="价格区间" width="150">
        <template #default="{ row }">￥{{ formatAmount(row.minPrice) }} ~ ￥{{ formatAmount(row.maxPrice) }}</template>
      </el-table-column>
      <el-table-column label="销售状态" width="120">
        <template #default="{ row }">
          <el-tag :type="salesStatusTagType(row.salesStatus)">{{ formatSalesStatus(row.salesStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="库存状态" width="120">
        <template #default="{ row }">{{ formatStockStatus(row.stockStatus) }}</template>
      </el-table-column>
      <el-table-column label="SKU统计" width="120">
        <template #default="{ row }">{{ row.onSaleSkuCount || 0 }}/{{ row.skuCount || 0 }}</template>
      </el-table-column>
      <el-table-column prop="totalStock" label="总库存" width="100" />
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button
            v-if="row.salesStatus === 'ON_SALE'"
            link
            type="danger"
            @click="toggleSalesStatus(row, 'OFF_SALE')"
          >
            下架
          </el-button>
          <el-button
            v-else
            link
            type="primary"
            @click="toggleSalesStatus(row, 'ON_SALE')"
          >
            上架
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-sizes="[10, 20, 50]"
        :page-size="query.pageSize"
        :current-page="query.pageNo"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </el-card>

  <el-drawer v-model="editVisible" title="编辑商品" size="760px" :destroy-on-close="true">
    <div v-loading="editLoading">
      <el-form label-position="top" class="edit-form">
        <el-form-item label="商品标题">
          <el-input v-model="editForm.title" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="商品副标题">
          <el-input v-model="editForm.subtitle" maxlength="255" show-word-limit />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="分类">
              <el-select v-model="editForm.categoryId" class="full">
                <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="editForm.sortNo" :min="0" :max="999999" class="full" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="最低价">
              <el-input-number v-model="editForm.minPrice" :min="0" :precision="2" :step="0.1" class="full" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最高价">
              <el-input-number v-model="editForm.maxPrice" :min="0" :precision="2" :step="0.1" class="full" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="销售状态">
              <el-select v-model="editForm.salesStatus" class="full">
                <el-option label="上架中" value="ON_SALE" />
                <el-option label="已下架" value="OFF_SALE" />
                <el-option label="已售罄" value="SOLD_OUT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库存状态">
              <el-select v-model="editForm.stockStatus" class="full">
                <el-option label="库存充足" value="HAS_STOCK" />
                <el-option label="库存紧张" value="LOW_STOCK" />
                <el-option label="无库存" value="OUT_OF_STOCK" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="封面图 URL">
          <el-input v-model="editForm.coverImageUrl" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="标签（逗号分隔）">
          <el-input v-model="editForm.tags" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>

      <div class="sku-head">
        <h4 class="section-title">SKU 列表</h4>
        <el-button type="success" size="small" @click="openCreateSku">新增 SKU</el-button>
      </div>
      <el-table :data="editSkus" border>
        <el-table-column prop="skuId" label="SKU ID" width="90" />
        <el-table-column prop="specText" label="规格" min-width="140" />
        <el-table-column label="价格" width="120">
          <template #default="{ row }">￥{{ formatAmount(row.price) }}</template>
        </el-table-column>
        <el-table-column prop="stockQty" label="库存" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">{{ formatSalesStatus(row.salesStatus) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditSku(row)">编辑</el-button>
            <el-button
              v-if="row.salesStatus === 'ON_SALE'"
              link
              type="warning"
              @click="toggleSkuStatus(row, 'OFF_SALE')"
            >
              下架
            </el-button>
            <el-button
              v-else
              link
              type="success"
              @click="toggleSkuStatus(row, 'ON_SALE')"
            >
              上架
            </el-button>
            <el-button link type="danger" @click="removeSku(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="drawer-footer">
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="submitEdit">保存商品</el-button>
      </div>
    </div>
  </el-drawer>

  <el-dialog v-model="skuDialogVisible" :title="skuMode === 'create' ? '新增 SKU' : '编辑 SKU'" width="520px">
    <el-form label-position="top">
      <el-form-item label="规格文案">
        <el-input v-model="skuForm.specText" maxlength="128" show-word-limit />
      </el-form-item>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="售价">
            <el-input-number v-model="skuForm.price" :min="0" :precision="2" :step="0.1" class="full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="原价">
            <el-input-number v-model="skuForm.originPrice" :min="0" :precision="2" :step="0.1" class="full" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="库存数量">
            <el-input-number v-model="skuForm.stockQty" :min="0" :max="999999" class="full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="销售状态">
            <el-select v-model="skuForm.salesStatus" class="full">
              <el-option label="上架中" value="ON_SALE" />
              <el-option label="已下架" value="OFF_SALE" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="SKU 图片 URL">
        <el-input v-model="skuForm.skuImageUrl" maxlength="255" show-word-limit />
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
import {
  createAdminGoodsSku,
  deleteAdminGoodsSku,
  fetchAdminGoodsCategories,
  fetchAdminGoodsDetail,
  fetchAdminGoodsList,
  saveAdminGoods,
  saveAdminGoodsSku,
  updateAdminGoodsSkuStatus,
  updateAdminGoodsStatus,
  type AdminGoodsCategory,
  type AdminGoodsItem,
  type AdminGoodsSkuItem,
} from "../api/adminGoods";

const loading = ref(false);
const rows = ref<AdminGoodsItem[]>([]);
const total = ref(0);
const categories = ref<AdminGoodsCategory[]>([]);

const editVisible = ref(false);
const editLoading = ref(false);
const editSaving = ref(false);
const editingSpuId = ref<number | null>(null);
const editSkus = ref<AdminGoodsSkuItem[]>([]);
const editForm = reactive({
  title: "",
  subtitle: "",
  categoryId: undefined as number | undefined,
  sortNo: 0,
  minPrice: 0,
  maxPrice: 0,
  salesStatus: "ON_SALE",
  stockStatus: "HAS_STOCK",
  coverImageUrl: "",
  tags: "",
});

const skuDialogVisible = ref(false);
const skuSaving = ref(false);
const skuMode = ref<"create" | "edit">("create");
const editingSkuId = ref<number | null>(null);
const skuForm = reactive({
  specText: "",
  price: 0,
  originPrice: 0,
  stockQty: 0,
  salesStatus: "ON_SALE",
  skuImageUrl: "",
});

const query = reactive({
  keyword: "",
  categoryId: undefined as number | undefined,
  salesStatus: "",
  pageNo: 1,
  pageSize: 20,
});

function formatAmount(amount: number | undefined): string {
  return Number(amount || 0).toFixed(2);
}

function formatSalesStatus(status?: string): string {
  const map: Record<string, string> = {
    ON_SALE: "上架中",
    OFF_SALE: "已下架",
    SOLD_OUT: "已售罄",
  };
  if (!status) {
    return "-";
  }
  return map[status] || status;
}

function formatStockStatus(status?: string): string {
  const map: Record<string, string> = {
    HAS_STOCK: "库存充足",
    LOW_STOCK: "库存紧张",
    OUT_OF_STOCK: "无库存",
  };
  if (!status) {
    return "-";
  }
  return map[status] || status;
}

function salesStatusTagType(status?: string): string {
  if (status === "ON_SALE") {
    return "success";
  }
  if (status === "OFF_SALE") {
    return "info";
  }
  if (status === "SOLD_OUT") {
    return "warning";
  }
  return "";
}

async function loadCategories() {
  try {
    categories.value = await fetchAdminGoodsCategories();
  } catch (error: any) {
    ElMessage.error(error?.message || "分类加载失败");
  }
}

async function loadGoods() {
  loading.value = true;
  try {
    const result = await fetchAdminGoodsList({
      categoryId: query.categoryId,
      keyword: query.keyword || undefined,
      salesStatus: query.salesStatus || undefined,
      pageNo: query.pageNo,
      pageSize: query.pageSize,
    });
    rows.value = result.list || [];
    total.value = result.page?.total || 0;
  } catch (error: any) {
    ElMessage.error(error?.message || "商品加载失败");
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNo = 1;
  loadGoods();
}

function handleReset() {
  query.keyword = "";
  query.categoryId = undefined;
  query.salesStatus = "";
  query.pageNo = 1;
  query.pageSize = 20;
  loadGoods();
}

function handlePageChange(pageNo: number) {
  query.pageNo = pageNo;
  loadGoods();
}

function handleSizeChange(pageSize: number) {
  query.pageSize = pageSize;
  query.pageNo = 1;
  loadGoods();
}

async function toggleSalesStatus(row: AdminGoodsItem, salesStatus: "ON_SALE" | "OFF_SALE") {
  const text = salesStatus === "ON_SALE" ? "上架" : "下架";
  try {
    await ElMessageBox.confirm(`确认${text}该商品吗？`, "提示", { type: "warning" });
    await updateAdminGoodsStatus(row.spuId, salesStatus);
    ElMessage.success(`已${text}`);
    await loadGoods();
  } catch (error: any) {
    if (error === "cancel") {
      return;
    }
    ElMessage.error(error?.message || `${text}失败`);
  }
}

function resetEditForm() {
  editForm.title = "";
  editForm.subtitle = "";
  editForm.categoryId = undefined;
  editForm.sortNo = 0;
  editForm.minPrice = 0;
  editForm.maxPrice = 0;
  editForm.salesStatus = "ON_SALE";
  editForm.stockStatus = "HAS_STOCK";
  editForm.coverImageUrl = "";
  editForm.tags = "";
}

function resetSkuForm() {
  skuForm.specText = "";
  skuForm.price = 0;
  skuForm.originPrice = 0;
  skuForm.stockQty = 0;
  skuForm.salesStatus = "ON_SALE";
  skuForm.skuImageUrl = "";
}

async function reloadCurrentDetail() {
  if (!editingSpuId.value) {
    return;
  }
  const result = await fetchAdminGoodsDetail(editingSpuId.value);
  const spu = result.spu;
  editForm.title = spu.title || "";
  editForm.subtitle = spu.subtitle || "";
  editForm.categoryId = spu.categoryId;
  editForm.sortNo = Number(spu.sortNo || 0);
  editForm.minPrice = Number(spu.minPrice || 0);
  editForm.maxPrice = Number(spu.maxPrice || 0);
  editForm.salesStatus = spu.salesStatus || "ON_SALE";
  editForm.stockStatus = spu.stockStatus || "HAS_STOCK";
  editForm.coverImageUrl = spu.coverImageUrl || "";
  editForm.tags = (spu.tags || []).join(",");
  editSkus.value = result.skus || [];
}

async function openEdit(row: AdminGoodsItem) {
  editingSpuId.value = row.spuId;
  resetEditForm();
  editSkus.value = [];
  editVisible.value = true;
  editLoading.value = true;
  try {
    await reloadCurrentDetail();
  } catch (error: any) {
    ElMessage.error(error?.message || "商品详情加载失败");
  } finally {
    editLoading.value = false;
  }
}

async function submitEdit() {
  if (!editingSpuId.value) {
    return;
  }
  if (!editForm.title.trim()) {
    ElMessage.warning("商品标题不能为空");
    return;
  }
  editSaving.value = true;
  try {
    await saveAdminGoods(editingSpuId.value, {
      title: editForm.title.trim(),
      subtitle: editForm.subtitle.trim(),
      categoryId: editForm.categoryId,
      sortNo: editForm.sortNo,
      minPrice: editForm.minPrice,
      maxPrice: editForm.maxPrice,
      salesStatus: editForm.salesStatus,
      stockStatus: editForm.stockStatus,
      coverImageUrl: editForm.coverImageUrl.trim(),
      tags: editForm.tags.trim(),
    });
    ElMessage.success("保存成功");
    editVisible.value = false;
    await loadGoods();
  } catch (error: any) {
    ElMessage.error(error?.message || "保存失败");
  } finally {
    editSaving.value = false;
  }
}

function openCreateSku() {
  if (!editingSpuId.value) {
    return;
  }
  skuMode.value = "create";
  editingSkuId.value = null;
  resetSkuForm();
  skuDialogVisible.value = true;
}

function openEditSku(row: AdminGoodsSkuItem) {
  skuMode.value = "edit";
  editingSkuId.value = row.skuId;
  skuForm.specText = row.specText || "";
  skuForm.price = Number(row.price || 0);
  skuForm.originPrice = Number(row.originPrice || 0);
  skuForm.stockQty = Number(row.stockQty || 0);
  skuForm.salesStatus = row.salesStatus || "ON_SALE";
  skuForm.skuImageUrl = row.skuImageUrl || "";
  skuDialogVisible.value = true;
}

async function submitSku() {
  if (!editingSpuId.value) {
    return;
  }
  if (!skuForm.specText.trim()) {
    ElMessage.warning("规格文案不能为空");
    return;
  }
  skuSaving.value = true;
  try {
    if (skuMode.value === "create") {
      await createAdminGoodsSku(editingSpuId.value, {
        specText: skuForm.specText.trim(),
        price: Number(skuForm.price || 0),
        originPrice: Number(skuForm.originPrice || 0),
        stockQty: Number(skuForm.stockQty || 0),
        salesStatus: skuForm.salesStatus,
        skuImageUrl: skuForm.skuImageUrl.trim(),
      });
    } else if (editingSkuId.value) {
      await saveAdminGoodsSku(editingSkuId.value, {
        specText: skuForm.specText.trim(),
        price: Number(skuForm.price || 0),
        originPrice: Number(skuForm.originPrice || 0),
        stockQty: Number(skuForm.stockQty || 0),
        salesStatus: skuForm.salesStatus,
        skuImageUrl: skuForm.skuImageUrl.trim(),
      });
    }
    ElMessage.success("SKU 保存成功");
    skuDialogVisible.value = false;
    await reloadCurrentDetail();
    await loadGoods();
  } catch (error: any) {
    ElMessage.error(error?.message || "SKU 保存失败");
  } finally {
    skuSaving.value = false;
  }
}

async function toggleSkuStatus(row: AdminGoodsSkuItem, salesStatus: "ON_SALE" | "OFF_SALE") {
  const text = salesStatus === "ON_SALE" ? "上架" : "下架";
  try {
    await ElMessageBox.confirm(`确认${text}该 SKU 吗？`, "提示", { type: "warning" });
    await updateAdminGoodsSkuStatus(row.skuId, salesStatus);
    ElMessage.success(`SKU 已${text}`);
    await reloadCurrentDetail();
    await loadGoods();
  } catch (error: any) {
    if (error === "cancel") {
      return;
    }
    ElMessage.error(error?.message || `${text}失败`);
  }
}

async function removeSku(row: AdminGoodsSkuItem) {
  try {
    await ElMessageBox.confirm("确认删除该 SKU 吗？", "提示", { type: "warning" });
    await deleteAdminGoodsSku(row.skuId);
    ElMessage.success("SKU 已删除");
    await reloadCurrentDetail();
    await loadGoods();
  } catch (error: any) {
    if (error === "cancel") {
      return;
    }
    ElMessage.error(error?.message || "删除失败");
  }
}

onMounted(async () => {
  await loadCategories();
  await loadGoods();
});
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
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

.main-line {
  font-weight: 600;
}

.sub-line {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.edit-form {
  margin-bottom: 16px;
}

.full {
  width: 100%;
}

.sku-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 6px 0 10px;
}

.section-title {
  margin: 0;
}

.drawer-footer {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
