package com.wxy.rental.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 房源图片关系表 {@code rental_image} 的实体：公寓与房间的图片共用一张表。
 *
 * <p>只存 {@code file_id}，不存访问地址：MinIO 的地址是预签名的、会过期，
 * 落库就是脏数据；展示时按 {@code file_id} 重新签发。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rental_image")
public class RentalImage extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属对象类型：1 公寓、2 房间，取值见 {@code RentalImageItemTypeEnum} */
    private Integer itemType;

    /** 所属对象 ID：公寓 ID 或房间 ID */
    private Long itemId;

    /** 文件 ID（infra 文件表），访问地址按需重新签发 */
    private Long fileId;

    /** 排序号，越小越靠前；相同排序号时按主键升序（即提交顺序） */
    private Integer sort;
}
