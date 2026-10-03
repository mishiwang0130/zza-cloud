-- =============================================================================
-- infra 服务建表与初始化脚本（MySQL 8）
-- 执行方式：mysql -h 192.168.205.128 -u root -p < sql/infra.sql
-- 说明：
--   1. 库名固定 zza，与 application-dev.yml / application-local.yml 中的连接库一致；
--   2. 表名以服务名 infra 为前缀，跨服务的系统表才用 sys_ 前缀；
--   3. 每张表都带公共字段（id、create_time、create_by、update_time、update_by、is_delete），
--      由 common-mybatis 的 AuditMetaObjectHandler 自动填充；
--   4. 种子数据里的 admin 密码是 123456 的 BCrypt 哈希，首次登录后请立即修改。
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `zza` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `zza`;

-- -----------------------------------------------------------------------------
-- 1. 管理后台用户表
-- 只存 admin 端账号；app 端用户表由后续需求单独提供，通过 infra_token.user_type 区分端
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `infra_user`;
CREATE TABLE `infra_user`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(64)  NOT NULL COMMENT '登录用户名',
    `password`    VARCHAR(100) NOT NULL COMMENT '登录密码（BCrypt 哈希，禁止存明文）',
    `nickname`    VARCHAR(64)  NOT NULL COMMENT '昵称',
    `mobile`      VARCHAR(20)  NOT NULL COMMENT '手机号',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0 启用、1 停用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`   BIGINT       NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_infra_user_username` (`username`),
    UNIQUE KEY `uk_infra_user_mobile` (`mobile`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = 'infra 管理后台用户表';

-- -----------------------------------------------------------------------------
-- 2. 角色表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `infra_role`;
CREATE TABLE `infra_role`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(64) NOT NULL COMMENT '角色名称',
    `code`        VARCHAR(64) NOT NULL COMMENT '角色编码，超管固定 super_admin',
    `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
    `status`      TINYINT     NOT NULL DEFAULT 0 COMMENT '状态：0 启用、1 停用',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   BIGINT      NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`   BIGINT      NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`   TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_infra_role_code` (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = 'infra 角色表';

-- -----------------------------------------------------------------------------
-- 3. 菜单权限表
-- 目录 / 菜单 / 按钮共用一张表：type 区分，perms 承载接口权限标识
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `infra_menu`;
CREATE TABLE `infra_menu`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '上级菜单 ID，0 表示顶级',
    `name`        VARCHAR(64)  NOT NULL COMMENT '菜单名称',
    `type`        TINYINT      NOT NULL DEFAULT 2 COMMENT '类型：1 目录、2 菜单、3 按钮',
    `path`        VARCHAR(200) NOT NULL DEFAULT '' COMMENT '前端路由地址',
    `component`   VARCHAR(200) NOT NULL DEFAULT '' COMMENT '前端组件路径',
    `perms`       VARCHAR(100) NOT NULL DEFAULT '' COMMENT '权限标识，如 infra:user:create',
    `icon`        VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '图标名称',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
    `visible`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否显示：0 显示、1 隐藏',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0 启用、1 停用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`   BIGINT       NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_infra_menu_parent_id` (`parent_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = 'infra 菜单权限表';

-- -----------------------------------------------------------------------------
-- 4. 用户角色关联表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `infra_user_role`;
CREATE TABLE `infra_user_role`
(
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT   NOT NULL COMMENT '用户 ID',
    `role_id`     BIGINT   NOT NULL COMMENT '角色 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   BIGINT   NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`   BIGINT   NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`   TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_infra_user_role_user_id_role_id` (`user_id`, `role_id`),
    KEY `idx_infra_user_role_role_id` (`role_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = 'infra 用户角色关联表';

-- -----------------------------------------------------------------------------
-- 5. 角色菜单关联表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `infra_role_menu`;
CREATE TABLE `infra_role_menu`
(
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_id`     BIGINT   NOT NULL COMMENT '角色 ID',
    `menu_id`     BIGINT   NOT NULL COMMENT '菜单 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   BIGINT   NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`   BIGINT   NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`   TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_infra_role_menu_role_id_menu_id` (`role_id`, `menu_id`),
    KEY `idx_infra_role_menu_menu_id` (`menu_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = 'infra 角色菜单关联表';

-- -----------------------------------------------------------------------------
-- 6. 访问凭证表
-- MySQL 是凭证状态的权威数据，Redis 只做校验缓存；只存 token 的 SHA-256 摘要
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `infra_token`;
CREATE TABLE `infra_token`
(
    `id`                 BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `token_hash`         CHAR(64)    NOT NULL COMMENT 'access token 的 SHA-256 摘要（十六进制小写）',
    `user_id`            BIGINT      NOT NULL COMMENT '凭证所属用户 ID',
    `user_type`          TINYINT     NOT NULL COMMENT '登录端类型：1 管理后台、2 用户端',
    `username`           VARCHAR(64) NOT NULL DEFAULT '' COMMENT '登录用户名（冗余，校验时不必回查用户表）',
    `expire_time`        DATETIME    NOT NULL COMMENT '凭证过期时间',
    `status`             TINYINT     NOT NULL DEFAULT 0 COMMENT '状态：0 有效、1 已失效',
    `login_ip`           VARCHAR(64) NOT NULL DEFAULT '' COMMENT '登录 IP，仅用于审计排查',
    `refresh_token_hash` CHAR(64)    NOT NULL DEFAULT '' COMMENT '同一登录会话的续期凭证摘要，登出时一并失效',
    `create_time`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`          BIGINT      NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`          BIGINT      NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`          TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_infra_token_token_hash` (`token_hash`),
    KEY `idx_infra_token_user_id` (`user_id`),
    KEY `idx_infra_token_expire_time` (`expire_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = 'infra 访问凭证表';

-- -----------------------------------------------------------------------------
-- 7. 续期凭证表
-- 续期凭证是不透明随机串（不是 JWT），是否有效完全以本表为准；每次续期都做轮换
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `infra_token_refresh`;
CREATE TABLE `infra_token_refresh`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `token_hash`  CHAR(64)    NOT NULL COMMENT '续期凭证的 SHA-256 摘要（十六进制小写）',
    `user_id`     BIGINT      NOT NULL COMMENT '凭证所属用户 ID',
    `user_type`   TINYINT     NOT NULL COMMENT '登录端类型：1 管理后台、2 用户端',
    `username`    VARCHAR(64) NOT NULL DEFAULT '' COMMENT '登录用户名（冗余，续期时不必回查用户表）',
    `expire_time` DATETIME    NOT NULL COMMENT '凭证过期时间',
    `status`      TINYINT     NOT NULL DEFAULT 0 COMMENT '状态：0 有效、1 已失效',
    `login_ip`    VARCHAR(64) NOT NULL DEFAULT '' COMMENT '登录 IP，仅用于审计排查',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   BIGINT      NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`   BIGINT      NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`   TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_infra_token_refresh_token_hash` (`token_hash`),
    KEY `idx_infra_token_refresh_user_id` (`user_id`),
    KEY `idx_infra_token_refresh_expire_time` (`expire_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = 'infra 续期凭证表';

-- =============================================================================
-- 初始化数据
-- =============================================================================

-- 超级管理员账号：admin / 123456（BCrypt 哈希），首次登录后请立即修改密码
INSERT INTO `infra_user` (`id`, `username`, `password`, `nickname`, `mobile`, `status`, `create_by`, `update_by`)
VALUES (1, 'admin', '$2a$10$CBAQhdzNMdifqQ.wKs8PTuQOjOmm8IzV5Qrp5uKtFnX7UydHIdEAu', '超级管理员', '13800000000', 0, 0, 0);

-- 超级管理员角色：code 固定 super_admin，拥有该角色的用户跳过权限校验
INSERT INTO `infra_role` (`id`, `name`, `code`, `sort`, `status`, `create_by`, `update_by`)
VALUES (1, '超级管理员', 'super_admin', 1, 0, 0, 0);

-- 菜单与按钮权限：目录 1、菜单 2~3 段、按钮承载 perms
INSERT INTO `infra_menu` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`,
                          `status`, `create_by`, `update_by`)
VALUES (1, 0, '系统管理', 1, '/system', '', '', 'Setting', 1, 0, 0, 0, 0),
       (2, 1, '用户管理', 2, 'user', 'system/user/index', 'infra:user:query', 'User', 1, 0, 0, 0, 0),
       (3, 2, '用户新增', 3, '', '', 'infra:user:create', '', 1, 0, 0, 0, 0),
       (4, 2, '用户修改', 3, '', '', 'infra:user:update', '', 2, 0, 0, 0, 0),
       (5, 2, '用户删除', 3, '', '', 'infra:user:delete', '', 3, 0, 0, 0, 0),
       (6, 2, '重置密码', 3, '', '', 'infra:user:reset-password', '', 4, 0, 0, 0, 0),
       (7, 2, '修改状态', 3, '', '', 'infra:user:update-status', '', 5, 0, 0, 0, 0),
       (8, 1, '角色管理', 2, 'role', 'system/role/index', 'infra:role:query', 'Avatar', 2, 0, 0, 0, 0),
       (9, 8, '角色新增', 3, '', '', 'infra:role:create', '', 1, 0, 0, 0, 0),
       (10, 8, '角色修改', 3, '', '', 'infra:role:update', '', 2, 0, 0, 0, 0),
       (11, 8, '角色删除', 3, '', '', 'infra:role:delete', '', 3, 0, 0, 0, 0),
       (12, 8, '修改状态', 3, '', '', 'infra:role:update-status', '', 4, 0, 0, 0, 0),
       (13, 1, '菜单管理', 2, 'menu', 'system/menu/index', 'infra:menu:query', 'Menu', 3, 0, 0, 0, 0),
       (14, 13, '菜单新增', 3, '', '', 'infra:menu:create', '', 1, 0, 0, 0, 0),
       (15, 13, '菜单修改', 3, '', '', 'infra:menu:update', '', 2, 0, 0, 0, 0),
       (16, 13, '菜单删除', 3, '', '', 'infra:menu:delete', '', 3, 0, 0, 0, 0);

-- 超管账号绑定超管角色
INSERT INTO `infra_user_role` (`user_id`, `role_id`, `create_by`, `update_by`)
VALUES (1, 1, 0, 0);

-- 超管角色拥有全部菜单与按钮权限
INSERT INTO `infra_role_menu` (`role_id`, `menu_id`, `create_by`, `update_by`)
VALUES (1, 1, 0, 0), (1, 2, 0, 0), (1, 3, 0, 0), (1, 4, 0, 0), (1, 5, 0, 0), (1, 6, 0, 0), (1, 7, 0, 0),
       (1, 8, 0, 0), (1, 9, 0, 0), (1, 10, 0, 0), (1, 11, 0, 0), (1, 12, 0, 0),
       (1, 13, 0, 0), (1, 14, 0, 0), (1, 15, 0, 0), (1, 16, 0, 0);
