package com.wxy.infra.biz.service;

import com.wxy.common.core.vo.PageRespVO;
import com.wxy.infra.biz.vo.admin.DictDataCreateReqVO;
import com.wxy.infra.biz.vo.admin.DictDataPageReqVO;
import com.wxy.infra.biz.vo.admin.DictDataRespVO;
import com.wxy.infra.biz.vo.admin.DictDataSimpleRespVO;
import com.wxy.infra.biz.vo.admin.DictDataUpdateReqVO;
import com.wxy.infra.biz.vo.app.DictDataAppRespVO;
import java.util.List;

/**
 * 管理后台字典数据服务。
 *
 * @author wxy
 * @date 2026/10/04
 */
public interface InfraDictDataService {

    /**
     * 新增字典数据
     *
     * @param reqVO 新增入参
     * @return 新字典数据 ID
     */
    Long createDictData(DictDataCreateReqVO reqVO);

    /**
     * 修改字典数据
     *
     * @param reqVO 修改入参
     */
    void updateDictData(DictDataUpdateReqVO reqVO);

    /**
     * 删除字典数据（逻辑删除）
     *
     * @param id 字典数据 ID
     */
    void deleteDictData(Long id);

    /**
     * 查询字典数据详情
     *
     * @param id 字典数据 ID
     * @return 字典数据详情
     */
    DictDataRespVO getDictData(Long id);

    /**
     * 分页查询字典数据
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    PageRespVO<DictDataRespVO> pageDictData(DictDataPageReqVO reqVO);

    /**
     * 按类型编码查询启用的字典数据（前端下拉/标签展示用），按排序号升序
     *
     * @param dictType 字典类型编码
     * @return 字典数据精简列表
     */
    List<DictDataSimpleRespVO> listDictDataByType(String dictType);

    /**
     * 用户端按类型编码查询启用的字典数据（只返回标签与值），按排序号升序
     *
     * @param dictType 字典类型编码
     * @return 用户端字典数据列表
     */
    List<DictDataAppRespVO> listAppDictDataByType(String dictType);
}
