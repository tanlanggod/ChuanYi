<template>
  <view class="container">
    <view class="card tip" v-if="from === 'order-submit'">
      <view class="tip-title">地址选择模式</view>
      <view class="tip-sub">点击任意地址即可选中并返回提交订单页</view>
    </view>

    <view class="card loading-card" v-if="loading">
      <view class="muted">地址数据加载中...</view>
    </view>

    <view v-else-if="addresses.length === 0" class="empty-wrap card">
      <view class="empty-title">暂无收货地址</view>
      <view class="empty-sub">先添加一个地址，方便后续快速下单</view>
      <view class="empty-btn" @click="goEdit()">立即添加</view>
    </view>

    <view v-else class="list">
      <view class="card item" v-for="item in addresses" :key="item.id" @click="chooseAddress(item)">
        <view class="line1">
          <view class="name">{{ item.receiverName }}</view>
          <view class="phone">{{ item.receiverPhone }}</view>
          <view class="tag" v-if="item.isDefault === 1">默认</view>
        </view>
        <view class="address-text">{{ item.province }} {{ item.city }} {{ item.district }} {{ item.detailAddress }}</view>
        <view class="actions">
          <view v-if="item.isDefault !== 1" class="btn-mini" @click.stop="setDefault(item)">设为默认</view>
          <view class="btn-mini" @click.stop="goEdit(item.id)">编辑</view>
          <view class="btn-mini danger" @click.stop="remove(item)">删除</view>
        </view>
      </view>
    </view>

    <view class="btn-primary add-btn" @click="goEdit()">新增地址</view>
  </view>
</template>

<script>
import { deleteAddress, fetchAddressList, saveAddress } from "../common/api";
import { loginGuestIfNeeded } from "../common/session";

export default {
  data() {
    return {
      addresses: [],
      from: "",
      loading: false,
    };
  },
  onLoad(options) {
    this.from = (options && options.from) || "";
  },
  async onShow() {
    await this.loadAddress();
  },
  async onPullDownRefresh() {
    await this.loadAddress();
    uni.stopPullDownRefresh();
  },
  methods: {
    async loadAddress() {
      try {
        this.loading = true;
        await loginGuestIfNeeded();
        const data = await fetchAddressList();
        this.addresses = (data && data.addresses) || [];
      } catch (error) {
        uni.showToast({ title: "地址加载失败", icon: "none" });
      } finally {
        this.loading = false;
      }
    },
    goEdit(id) {
      const parts = [];
      if (id) {
        parts.push(`id=${id}`);
      }
      if (this.from) {
        parts.push(`from=${this.from}`);
      }
      const suffix = parts.length > 0 ? `?${parts.join("&")}` : "";
      uni.navigateTo({ url: `/pages-user/address-edit${suffix}` });
    },
    async setDefault(item, silent = false) {
      try {
        await saveAddress({
          id: item.id,
          receiverName: item.receiverName,
          receiverPhone: item.receiverPhone,
          province: item.province,
          city: item.city,
          district: item.district,
          detailAddress: item.detailAddress,
          isDefault: true,
        });
        if (!silent) {
          uni.showToast({ title: "已设为默认地址", icon: "none" });
        }
        await this.loadAddress();
      } catch (error) {
        if (!silent) {
          uni.showToast({ title: "设置默认失败", icon: "none" });
        }
        throw error;
      }
    },
    chooseAddress(item) {
      if (this.from !== "order-submit" || !item || !item.id) {
        return;
      }
      uni.setStorageSync("order_submit_recent_address_id", Number(item.id));
      uni.navigateBack();
    },
    remove(item) {
      uni.showModal({
        title: "删除地址",
        content: "确认删除该地址吗？",
        success: async (res) => {
          if (!res.confirm) {
            return;
          }
          try {
            await deleteAddress(item.id);
            uni.showToast({ title: "删除成功", icon: "none" });
            await this.loadAddress();
            const deletedDefault = Number(item.isDefault) === 1;
            const hasDefault = this.addresses.some((address) => Number(address.isDefault) === 1);
            if (deletedDefault && this.addresses.length > 0 && !hasDefault) {
              await this.setDefault(this.addresses[0], true);
              uni.showToast({ title: "已自动设置默认地址", icon: "none" });
            }
          } catch (error) {
            uni.showToast({ title: "删除失败", icon: "none" });
          }
        },
      });
    },
  },
};
</script>

