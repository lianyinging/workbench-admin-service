-- 同事：单页菜单 → 父级「同事管理」+ 同事列表 / 轮播配置 / 分类管理
-- 在 cm_opc_workbench（dev）执行；可重复执行

-- 15150：改为目录「同事管理」
UPDATE sys_menu
SET menu_name     = '同事管理',
    parent_id     = 15100,
    order_num     = 5,
    path          = 'coworker',
    component     = NULL,
    component_name = NULL,
    query_param   = NULL,
    is_frame      = 0,
    is_cache      = 1,
    menu_type     = 'M',
    visible       = '1',
    status        = '1',
    perms         = NULL,
    icon          = 'user-talk',
    remark        = '同事管理目录'
WHERE menu_id = 15150;

-- 同事列表（原同事页）
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, component_name, query_param,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    hidden_expression, shop_expression, create_dept, create_by, create_time, update_by, update_time, remark
) VALUES (
    15155, '同事列表', 15150, 1, 'list', 'system/coworker/index', 'Coworker', '',
    0, 1, 'C', '1', '1', 'system:coworker:list', 'list',
    NULL, NULL, 103, 1, NOW(), NULL, NULL, '同事目录列表'
) ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    parent_id = VALUES(parent_id),
    order_num = VALUES(order_num),
    path = VALUES(path),
    component = VALUES(component),
    component_name = VALUES(component_name),
    menu_type = VALUES(menu_type),
    perms = VALUES(perms),
    visible = '1',
    status = '1';

-- 按钮挂到同事列表下
UPDATE sys_menu SET parent_id = 15155 WHERE menu_id IN (15151, 15152, 15153, 15154);

-- 轮播配置（占位）
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, component_name, query_param,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    hidden_expression, shop_expression, create_dept, create_by, create_time, update_by, update_time, remark
) VALUES (
    15156, '轮播配置', 15150, 2, 'carousel', 'system/coworkerCarousel/index', 'CoworkerCarousel', '',
    0, 1, 'C', '1', '1', 'system:coworkerCarousel:list', 'slider',
    NULL, NULL, 103, 1, NOW(), NULL, NULL, '同事轮播配置（占位）'
) ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    parent_id = VALUES(parent_id),
    path = VALUES(path),
    component = VALUES(component),
    component_name = VALUES(component_name),
    perms = VALUES(perms),
    visible = '1',
    status = '1';

-- 分类管理已改为同事列表左侧栏，不再单独挂菜单（若历史有 15157 则隐藏）
UPDATE sys_menu SET visible = '0', status = '0' WHERE menu_id = 15157;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 15150), (1, 15155), (1, 15151), (1, 15152), (1, 15153), (1, 15154),
(1, 15156);
