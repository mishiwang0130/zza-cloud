package com.wxy.gateway.filter;

import java.util.Set;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 内部端点拦截过滤器：不把业务服务的内部端点放给外部访问。
 *
 * <p>{@code /api/{服务名}/actuator/**}、{@code /api/{服务名}/v3/api-docs/**}、
 * {@code /api/{服务名}/doc.html} 这类路径按路由规则会被原样转发到业务服务，而业务服务的
 * 凭证校验白名单又放行了它们（这些路径本来是给内网运维与本地调试用的），结果是任何人都能
 * 拿到健康信息与接口文档。网关是唯一入口，这类路径在这里直接当作不存在处理。
 *
 * <p>按「服务名之后的第一段」匹配，而不是写死 {@code infra}：新增服务时不需要改本类。
 * 返回 404 而不是 403，避免对外暴露「这个服务存在、只是这条路径被拦了」。
 *
 * <p>用抛异常而不是直接写响应：统一响应体交给 common-webflux 的全局异常处理器渲染，
 * 与其它异常路径保持同一套 {@code Result} 结构。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Component
public class InternalEndpointBlockFilter implements GlobalFilter, Ordered {

    /** 网关前缀：只有 {@code /api} 开头的请求才由网关按服务名路由 */
    private static final String GATEWAY_PREFIX = "api";

    /** 路径分隔符 */
    private static final String PATH_SEPARATOR = "/";

    /** 服务名之后不允许对外暴露的第一段路径 */
    private static final Set<String> BLOCKED_SEGMENTS = Set.of(
            // 健康检查与指标
            "actuator",
            // springdoc 的接口文档数据，Knife4j 与 swagger-ui 都靠它渲染
            "v3",
            // 接口文档页面与其静态资源
            "doc.html",
            "swagger-ui",
            "swagger-ui.html",
            "webjars",
            "favicon.ico",
            // 服务间接口：只允许内网调用，对外的同名路径必须挡在网关（删掉的 TokenInternalController 用的就是这个前缀）
            "internal-api"
    );

    /**
     * 命中内部端点直接返回 404，其余请求继续走后面的过滤器
     *
     * @param exchange 当前请求上下文
     * @param chain    后续过滤器链
     * @return 拦截完成信号或链路执行完成信号
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (isInternalEndpoint(path)) {
            return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
        }
        return chain.filter(exchange);
    }

    /**
     * 顺序紧跟在请求头清洗之后、任何转发动作之前
     *
     * @return 只比最高优先级低两级
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    /**
     * 判断路径是否为受保护的内部端点
     *
     * <p>路径按「网关前缀 + 服务名 + 业务路径」的契约拆开，形如
     * {@code ["", "api", "{服务名}", "{业务路径第一段}", ...]}，只比对业务路径的第一段，
     * 因此不关心具体是哪个服务。
     *
     * @param path 请求路径
     * @return 属于内部端点返回 true
     */
    private static boolean isInternalEndpoint(String path) {
        String[] segments = path.split(PATH_SEPARATOR);
        if (segments.length < 4 || !GATEWAY_PREFIX.equals(segments[1])) {
            return false;
        }
        return BLOCKED_SEGMENTS.contains(segments[3]);
    }
}
