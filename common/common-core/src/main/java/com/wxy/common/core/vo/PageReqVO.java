package com.wxy.common.core.vo;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 分页查询入参：所有分页接口统一使用它，各服务不得自己再造一套分页参数。
 *
 * <p>页码与条数在 setter 中做边界收敛（页码至少为 1、条数落在 [1, {@value #MAX_PAGE_SIZE}]），
 * 这样调用方传 0、负数或超大条数都不会打到数据库；需要严格报错的场景，
 * 由接口层再补 {@code @Validated} 校验。
 *
 * @author wxy
 * @date 2026/10/02
 */
@Data
public class PageReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 默认页码：从 1 开始 */
    public static final int DEFAULT_PAGE_NUM = 1;

    /** 默认每页条数 */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 每页条数上限：防止一次拉取过多数据拖垮数据库 */
    public static final int MAX_PAGE_SIZE = 500;

    /** 页码，从 1 开始，默认 1 */
    private Integer pageNum = DEFAULT_PAGE_NUM;

    /** 每页条数，默认 20，上限 {@value #MAX_PAGE_SIZE} */
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    /**
     * 设置页码：小于 1 或为 null 时收敛为默认值
     *
     * @param pageNum 页码
     */
    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum == null || pageNum < DEFAULT_PAGE_NUM ? DEFAULT_PAGE_NUM : pageNum;
    }

    /**
     * 设置每页条数：为 null、小于 1 或超过上限时收敛到合法区间
     *
     * @param pageSize 每页条数
     */
    public void setPageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            this.pageSize = DEFAULT_PAGE_SIZE;
        } else {
            this.pageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        }
    }

    /**
     * 获取页码：反序列化等极端情况下字段可能为 null，这里再兜底一次
     *
     * @return 合法页码，最小为 1
     */
    public Integer getPageNum() {
        return pageNum == null || pageNum < DEFAULT_PAGE_NUM ? DEFAULT_PAGE_NUM : pageNum;
    }

    /**
     * 获取每页条数：兜底到 [1, {@value #MAX_PAGE_SIZE}] 区间
     *
     * @return 合法条数
     */
    public Integer getPageSize() {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
