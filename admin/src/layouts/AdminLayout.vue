<template>
  <el-container class="layout">
    <el-aside width="220px" class="sidebar">
      <div class="brand">养个石头管理端</div>
      <el-menu :default-active="activePath" router>
        <el-menu-item index="/">
          <span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/orders">
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/goods">
          <span>好物管理</span>
        </el-menu-item>
        <el-menu-item index="/components">
          <span>素材库管理</span>
        </el-menu-item>
        <el-menu-item index="/content">
          <span>内容管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-title">手串定制管理后台</div>
        <div class="header-right">
          <span class="admin-name">{{ adminName }}</span>
          <el-button text @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
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
const adminName = ref("管理员");

const logout = async () => {
  try {
    await adminLogout();
  } catch (_error) {
    // ignore network failures on logout
  }
  clearAdminSession();
  router.push("/login");
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
.layout {
  min-height: 100vh;
}

.sidebar {
  border-right: 1px solid #e5e7eb;
  background: #ffffff;
}

.brand {
  padding: 16px;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid #e5e7eb;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.header-title {
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-name {
  color: #6b7280;
  font-size: 13px;
}

.main {
  background: #f5f7fa;
}
</style>
