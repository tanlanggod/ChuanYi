<template>
  <div v-loading="loading" class="dashboard">
    <!-- Top Header Card -->
    <el-card class="top-card">
      <div class="top-head">
        <div class="top-info">
          <div class="top-title">运营看板</div>
          <div class="top-sub">订单与营收概览（实时）</div>
        </div>
        <div class="top-actions">
          <span class="update-time"><el-icon><Timer /></el-icon> 最近更新：{{ updateTimeText }}</span>
          <el-button type="primary" :loading="loading" @click="loadOverview" :icon="'Refresh'" round>刷新数据</el-button>
        </div>
      </div>
    </el-card>

    <!-- Key Metrics Row -->
    <el-row :gutter="24" class="mt24">
      <el-col :span="6">
        <el-card class="metric-card bg-gradient-primary">
          <div class="metric-content">
            <div class="metric-title text-white">今日营收 (元)</div>
            <div class="metric-value text-white">￥{{ formatAmount(overview.todayRevenue) }}</div>
          </div>
          <el-icon class="metric-icon text-white"><Money /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card bg-gradient-success">
          <div class="metric-content">
            <div class="metric-title text-white">今日下单 (笔)</div>
            <div class="metric-value text-white">{{ overview.todayOrderCount }}</div>
          </div>
          <el-icon class="metric-icon text-white"><ShoppingCart /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card bg-gradient-warning">
          <div class="metric-content">
            <div class="metric-title text-white">订单总数 (笔)</div>
            <div class="metric-value text-white">{{ overview.totalOrders }}</div>
          </div>
          <el-icon class="metric-icon text-white"><List /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card bg-gradient-info">
          <div class="metric-content">
            <div class="metric-title text-white">完成率</div>
            <div class="metric-value text-white">{{ completeRateText }}</div>
          </div>
          <el-icon class="metric-icon text-white"><DataLine /></el-icon>
        </el-card>
      </el-col>
    </el-row>

    <!-- Status Overview Row -->
    <el-row :gutter="24" class="mt24">
      <el-col :span="6">
        <el-card class="status-card">
          <div class="status-icon-wrapper bg-warning-light text-warning"><el-icon><Wallet /></el-icon></div>
          <div class="status-info">
            <div class="status-title">待支付</div>
            <div class="status-value">{{ overview.pendingPayCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="status-card">
          <div class="status-icon-wrapper bg-primary-light text-primary"><el-icon><Box /></el-icon></div>
          <div class="status-info">
            <div class="status-title">待发货</div>
            <div class="status-value">{{ overview.paidWaitShipCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="status-card">
          <div class="status-icon-wrapper bg-success-light text-success"><el-icon><Van /></el-icon></div>
          <div class="status-info">
            <div class="status-title">待收货</div>
            <div class="status-value">{{ overview.shippedWaitReceiveCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="status-card">
          <div class="status-icon-wrapper bg-danger-light text-danger"><el-icon><CircleClose /></el-icon></div>
          <div class="status-info">
            <div class="status-title">已取消</div>
            <div class="status-value">{{ overview.cancelledCount }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts and Tips Row -->
    <el-row :gutter="24" class="mt24 mb24">
      <el-col :span="12">
        <el-card class="panel-card h-100">
          <template #header>
            <div class="panel-head">
              <el-icon><PieChart /></el-icon>
              <span>订单状态占比</span>
            </div>
          </template>
          <div class="status-list">
            <div class="status-item" v-for="(item, index) in statusDistribution" :key="item.label">
              <div class="status-line">
                <span class="status-label">{{ item.label }}</span>
                <span class="status-val">{{ item.value }} 笔 <span class="status-pct">({{ item.percent }})</span></span>
              </div>
              <el-progress 
                :percentage="item.percentNumber" 
                :stroke-width="8" 
                :show-text="false" 
                :color="getProgressColor(index)"
              />
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card class="panel-card h-100">
          <template #header>
            <div class="panel-head">
              <el-icon><Bell /></el-icon>
              <span>运营提示</span>
            </div>
          </template>
          <div class="tips">
            <div class="tip-item">
              <div class="tip-icon-box bg-warning-light text-warning"><el-icon><InfoFilled /></el-icon></div>
              <div class="tip-content">
                <div class="tip-title">关注支付转化</div>
                <div class="tip-desc">当前有 <strong class="text-warning">{{ overview.pendingPayCount }}</strong> 笔订单待支付，建议及时跟进。</div>
              </div>
            </div>
            <div class="tip-item">
              <div class="tip-icon-box bg-primary-light text-primary"><el-icon><Promotion /></el-icon></div>
              <div class="tip-content">
                <div class="tip-title">优先安排出库</div>
                <div class="tip-desc">当前有 <strong class="text-primary">{{ overview.paidWaitShipCount }}</strong> 笔订单已付款待发货，请尽快处理。</div>
              </div>
            </div>
            <div class="tip-item">
              <div class="tip-icon-box bg-success-light text-success"><el-icon><Location /></el-icon></div>
              <div class="tip-content">
                <div class="tip-title">关注物流状态</div>
                <div class="tip-desc">当前有 <strong class="text-success">{{ overview.shippedWaitReceiveCount }}</strong> 笔订单在途，可关注是否有物流异常。</div>
              </div>
            </div>
            <div class="tip-item">
              <div class="tip-icon-box bg-danger-light text-danger"><el-icon><Warning /></el-icon></div>
              <div class="tip-content">
                <div class="tip-title">异常订单排查</div>
                <div class="tip-desc">累计取消订单 <strong class="text-danger">{{ overview.cancelledCount }}</strong> 笔，请排查原因并优化体验。</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { fetchAdminOverview } from "../api/adminDashboard";

const loading = ref(false);
const updateTime = ref<Date | null>(null);
const overview = reactive({
  totalOrders: 0,
  pendingPayCount: 0,
  paidWaitShipCount: 0,
  shippedWaitReceiveCount: 0,
  completedCount: 0,
  cancelledCount: 0,
  todayOrderCount: 0,
  todayRevenue: 0,
});

const updateTimeText = computed(() => {
  if (!updateTime.value) {
    return "--";
  }
  const date = updateTime.value;
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(
    date.getMinutes()
  )}:${pad(date.getSeconds())}`;
});

const completeRateText = computed(() => {
  const total = Number(overview.totalOrders || 0);
  if (total <= 0) {
    return "0.00%";
  }
  const ratio = (Number(overview.completedCount || 0) / total) * 100;
  return `${ratio.toFixed(2)}%`;
});

const statusDistribution = computed(() => {
  const total = Math.max(1, Number(overview.totalOrders || 0));
  const rows = [
    { label: "待支付", value: Number(overview.pendingPayCount || 0) },
    { label: "待发货", value: Number(overview.paidWaitShipCount || 0) },
    { label: "待收货", value: Number(overview.shippedWaitReceiveCount || 0) },
    { label: "已完成", value: Number(overview.completedCount || 0) },
    { label: "已取消", value: Number(overview.cancelledCount || 0) },
  ];
  return rows.map((item) => {
    const percentNumber = Number(((item.value / total) * 100).toFixed(2));
    return {
      ...item,
      percentNumber,
      percent: `${percentNumber.toFixed(2)}%`,
    };
  });
});

function formatAmount(amount: number): string {
  return Number(amount || 0).toFixed(2);
}

function getProgressColor(index: number) {
  const colors = ["#e6a23c", "#409eff", "#67c23a", "#909399", "#f56c6c"];
  return colors[index % colors.length];
}

async function loadOverview() {
  loading.value = true;
  try {
    const data = await fetchAdminOverview();
    Object.assign(overview, data);
    updateTime.value = new Date();
  } catch (error: any) {
    ElMessage.error(error?.message || "加载看板数据失败");
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadOverview();
});
</script>

<style scoped>
.dashboard {
  min-height: 400px;
}

.top-card {
  margin-bottom: 24px;
}

.top-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.top-title {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
}

.top-sub {
  margin-top: 6px;
  color: #6b7280;
  font-size: 13px;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.update-time {
  color: #6b7280;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.mt24 { margin-top: 24px; }
.mb24 { margin-bottom: 24px; }
.h-100 { height: 100%; }

/* Metric Cards */
.metric-card {
  position: relative;
  overflow: hidden;
  border-radius: 16px !important;
  color: white;
}

.metric-card ::v-deep(.el-card__body) {
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.metric-title {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.metric-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
}

.metric-icon {
  font-size: 48px;
  opacity: 0.2;
  position: absolute;
  right: -10px;
  bottom: -10px;
  transform: rotate(-15deg);
  transition: all 0.3s ease;
}

.metric-card:hover .metric-icon {
  transform: scale(1.1) rotate(0deg);
  opacity: 0.3;
}

.bg-gradient-primary { background: linear-gradient(135deg, #409eff 0%, #3a7bd5 100%); }
.bg-gradient-success { background: linear-gradient(135deg, #67c23a 0%, #4caf50 100%); }
.bg-gradient-warning { background: linear-gradient(135deg, #e6a23c 0%, #ff9800 100%); }
.bg-gradient-info { background: linear-gradient(135deg, #909399 0%, #606266 100%); }

.text-white { color: #fff !important; }

/* Status Cards */
.status-card {
  border-radius: 16px !important;
}

.status-card ::v-deep(.el-card__body) {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 28px;
}

.status-info {
  flex: 1;
}

.status-title {
  color: #6b7280;
  font-size: 14px;
  margin-bottom: 4px;
}

.status-value {
  color: #1f2937;
  font-size: 24px;
  font-weight: 700;
}

.bg-primary-light { background-color: #ecf5ff; }
.bg-success-light { background-color: #f0f9eb; }
.bg-warning-light { background-color: #fdf6ec; }
.bg-danger-light { background-color: #fef0f0; }

.text-primary { color: #409eff; }
.text-success { color: #67c23a; }
.text-warning { color: #e6a23c; }
.text-danger { color: #f56c6c; }

/* Panel Cards */
.panel-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 10px 0;
}

.status-line {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}

.status-label {
  color: #4b5563;
  font-weight: 500;
}

.status-val {
  color: #111827;
  font-weight: 600;
}

.status-pct {
  color: #9ca3af;
  font-weight: normal;
  margin-left: 4px;
}

.tips {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.tip-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  background-color: #f9fafb;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.tip-item:hover {
  background-color: #f3f4f6;
  transform: translateX(4px);
}

.tip-icon-box {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 20px;
  flex-shrink: 0;
}

.tip-content {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.tip-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.tip-desc {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
}
</style>
