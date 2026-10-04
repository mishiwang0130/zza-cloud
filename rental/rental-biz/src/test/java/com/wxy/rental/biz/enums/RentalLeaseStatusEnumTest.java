package com.wxy.rental.biz.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 租约状态流转单元测试：把契约稿第 3 节的流转表逐条钉住。
 *
 * <p>状态迁移写错的后果是运营能把已退租的合同改回已签约，只能靠测试兜住。
 *
 * @author wxy
 * @date 2026/10/04
 */
class RentalLeaseStatusEnumTest {

    /**
     * 允许的迁移必须放行
     */
    @Test
    @DisplayName("canTransitionTo：契约稿允许的迁移全部放行")
    void shouldAllowConfiguredTransitions() {
        assertThat(RentalLeaseStatusEnum.PENDING_CONFIRM.canTransitionTo(RentalLeaseStatusEnum.SIGNED)).isTrue();
        assertThat(RentalLeaseStatusEnum.PENDING_CONFIRM.canTransitionTo(RentalLeaseStatusEnum.CANCELED)).isTrue();
        assertThat(RentalLeaseStatusEnum.SIGNED.canTransitionTo(RentalLeaseStatusEnum.EXPIRED)).isTrue();
        assertThat(RentalLeaseStatusEnum.SIGNED.canTransitionTo(RentalLeaseStatusEnum.WITHDRAW_PENDING)).isTrue();
        assertThat(RentalLeaseStatusEnum.SIGNED.canTransitionTo(RentalLeaseStatusEnum.RENEW_PENDING)).isTrue();
        assertThat(RentalLeaseStatusEnum.WITHDRAW_PENDING.canTransitionTo(RentalLeaseStatusEnum.SIGNED)).isTrue();
        assertThat(RentalLeaseStatusEnum.WITHDRAW_PENDING.canTransitionTo(RentalLeaseStatusEnum.WITHDRAWN)).isTrue();
        assertThat(RentalLeaseStatusEnum.RENEW_PENDING.canTransitionTo(RentalLeaseStatusEnum.SIGNED)).isTrue();
    }

    /**
     * 终态与跳跃迁移必须拒绝
     */
    @Test
    @DisplayName("canTransitionTo：终态与跨状态跳跃一律拒绝")
    void shouldRejectInvalidTransitions() {
        assertThat(RentalLeaseStatusEnum.CANCELED.canTransitionTo(RentalLeaseStatusEnum.SIGNED)).isFalse();
        assertThat(RentalLeaseStatusEnum.EXPIRED.canTransitionTo(RentalLeaseStatusEnum.SIGNED)).isFalse();
        assertThat(RentalLeaseStatusEnum.WITHDRAWN.canTransitionTo(RentalLeaseStatusEnum.SIGNED)).isFalse();
        assertThat(RentalLeaseStatusEnum.PENDING_CONFIRM.canTransitionTo(RentalLeaseStatusEnum.EXPIRED)).isFalse();
        assertThat(RentalLeaseStatusEnum.SIGNED.canTransitionTo(RentalLeaseStatusEnum.WITHDRAWN)).isFalse();
        assertThat(RentalLeaseStatusEnum.SIGNED.canTransitionTo(null)).isFalse();
    }

    /**
     * 生效中的状态恰好是 1/2/5，房间占用判断与它对齐
     */
    @Test
    @DisplayName("effectiveStatuses：生效中的状态为 1/2/5")
    void effectiveStatusesShouldContainPendingSignedAndWithdrawPending() {
        assertThat(RentalLeaseStatusEnum.effectiveStatuses()).containsExactlyInAnyOrder(
                RentalLeaseStatusEnum.PENDING_CONFIRM,
                RentalLeaseStatusEnum.SIGNED,
                RentalLeaseStatusEnum.WITHDRAW_PENDING);
    }

    /**
     * 按值取枚举与中文名
     */
    @Test
    @DisplayName("of / labelOf：按值取枚举与中文名")
    void shouldResolveValueAndLabel() {
        assertThat(RentalLeaseStatusEnum.of(2)).isEqualTo(RentalLeaseStatusEnum.SIGNED);
        assertThat(RentalLeaseStatusEnum.labelOf(7)).isEqualTo("续约待确认");
        assertThat(RentalLeaseStatusEnum.of(99)).isNull();
        assertThat(RentalLeaseStatusEnum.labelOf(null)).isNull();
    }
}
