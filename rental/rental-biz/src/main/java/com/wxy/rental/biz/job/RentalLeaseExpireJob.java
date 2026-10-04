package com.wxy.rental.biz.job;

import com.wxy.common.core.constant.CommonConstant;
import com.wxy.rental.biz.mapper.RentalLeaseMapper;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 租约到期定时任务：每天把「已签约且结束日期早于今天」的租约置为 4 已到期。
 *
 * <p>为什么用置状态而不是删除或另建到期记录：租约是合同，必须留下完整历史；
 * 到期只是一个状态，改了状态历史数据一条不少，统计口径也不会断。
 *
 * <p>触发方式用 Spring 的 {@code @Scheduled}（启动类已开启 {@code @EnableScheduling}）：
 * 本服务目前没有引入分布式调度平台，先用进程内定时器；将来接入 XXL-Job 这类平台时，
 * 把触发从这里挪到平台即可（业务逻辑不用动）。
 *
 * <p>多实例同时跑是安全的：更新语句只动状态仍为 2 的行，重复执行不会误改已退租 / 已取消的租约。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Slf4j
@Component
public class RentalLeaseExpireJob {

    /** 每天凌晨 1 点扫描一次：避开零点与业务高峰，也只扫一次 */
    private static final String EXPIRE_CRON = "0 0 1 * * ?";

    /** 租约 Mapper */
    @Resource
    private RentalLeaseMapper rentalLeaseMapper;

    /**
     * 扫描到期租约并置为已到期
     */
    @Scheduled(cron = EXPIRE_CRON)
    public void expireLeases() {
        LocalDate today = LocalDate.now();
        int updated = rentalLeaseMapper.updateExpiredLeases(today, CommonConstant.SYSTEM_USER_ID);
        log.info("[expireLeases][租约到期扫描完成] today={}, updatedRows={}", today, updated);
    }
}
