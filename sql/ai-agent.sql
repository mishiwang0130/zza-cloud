-- =============================================================================
-- ai-agent 服务建表与初始化脚本（MySQL 8）
-- 执行顺序：先执行 sql/infra.sql（建库与 infra 表），再执行本脚本
-- 约定：
--   1. 与 infra / rental 同库 zza，表靠 ai_agent_ 前缀隔离；
--   2. 表前缀 = 服务名 ai-agent 的 snake 写法（= ai-agent-biz 的 spring.application.name）；
--   3. 每张表都带公共字段 id / create_time / create_by / update_time / update_by / is_delete，
--      由 common-mybatis 的 AuditMetaObjectHandler 自动填充；
--   4. 知识切片的正文与向量不在 MySQL：它们在 Qdrant 里，按 metadata 的 documentId 关联本表；
--   5. 菜单 id 从 44 起（rental 已占用 25~43），重复执行时按 id 区间先删后插。
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `zza` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `zza`;

-- -----------------------------------------------------------------------------
-- 1. 智能客服会话
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_agent_conversation`;
CREATE TABLE `ai_agent_conversation`
(
    `id`                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`           BIGINT      NOT NULL COMMENT '所属小程序用户 ID（infra_app_user.id）',
    `title`             VARCHAR(64) NOT NULL DEFAULT '' COMMENT '会话标题：取首条用户问题截断，不调用模型生成',
    `message_count`     INT         NOT NULL DEFAULT 0 COMMENT '消息条数：用户消息与 AI 消息都计入',
    `last_message_time` DATETIME    NOT NULL COMMENT '最后一条消息时间，会话列表按它倒序',
    `create_time`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`         BIGINT      NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`         BIGINT      NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`         TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_ai_agent_conversation_user_id` (`user_id`),
    KEY `idx_ai_agent_conversation_last_message_time` (`last_message_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '智能客服会话表';

-- -----------------------------------------------------------------------------
-- 2. 智能客服消息
--    本表是「历史与审计」的唯一来源；多轮上下文由 Spring AI 的 Redis 会话记忆维护
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_agent_message`;
CREATE TABLE `ai_agent_message`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `conversation_id` BIGINT      NOT NULL COMMENT '所属会话 ID（ai_agent_conversation.id）',
    `user_id`         BIGINT      NOT NULL COMMENT '所属用户 ID，便于后台按用户维度排查',
    `sender_type`     TINYINT     NOT NULL COMMENT '发送方：1 用户、2 智能客服',
    `content`         TEXT        NOT NULL COMMENT '消息内容',
    `sources`         TEXT        NULL COMMENT '命中的知识来源 JSON 数组（Fastjson2 序列化）；用户消息为空',
    `model`           VARCHAR(64) NULL COMMENT '本次回答使用的模型；用户消息为空',
    `latency_ms`      INT         NULL COMMENT '本次回答耗时（毫秒）；用户消息为空',
    `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`       BIGINT      NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`       BIGINT      NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`       TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_ai_agent_message_conversation_id` (`conversation_id`),
    KEY `idx_ai_agent_message_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '智能客服消息表';

-- -----------------------------------------------------------------------------
-- 3. 知识库文档
--    只记录元数据与索引状态；切片正文与向量在 Qdrant，按 documentId 关联
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_agent_knowledge_document`;
CREATE TABLE `ai_agent_knowledge_document`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `file_name`     VARCHAR(255) NOT NULL COMMENT '原始文件名，回答引用来源时展示',
    `content_type`  VARCHAR(128) NOT NULL DEFAULT '' COMMENT '文件类型（Content-Type）',
    `city`          VARCHAR(32)  NOT NULL DEFAULT '通用' COMMENT '城市标签：平台级通用文档填「通用」',
    `file_size`     BIGINT       NOT NULL DEFAULT 0 COMMENT '文件字节数',
    `storage_key`   VARCHAR(512) NOT NULL DEFAULT '' COMMENT '原始文件在对象存储 / 本机磁盘里的对象名',
    `chunk_count`   INT          NOT NULL DEFAULT 0 COMMENT '切片数量：索引成功后回写',
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '索引状态：0 待索引、1 已索引、2 索引失败',
    `error_message` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '索引失败原因，只在 status=2 时有值',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`     BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`     BIGINT       NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_ai_agent_knowledge_document_city` (`city`),
    KEY `idx_ai_agent_knowledge_document_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '智能客服知识库文档表';

-- =============================================================================
-- 菜单与按钮权限
--   id 44~49（rental 占用 25~43）；type：1 目录、2 菜单、3 按钮
--   权限串与 AiAgentPermissionConstant 一一对应
--   4. 菜单是配置数据，重复执行时按 id 区间先清理再插入
-- =============================================================================
DELETE
FROM `infra_role_menu`
WHERE `menu_id` BETWEEN 44 AND 49;
DELETE
FROM `infra_menu`
WHERE `id` BETWEEN 44 AND 49;

INSERT INTO `infra_menu` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`,
                          `status`, `create_by`, `update_by`)
VALUES (44, 0, '智能客服', 1, '/ai-agent', '', '', 'Service', 3, 0, 0, 0, 0),
       (45, 44, '知识库管理', 2, 'knowledge', 'ai-agent/knowledge/index', 'ai-agent:knowledge:query',
        'Collection', 1, 0, 0, 0, 0),
       (46, 45, '知识库上传', 3, '', '', 'ai-agent:knowledge:create', '', 1, 0, 0, 0, 0),
       (47, 45, '知识库重建', 3, '', '', 'ai-agent:knowledge:rebuild', '', 2, 0, 0, 0, 0),
       (48, 45, '知识库删除', 3, '', '', 'ai-agent:knowledge:delete', '', 3, 0, 0, 0, 0),
       (49, 44, '会话记录', 2, 'conversation', 'ai-agent/conversation/index', 'ai-agent:conversation:query',
        'ChatDotRound', 2, 0, 0, 0, 0);

-- 超管角色（id = 1）拥有智能客服的全部菜单与按钮权限
INSERT INTO `infra_role_menu` (`role_id`, `menu_id`, `create_by`, `update_by`)
VALUES (1, 44, 0, 0),
       (1, 45, 0, 0),
       (1, 46, 0, 0),
       (1, 47, 0, 0),
       (1, 48, 0, 0),
       (1, 49, 0, 0);
