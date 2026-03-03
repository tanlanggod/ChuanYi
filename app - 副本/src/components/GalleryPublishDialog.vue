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
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background: rgba(47, 54, 50, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: flex-end;
}

.dialog-card {
  width: 100%;
  background: #F9F7F3;
  border-top-left-radius: 40rpx;
  border-top-right-radius: 40rpx;
  padding: 40rpx 40rpx calc(40rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  box-shadow: 0 -10rpx 40rpx rgba(0,0,0,0.1);
}

.dialog-title {
  font-size: 36rpx;
  font-weight: 800;
  color: #1A241D;
  margin-bottom: 30rpx;
  text-align: center;
  letter-spacing: 2rpx;
  position: relative;
}

.dialog-title::after {
  content: "";
  position: absolute;
  bottom: -16rpx;
  left: 50%;
  transform: translateX(-50%);
  width: 40rpx;
  height: 6rpx;
  background: #C2A578;
  border-radius: 6rpx;
}

.field {
  margin-top: 36rpx;
}

.label {
  font-size: 26rpx;
  color: #556B5D;
  margin-bottom: 12rpx;
  font-weight: 500;
}

.input {
  width: 100%;
  min-height: 88rpx;
  border-radius: 16rpx;
  background: #fff;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #2F3632;
  box-sizing: border-box;
  border: 1rpx solid rgba(232, 228, 219, 0.8);
  box-shadow: 0 4rpx 12rpx rgba(47, 54, 50, 0.02) inset;
  transition: all 0.2s;
}

.input:focus {
  border-color: #3B6E53;
  background: #FDFBF7;
}

.tip {
  margin-top: 10rpx;
  color: #8C968F;
  font-size: 22rpx;
  letter-spacing: 1rpx;
}

.actions {
  margin-top: 48rpx;
  display: grid;
  grid-template-columns: 1fr 1.5fr;
  gap: 20rpx;
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
  transition: all 0.2s;
}

.btn-cancel {
  border: 2rpx solid #E8E4DB;
  color: #7A8B7C;
  background: #fff;
}

.btn-submit {
  background: linear-gradient(135deg, #4A8365 0%, #3B6E53 100%);
  color: #fff;
  box-shadow: 0 8rpx 20rpx rgba(59, 110, 83, 0.2);
}

.btn-cancel:active,
.btn-submit:active {
  transform: scale(0.98);
}

.btn-submit.disabled {
  background: #D1E0D7;
  color: #F0F5F2;
  box-shadow: none;
  pointer-events: none;
}
</style>
