-- OPC 桌面端在线设备
CREATE TABLE IF NOT EXISTS opc_desktop_device (
    device_id            varchar(64)    NOT NULL               COMMENT '设备ID（桌面本地UUID）',
    device_name          varchar(128)   DEFAULT ''             COMMENT '设备名称',
    os_name              varchar(64)    DEFAULT ''             COMMENT '操作系统',
    os_version           varchar(64)    DEFAULT ''             COMMENT '系统版本',
    app_version          varchar(32)    DEFAULT ''             COMMENT '应用版本',
    user_id              bigint         DEFAULT NULL           COMMENT '绑定用户ID',
    tenant_id            varchar(20)    DEFAULT '000000'       COMMENT '租户编号',
    last_heartbeat_time  datetime       DEFAULT NULL           COMMENT '最后心跳时间',
    last_ip              varchar(128)   DEFAULT ''             COMMENT '最后上报IP',
    status               char(1)        DEFAULT '1'            COMMENT '状态（1正常 0禁用）',
    create_time          datetime                              COMMENT '创建时间',
    update_time          datetime                              COMMENT '更新时间',
    PRIMARY KEY (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OPC桌面端设备';

-- 设备类型字典：桌面端
INSERT INTO sys_dict_data (
    dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type,
    css_class, list_class, tag_style, is_default, create_dept, create_by, create_time, remark
) SELECT
    (SELECT IFNULL(MAX(dict_code), 0) + 1 FROM sys_dict_data d),
    '000000', 5, '桌面端', 'desktop', 'sys_device_type',
    NULL, 'primary', NULL, 'N', 103, 1, NOW(), 'OPC 桌面工作台'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sys_device_type' AND dict_value = 'desktop');

-- 桌面客户端（register 校验 clientId/secret；grant_type=password 仅为框架字段占位，非用户密码登录）
INSERT INTO sys_client (
    id, client_id, client_key, client_secret, grant_type, device_type,
    active_timeout, timeout, status, del_flag, create_dept, create_by, create_time, update_by, update_time
) SELECT
    3, 'a1b2c3d4e5f6789012345678abcdef01', 'desktop', 'desktop123', 'password', 'desktop',
    1800, 604800, '1', '0', 103, 1, NOW(), 1, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_client WHERE client_key = 'desktop');

-- 菜单：系统监控 -> 桌面端在线
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, component_name, query_param,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    hidden_expression, shop_expression, create_dept, create_by, create_time, update_by, update_time, remark
) VALUES (
    15020, '桌面端在线', 2, 5, 'desktop', 'monitor/desktop/index', 'DesktopOnline', '',
    0, 1, 'C', '1', '1', 'monitor:desktop:list', 'laptop',
    NULL, NULL, 103, 1, NOW(), NULL, NULL, '桌面端在线设备监控'
) ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    path = VALUES(path),
    component = VALUES(component),
    perms = VALUES(perms),
    visible = '1',
    status = '1';

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, component_name, query_param,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    hidden_expression, shop_expression, create_dept, create_by, create_time, update_by, update_time, remark
) VALUES (
    15021, '桌面设备查询', 15020, 1, '#', '', '', '',
    0, 1, 'F', '1', '1', 'monitor:desktop:query', '#',
    NULL, NULL, 103, 1, NOW(), NULL, NULL, ''
) ON DUPLICATE KEY UPDATE perms = VALUES(perms);

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, component_name, query_param,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    hidden_expression, shop_expression, create_dept, create_by, create_time, update_by, update_time, remark
) VALUES (
    15022, '桌面设备禁用', 15020, 2, '#', '', '', '',
    0, 1, 'F', '1', '1', 'monitor:desktop:edit', '#',
    NULL, NULL, 103, 1, NOW(), NULL, NULL, ''
) ON DUPLICATE KEY UPDATE perms = VALUES(perms);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 15020), (1, 15021), (1, 15022);
