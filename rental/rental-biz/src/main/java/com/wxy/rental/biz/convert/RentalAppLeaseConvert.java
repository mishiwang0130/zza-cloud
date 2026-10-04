package com.wxy.rental.biz.convert;

import com.wxy.rental.biz.po.RentalLease;
import com.wxy.rental.biz.vo.app.AppLeaseItemRespVO;
import com.wxy.rental.biz.vo.app.AppLeaseRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * App 租约对象转换：实体到用户端返回体。
 *
 * <p>公寓名、房间号、状态中文名、封面图、合同地址与图片都由 Service 按需组装，这里 {@code ignore}。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RentalAppLeaseConvert {

    /**
     * 实体转列表行返回体（回填字段由 Service 负责）
     *
     * @param po 租约实体，可以为 null
     * @return 列表行返回体，入参为 null 时返回 null
     */
    @Mapping(target = "apartmentName", ignore = true)
    @Mapping(target = "roomNumber", ignore = true)
    @Mapping(target = "statusName", ignore = true)
    @Mapping(target = "coverFileId", ignore = true)
    AppLeaseItemRespVO toItemRespVO(RentalLease po);

    /**
     * 实体列表转列表行返回体列表
     *
     * @param list 租约实体列表，可以为 null
     * @return 列表行返回体列表，入参为 null 时返回 null
     */
    List<AppLeaseItemRespVO> toItemRespVOList(List<RentalLease> list);

    /**
     * 实体转详情返回体（回填字段由 Service 负责）
     *
     * @param po 租约实体，可以为 null
     * @return 详情返回体，入参为 null 时返回 null
     */
    @Mapping(target = "apartmentName", ignore = true)
    @Mapping(target = "roomNumber", ignore = true)
    @Mapping(target = "statusName", ignore = true)
    @Mapping(target = "coverFileId", ignore = true)
    @Mapping(target = "contractFileUrl", ignore = true)
    @Mapping(target = "images", ignore = true)
    AppLeaseRespVO toRespVO(RentalLease po);
}
