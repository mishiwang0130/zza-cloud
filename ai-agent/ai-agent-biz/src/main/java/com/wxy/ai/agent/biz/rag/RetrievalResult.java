package com.wxy.ai.agent.biz.rag;

import java.util.List;
import org.springframework.util.StringUtils;

/**
 * 一次知识库检索的结果：命中片段 + 拼好的参考资料文本 + 是否发生了降级。
 *
 * <p>{@code degraded=true} 表示本轮是「检索失败 / 没有向量库」的降级结果，回答仍会继续，
 * 只是退化成纯模型回答；排查线上问题时需要能区分这种情况与「知识库里确实没有」。
 *
 * @param chunks      命中的片段，按相似度倒序
 * @param contextText 拼接好的参考资料文本，直接塞进提示词
 * @param degraded    是否降级（检索不可用）
 * @author wxy
 * @date 2026/10/05
 */
public record RetrievalResult(List<RetrievedChunk> chunks, String contextText, boolean degraded) {

    /**
     * 空结果
     *
     * @param degraded 是否降级
     * @return 空的检索结果
     */
    public static RetrievalResult empty(boolean degraded) {
        return new RetrievalResult(List.of(), "", degraded);
    }

    /**
     * 是否有可用的参考资料
     *
     * @return 有参考资料返回 true
     */
    public boolean hasContext() {
        return StringUtils.hasText(contextText);
    }
}