<style scoped>
.container {
  padding: 24rpx;
  padding-bottom: 180rpx;
  background: #F9F7F3;
  min-height: 100vh;
}

.tip {
  margin-bottom: 24rpx;
  background: linear-gradient(135deg, #F0F5F2 0%, #E3EBE6 100%);
  border: 1rpx solid #D1E0D7;
  border-radius: 20rpx;
  padding: 24rpx 30rpx;
}

.tip-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #3B6E53;
}

.tip-sub {
  margin-top: 8rpx;
  color: #556B5D;
  font-size: 24rpx;
}

.loading-card {
  min-height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(47, 54, 50, 0.04);
}

.empty-wrap {
  min-height: 400rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  background: #fff;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(47, 54, 50, 0.04);
}

.empty-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1A241D;
  margin-bottom: 12rpx;
}

.empty-sub {
  color: #8C968F;
  font-size: 26rpx;
  margin-bottom: 24rpx;
}

.empty-btn {
  border-radius: 999rpx;
  background: linear-gradient(135deg, #4A8365 0%, #3B6E53 100%);
  color: #fff;
  font-size: 26rpx;
  padding: 16rpx 40rpx;
  font-weight: 600;
  box-shadow: 0 4rpx 12rpx rgba(59, 110, 83, 0.2);
}

.list {
  display: grid;
  gap: 20rpx;
}

.item {
  border-radius: 20rpx;
  background: #fff;
  padding: 30rpx;
  box-shadow: 0 6rpx 16rpx rgba(47, 54, 50, 0.04);
  border: 1rpx solid rgba(232, 228, 219, 0.6);
  position: relative;
  overflow: hidden;
  transition: transform 0.2s;
}

.item:active {
  transform: scale(0.98);
}

.line1 {
  display: flex;
  gap: 16rpx;
  align-items: center;
  margin-bottom: 12rpx;
}

.name {
  font-size: 32rpx;
  font-weight: 700;
  color: #1A241D;
}

.phone {
  font-size: 30rpx;
  color: #2F3632;
  font-weight: 600;
  font-family: -apple-system, BlinkMacSystemFont, "Helvetica Neue", Helvetica, Arial, sans-serif;
}

.tag {
  color: #3B6E53;
  background: #F0F5F2;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
  font-size: 20rpx;
  border: 1rpx solid #D1E0D7;
}

.address-text {
  color: #7A8B7C;
  line-height: 1.6;
  font-size: 26rpx;
  margin-bottom: 24rpx;
}

.actions {
  display: flex;
  gap: 16rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid rgba(232, 228, 219, 0.4);
}

.btn-mini {
  border-radius: 999rpx;
  background: #F9F7F3;
  color: #556B5D;
  padding: 10rpx 24rpx;
  font-size: 24rpx;
  font-weight: 500;
  border: 1rpx solid #E8E4DB;
  transition: all 0.2s;
}

.btn-mini:active {
  background: #F0F5F2;
}

.btn-mini.danger {
  background: #FDFBF7;
  color: #A68754;
  border-color: #E8E4DB;
}

.add-btn {
  position: fixed;
  left: 30rpx;
  right: 30rpx;
  bottom: calc(30rpx + env(safe-area-inset-bottom));
  box-shadow: 0 8rpx 24rpx rgba(59, 110, 83, 0.2);
  z-index: 10;
}
</style>
