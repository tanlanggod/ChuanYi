<template>
  <view class="page">
    <view class="card header-card">
      <view class="title">{{ isEdit ? "编辑地址" : "新增地址" }}</view>
      <view class="subtitle">请填写完整收货信息，确保配送顺利</view>
    </view>

    <view class="card form-card">
      <view class="field">
        <view class="label">收货人</view>
        <input
          v-model="form.receiverName"
          class="input-control"
          maxlength="30"
          placeholder="请输入收货人姓名"
        />
      </view>

      <view class="field">
        <view class="label">手机号</view>
        <input
          v-model="form.receiverPhone"
          class="input-control"
          type="digit"
          maxlength="11"
          placeholder="请输入手机号"
        />
      </view>

      <view class="field">
        <view class="label">所在地区</view>
        <picker mode="region" :value="regionPickerValue" @change="onRegionChange">
          <view class="picker-control" :class="{ placeholder: !regionText }">
            {{ regionText || "请选择省 / 市 / 区" }}
          </view>
        </picker>
        <view class="field-tip">若弹窗为空白，可直接在下方手动填写省/市/区</view>
      </view>

      <view class="field">
        <view class="label">手动填写省 / 市 / 区（兜底）</view>
        <view class="region-grid">
          <input v-model="form.province" class="input-control region-cell" maxlength="20" placeholder="省" />
          <input v-model="form.city" class="input-control region-cell" maxlength="20" placeholder="市" />
          <input v-model="form.district" class="input-control region-cell" maxlength="20" placeholder="区" />
        </view>
      </view>

      <view class="field">
        <view class="label">详细地址</view>
        <textarea
          v-model="form.detailAddress"
          class="textarea-control"
          maxlength="120"
          auto-height
          placeholder="请输入详细地址（街道、门牌号等）"
        ></textarea>
      </view>

      <view class="default-row">
        <view class="default-texts">
          <view class="default-title">设为默认地址</view>
          <view class="default-sub">下单时优先使用该地址</view>
        </view>
        <switch :checked="form.isDefault" color="#d62b2b" @change="onDefaultChange" />
      </view>
    </view>

    <view class="action-wrap">
      <view class="btn-primary action-btn" :class="{ disabled: saving }" @click="submit">
        {{ saving ? "保存中..." : submitText }}
      </view>
    </view>
  </view>
</template>

<script>
import { fetchAddressList, saveAddress } from "../common/api";
import { loginGuestIfNeeded } from "../common/session";

export default {
  data() {
    return {
      id: null,
      from: "",
      saving: false,
      form: {
        receiverName: "",
        receiverPhone: "",
        province: "",
        city: "",
        district: "",
        detailAddress: "",
        isDefault: false,
      },
    };
  },
  computed: {
    isEdit() {
      return !!this.id;
    },
    submitText() {
      return this.isEdit ? "保存修改" : "保存地址";
    },
    regionText() {
      const province = (this.form.province || "").trim();
      const city = (this.form.city || "").trim();
      const district = (this.form.district || "").trim();
      if (!province || !city || !district) {
        return "";
      }
      return `${province} ${city} ${district}`;
    },
    regionPickerValue() {
      if (this.regionText) {
        return [this.form.province, this.form.city, this.form.district];
      }
      return ["北京市", "北京市", "东城区"];
    },
  },
  async onLoad(options) {
    this.id = options && options.id ? Number(options.id) : null;
    this.from = (options && options.from) || "";
    await this.initData();
  },
  methods: {
    async initData() {
      try {
        await loginGuestIfNeeded();
        if (!this.id) {
          return;
        }
        const data = await fetchAddressList();
        const list = (data && data.addresses) || [];
        const current = list.find((item) => Number(item.id) === Number(this.id));
        if (!current) {
          uni.showToast({ title: "地址不存在", icon: "none" });
          return;
        }
        this.form.receiverName = current.receiverName || "";
        this.form.receiverPhone = current.receiverPhone || "";
        this.form.province = current.province || "";
        this.form.city = current.city || "";
        this.form.district = current.district || "";
        this.form.detailAddress = current.detailAddress || "";
        this.form.isDefault = Number(current.isDefault) === 1;
      } catch (_error) {
        uni.showToast({ title: "地址数据加载失败", icon: "none" });
      }
    },
    onRegionChange(e) {
      const values = (e && e.detail && e.detail.value) || [];
      if (!Array.isArray(values) || values.length < 3) {
        uni.showToast({ title: "地区选择失败，请手动填写", icon: "none" });
        return;
      }
      this.form.province = String(values[0] || "").trim();
      this.form.city = String(values[1] || "").trim();
      this.form.district = String(values[2] || "").trim();
    },
    onDefaultChange(e) {
      this.form.isDefault = !!(e && e.detail && e.detail.value);
    },
    buildPayload() {
      return {
        id: this.id || undefined,
        receiverName: (this.form.receiverName || "").trim(),
        receiverPhone: (this.form.receiverPhone || "").trim(),
        province: (this.form.province || "").trim(),
        city: (this.form.city || "").trim(),
        district: (this.form.district || "").trim(),
        detailAddress: (this.form.detailAddress || "").trim(),
        isDefault: !!this.form.isDefault,
      };
    },
    validate(payload) {
      if (!payload.receiverName) {
        return "请填写收货人";
      }
      if (!payload.receiverPhone) {
        return "请填写手机号";
      }
      if (!/^1\d{10}$/.test(payload.receiverPhone)) {
        return "手机号格式不正确";
      }
      if (!payload.province || !payload.city || !payload.district) {
        return "请完整填写省/市/区";
      }
      if (!payload.detailAddress) {
        return "请填写详细地址";
      }
      return "";
    },
    async submit() {
      if (this.saving) {
        return;
      }
      const payload = this.buildPayload();
      const errorText = this.validate(payload);
      if (errorText) {
        uni.showToast({ title: errorText, icon: "none" });
        return;
      }
      this.saving = true;
      try {
        const result = await saveAddress(payload);
        const savedId = result && result.id ? Number(result.id) : this.id;
        if (this.from === "order-submit" && savedId) {
          uni.setStorageSync("order_submit_recent_address_id", savedId);
        }
        uni.showToast({ title: "保存成功", icon: "none" });
        const pages = getCurrentPages();
        if (pages && pages.length > 1) {
          setTimeout(() => {
            uni.navigateBack();
          }, 300);
        } else {
          setTimeout(() => {
            uni.redirectTo({ url: "/pages-user/address-list" });
          }, 300);
        }
      } catch (_error) {
        uni.showToast({ title: "保存失败", icon: "none" });
      } finally {
        this.saving = false;
      }
    },
  },
};
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx;
  box-sizing: border-box;
  background: #F9F7F3;
}

