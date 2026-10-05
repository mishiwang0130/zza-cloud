package com.wxy.ai.agent.biz.controller.admin;

import com.wxy.ai.agent.biz.constant.AiAgentPermissionConstant;
import com.wxy.ai.agent.biz.service.KnowledgeService;
import com.wxy.ai.agent.biz.vo.admin.KnowledgeDocumentIdReqVO;
import com.wxy.ai.agent.biz.vo.admin.KnowledgeDocumentPageReqVO;
import com.wxy.ai.agent.biz.vo.admin.KnowledgeDocumentRespVO;
import com.wxy.ai.agent.biz.vo.admin.KnowledgeSearchItemRespVO;
import com.wxy.ai.agent.biz.vo.admin.KnowledgeSearchReqVO;
import com.wxy.common.core.result.Result;
import com.wxy.common.core.security.RequiresPermission;
import com.wxy.common.core.vo.PageRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理后台知识库接口：最终对外路径为 {@code /api/ai-agent/admin-api/knowledge/...}。
 *
 * <p>上传与重建都会调用模型做向量化，属于耗时操作；同一文档的写操作由 Service 用分布式锁串行。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Tag(name = "管理后台 - 知识库")
@RestController
@RequestMapping("/knowledge")
public class KnowledgeAdminController {

    /** 知识库服务 */
    @Resource
    private KnowledgeService knowledgeService;

    /**
     * 上传文档
     *
     * @param file 上传文件
     * @param city 城市标签，可为空（为空按「通用」处理）
     * @return 新文档 ID
     */
    @Operation(summary = "上传知识库文档",
            description = "支持 md / txt / pdf / docx / doc / html；接口只保存文件与元数据并投递索引任务，"
                    + "解析切片与向量化由消费者异步完成，返回的文档 ID 稍后查列表看状态")
    @RequiresPermission(AiAgentPermissionConstant.KNOWLEDGE_CREATE)
    @PostMapping(value = "/document/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Long> upload(
            @Parameter(description = "文档文件") @RequestPart("file") MultipartFile file,
            @Parameter(description = "城市标签，为空按通用处理") @RequestParam(required = false) String city) {
        return Result.success(knowledgeService.upload(file, city));
    }

    /**
     * 分页查询文档
     *
     * @param reqVO 分页入参
     * @return 文档分页
     */
    @Operation(summary = "分页查询知识库文档")
    @RequiresPermission(AiAgentPermissionConstant.KNOWLEDGE_QUERY)
    @PostMapping("/document/page")
    public Result<PageRespVO<KnowledgeDocumentRespVO>> page(
            @Validated @RequestBody KnowledgeDocumentPageReqVO reqVO) {
        return Result.success(knowledgeService.page(reqVO));
    }

    /**
     * 重建索引
     *
     * @param reqVO 文档入参
     * @return 空响应
     */
    @Operation(summary = "重建文档索引",
            description = "接口只投递重建任务：后台删掉该文档的旧向量后重新解析入库；解析规则调整后用它刷新存量文档")
    @RequiresPermission(AiAgentPermissionConstant.KNOWLEDGE_REBUILD)
    @PostMapping("/document/rebuild")
    public Result<Void> rebuild(@Validated @RequestBody KnowledgeDocumentIdReqVO reqVO) {
        knowledgeService.rebuild(reqVO.getId());
        return Result.success();
    }

    /**
     * 删除文档
     *
     * @param reqVO 文档入参
     * @return 空响应
     */
    @Operation(summary = "删除知识库文档", description = "清理向量与原始文件，文档行逻辑删除")
    @RequiresPermission(AiAgentPermissionConstant.KNOWLEDGE_DELETE)
    @PostMapping("/document/delete")
    public Result<Void> delete(@Validated @RequestBody KnowledgeDocumentIdReqVO reqVO) {
        knowledgeService.delete(reqVO.getId());
        return Result.success();
    }

    /**
     * 语义检索调试
     *
     * @param reqVO 检索入参
     * @return 命中片段
     */
    @Operation(summary = "语义检索调试",
            description = "返回 TopK 命中片段与相似度；带宽 city 时按「城市 + 通用」过滤，便于对比过滤前后差异")
    @RequiresPermission(AiAgentPermissionConstant.KNOWLEDGE_QUERY)
    @PostMapping("/search")
    public Result<List<KnowledgeSearchItemRespVO>> search(@Validated @RequestBody KnowledgeSearchReqVO reqVO) {
        return Result.success(knowledgeService.search(reqVO));
    }
}
