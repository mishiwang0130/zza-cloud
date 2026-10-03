package com.wxy.infra.biz.convert;

import com.wxy.infra.biz.po.InfraRole;
import com.wxy.infra.biz.vo.admin.RoleRespVO;
import com.wxy.infra.biz.vo.admin.RoleSimpleRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * 角色对象转换：实体到返回体。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InfraRoleConvert {

    /**
     * 实体转返回体（menuIds 由 Service 单独装配）
     *
     * @param po 角色实体，可以为 null
     * @return 返回体，入参为 null 时返回 null
     */
    RoleRespVO toRespVO(InfraRole po);

    /**
     * 实体列表转返回体列表
     *
     * @param list 角色实体列表，可以为 null
     * @return 返回体列表，入参为 null 时返回 null
     */
    List<RoleRespVO> toRespVOList(List<InfraRole> list);

    /**
     * 实体转精简返回体（下拉用）
     *
     * @param po 角色实体，可以为 null
     * @return 精简返回体，入参为 null 时返回 null
     */
    RoleSimpleRespVO toSimpleRespVO(InfraRole po);

    /**
     * 实体列表转精简返回体列表
     *
     * @param list 角色实体列表，可以为 null
     * @return 精简返回体列表，入参为 null 时返回 null
     */
    List<RoleSimpleRespVO> toSimpleRespVOList(List<InfraRole> list);
}
