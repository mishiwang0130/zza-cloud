package com.wxy.infra.biz.service;

import com.wxy.infra.biz.vo.admin.AreaRespVO;
import com.wxy.infra.biz.vo.app.AreaAppRespVO;
import java.util.List;

/**
 * 行政区划服务：只提供查询（省市区是标准数据，靠脚本 {@code sql/infra_area.sql} 维护，不开放增删改）。
 *
 * @author wxy
 * @date 2026/10/04
 */
public interface InfraAreaService {

    /**
     * 查询某一级下的子级区划（前端三级联动逐级加载用）
     *
     * @param parentId 上级区划 ID；为 null 或 0 时返回全部省级
     * @return 子级区划列表，按行政区划代码升序
     */
    List<AreaRespVO> listChildren(Long parentId);

    /**
     * 查询完整的省市区树（一次拿全，前端可本地缓存后做联动）
     *
     * @return 省级为根的树
     */
    List<AreaRespVO> listTree();

    /**
     * 用户端查询某一级下的子级区划（匿名浏览房源时做城市 / 区县筛选）
     *
     * @param parentId 上级区划 ID；为 null 或 0 时返回全部省级
     * @return 子级区划列表（children 为空列表），按行政区划代码升序
     */
    List<AreaAppRespVO> listAppChildren(Long parentId);

    /**
     * 用户端查询完整的省市区树
     *
     * @return 省级为根的三级树
     */
    List<AreaAppRespVO> listAppTree();

    /**
     * 按城市名称列出
     *
     * @param cityName 城市名称
     * @return {@code List<AreaRespVO> }
     * @author wxy
     * @date 2026/10/05
     */
    List<AreaRespVO> listByCityName(String cityName);
}
