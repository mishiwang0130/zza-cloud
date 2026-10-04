package com.wxy.rental.biz.vo.app;

import com.wxy.common.core.vo.PageReqVO;
import java.io.Serial;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * App 房间列表查询入参：只查已发布公寓下的已发布房间。
 *
 * <p>标签、配套多选之间是「或」的关系，与公寓列表保持一致的口径。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppRoomPageReqVO extends PageReqVO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属公寓 ID */
    private Long apartmentId;

    /** 所在区县 ID（公寓的区县） */
    private Long districtId;

    /** 所在市 ID（公寓的市）：由 Service 展开成区县 ID 列表后查询 */
    private Long cityId;

    /** 月租金下限（含） */
    private BigDecimal minRent;

    /** 月租金上限（含） */
    private BigDecimal maxRent;

    /** 户型室数 */
    private Integer roomCount;

    /** 面积下限（㎡） */
    private BigDecimal minArea;

    /** 面积上限（㎡） */
    private BigDecimal maxArea;

    /** 朝向编码，取 infra 字典 rental_room_orientation */
    private String orientation;

    /** 房间标签编码，多选之间是「或」 */
    private List<String> labelCodes;

    /** 房间配套编码，多选之间是「或」 */
    private List<String> facilityCodes;

    /** 只看空置房间：true 表示排除有生效中租约的房间 */
    private Boolean vacantOnly;

    /** 关键字：按房间号或所属公寓名称模糊匹配，空白视为不过滤 */
    private String keyword;

    /** 排序方式：0 综合（默认，保持现有排序）、1 月租金从低到高、2 月租金从高到低、3 最新上架；非法值按 0 */
    private Integer sortType;
}
