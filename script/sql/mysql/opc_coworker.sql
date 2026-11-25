-- OPC 同事目录 + MCP 网关配置 + 用户授权（默认跟 application-dev.yml 所连库）

CREATE TABLE IF NOT EXISTS opc_coworker (
    coworker_id      bigint         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        varchar(20)    DEFAULT '000000'        COMMENT '租户编号',
    persona_key      varchar(64)    NOT NULL               COMMENT 'Persona 唯一键',
    name             varchar(128)   NOT NULL               COMMENT '展示名称',
    tagline          varchar(255)   DEFAULT ''              COMMENT '一句话',
    description      varchar(1000)  DEFAULT ''              COMMENT '描述',
    icon             varchar(64)    DEFAULT ''              COMMENT '图标',
    group_key        varchar(32)    DEFAULT 'general'       COMMENT '分组',
    version          varchar(32)    DEFAULT '1.0.0'         COMMENT '版本',
    package_zip      longtext       DEFAULT NULL            COMMENT 'zip base64（可含 data URL 前缀）',
    package_sha256   varchar(64)    DEFAULT NULL            COMMENT '包内容 SHA-256',
    package_size     bigint         DEFAULT NULL            COMMENT '包原始字节数',
    visibility       varchar(16)    DEFAULT 'subscribe'     COMMENT 'public|login|subscribe',
    sort_order       int            DEFAULT 0               COMMENT '排序，越小越前',
    status           char(1)        DEFAULT '1'             COMMENT '状态（1启用 0停用）',
    remark           varchar(500)   DEFAULT NULL            COMMENT '备注',
    del_flag         char(1)        DEFAULT '0'             COMMENT '删除标志（0存在 1删除）',
    create_dept      bigint         DEFAULT NULL            COMMENT '创建部门',
    create_by        bigint         DEFAULT NULL            COMMENT '创建者',
    create_time      datetime       DEFAULT NULL            COMMENT '创建时间',
    update_by        bigint         DEFAULT NULL            COMMENT '更新者',
    update_time      datetime       DEFAULT NULL            COMMENT '更新时间',
    PRIMARY KEY (coworker_id),
    UNIQUE KEY uk_opc_coworker_persona (tenant_id, persona_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OPC同事目录';

CREATE TABLE IF NOT EXISTS opc_coworker_mcp_tool (
    id               bigint         NOT NULL AUTO_INCREMENT COMMENT '主键',
    coworker_id      bigint         NOT NULL               COMMENT '同事ID',
    logical_mcp      varchar(128)   DEFAULT ''              COMMENT '逻辑 MCP 名',
    tool_id          varchar(128)   DEFAULT ''              COMMENT '网关 tool id',
    tool_name        varchar(128)   NOT NULL               COMMENT '工具名',
    PRIMARY KEY (id),
    KEY idx_opc_coworker_mcp_tool_cw (coworker_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='同事可绑 MCP 工具上限';

CREATE TABLE IF NOT EXISTS opc_desktop_user_coworker (
    id               bigint         NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id          bigint         NOT NULL               COMMENT '桌面用户ID',
    coworker_id      bigint         NOT NULL               COMMENT '同事ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_desktop_user_coworker (user_id, coworker_id),
    KEY idx_desktop_user_coworker_cw (coworker_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='桌面用户订阅的同事';

CREATE TABLE IF NOT EXISTS opc_desktop_user_coworker_tool (
    id               bigint         NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id          bigint         NOT NULL               COMMENT '桌面用户ID',
    coworker_id      bigint         NOT NULL               COMMENT '同事ID',
    logical_mcp      varchar(128)   DEFAULT ''              COMMENT '逻辑 MCP 名',
    tool_name        varchar(128)   NOT NULL               COMMENT '工具名',
    PRIMARY KEY (id),
    KEY idx_desktop_user_cw_tool_user (user_id),
    KEY idx_desktop_user_cw_tool_cw (coworker_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='桌面用户同事工具授权';

CREATE TABLE IF NOT EXISTS opc_mcp_gateway_config (
    config_id            bigint         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id            varchar(20)    DEFAULT '000000'        COMMENT '租户编号',
    gateway_base_url     varchar(512)   DEFAULT NULL            COMMENT '网关 Base URL',
    gateway_api_key      varchar(1024)  DEFAULT NULL            COMMENT '组织 Gateway Key',
    virtual_server_id    varchar(128)   DEFAULT NULL            COMMENT 'Virtual Server ID',
    admin_token          varchar(1024)  DEFAULT NULL            COMMENT '拉 catalog 用 Admin Token',
    remark               varchar(500)   DEFAULT NULL            COMMENT '备注',
    del_flag             char(1)        DEFAULT '0'             COMMENT '删除标志',
    create_dept          bigint         DEFAULT NULL            COMMENT '创建部门',
    create_by            bigint         DEFAULT NULL            COMMENT '创建者',
    create_time          datetime       DEFAULT NULL            COMMENT '创建时间',
    update_by            bigint         DEFAULT NULL            COMMENT '更新者',
    update_time          datetime       DEFAULT NULL            COMMENT '更新时间',
    PRIMARY KEY (config_id),
    UNIQUE KEY uk_opc_mcp_gateway_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OPC MCP 网关组织配置';

-- 菜单：客户端管理(15100) -> 同事 / MCP 网关
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, component_name, query_param,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    hidden_expression, shop_expression, create_dept, create_by, create_time, update_by, update_time, remark
) VALUES (
    15150, '同事', 15100, 5, 'coworker', 'system/coworker/index', 'Coworker', '',
    0, 1, 'C', '1', '1', 'system:coworker:list', 'user-talk',
    NULL, NULL, 103, 1, NOW(), NULL, NULL, 'Opc WorkBench 同事目录'
) ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name), parent_id = VALUES(parent_id), path = VALUES(path),
    component = VALUES(component), component_name = VALUES(component_name),
    perms = VALUES(perms), visible = '1', status = '1';

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, component_name, query_param,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    hidden_expression, shop_expression, create_dept, create_by, create_time, update_by, update_time, remark
) VALUES
(15151, '同事查询', 15150, 1, '#', '', '', '', 0, 1, 'F', '1', '1', 'system:coworker:query', '#', NULL, NULL, 103, 1, NOW(), NULL, NULL, ''),
(15152, '同事新增', 15150, 2, '#', '', '', '', 0, 1, 'F', '1', '1', 'system:coworker:add', '#', NULL, NULL, 103, 1, NOW(), NULL, NULL, ''),
(15153, '同事修改', 15150, 3, '#', '', '', '', 0, 1, 'F', '1', '1', 'system:coworker:edit', '#', NULL, NULL, 103, 1, NOW(), NULL, NULL, ''),
(15154, '同事删除', 15150, 4, '#', '', '', '', 0, 1, 'F', '1', '1', 'system:coworker:remove', '#', NULL, NULL, 103, 1, NOW(), NULL, NULL, '')
ON DUPLICATE KEY UPDATE perms = VALUES(perms), visible = '1', status = '1';

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, component_name, query_param,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    hidden_expression, shop_expression, create_dept, create_by, create_time, update_by, update_time, remark
) VALUES (
    15160, 'MCP网关', 15100, 6, 'mcpGateway', 'system/mcpGateway/index', 'McpGateway', '',
    0, 1, 'C', '1', '1', 'system:mcpGateway:query', 'link',
    NULL, NULL, 103, 1, NOW(), NULL, NULL, 'Opc WorkBench MCP 网关组织配置'
) ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name), parent_id = VALUES(parent_id), path = VALUES(path),
    component = VALUES(component), component_name = VALUES(component_name),
    perms = VALUES(perms), visible = '1', status = '1';

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, component_name, query_param,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    hidden_expression, shop_expression, create_dept, create_by, create_time, update_by, update_time, remark
) VALUES
(15161, 'MCP网关查询', 15160, 1, '#', '', '', '', 0, 1, 'F', '1', '1', 'system:mcpGateway:query', '#', NULL, NULL, 103, 1, NOW(), NULL, NULL, ''),
(15162, 'MCP网关修改', 15160, 2, '#', '', '', '', 0, 1, 'F', '1', '1', 'system:mcpGateway:edit', '#', NULL, NULL, 103, 1, NOW(), NULL, NULL, '')
ON DUPLICATE KEY UPDATE perms = VALUES(perms), visible = '1', status = '1';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 15150), (1, 15151), (1, 15152), (1, 15153), (1, 15154),
(1, 15160), (1, 15161), (1, 15162);

-- 已有库升级：旧版 opc_mcp_gateway_config 缺审计/逻辑删字段时执行
-- ALTER TABLE opc_mcp_gateway_config ADD COLUMN del_flag char(1) DEFAULT '0' COMMENT '删除标志' AFTER remark;
-- ALTER TABLE opc_mcp_gateway_config ADD COLUMN create_dept bigint DEFAULT NULL COMMENT '创建部门' AFTER del_flag;
-- ALTER TABLE opc_mcp_gateway_config ADD COLUMN create_by bigint DEFAULT NULL COMMENT '创建者' AFTER create_dept;
-- ALTER TABLE opc_mcp_gateway_config ADD COLUMN create_time datetime DEFAULT NULL COMMENT '创建时间' AFTER create_by;
