package com.wxy.infra.biz.service;

import com.wxy.common.core.vo.PageRespVO;
import com.wxy.infra.biz.vo.admin.DictTypeCreateReqVO;
import com.wxy.infra.biz.vo.admin.DictTypePageReqVO;
import com.wxy.infra.biz.vo.admin.DictTypeRespVO;
import com.wxy.infra.biz.vo.admin.DictTypeSimpleRespVO;
import com.wxy.infra.biz.vo.admin.DictTypeUpdateReqVO;
import java.util.List;

/**
 * 管理后台字典类型服务。
 *
 * @author wxy
 * @date 2026/10/04
 */
public interface InfraDictTypeService {

    /**
     * 新增字典类型
     *
     * @param reqVO 新增入参
     * @return 新字典类型 ID
     */
    Long createDictType(DictTypeCreateReqVO reqVO);

    /**
     * 修改字典类型；改编码时同步刷新该类型下的字典数据
     *
     * @param reqVO 修改入参
     */
    void updateDictType(DictTypeUpdateReqVO reqVO);

    /**
     * 删除字典类型（逻辑删除），类型下还有字典数据时不允许删除
     *
     * @param id 字典类型 ID
     */
    void deleteDictType(Long id);

    /**
     * 查询字典类型详情
     *
     * @param id 字典类型 ID
     * @return 字典类型详情
     */
    DictTypeRespVO getDictType(Long id);

    /**
     * 分页查询字典类型
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    PageRespVO<DictTypeRespVO> pageDictType(DictTypePageReqVO reqVO);

    /**
     * 查询启用状态的字典类型列表（新增/修改字典数据时的下拉用）
     *
     * @return 字典类型精简列表
     */
    List<DictTypeSimpleRespVO> listDictType();
}
