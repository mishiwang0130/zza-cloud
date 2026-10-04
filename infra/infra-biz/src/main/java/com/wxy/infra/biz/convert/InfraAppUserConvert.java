package com.wxy.infra.biz.convert;

import com.wxy.infra.api.dto.AppUserSimpleDTO;
import com.wxy.infra.biz.po.InfraAppUser;
import com.wxy.infra.biz.vo.AppUserRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * 用户端用户对象转换：实体到返回体，返回体到服务间 DTO。
 *
 * <p>单独留一层是为了让「哪些字段可以对外」有明确落点：实体新增头像、状态等内部字段时，
 * 不会顺着默认映射偷偷发给其他服务。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InfraAppUserConvert {

    /**
     * 实体转返回体（只保留 ID、昵称、手机号）
     *
     * @param po 用户端用户实体，可以为 null
     * @return 返回体，入参为 null 时返回 null
     */
    AppUserRespVO toRespVO(InfraAppUser po);

    /**
     * 实体列表转返回体列表
     *
     * @param list 用户端用户实体列表，可以为 null
     * @return 返回体列表，入参为 null 时返回 null
     */
    List<AppUserRespVO> toRespVOList(List<InfraAppUser> list);

    /**
     * 返回体转服务间 DTO
     *
     * @param vo 用户端用户返回体，可以为 null
     * @return DTO，入参为 null 时返回 null
     */
    AppUserSimpleDTO toDTO(AppUserRespVO vo);

    /**
     * 返回体列表转 DTO 列表
     *
     * @param list 用户端用户返回体列表，可以为 null
     * @return DTO 列表，入参为 null 时返回 null
     */
    List<AppUserSimpleDTO> toDTOList(List<AppUserRespVO> list);
}
