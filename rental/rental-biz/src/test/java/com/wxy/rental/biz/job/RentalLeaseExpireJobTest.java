package com.wxy.rental.biz.job;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wxy.rental.biz.mapper.RentalLeaseMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 租约到期定时任务单元测试：只校验调用口径（当天日期 + 系统操作人）。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class RentalLeaseExpireJobTest {

    /** 租约 Mapper */
    @Mock
    private RentalLeaseMapper rentalLeaseMapper;

    /** 被测任务 */
    private RentalLeaseExpireJob leaseExpireJob;

    /**
     * 装配被测任务
     */
    @BeforeEach
    void setUp() {
        leaseExpireJob = new RentalLeaseExpireJob();
        ReflectionTestUtils.setField(leaseExpireJob, "rentalLeaseMapper", rentalLeaseMapper);
    }

    /**
     * 扫描时用当天日期与系统操作人 ID 调用批量更新
     */
    @Test
    @DisplayName("expireLeases：按当天日期与系统操作人执行到期批量更新")
    void expireLeasesShouldUpdateWithTodayAndSystemUser() {
        when(rentalLeaseMapper.updateExpiredLeases(ArgumentMatchers.any(LocalDate.class),
                ArgumentMatchers.anyLong())).thenReturn(3);

        leaseExpireJob.expireLeases();

        verify(rentalLeaseMapper).updateExpiredLeases(LocalDate.now(), 0L);
    }
}
