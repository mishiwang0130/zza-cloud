-- =============================================================================
-- rental 服务建表脚本（MySQL 8）
-- 执行顺序：先执行 sql/infra.sql（建库与 infra 表），再执行本脚本
-- 约定：
--   1. 与 infra 同库 zza，表靠 rental_ 前缀隔离；
--   2. 表前缀 = 服务名 rental（= rental-biz 的 spring.application.name）；
--   3. 每张表都带公共字段 id / create_time / create_by / update_time / update_by / is_delete，
--      由 common-mybatis 的 AuditMetaObjectHandler 自动填充；
--   4. 关联表同样保留公共字段，但覆盖写入必须物理 DELETE（逻辑删除会占着唯一键）；
--   5. 字典编码取 infra 字典表，文件取 infra 文件表，不落库文件访问地址。
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `zza` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `zza`;

-- -----------------------------------------------------------------------------
-- 1. 公寓
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `rental_apartment`;
CREATE TABLE `rental_apartment`
(
    `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`              VARCHAR(100)  NOT NULL COMMENT '公寓名称',
    `introduction`      VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '公寓介绍',
    `district_id`       BIGINT        NOT NULL COMMENT '所在区县 ID（infra 行政区划表区县节点，省/市由父级链推导）',
    `address_detail`    VARCHAR(255)  NOT NULL DEFAULT '' COMMENT '详细地址（不含省市区前缀）',
    `phone`             VARCHAR(20)   NOT NULL DEFAULT '' COMMENT '公寓前台电话',
    `min_lease_months`  INT           NOT NULL DEFAULT 1 COMMENT '最低起租月数，更长的租期自然支持',
    `deposit_months`    INT           NOT NULL DEFAULT 1 COMMENT '押金月数，如 1 表示押一个月',
    `payment_method`    TINYINT       NOT NULL DEFAULT 1 COMMENT '付款方式：1 月付、2 季付、3 半年付、4 年付',
    `publish_status`    TINYINT       NOT NULL DEFAULT 0 COMMENT '发布状态：0 未发布、1 已发布',
    `label_codes`       VARCHAR(500)  NOT NULL DEFAULT '' COMMENT '标签编码，逗号分隔，取 infra 字典 rental_apartment_label',
    `facility_codes`    VARCHAR(500)  NOT NULL DEFAULT '' COMMENT '配套编码，逗号分隔，取 infra 字典 rental_apartment_facility',
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`         BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`         BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_rental_apartment_district_id` (`district_id`),
    KEY `idx_rental_apartment_min_lease_months` (`min_lease_months`),
    KEY `idx_rental_apartment_payment_method` (`payment_method`),
    KEY `idx_rental_apartment_publish_status` (`publish_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '公寓信息表';

-- -----------------------------------------------------------------------------
-- 2. 房间
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `rental_room`;
CREATE TABLE `rental_room`
(
    `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `apartment_id`    BIGINT         NOT NULL COMMENT '所属公寓 ID',
    `room_number`     VARCHAR(50)    NOT NULL COMMENT '房间号，同一公寓内唯一',
    `rent`            DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '月租金（元/月）',
    `area`            DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '面积（㎡）',
    `room_count`      TINYINT        NOT NULL DEFAULT 1 COMMENT '户型室数：1 一室、2 两室',
    `orientation`     VARCHAR(32)    NOT NULL DEFAULT '' COMMENT '朝向，取 infra 字典 rental_room_orientation 的编码',
    `floor_no`        VARCHAR(32)    NOT NULL DEFAULT '' COMMENT '楼层，如 3、3/18',
    `publish_status`  TINYINT        NOT NULL DEFAULT 0 COMMENT '发布状态：0 未发布、1 已发布',
    `label_codes`     VARCHAR(500)   NOT NULL DEFAULT '' COMMENT '标签编码，逗号分隔，取 infra 字典 rental_room_label',
    `facility_codes`  VARCHAR(500)   NOT NULL DEFAULT '' COMMENT '配套编码，逗号分隔，取 infra 字典 rental_room_facility',
    `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`       BIGINT         NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`       BIGINT         NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`       TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rental_room_apartment_id_room_number` (`apartment_id`, `room_number`),
    KEY `idx_rental_room_rent` (`rent`),
    KEY `idx_rental_room_publish_status` (`publish_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '房间信息表';

-- -----------------------------------------------------------------------------
-- 3. 费用项：公寓的杂费，如水费、电费、宽带费
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `rental_fee_item`;
CREATE TABLE `rental_fee_item`
(
    `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(64)    NOT NULL COMMENT '费用项名称，如 水费、电费、宽带费',
    `amount`      DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '费用金额（元）',
    `unit`        VARCHAR(16)    NOT NULL DEFAULT '' COMMENT '计价单位，如 吨、度、月；无单位时为空串',
    `create_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   BIGINT         NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`   BIGINT         NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`   TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rental_fee_item_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '费用项表';

-- -----------------------------------------------------------------------------
-- 4. 租约
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `rental_lease`;
CREATE TABLE `rental_lease`
(
    `id`                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`           BIGINT         NOT NULL COMMENT '承租人 App 用户 ID（infra App 端用户表）',
    `apartment_id`      BIGINT         NOT NULL COMMENT '签约公寓 ID',
    `room_id`           BIGINT         NOT NULL COMMENT '签约房间 ID',
    `contract_file_id`  BIGINT         NOT NULL DEFAULT 0 COMMENT '合同文件 ID（infra 文件表），0 表示尚未上传',
    `lease_start_date`  DATE           NOT NULL COMMENT '租约开始日期',
    `lease_end_date`    DATE           NOT NULL COMMENT '租约结束日期',
    `rent`              DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '签约月租金（元/月），签约时从房间快照，后续调价不影响已签合同',
    `deposit`           DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '押金（元）',
    `status`            TINYINT        NOT NULL DEFAULT 1 COMMENT '租约状态：1 签约待确认、2 已签约、3 已取消、4 已到期、5 退租待确认、6 已退租、7 续约待确认',
    `source_type`       TINYINT        NOT NULL DEFAULT 1 COMMENT '租约来源：1 新签、2 续约',
    `remark`            VARCHAR(500)   NOT NULL DEFAULT '' COMMENT '备注',
    `create_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`         BIGINT         NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`         BIGINT         NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`         TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_rental_lease_user_id` (`user_id`),
    KEY `idx_rental_lease_apartment_id` (`apartment_id`),
    KEY `idx_rental_lease_room_id` (`room_id`),
    KEY `idx_rental_lease_status` (`status`),
    KEY `idx_rental_lease_lease_end_date` (`lease_end_date`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '租约信息表';

-- -----------------------------------------------------------------------------
-- 5. 看房预约
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `rental_view_appointment`;
CREATE TABLE `rental_view_appointment`
(
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`           BIGINT       NOT NULL COMMENT '预约人 App 用户 ID',
    `apartment_id`      BIGINT       NOT NULL COMMENT '预约公寓 ID',
    `name`              VARCHAR(64)  NOT NULL COMMENT '预约人姓名（下单时快照，便于前台联系）',
    `mobile`            VARCHAR(20)  NOT NULL COMMENT '预约人手机号（下单时快照，便于前台联系）',
    `appointment_time`  DATETIME     NOT NULL COMMENT '预约看房时间',
    `status`            TINYINT      NOT NULL DEFAULT 1 COMMENT '预约状态：1 待看房、2 已取消、3 已看房',
    `remark`            VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`         BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`         BIGINT       NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_rental_view_appointment_user_id` (`user_id`),
    KEY `idx_rental_view_appointment_apartment_id` (`apartment_id`),
    KEY `idx_rental_view_appointment_status` (`status`),
    KEY `idx_rental_view_appointment_appointment_time` (`appointment_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '预约看房信息表';

-- -----------------------------------------------------------------------------
-- 6. 浏览记录（浏览时间即 create_time）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `rental_browse_history`;
CREATE TABLE `rental_browse_history`
(
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT   NOT NULL COMMENT '浏览用户 App 用户 ID',
    `room_id`     BIGINT   NOT NULL COMMENT '浏览房间 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
    `create_by`   BIGINT   NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`   BIGINT   NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`   TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_rental_browse_history_user_id_create_time` (`user_id`, `create_time`),
    KEY `idx_rental_browse_history_room_id` (`room_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '房源浏览记录表';

-- -----------------------------------------------------------------------------
-- 7. 房源图片
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `rental_image`;
CREATE TABLE `rental_image`
(
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `item_type`   TINYINT  NOT NULL COMMENT '所属对象类型：1 公寓、2 房间',
    `item_id`     BIGINT   NOT NULL COMMENT '所属对象 ID（公寓 ID 或房间 ID）',
    `file_id`     BIGINT   NOT NULL COMMENT '文件 ID（infra 文件表），访问地址按需重新签发',
    `sort`        INT      NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   BIGINT   NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`   BIGINT   NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`   TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_rental_image_item_type_item_id` (`item_type`, `item_id`, `sort`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '房源图片关系表';

-- -----------------------------------------------------------------------------
-- 8. 公寓费用项关联表：覆盖写入一律物理删除，再整体插入
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `rental_apartment_fee`;
CREATE TABLE `rental_apartment_fee`
(
    `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `apartment_id`  BIGINT   NOT NULL COMMENT '公寓 ID',
    `fee_item_id`   BIGINT   NOT NULL COMMENT '费用项 ID',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`     BIGINT   NOT NULL DEFAULT 0 COMMENT '创建人 ID，0 表示系统或未登录',
    `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`     BIGINT   NOT NULL DEFAULT 0 COMMENT '更新人 ID，0 表示系统或未登录',
    `is_delete`     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除、1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rental_apartment_fee_apartment_id_fee_item_id` (`apartment_id`, `fee_item_id`),
    KEY `idx_rental_apartment_fee_fee_item_id` (`fee_item_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '公寓费用项关联表';

-- =============================================================================
-- 初始化数据：rental 字典种子（重复执行时先清空这 5 个类型再重灌）
-- 约定：
--   1. 字典类型编码统一以 rental_ 开头，不与别的服务冲突；
--   2. 标签、配套是逗号分隔多选，编码之间不得互相包含（LIKE 前缀匹配会误命中）；
--   3. 配套图标由前端按字典值映射，字典数据表不存图标；
--   4. 不写死 id，交给自增，避免与 infra 已有字典数据撞号。
-- =============================================================================
DELETE
FROM `infra_dict_data`
WHERE `dict_type` IN ('rental_room_orientation', 'rental_apartment_label', 'rental_room_label',
                      'rental_apartment_facility', 'rental_room_facility');
DELETE
FROM `infra_dict_type`
WHERE `type` IN ('rental_room_orientation', 'rental_apartment_label', 'rental_room_label',
                 'rental_apartment_facility', 'rental_room_facility');

INSERT INTO `infra_dict_type` (`name`, `type`, `status`, `remark`, `create_by`, `update_by`)
VALUES ('房间朝向', 'rental_room_orientation', 0, 'rental 房间朝向', 0, 0),
       ('公寓标签', 'rental_apartment_label', 0, 'rental 公寓标签', 0, 0),
       ('房间标签', 'rental_room_label', 0, 'rental 房间标签', 0, 0),
       ('公寓配套', 'rental_apartment_facility', 0, 'rental 公寓配套', 0, 0),
       ('房间配套', 'rental_room_facility', 0, 'rental 房间配套', 0, 0);

INSERT INTO `infra_dict_data` (`dict_type`, `label`, `value`, `sort`, `status`, `remark`, `create_by`, `update_by`)
VALUES ('rental_room_orientation', '朝南', 'south', 1, 0, '', 0, 0),
       ('rental_room_orientation', '朝北', 'north', 2, 0, '', 0, 0),
       ('rental_room_orientation', '朝东', 'east', 3, 0, '', 0, 0),
       ('rental_room_orientation', '朝西', 'west', 4, 0, '', 0, 0),
       ('rental_room_orientation', '东南', 'southeast', 5, 0, '', 0, 0),
       ('rental_room_orientation', '西南', 'southwest', 6, 0, '', 0, 0),
       ('rental_room_orientation', '东北', 'northeast', 7, 0, '', 0, 0),
       ('rental_room_orientation', '西北', 'northwest', 8, 0, '', 0, 0),
       ('rental_apartment_label', '近地铁', 'near_subway', 1, 0, '', 0, 0),
       ('rental_apartment_label', '品牌公寓', 'brand_apartment', 2, 0, '', 0, 0),
       ('rental_apartment_label', '市中心', 'downtown', 3, 0, '', 0, 0),
       ('rental_apartment_label', '安静宜居', 'quiet', 4, 0, '', 0, 0),
       ('rental_apartment_label', '高端公寓', 'high_end', 5, 0, '', 0, 0),
       ('rental_apartment_label', '可养宠物', 'pet_friendly', 6, 0, '', 0, 0),
       ('rental_room_label', '主卧', 'master_room', 1, 0, '', 0, 0),
       ('rental_room_label', '次卧', 'second_room', 2, 0, '', 0, 0),
       ('rental_room_label', '单间', 'single_room', 3, 0, '', 0, 0),
       ('rental_room_label', '独立卫浴', 'private_bathroom', 4, 0, '', 0, 0),
       ('rental_room_label', '独立厨房', 'independent_kitchen', 5, 0, '', 0, 0),
       ('rental_room_label', '精装修', 'newly_decorated', 6, 0, '', 0, 0),
       ('rental_room_label', '采光好', 'good_light', 7, 0, '', 0, 0),
       ('rental_room_label', '带阳台', 'with_balcony', 8, 0, '', 0, 0),
       ('rental_room_label', '拎包入住', 'move_in_ready', 9, 0, '', 0, 0),
       ('rental_apartment_facility', '电梯', 'elevator', 1, 0, '', 0, 0),
       ('rental_apartment_facility', '安保', 'security', 2, 0, '', 0, 0),
       ('rental_apartment_facility', '健身房', 'gym', 3, 0, '', 0, 0),
       ('rental_apartment_facility', '停车场', 'parking', 4, 0, '', 0, 0),
       ('rental_apartment_facility', '公共洗衣房', 'laundry_room', 5, 0, '', 0, 0),
       ('rental_apartment_facility', '快递柜', 'delivery_locker', 6, 0, '', 0, 0),
       ('rental_room_facility', '空调', 'air_conditioner', 1, 0, '', 0, 0),
       ('rental_room_facility', '洗衣机', 'washing_machine', 2, 0, '', 0, 0),
       ('rental_room_facility', '冰箱', 'refrigerator', 3, 0, '', 0, 0),
       ('rental_room_facility', '热水器', 'water_heater', 4, 0, '', 0, 0),
       ('rental_room_facility', '宽带WiFi', 'broadband', 5, 0, '', 0, 0),
       ('rental_room_facility', '电视', 'tv', 6, 0, '', 0, 0),
       ('rental_room_facility', '衣柜', 'wardrobe', 7, 0, '', 0, 0),
       ('rental_room_facility', '书桌', 'desk', 8, 0, '', 0, 0),
       ('rental_room_facility', '燃气灶', 'gas_stove', 9, 0, '', 0, 0),
       ('rental_room_facility', '油烟机', 'range_hood', 10, 0, '', 0, 0);
