package com.wxy.infra.biz.service;

import com.wxy.common.core.vo.PageRespVO;
import com.wxy.infra.biz.vo.admin.RoleCreateReqVO;
import com.wxy.infra.biz.vo.admin.RolePageReqVO;
import com.wxy.infra.biz.vo.admin.RoleRespVO;
import com.wxy.infra.biz.vo.admin.RoleSimpleRespVO;
import com.wxy.infra.biz.vo.admin.RoleUpdateReqVO;
import com.wxy.infra.biz.vo.admin.RoleUpdateStatusReqVO;
import java.util.List;

/**
 * 管理后台角色服务。
 *
 * @author wxy
 * @date 2026/10/03
 */
public interface InfraRoleService {

    /**
     * 新增角色并分配菜单权限
     *
     * @param reqVO 新增入参
     * @return 新角色 ID
     */
    Long createRole(RoleCreateReqVO reqVO);

    /**
     * 修改角色并覆盖菜单权限
     *
     * @param reqVO 修改入参
     */
    void updateRole(RoleUpdateReqVO reqVO);

    /**
     * 删除角色（逻辑删除），已分配给用户的角色不允许删除
     *
     * @param id 角色 ID
     */
    void deleteRole(Long id);

    /**
     * 查询角色详情（含已分配菜单）
     *
     * @param id 角色 ID
     * @return 角色详情
     */
    RoleRespVO getRole(Long id);

    /**
     * 分页查询角色
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    PageRespVO<RoleRespVO> pageRole(RolePageReqVO reqVO);

    /**
     * 查询启用状态的角色列表（下拉用）
     *
     * @return 角色精简列表
     */
    List<RoleSimpleRespVO> listRole();

    /**
     * 修改角色状态
     *
     * @param reqVO 修改入参
     */
    void updateStatus(RoleUpdateStatusReqVO reqVO);
}
