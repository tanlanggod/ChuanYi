# ChuanYi 项目骨架

本仓库按 PRD 拆分为三端：

1. `backend`：Spring Boot + MyBatis-Plus + MySQL（核心业务接口可跑通）。
2. `app`：uni-app 客户端（DIY、好物、购物车、订单、地址等页面已联通）。
3. `admin`：Vue3 + Element Plus 管理后台（登录、看板、订单、好物、内容管理已接入）。

## 当前文档

1. `UI操作逻辑与接口映射.md`
2. `好物_购物车接口清单.md`
3. `后端OpenAPI草案_v1.yaml`

## 后端启动

1. 进入目录：`cd backend`
2. 编译验证：`mvn -DskipTests package`
3. 运行：`mvn spring-boot:run`
4. Swagger UI：`http://localhost:8080/swagger-ui/index.html`
5. 默认数据库：MySQL（库名建议 `chuanyi`）

## 管理后台启动

1. 进入目录：`cd admin`
2. 安装依赖：`npm install`
3. 本地启动：`npm run dev`
4. 打包验证：`npm run build`

## App 初始化

请查看：`app/README.md`

## 当前联调闭环

1. App：保存草稿 -> 创建快照 -> 提交订单 -> 模拟支付 -> 订单详情。
2. App：好物列表/详情 -> 加入购物车 -> 购物车下单 -> 订单详情。
3. Admin：订单筛选/详情/取消/发货/完成、好物上下架、Banner/设计广场状态管理。
