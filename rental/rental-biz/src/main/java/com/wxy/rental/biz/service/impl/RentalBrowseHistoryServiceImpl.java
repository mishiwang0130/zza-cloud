package com.wxy.rental.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wxy.rental.biz.mapper.RentalBrowseHistoryMapper;
import com.wxy.rental.biz.po.RentalBrowseHistory;
import com.wxy.rental.biz.service.RentalBrowseHistoryService;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 浏览记录写入实现：先按「用户 + 房间」找已有记录，有就刷新浏览时间，没有才插入。
 *
 * <p>库上只有 {@code (user_id, create_time)} 与 {@code room_id} 的普通索引，没有「用户 + 房间」唯一键
 * （建表脚本本期不改结构），所以去重靠这段逻辑保证。两个并发的同房间浏览理论上可能各插一条，
 * 但这种极端情况下多一条流水没有实际影响，不值得为它加分布式锁。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Service
public class RentalBrowseHistoryServiceImpl implements RentalBrowseHistoryService {

    /** 浏览记录 Mapper */
    @Resource
    private RentalBrowseHistoryMapper rentalBrowseHistoryMapper;

    /**
     * 记录一次浏览：已有记录则刷新浏览时间，没有则新增
     *
     * @param userId     浏览用户 ID
     * @param roomId     被浏览的房间 ID
     * @param browseTime 浏览时间，可以为 null（按当前时间处理）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(Long userId, Long roomId, LocalDateTime browseTime) {
        LocalDateTime effectiveBrowseTime = browseTime == null ? LocalDateTime.now() : browseTime;
        List<RentalBrowseHistory> existingList = rentalBrowseHistoryMapper.selectList(
                new LambdaQueryWrapper<RentalBrowseHistory>()
                        .eq(RentalBrowseHistory::getUserId, userId)
                        .eq(RentalBrowseHistory::getRoomId, roomId)
                        .orderByDesc(RentalBrowseHistory::getId));
        if (!existingList.isEmpty()) {
            // 浏览时间就是 create_time：重复浏览把它刷到最后一次浏览的时刻，列表按它倒序自然排到最前
            RentalBrowseHistory existing = existingList.get(0);
            existing.setCreateTime(effectiveBrowseTime);
            rentalBrowseHistoryMapper.updateById(existing);
            return;
        }
        RentalBrowseHistory po = new RentalBrowseHistory();
        po.setUserId(userId);
        po.setRoomId(roomId);
        po.setCreateTime(effectiveBrowseTime);
        rentalBrowseHistoryMapper.insert(po);
    }
}
