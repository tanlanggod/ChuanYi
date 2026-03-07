<script>
export default {
  onLaunch() {
    console.log("App Launch");
  },
};
</script>

<style>
:root,
page {
  --jade-bg: #f5f1e8;
  --jade-bg-soft: #fbf8f1;
  --jade-surface: #fffcf8;
  --jade-surface-2: #f6efe3;
  --jade-text: #1f2f27;
  --jade-text-2: #55695d;
  --jade-text-3: #8e9a90;
  --jade-primary: #3e6f57;
  --jade-primary-2: #5a8c72;
  --jade-gold: #b99864;
  --jade-border: rgba(178, 161, 130, 0.22);
  --jade-shadow: 0 12rpx 28rpx rgba(36, 56, 45, 0.08);
  --jade-shadow-soft: 0 6rpx 16rpx rgba(36, 56, 45, 0.05);
  --jade-radius-lg: 24rpx;
  --jade-radius-md: 16rpx;
  --jade-radius-pill: 999rpx;
}

page {
  background: radial-gradient(circle at 20% -10%, #fffdf8 0%, var(--jade-bg) 44%, #efe8da 100%);
  color: var(--jade-text);
  font-family: "PingFang SC", "Microsoft YaHei", "Noto Serif SC", sans-serif;
}

page::before {
  content: "";
  position: fixed;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(120deg, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0) 42%),
    radial-gradient(circle at 80% 12%, rgba(62, 111, 87, 0.07) 0%, transparent 35%);
  opacity: 0.9;
}

.card {
  background: linear-gradient(160deg, var(--jade-surface) 0%, #f9f4ea 100%);
  border-radius: var(--jade-radius-lg);
  padding: 30rpx;
  border: 1rpx solid var(--jade-border);
  box-shadow: var(--jade-shadow-soft);
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.card:active {
  transform: translateY(1rpx) scale(0.996);
}

.section-title {
  margin: 30rpx 0 20rpx;
  padding-left: 20rpx;
  font-size: 34rpx;
  font-weight: 700;
  color: var(--jade-text);
  letter-spacing: 1rpx;
  position: relative;
}

.section-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 7rpx;
  height: 30rpx;
  background: linear-gradient(180deg, var(--jade-gold) 0%, var(--jade-primary) 100%);
  border-radius: 10rpx;
}

.muted {
  color: var(--jade-text-3);
  font-size: 24rpx;
}

.btn-primary {
  border-radius: var(--jade-radius-pill);
  padding: 20rpx 32rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, var(--jade-primary-2) 0%, var(--jade-primary) 55%, #2f5d47 100%);
  box-shadow: 0 10rpx 20rpx rgba(62, 111, 87, 0.24);
  transition: transform 0.18s ease, box-shadow 0.18s ease, opacity 0.18s ease;
}

.btn-primary:active {
  transform: scale(0.98);
  box-shadow: 0 6rpx 14rpx rgba(62, 111, 87, 0.2);
}

.btn-outline {
  border-radius: var(--jade-radius-pill);
  padding: 18rpx 30rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 500;
  color: var(--jade-primary);
  border: 2rpx solid rgba(62, 111, 87, 0.68);
  background: rgba(255, 252, 248, 0.75);
}

.load-in {
  animation: fadeRise 0.35s ease both;
}

@keyframes fadeRise {
  0% {
    opacity: 0;
    transform: translateY(8rpx);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
