/**
 * Servlet 栈的 Web 公共能力：全局异常处理、端前缀自动配置（{@code /admin-api}、{@code /app-api}）、
 * 参数校验与接口文档配置。
 *
 * <p>包名约定：{@code common-webmvc} 对应 {@code com.wxy.common.webmvc}。
 * 本模块基于 spring-webmvc，只有业务服务可以引用，网关（WebFlux）禁止引用。
 */
package com.wxy.common.webmvc;
