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

-- -----------------------------------------------------------------------------
-- 8. 上传文件表
-- 只记录文件元数据；访问地址是预签名的、会过期，因此不落库，取文件时按 path 重新签发
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `infra_file`;
CREATE TABLE `infra_file`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`         VARCHAR(255) NOT NULL COMMENT '上传时的原始文件名，仅用于展示与检索',
    `path`         VARCHAR(255) NOT NULL COMMENT '对象存储中的对象名（key），取文件时用它重新签发地址',
    `size`         BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    `content_type` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '内容类型（MIME），浏览器未带时为空串',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`    BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`    BIGINT       NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_infra_file_path` (`path`),
    KEY `idx_infra_file_create_by` (`create_by`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = 'infra 上传文件表';

-- -----------------------------------------------------------------------------
-- 9. 字典类型表
-- 唯一性（type 不重复）由服务层按「未删除」数据校验，不在库上加唯一索引：
-- 逻辑删除会把行留在表里继续占着唯一键，「删了同编码再建」就会报 Duplicate entry；
-- 字典属于配置数据，删除后重建是正常操作。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `infra_dict_type`;
CREATE TABLE `infra_dict_type`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(100) NOT NULL COMMENT '字典类型名称',
    `type`        VARCHAR(100) NOT NULL COMMENT '字典类型编码，服务内唯一，前端按它取字典数据',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0 启用、1 停用',
    `remark`      VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`   BIGINT       NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_infra_dict_type_type` (`type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = 'infra 字典类型表';

-- -----------------------------------------------------------------------------
-- 10. 字典数据表
-- 与类型表同理：同一类型下的 value 唯一由服务层校验，不加库级唯一索引
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `infra_dict_data`;
CREATE TABLE `infra_dict_data`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `dict_type`   VARCHAR(100) NOT NULL COMMENT '所属字典类型编码',
    `label`       VARCHAR(100) NOT NULL COMMENT '字典标签，展示用',
    `value`       VARCHAR(100) NOT NULL COMMENT '字典值，存库与传参用',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0 启用、1 停用',
    `remark`      VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`   BIGINT       NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_infra_dict_data_dict_type` (`dict_type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = 'infra 字典数据表';

-- -----------------------------------------------------------------------------
-- 11. 行政区划表（省 / 市 / 区县三级自关联）
-- 数据量约 3300 行，种子数据单独放 sql/infra_area.sql（按需导入）；这里只建结构。
-- 省市区属于标准数据，不提供维护接口，靠脚本更新。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `infra_area`;
CREATE TABLE `infra_area`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id`   BIGINT      NOT NULL DEFAULT 0 COMMENT '上级区划 ID，0 表示省级',
    `name`        VARCHAR(50) NOT NULL COMMENT '区划名称',
    `code`        VARCHAR(12) NOT NULL COMMENT '行政区划代码（统计局口径：省级 2 位、市级 4 位、区县 6 位）',
    `level`       TINYINT     NOT NULL COMMENT '层级：1 省、2 市、3 区县',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   BIGINT      NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`   BIGINT      NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`   TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_infra_area_code` (`code`),
    KEY `idx_infra_area_parent_id` (`parent_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = 'infra 行政区划表';

-- -----------------------------------------------------------------------------
-- 12. 用户端用户表
-- 与管理后台用户表 infra_user 分开建：两端账号体系不同（app 靠手机号注册、无角色权限），
-- 凭证通过 infra_token.user_type = 2 关联到本表；app 端用短信验证码登录，本表不存密码。
-- 本表不提供删除接口（只有停用），所以手机号上的唯一索引不会和逻辑删除打架。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `infra_app_user`;
CREATE TABLE `infra_app_user`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `mobile`         VARCHAR(20)  NOT NULL COMMENT '登录手机号，app 端以此为账号',
    `nickname`       VARCHAR(64)  NOT NULL COMMENT '昵称；注册时未填则用手机号脱敏值兜底',
    `avatar_file_id` BIGINT       NOT NULL DEFAULT 0 COMMENT '头像文件 ID，0 表示未设置；只存 ID，展示地址由文件接口按需签发',
    `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0 启用、1 停用；停用后不允许登录',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`      BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录（app 自助注册固定 0）',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`      BIGINT       NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_infra_app_user_mobile` (`mobile`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = 'infra 用户端用户表';

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

-- 字典管理：挂在「系统管理」下的菜单与按钮（字典数据是详情页，不做导航项，只留查询等按钮权限）
INSERT INTO `infra_menu` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`,
                          `status`, `create_by`, `update_by`)
VALUES (17, 1, '字典类型', 2, 'dict-type', 'system/dict/type/index', 'infra:dict-type:query', 'Collection', 4, 0, 0, 0, 0),
       (18, 17, '字典类型新增', 3, '', '', 'infra:dict-type:create', '', 1, 0, 0, 0, 0),
       (19, 17, '字典类型修改', 3, '', '', 'infra:dict-type:update', '', 2, 0, 0, 0, 0),
       (20, 17, '字典类型删除', 3, '', '', 'infra:dict-type:delete', '', 3, 0, 0, 0, 0),
       (21, 17, '字典数据查询', 3, '', '', 'infra:dict-data:query', '', 4, 0, 0, 0, 0),
       (22, 17, '字典数据新增', 3, '', '', 'infra:dict-data:create', '', 5, 0, 0, 0, 0),
       (23, 17, '字典数据修改', 3, '', '', 'infra:dict-data:update', '', 6, 0, 0, 0, 0),
       (24, 17, '字典数据删除', 3, '', '', 'infra:dict-data:delete', '', 7, 0, 0, 0, 0);

-- 超管角色同样拥有字典管理的菜单与按钮
INSERT INTO `infra_role_menu` (`role_id`, `menu_id`, `create_by`, `update_by`)
VALUES (1, 17, 0, 0), (1, 18, 0, 0), (1, 19, 0, 0), (1, 20, 0, 0),
       (1, 21, 0, 0), (1, 22, 0, 0), (1, 23, 0, 0), (1, 24, 0, 0);

-- 示例字典：通用状态（0 启用、1 停用），与代码里的 CommonStatusEnum 语义一致
INSERT INTO `infra_dict_type` (`id`, `name`, `type`, `status`, `remark`, `create_by`, `update_by`)
VALUES (1, '通用状态', 'common_status', 0, '启用 / 停用这类通用状态', 0, 0);

INSERT INTO `infra_dict_data` (`id`, `dict_type`, `label`, `value`, `sort`, `status`, `remark`, `create_by`, `update_by`)
VALUES (1, 'common_status', '启用', '0', 1, 0, '', 0, 0),
       (2, 'common_status', '停用', '1', 2, 0, '', 0, 0);

-- 访客端联调演示账号：手机号 13800000000、昵称「测试租客」（app 端用手机号 + 短信验证码登录，本表不存密码）
-- 不写死 id：交给自增分配，避免与手工造的账号撞号；重复执行前本脚本会重建该表
INSERT INTO `infra_app_user` (`mobile`, `nickname`, `avatar_file_id`, `status`, `create_by`, `update_by`)
VALUES ('13800000000', '测试租客', 0, 0, 0, 0);
