<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2>管理员登录</h2>
      <el-form :model="form" label-position="top">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-button type="primary" style="width: 100%" :loading="loading" @click="login">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { adminLogin, saveAdminSession } from "../api/adminAuth";

const router = useRouter();
const form = reactive({
  username: "admin",
  password: "123456",
});
const loading = ref(false);

const login = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning("请输入账号和密码");
    return;
  }
  loading.value = true;
  try {
    const data = await adminLogin(form.username, form.password);
    saveAdminSession(data.token, data.admin);
    ElMessage.success("登录成功");
    router.push("/");
  } catch (error: any) {
    ElMessage.error(error?.message || "登录失败");
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #fff5f5 0%, #f4f7fb 100%);
}

.login-card {
  width: 380px;
}
</style>
