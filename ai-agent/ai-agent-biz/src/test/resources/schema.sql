-- 上下文测试用的 H2 建表脚本：字段与 sql/ai-agent.sql 保持一致（去掉索引与注释）
DROP TABLE IF EXISTS `ai_agent_conversation`;
CREATE TABLE `ai_agent_conversation`
(
    `id`                BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`           BIGINT      NOT NULL,
    `title`             VARCHAR(64) NOT NULL DEFAULT '',
    `message_count`     INT         NOT NULL DEFAULT 0,
    `last_message_time` DATETIME    NOT NULL,
    `create_time`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `create_by`         BIGINT      NOT NULL DEFAULT 0,
    `update_time`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`         BIGINT      NOT NULL DEFAULT 0,
    `is_delete`         TINYINT     NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS `ai_agent_message`;
CREATE TABLE `ai_agent_message`
(
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `conversation_id` BIGINT      NOT NULL,
    `user_id`         BIGINT      NOT NULL,
    `sender_type`     TINYINT     NOT NULL,
    `content`         TEXT        NOT NULL,
    `sources`         TEXT        NULL,
    `model`           VARCHAR(64) NULL,
    `latency_ms`      INT         NULL,
    `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `create_by`       BIGINT      NOT NULL DEFAULT 0,
    `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       BIGINT      NOT NULL DEFAULT 0,
    `is_delete`       TINYINT     NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS `ai_agent_knowledge_document`;
CREATE TABLE `ai_agent_knowledge_document`
(
    `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
    `file_name`     VARCHAR(255) NOT NULL,
    `content_type`  VARCHAR(128) NOT NULL DEFAULT '',
    `city`          VARCHAR(32)  NOT NULL DEFAULT '通用',
    `file_size`     BIGINT       NOT NULL DEFAULT 0,
    `storage_key`   VARCHAR(512) NOT NULL DEFAULT '',
    `chunk_count`   INT          NOT NULL DEFAULT 0,
    `status`        TINYINT      NOT NULL DEFAULT 0,
    `error_message` VARCHAR(500) NOT NULL DEFAULT '',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `create_by`     BIGINT       NOT NULL DEFAULT 0,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`     BIGINT       NOT NULL DEFAULT 0,
    `is_delete`     TINYINT      NOT NULL DEFAULT 0
);
