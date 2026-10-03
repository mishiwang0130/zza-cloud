package com.wxy.infra.biz.service.impl;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.security.TokenValidator;
import com.wxy.infra.biz.service.InfraTokenService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * infra 的令牌校验实现：把公共 SPI 接到本服务的凭证服务上。
 *
 * <p>为什么需要这个适配类：infra 是「持有用户与凭证数据」的服务，校验逻辑是
 * 验签 + 查凭证状态（Redis 优先、MySQL 兜底），这套逻辑已经有 {@link InfraTokenService} 了，
 * 这里只做接口对接，不重复实现。
 *
 * <p>端类型不在这里比对：公共拦截器按接口前缀统一完成，其他服务实现自己的 {@code TokenValidator} 时
 * 也不必各自维护一份端前缀规则。
 *
 * <p>其他服务没有用户表时，实现 {@code TokenValidator} 调 infra 的内部校验接口即可，
 * 见 {@code /internal-api/auth/check}。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Service
public class InfraTokenValidator implements TokenValidator {

    /** 凭证服务 */
    @Resource
    private InfraTokenService infraTokenService;

    /**
     * 校验令牌并返回登录用户
     *
     * @param token 裸令牌（已去掉 Bearer 前缀）
     * @return 登录用户
     */
    @Override
    public LoginUser validate(String token) {
        return infraTokenService.validate(token, null);
    }
}
