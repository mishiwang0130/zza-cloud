package com.wxy.ai.agent.biz.tool;

import com.wxy.ai.agent.biz.config.AiAgentProperties;
import com.wxy.ai.agent.biz.service.RagService;
import com.wxy.ai.agent.biz.vo.admin.KnowledgeSearchItemRespVO;
import jakarta.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 知识库检索工具（本地实现，不需要远程调用）。
 *
 * <p>与「每轮自动检索」的分工：主链路会把召回片段直接拼进提示词，本工具用于模型自己想再查一次
 * （例如用户追问细节、或第一次召回不够）。它也是验证「工具调用链路真的通」的抓手——
 * 其他工具都要等跨服务接口接好才能用。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Slf4j
@Component
public class KnowledgeTools {

    /** 单次最多返回多少片，避免一次把上下文塞满 */
    private static final int MAX_TOP_K = 10;

    /** 检索能力 */
    @Resource
    private RagService ragService;

    /** 业务配置：默认召回条数取配置值 */
    @Resource
    private AiAgentProperties properties;

    /**
     * 检索知识库
     *
     * @param query 检索问题
     * @param topK  召回条数，可空
     * @param city  城市标签，可空
     * @return 给模型看的检索结果文本
     */
    @Tool(name = "searchKnowledgeBase",
            description = "查询平台知识库（租赁规则、费用说明、入住退租流程等）。"
                    + "当用户问规则、流程、费用口径，或你不确定答案依据时调用。")
    public String searchKnowledgeBase(
            @ToolParam(description = "要检索的问题，用用户的原话或改写后的关键词") String query,
            @ToolParam(required = false, description = "最多返回几条，默认 5，最大 10") Integer topK,
            @ToolParam(required = false, description = "城市标签，如 武汉；用户没提城市就留空") String city) {
        if (!StringUtils.hasText(query)) {
            return "请先说明要查询的问题内容。";
        }
        int size = topK == null ? properties.getRag().getTopK() : Math.max(1, Math.min(topK, MAX_TOP_K));
        List<KnowledgeSearchItemRespVO> items = ragService.search(query.trim(), size,
                StringUtils.hasText(city) ? city.trim() : null);
        if (items.isEmpty()) {
            return "知识库里没有检索到相关内容。可以请用户换个说法，或建议咨询公寓管家。";
        }
        StringBuilder result = new StringBuilder("知识库检索结果：\n");
        int index = 1;
        for (KnowledgeSearchItemRespVO item : items) {
            result.append("[%d] 来源文件：%s\n%s\n\n".formatted(index++, item.getFileName(), item.getText()));
        }
        return result.toString().trim();
    }
}
