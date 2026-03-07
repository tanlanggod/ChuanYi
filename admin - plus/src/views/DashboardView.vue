<template>
  <div v-loading="loading" class="admin-page dashboard">
    <section class="admin-page__header">
      <div class="admin-page__heading">
        <div class="admin-page__title"><el-icon><DataBoard /></el-icon><span>运营看板</span></div>
        <div class="admin-page__subtitle">订单与营收概览（实时）</div>
      </div>
      <div class="admin-page__actions">
        <span class="update-time"><el-icon><Timer /></el-icon>最近更新：{{ updateTimeText }}</span>
        <el-button type="primary" :loading="loading" @click="loadOverview" :icon="'Refresh'">刷新数据</el-button>
      </div>
    </section>

    <section class="metrics-grid">
      <el-card class="metric-card">
        <div class="metric-label">今日营收（元）</div>
        <div class="metric-value">￥{{ formatAmount(overview.todayRevenue) }}</div>
        <el-icon class="metric-icon"><Money /></el-icon>
      </el-card>
      <el-card class="metric-card">
        <div class="metric-label">今日下单（笔）</div>
        <div class="metric-value">{{ overview.todayOrderCount }}</div>
        <el-icon class="metric-icon"><ShoppingCart /></el-icon>
      </el-card>
      <el-card class="metric-card">
        <div class="metric-label">订单总数（笔）</div>
        <div class="metric-value">{{ overview.totalOrders }}</div>
        <el-icon class="metric-icon"><List /></el-icon>
      </el-card>
      <el-card class="metric-card">
        <div class="metric-label">完成率</div>
        <div class="metric-value">{{ completeRateText }}</div>
        <el-icon class="metric-icon"><DataLine /></el-icon>
      </el-card>
    </section>

    <section class="status-band">
      <div class="status-item">
        <span class="status-name">待支付</span>
        <span class="status-num">{{ overview.pendingPayCount }}</span>
      </div>
      <div class="status-item">
        <span class="status-name">待发货</span>
        <span class="status-num">{{ overview.paidWaitShipCount }}</span>
      </div>
      <div class="status-item">
        <span class="status-name">待收货</span>
        <span class="status-num">{{ overview.shippedWaitReceiveCount }}</span>
      </div>
      <div class="status-item">
        <span class="status-name">已取消</span>
        <span class="status-num">{{ overview.cancelledCount }}</span>
      </div>
    </section>

    <section class="insight-grid">
      <el-card class="admin-table-shell">
        <template #header>
          <h3 class="admin-section-title">订单状态占比</h3>
        </template>
        <div class="status-list">
          <div class="status-row" v-for="(item, index) in statusDistribution" :key="item.label">
            <div class="status-line">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }} 笔（{{ item.percent }}）</strong>
            </div>
            <el-progress :percentage="item.percentNumber" :stroke-width="7" :show-text="false" :color="getProgressColor(index)" />
          </div>
        </div>
      </el-card>

      <el-card class="admin-table-shell">
        <template #header>
          <h3 class="admin-section-title">运营提示</h3>
        </template>
        <ul class="tips-list">
          <li><span>关注支付转化</span><em>待支付 {{ overview.pendingPayCount }} 笔，建议及时跟进。</em></li>
          <li><span>优先安排出库</span><em>待发货 {{ overview.paidWaitShipCount }} 笔，请尽快处理。</em></li>
          <li><span>关注物流状态</span><em>在途 {{ overview.shippedWaitReceiveCount }} 笔，可排查异常物流。</em></li>
          <li><span>异常订单排查</span><em>累计取消 {{ overview.cancelledCount }} 笔，建议复盘原因。</em></li>
        </ul>
      </el-card>
    </section>
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
  gap: var(--space-5);
}

.update-time {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--text-3);
  font-size: 12px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-4);
}

.metric-card {
  position: relative;
  overflow: hidden;
  background: linear-gradient(160deg, #fdfbf7 0%, #f5efe5 100%);
}

.metric-card :deep(.el-card__body) {
  padding: 18px 20px;
}

.metric-label {
  font-size: 12px;
  color: var(--text-3);
  margin-bottom: 10px;
}

.metric-value {
  font-family: var(--font-display);
  font-size: 30px;
  color: var(--text-1);
  line-height: 1.05;
}

.metric-icon {
  position: absolute;
  right: 14px;
  bottom: 10px;
  font-size: 28px;
  color: rgba(141, 106, 79, 0.3);
}

.status-band {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-3);
  padding: 14px;
  border: 1px solid var(--line-2);
  border-radius: var(--radius-md);
  background: var(--surface-1);
}

.status-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  background: var(--surface-elevated);
}

.status-name {
  font-size: 12px;
  color: var(--text-3);
}

.status-num {
  font-family: var(--font-display);
  font-size: 24px;
  color: var(--text-1);
}

.insight-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.status-line {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  color: var(--text-2);
  font-size: 13px;
}

.status-line strong {
  color: var(--text-1);
  font-weight: 600;
}

.tips-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tips-list li {
  border: 1px solid var(--line-2);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: #fff;
}

.tips-list span {
  font-size: 13px;
  color: var(--text-1);
  font-weight: 600;
}

.tips-list em {
  font-style: normal;
  font-size: 12px;
  color: var(--text-3);
}

@media (max-width: 1200px) {
  .metrics-grid,
  .status-band {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .insight-grid {
    grid-template-columns: 1fr;
  }
}
</style>
