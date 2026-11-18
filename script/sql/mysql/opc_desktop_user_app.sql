-- 桌面用户订阅的行业应用（多选）
CREATE TABLE IF NOT EXISTS opc_desktop_user_app (
    id       bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id  bigint NOT NULL COMMENT '桌面用户ID',
    app_id   bigint NOT NULL COMMENT '行业应用ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_desktop_user_app (user_id, app_id),
    KEY idx_desktop_user_app_app (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='桌面用户订阅的行业应用';
