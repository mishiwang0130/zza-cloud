package com.wxy.rental.biz.service;

import java.time.LocalDateTime;

/**
 * 浏览记录写入服务：由 App 房间详情接口触发的 MQ 消费者调用。
 *
 * <p>同一用户看同一房间只留一条记录，重复浏览刷新浏览时间：这是「最近看过哪些房」的口径——同一个房间看十次，历史里也只是一条，只是排到了最前面。
 *
 * @author wxy
 * @date 2026/10/04
 */
public interface RentalBrowseHistoryService {

    /**
     * 记录一次浏览：已有记录则刷新浏览时间，没有则新增
     *
     * @param userId     浏览用户 ID
     * @param roomId     被浏览的房间 ID
     * @param browseTime 浏览时间，可以为 null（按当前时间处理）
     */
    void record(Long userId, Long roomId, LocalDateTime browseTime);
}
