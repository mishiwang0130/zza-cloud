package com.wxy.infra.api.client;

import com.wxy.common.core.result.Result;
import com.wxy.infra.api.constant.InfraApiConstant;
import com.wxy.infra.api.dto.TokenCheckReqDTO;
import com.wxy.infra.api.dto.TokenCheckRespDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * infra 对外发布的凭证服务接口：其他服务没有自己的用户表时，调它校验访问令牌。
 *
 * <p>调用方接入方式：依赖 infra-api → 启动类加
 * {@code @EnableFeignClients(basePackages = "com.wxy.infra.api.client")} → 需要时写自己的
 * {@code TokenValidator}（否则用 common 提供的默认实现，见 {@code DefaultTokenValidator}）。
 *
 * <p>返回体是统一响应 {@link Result}：调用方用 {@code Result#requireData()} 取数据，
 * 失败会直接抛异常，不用自己判断 code。
 *
 * @author wxy
 * @date 2026/10/03
 */
@FeignClient(name = InfraApiConstant.SERVICE_NAME, contextId = "infraTokenClient")
public interface InfraTokenClient {

    /**
     * 校验访问令牌
     *
     * @param reqDTO 校验入参
     * @return 身份三要素；令牌无效时返回未登录错误码
     */
    @PostMapping(InfraApiConstant.TOKEN_API_PREFIX + InfraApiConstant.TOKEN_CHECK_PATH)
    Result<TokenCheckRespDTO> checkToken(@RequestBody TokenCheckReqDTO reqDTO);
}