.header-card {
  margin-bottom: 24rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(47, 54, 50, 0.04);
  border: 1rpx solid rgba(232, 228, 219, 0.6);
}

.title {
  font-size: 40rpx;
  line-height: 1.3;
  font-weight: 800;
  color: #1A241D;
  position: relative;
  padding-left: 20rpx;
}

.title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 8rpx;
  height: 30rpx;
  background: #C2A578;
  border-radius: 4rpx;
}

.subtitle {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #7A8B7C;
}

.form-card {
  padding: 30rpx;
  background: #fff;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(47, 54, 50, 0.04);
  border: 1rpx solid rgba(232, 228, 219, 0.6);
}

.field {
  margin-top: 24rpx;
}

.field:first-child {
  margin-top: 0;
}

.label {
  font-size: 28rpx;
  font-weight: 600;
  color: #2F3632;
  margin-bottom: 12rpx;
}

.input-control,
.picker-control,
.textarea-control {
  width: 100%;
  box-sizing: border-box;
  background: #F9F7F3;
  border: 1rpx solid rgba(232, 228, 219, 0.8);
  border-radius: 12rpx;
  font-size: 30rpx;
  color: #1A241D;
  transition: border-color 0.2s;
}

.input-control:focus,
.textarea-control:focus {
  border-color: #3B6E53;
  background: #FDFBF7;
}

.input-control,
.picker-control {
  height: 88rpx;
  line-height: 88rpx;
  padding: 0 24rpx;
}

.textarea-control {
  min-height: 180rpx;
  line-height: 1.6;
  padding: 24rpx;
}

.picker-control.placeholder {
  color: #8C968F;
}

.field-tip {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #8C968F;
}

.region-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16rpx;
}

.region-cell {
  min-width: 0;
  text-align: center;
}

.default-row {
  margin-top: 30rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid rgba(232, 228, 219, 0.4);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.default-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1A241D;
}

.default-sub {
  margin-top: 8rpx;
  color: #7A8B7C;
  font-size: 24rpx;
}

.action-wrap {
  margin-top: 40rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
}

.action-btn {
  font-size: 32rpx;
  line-height: 1;
  padding: 26rpx 20rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #4A8365 0%, #3B6E53 100%);
  color: #fff;
  font-weight: 600;
  text-align: center;
  box-shadow: 0 8rpx 20rpx rgba(59, 110, 83, 0.2);
  transition: all 0.2s ease;
}

.action-btn:active {
  transform: scale(0.98);
}

.action-btn.disabled {
  background: #D1E0D7;
  color: #F0F5F2;
  box-shadow: none;
  pointer-events: none;
}
</style>
