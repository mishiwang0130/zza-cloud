package com.wxy.infra.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * infra 服务启动类：提供管理后台的登录、用户、角色、菜单权限与文件上传能力。
 *
 * <p>注册到 Nacos 后服务名为 {@code infra}，网关按 {@code lb://infra} 转发；
 * Mapper 扫描范围限定在本服务自己的 {@code mapper} 包，公共模块的自动配置由
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} 生效，
 * 不需要把 {@code com.wxy.common} 加进扫描范围。
 *
 * @author wxy
 * @date 2026/10/03
 */
@EnableDiscoveryClient
@SpringBootApplication
@MapperScan("com.wxy.infra.biz.mapper")
public class InfraApplication {

    /**
     * 服务入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(InfraApplication.class, args);
    }
}
