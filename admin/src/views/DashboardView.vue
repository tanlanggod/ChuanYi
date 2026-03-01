<template>
  <div v-loading="loading" class="dashboard">
    <el-card class="top-card">
      <div class="top-head">
        <div>
          <div class="top-title">运营看板</div>
          <div class="top-sub">订单与营收概览（实时）</div>
        </div>
        <div class="top-actions">
          <span class="update-time">最近更新：{{ updateTimeText }}</span>
          <el-button type="primary" :loading="loading" @click="loadOverview">刷新</el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" class="mt16">
      <el-col :span="6">
        <el-card>
          <div class="metric-title">今日营收</div>
          <div class="metric-value">￥{{ formatAmount(overview.todayRevenue) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="metric-title">今日下单</div>
          <div class="metric-value">{{ overview.todayOrderCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="metric-title">订单总数</div>
          <div class="metric-value">{{ overview.totalOrders }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="metric-title">完成率</div>
          <div class="metric-value">{{ completeRateText }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :span="6">
        <el-card>
          <div class="metric-title">待支付</div>
          <div class="metric-value small">{{ overview.pendingPayCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="metric-title">待发货</div>
          <div class="metric-value small">{{ overview.paidWaitShipCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="metric-title">待收货</div>
          <div class="metric-value small">{{ overview.shippedWaitReceiveCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="metric-title">已取消</div>
          <div class="metric-value small">{{ overview.cancelledCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="panel-head">订单状态占比</div>
          </template>
          <div class="status-list">
            <div class="status-item" v-for="item in statusDistribution" :key="item.label">
              <div class="status-line">
                <span>{{ item.label }}</span>
                <span>{{ item.value }}（{{ item.percent }}）</span>
              </div>
              <el-progress :percentage="item.percentNumber" :stroke-width="10" :show-text="false" />
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="panel-head">运营提示</div>
          </template>
          <div class="tips">
            <div class="tip-item">1. 待支付订单 {{ overview.pendingPayCount }} 笔，建议关注支付转化。</div>
            <div class="tip-item">2. 待发货订单 {{ overview.paidWaitShipCount }} 笔，建议优先安排出库。</div>
            <div class="tip-item">3. 待收货订单 {{ overview.shippedWaitReceiveCount }} 笔，可关注物流异常。</div>
            <div class="tip-item">4. 已取消订单 {{ overview.cancelledCount }} 笔，请排查原因并优化。</div>
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
  border: 1px solid #eef2f7;
}

.top-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.top-title {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

.top-sub {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.update-time {
  color: #64748b;
  font-size: 12px;
}

.metric-title {
  color: #64748b;
  font-size: 14px;
}

.metric-value {
  margin-top: 10px;
  color: #111827;
  font-size: 26px;
  font-weight: 600;
}

.metric-value.small {
  font-size: 24px;
}

.mt16 {
  margin-top: 16px;
}

.panel-head {
  font-weight: 700;
  color: #111827;
}

.status-list {
  display: grid;
  gap: 10px;
}

.status-line {
  margin-bottom: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #374151;
  font-size: 13px;
}

.tips {
  display: grid;
  gap: 10px;
}

.tip-item {
  line-height: 1.7;
  color: #4b5563;
  font-size: 13px;
}
</style>
