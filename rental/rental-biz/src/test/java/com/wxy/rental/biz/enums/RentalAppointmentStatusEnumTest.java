package com.wxy.rental.biz.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 看房预约状态流转单元测试：管理端只允许待看房流转。
 *
 * @author wxy
 * @date 2026/10/04
 */
class RentalAppointmentStatusEnumTest {

    /**
     * 待看房可以标记已看房或已取消
     */
    @Test
    @DisplayName("canTransitionTo：待看房可流转为已看房 / 已取消")
    void shouldAllowTransitionsFromPending() {
        assertThat(RentalAppointmentStatusEnum.PENDING.canTransitionTo(RentalAppointmentStatusEnum.VIEWED)).isTrue();
        assertThat(RentalAppointmentStatusEnum.PENDING.canTransitionTo(RentalAppointmentStatusEnum.CANCELED)).isTrue();
    }

    /**
     * 终态不允许再流转
     */
    @Test
    @DisplayName("canTransitionTo：已看房与已取消是终态")
    void shouldRejectTransitionsFromFinalStatuses() {
        assertThat(RentalAppointmentStatusEnum.VIEWED.canTransitionTo(RentalAppointmentStatusEnum.PENDING)).isFalse();
        assertThat(RentalAppointmentStatusEnum.CANCELED.canTransitionTo(RentalAppointmentStatusEnum.VIEWED)).isFalse();
        assertThat(RentalAppointmentStatusEnum.PENDING.canTransitionTo(null)).isFalse();
    }
}
