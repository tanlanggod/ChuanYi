<template>
  <el-container class="admin-layout">
    <el-aside :width="isCollapse ? '82px' : '268px'" class="sidebar">
      <div class="brand">
        <div class="brand-logo">
          <el-icon><GobletFull /></el-icon>
        </div>
        <transition name="el-fade-in">
          <div v-show="!isCollapse" class="brand-copy">
            <div class="brand-title">养个石头</div>
            <div class="brand-sub">Admin Studio</div>
          </div>
        </transition>
      </div>

      <el-menu
        :default-active="activePath"
        class="custom-menu"
        :collapse="isCollapse"
        :collapse-transition="true"
        router
      >
        <el-menu-item index="/">
          <el-icon><DataBoard /></el-icon>
          <template #title>数据看板</template>
        </el-menu-item>
        <el-menu-item index="/orders">
          <el-icon><Tickets /></el-icon>
          <template #title>订单管理</template>
        </el-menu-item>
        <el-menu-item index="/goods">
          <el-icon><Goods /></el-icon>
          <template #title>好物管理</template>
        </el-menu-item>
        <el-menu-item index="/components">
          <el-icon><Box /></el-icon>
          <template #title>素材库管理</template>
        </el-menu-item>
        <el-menu-item index="/content">
          <el-icon><Document /></el-icon>
          <template #title>内容管理</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="main-container">
      <el-header class="custom-header">
        <div class="header-left">
          <button class="collapse-btn" @click="toggleCollapse">
            <el-icon><component :is="isCollapse ? 'Expand' : 'Fold'" /></el-icon>
          </button>
          <div class="crumb-wrap">
            <div class="crumb-meta">管理后台</div>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item>{{ currentRouteName }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
        </div>

        <div class="header-right">
          <el-tooltip content="通知" placement="bottom">
            <button class="header-icon-btn">
              <el-icon class="header-icon"><Bell /></el-icon>
            </button>
          </el-tooltip>
          <el-dropdown trigger="click" @command="handleCommand">
            <button class="user-profile">
              <el-avatar :size="32" class="user-avatar">{{ adminName.charAt(0) }}</el-avatar>
              <span class="user-name">{{ adminName }}</span>
              <el-icon class="user-arrow"><CaretBottom /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item divided command="logout" class="danger-text">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="custom-main">
        <div class="main-shell">
          <router-view v-slot="{ Component }">
            <transition name="fade-slide" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import {
  adminLogout,
  clearAdminSession,
  fetchAdminMe,
  loadAdminProfile,
} from "../api/adminAuth";

const route = useRoute();
const router = useRouter();

const activePath = computed(() => route.path || "/");
const isCollapse = ref(false);
const adminName = ref("管理员");

const routeNameMap: Record<string, string> = {
  "/": "数据看板",
  "/orders": "订单管理",
  "/goods": "好物管理",
  "/components": "素材库管理",
  "/content": "内容管理",
};

const currentRouteName = computed(() => routeNameMap[route.path] || "控制台");

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value;
};

const handleCommand = async (command: string) => {
  if (command === "logout") {
    try {
      await adminLogout();
    } catch (_error) {
      // ignore network failures on logout
    }
    clearAdminSession();
    router.push("/login");
    ElMessage.success("已安全退出");
  } else if (command === "profile") {
    ElMessage.info("个人中心开发中...");
  }
};

onMounted(async () => {
  const localProfile = loadAdminProfile();
  if (localProfile) {
    adminName.value = localProfile.nickname || localProfile.username || "管理员";
  }
  try {
    const profile = await fetchAdminMe();
    adminName.value = profile.nickname || profile.username || "管理员";
  } catch (error: any) {
    clearAdminSession();
    ElMessage.error(error?.message || "登录已过期，请重新登录");
    router.push("/login");
  }
});
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  overflow: hidden;
  background: var(--bg-page);
}

