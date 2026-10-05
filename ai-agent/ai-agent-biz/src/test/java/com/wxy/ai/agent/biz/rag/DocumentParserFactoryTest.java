package com.wxy.ai.agent.biz.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wxy.ai.agent.biz.constant.AiAgentErrorConstant;
import com.wxy.common.core.exception.BizException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

/**
 * 文档解析单元测试：只解析纯文本，不依赖网络与模型（PDF / docx 的真实解析放到联调验收）。
 *
 * @author wxy
 * @date 2026/10/05
 */
class DocumentParserFactoryTest {

    /** 被测解析入口 */
    private final DocumentParserFactory documentParserFactory = new DocumentParserFactory();

    /**
     * 不在白名单里的扩展名直接拒绝：让图片、压缩包进知识库只会污染召回
     */
    @Test
    @DisplayName("解析：不支持的扩展名报文件类型错误")
    void shouldRejectUnsupportedExtension() {
        assertThatThrownBy(() -> documentParserFactory.parse("户型图.png", "image/png", new byte[]{1, 2, 3}))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getCode())
                .isEqualTo(AiAgentErrorConstant.KNOWLEDGE_FILE_TYPE_UNSUPPORTED.code());
    }

    /**
     * 纯文本能被解析出正文
     */
    @Test
    @DisplayName("解析：txt 文档能解析出正文")
    void shouldParseTextDocument() {
        byte[] content = "押金为一个月租金，租金按月支付。".getBytes(StandardCharsets.UTF_8);

        List<Document> documents = documentParserFactory.parse("租赁规则.txt", "text/plain", content);

        assertThat(documents).isNotEmpty();
        assertThat(documents.get(0).getText()).contains("押金");
    }
}
