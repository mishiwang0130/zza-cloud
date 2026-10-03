package com.wxy.infra.api.constant;

/**
 * infra 对外发布的常量：目前只有 Nacos 服务名。
 *
 * <p>服务名写在 api 模块里、由 Feign 客户端引用，是为了让它只定义一次：
 * infra-biz 的 {@code spring.application.name}（yml 里只能写字符串）、各个
 * {@code @FeignClient(name = ...)} 都必须是同一个值，多写一处就多一个改漏的机会。
 * 改这里的值时，记得同步 infra-biz 的 {@code spring.application.name}。
 *
 * <p>接口路径刻意不放常量：路径直接写在 {@code @PostMapping} 上更直观，
 * 而且实现类会继承这份映射，不存在「两处路径要同步」的问题。
 *
 * @author wxy
 * @date 2026/10/03
 */
public final class InfraApiConstant {

    /**
     * Nacos 中注册的服务名：不带 {@code -biz} 后缀，与 infra-biz 的
     * {@code spring.application.name} 保持一致，网关按 {@code lb://infra} 路由。
     */
    public static final String SERVICE_NAME = "infra";

    /**
     * 工具类常量类，禁止实例化
     */
    private InfraApiConstant() {
    }
}
