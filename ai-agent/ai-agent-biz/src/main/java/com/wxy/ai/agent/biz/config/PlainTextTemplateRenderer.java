package com.wxy.ai.agent.biz.config;

import java.util.Map;
import org.springframework.ai.template.TemplateRenderer;

/**
 * 原样输出模板的渲染器：不做任何变量替换。
 *
 * <p><b>为什么需要它</b>：Spring AI 默认用 StringTemplate 渲染提示词，会把 {@code {...}} 当变量。
 * 本服务的用户消息里会拼「用户问题 + 知识库原文」，原文出现花括号（代码片段、表格、金额区间）
 * 就会被误当成变量而报错或替换成空。这里改成原样返回：提示词全部由我们自己拼好，
 * 不需要模板引擎再加工一次。
 *
 * @author wxy
 * @date 2026/10/05
 */
public class PlainTextTemplateRenderer implements TemplateRenderer {

    /**
     * 原样返回模板
     *
     * @param template  模板文本
     * @param variables 变量（本实现忽略）
     * @return 模板原文
     */
    @Override
    public String apply(String template, Map<String, Object> variables) {
        return template;
    }
}
