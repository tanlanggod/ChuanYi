# 一周迭代计划：广场互动闭环（发布 + 点赞 + 热度排序）

## 1. 摘要
本轮按你确认的方向执行：
1. 主线：`广场互动闭环`
2. 规模：`一周快速迭代`
3. 数据库：`允许结构变更`
4. 功能范围：`发布+点赞+热度排序`
5. 发布策略：`先发后审`
6. 入口：`快照详情 + 我的设计` 两个入口
7. 发布交互：`轻量弹窗`
8. 管理端：`本轮不改（只做 App + API）`

## 2. 目标与验收标准
1. 用户可从两个入口发布作品到广场，发布后立即可见。
2. 用户可点赞/取消点赞，列表和详情可见点赞数与浏览数。
3. 广场支持后端热度排序，替换当前前端“伪热度”算法。
4. 全流程具备幂等和基本防重复保护，不出现重复点赞、重复发布脏数据。
5. 不修改 Admin 页面，不影响现有订单/购物车/DIY 主流程。

## 3. 公共接口与数据结构变更（对外约定）
### 3.1 API 变更
1. `GET /api/gallery/list`
- 新增参数：`sortType`，可选值：`PUBLISH_TIME`、`HOT`，默认 `PUBLISH_TIME`。
- 响应每条新增字段：`viewCount`、`likeCount`、`isLiked`、`hotScore`。

2. `GET /api/gallery/{id}`
- 保持原路径。
- 响应新增字段：`viewCount`、`likeCount`、`isLiked`、`hotScore`。
- 行为变更：成功读取详情时自动 `viewCount + 1`（服务端累加）。

3. `POST /api/gallery/publish`（新增）
- 请求体：`snapshotNo`、`title`、`tags`、`coverImageUrl`。
- 校验：`snapshotNo` 必填，`title` 必填；`tags` 最多 5 个，每个不超过 10 字符。
- 业务规则：同一用户同一 `snapshotNo` 若已发布，则更新原记录并刷新发布时间；否则新建。
- 返回：`id`、`snapshotId`、`publishedAt`、`displayStatus`。

4. `POST /api/gallery/{id}/like`（新增）
- 行为：点赞，幂等。
- 返回：`galleryId`、`liked=true`、`likeCount`。

5. `DELETE /api/gallery/{id}/like`（新增）
- 行为：取消点赞，幂等。
- 返回：`galleryId`、`liked=false`、`likeCount`。

### 3.2 数据库变更
新增表：
1. `design_gallery_stat`
- 字段：`gallery_id(PK)`、`view_count`、`like_count`、`updated_at`。
- 索引：`PRIMARY KEY(gallery_id)`。

2. `design_gallery_like`
- 字段：`id`、`gallery_id`、`user_id`、`created_at`。
- 索引：`UNIQUE KEY uk_gallery_user(gallery_id,user_id)`，`KEY idx_user(user_id)`。

保留现有 `design_gallery`，不新增审核状态字段（先发后审由现有 `display_status` 管理）。

## 4. 后端实现计划（decision-complete）
1. 扩展内容模块实体与 Mapper：
- 新增 `DesignGalleryStatEntity`、`DesignGalleryLikeEntity`。
- 新增 `DesignGalleryStatMapper`、`DesignGalleryLikeMapper`。

2. 扩展 `ContentController`：
- 新增 publish/like/unlike 路由。
- list 支持 `sortType` 参数。

3. 重构 `ContentService`：
- `gallery(pageNo,pageSize,sortType)`：按 `sortType` 输出稳定排序。
- `galleryDetail(id)`：读取详情并原子增加浏览数。
- `publish(body)`：校验快照归属、执行 upsert 发布逻辑。
- `like(id)` / `unlike(id)`：按唯一索引实现幂等增减计数。
- 对列表页批量回填 `isLiked`，避免 N+1 查询。

4. 热度算法（固定规则）：
- `hotScore = likeCount * 10 + viewCount`。
- `HOT` 排序：`hotScore DESC, publishedAt DESC, id DESC`。

5. 错误与边界约定：
- 作品不存在或非展示状态：`404 gallery not found`。
- 发布参数非法：`400`。
- 幂等点赞重复请求：返回 `code=0`，不报错。

## 5. App 端实现计划（decision-complete）
1. 新增可复用发布弹窗组件：
- 路径：`app/src/components/GalleryPublishDialog.vue`。
- 字段：标题、标签（逗号分隔，自动去重和截断）、封面 URL（默认快照图）。
- 提交按钮：发布中状态锁定，防重复点击。

2. 快照详情页接入发布入口：
- 文件：`app/src/pages-diy/snapshot-detail.vue`。
- 在底部按钮区新增 `发布广场`。
- 发布成功后弹提示并可一键跳转广场详情。

3. 我的设计页接入发布入口：
- 文件：`app/src/pages-diy/designs.vue`。
- 草稿卡新增 `发布广场` 按钮。
- 流程：先调用 `createSnapshotFromDraft`，再弹发布框提交。

4. 广场列表页改为服务端排序与互动展示：
- 文件：`app/src/pages-gallery/list.vue`。
- 继续保留“按发布时间/按热度”切换，但改为请求参数 `sortType`。
- 移除当前本地 `buildHotScore` 排序逻辑。
- 卡片展示 `likeCount`、`viewCount`。

5. 广场详情页接入点赞：
- 文件：`app/src/pages-gallery/detail.vue`。
- 新增点赞按钮与计数显示。
- 点赞/取消后本地状态即时更新并与服务端结果对齐。

6. API 封装补齐：
- 文件：`app/src/common/api.js`。
- 新增 `publishGallery`、`likeGallery`、`unlikeGallery`。
- `fetchGallery` 增加 `sortType` 参数并保持向后兼容默认值。

## 6. 测试与验收场景
### 6.1 后端接口验收
1. 发布新作品成功，`displayStatus=ENABLED`。
2. 同快照二次发布走更新，不产生重复记录。
3. 点赞接口重复调用保持幂等，`likeCount` 不重复增长。
4. 取消点赞重复调用保持幂等，`likeCount` 不出现负数。
5. 详情访问后 `viewCount` 递增。
6. 列表 `HOT` 排序结果与 `hotScore` 一致。
7. 非展示状态作品详情/点赞返回 404。

### 6.2 App 端场景验收
1. 快照详情发布成功链路完整。
2. 我的设计发布链路完整（含先生成快照）。
3. 广场列表切换“发布时间/热度”后数据顺序正确且可分页。
4. 点赞状态在列表和详情间往返后一致。
5. 发布表单字段校验有效（空标题、超长标签拦截）。

### 6.3 回归检查
1. DIY 编辑、保存、完成设计不受影响。
2. 购物车下单、订单详情、模拟支付不受影响。
3. 管理端现有内容管理页面功能不回归。

## 7. 排期（1 周）
1. D1：数据库与后端实体/Mapper/接口骨架。
2. D2-D3：发布、点赞、热度排序服务逻辑 + 自测。
3. D4：App 发布弹窗 + 两入口接入。
4. D5：广场列表/详情互动改造 + 联调。
5. D6：回归测试 + Postman 用例补充 + 文档更新。
6. D7：缓冲与修复。

## 8. 关键假设与默认决策
1. 默认登录用户可直接发布和点赞（沿用当前登录机制）。
2. 先发后审：发布后立即展示，后续由既有内容管理状态控制下线。
3. 本轮不新增管理端页面与审核看板。
4. 同一用户同一快照只保留一个广场发布记录（重复发布为更新）。
5. 本轮不做评论、举报、风控黑名单、真实支付和真实物流接入。
