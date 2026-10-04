package com.wxy.rental.biz.service;

import com.wxy.rental.biz.enums.RentalImageItemTypeEnum;
import com.wxy.rental.biz.vo.admin.ImageItemReqVO;
import com.wxy.rental.biz.vo.admin.ImageRespVO;
import java.util.List;

/**
 * 房源图片服务：公寓与房间共用的一套图片读写。
 *
 * <p>图片是房源表单的一部分，不单独出接口：保存表单时随主表一起覆盖写，
 * 详情返回时直接带出来，前端不需要为了图片多调一次。
 *
 * @author wxy
 * @date 2026/10/04
 */
public interface RentalImageService {

    /**
     * 覆盖写某个对象的图片（随公寓 / 房间新增修改提交）
     *
     * <p>入参为 null 表示本次没有提交图片，此时不动库里的数据——表单类接口少传一个字段
     * 不应该等于「把图片全删了」；要清空请显式提交空列表。
     *
     * @param itemType 所属对象类型
     * @param itemId   所属对象 ID
     * @param images   图片列表，可以为 null（不改动）
     */
    void replaceImages(RentalImageItemTypeEnum itemType, Long itemId, List<ImageItemReqVO> images);

    /**
     * 查询某个对象的图片（详情返回用），按排序号升序
     *
     * @param itemType 所属对象类型
     * @param itemId   所属对象 ID
     * @return 图片列表（含预签名访问地址），没有图片时返回空列表
     */
    List<ImageRespVO> listImages(RentalImageItemTypeEnum itemType, Long itemId);
}
