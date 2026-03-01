# 手串DIY App：页面状态流转与接口映射（基于现有UI截图）

## 1. 页面状态流转（V1）

### 1.1 全局导航
1. `启动页(Splash)` -> `首页`
2. 底部 Tab：`首页 / 好物 / DIY / 购物车 / 个人中心`

### 1.2 DIY 核心闭环（主流程）
1. `DIY入口页`
2. 点击`自由定制手串` -> `DIY编辑器`（创建临时草稿）
3. 编辑器中添加/调整组件，实时显示`手围`和`总价`
4. 点击`保存` -> 草稿持久化（仍停留编辑器）
5. 点击`完成设计` -> 生成`设计快照` -> 进入`定制商品详情`
6. 点击`立即购买` -> `提交订单`
7. 选择地址 + 备注 -> 点击`提交订单`
8. 后端创建订单并预占库存 -> 进入`待支付`状态
9. 模拟支付成功 -> `已支付待发货` -> 后续`已发货待收货` -> `已完成`

### 1.3 DIY 相关分支流程
1. `DIY入口页` -> `我的设计`
2. `我的设计`支持：`新增设计`、`继续设计`、`删除草稿`
3. 删除草稿不影响已下单快照订单
4. `定制商品详情`可返回编辑器二次修改，重新生成新快照再下单

### 1.4 账号与拦截规则
1. 游客可进入编辑器并操作
2. 在`保存设计`或`提交订单`前触发登录拦截
3. `我的设计`页建议登录后可见（与PRD一致）

### 1.5 辅助流程
1. `个人中心` -> `我的订单`（全部/待发货/待收货/已完成/退款售后）
2. `个人中心` -> `收货地址`（新增/编辑/删除/默认地址）
3. `好物`与`购物车`为商品补充链路（当前PRD未给完整接口细节）

## 2. 页面动作 -> 接口映射（V1）

| 页面/动作 | 方法 | 接口 | 请求要点 | 响应与后续 |
|---|---|---|---|---|
| 进入APP（游客会话） | POST | `/api/auth/login` | 设备标识或游客参数 | 返回token，后续请求带鉴权 |
| 打开DIY编辑器，加载分类 | GET | `/api/sku/categories` | 无或业务类型 | 返回分类树（白水晶/紫水晶/隔珠等） |
| 编辑器搜索/筛选SKU | GET | `/api/sku/list?categoryId=&keyword=` | 分类ID、关键词 | 返回SKU列表，含价格/库存/销售状态 |
| 编辑器点`保存` | POST | `/api/design/draft/save` | 草稿JSON、截图URL、手围、价格预览 | 返回`draftId/draftNo`，用于继续编辑 |
| 打开`我的设计`列表 | GET | `/api/design/draft/list` | 分页参数（建议） | 返回草稿网格数据 |
| 点击`继续设计` | GET | `/api/design/draft/{id}` | 草稿ID | 返回完整设计JSON回显画布 |
| 草稿删除 | DELETE | `/api/design/draft/{id}` | 草稿ID | 删除成功后刷新列表 |
| 编辑器点`完成设计` | POST | `/api/design/snapshot/create-from-draft` | draftId、确认长度校验标记 | 返回`snapshotNo`，跳详情页 |
| 打开定制商品详情 | GET | `/api/design/snapshot/{snapshotNo}` | snapshotNo | 返回快照图、价格、手围（冻结态） |
| 地址列表 | GET | `/api/address/list` | 无 | 返回地址数组和默认地址 |
| 新增/编辑地址 | POST | `/api/address/save` | 姓名/手机/省市区/详情/默认 | 保存后可回填到下单页 |
| 删除地址 | DELETE | `/api/address/{id}` | 地址ID | 删除成功并刷新 |
| 提交订单 | POST | `/api/order/create` | snapshotNo、addressId、remark | 订单创建 + 库存预占，返回orderNo和支付参数 |
| 发起模拟支付 | POST | `/api/payment/mock/pay` | orderNo、payAmount | 返回支付受理结果 |
| 模拟支付回调 | POST | `/api/payment/mock/callback` | payNo/orderNo、状态 | 更新支付与订单状态（幂等） |
| 我的订单列表 | GET | `/api/order/list?status=` | 状态筛选 | 返回对应状态订单 |
| 订单详情 | GET | `/api/order/{orderNo}` | orderNo | 返回状态流转、商品快照信息 |
| 首页Banner | GET | `/api/content/banner/list` | 无 | 首页轮播展示 |
| 设计广场列表 | GET | `/api/gallery/list` | 分页/筛选（建议） | 作品只读列表 |
| 物流查询 | GET | `/api/logistics/track?carrierCode=&trackingNo=` | 物流商编码+单号 | 返回轨迹信息 |

## 3. 状态与规则（建议直接落表）

### 3.1 订单状态
1. `PENDING_PAY`（待支付）
2. `PAID_WAIT_SHIP`（已支付待发货）
3. `SHIPPED_WAIT_RECEIVE`（已发货待收货）
4. `COMPLETED`（已完成）
5. `CANCELLED`（已取消）
6. `AFTER_SALE`（售后中）

### 3.2 关键业务规则
1. 下单必须基于`snapshot`，禁止直接用草稿结算
2. 创建订单时执行库存预占，支付超时释放
3. 支付回调必须幂等，防止重复扣减库存
4. 前端价格仅展示，最终金额以后端重算为准
5. 备注限制45字，地址为空禁止提交

## 4. 当前UI与PRD对齐结论

1. 你给的截图已覆盖V1主闭环：`设计 -> 保存/完成 -> 详情 -> 提交订单 -> 订单/地址管理`
2. 与PRD存在一处命名差异：PRD默认品牌代称“串意”，UI品牌为“养个石头”
3. `好物/购物车`链路的接口在当前PRD中未细化，建议单独补充商品与购物车接口清单

## 5. 本次补充产物

1. `好物/购物车`完整接口清单（含字段与状态）：`好物_购物车接口清单.md`
2. 后端 OpenAPI/Swagger 草案（含DIY+订单+好物/购物车）：`后端OpenAPI草案_v1.yaml`
