package com.wxy.rental.biz.constant;

import com.wxy.common.core.result.ErrorCode;

/**
 * rental 业务错误码常量：服务位固定 {@code 03}，模块位 000 跨模块通用、001 公寓、002 房间、
 * 003 费用项、004 租约、005 预约、006 浏览、007 图片。
 *
 * <p>公共错误码（参数错误、未登录、无权限、系统异常等）用 {@code CommonErrorConstant}，
 * 这里只放 rental 自己的业务错误；业务代码只引用常量，禁止出现数字字面量。
 *
 * <p>浏览记录模块（006）没有业务错误码：写入是 MQ 异步的，发送失败只记日志、
 * 消费失败抛出让 RocketMQ 重试，都不该变成接口错误码。
 *
 * @author wxy
 * @date 2026/10/04
 */
public final class RentalErrorConstant {

    // ==================== 000 跨模块通用 ====================

    /** 字典编码不存在或已停用：提交的标签 / 配套 / 朝向编码不在 infra 字典里 */
    public static final ErrorCode DICT_CODE_INVALID = new ErrorCode(1_03_000_0001, "字典编码不存在或已停用");

    /** 行政区划不存在：提交的区县 ID 不在 infra 行政区划表里 */
    public static final ErrorCode AREA_NOT_FOUND = new ErrorCode(1_03_000_0002, "行政区划不存在");

    /** 依赖的基础服务调用失败：调 infra 时出现非业务异常（熔断、序列化、连接失败等） */
    public static final ErrorCode REMOTE_SERVICE_ERROR = new ErrorCode(1_03_000_0003, "依赖的基础服务调用失败");

    // ==================== 001 公寓 ====================

    /** 公寓不存在：按 ID 查不到未删除的公寓 */
    public static final ErrorCode APARTMENT_NOT_FOUND = new ErrorCode(1_03_001_0001, "公寓不存在");

    /** 公寓下还有已发布房间，不能下架 */
    public static final ErrorCode APARTMENT_HAS_ROOM = new ErrorCode(1_03_001_0002, "公寓下还有已发布房间，不能下架");

    // ==================== 002 房间 ====================

    /** 房间不存在 */
    public static final ErrorCode ROOM_NOT_FOUND = new ErrorCode(1_03_002_0001, "房间不存在");

    /** 同一公寓下房间号已存在 */
    public static final ErrorCode ROOM_NUMBER_EXISTS = new ErrorCode(1_03_002_0002, "同一公寓下房间号已存在");

    /** 房间不属于该公寓：签约时传入的 apartmentId 与房间实际所属公寓不一致 */
    public static final ErrorCode ROOM_APARTMENT_MISMATCH = new ErrorCode(1_03_002_0003, "房间不属于该公寓");

    /** 房间存在生效中的租约，不能下架 */
    public static final ErrorCode ROOM_HAS_LEASE = new ErrorCode(1_03_002_0004, "房间存在生效中的租约，不能下架");

    // ==================== 003 费用项 ====================

    /** 费用项不存在 */
    public static final ErrorCode FEE_ITEM_NOT_FOUND = new ErrorCode(1_03_003_0001, "费用项不存在");

    /** 费用项名称已存在：名称唯一，按未删除的数据判断 */
    public static final ErrorCode FEE_ITEM_NAME_EXISTS = new ErrorCode(1_03_003_0002, "费用项名称已存在");

    /** 费用项已被公寓引用，不能删除 */
    public static final ErrorCode FEE_ITEM_IN_USE = new ErrorCode(1_03_003_0003, "费用项已被公寓引用，不能删除");

    // ==================== 004 租约 ====================

    /** 租约不存在 */
    public static final ErrorCode LEASE_NOT_FOUND = new ErrorCode(1_03_004_0001, "租约不存在");

    /** 房间已有生效中的租约：同一房间同时只能有一份状态为 1/2/5 的租约 */
    public static final ErrorCode ROOM_LEASE_EXISTS = new ErrorCode(1_03_004_0002, "该房间已有生效中的租约");

    /** 租约结束日期必须晚于开始日期 */
    public static final ErrorCode LEASE_DATE_INVALID = new ErrorCode(1_03_004_0003, "租约结束日期必须晚于开始日期");

    /** 租约状态不允许这样流转 */
    public static final ErrorCode LEASE_STATUS_TRANSITION_INVALID =
            new ErrorCode(1_03_004_0004, "租约状态不允许这样流转");

    /** 该状态的租约不允许修改条款：已取消 / 已到期 / 已退租只能改合同文件与备注 */
    public static final ErrorCode LEASE_UPDATE_FORBIDDEN = new ErrorCode(1_03_004_0005, "该状态的租约不允许修改条款");

    // ==================== 005 预约 ====================

    /** 预约记录不存在 */
    public static final ErrorCode APPOINTMENT_NOT_FOUND = new ErrorCode(1_03_005_0001, "预约记录不存在");

    /** 只能取消自己的待看房预约（用户端用，本期未实现接口，常量先按契约占位） */
    public static final ErrorCode APPOINTMENT_CANCEL_FORBIDDEN =
            new ErrorCode(1_03_005_0002, "只能取消自己的待看房预约");

    /** 预约状态不允许这样流转 */
    public static final ErrorCode APPOINTMENT_STATUS_TRANSITION_INVALID =
            new ErrorCode(1_03_005_0003, "预约状态不允许这样流转");

    // ==================== 007 图片 ====================

    /** 图片所属对象类型不合法：只允许 1 公寓、2 房间 */
    public static final ErrorCode IMAGE_ITEM_TYPE_INVALID = new ErrorCode(1_03_007_0001, "图片所属对象类型不合法");

    /** 图片文件不存在：图片引用的 fileId 在 infra 文件表里查不到 */
    public static final ErrorCode IMAGE_FILE_NOT_FOUND = new ErrorCode(1_03_007_0002, "图片文件不存在");

    /**
     * 工具类常量类，禁止实例化
     */
    private RentalErrorConstant() {
    }
}
