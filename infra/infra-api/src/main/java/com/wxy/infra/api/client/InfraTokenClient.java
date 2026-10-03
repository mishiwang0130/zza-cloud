package com.wxy.infra.api.client;

import com.wxy.common.core.result.Result;
import com.wxy.infra.api.client.fallback.InfraTokenClientFallbackFactory;
import com.wxy.infra.api.dto.TokenCheckReqDTO;
import com.wxy.infra.api.dto.TokenCheckRespDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * infra 对外发布的凭证服务接口：其他服务没有自己的用户表时，调它校验访问令牌。
 *
 * <p>调用方接入方式：依赖 infra-api → 启动类加
 * {@code @EnableFeignClients(basePackages = "com.wxy.infra.api.client")}，
 * 并让组件扫描覆盖 {@code com.wxy.infra.api}（降级工厂是 {@code @Component}，
 * 扫描不到时 Feign 会因为找不到降级工厂直接启动失败）→
 * 不写任何代码就能用默认鉴权（见 {@code DefaultTokenValidator}），
 * 需要自己的校验逻辑时定义同类型的 Bean 覆盖即可。
 *
 * <p><b>熔断降级</b>：{@link #fallbackFactory} 指定了降级工厂，远程调用失败或被 Sentinel 熔断时返回
 * 「服务调用失败」的失败响应（绝不返回「有身份」，鉴权不能因为依赖不可用而放行）。
 * 生效需要两件事：classpath 上有 Sentinel（引 infra-api / common-security 即带入），以及配置
 * {@code feign.sentinel.enabled: true}——少了这个开关，Spring Cloud 不会用 Sentinel 包装 Feign，
 * 降级工厂会被静默忽略，调用异常会直接冒到上层。
 *
 * <p>返回体是统一响应 {@link Result}：调用方用 {@code Result#requireData()} 取数据，失败会直接抛异常。
 *
 * <p>路径用 {@code /internal-api} 前缀：调用方经 Nacos 服务发现直连 infra，不经过网关；
 * 网关的 InternalEndpointBlockFilter 就是按这个前缀挡掉外部的同名访问。
 *
 * @author wxy
 * @date 2026/10/03
 */
@FeignClient(name = "infra", contextId = "infraTokenClient",
        fallbackFactory = InfraTokenClientFallbackFactory.class)
public interface InfraTokenClient {

    /**
     * 校验访问令牌
     *
     * @param reqDTO 校验入参
     * @return 身份三要素；令牌无效时返回未登录错误码
     */
    @PostMapping("/internal-api/auth/check")
    Result<TokenCheckRespDTO> checkToken(@Validated @RequestBody TokenCheckReqDTO reqDTO);
}
