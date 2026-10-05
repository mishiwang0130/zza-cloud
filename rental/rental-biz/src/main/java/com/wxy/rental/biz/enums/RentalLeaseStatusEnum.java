package com.wxy.rental.biz.enums;

import java.util.EnumSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 租约状态：对应 {@code rental_lease.status}，并承载「允许怎么流转」这条业务规则。
 *
 * <p>把流转规则放在枚举里而不是散在 Service 的 if 里，是因为它就是这个状态机的定义：
 * 新增状态、调整允许的迁移时只改一处，管理端与后续的 App 端租约接口复用同一份判断。
 *
 * <p>流转表（契约稿第 3 节）：
 * <ul>
 *   <li>1 签约待确认 → 2 已签约、3 已取消</li>
 *   <li>2 已签约 → 4 已到期、5 退租待确认、7 续约待确认</li>
 *   <li>5 退租待确认 → 2 已签约（驳回退租）、6 已退租（确认退租，同时把租期结束日期改到实际退租日期）</li>
 *   <li>7 续约待确认 → 2 已签约（续约完成或驳回）</li>
 *   <li>3 已取消 / 4 已到期 / 6 已退租 是终态，不允许再流转</li>
 * </ul>
 *
 * @author wxy
 * @date 2026/10/04
 */
@Getter
@AllArgsConstructor
public enum RentalLeaseStatusEnum {

    /** 签约待确认：租约已创建，等待签约确认 */
    PENDING_CONFIRM(1, "签约待确认"),

    /** 已签约：生效中的租约 */
    SIGNED(2, "已签约"),

    /** 已取消：终态，合同不物理删除，靠该状态作废 */
    CANCELED(3, "已取消"),

    /** 已到期：终态，由到期定时任务把到期未续约的租约置为该状态 */
    EXPIRED(4, "已到期"),

    /** 退租待确认：发起退租后等待确认 */
    WITHDRAW_PENDING(5, "退租待确认"),

    /** 已退租：终态 */
    WITHDRAWN(6, "已退租"),

    /** 续约待确认：等待续约完成或驳回 */
    RENEW_PENDING(7, "续约待确认");

    /** 入库值 */
    private final Integer value;

    /** 中文描述，直接展示给前端 */
    private final String label;

    /**
     * 按入库值查找枚举
     *
     * @param value 入库值，可以为 null
     * @return 匹配的枚举，找不到时返回 null
     */
    public static RentalLeaseStatusEnum of(Integer value) {
        for (RentalLeaseStatusEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 按入库值取中文描述
     *
     * @param value 入库值，可以为 null
     * @return 中文描述，找不到时返回 null
     */
    public static String labelOf(Integer value) {
        RentalLeaseStatusEnum item = of(value);
        return item == null ? null : item.label;
    }

    /**
     * 生效中的租约状态集合：这些状态的租约占着房间，房间不能下架、也不能再签新租约。
     *
     * @return 生效中的状态集合
     */
    public static Set<RentalLeaseStatusEnum> effectiveStatuses() {
        return EnumSet.of(PENDING_CONFIRM, SIGNED, WITHDRAW_PENDING);
    }

    /**
     * 判断本状态是否为终态（不允许再流转，也不允许修改条款）
     *
     * @return 终态返回 true
     */
    public boolean isFinal() {
        return this == CANCELED || this == EXPIRED || this == WITHDRAWN;
    }

    /**
     * 判断本状态能否流转到目标状态
     *
     * @param target 目标状态，可以为 null
     * @return 允许流转时返回 true
     */
    public boolean canTransitionTo(RentalLeaseStatusEnum target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case PENDING_CONFIRM -> target == SIGNED || target == CANCELED;
            case SIGNED -> target == EXPIRED || target == WITHDRAW_PENDING || target == RENEW_PENDING;
            case WITHDRAW_PENDING -> target == SIGNED || target == WITHDRAWN;
            case RENEW_PENDING -> target == SIGNED;
            // 终态：不允许再流转
            case CANCELED, EXPIRED, WITHDRAWN -> false;
        };
    }
}
