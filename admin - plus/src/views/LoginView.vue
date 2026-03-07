<template>
  <div class="login-page">
    <div class="login-panel">
      <section class="login-brand">
        <div class="logo-wrap"><el-icon class="logo-icon"><GobletFull /></el-icon></div>
        <p class="brand-kicker">Admin Studio</p>
        <h1 class="brand-title">养个石头 · 后台管理</h1>
        <p class="brand-desc">浅色杂志编辑风视觉系统，统一内容、商品与订单管理体验。</p>
      </section>

      <section class="login-form-wrap">
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
      </section>
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
  padding: 32px;
  background:
    radial-gradient(circle at 12% 10%, #f9f5ef 0, transparent 42%),
    radial-gradient(circle at 90% 92%, #efe6d8 0, transparent 46%),
    var(--bg-page);
}

.login-panel {
  width: min(1040px, 100%);
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  border: 1px solid var(--line-2);
  border-radius: 24px;
  overflow: hidden;
  background: var(--surface-elevated);
  box-shadow: var(--shadow-lg);
}

.login-brand {
  padding: 48px 44px;
  background: linear-gradient(165deg, #f8f1e7 0%, #f2e8d9 100%);
  border-right: 1px solid var(--line-2);
}

.logo-wrap {
  width: 58px;
  height: 58px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, var(--brand-2) 0%, var(--brand-1) 100%);
  color: #fff;
  box-shadow: 0 10px 22px rgba(141, 106, 79, 0.34);
}

.logo-icon {
  font-size: 30px;
}

.brand-kicker {
  margin: 24px 0 8px;
  font-size: 11px;
  letter-spacing: 1.4px;
  text-transform: uppercase;
  color: var(--text-3);
}

.brand-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 40px;
  line-height: 1.12;
  color: var(--text-1);
}

.brand-desc {
  margin: 18px 0 0;
  color: var(--text-2);
  font-size: 14px;
  line-height: 1.7;
  max-width: 420px;
}

.login-form-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 36px;
  background: #fffdf9;
}

.login-card {
  width: min(380px, 100%);
  background: transparent !important;
  box-shadow: none !important;
}

.card-header {
  margin-bottom: 22px;
}

.card-header h3 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 30px;
  color: var(--text-1);
}

.card-header p {
  margin: 8px 0 0;
  color: var(--text-3);
  font-size: 13px;
}

.login-form {
  margin-top: 12px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  height: 46px;
  letter-spacing: 4px;
}

@media (max-width: 900px) {
  .login-panel {
    grid-template-columns: 1fr;
  }

  .login-brand {
    border-right: none;
    border-bottom: 1px solid var(--line-2);
  }
}
</style>
