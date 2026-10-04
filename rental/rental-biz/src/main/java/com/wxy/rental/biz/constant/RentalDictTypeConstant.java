package com.wxy.rental.biz.constant;

/**
 * rental 用到的 infra 字典类型编码：与 {@code sql/rental.sql} 里灌入的字典种子一一对应。
 *
 * <p>字典编码是跨服务契约（rental 存编码、infra 存中文名），写错一个字就会静默变成
 * 「字典里查不到这个编码」，所以集中放这里，不散落在各 Service 的字符串字面量里。
 *
 * @author wxy
 * @date 2026/10/04
 */
public final class RentalDictTypeConstant {

    /** 公寓标签 */
    public static final String APARTMENT_LABEL = "rental_apartment_label";

    /** 公寓配套 */
    public static final String APARTMENT_FACILITY = "rental_apartment_facility";

    /** 房间标签 */
    public static final String ROOM_LABEL = "rental_room_label";

    /** 房间配套 */
    public static final String ROOM_FACILITY = "rental_room_facility";

    /** 房间朝向 */
    public static final String ROOM_ORIENTATION = "rental_room_orientation";

    /**
     * 工具类常量类，禁止实例化
     */
    private RentalDictTypeConstant() {
    }
}
