package com.wxy.ai.agent.biz.rag;

import java.util.List;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.util.StringUtils;

/**
 * 城市标签过滤条件构造：RAG 检索链路的元数据过滤。
 *
 * <p><b>规则</b>：没传城市返回 {@code null}（完全不过滤）；城市就是「通用」时只按通用过滤；
 * 其它城市按 {@code city in (选中城市, 通用)} 过滤——平台级通用条款在任何城市下都要能召回。
 *
 * <p><b>为什么用表达式而不是取回后再筛</b>：表达式下推到向量库，TopK 与相似度阈值都在过滤后生效；
 * 先取 TopK 再由应用层筛掉不匹配的城市，会让结果数量变少甚至为空。
 *
 * @author wxy
 * @date 2026/10/05
 */
public final class CityFilter {

    /**
     * 构造城市过滤表达式
     *
     * @param city            选中的城市，可为空
     * @param cityMetadataKey 城市在向量库 metadata 中的字段名
     * @param commonCity      平台级通用文档的城市标签值
     * @return 过滤表达式；不需要过滤时返回 null
     */
    public static Filter.Expression build(String city, String cityMetadataKey, String commonCity) {
        if (!StringUtils.hasText(city)) {
            return null;
        }
        String selected = city.trim();
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        if (selected.equals(commonCity)) {
            return builder.eq(cityMetadataKey, selected).build();
        }
        return builder.in(cityMetadataKey, List.of(selected, commonCity)).build();
    }

    /**
     * 工具类，禁止实例化
     */
    private CityFilter() {
    }
}
