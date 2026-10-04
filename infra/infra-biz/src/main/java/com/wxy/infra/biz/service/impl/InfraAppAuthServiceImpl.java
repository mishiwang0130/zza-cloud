package com.wxy.infra.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wxy.common.core.enums.CommonStatusEnum;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.util.DesensitizeUtil;
import com.wxy.infra.biz.constant.InfraErrorConstant;
import com.wxy.infra.biz.mapper.InfraAppUserMapper;
import com.wxy.infra.biz.po.InfraAppUser;
import com.wxy.infra.biz.service.InfraAppAuthService;
import com.wxy.infra.biz.service.InfraSmsCodeService;
import com.wxy.infra.biz.service.InfraTokenService;
import com.wxy.infra.biz.vo.AuthTokenRespVO;
import com.wxy.infra.biz.vo.app.AuthAppLoginReqVO;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户端认证服务实现：手机号 + 短信验证码登录，首次登录即注册。
 *
 * <p>为什么先校验验证码：验证码没通过之前不该暴露「这个手机号有没有注册过」，
 * 也不该为一次无效请求去查库、写库。
 *
 * <p>为什么注册与登录合成一个入口：app 用户没有角色、也没有后台那种账号开通流程，
 * 验证码已经证明了手机号可用，再让用户单独填一遍注册表单没有额外价值。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Service
public class InfraAppAuthServiceImpl implements InfraAppAuthService {

    /** 短信验证码服务 */
    @Resource
    private InfraSmsCodeService infraSmsCodeService;

    /** 用户端用户 Mapper */
    @Resource
    private InfraAppUserMapper infraAppUserMapper;

    /** 凭证服务：admin 端与 app 端共用，这里传 UserTypeEnum.APP */
    @Resource
    private InfraTokenService infraTokenService;

    /**
     * 用户端登录
     *
     * @param reqVO   登录入参
     * @param loginIp 登录 IP，可为空
     * @return 凭证返回体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthTokenRespVO login(AuthAppLoginReqVO reqVO, String loginIp) {
        String mobile = reqVO.getMobile();
        // 校验通过后验证码立即失效（InfraSmsCodeService 里已删缓存），同一个码不能被重复提交
        infraSmsCodeService.verifyCode(mobile, reqVO.getCode());
        InfraAppUser user = getByMobile(mobile);
        if (user == null) {
            user = register(mobile);
        } else if (!CommonStatusEnum.ENABLED.getValue().equals(user.getStatus())) {
            throw new BizException(InfraErrorConstant.USER_DISABLED);
        }
        // 端类型传 APP：凭证落库时 user_type = 2，app 凭证打到 admin 接口会被拦截器按端前缀挡掉
        return infraTokenService.createTokenPair(user.getId(), UserTypeEnum.APP, user.getMobile(), loginIp);
    }

    /**
     * 按手机号查用户
     *
     * <p>手机号上有唯一索引 {@code uk_infra_app_user_mobile}，不会查出多条；
     * 逻辑删除条件由 MyBatis-Plus 按 {@code BasePO} 的 {@code @TableLogic} 自动带上，
     * 已删除的账号查不到，会走注册分支。
     *
     * @param mobile 手机号
     * @return 用户实体，不存在时返回 null
     */
    private InfraAppUser getByMobile(String mobile) {
        return infraAppUserMapper.selectOne(new LambdaQueryWrapper<InfraAppUser>()
                .eq(InfraAppUser::getMobile, mobile));
    }

    /**
     * 首次登录即注册
     *
     * <p>并发首登时两个请求可能同时走到这里，靠手机号唯一索引兜底：谁先插谁赢，
     * 后到的那个捕获唯一键冲突后查回已有记录，照常签发凭证，不把冲突抛给用户。
     *
     * @param mobile 手机号
     * @return 用户实体
     */
    private InfraAppUser register(String mobile) {
        InfraAppUser po = new InfraAppUser();
        po.setMobile(mobile);
        // 昵称先用手机号脱敏值（138****0000）兜底，用户可在个人中心改
        po.setNickname(DesensitizeUtil.mobile(mobile));
        po.setStatus(CommonStatusEnum.ENABLED.getValue());
        // avatar_file_id、审计字段都不用手动设：头像走库默认值 0，审计字段由 AuditMetaObjectHandler 填
        try {
            infraAppUserMapper.insert(po);
            return po;
        } catch (DuplicateKeyException ex) {
            InfraAppUser exist = getByMobile(mobile);
            if (exist == null) {
                throw ex;
            }
            return exist;
        }
    }
}