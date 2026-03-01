# ChuanYi Backend

Spring Boot backend scaffold for ChuanYi app.

## Tech Stack

1. Spring Boot 2.7.18
2. MyBatis-Plus 3.5.5
3. MySQL 8.x (default runtime)
4. Springdoc OpenAPI (Swagger UI)

## Run

```bash
mvn -DskipTests package
mvn spring-boot:run
```

Optional environment override:

```bash
set DB_URL=jdbc:mysql://127.0.0.1:3306/chuanyi?useUnicode=true^&characterEncoding=utf8^&serverTimezone=Asia/Shanghai^&useSSL=false^&allowPublicKeyRetrieval=true
set DB_USERNAME=root
set DB_PASSWORD=root
set REDIS_HOST=127.0.0.1
set REDIS_PORT=6379
mvn spring-boot:run
```

## Swagger

`http://localhost:8080/swagger-ui/index.html`

## Module Layout

1. `common` shared response/config/exception/util
2. `modules.auth` login API
3. `modules.component` component category/SKU APIs
4. `modules.design` draft APIs
5. `modules.snapshot` snapshot APIs
6. `modules.address` address APIs
7. `modules.order` order APIs
8. `modules.payment` mock payment APIs
9. `modules.content` banner/gallery APIs
10. `modules.logistics` logistics API
11. `modules.goods` goods APIs
12. `modules.cart` cart APIs

## Current Implementation Status

Database-backed (MyBatis-Plus):

1. `address` list/save/delete
2. `design draft` save/list/detail/delete
3. `snapshot` create-from-draft/detail
4. `order` create/create-from-cart/list/detail
5. `payment mock` pay/callback (updates order status)
6. `component` categories/list
7. `goods` categories/list/detail
8. `content` banner/gallery list
9. `cart` detail/add/update/select/preview

Still scaffold mock data:

1. `auth`
2. `logistics`

## SQL Draft

`src/main/resources/db/schema_v1.sql`
`src/main/resources/db/data_v1.sql`

请先在 MySQL 中创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS chuanyi DEFAULT CHARACTER SET utf8mb4;
```

When backend starts, schema and seed data are auto-initialized via `spring.sql.init.*`.
