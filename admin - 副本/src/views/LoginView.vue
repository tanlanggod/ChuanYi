<template>
  <div class="login-page">
    <!-- Animated background shapes -->
    <div class="bg-shape shape-1"></div>
    <div class="bg-shape shape-2"></div>
    <div class="bg-shape shape-3"></div>

    <div class="login-container">
      <div class="login-left">
        <div class="logo-wrapper">
          <el-icon class="logo-icon"><GobletFull /></el-icon>
        </div>
        <h1 class="welcome-text">欢迎来到</h1>
        <h2 class="brand-name">养个石头 · 后台管理系统</h2>
        <p class="brand-desc">一站式解决您的手串定制管理需求</p>
      </div>

      <div class="login-right">
        <el-card class="login-card" shadow="never">
          <div class="card-header">
            <h3>账号登录</h3>
            <p>请输入管理员账号与密码</p>
          </div>
          
          <el-form :model="form" class="login-form">
            <el-form-item>
              <el-input 
                v-model="form.username" 
                placeholder="请输入账号" 
                size="large"
                :prefix-icon="'User'"
              />
            </el-form-item>
            <el-form-item>
              <el-input 
                v-model="form.password" 
                type="password" 
                placeholder="请输入密码" 
                show-password 
                size="large"
                :prefix-icon="'Lock'"
                @keyup.enter="login"
              />
            </el-form-item>
            
            <el-button 
              type="primary" 
              class="submit-btn" 
              size="large" 
              :loading="loading" 
              @click="login"
            >
              登 录
            </el-button>
          </el-form>
        </el-card>
      </div>
    </div>
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
    ElMessage.success("登录成功，欢迎回来");
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
  background-color: #f0f4f8;
  position: relative;
  overflow: hidden;
}

/* Abstract Background Shapes */
.bg-shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  z-index: 0;
  animation: float 10s infinite ease-in-out alternate;
}

.shape-1 {
  width: 500px;
  height: 500px;
  background: rgba(64, 158, 255, 0.2);
  top: -150px;
  left: -100px;
  animation-delay: 0s;
}

.shape-2 {
  width: 400px;
  height: 400px;
  background: rgba(103, 194, 58, 0.15);
  bottom: -100px;
  right: -50px;
  animation-delay: -3s;
}

.shape-3 {
  width: 300px;
  height: 300px;
  background: rgba(230, 162, 60, 0.1);
  top: 30%;
  left: 60%;
  animation-delay: -6s;
}

@keyframes float {
  0% { transform: translateY(0) scale(1); }
  100% { transform: translateY(-30px) scale(1.05); }
}

.login-container {
  display: flex;
  width: 1000px;
  max-width: 90%;
  min-height: 500px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  z-index: 1;
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, var(--el-color-primary) 0%, #2e62aa 100%);
  color: white;
  padding: 60px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.logo-wrapper {
  width: 64px;
  height: 64px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 30px;
  backdrop-filter: blur(10px);
}

.logo-icon {
  font-size: 36px;
  color: #fff;
}

.welcome-text {
  font-size: 24px;
  font-weight: 300;
  margin: 0 0 10px;
  opacity: 0.9;
}

.brand-name {
  font-size: 32px;
  font-weight: 600;
  margin: 0 0 20px;
  letter-spacing: 1px;
}

.brand-desc {
  font-size: 15px;
  opacity: 0.8;
  line-height: 1.6;
}

.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: #ffffff;
}

.login-card {
  width: 100%;
  max-width: 360px;
  background: transparent !important;
  box-shadow: none !important;
}

.card-header {
  margin-bottom: 30px;
  text-align: center;
}

.card-header h3 {
  font-size: 24px;
  color: #1f2937;
  margin: 0 0 8px;
  font-weight: 600;
}

.card-header p {
  color: #6b7280;
  font-size: 14px;
  margin: 0;
}

.login-form {
  margin-top: 20px;
}

::v-deep(.el-input__wrapper) {
  padding: 0 15px;
  background-color: #f9fafb !important;
  box-shadow: none !important;
  border: 1px solid #e5e7eb;
}

::v-deep(.el-input__wrapper.is-focus) {
  background-color: #fff !important;
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1) !important;
}

::v-deep(.el-input__inner) {
  height: 48px;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  margin-top: 10px;
  border-radius: 12px;
  letter-spacing: 4px;
  text-indent: 4px;
  box-shadow: 0 8px 16px rgba(64, 158, 255, 0.2);
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 20px rgba(64, 158, 255, 0.3);
}

@media (max-width: 768px) {
  .login-container {
    flex-direction: column;
    width: 90%;
    border-radius: 16px;
  }
  .login-left {
    padding: 40px 30px;
  }
}
</style>
