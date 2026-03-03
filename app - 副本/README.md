# ChuanYi App（uni-app）

`app` 目录是可直接运行的 uni-app 项目，已接入后端 API 并覆盖核心业务流程。

## 启动方式

1. 安装依赖：
   - `npm install`
2. 启动 H5 开发环境：
   - `npm run dev:h5`
3. 打包 H5：
   - `npm run build:h5`

## 已实现页面

Tab 页面：
- `pages/home/index`
- `pages/goods/index`
- `pages/diy/index`
- `pages/cart/index`
- `pages/profile/index`

业务页面：
- `pages/index/index`（启动页）
- `pages-diy/editor`
- `pages-diy/designs`
- `pages-diy/snapshot-detail`
- `pages-goods/detail`
- `pages-order/list`
- `pages-order/submit`
- `pages-order/detail`
- `pages-user/address-list`
- `pages-user/address-edit`
- `pages-user/help`
- `pages-user/terms`

## 当前流程覆盖

1. DIY 主流程：
- 编辑草稿 -> 保存草稿 -> 生成快照 -> 定制商品详情 -> 提交订单 -> 模拟支付 -> 订单详情

2. 好物 + 购物车流程：
- 好物列表（搜索/排序/分类）-> 商品详情 -> 加入购物车 -> 购物车下单 -> 订单详情
- 好物详情支持“立即购买”（直下单）-> 订单详情

3. 设计广场流程：
- 首页设计广场列表 -> 广场详情 -> 一键进入 DIY

4. 用户中心流程：
- 订单列表与状态筛选
- 未支付订单取消
- 订单详情含物流轨迹查看（发货后）
- 地址 CRUD（列表/新增/编辑/删除/默认地址）
- 帮助中心与条款页面

5. 启动流程：
- 启动页（品牌引导）-> 自动跳转首页（可手动跳过）

## API 配置

- API 封装：`src/common/api.js`
- HTTP 封装：`src/common/http.js`
- 会话封装：`src/common/session.js`
- H5 代理配置：`vite.config.js`（`/api`、`/uploads` -> `http://127.0.0.1:8080`）
- 静态资源地址可通过 `VITE_ASSET_BASE_URL` 覆盖（默认 `http://127.0.0.1:8080`）

## 说明

`loginGuestIfNeeded()` 会优先使用演示手机号 `15700000000` 登录，以便直接看到初始化地址和订单数据。
若失败则回退为游客登录。
