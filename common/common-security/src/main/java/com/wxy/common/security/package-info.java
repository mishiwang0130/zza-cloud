/**
 * 认证公共能力：JWT 签发与解析（{@code JwtUtil}）、密钥配置（{@code JwtProperties}）。
 *
 * <p>包名约定：{@code common-security} 对应 {@code com.wxy.common.security}。
 * 登录上下文不在本模块，放在 {@code com.wxy.common.core.context.UserContextHolder}，
 * 因为它同时被 Web 拦截器、MyBatis 审计填充与 Feign 透传使用。
 */
package com.wxy.common.security;
