package com.wxy.rental.biz.convert;

import com.wxy.rental.biz.vo.app.ImageRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * 图片返回体转换：内部统一用服务层的图片返回体，用户端接口再转成 {@code vo/app/ImageRespVO}。
 *
 * <p>两端图片返回体现在字段相同，但按契约稿各留一份（App 以后可能补宽高），因此需要这一层显式转换：字段分叉时改这里，不会波及服务层。
 *
 * <p>两端图片返回体的类名相同，Java 没有 import 别名，因此这里只 import 用户端的那份，管理端类型用全限定名。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RentalAppImageConvert {

    /**
     * 服务层图片返回体转用户端图片返回体
     *
     * @param vo 服务层图片返回体，可以为 null
     * @return 用户端图片返回体，入参为 null 时返回 null
     */
    ImageRespVO toAppImageRespVO(com.wxy.rental.biz.vo.admin.ImageRespVO vo);

    /**
     * 服务层图片返回体列表转用户端图片返回体列表
     *
     * @param list 服务层图片返回体列表，可以为 null
     * @return 用户端图片返回体列表，入参为 null 时返回 null
     */
    List<ImageRespVO> toAppImageRespVOList(List<com.wxy.rental.biz.vo.admin.ImageRespVO> list);
}
