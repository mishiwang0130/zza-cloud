/**
 * 响应式（WebFlux）公共能力：网关统一异常处理，把异常渲染成与业务服务一致的 {@code Result}。
 *
 * <p>包名约定：{@code common-webflux} 对应 {@code com.wxy.common.webflux}。
 * 只有网关这类响应式应用引用本模块，业务服务引用的是 {@code common-webmvc}。
 */
package com.wxy.common.webflux;
