package com.wxy.common.core.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页查询结果：与 {@link PageReqVO} 配套，分页接口统一返回 {@code Result<PageRespVO<XxxRespVO>>}。
 *
 * <p>由 Service 层把 MyBatis-Plus 的 {@code IPage} 转换而来，
 * {@code common-mybatis} 的 {@code PageUtil} 提供了现成转换方法，不要在业务里重复写。
 *
 * @param <T> 列表元素类型
 * @author wxy
 * @date 2026/10/02
 */
@Data
@NoArgsConstructor
public class PageRespVO<T> implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 总记录数 */
    private Long total;

    /** 当前页码，从 1 开始 */
    private Integer pageNum;

    /** 每页条数 */
    private Integer pageSize;

    /** 当前页数据 */
    private List<T> records;

    /**
     * 构造分页结果
     *
     * @param total    总记录数
     * @param pageNum  当前页码
     * @param pageSize 每页条数
     * @param records  当前页数据，可以为 null（会转成空列表）
     * @param <T>      列表元素类型
     * @return 分页结果
     */
    public static <T> PageRespVO<T> of(long total, int pageNum, int pageSize, List<T> records) {
        PageRespVO<T> resp = new PageRespVO<>();
        resp.setTotal(total);
        resp.setPageNum(pageNum);
        resp.setPageSize(pageSize);
        resp.setRecords(records == null ? List.of() : records);
        return resp;
    }
}
