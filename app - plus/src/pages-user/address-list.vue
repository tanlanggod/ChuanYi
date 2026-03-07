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
  min-height: 100vh;
  padding: 24rpx;
  padding-bottom: 180rpx;
}

.tip,
.loading-card,
.empty-wrap,
.item {
  border-radius: 24rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.2);
  background: linear-gradient(160deg, #fffdfa 0%, #f8f1e6 100%);
  box-shadow: 0 10rpx 22rpx rgba(35, 56, 45, 0.08);
}

.tip {
  margin-bottom: 24rpx;
  padding: 24rpx 28rpx;
}

.tip-title {
  color: #3e6f57;
  font-size: 28rpx;
  font-weight: 700;
}

.tip-sub {
  margin-top: 8rpx;
  color: #5f7367;
  font-size: 24rpx;
}

.loading-card {
  min-height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-wrap {
  min-height: 400rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.empty-title {
  margin-bottom: 10rpx;
  color: #1f2f27;
  font-size: 32rpx;
  font-weight: 700;
}

.empty-sub {
  margin-bottom: 24rpx;
  color: #8a968d;
  font-size: 26rpx;
}

.empty-btn {
  padding: 16rpx 40rpx;
  border-radius: 999rpx;
  color: #fff;
  font-size: 26rpx;
  font-weight: 600;
  background: linear-gradient(135deg, #5a8c72 0%, #3e6f57 100%);
  box-shadow: 0 8rpx 16rpx rgba(35, 56, 45, 0.16);
}

.list {
  display: grid;
  gap: 20rpx;
}

.item {
  padding: 28rpx;
  position: relative;
  overflow: hidden;
}

.item:active {
  transform: scale(0.985);
}

.line1 {
  margin-bottom: 10rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.name {
  color: #1f2f27;
  font-size: 32rpx;
  font-weight: 700;
}

.phone {
  color: #2f463b;
  font-size: 30rpx;
  font-weight: 600;
}

.tag {
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(62, 111, 87, 0.24);
  background: #edf4ef;
  color: #3e6f57;
  font-size: 20rpx;
}

.address-text {
  margin-bottom: 20rpx;
  color: #5d7265;
  font-size: 26rpx;
  line-height: 1.58;
}

.actions {
  padding-top: 18rpx;
  border-top: 1rpx solid rgba(185, 152, 100, 0.2);
  display: flex;
  gap: 12rpx;
  flex-wrap: wrap;
}

.btn-mini {
  padding: 10rpx 22rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(185, 152, 100, 0.26);
  background: rgba(255, 252, 246, 0.9);
  color: #5a7265;
  font-size: 24rpx;
  font-weight: 600;
}

.btn-mini:active {
  background: #f2eadc;
}

.btn-mini.danger {
  background: #fff3e1;
  color: #a68450;
  border-color: rgba(185, 152, 100, 0.32);
}

.add-btn {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: calc(24rpx + env(safe-area-inset-bottom));
  box-shadow: 0 8rpx 18rpx rgba(35, 56, 45, 0.16);
  z-index: 10;
}
</style>
