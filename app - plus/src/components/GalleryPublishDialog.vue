<template>
  <view class="dialog-mask" v-if="visible" @click.self="handleCancel">
    <view class="dialog-card">
      <view class="dialog-title">发布到设计广场</view>

      <view class="field">
        <view class="label">标题</view>
        <input
          class="input"
          v-model="form.title"
          maxlength="32"
          placeholder="请输入作品标题"
        />
      </view>

      <view class="field">
        <view class="label">标签</view>
        <input
          class="input"
          v-model="tagsInput"
          maxlength="80"
          placeholder="多个标签请用逗号分隔"
        />
        <view class="tip">最多 5 个标签，每个不超过 10 个字</view>
      </view>

      <view class="field">
        <view class="label">封面 URL</view>
        <input
          class="input"
          v-model="form.coverImageUrl"
          maxlength="255"
          placeholder="可选，不填使用快照封面"
        />
      </view>

      <view class="actions">
        <view class="btn-cancel" @click="handleCancel">取消</view>
        <view class="btn-submit" :class="{ disabled: submitting }" @click="handleSubmit">
          {{ submitting ? "发布中..." : "确认发布" }}
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: "GalleryPublishDialog",
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    submitting: {
      type: Boolean,
      default: false,
    },
    snapshotNo: {
      type: String,
      default: "",
    },
    defaultTitle: {
      type: String,
      default: "",
    },
    defaultTags: {
      type: Array,
      default: () => [],
    },
    defaultCoverImageUrl: {
      type: String,
      default: "",
    },
  },
  data() {
    return {
      form: {
        title: "",
        coverImageUrl: "",
      },
      tagsInput: "",
    };
  },
  watch: {
    visible: {
      immediate: true,
      handler(next) {
        if (!next) {
          return;
        }
        this.resetForm();
      },
    },
  },
  methods: {
    resetForm() {
      this.form.title = String(this.defaultTitle || "").trim();
      this.form.coverImageUrl = String(this.defaultCoverImageUrl || "").trim();
      const tags = Array.isArray(this.defaultTags) ? this.defaultTags : [];
      this.tagsInput = tags.join(", ");
    },
    normalizeTags(raw) {
      const input = String(raw || "").trim();
      if (!input) {
        return [];
      }
      const rows = input.split(/[，,]/);
      const map = {};
      const result = [];
      rows.forEach((item) => {
        const tag = String(item || "").trim();
        if (!tag) {
          return;
        }
        if (tag.length > 10) {
          throw new Error("标签不能超过 10 个字");
        }
        if (map[tag]) {
          return;
        }
        if (result.length >= 5) {
          throw new Error("最多输入 5 个标签");
        }
        map[tag] = true;
        result.push(tag);
      });
      return result;
    },
    handleCancel() {
      if (this.submitting) {
        return;
      }
      this.$emit("cancel");
    },
    handleSubmit() {
      if (this.submitting) {
        return;
      }
      const title = String(this.form.title || "").trim();
      if (!title) {
        uni.showToast({ title: "请输入标题", icon: "none" });
        return;
      }
      let tags = [];
      try {
        tags = this.normalizeTags(this.tagsInput);
      } catch (error) {
        uni.showToast({ title: error.message || "标签格式不正确", icon: "none" });
        return;
      }
      this.$emit("submit", {
        snapshotNo: String(this.snapshotNo || "").trim(),
        title,
        tags,
        coverImageUrl: String(this.form.coverImageUrl || "").trim(),
      });
    },
  },
};
</script>

<style scoped>
.dialog-mask {
  position: fixed;
  z-index: 1000;
  inset: 0;
  display: flex;
  align-items: flex-end;
  background: rgba(26, 39, 32, 0.5);
  backdrop-filter: blur(6px);
}

.dialog-card {
  width: 100%;
  box-sizing: border-box;
  border-top-left-radius: 40rpx;
  border-top-right-radius: 40rpx;
  border-top: 1rpx solid rgba(185, 152, 100, 0.24);
  background: linear-gradient(175deg, #fffdf9 0%, #f7efe3 100%);
  box-shadow: 0 -16rpx 34rpx rgba(35, 56, 45, 0.14);
  padding: 40rpx 32rpx calc(34rpx + env(safe-area-inset-bottom));
}

.dialog-title {
  margin-bottom: 28rpx;
  position: relative;
  text-align: center;
  color: #1f2f27;
  font-size: 36rpx;
  font-weight: 800;
  letter-spacing: 2rpx;
}

.dialog-title::after {
  content: "";
  position: absolute;
  left: 50%;
  bottom: -14rpx;
  transform: translateX(-50%);
  width: 42rpx;
  height: 6rpx;
  border-radius: 6rpx;
  background: linear-gradient(90deg, #c39e67 0%, #3e6f57 100%);
}

.field {
  margin-top: 34rpx;
}

.label {
  margin-bottom: 12rpx;
  color: #5f7367;
  font-size: 26rpx;
  font-weight: 600;
}

.input {
  width: 100%;
  min-height: 90rpx;
  box-sizing: border-box;
  padding: 0 22rpx;
  border-radius: 16rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.24);
  background: rgba(255, 255, 255, 0.9);
  color: #2f463b;
  font-size: 28rpx;
  box-shadow: inset 0 2rpx 8rpx rgba(35, 56, 45, 0.04);
}

.input:focus {
  border-color: rgba(62, 111, 87, 0.6);
  background: #fff;
}

.tip {
  margin-top: 10rpx;
  color: #8a968d;
  font-size: 22rpx;
}

.actions {
  margin-top: 48rpx;
  display: grid;
  grid-template-columns: 1fr 1.5fr;
  gap: 18rpx;
}

.btn-cancel,
.btn-submit {
  min-height: 88rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  font-weight: 600;
}

.btn-cancel {
  border: 1rpx solid rgba(185, 152, 100, 0.34);
  background: rgba(255, 252, 246, 0.9);
  color: #8c7248;
}

.btn-submit {
  border: 1rpx solid transparent;
  background: linear-gradient(135deg, #5a8c72 0%, #3e6f57 100%);
  color: #fff;
  box-shadow: 0 8rpx 16rpx rgba(35, 56, 45, 0.18);
}

.btn-cancel:active,
.btn-submit:active {
  transform: scale(0.98);
}

.btn-submit.disabled {
  background: #c4d3ca;
  color: #eff4f1;
  box-shadow: none;
  pointer-events: none;
}
</style>
