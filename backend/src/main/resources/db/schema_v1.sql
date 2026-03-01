-- Core tables for ChuanYi V1 (draft)

CREATE TABLE IF NOT EXISTS user_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_no VARCHAR(64) NOT NULL,
  nickname VARCHAR(64),
  avatar_url VARCHAR(255),
  phone VARCHAR(32),
  login_type VARCHAR(20),
  status VARCHAR(20),
  created_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS admin_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password VARCHAR(128) NOT NULL,
  nickname VARCHAR(64),
  status VARCHAR(20),
  created_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS user_address (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  receiver_name VARCHAR(64) NOT NULL,
  receiver_phone VARCHAR(32) NOT NULL,
  province VARCHAR(64),
  city VARCHAR(64),
  district VARCHAR(64),
  detail_address VARCHAR(255),
  is_default TINYINT DEFAULT 0,
  created_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS component_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT,
  category_type VARCHAR(32),
  name VARCHAR(64) NOT NULL,
  sort_no INT DEFAULT 0,
  status VARCHAR(20) DEFAULT 'ENABLED'
);

CREATE TABLE IF NOT EXISTS component_sku (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sku_code VARCHAR(64),
  category_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  spec_text VARCHAR(128),
  bead_diameter_mm DECIMAL(10,2),
  price DECIMAL(10,2) NOT NULL,
  stock_qty INT DEFAULT 0,
  stock_warn_qty INT DEFAULT 0,
  sales_status VARCHAR(20) DEFAULT 'ON_SALE',
  image_url VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS design_draft (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  draft_no VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  title VARCHAR(128),
  wrist_size_cm DECIMAL(10,2),
  config_json TEXT,
  preview_image_url VARCHAR(255),
  price_preview DECIMAL(10,2),
  status VARCHAR(20),
  version INT DEFAULT 1,
  created_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS design_snapshot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  snapshot_no VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  source_draft_id BIGINT,
  wrist_size_cm DECIMAL(10,2),
  snapshot_json TEXT,
  preview_image_url VARCHAR(255),
  price_snapshot DECIMAL(10,2),
  created_at DATETIME
);

CREATE TABLE IF NOT EXISTS order_main (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  total_amount DECIMAL(10,2),
  pay_amount DECIMAL(10,2),
  order_status VARCHAR(32),
  pay_status VARCHAR(32),
  preoccupy_status VARCHAR(32),
  address_snapshot_json TEXT,
  remark VARCHAR(255),
  created_at DATETIME,
  paid_at DATETIME
);

CREATE TABLE IF NOT EXISTS order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  item_type VARCHAR(32) DEFAULT 'DIY_SNAPSHOT',
  snapshot_id BIGINT,
  sku_id BIGINT,
  item_name VARCHAR(128),
  item_image_url VARCHAR(255),
  unit_price DECIMAL(10,2),
  qty INT,
  amount DECIMAL(10,2),
  item_snapshot_json TEXT
);

CREATE TABLE IF NOT EXISTS stock_preoccupy (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  biz_no VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(32),
  expired_at DATETIME,
  created_at DATETIME
);

CREATE TABLE IF NOT EXISTS stock_preoccupy_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  preoccupy_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  qty INT NOT NULL
);

CREATE TABLE IF NOT EXISTS payment_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  pay_no VARCHAR(64) NOT NULL,
  order_id BIGINT NOT NULL,
  channel VARCHAR(32),
  pay_status VARCHAR(32),
  req_json TEXT,
  resp_json TEXT,
  created_at DATETIME,
  paid_at DATETIME
);

CREATE TABLE IF NOT EXISTS order_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  from_status VARCHAR(32),
  to_status VARCHAR(32),
  action VARCHAR(64),
  operator_type VARCHAR(32),
  operator_id BIGINT,
  remark VARCHAR(255),
  created_at DATETIME
);

CREATE TABLE IF NOT EXISTS banner_content (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128),
  image_url VARCHAR(255),
  jump_type VARCHAR(32),
  jump_value VARCHAR(255),
  sort_no INT DEFAULT 0,
  status VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS design_gallery (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  snapshot_id BIGINT,
  user_id BIGINT,
  title VARCHAR(128),
  cover_image_url VARCHAR(255),
  tags_json TEXT,
  display_status VARCHAR(20),
  published_at DATETIME
);

CREATE TABLE IF NOT EXISTS design_gallery_stat (
  gallery_id BIGINT PRIMARY KEY,
  view_count INT DEFAULT 0,
  like_count INT DEFAULT 0,
  updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS design_gallery_like (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  gallery_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at DATETIME,
  UNIQUE KEY uk_gallery_user(gallery_id, user_id),
  KEY idx_user(user_id)
);

CREATE TABLE IF NOT EXISTS goods_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT,
  name VARCHAR(64) NOT NULL,
  icon_url VARCHAR(255),
  sort_no INT DEFAULT 0,
  status VARCHAR(20) DEFAULT 'ENABLED'
);

CREATE TABLE IF NOT EXISTS goods_spu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  subtitle VARCHAR(255),
  cover_image_url VARCHAR(255),
  image_urls_json TEXT,
  min_price DECIMAL(10,2),
  max_price DECIMAL(10,2),
  sales_status VARCHAR(20),
  stock_status VARCHAR(20),
  tags_json TEXT,
  sort_no INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS goods_sku (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  spu_id BIGINT NOT NULL,
  spec_text VARCHAR(128),
  sku_image_url VARCHAR(255),
  price DECIMAL(10,2) NOT NULL,
  origin_price DECIMAL(10,2),
  stock_qty INT DEFAULT 0,
  sales_status VARCHAR(20) DEFAULT 'ON_SALE'
);

CREATE TABLE IF NOT EXISTS cart_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  item_type VARCHAR(32) NOT NULL,
  biz_no VARCHAR(64) NOT NULL,
  spu_id BIGINT,
  sku_id BIGINT,
  snapshot_no VARCHAR(64),
  title VARCHAR(128),
  image_url VARCHAR(255),
  unit_price DECIMAL(10,2),
  qty INT DEFAULT 1,
  selected TINYINT DEFAULT 1,
  validity_status VARCHAR(32) DEFAULT 'VALID',
  invalid_reason VARCHAR(255),
  created_at DATETIME,
  updated_at DATETIME
);
