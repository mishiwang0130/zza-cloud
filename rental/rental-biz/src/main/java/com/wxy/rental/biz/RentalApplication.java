package com.wxy.rental.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * rental 服务启动类：提供公寓、房间、费用项、租约与看房预约的管理端接口。
 *
 * <p>注册到 Nacos 后服务名为 {@code rental}（见 {@code RentalApiConstant.SERVICE_NAME}），
 * 网关按 {@code lb://rental} 转发 {@code /api/rental/**}。
 *
 * <p><b>为什么要开 Feign 客户端扫描</b>：rental 本身没有用户与权限数据，鉴权走 common-security 的
 * 默认实现（{@code DefaultTokenValidator} / {@code DefaultPermissionChecker}），它们依赖
 * {@code com.wxy.infra.api.client} 下的 Feign 客户端回源 infra；字典、区划、文件的服务间接口也在同一个包。
 * 组件扫描同时覆盖 {@code com.wxy.infra.api}，因为这些客户端的降级工厂是 {@code @Component}
 * （扫描不到时 Feign 会因为找不到降级工厂直接启动失败）。
 *
 * <p>Mapper 扫描范围限定在本服务自己的 {@code mapper} 包；public 模块的自动配置由
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} 生效，
 * 不需要把 {@code com.wxy.common} 加进扫描范围。
 *
 * @author wxy
 * @date 2026/10/04
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.wxy.infra.api.client")
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.wxy.rental.biz", "com.wxy.infra.api"})
@MapperScan("com.wxy.rental.biz.mapper")
public class RentalApplication {

    /**
     * 服务入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(RentalApplication.class, args);
    }
}
