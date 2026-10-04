package com.wxy.infra.biz.controller.internal;

import com.wxy.common.core.result.Result;
import com.wxy.infra.api.client.InfraFileClient;
import com.wxy.infra.api.dto.FileRespDTO;
import com.wxy.infra.biz.convert.InfraFileConvert;
import com.wxy.infra.biz.service.InfraFileService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

/**
 * infra 文件读取服务间接口的实现：实现 infra-api 发布的 {@link InfraFileClient}。
 *
 * <p>只做「按 ID 批量查 + 签发地址」，不提供上传（上传由调用方自己走
 * {@code /admin-api/file/upload}，需要管理端登录态）与删除。
 *
 * <p>路径与参数绑定都在 client 上，本类只写实现；服务间接口不对外，因此不进接口文档（{@link Hidden}）。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Hidden
@RestController
public class InfraFileClientImpl implements InfraFileClient {

    /** 文件服务 */
    @Resource
    private InfraFileService infraFileService;

    /** 文件转换器 */
    @Resource
    private InfraFileConvert infraFileConvert;

    /**
     * 按 ID 批量查询文件（含预签名访问地址）
     *
     * @param ids 文件 ID 列表
     * @return 文件列表
     */
    @Override
    public Result<List<FileRespDTO>> listByIds(List<Long> ids) {
        return Result.success(infraFileConvert.toDTOList(infraFileService.listByIds(ids)));
    }
}
