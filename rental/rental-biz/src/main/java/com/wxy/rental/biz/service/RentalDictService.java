package com.wxy.rental.biz.service;

import com.wxy.rental.biz.vo.admin.DictItemVO;
import java.util.List;
import java.util.Map;

/**
 * 字典服务：负责 rental 与 infra 字典之间的编解码，并做 Redis 缓存。
 *
 * <p>rental 只存字典编码，中文名一律在返回前回填，所以「编码 → 中文名」必须有唯一入口，
 * 否则每个 Service 都自己调一次 infra，缓存也各有各的写法。
 *
 * @author wxy
 * @date 2026/10/04
 */
public interface RentalDictService {

    /**
     * 把逗号分隔的编码串转成带中文名的字典项列表（详情与列表回填用）
     *
     * <p>字典里查不到的编码（字典被停用或历史脏数据）用编码本身兜底展示并打 warn：
     * 一个标签查不到不应该让整页详情失败。
     *
     * @param dictType 字典类型编码
     * @param codesCsv 逗号分隔的编码串，可以为 null 或空
     * @return 字典项列表，无有效编码时返回空列表
     */
    List<DictItemVO> listDictItems(String dictType, String codesCsv);

    /**
     * 取单个编码的中文名（单值字段回填用，例如房间朝向）
     *
     * @param dictType 字典类型编码
     * @param code     字典编码，可以为 null
     * @return 中文名；编码为空时返回 null，字典里查不到时按编码兜底展示
     */
    String getLabel(String dictType, String code);

    /**
     * 取整个字典类型的「编码 → 中文名」映射（列表批量回填用）
     *
     * <p>列表场景要按行回填中文名，逐行走 {@link #getLabel} 会变成每行一次缓存访问；
     * 一次拿到映射后本地查表更合适。
     *
     * @param dictType 字典类型编码
     * @return 映射，按 infra 返回顺序
     */
    Map<String, String> getLabelMap(String dictType);

    /**
     * 校验编码是否都在字典里，并拼成逗号分隔的串（写库前用）
     *
     * @param dictType 字典类型编码
     * @param codes    编码列表，可以为 null（按空处理）
     * @return 逗号分隔的编码串，无有效编码时返回空串
     * @throws com.wxy.common.core.exception.BizException 存在字典里没有的编码时抛出
     */
    String joinCodes(String dictType, List<String> codes);
}
