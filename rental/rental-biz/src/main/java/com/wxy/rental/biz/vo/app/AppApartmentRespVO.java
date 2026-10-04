package com.wxy.rental.biz.vo.app;

import com.wxy.rental.biz.vo.FeeItemSimpleRespVO;
import java.io.Serial;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * App 公寓详情返回体：列表字段 + 详情页要展示的介绍、电话、费用项与图片。
 *
 * <p>不含房间列表：房间是独立功能，App 端有自己的房间列表接口，公寓详情硬塞房间会让接口随房间字段变化。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppApartmentRespVO extends AppApartmentItemRespVO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 公寓介绍 */
    private String introduction;

    /** 公寓前台电话 */
    private String phone;

    /** 公寓包含的费用项 */
    private List<FeeItemSimpleRespVO> feeItems;

    /** 公寓图片（含预签名访问地址） */
    private List<ImageRespVO> images;
}
