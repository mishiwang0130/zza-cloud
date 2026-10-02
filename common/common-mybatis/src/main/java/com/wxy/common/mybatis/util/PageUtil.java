package com.wxy.common.mybatis.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxy.common.core.vo.PageReqVO;
import com.wxy.common.core.vo.PageRespVO;
import java.util.List;

/**
 * 分页转换工具：把分页入参转成 MyBatis-Plus 的 {@code Page}，把查询结果转成统一的分页返回体。
 *
 * <p>各服务不要重复写这两段转换：分页参数与返回结构只维护一套，分页行为才一致。
 *
 * @author wxy
 * @date 2026/10/02
 */
public final class PageUtil {

    /**
     * 工具类，禁止实例化
     */
    private PageUtil() {
    }

    /**
     * 把分页入参转成 MyBatis-Plus 的分页对象
     *
     * @param reqVO 分页入参，可以为 null（按默认值处理）
     * @param <T>   实体类型
     * @return MyBatis-Plus 分页对象
     */
    public static <T> Page<T> toPage(PageReqVO reqVO) {
        PageReqVO pageReq = reqVO == null ? new PageReqVO() : reqVO;
        return new Page<>(pageReq.getPageNum(), pageReq.getPageSize());
    }

    /**
     * 把数据库分页结果转成统一的分页返回体
     *
     * @param page 数据库分页结果，可以为 null
     * @param <T>  列表元素类型
     * @return 分页返回体；入参为 null 时返回空结果
     */
    public static <T> PageRespVO<T> of(IPage<T> page) {
        if (page == null) {
            return PageRespVO.of(0L, PageReqVO.DEFAULT_PAGE_NUM, PageReqVO.DEFAULT_PAGE_SIZE, List.of());
        }
        return PageRespVO.of(page.getTotal(), (int) page.getCurrent(), (int) page.getSize(), page.getRecords());
    }

    /**
     * 把数据库分页结果转成统一的分页返回体，并替换当前页数据
     *
     * <p>用于实体需要转换成 VO 的场景：先查出实体分页结果，转换列表后再调用本方法，
     * 这样总数与分页参数仍取自数据库结果，不会因为转换丢字段。
     *
     * @param page    数据库分页结果，可以为 null
     * @param records 转换后的当前页数据，可以为 null
     * @param <S>     实体类型
     * @param <T>     目标类型
     * @return 分页返回体
     */
    public static <S, T> PageRespVO<T> of(IPage<S> page, List<T> records) {
        if (page == null) {
            return PageRespVO.of(0L, PageReqVO.DEFAULT_PAGE_NUM, PageReqVO.DEFAULT_PAGE_SIZE, records);
        }
        return PageRespVO.of(page.getTotal(), (int) page.getCurrent(), (int) page.getSize(), records);
    }
}
