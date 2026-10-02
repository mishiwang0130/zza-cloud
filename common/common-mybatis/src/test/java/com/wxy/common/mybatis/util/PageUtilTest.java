package com.wxy.common.mybatis.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxy.common.core.vo.PageReqVO;
import com.wxy.common.core.vo.PageRespVO;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 分页转换测试：入参为空按默认值处理，数据库结果按原样转换。
 *
 * @author wxy
 * @date 2026/10/02
 */
class PageUtilTest {

    @Test
    @DisplayName("分页入参为空时使用默认页码与条数")
    void shouldUseDefaultWhenReqIsNull() {
        Page<Object> page = PageUtil.toPage(null);

        assertThat(page.getCurrent()).isEqualTo(PageReqVO.DEFAULT_PAGE_NUM);
        assertThat(page.getSize()).isEqualTo(PageReqVO.DEFAULT_PAGE_SIZE);
    }

    @Test
    @DisplayName("把数据库分页结果转成统一返回体")
    void shouldConvertPageToResp() {
        Page<String> page = new Page<>(2, 10);
        page.setTotal(25);
        page.setRecords(List.of("a", "b"));

        PageRespVO<String> resp = PageUtil.of(page);

        assertThat(resp.getTotal()).isEqualTo(25L);
        assertThat(resp.getPageNum()).isEqualTo(2);
        assertThat(resp.getPageSize()).isEqualTo(10);
        assertThat(resp.getRecords()).containsExactly("a", "b");
    }

    @Test
    @DisplayName("数据库分页结果为空时返回空分页")
    void shouldReturnEmptyWhenPageIsNull() {
        PageRespVO<Object> resp = PageUtil.of(null);

        assertThat(resp.getTotal()).isZero();
        assertThat(resp.getRecords()).isEmpty();
    }
}
