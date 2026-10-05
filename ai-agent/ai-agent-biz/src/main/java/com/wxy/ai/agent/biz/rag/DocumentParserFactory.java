package com.wxy.ai.agent.biz.rag;

import com.wxy.ai.agent.biz.constant.AiAgentErrorConstant;
import com.wxy.common.core.exception.BizException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文档解析入口：按文件类型选择解析器，产出带正文的 {@link Document} 列表。
 *
 * <p><b>解析器分工</b>：PDF 用 {@code PagePdfDocumentReader}（按页解析，保留页码 metadata，
 * 排查「哪一页答错了」时有用）；其余可解析文本的格式统一交给 {@code TikaDocumentReader}
 * （docx / doc / html / md / txt 都由 Tika 处理，不需要为每种格式写一个解析器）。
 *
 * <p><b>为什么固定白名单</b>：图片、压缩包、表格这类文件即使能解析出字节也没有可检索的语义，
 * 让它们进入知识库只会污染召回结果，所以不支持的扩展名直接拒绝，而不是「尽力解析」。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Slf4j
@Component
public class DocumentParserFactory {

    /** 支持解析的扩展名：与前端上传组件的 accept 保持一致 */
    private static final Set<String> SUPPORTED_EXTENSIONS =
            Set.of("pdf", "doc", "docx", "md", "markdown", "txt", "html", "htm");

    /**
     * 解析文档内容
     *
     * @param fileName    原始文件名，用于判断类型与记录来源
     * @param contentType 文件类型，可为空
     * @param content     文件字节内容（上传时已读入内存，上限由配置控制）
     * @return 解析出的文档列表；PDF 按页返回多条，其余通常一条
     */
    public List<Document> parse(String fileName, String contentType, byte[] content) {
        String extension = resolveExtension(fileName);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new BizException(AiAgentErrorConstant.KNOWLEDGE_FILE_TYPE_UNSUPPORTED,
                    "只支持 " + String.join(" / ", SUPPORTED_EXTENSIONS) + " 格式的文档");
        }
        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
        try {
            List<Document> documents = isPdf(extension, contentType)
                    ? new PagePdfDocumentReader(resource).get()
                    : new TikaDocumentReader(resource).get();
            if (documents == null || documents.isEmpty()) {
                throw new BizException(AiAgentErrorConstant.KNOWLEDGE_DOCUMENT_PARSE_FAILED, "文档内容为空");
            }
            log.debug("解析文档 {} 成功，产出段落数 {}", fileName, documents.size());
            return documents;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            // 解析失败原因（加密、损坏、编码异常）对使用者没有意义，记日志即可，返回统一提示
            log.warn("解析文档 {} 失败：{}", fileName, ex.getMessage());
            throw new BizException(AiAgentErrorConstant.KNOWLEDGE_DOCUMENT_PARSE_FAILED, null, ex);
        }
    }

    /**
     * 取扩展名（小写，不含点）
     *
     * @param fileName 文件名
     * @return 扩展名；没有扩展名时返回空串
     */
    private String resolveExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        return index < 0 ? "" : fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 判断是否走 PDF 解析器：扩展名或 Content-Type 任一命中即可
     *
     * @param extension   扩展名
     * @param contentType 文件类型
     * @return 是 PDF 返回 true
     */
    private boolean isPdf(String extension, String contentType) {
        return "pdf".equals(extension)
                || (StringUtils.hasText(contentType) && contentType.toLowerCase(Locale.ROOT).contains("pdf"));
    }
}
