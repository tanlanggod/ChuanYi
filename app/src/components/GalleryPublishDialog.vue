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
  background: rgba(0, 0, 0, 0.42);
  display: flex;
  align-items: flex-end;
}

.dialog-card {
  width: 100%;
  background: #fff;
  border-top-left-radius: 24rpx;
  border-top-right-radius: 24rpx;
  padding: 26rpx 24rpx calc(24rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.dialog-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #111827;
}

.field {
  margin-top: 18rpx;
}

.label {
  font-size: 24rpx;
  color: #4b5563;
  margin-bottom: 8rpx;
}

.input {
  width: 100%;
  min-height: 74rpx;
  border-radius: 12rpx;
  background: #f4f6f9;
  padding: 0 14rpx;
  font-size: 26rpx;
  box-sizing: border-box;
}

.tip {
  margin-top: 6rpx;
  color: #9ca3af;
  font-size: 21rpx;
}

.actions {
  margin-top: 24rpx;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12rpx;
}

.btn-cancel,
.btn-submit {
  min-height: 74rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: 700;
}

.btn-cancel {
  border: 1px solid #d1d5db;
  color: #4b5563;
  background: #fff;
}

.btn-submit {
  background: #d62b2b;
  color: #fff;
}

.btn-submit.disabled {
  opacity: 0.65;
}
</style>
