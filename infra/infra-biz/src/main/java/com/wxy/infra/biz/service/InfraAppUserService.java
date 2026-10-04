package com.wxy.infra.biz.service;

import com.wxy.infra.biz.vo.AppUserRespVO;
import java.util.List;

/**
 * 用户端用户服务：按 ID 批量查询用户档案，供服务间接口把 {@code userId} 还原成昵称与手机号。
 *
 * <p>只提供查询：app 用户的注册、改资料、停用都归 {@link InfraAppAuthService}，
 * 这里不重复一遍，避免同一份状态出现两个写入入口。
 *
 * @author wxy
 * @date 2026/10/05
 */
public interface InfraAppUserService {

    /**
     * 按 ID 批量查询用户端用户
     *
     * <p>查不到的 ID 直接不返回，由调用方按「用户不存在」处理；重复 ID 会先去重，
     * 避免同一次查询对同一条记录查两遍。
     *
     * @param ids 用户 ID 列表，为空时直接返回空列表
     * @return 用户列表，按传入顺序不保证
     */
    List<AppUserRespVO> listByIds(List<Long> ids);
}
