<script>
export default {
  onLaunch() {
    console.log("App Launch");
  },
};
</script>

<style>
/* 全局雅致玉石与原木风设计 */
page {
  background: #F9F7F3; /* 暖白底色，如宣纸 */
  color: #2F3632; /* 墨绿色字，不刺眼 */
  font-family: -apple-system, BlinkMacSystemFont, "Helvetica Neue", Helvetica, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "微软雅黑", Arial, sans-serif;
}

.card {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 10rpx 30rpx rgba(47, 54, 50, 0.03); /* 轻柔阴影 */
  border: 1rpx solid rgba(232, 228, 219, 0.6);
  transition: all 0.3s ease;
}

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  margin: 30rpx 0 20rpx;
  color: #1A241D;
  position: relative;
  padding-left: 20rpx;
}

.section-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 6rpx;
  height: 28rpx;
  background: #3B6E53; /* 玉石绿 */
  border-radius: 4rpx;
}

.muted {
  color: #8C968F; /* 灰绿 */
  font-size: 24rpx;
}

.btn-primary {
  background: linear-gradient(135deg, #4A8365 0%, #3B6E53 100%); /* 玉石绿渐变 */
  color: #FFFFFF;
  border-radius: 999rpx;
  padding: 20rpx 32rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 500;
  box-shadow: 0 8rpx 20rpx rgba(59, 110, 83, 0.2);
  transition: opacity 0.2s;
}

.btn-primary:active {
  opacity: 0.8;
}

.btn-outline {
  background: transparent;
  color: #3B6E53;
  border: 2rpx solid #3B6E53;
  border-radius: 999rpx;
  padding: 18rpx 30rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 500;
}
</style>
