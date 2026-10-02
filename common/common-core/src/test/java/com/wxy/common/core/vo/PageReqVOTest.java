package com.wxy.common.core.vo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 分页入参边界收敛的测试：保证调用方传非法分页参数时不会打到数据库。
 *
 * @author wxy
 * @date 2026/10/02
 */
class PageReqVOTest {

    @Test
    @DisplayName("默认页码为 1、默认条数为 20")
    void shouldUseDefaultValues() {
        PageReqVO reqVO = new PageReqVO();
        assertThat(reqVO.getPageNum()).isEqualTo(PageReqVO.DEFAULT_PAGE_NUM);
        assertThat(reqVO.getPageSize()).isEqualTo(PageReqVO.DEFAULT_PAGE_SIZE);
    }

    @Test
    @DisplayName("页码小于 1 或为 null 时收敛为 1")
    void shouldClampPageNum() {
        PageReqVO reqVO = new PageReqVO();
        reqVO.setPageNum(0);
        assertThat(reqVO.getPageNum()).isEqualTo(1);
        reqVO.setPageNum(null);
        assertThat(reqVO.getPageNum()).isEqualTo(1);
        reqVO.setPageNum(3);
        assertThat(reqVO.getPageNum()).isEqualTo(3);
    }

    @Test
    @DisplayName("条数超过上限时收敛为上限值")
    void shouldClampPageSizeToMax() {
        PageReqVO reqVO = new PageReqVO();
        reqVO.setPageSize(PageReqVO.MAX_PAGE_SIZE + 1);
        assertThat(reqVO.getPageSize()).isEqualTo(PageReqVO.MAX_PAGE_SIZE);
    }

    @Test
    @DisplayName("条数小于 1 或为 null 时回退为默认值")
    void shouldFallbackPageSizeToDefault() {
        PageReqVO reqVO = new PageReqVO();
        reqVO.setPageSize(0);
        assertThat(reqVO.getPageSize()).isEqualTo(PageReqVO.DEFAULT_PAGE_SIZE);
        reqVO.setPageSize(null);
        assertThat(reqVO.getPageSize()).isEqualTo(PageReqVO.DEFAULT_PAGE_SIZE);
    }
}
