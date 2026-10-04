package com.wxy.rental.biz.util;

import com.wxy.common.core.constant.CommonConstant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * 编码串工具：标签、配套这类「多选编码存成一列逗号分隔」的字段靠它做编解码。
 *
 * <p>统一在这里去重与去空，是因为这两种脏数据都会造成难排查的问题：
 * 重复编码会让详情页出现两个一样的标签，空编码会让前端拿到一个点不动的空标签。
 * 编码本身合法性（是否存在于字典）不在这里判断，那是 {@code RentalDictService} 的职责。
 *
 * @author wxy
 * @date 2026/10/04
 */
public final class RentalCodeUtil {

    /**
     * 工具类，禁止实例化
     */
    private RentalCodeUtil() {
    }

    /**
     * 把逗号分隔的编码串拆成列表
     *
     * @param codesCsv 逗号分隔的编码串，可以为 null 或空
     * @return 去空、去重后的编码列表，入参为空时返回空列表
     */
    public static List<String> split(String codesCsv) {
        if (!StringUtils.hasText(codesCsv)) {
            return List.of();
        }
        return List.of(codesCsv.split(CommonConstant.COMMA)).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    /**
     * 把编码列表拼成逗号分隔的串
     *
     * @param codes 编码列表，可以为 null
     * @return 逗号分隔的编码串，无有效编码时返回空串
     */
    public static String join(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return "";
        }
        return codes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(CommonConstant.COMMA));
    }
}
