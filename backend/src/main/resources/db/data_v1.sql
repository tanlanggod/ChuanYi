-- Seed data for local development

INSERT IGNORE INTO user_account (
  id, user_no, nickname, avatar_url, phone, login_type, status, created_at, updated_at
) VALUES
  (1, 'U_DEMO_0001', 'Alex', '', '15700000000', 'PHONE', 'ENABLED', NOW(), NOW());

INSERT IGNORE INTO admin_account (
  id, username, password, nickname, status, created_at, updated_at
) VALUES
  (1, 'admin', '123456', 'Super Admin', 'ENABLED', NOW(), NOW());

INSERT IGNORE INTO user_address (
  id, user_id, receiver_name, receiver_phone, province, city, district, detail_address, is_default, created_at, updated_at
) VALUES
  (1, 1, 'Alex', '15700000000', 'Guangdong', 'Zhuhai', 'Xiangzhou', 'Gangwan Apartment', 1, NOW(), NOW());

INSERT IGNORE INTO component_category (
  id, parent_id, category_type, name, sort_no, status
) VALUES
  (1, NULL, 'BEAD', 'Clear Quartz', 1, 'ENABLED'),
  (2, NULL, 'SPACER', 'Spacer', 2, 'ENABLED');

INSERT IGNORE INTO component_sku (
  id, sku_code, category_id, name, spec_text, bead_diameter_mm, price, stock_qty, stock_warn_qty, sales_status, image_url
) VALUES
  (10001, 'SKU-CQ-8', 1, 'Clear Quartz Bead', '8mm', 8.00, 5.00, 120, 10, 'ON_SALE', ''),
  (10002, 'SKU-SP-1', 2, 'Silver Spacer', '1pc', 0.00, 3.50, 80, 10, 'ON_SALE', '');

INSERT IGNORE INTO design_draft (
  id, draft_no, user_id, title, wrist_size_cm, config_json, preview_image_url, price_preview, status, version, created_at, updated_at
) VALUES
  (1, 'D_DEMO_0001', 1, 'My Design', 13.00, '{"version":1}', '', 359.50, 'ACTIVE', 1, NOW(), NOW());

INSERT IGNORE INTO design_snapshot (
  id, snapshot_no, user_id, source_draft_id, wrist_size_cm, snapshot_json, preview_image_url, price_snapshot, created_at
) VALUES
  (1, 'S_DEMO_0001', 1, 1, 13.00, '{"version":1}', '', 359.50, NOW());

INSERT IGNORE INTO goods_category (
  id, parent_id, name, icon_url, sort_no, status
) VALUES
  (1, NULL, 'Crystal Accessories', '', 1, 'ENABLED');

INSERT IGNORE INTO goods_spu (
  id, category_id, title, subtitle, cover_image_url, image_urls_json, min_price, max_price, sales_status, stock_status, tags_json, sort_no
) VALUES
  (20001, 1, '925 Silver Ring', 'Minimal Daily Wear', '', '[]', 39.90, 39.90, 'ON_SALE', 'HAS_STOCK', '["daily"]', 1);

INSERT IGNORE INTO goods_sku (
  id, spu_id, spec_text, sku_image_url, price, origin_price, stock_qty, sales_status
) VALUES
  (30001, 20001, 'silver-single', '', 39.90, 59.90, 30, 'ON_SALE');

INSERT IGNORE INTO banner_content (
  id, title, image_url, jump_type, jump_value, sort_no, status
) VALUES
  (1, 'Contact Service', '', 'NONE', '', 1, 'ENABLED');

INSERT IGNORE INTO design_gallery (
  id, snapshot_id, user_id, title, cover_image_url, tags_json, display_status, published_at
) VALUES
  (1, 1, 1, 'Bodhi Cake', '', '["quartz","minimal"]', 'ENABLED', NOW());

INSERT IGNORE INTO design_gallery_stat (
  gallery_id, view_count, like_count, updated_at
) VALUES
  (1, 12, 2, NOW());
