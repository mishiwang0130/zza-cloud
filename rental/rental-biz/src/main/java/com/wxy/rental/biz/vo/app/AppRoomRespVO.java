package com.wxy.rental.biz.vo.app;

import com.wxy.rental.biz.vo.FeeItemSimpleRespVO;
import java.io.Serial;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * App 房间详情返回体：列表字段 + 所属公寓精简信息 + 房间图片。
 *
 * <p>这就是「一次功能一个接口」的例子：详情页要展示的数据一次给全，前端不需要再为图片、费用项多调几次；同时这个接口自己负责异步补写浏览记录，前端也不需要调写浏览记录的接口。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppRoomRespVO extends AppRoomItemRespVO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属公寓详细地址 */
    private String addressDetail;

    /** 所属公寓前台电话 */
    private String phone;

    /** 所属公寓介绍 */
    private String introduction;

    /** 所属公寓包含的费用项 */
    private List<FeeItemSimpleRespVO> feeItems;

    /** 房间图片（含预签名访问地址） */
    private List<ImageRespVO> images;
}
