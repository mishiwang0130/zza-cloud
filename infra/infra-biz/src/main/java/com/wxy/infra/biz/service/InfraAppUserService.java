package com.wxy.infra.biz.service;

import com.wxy.common.core.vo.PageRespVO;
import com.wxy.infra.biz.vo.AppUserRespVO;
import com.wxy.infra.biz.vo.admin.AppUserPageReqVO;
import java.util.List;

/**
 * 用户端用户服务：按 ID 批量查询用户档案（服务间用），以及后台分页查询（管理端用）。
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

    /**
     * 分页查询 App 用户
     *
     * <p>供后台「App 用户」列表与租约表单的承租人选择器使用：关键字同时匹配昵称与手机号，
     * 前端可以先分页列出来，也可以按关键字远程搜索。
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    PageRespVO<AppUserRespVO> pageAppUser(AppUserPageReqVO reqVO);
}
