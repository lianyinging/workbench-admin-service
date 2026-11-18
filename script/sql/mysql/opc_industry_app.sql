-- OPC 行业应用配置（默认跟 application-dev.yml 所连库）
CREATE TABLE IF NOT EXISTS opc_industry_app (
    app_id           bigint         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        varchar(20)    DEFAULT '000000'        COMMENT '租户编号',
    app_key          varchar(64)    NOT NULL               COMMENT '应用唯一键（与本地 plugin id 可对齐）',
    title            varchar(128)   NOT NULL               COMMENT '标题',
    subtitle         varchar(255)   DEFAULT ''              COMMENT '副标题',
    category         varchar(32)    DEFAULT 'global'        COMMENT '分类',
    tags             varchar(500)   DEFAULT ''              COMMENT '标签，逗号分隔',
    icon             varchar(64)    DEFAULT 'sparkle'       COMMENT '图标名（无 logo 时回退）',
    logo             mediumtext     DEFAULT NULL            COMMENT 'Logo base64(data URL)',
    tone             varchar(32)    DEFAULT '#1d4ed8'       COMMENT '色调',
    mode             varchar(16)    NOT NULL               COMMENT 'embed|plugin|external',
    url              varchar(512)   DEFAULT NULL            COMMENT 'Web/外开 URL（embed/external）',
    plugin_id        varchar(64)    DEFAULT NULL            COMMENT '本地插件 id（plugin）',
    visibility       varchar(16)    DEFAULT 'subscribe'     COMMENT 'public公开 login登录 subscribe登录且订阅',
    sort_order       int            DEFAULT 0               COMMENT '排序，越小越前',
    status           char(1)        DEFAULT '1'             COMMENT '状态（1启用 0停用）',
    remark           varchar(500)   DEFAULT NULL            COMMENT '备注',
    del_flag         char(1)        DEFAULT '0'             COMMENT '删除标志（0存在 1删除）',
    create_dept      bigint         DEFAULT NULL            COMMENT '创建部门',
    create_by        bigint         DEFAULT NULL            COMMENT '创建者',
    create_time      datetime       DEFAULT NULL            COMMENT '创建时间',
    update_by        bigint         DEFAULT NULL            COMMENT '更新者',
    update_time      datetime       DEFAULT NULL            COMMENT '更新时间',
    PRIMARY KEY (app_id),
    UNIQUE KEY uk_opc_industry_app_key (tenant_id, app_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OPC行业应用';

-- 种子：现有桌面端两例
INSERT INTO opc_industry_app (
    tenant_id, app_key, title, subtitle, category, tags, icon, tone, mode, url, plugin_id,
    visibility, sort_order, status, remark, del_flag, create_dept, create_by, create_time, update_by, update_time
) SELECT
    '000000', 'ai_ecommerce', 'AI 电商工作台', '选品 / 采购 / 刊登一体化工作台',
    'global', 'AI,选品', 'sparkle', '#1d4ed8', 'embed', 'http://118.253.178.37:3030/', NULL,
    'subscribe', 10, '1', 'Web 内嵌', '0', 103, 1, NOW(), 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM opc_industry_app WHERE app_key = 'ai_ecommerce');

INSERT INTO opc_industry_app (
    tenant_id, app_key, title, subtitle, category, tags, icon, tone, mode, url, plugin_id,
    visibility, sort_order, status, remark, del_flag, create_dept, create_by, create_time, update_by, update_time
) SELECT
    '000000', 'sample_a', '行业应用 A', '本地插件示例（非 iframe）',
    'ops', '示例,插件', 'wrench', '#0f766e', 'plugin', NULL, 'sample_a',
    'subscribe', 20, '1', '本地 plugin', '0', 103, 1, NOW(), 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM opc_industry_app WHERE app_key = 'sample_a');

-- 菜单：客户端管理(15100) -> 行业应用
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, component_name, query_param,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    hidden_expression, shop_expression, create_dept, create_by, create_time, update_by, update_time, remark
) VALUES (
    15140, '行业应用', 15100, 4, 'industryApp', 'system/industryApp/index', 'IndustryApp', '',
    0, 1, 'C', '1', '1', 'system:industryApp:list', 'app',
    NULL, NULL, 103, 1, NOW(), NULL, NULL, 'Opc WorkBench 行业应用配置'
) ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name), parent_id = VALUES(parent_id), path = VALUES(path),
    component = VALUES(component), component_name = VALUES(component_name),
    perms = VALUES(perms), visible = '1', status = '1';

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, component_name, query_param,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    hidden_expression, shop_expression, create_dept, create_by, create_time, update_by, update_time, remark
) VALUES
(15141, '行业应用查询', 15140, 1, '#', '', '', '', 0, 1, 'F', '1', '1', 'system:industryApp:query', '#', NULL, NULL, 103, 1, NOW(), NULL, NULL, ''),
(15142, '行业应用新增', 15140, 2, '#', '', '', '', 0, 1, 'F', '1', '1', 'system:industryApp:add', '#', NULL, NULL, 103, 1, NOW(), NULL, NULL, ''),
(15143, '行业应用修改', 15140, 3, '#', '', '', '', 0, 1, 'F', '1', '1', 'system:industryApp:edit', '#', NULL, NULL, 103, 1, NOW(), NULL, NULL, ''),
(15144, '行业应用删除', 15140, 4, '#', '', '', '', 0, 1, 'F', '1', '1', 'system:industryApp:remove', '#', NULL, NULL, 103, 1, NOW(), NULL, NULL, '')
ON DUPLICATE KEY UPDATE perms = VALUES(perms), visible = '1', status = '1';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 15140), (1, 15141), (1, 15142), (1, 15143), (1, 15144);
