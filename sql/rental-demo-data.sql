-- =============================================================================
-- rental 本地联调演示数据（公寓 / 房间 / 费用项 / 图片），可重复执行
-- 执行顺序：先 infra.sql（建库与 infra 表）、infra_area.sql（省市区）、rental.sql（rental 表 / 字典 / 菜单），
--           最后执行本脚本：mysql -uroot -proot zza < sql/rental-demo-data.sql
-- 约定：
--   1. 用高位显式 ID（公寓 1001+、房间 2001+、费用项 3001+、图片 4001+），避免与手工造的数据撞号；
--   2. 主表用 INSERT ... ON DUPLICATE KEY UPDATE（顺带把 is_delete 复位），关联表先物理删再整体插入，
--      与代码里「关联表覆盖写用物理删除」的做法一致，所以本脚本可以反复执行；
--   3. district_id 取 infra_area 里的真实节点：杭州市(964) 下的西湖区(967) / 滨江区(968) / 余杭区(970)；
--   4. 标签、配套、朝向的编码都取自 rental.sql 灌入的字典种子，App / 管理端才能回填出中文名；
--   5. 图片的 file_id 引用 infra_file 里已存在的记录：这些图片是先用
--      POST /admin-api/file/upload 上传到 MinIO 后拿到 ID 的（12 张占位图，见文件末尾的对照表）。
--      换环境时请重新上传，并把下面对应的 file_id 换成新值；若还没有上传过图片，删掉图片那一段即可
--      （没有图片不会报错，只是详情里的图片地址为空）。
-- =============================================================================
USE `zza`;

-- -----------------------------------------------------------------------------
-- 1. 费用项：水费 / 电费 / 宽带费 / 物业费
-- -----------------------------------------------------------------------------
INSERT INTO `rental_fee_item` (`id`, `name`, `amount`, `unit`, `create_by`, `update_by`, `is_delete`)
VALUES (3001, '水费', 3.50, '吨', 0, 0, 0),
       (3002, '电费', 0.68, '度', 0, 0, 0),
       (3003, '宽带费', 60.00, '月', 0, 0, 0),
       (3004, '物业费', 120.00, '月', 0, 0, 0)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`),
                        `amount` = VALUES(`amount`),
                        `unit` = VALUES(`unit`),
                        `is_delete` = 0;

-- -----------------------------------------------------------------------------
-- 2. 公寓：1001 / 1002 已发布（App 端能看到），1003 未发布（用来验证 App 端只展示已发布）
--    付款方式：1 月付、2 季付、3 半年付、4 年付
-- -----------------------------------------------------------------------------
INSERT INTO `rental_apartment` (`id`, `name`, `introduction`, `district_id`, `address_detail`, `phone`,
                                `min_lease_months`, `deposit_months`, `payment_method`, `publish_status`,
                                `label_codes`, `facility_codes`, `create_by`, `update_by`, `is_delete`)
VALUES (1001, '文三路公寓',
        '文三路核心地段，步行 5 分钟到地铁 2 号线学院路站，房间独立卫浴，24 小时安保与快递柜。',
        967, '文三路 100 号', '0571-88880001', 6, 1, 1, 1,
        'near_subway,brand_apartment', 'elevator,security,delivery_locker', 0, 0, 0),
       (1002, '滨江星光公寓',
        '滨江星光大道旁，公区带健身房与停车位，房型以两室为主，适合合租。',
        968, '星光大道 66 号', '0571-88880002', 3, 1, 2, 1,
        'downtown,quiet', 'elevator,parking,gym', 0, 0, 0),
       (1003, '未来科技城公寓',
        '未来科技城五常大道，主打长租（起租一年），押二付一，配套停车位与公共洗衣房。',
        970, '五常大道 8 号', '0571-88880003', 12, 2, 1, 0,
        'high_end', 'parking,laundry_room', 0, 0, 0)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`),
                        `introduction` = VALUES(`introduction`),
                        `district_id` = VALUES(`district_id`),
                        `address_detail` = VALUES(`address_detail`),
                        `phone` = VALUES(`phone`),
                        `min_lease_months` = VALUES(`min_lease_months`),
                        `deposit_months` = VALUES(`deposit_months`),
                        `payment_method` = VALUES(`payment_method`),
                        `publish_status` = VALUES(`publish_status`),
                        `label_codes` = VALUES(`label_codes`),
                        `facility_codes` = VALUES(`facility_codes`),
                        `is_delete` = 0;

-- -----------------------------------------------------------------------------
-- 3. 房间：2001 / 2002 属于已发布公寓 1001，2003 未发布；
--          2004 / 2005 属于 1002，2006 / 2007 属于未发布的 1003（App 端同样看不到）
-- -----------------------------------------------------------------------------
INSERT INTO `rental_room` (`id`, `apartment_id`, `room_number`, `rent`, `area`, `room_count`, `orientation`,
                           `floor_no`, `publish_status`, `label_codes`, `facility_codes`,
                           `create_by`, `update_by`, `is_delete`)
