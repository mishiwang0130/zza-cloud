package com.wxy.infra.biz.service;

import com.wxy.common.core.vo.PageRespVO;
import com.wxy.infra.biz.vo.admin.UserCreateReqVO;
import com.wxy.infra.biz.vo.admin.UserPageReqVO;
import com.wxy.infra.biz.vo.admin.UserResetPasswordReqVO;
import com.wxy.infra.biz.vo.admin.UserRespVO;
import com.wxy.infra.biz.vo.admin.UserUpdateReqVO;
import com.wxy.infra.biz.vo.admin.UserUpdateStatusReqVO;

/**
 * 管理后台用户服务。
 *
 * @author wxy
 * @date 2026/10/03
 */
public interface InfraUserService {

    /**
     * 新增用户并分配角色
     *
     * @param reqVO 新增入参
     * @return 新用户 ID
     */
    Long createUser(UserCreateReqVO reqVO);

    /**
     * 修改用户资料、状态与角色
     *
     * @param reqVO 修改入参
     */
    void updateUser(UserUpdateReqVO reqVO);

    /**
     * 删除用户（逻辑删除）并清空其角色关联
     *
     * @param id 用户 ID
     */
    void deleteUser(Long id);

    /**
     * 查询用户详情（含已分配角色）
     *
     * @param id 用户 ID
     * @return 用户详情
     */
    UserRespVO getUser(Long id);

    /**
     * 分页查询用户
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    PageRespVO<UserRespVO> pageUser(UserPageReqVO reqVO);

    /**
     * 修改用户状态
     *
     * @param reqVO 修改入参
     */
    void updateStatus(UserUpdateStatusReqVO reqVO);

    /**
     * 管理员重置他人密码
     *
     * @param reqVO 重置入参
     */
    void resetPassword(UserResetPasswordReqVO reqVO);
}
