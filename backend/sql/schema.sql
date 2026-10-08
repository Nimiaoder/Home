CREATE DATABASE IF NOT EXISTS dev_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dev_db;

CREATE TABLE IF NOT EXISTS `USER` (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL COMMENT '帳號',
    password    VARCHAR(100) NOT NULL COMMENT '密碼(BCrypt雜湊)',
    nickname    VARCHAR(50)  NOT NULL COMMENT '使用者暱稱',
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '權限 ADMIN/USER',
    enabled     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1啟用 0停用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '註冊日期',
    UNIQUE KEY uk_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================ 遊戲伺服器：Minecraft ============================
-- 預設由 JPA (ddl-auto: update) 自動建立；若改用 validate，請手動執行下列語法。

CREATE TABLE IF NOT EXISTS `mc_server` (
    id            BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(50)   NOT NULL COMMENT '伺服器名稱',
    description   VARCHAR(255)  NULL COMMENT '說明',
    dir_name      VARCHAR(100)  NOT NULL COMMENT '在 servers/ 底下的資料夾名稱',
    type          VARCHAR(20)   NOT NULL COMMENT 'VANILLA/PAPER/FABRIC/FORGE/NEOFORGE',
    mc_version    VARCHAR(50)   NOT NULL COMMENT 'Minecraft 版本',
    build_id      VARCHAR(100)  NULL COMMENT '建置/Loader 版本',
    port          INT           NOT NULL DEFAULT 25565,
    memory_mb     INT           NOT NULL DEFAULT 2048,
    java_path     VARCHAR(300)  NULL COMMENT '指定的 java；空白為自動選擇',
    jvm_args      VARCHAR(1000) NULL,
    auto_start    TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '後端啟動時自動啟動',
    install_state VARCHAR(20)   NOT NULL DEFAULT 'INSTALLING' COMMENT 'INSTALLING/READY/FAILED',
    install_error VARCHAR(1000) NULL,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_mc_server_dir (dir_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `mc_version_log` (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    server_id   BIGINT       NOT NULL,
    type        VARCHAR(20)  NOT NULL,
    mc_version  VARCHAR(50)  NOT NULL,
    build_id    VARCHAR(100) NULL,
    action      VARCHAR(20)  NOT NULL COMMENT 'INSTALL/CHANGE',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_mc_version_log_server (server_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================ 系統參數 ============================
CREATE TABLE IF NOT EXISTS `sys_param` (
    param_key   VARCHAR(100) NOT NULL PRIMARY KEY COMMENT '參數代碼',
    param_value VARCHAR(500) NULL COMMENT '參數值',
    description VARCHAR(255) NULL COMMENT '說明',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 是否開放註冊帳號（Y 開放 / N 關閉），預設關閉
INSERT IGNORE INTO `sys_param` (param_key, param_value, description)
VALUES ('REGISTER_ENABLED', 'N', '是否開放註冊帳號 Y/N');
