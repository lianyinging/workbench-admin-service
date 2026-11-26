-- MCP 网关：单租户单配置 → 多配置；同事绑定 mcp_config_id
-- 已在 cm_opc_workbench (dev) 执行；新环境请同步执行

ALTER TABLE opc_mcp_gateway_config DROP INDEX uk_opc_mcp_gw_tenant;

ALTER TABLE opc_mcp_gateway_config
    ADD COLUMN config_name varchar(128) NOT NULL DEFAULT '默认网关' COMMENT '配置名称' AFTER tenant_id,
    ADD COLUMN status char(1) DEFAULT '1' COMMENT '状态（1正常 0停用）' AFTER remark,
    ADD COLUMN sort_order int DEFAULT 0 COMMENT '排序' AFTER status;

ALTER TABLE opc_mcp_gateway_config
    MODIFY COLUMN gateway_base_url varchar(512) DEFAULT NULL COMMENT '网关 Base URL';

UPDATE opc_mcp_gateway_config
SET config_name = IF(IFNULL(config_name, '') = '' OR config_name = '默认网关', '默认网关', config_name),
    status = IFNULL(status, '1'),
    sort_order = IFNULL(sort_order, 0)
WHERE config_id > 0;

ALTER TABLE opc_coworker
    ADD COLUMN mcp_config_id bigint DEFAULT NULL COMMENT '绑定的 MCP 网关配置ID' AFTER visibility;

UPDATE opc_coworker c
SET c.mcp_config_id = (SELECT MIN(g.config_id) FROM opc_mcp_gateway_config g WHERE g.del_flag = '0')
WHERE c.mcp_config_id IS NULL;

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, component_name, query_param,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_dept, create_by, create_time, remark
) VALUES
(15163, 'MCP网关新增', 15160, 3, '#', '', '', '', 0, 1, 'F', '1', '1', 'system:mcpGateway:add', '#', 103, 1, NOW(), ''),
(15164, 'MCP网关删除', 15160, 4, '#', '', '', '', 0, 1, 'F', '1', '1', 'system:mcpGateway:remove', '#', 103, 1, NOW(), '')
ON DUPLICATE KEY UPDATE perms = VALUES(perms), visible = '1', status = '1';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 15163), (1, 15164);
