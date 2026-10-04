package com.wxy.infra.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.CommonStatusEnum;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.infra.biz.constant.InfraErrorConstant;
import com.wxy.infra.biz.mapper.InfraUserMapper;
import com.wxy.infra.biz.po.InfraUser;
import com.wxy.infra.biz.service.InfraAdminAuthService;
import com.wxy.infra.biz.service.InfraMenuService;
import com.wxy.infra.biz.service.InfraPermissionService;
import com.wxy.infra.biz.service.InfraTokenService;
import com.wxy.infra.biz.vo.admin.AuthLoginReqVO;
import com.wxy.infra.biz.vo.admin.AuthMenuRespVO;
import com.wxy.infra.biz.vo.admin.AuthRefreshReqVO;
import com.wxy.infra.biz.vo.AuthTokenRespVO;
import com.wxy.infra.biz.vo.admin.AuthUpdatePasswordReqVO;
import com.wxy.infra.biz.vo.admin.AuthUserInfoRespVO;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理后台认证服务实现。
 *
 * <p>登录失败统一返回「用户名或密码错误」，不区分账号不存在与密码错误，避免被用来枚举账号；
 * 账号停用单独提示，因为这不是安全问题，而是需要使用者知道的状态问题。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Service
public class InfraAdminAuthServiceImpl implements InfraAdminAuthService {

    /** 用户 Mapper */
    @Resource
    private InfraUserMapper infraUserMapper;

    /** 凭证服务：两端共用 */
    @Resource
    private InfraTokenService infraTokenService;

    /** 权限服务：提供角色与权限标识 */
    @Resource
    private InfraPermissionService infraPermissionService;

    /** 菜单服务：提供当前用户导航菜单树 */
    @Resource
    private InfraMenuService infraMenuService;

    /** 密码编码器 */
    @Resource
    private PasswordEncoder passwordEncoder;

    /**
     * 管理后台登录
     *
     * @param reqVO   登录入参
     * @param loginIp 登录 IP，可为空
     * @return 凭证返回体
     */
    @Override
    public AuthTokenRespVO login(AuthLoginReqVO reqVO, String loginIp) {
        InfraUser user = infraUserMapper.selectOne(new LambdaQueryWrapper<InfraUser>()
                .eq(InfraUser::getUsername, reqVO.getUsername()));
        if (user == null || !passwordEncoder.matches(reqVO.getPassword(), user.getPassword())) {
            throw new BizException(InfraErrorConstant.LOGIN_FAILED);
        }
        if (!CommonStatusEnum.ENABLED.getValue().equals(user.getStatus())) {
            throw new BizException(InfraErrorConstant.USER_DISABLED);
        }
        return infraTokenService.createTokenPair(user.getId(), UserTypeEnum.ADMIN, user.getUsername(), loginIp);
    }

    /**
     * 续期
     *
     * @param reqVO 续期入参
     * @return 新的凭证返回体
     */
    @Override
    public AuthTokenRespVO refresh(AuthRefreshReqVO reqVO) {
        return infraTokenService.refresh(reqVO.getRefreshToken(), UserTypeEnum.ADMIN);
    }

    /**
     * 登出
     *
     * @param authorization 当前请求的凭证头
     */
    @Override
    public void logout(String authorization) {
        infraTokenService.revoke(authorization);
    }

    /**
     * 查询当前登录用户信息（含角色与权限）
     *
     * @return 用户信息
     */
    @Override
    public AuthUserInfoRespVO getUserInfo() {
        Long userId = currentUserId();
        InfraUser user = infraUserMapper.selectById(userId);
        if (user == null || !CommonStatusEnum.ENABLED.getValue().equals(user.getStatus())) {
            // 账号被删除或停用后，已签发的凭证不再可用
            throw new UnauthorizedException("登录用户不存在或已停用");
        }
        AuthUserInfoRespVO vo = new AuthUserInfoRespVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setUserType(UserContextHolder.getUserType());
        vo.setRoleCodes(new ArrayList<>(infraPermissionService.getRoleCodes(userId)));
        vo.setPerms(new ArrayList<>(infraPermissionService.getPermissions(userId)));
        return vo;
    }

    /**
     * 查询当前登录用户的导航菜单树
     *
     * @return 菜单树
     */
    @Override
    public List<AuthMenuRespVO> listMenus() {
        return infraMenuService.listMenuTreeByUser(currentUserId());
    }

    /**
     * 修改当前登录用户的密码
     *
     * <p>改密成功后该用户的所有已签发凭证立即失效（含当前会话），前端需要重新登录；
     * 这样即使旧密码已经泄露，攻击者手里的凭证也会同步作废。
     *
     * @param reqVO 修改密码入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(AuthUpdatePasswordReqVO reqVO) {
        Long userId = currentUserId();
        InfraUser user = infraUserMapper.selectById(userId);
        if (user == null) {
            throw new UnauthorizedException("登录用户不存在或已停用");
        }
        if (!passwordEncoder.matches(reqVO.getOldPassword(), user.getPassword())) {
            throw new BizException(InfraErrorConstant.OLD_PASSWORD_ERROR);
        }
        user.setPassword(passwordEncoder.encode(reqVO.getNewPassword()));
        infraUserMapper.updateById(user);
        infraTokenService.revokeAll(userId);
    }

    /**
     * 获取当前登录用户 ID，未登录直接抛 401
     *
     * <p>正常流程下凭证拦截器已经写入上下文，这里是防御性检查：
     * 一旦拦截器配置被改错，也应该拒绝而不是按匿名放行。
     *
     * @return 当前登录用户 ID
     */
    private Long currentUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new UnauthorizedException();
        }
        return userId;
    }
}
