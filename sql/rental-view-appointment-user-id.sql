-- =============================================================================
-- rental 变更脚本（MySQL 8）：看房预约只存预约人 ID，不再快照姓名与手机号
-- 执行顺序：先执行 sql/rental.sql（建库建表），再执行本脚本
-- 约定：
--   1. 建表总脚本 sql/rental.sql 只追加不修改，本次变更单独成文；
--      新库按「rental.sql → 本脚本」的顺序执行，老库直接执行本脚本即可；
--   2. rental_view_appointment.user_id 已经存在（索引 idx_rental_view_appointment_user_id），
--      预约人姓名与手机号改为后台按 user_id 查 infra 的用户表（infra_app_user）动态获得，
--      因此 name / mobile 两列不再需要，直接删除；
--   3. 删列前如需保留历史数据，先把 name / mobile 备份到临时表再执行本脚本。
-- =============================================================================

USE `zza`;

ALTER TABLE `rental_view_appointment`
    DROP COLUMN `name`,
    DROP COLUMN `mobile`;
