package com.wxy.gateway.filter;

import com.wxy.common.core.constant.HeaderConstant;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 登录用户请求头清洗过滤器：请求一进网关就删掉外部传入的 {@code X-User-Id}、
 * {@code X-User-Type}、{@code X-User-Name}。
 *
 * <p>为什么是无条件删除而不是校验：这三个头按约定「只能由网关写入、业务服务无条件信任」
 * （见 {@code HeaderConstant}），业务服务的登录上下文拦截器对所有路径生效、谁来都读。
 * 网关若不处理，客户端只要自己带上 {@code X-User-Id} 就能伪造身份——尤其是登录、续期
 * 这类被业务服务排除在凭证校验之外的路径，伪造出来的身份会直接进上下文并写进审计字段。
 *
 * <p>因此这里不做任何判断，先删干净：网关自身不依赖这三个头，将来若在网关上做凭证校验，
 * 由校验逻辑解析出真实身份后重新写入，客户端永远无法自带身份。
 *
 * <p>服务之间的 Feign 调用不经过网关，透传登录身份的能力不受影响。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Component
public class UserHeaderSanitizeFilter implements GlobalFilter, Ordered {

    /** 需要清洗的请求头：登录用户上下文，只能由网关写入 */
    private static final String[] FORGED_HEADERS = {
            HeaderConstant.USER_ID,
            HeaderConstant.USER_TYPE,
            HeaderConstant.USER_NAME
    };

    /**
     * 删除外部传入的登录用户头后继续执行过滤器链
     *
     * <p>请求头的读写是大小写不敏感的，这里不需要额外做大小写兼容。
     *
     * @param exchange 当前请求上下文
     * @param chain    后续过滤器链
     * @return 链路执行完成信号
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    for (String forgedHeader : FORGED_HEADERS) {
                        headers.remove(forgedHeader);
                    }
                })
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    /**
     * 顺序最靠前：必须早于任何鉴权与转发动作，否则清洗没有意义
     *
     * @return 最高优先级
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
