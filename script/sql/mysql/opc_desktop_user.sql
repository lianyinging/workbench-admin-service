-- OPC 桌面端独立登录用户（与 sys_user 隔离，仅 WorkBench 账号密码登录）
CREATE TABLE IF NOT EXISTS opc_desktop_user (
    user_id          bigint         NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    tenant_id        varchar(20)    DEFAULT '000000'        COMMENT '租户编号',
    user_name        varchar(64)    NOT NULL               COMMENT '登录账号',
    nick_name        varchar(64)    DEFAULT ''              COMMENT '昵称',
    password         varchar(128)   NOT NULL               COMMENT '密码（BCrypt）',
    status           char(1)        DEFAULT '1'             COMMENT '状态（1正常 0停用）',
    subscribed       char(1)        DEFAULT '0'             COMMENT '订阅（1已订阅 0未订阅）',
    remark           varchar(500)   DEFAULT NULL            COMMENT '备注',
    last_login_ip    varchar(128)   DEFAULT ''              COMMENT '最后登录IP',
    last_login_time  datetime       DEFAULT NULL            COMMENT '最后登录时间',
    del_flag         char(1)        DEFAULT '0'             COMMENT '删除标志（0存在 1删除）',
    create_dept      bigint         DEFAULT NULL            COMMENT '创建部门',
    create_by        bigint         DEFAULT NULL            COMMENT '创建者',
    create_time      datetime       DEFAULT NULL            COMMENT '创建时间',
    update_by        bigint         DEFAULT NULL            COMMENT '更新者',
    update_time      datetime       DEFAULT NULL            COMMENT '更新时间',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_opc_desktop_user_name (tenant_id, user_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OPC桌面端登录用户';

-- 菜单：客户端管理(15100) -> 客户端用户
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, component_name, query_param,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    hidden_expression, shop_expression, create_dept, create_by, create_time, update_by, update_time, remark
) VALUES (
    15130, '客户端用户', 15100, 3, 'desktopUser', 'system/desktopUser/index', 'DesktopUser', '',
    0, 1, 'C', '1', '1', 'system:desktopUser:list', 'user',
    NULL, NULL, 103, 1, NOW(), NULL, NULL, 'Opc WorkBench 桌面端登录用户'
) ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    parent_id = VALUES(parent_id),
    path = VALUES(path),
    component = VALUES(component),
    component_name = VALUES(component_name),
    perms = VALUES(perms),
    visible = '1',
    status = '1';

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, component_name, query_param,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    hidden_expression, shop_expression, create_dept, create_by, create_time, update_by, update_time, remark
) VALUES
(15131, '客户端用户查询', 15130, 1, '#', '', '', '', 0, 1, 'F', '1', '1', 'system:desktopUser:query', '#', NULL, NULL, 103, 1, NOW(), NULL, NULL, ''),
(15132, '客户端用户新增', 15130, 2, '#', '', '', '', 0, 1, 'F', '1', '1', 'system:desktopUser:add', '#', NULL, NULL, 103, 1, NOW(), NULL, NULL, ''),
(15133, '客户端用户修改', 15130, 3, '#', '', '', '', 0, 1, 'F', '1', '1', 'system:desktopUser:edit', '#', NULL, NULL, 103, 1, NOW(), NULL, NULL, ''),
(15134, '客户端用户删除', 15130, 4, '#', '', '', '', 0, 1, 'F', '1', '1', 'system:desktopUser:remove', '#', NULL, NULL, 103, 1, NOW(), NULL, NULL, ''),
(15135, '客户端用户重置密码', 15130, 5, '#', '', '', '', 0, 1, 'F', '1', '1', 'system:desktopUser:resetPwd', '#', NULL, NULL, 103, 1, NOW(), NULL, NULL, '')
ON DUPLICATE KEY UPDATE perms = VALUES(perms), visible = '1', status = '1';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 15130), (1, 15131), (1, 15132), (1, 15133), (1, 15134), (1, 15135);

-- 可选种子账号 opc / opc123（BCrypt）；生产环境请删除或改密
INSERT INTO opc_desktop_user (
    tenant_id, user_name, nick_name, password, status, subscribed, remark, del_flag,
    create_dept, create_by, create_time, update_by, update_time
) SELECT
    '000000', 'opc', 'OPC演示',
    '$2a$10$oJP/kh8kDfW8LSEZEEMiVex3Bovyld8OI0wBDo.4seK79IpKBGsP6',
    '1', '1', '种子账号 opc/opc123（已订阅）', '0',
    103, 1, NOW(), 1, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM opc_desktop_user WHERE user_name = 'opc');

-- 已有库升级：补充订阅字段
-- ALTER TABLE opc_desktop_user ADD COLUMN subscribed char(1) DEFAULT '0' COMMENT '订阅（1已订阅 0未订阅）' AFTER status;