VALUES (2001, 1001, '301', 3800.00, 28.50, 1, 'south', '3/18', 1,
        'master_room,private_bathroom', 'air_conditioner,washing_machine,water_heater,broadband', 0, 0, 0),
       (2002, 1001, '302', 5200.00, 45.00, 2, 'south', '3/18', 1,
        'newly_decorated,good_light,with_balcony',
        'air_conditioner,refrigerator,washing_machine,broadband,tv', 0, 0, 0),
       (2003, 1001, '401', 3600.00, 26.00, 1, 'north', '4/18', 0,
        'single_room', 'air_conditioner,broadband', 0, 0, 0),
       (2004, 1002, 'A1201', 6500.00, 55.00, 2, 'southeast', '12/26', 1,
        'newly_decorated,move_in_ready',
        'air_conditioner,refrigerator,washing_machine,water_heater,broadband,wardrobe', 0, 0, 0),
       (2005, 1002, 'A1202', 4200.00, 32.00, 1, 'east', '12/26', 1,
        'master_room,good_light', 'air_conditioner,washing_machine,broadband,desk', 0, 0, 0),
       (2006, 1003, 'B0801', 2600.00, 24.00, 1, 'west', '8/20', 1,
        'single_room', 'air_conditioner,broadband,wardrobe', 0, 0, 0),
       (2007, 1003, 'B0802', 3900.00, 40.00, 2, 'southwest', '8/20', 1,
        'second_room,with_balcony', 'air_conditioner,refrigerator,gas_stove,range_hood,broadband', 0, 0, 0)
ON DUPLICATE KEY UPDATE `apartment_id` = VALUES(`apartment_id`),
                        `room_number` = VALUES(`room_number`),
                        `rent` = VALUES(`rent`),
                        `area` = VALUES(`area`),
                        `room_count` = VALUES(`room_count`),
                        `orientation` = VALUES(`orientation`),
                        `floor_no` = VALUES(`floor_no`),
                        `publish_status` = VALUES(`publish_status`),
                        `label_codes` = VALUES(`label_codes`),
                        `facility_codes` = VALUES(`facility_codes`),
                        `is_delete` = 0;

-- -----------------------------------------------------------------------------
-- 4. 公寓费用项关联：覆盖写，先按公寓物理删再整体插入
-- -----------------------------------------------------------------------------
DELETE
FROM `rental_apartment_fee`
WHERE `apartment_id` IN (1001, 1002, 1003);

INSERT INTO `rental_apartment_fee` (`apartment_id`, `fee_item_id`, `create_by`, `update_by`)
VALUES (1001, 3001, 0, 0),
       (1001, 3002, 0, 0),
       (1001, 3003, 0, 0),
       (1002, 3001, 0, 0),
       (1002, 3002, 0, 0),
       (1002, 3004, 0, 0),
       (1003, 3003, 0, 0),
       (1003, 3004, 0, 0);

-- -----------------------------------------------------------------------------
-- 5. 房源图片：item_type 1 公寓 / 2 房间，sort 越小越靠前（0 就是封面图）
--    下面这段依赖 infra_file 里已经上传好的图片记录（file_id 见文件末尾对照表）
-- -----------------------------------------------------------------------------
DELETE
FROM `rental_image`
WHERE (`item_type`, `item_id`) IN ((1, 1001), (1, 1002), (1, 1003),
                                   (2, 2001), (2, 2002), (2, 2004), (2, 2005), (2, 2006), (2, 2007));

INSERT INTO `rental_image` (`id`, `item_type`, `item_id`, `file_id`, `sort`, `create_by`, `update_by`)
VALUES (4001, 1, 1001, 1, 0, 0, 0),
       (4002, 1, 1001, 2, 1, 0, 0),
       (4003, 1, 1002, 3, 0, 0, 0),
       (4004, 1, 1002, 4, 1, 0, 0),
       (4005, 1, 1003, 5, 0, 0, 0),
       (4006, 1, 1003, 6, 1, 0, 0),
       (4007, 2, 2001, 7, 0, 0, 0),
       (4008, 2, 2002, 8, 0, 0, 0),
       (4009, 2, 2004, 9, 0, 0, 0),
       (4010, 2, 2005, 10, 0, 0, 0),
       (4011, 2, 2006, 11, 0, 0, 0),
       (4012, 2, 2007, 12, 0, 0, 0);

-- =============================================================================
-- 图片对照表（本机上传后 infra_file 里的记录）
--   file_id 1  apt-1001-0.jpg  文三路公寓 客厅（封面）
--   file_id 2  apt-1001-1.jpg  文三路公寓 楼栋外观
--   file_id 3  apt-1002-0.jpg  滨江星光公寓 公区（封面）
--   file_id 4  apt-1002-1.jpg  滨江星光公寓 楼栋外观
--   file_id 5  apt-1003-0.jpg  未来科技城公寓 大堂（封面）
--   file_id 6  apt-1003-1.jpg  未来科技城公寓 楼栋
--   file_id 7  room-2001-0.jpg 301 室 主卧（封面）
--   file_id 8  room-2002-0.jpg 302 室 客厅（封面）
--   file_id 9  room-2004-0.jpg A1201 室 主卧（封面）
--   file_id 10 room-2005-0.jpg A1202 室 单间（封面）
--   file_id 11 room-2006-0.jpg B0801 室 单间（封面）
--   file_id 12 room-2007-0.jpg B0802 室 客厅（封面）
-- =============================================================================
