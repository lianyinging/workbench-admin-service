-- 同事分类（一层）+ 同事挂 category_id；下线独立「分类管理」菜单
-- 默认库：application-dev.yml 所连库（cm_opc_workbench）

CREATE TABLE IF NOT EXISTS opc_coworker_category (
    category_id      bigint         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        varchar(20)    DEFAULT '000000'        COMMENT '租户编号',
    category_name    varchar(64)    NOT NULL               COMMENT '分类名称',
    sort_order       int            DEFAULT 0               COMMENT '排序，越小越前',
    status           char(1)        DEFAULT '1'             COMMENT '状态（1正常 0停用）',
    remark           varchar(500)   DEFAULT NULL            COMMENT '备注',
    del_flag         char(1)        DEFAULT '0'             COMMENT '删除标志',
    create_dept      bigint         DEFAULT NULL            COMMENT '创建部门',
    create_by        bigint         DEFAULT NULL            COMMENT '创建者',
    create_time      datetime       DEFAULT NULL            COMMENT '创建时间',
    update_by        bigint         DEFAULT NULL            COMMENT '更新者',
    update_time      datetime       DEFAULT NULL            COMMENT '更新时间',
    PRIMARY KEY (category_id),
    UNIQUE KEY uk_opc_cw_cat_name (tenant_id, category_name),
    KEY idx_opc_cw_cat_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OPC同事分类（一层）';

-- 同事挂分类（可空 = 未分类）
SET @col_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'opc_coworker'
      AND COLUMN_NAME = 'category_id'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE opc_coworker ADD COLUMN category_id bigint DEFAULT NULL COMMENT ''所属分类ID'' AFTER group_key, ADD KEY idx_opc_coworker_category (category_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 下线独立分类管理菜单
UPDATE sys_menu SET visible = '0', status = '0' WHERE menu_id = 15157;

-- 分类按钮挂在同事列表下
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, component_name, query_param,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    hidden_expression, shop_expression, create_dept, create_by, create_time, update_by, update_time, remark
) VALUES
(15158, '同事分类查询', 15155, 5, '#', '', '', '', 0, 1, 'F', '1', '1', 'system:coworkerCategory:query', '#', NULL, NULL, 103, 1, NOW(), NULL, NULL, ''),
(15159, '同事分类新增', 15155, 6, '#', '', '', '', 0, 1, 'F', '1', '1', 'system:coworkerCategory:add', '#', NULL, NULL, 103, 1, NOW(), NULL, NULL, ''),
(15170, '同事分类修改', 15155, 7, '#', '', '', '', 0, 1, 'F', '1', '1', 'system:coworkerCategory:edit', '#', NULL, NULL, 103, 1, NOW(), NULL, NULL, ''),
(15171, '同事分类删除', 15155, 8, '#', '', '', '', 0, 1, 'F', '1', '1', 'system:coworkerCategory:remove', '#', NULL, NULL, 103, 1, NOW(), NULL, NULL, '')
ON DUPLICATE KEY UPDATE perms = VALUES(perms), parent_id = VALUES(parent_id), visible = '1', status = '1';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 15158), (1, 15159), (1, 15170), (1, 15171);

-- 默认同事分类（截图清单，幂等按名称）
INSERT INTO opc_coworker_category (
    tenant_id, category_name, sort_order, status, remark, del_flag, create_dept, create_by, create_time
) VALUES
('000000', '产品设计', 1,  '1', NULL, '0', 103, 1, NOW()),
('000000', '技术工程', 2,  '1', NULL, '0', 103, 1, NOW()),
('000000', '金融投资', 3,  '1', NULL, '0', 103, 1, NOW()),
('000000', '全球发展', 4,  '1', NULL, '0', 103, 1, NOW()),
('000000', '教育学习', 5,  '1', NULL, '0', 103, 1, NOW()),
('000000', '游戏空间', 6,  '1', NULL, '0', 103, 1, NOW()),
('000000', '数据智能', 7,  '1', NULL, '0', 103, 1, NOW()),
('000000', '营销增长', 8,  '1', NULL, '0', 103, 1, NOW()),
('000000', '内容创作', 9,  '1', NULL, '0', 103, 1, NOW()),
('000000', '销售商务', 10, '1', NULL, '0', 103, 1, NOW()),
('000000', '运营人力', 11, '1', NULL, '0', 103, 1, NOW()),
('000000', '健康医疗', 12, '1', NULL, '0', 103, 1, NOW()),
('000000', '法务安全', 13, '1', NULL, '0', 103, 1, NOW()),
('000000', '项目质量', 14, '1', NULL, '0', 103, 1, NOW()),
('000000', '行业顾问', 15, '1', NULL, '0', 103, 1, NOW())
ON DUPLICATE KEY UPDATE
    sort_order = VALUES(sort_order),
    status = '1',
    del_flag = '0',
    update_time = NOW();
