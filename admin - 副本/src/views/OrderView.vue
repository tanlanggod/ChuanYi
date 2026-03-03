<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <div class="page-title"><el-icon><Tickets /></el-icon> <span>订单管理</span></div>
        <div class="header-actions">
          <el-input
            v-model="query.keyword"
            placeholder="订单号 / 备注"
            clearable
            class="w220 custom-input"
            :prefix-icon="'Search'"
            @keyup.enter="handleSearch"
          />
          <el-select v-model="query.status" placeholder="全部状态" clearable class="w180 custom-select">
            <el-option label="待支付" value="PENDING_PAY" />
            <el-option label="待发货" value="PAID_WAIT_SHIP" />
            <el-option label="待收货" value="SHIPPED_WAIT_RECEIVE" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
          <el-button type="primary" :loading="loading" @click="handleSearch" :icon="'Search'">查询</el-button>
          <el-button @click="handleReset" :icon="'Refresh'">重置</el-button>
        </div>
      </div>
    </template>

    <div class="table-tools">
      <div class="summary-group">
        <el-tag type="warning" effect="light" class="summary-tag"><el-icon><Wallet /></el-icon>待支付 {{ statusSummary.pendingPay }}</el-tag>
        <el-tag type="primary" effect="light" class="summary-tag"><el-icon><Box /></el-icon>待发货 {{ statusSummary.paidWaitShip }}</el-tag>
        <el-tag type="success" effect="light" class="summary-tag"><el-icon><Van /></el-icon>待收货 {{ statusSummary.shippedWaitReceive }}</el-tag>
        <el-tag effect="light" class="summary-tag"><el-icon><CircleCheck /></el-icon>已完成 {{ statusSummary.completed }}</el-tag>
        <el-tag type="info" effect="light" class="summary-tag"><el-icon><CircleClose /></el-icon>已取消 {{ statusSummary.cancelled }}</el-tag>
      </div>
      <div class="batch-actions">
        <el-button
          type="danger"
          plain
          :disabled="batchCancelableCount === 0"
          @click="batchCancel"
        >
          批量取消（{{ batchCancelableCount }}）
        </el-button>
        <el-button
          type="success"
          plain
          :disabled="batchCompletableCount === 0"
          @click="batchComplete"
        >
          批量完成（{{ batchCompletableCount }}）
        </el-button>
      </div>
    </div>

    <el-table :data="rows" border v-loading="loading" row-key="orderNo" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="46" />
      <el-table-column prop="orderNo" label="订单号" min-width="190" />
      <el-table-column prop="userId" label="用户ID" width="90" />
      <el-table-column label="订单状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.orderStatus)">{{ formatOrderStatus(row.orderStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="支付状态" width="110">
        <template #default="{ row }">{{ formatPayStatus(row.payStatus) }}</template>
      </el-table-column>
      <el-table-column label="实付金额" width="120">
        <template #default="{ row }">￥{{ formatAmount(row.payAmount) }}</template>
      </el-table-column>
      <el-table-column label="物流信息" min-width="180">
        <template #default="{ row }">
          <span v-if="row.shipping && row.shipping.trackingNo">
            {{ row.shipping.carrierCode }} / {{ row.shipping.trackingNo }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="下单时间" min-width="170" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.orderNo)">详情</el-button>
          <el-button
            v-if="row.orderStatus === 'PAID_WAIT_SHIP'"
            link
            type="primary"
            @click="openShip(row.orderNo)"
          >
            发货
          </el-button>
          <el-button
            v-if="row.orderStatus === 'PENDING_PAY'"
            link
            type="danger"
            @click="handleCancel(row.orderNo)"
          >
            取消
          </el-button>
          <el-button
            v-if="row.orderStatus === 'SHIPPED_WAIT_RECEIVE'"
            link
            type="success"
            @click="handleComplete(row.orderNo)"
          >
            完成
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

  <el-drawer v-model="detailVisible" title="订单详情" size="760px" :destroy-on-close="true">
    <div v-loading="detailLoading">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ detail.userId }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">{{ formatOrderStatus(detail.orderStatus) }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">{{ formatPayStatus(detail.payStatus) }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">￥{{ formatAmount(detail.payAmount) }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ detail.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">{{ renderAddress(detail.addressSnapshot) }}</el-descriptions-item>
          <el-descriptions-item label="物流信息" :span="2">{{ renderShipping(detail.shipping) }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="section-title">商品明细</h4>
        <el-table :data="detail.items" border>
          <el-table-column prop="itemName" label="商品名称" min-width="180" />
          <el-table-column label="类型" width="130">
            <template #default="{ row }">{{ formatItemType(row.itemType) }}</template>
          </el-table-column>
          <el-table-column prop="qty" label="数量" width="80" />
          <el-table-column label="单价" width="120">
            <template #default="{ row }">￥{{ formatAmount(row.unitPrice) }}</template>
          </el-table-column>
          <el-table-column label="金额" width="120">
            <template #default="{ row }">￥{{ formatAmount(row.amount) }}</template>
          </el-table-column>
        </el-table>

        <h4 class="section-title">操作日志</h4>
        <el-timeline>
          <el-timeline-item
            v-for="(log, index) in detail.logs"
            :key="index"
            :timestamp="log.createdAt"
            placement="top"
          >
            <div class="log-line">
              <strong>{{ formatLogAction(log.action) }}</strong>
              <span>{{ formatOrderStatus(log.fromStatus) }} -> {{ formatOrderStatus(log.toStatus) }}</span>
              <span>{{ formatOperatorType(log.operatorType) }}#{{ log.operatorId }}</span>
            </div>
            <div class="log-remark">{{ log.remark || '-' }}</div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </div>
  </el-drawer>

  <el-dialog v-model="shipVisible" title="订单发货" width="440px">
    <el-form label-position="top">
      <el-form-item label="物流公司编码">
        <el-input v-model="shipForm.carrierCode" placeholder="例如：SF" />
      </el-form-item>
      <el-form-item label="运单号">
        <el-input v-model="shipForm.trackingNo" placeholder="请输入运单号" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="shipForm.remark" type="textarea" :rows="3" maxlength="128" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="shipVisible = false">取消</el-button>
      <el-button type="primary" :loading="shipSubmitting" @click="submitShip">确认发货</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  cancelAdminOrder,
  completeAdminOrder,
  fetchAdminOrderDetail,
  fetchAdminOrderList,
  shipAdminOrder,
  type AdminOrderDetail,
  type AdminOrderSummary,
} from "../api/adminOrder";

const loading = ref(false);
const rows = ref<AdminOrderSummary[]>([]);
const total = ref(0);
const selectedOrderNos = ref<string[]>([]);
const query = reactive({
  status: "",
  keyword: "",
  pageNo: 1,
  pageSize: 20,
});

const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<AdminOrderDetail | null>(null);

const shipVisible = ref(false);
const shipSubmitting = ref(false);
const shipOrderNo = ref("");
const shipForm = reactive({
  carrierCode: "SF",
  trackingNo: "",
  remark: "",
});

const selectedRows = computed(() => {
  if (selectedOrderNos.value.length === 0) {
    return [];
  }
  const orderNoMap: Record<string, boolean> = {};
  selectedOrderNos.value.forEach((orderNo) => {
    orderNoMap[orderNo] = true;
  });
  return rows.value.filter((row) => Boolean(orderNoMap[row.orderNo]));
});

const batchCancelableCount = computed(() => {
  return selectedRows.value.filter((row) => row.orderStatus === "PENDING_PAY").length;
});

const batchCompletableCount = computed(() => {
  return selectedRows.value.filter((row) => row.orderStatus === "SHIPPED_WAIT_RECEIVE").length;
});

const statusSummary = computed(() => {
  const summary = {
    pendingPay: 0,
    paidWaitShip: 0,
    shippedWaitReceive: 0,
    completed: 0,
    cancelled: 0,
  };
  rows.value.forEach((row) => {
    if (row.orderStatus === "PENDING_PAY") {
      summary.pendingPay += 1;
    } else if (row.orderStatus === "PAID_WAIT_SHIP") {
      summary.paidWaitShip += 1;
    } else if (row.orderStatus === "SHIPPED_WAIT_RECEIVE") {
      summary.shippedWaitReceive += 1;
    } else if (row.orderStatus === "COMPLETED") {
      summary.completed += 1;
    } else if (row.orderStatus === "CANCELLED") {
      summary.cancelled += 1;
    }
  });
  return summary;
});

function formatAmount(amount: number | undefined): string {
  return Number(amount || 0).toFixed(2);
}

function formatOrderStatus(status?: string): string {
  const map: Record<string, string> = {
    PENDING_PAY: "待支付",
    PAID_WAIT_SHIP: "待发货",
    SHIPPED_WAIT_RECEIVE: "待收货",
    COMPLETED: "已完成",
    CANCELLED: "已取消",
  };
  if (!status) {
    return "-";
  }
  return map[status] || status;
}

function formatPayStatus(status?: string): string {
  const map: Record<string, string> = {
    UNPAID: "未支付",
    PAID: "已支付",
    REFUNDED: "已退款",
    PARTIAL_REFUNDED: "部分退款",
  };
  if (!status) {
    return "-";
  }
  return map[status] || status;
}

function formatItemType(itemType?: string): string {
  const map: Record<string, string> = {
    NORMAL_GOODS: "普通商品",
    DIY_SNAPSHOT: "定制手串",
    CUSTOM_BRACELET: "定制手串",
  };
  if (!itemType) {
    return "-";
  }
  return map[itemType] || itemType;
}

function formatLogAction(action?: string): string {
  const map: Record<string, string> = {
    CREATE_ORDER: "创建订单",
    PAY_SUCCESS: "支付成功",
    SHIP_ORDER: "订单发货",
    CONFIRM_RECEIPT: "确认收货",
    CANCEL_ORDER: "取消订单",
    ADMIN_COMPLETE: "后台完成",
  };
  if (!action) {
    return "-";
  }
  return map[action] || action;
}

function formatOperatorType(operatorType?: string): string {
  const map: Record<string, string> = {
    ADMIN: "管理员",
    USER: "用户",
    SYSTEM: "系统",
  };
  if (!operatorType) {
    return "-";
  }
  return map[operatorType] || operatorType;
}

function statusTagType(status: string): string {
  if (status === "PENDING_PAY") {
    return "warning";
  }
  if (status === "PAID_WAIT_SHIP") {
    return "primary";
  }
  if (status === "SHIPPED_WAIT_RECEIVE") {
    return "success";
  }
  if (status === "CANCELLED") {
    return "info";
  }
  return "";
}

function renderAddress(address?: Record<string, string>): string {
  if (!address) {
    return "-";
  }
  const parts = [
    address.receiverName,
    address.receiverPhone,
    address.province,
    address.city,
    address.district,
    address.detailAddress,
  ].filter(Boolean);
  return parts.length > 0 ? parts.join(" ") : "-";
}

function renderShipping(shipping?: {
  carrierCode?: string;
  trackingNo?: string;
  remark?: string;
  shippedAt?: string;
}): string {
  if (!shipping || !shipping.trackingNo) {
    return "-";
  }
  const parts = [shipping.carrierCode, shipping.trackingNo, shipping.remark, shipping.shippedAt].filter(Boolean);
  return parts.join(" / ");
}

async function loadOrders() {
  loading.value = true;
  try {
    const result = await fetchAdminOrderList({
      status: query.status || undefined,
      keyword: query.keyword || undefined,
      pageNo: query.pageNo,
      pageSize: query.pageSize,
    });
    rows.value = result.list || [];
    const orderNoMap: Record<string, boolean> = {};
    rows.value.forEach((item) => {
      orderNoMap[item.orderNo] = true;
    });
    selectedOrderNos.value = selectedOrderNos.value.filter((orderNo) => Boolean(orderNoMap[orderNo]));
    total.value = result.page?.total || 0;
  } catch (error: any) {
    ElMessage.error(error?.message || "订单加载失败");
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNo = 1;
  loadOrders();
}

function handleReset() {
  query.status = "";
  query.keyword = "";
  query.pageNo = 1;
  query.pageSize = 20;
  loadOrders();
}

function onSelectionChange(selection: AdminOrderSummary[]) {
  selectedOrderNos.value = (selection || []).map((item) => item.orderNo);
}

function handlePageChange(pageNo: number) {
  query.pageNo = pageNo;
  loadOrders();
}

function handleSizeChange(pageSize: number) {
  query.pageSize = pageSize;
  query.pageNo = 1;
  loadOrders();
}

async function openDetail(orderNo: string) {
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    detail.value = await fetchAdminOrderDetail(orderNo);
  } catch (error: any) {
    ElMessage.error(error?.message || "订单详情加载失败");
  } finally {
    detailLoading.value = false;
  }
}

function openShip(orderNo: string) {
  shipOrderNo.value = orderNo;
  shipForm.carrierCode = "SF";
  shipForm.trackingNo = `SF${Date.now()}`;
  shipForm.remark = "";
  shipVisible.value = true;
}

async function submitShip() {
  if (!shipOrderNo.value) {
    return;
  }
  if (!shipForm.carrierCode || !shipForm.trackingNo) {
    ElMessage.warning("物流公司编码和运单号不能为空");
    return;
  }
  shipSubmitting.value = true;
  try {
    await shipAdminOrder(shipOrderNo.value, {
      carrierCode: shipForm.carrierCode.trim(),
      trackingNo: shipForm.trackingNo.trim(),
      remark: shipForm.remark.trim(),
    });
    ElMessage.success("发货成功");
    shipVisible.value = false;
    await loadOrders();
    if (detailVisible.value && detail.value?.orderNo === shipOrderNo.value) {
      await openDetail(shipOrderNo.value);
    }
  } catch (error: any) {
    ElMessage.error(error?.message || "发货失败");
  } finally {
    shipSubmitting.value = false;
  }
}

async function handleCancel(orderNo: string) {
  try {
    await ElMessageBox.confirm("确认取消该订单吗？", "提示", { type: "warning" });
    await cancelAdminOrder(orderNo, "管理员取消订单");
    ElMessage.success("订单已取消");
    await loadOrders();
    if (detailVisible.value && detail.value?.orderNo === orderNo) {
      await openDetail(orderNo);
    }
  } catch (error: any) {
    if (error === "cancel") {
      return;
    }
    ElMessage.error(error?.message || "取消订单失败");
  }
}

async function handleComplete(orderNo: string) {
  try {
    await ElMessageBox.confirm("确认将该订单标记为已完成吗？", "提示", { type: "info" });
    await completeAdminOrder(orderNo, "管理员完成订单");
    ElMessage.success("订单已标记完成");
    await loadOrders();
    if (detailVisible.value && detail.value?.orderNo === orderNo) {
      await openDetail(orderNo);
    }
  } catch (error: any) {
    if (error === "cancel") {
      return;
    }
    ElMessage.error(error?.message || "完成订单失败");
  }
}

async function batchCancel() {
  const targets = selectedRows.value.filter((row) => row.orderStatus === "PENDING_PAY");
  if (targets.length === 0) {
    ElMessage.warning("请先勾选待支付订单");
    return;
  }
  try {
    await ElMessageBox.confirm(`确认批量取消 ${targets.length} 笔订单吗？`, "提示", { type: "warning" });
    const results = await Promise.allSettled(
      targets.map((row) => cancelAdminOrder(row.orderNo, "管理员批量取消订单"))
    );
    const successCount = results.filter((result) => result.status === "fulfilled").length;
    const failCount = results.length - successCount;
    if (successCount > 0 && failCount === 0) {
      ElMessage.success(`批量取消成功，共 ${successCount} 笔`);
    } else if (successCount > 0 && failCount > 0) {
      ElMessage.warning(`批量取消完成，成功 ${successCount} 笔，失败 ${failCount} 笔`);
    } else {
      ElMessage.error("批量取消失败");
    }
    await loadOrders();
  } catch (error: any) {
    if (error === "cancel") {
      return;
    }
    ElMessage.error(error?.message || "批量取消失败");
  }
}

async function batchComplete() {
  const targets = selectedRows.value.filter((row) => row.orderStatus === "SHIPPED_WAIT_RECEIVE");
  if (targets.length === 0) {
    ElMessage.warning("请先勾选待收货订单");
    return;
  }
  try {
    await ElMessageBox.confirm(`确认批量完成 ${targets.length} 笔订单吗？`, "提示", { type: "warning" });
    const results = await Promise.allSettled(
      targets.map((row) => completeAdminOrder(row.orderNo, "管理员批量完成订单"))
    );
    const successCount = results.filter((result) => result.status === "fulfilled").length;
    const failCount = results.length - successCount;
    if (successCount > 0 && failCount === 0) {
      ElMessage.success(`批量完成成功，共 ${successCount} 笔`);
    } else if (successCount > 0 && failCount > 0) {
      ElMessage.warning(`批量完成结束，成功 ${successCount} 笔，失败 ${failCount} 笔`);
    } else {
      ElMessage.error("批量完成失败");
    }
    await loadOrders();
  } catch (error: any) {
    if (error === "cancel") {
      return;
    }
    ElMessage.error(error?.message || "批量完成失败");
  }
}

onMounted(() => {
  loadOrders();
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

.table-tools {
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.summary-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.batch-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.section-title {
  margin: 18px 0 10px;
}

.log-line {
  display: flex;
  gap: 10px;
  align-items: center;
}

.log-remark {
  color: #6b7280;
  margin-top: 4px;
}
</style>
