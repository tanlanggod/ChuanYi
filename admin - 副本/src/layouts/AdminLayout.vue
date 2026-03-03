<template>
  <el-container class="admin-layout">
    <!-- Sidebar -->
    <el-aside :width="isCollapse ? '64px' : '240px'" class="sidebar">
      <div class="brand">
        <div class="brand-logo">
          <el-icon :size="24" color="#fff"><GobletFull /></el-icon>
        </div>
        <transition name="el-fade-in">
          <span v-show="!isCollapse" class="brand-text">养个石头 · 管理后台</span>
        </transition>
      </div>

      <el-menu
        :default-active="activePath"
        class="custom-menu"
        :collapse="isCollapse"
        :collapse-transition="true"
        background-color="#1e222d"
        text-color="#a3a6af"
        active-text-color="#ffffff"
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

    <!-- Main Container -->
    <el-container class="main-container">
      <!-- Header -->
      <el-header class="custom-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleCollapse">
            <component :is="isCollapse ? 'Expand' : 'Fold'" />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentRouteName }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-tooltip content="通知" placement="bottom">
            <el-icon class="header-icon"><Bell /></el-icon>
          </el-tooltip>
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-profile">
              <el-avatar :size="32" class="user-avatar">{{ adminName.charAt(0) }}</el-avatar>
              <span class="user-name">{{ adminName }}</span>
              <el-icon class="user-arrow"><CaretBottom /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item divided command="logout" class="danger-text">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- Content -->
      <el-main class="custom-main">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
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
  background-color: #f0f2f5;
}

/* Sidebar */
.sidebar {
  background-color: #1e222d;
  transition: width 0.3s cubic-bezier(0.2, 0, 0, 1) 0s;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 8px 0 rgba(29,35,41,.05);
  z-index: 10;
}

.brand {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  background-color: #1e222d;
  overflow: hidden;
  white-space: nowrap;
  box-sizing: border-box;
}

.brand-logo {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #409eff 0%, #3a7bd5 100%);
  border-radius: 8px;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(64,158,255,0.4);
}

.brand-text {
  margin-left: 12px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.custom-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

.custom-menu:not(.el-menu--collapse) {
  width: 240px;
}

::v-deep(.el-menu-item) {
  height: 50px;
  line-height: 50px;
  margin: 4px 12px;
  border-radius: 8px;
  transition: all 0.3s;
}

::v-deep(.el-menu-item.is-active) {
  background: var(--el-color-primary) !important;
  color: #fff !important;
  box-shadow: 0 4px 12px rgba(64,158,255,0.3);
}

::v-deep(.el-menu-item:hover:not(.is-active)) {
  background-color: rgba(255, 255, 255, 0.05) !important;
  color: #fff !important;
}

/* Main Container */
.main-container {
  display: flex;
  flex-direction: column;
}

/* Header */
.custom-header {
  height: 60px;
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 9;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
  transition: color 0.3s;
}

.collapse-btn:hover {
  color: var(--el-color-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-icon {
  font-size: 20px;
  color: #606266;
  cursor: pointer;
  transition: color 0.3s;
}

.header-icon:hover {
  color: var(--el-color-primary);
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background-color 0.3s;
}

.user-profile:hover {
  background-color: #f5f7fa;
}

.user-avatar {
  background: var(--el-color-primary-light-3);
  color: #fff;
  font-weight: bold;
}

.user-name {
  font-size: 14px;
  color: #303133;
}

.user-arrow {
  font-size: 12px;
  color: #909399;
}

.danger-text {
  color: var(--el-color-danger) !important;
}

/* Main Content Area */
.custom-main {
  padding: 24px;
  background-color: #f0f2f5;
  box-sizing: border-box;
  overflow-y: auto;
  position: relative;
}

/* Transitions */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.2, 0, 0, 1);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}
</style>
