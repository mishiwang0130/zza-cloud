package com.wxy.infra.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.CommonStatusEnum;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.util.DesensitizeUtil;
import com.wxy.infra.biz.constant.InfraErrorConstant;
import com.wxy.infra.biz.mapper.InfraAppUserMapper;
import com.wxy.infra.biz.po.InfraAppUser;
import com.wxy.infra.biz.service.InfraAppAuthService;
import com.wxy.infra.biz.service.InfraFileService;
import com.wxy.infra.biz.service.InfraSmsCodeService;
import com.wxy.infra.biz.service.InfraTokenService;
import com.wxy.infra.biz.vo.AuthTokenRespVO;
import com.wxy.infra.biz.vo.FileRespVO;
import com.wxy.infra.biz.vo.app.AppAuthUserInfoRespVO;
import com.wxy.infra.biz.vo.app.AuthAppLoginReqVO;
import com.wxy.infra.biz.vo.app.AuthAppRefreshReqVO;
import com.wxy.infra.biz.vo.app.AuthAppUpdateProfileReqVO;
import jakarta.annotation.Resource;
import java.util.List;
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

    /** 昵称长度上限，与 {@code infra_app_user.nickname} 列长度保持一致 */
    private static final int NICKNAME_MAX_LENGTH = 64;

    /** 头像文件 ID 为 0 表示未设置 */
    private static final long NO_AVATAR_FILE_ID = 0L;

    /** 短信验证码服务 */
    @Resource
    private InfraSmsCodeService infraSmsCodeService;

    /** 用户端用户 Mapper */
    @Resource
    private InfraAppUserMapper infraAppUserMapper;

    /** 凭证服务：admin 端与 app 端共用，这里传 UserTypeEnum.APP */
    @Resource
    private InfraTokenService infraTokenService;

    /** 文件服务：把头像 fileId 换成预签名访问地址 */
    @Resource
    private InfraFileService infraFileService;

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

    /**
     * 用户端续期
     *
     * @param reqVO 续期入参
     * @return 新的凭证返回体
     */
    @Override
    public AuthTokenRespVO refresh(AuthAppRefreshReqVO reqVO) {
        // 端类型固定传 APP：续期凭证的 user_type 不是 2 时会抛「登录端类型不匹配」
        return infraTokenService.refresh(reqVO.getRefreshToken(), UserTypeEnum.APP);
    }

    /**
     * 用户端登出
     *
     * @param authorization 当前请求的凭证头，可以为空
     */
    @Override
    public void logout(String authorization) {
        infraTokenService.revoke(authorization);
    }

    /**
     * 查询当前登录用户信息（手机号脱敏、头像按需签发地址）
     *
     * @return 用户信息
     */
    @Override
    public AppAuthUserInfoRespVO getUserInfo() {
        InfraAppUser user = requireLoginUser();
        AppAuthUserInfoRespVO vo = new AppAuthUserInfoRespVO();
        vo.setId(user.getId());
        vo.setMobile(DesensitizeUtil.mobile(user.getMobile()));
        vo.setNickname(user.getNickname());
        vo.setAvatarFileId(user.getAvatarFileId());
        vo.setAvatarUrl(avatarUrl(user.getAvatarFileId()));
        return vo;
    }

    /**
     * 修改当前登录用户的个人资料
     *
     * @param reqVO 修改入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(AuthAppUpdateProfileReqVO reqVO) {
        InfraAppUser user = requireLoginUser();
        // 昵称去掉首尾空格后再校验长度：只填空格等于没填，不能把空白昵称写进库
        String nickname = reqVO.getNickname().trim();
        if (nickname.isEmpty() || nickname.length() > NICKNAME_MAX_LENGTH) {
            throw new BizException(CommonErrorConstant.PARAM_ERROR, "昵称长度需为 1~64 个字符");
        }
        user.setNickname(nickname);
        if (reqVO.getAvatarFileId() != null) {
            // 0 表示清空头像：直接把 file_id 置 0，展示时自然会回 null 地址
            user.setAvatarFileId(reqVO.getAvatarFileId());
        }
        infraAppUserMapper.updateById(user);
    }

    /**
     * 取当前登录用户，未登录或账号已删除 / 停用时按 401 处理
     *
     * @return 用户端用户实体
     */
    private InfraAppUser requireLoginUser() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new UnauthorizedException();
        }
        InfraAppUser user = infraAppUserMapper.selectById(userId);
        if (user == null || !CommonStatusEnum.ENABLED.getValue().equals(user.getStatus())) {
            // 账号被删除或停用后，已签发的凭证不再可用
            throw new UnauthorizedException("登录用户不存在或已停用");
        }
        return user;
    }

    /**
     * 取头像的预签名访问地址
     *
     * @param avatarFileId 头像文件 ID，0 表示未设置
     * @return 预签名访问地址；未设置或文件查不到时返回 null
     */
    private String avatarUrl(Long avatarFileId) {
        if (avatarFileId == null || avatarFileId == NO_AVATAR_FILE_ID) {
            return null;
        }
        List<FileRespVO> files = infraFileService.listByIds(List.of(avatarFileId));
        return files.stream()
                .filter(file -> avatarFileId.equals(file.getId()))
                .map(FileRespVO::getUrl)
                .findFirst()
                .orElse(null);
    }
}
