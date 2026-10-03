package com.wxy.common.core.security;

import com.wxy.common.core.context.LoginUser;

/**
 * 令牌校验接口（SPI）：把「一个令牌换成一个登录用户」这件事抽象出来，各服务提供自己的实现。
 *
 * <p>为什么放 common-core 而不是 common-security：公共的凭证拦截器在 common-webmvc 里、
 * 它需要看到本接口，而 common-* 之间不允许互相依赖，所以接口只能落在所有模块都依赖的 common-core。
 *
 * <p><b>各服务实现什么</b>：
 * <ul>
 *   <li>自己持有用户与凭证的服务（例如 infra）：实现「验签 + 查凭证状态（Redis 优先、MySQL 兜底）」；</li>
 *   <li>没有用户表的服务：实现「调 infra 的内部校验接口」，或先用只验签的轻量实现；</li>
 *   <li>不提供实现的服务：公共凭证拦截器不会装配，服务也就没有登录校验——所以需要鉴权的服务必须给出实现。</li>
 * </ul>
 *
 * <p>接口只负责「令牌有效性 + 身份」，端类型（管理后台 / 用户端）比对由公共拦截器按接口前缀完成，
 * 这样各服务不用各自维护一份端前缀规则。
 *
 * @author wxy
 * @date 2026/10/03
 */
public interface TokenValidator {

    /**
     * 校验令牌并返回登录用户
     *
     * @param token 裸令牌（已去掉 {@code Bearer } 前缀）
     * @return 登录用户，不能返回 null
     * @throws com.wxy.common.core.exception.UnauthorizedException 令牌缺失、格式错误、已失效或已过期
     */
    LoginUser validate(String token);
}
