package com.wxy.infra.biz.convert;

import com.wxy.infra.biz.po.InfraUser;
import com.wxy.infra.biz.vo.admin.UserRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * 用户对象转换：只做实体到返回体的转换。
 *
 * <p>新增/修改的入参转实体刻意不在这里做：密码要加密、状态要兜默认值、角色要单独维护，
 * 用 MapStruct 反而容易把明文密码或 null 静默带进库里。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InfraUserConvert {

    /**
     * 实体转返回体
     *
     * @param po 用户实体，可以为 null
     * @return 返回体，入参为 null 时返回 null
     */
    UserRespVO toRespVO(InfraUser po);

    /**
     * 实体列表转返回体列表
     *
     * @param list 用户实体列表，可以为 null
     * @return 返回体列表，入参为 null 时返回 null
     */
    List<UserRespVO> toRespVOList(List<InfraUser> list);
}