.sidebar {
  background: linear-gradient(180deg, #2f2924 0%, #26211c 100%);
  transition: width var(--motion-base) var(--ease-standard);
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
}

.brand {
  height: 84px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 18px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.brand-logo {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #c7a57f 0%, #a87f5e 100%);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.22);
  color: #fffdf9;
  font-size: 22px;
  flex-shrink: 0;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.brand-title {
  color: #f2ede3;
  font-family: var(--font-display);
  font-size: 20px;
  line-height: 1.1;
  letter-spacing: 0.6px;
}

.brand-sub {
  color: #b8afa3;
  font-size: 11px;
  margin-top: 4px;
  letter-spacing: 1.1px;
  text-transform: uppercase;
}

.custom-menu {
  border-right: none;
  flex: 1;
  background: transparent;
  padding: 12px 10px 18px;
}

.custom-menu:not(.el-menu--collapse) {
  width: 268px;
}

::v-deep(.custom-menu .el-menu-item) {
  height: 46px;
  line-height: 46px;
  margin: 6px 6px;
  border-radius: 12px;
  color: #d7d1c7;
  transition: all var(--motion-fast) var(--ease-standard);
}

::v-deep(.custom-menu .el-menu-item .el-icon) {
  color: #b9b0a4;
}

::v-deep(.custom-menu .el-menu-item:hover:not(.is-active)) {
  background: rgba(255, 255, 255, 0.06) !important;
  color: #f2ede3 !important;
}

::v-deep(.custom-menu .el-menu-item:hover:not(.is-active) .el-icon) {
  color: #e6dece !important;
}

::v-deep(.custom-menu .el-menu-item.is-active) {
  background: linear-gradient(135deg, #a77d5f 0%, #8d6a4f 100%) !important;
  color: #fff !important;
  box-shadow: 0 12px 24px rgba(141, 106, 79, 0.34);
}

::v-deep(.custom-menu .el-menu-item.is-active .el-icon) {
  color: #fff !important;
}

.main-container {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.custom-header {
  height: 78px;
  background: rgba(252, 250, 246, 0.95);
  border-bottom: 1px solid var(--line-2);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 26px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.collapse-btn,
.header-icon-btn,
.user-profile {
  border: none;
  background: transparent;
  padding: 0;
}

.collapse-btn {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--text-2);
  cursor: pointer;
  transition: all var(--motion-fast) var(--ease-standard);
}

.collapse-btn:hover {
  background: var(--surface-2);
  color: var(--brand-1);
}

.crumb-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.crumb-meta {
  font-size: 11px;
  color: var(--text-3);
  letter-spacing: 0.9px;
  text-transform: uppercase;
}

::v-deep(.crumb-wrap .el-breadcrumb__inner) {
  color: var(--text-2);
}

::v-deep(.crumb-wrap .el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--text-1);
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon-btn {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--surface-1);
  border: 1px solid var(--line-2);
  cursor: pointer;
  transition: all var(--motion-fast) var(--ease-standard);
}

.header-icon-btn:hover {
  border-color: var(--brand-3);
  background: #fff;
}

.header-icon {
  color: var(--text-2);
  font-size: 17px;
}

.user-profile {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 2px 6px 2px 2px;
  border-radius: 999px;
  background: var(--surface-1);
  border: 1px solid var(--line-2);
  cursor: pointer;
  transition: all var(--motion-fast) var(--ease-standard);
}

.user-profile:hover {
  border-color: var(--brand-3);
  box-shadow: var(--shadow-sm);
}

.user-avatar {
  background: linear-gradient(145deg, var(--brand-2) 0%, var(--brand-1) 100%);
  color: #fff;
  font-weight: 600;
}

.user-name {
  color: var(--text-2);
  font-size: 13px;
}

.user-arrow {
  color: var(--text-3);
  font-size: 12px;
}

.danger-text {
  color: var(--state-danger) !important;
}

.custom-main {
  padding: 24px 26px 28px;
  overflow-y: auto;
}

.main-shell {
  max-width: 1440px;
  margin: 0 auto;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all var(--motion-base) var(--ease-standard);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
