package com.wxy.rental.api.constant;

/**
 * rental 对外发布的常量：目前只有 Nacos 服务名。
 *
 * <p>服务名写在 api 模块里，是为了让它只定义一次：{@code rental-biz} 的
 * {@code spring.application.name}（yml 里只能写字符串）与将来的各个
 * {@code @FeignClient(name = ...)} 都必须是同一个值；网关按 {@code lb://rental} 路由，
 * 也依赖这个值。改这里的值时，记得同步 {@code rental-biz} 的 {@code spring.application.name}。
 *
 * <p>接口路径刻意不放常量：路径直接写在 {@code @RequestMapping} 上更直观，
 * 实现类会继承契约里的映射，不存在「两处路径要同步」的问题。
 *
 * @author wxy
 * @date 2026/10/04
 */
public final class RentalApiConstant {

    /**
     * Nacos 中注册的服务名：不带 {@code -biz} 后缀，与 rental-biz 的
     * {@code spring.application.name} 保持一致，网关按 {@code lb://rental} 路由。
     */
    public static final String SERVICE_NAME = "rental";

    /**
     * 工具类常量类，禁止实例化
     */
    private RentalApiConstant() {
    }
}
