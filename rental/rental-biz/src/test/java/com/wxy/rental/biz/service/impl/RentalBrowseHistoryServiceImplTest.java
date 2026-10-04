package com.wxy.rental.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wxy.rental.biz.mapper.RentalBrowseHistoryMapper;
import com.wxy.rental.biz.po.RentalBrowseHistory;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 浏览记录写入单元测试：同用户同房间只留一条、重复浏览刷新时间。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class RentalBrowseHistoryServiceImplTest {

    /** 浏览记录 Mapper */
    @Mock
    private RentalBrowseHistoryMapper rentalBrowseHistoryMapper;

    /** 被测服务 */
    private RentalBrowseHistoryServiceImpl browseHistoryService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        browseHistoryService = new RentalBrowseHistoryServiceImpl();
        ReflectionTestUtils.setField(browseHistoryService, "rentalBrowseHistoryMapper", rentalBrowseHistoryMapper);
    }

    /**
     * 没有记录时插入一条，浏览时间落到 createTime
     */
    @Test
    @DisplayName("record：首次浏览时插入记录")
    void recordShouldInsertWhenAbsent() {
        LocalDateTime browseTime = LocalDateTime.of(2026, 10, 4, 15, 30);
        when(rentalBrowseHistoryMapper.selectList(any())).thenReturn(List.of());

        browseHistoryService.record(100L, 5L, browseTime);

        ArgumentCaptor<RentalBrowseHistory> captor = ArgumentCaptor.forClass(RentalBrowseHistory.class);
        verify(rentalBrowseHistoryMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(100L);
        assertThat(captor.getValue().getRoomId()).isEqualTo(5L);
        assertThat(captor.getValue().getCreateTime()).isEqualTo(browseTime);
    }

    /**
     * 已有记录时只刷新浏览时间，不再插入
     */
    @Test
    @DisplayName("record：重复浏览时刷新时间且不新增")
    void recordShouldRefreshExistingTime() {
        RentalBrowseHistory existing = new RentalBrowseHistory();
        existing.setId(1L);
        existing.setUserId(100L);
        existing.setRoomId(5L);
        existing.setCreateTime(LocalDateTime.of(2026, 10, 1, 9, 0));
        when(rentalBrowseHistoryMapper.selectList(any())).thenReturn(List.of(existing));
        LocalDateTime browseTime = LocalDateTime.of(2026, 10, 4, 15, 30);

        browseHistoryService.record(100L, 5L, browseTime);

        assertThat(existing.getCreateTime()).isEqualTo(browseTime);
        verify(rentalBrowseHistoryMapper).updateById(existing);
        verify(rentalBrowseHistoryMapper, never()).insert(any(RentalBrowseHistory.class));
    }

    /**
     * 浏览时间为空时用当前时间兜底
     */
    @Test
    @DisplayName("record：浏览时间为空时用当前时间")
    void recordShouldDefaultBrowseTime() {
        LocalDateTime before = LocalDateTime.now();
        when(rentalBrowseHistoryMapper.selectList(any())).thenReturn(List.of());

        browseHistoryService.record(100L, 5L, null);

        ArgumentCaptor<RentalBrowseHistory> captor = ArgumentCaptor.forClass(RentalBrowseHistory.class);
        verify(rentalBrowseHistoryMapper).insert(captor.capture());
        assertThat(captor.getValue().getCreateTime()).isNotNull();
        assertThat(captor.getValue().getCreateTime()).isAfterOrEqualTo(before);
    }

    /**
     * 历史脏数据（同用户同房间多条）只更新最新一条，不抛异常
     */
    @Test
    @DisplayName("record：同用户同房间多条历史数据时不抛异常")
    void recordShouldTolerateDuplicateRows() {
        RentalBrowseHistory newest = new RentalBrowseHistory();
        newest.setId(9L);
        RentalBrowseHistory older = new RentalBrowseHistory();
        older.setId(2L);
        when(rentalBrowseHistoryMapper.selectList(any())).thenReturn(List.of(newest, older));

        assertThatCode(() -> browseHistoryService.record(100L, 5L, LocalDateTime.now()))
                .doesNotThrowAnyException();
        verify(rentalBrowseHistoryMapper).updateById(newest);
    }
}
