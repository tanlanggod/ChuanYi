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
  padding-bottom: 140rpx;
}

.tip {
  margin-bottom: 12rpx;
  background: linear-gradient(145deg, #fff, #fff6f6);
  border: 1rpx solid #ffe3e3;
}

.tip-title {
  font-size: 27rpx;
  font-weight: 700;
  color: #b91c1c;
}

.tip-sub {
  margin-top: 6rpx;
  color: #6b7280;
  font-size: 23rpx;
}

.loading-card {
  min-height: 200rpx;
  display: grid;
  place-items: center;
}

.empty-wrap {
  min-height: 320rpx;
  display: grid;
  place-items: center;
  align-content: center;
  text-align: center;
}

.empty-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #374151;
}

.empty-sub {
  margin-top: 8rpx;
  color: #9ca3af;
  font-size: 23rpx;
}

.empty-btn {
  margin-top: 16rpx;
  border-radius: 999rpx;
  background: #d62b2b;
  color: #fff;
  font-size: 23rpx;
  padding: 10rpx 22rpx;
}

.list {
  display: grid;
  gap: 12rpx;
}

.item {
  border: 1rpx solid #eef1f5;
}

.line1 {
  display: flex;
  gap: 10rpx;
  align-items: center;
}

.name {
  font-size: 30rpx;
  font-weight: 700;
  color: #111827;
}

.phone {
  font-size: 28rpx;
  color: #374151;
}

.tag {
  color: #fff;
  background: #d62b2b;
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
}

.address-text {
  margin-top: 8rpx;
  color: #6b7280;
  line-height: 1.65;
  font-size: 23rpx;
}

.actions {
  margin-top: 12rpx;
  display: flex;
  gap: 10rpx;
}

.btn-mini {
  border-radius: 999rpx;
  background: #eef1f5;
  color: #1f2937;
  padding: 8rpx 18rpx;
  font-size: 22rpx;
}

.btn-mini.danger {
  background: #fef2f2;
  color: #b42318;
}

.add-btn {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: 30rpx;
}
</style>
