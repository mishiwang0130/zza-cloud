package com.wxy.rental.biz.vo.admin;

import com.wxy.rental.biz.vo.DictItemVO;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 房间详情返回体：只带详情页要展示的内容，外加所属公寓名称与图片。
 *
 * <p>不含公寓详情（介绍、费用项等）与租约：前者由公寓详情接口提供，后者是独立功能。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class RoomRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 房间 ID */
    private Long id;

    /** 所属公寓 ID */
    private Long apartmentId;

    /** 所属公寓名称，由 Service 按公寓 ID 回填 */
    private String apartmentName;

    /** 房间号 */
    private String roomNumber;

    /** 月租金（元/月） */
    private BigDecimal rent;

    /** 面积（㎡） */
    private BigDecimal area;

    /** 户型室数：1 一室、2 两室 */
    private Integer roomCount;

    /** 朝向编码，取 infra 字典 rental_room_orientation */
    private String orientation;

    /** 楼层，如 3、3/18 */
    private String floorNo;

    /** 标签（编码 + 中文名） */
    private List<DictItemVO> labelCodes;

    /** 配套（编码 + 中文名） */
    private List<DictItemVO> facilityCodes;

    /** 房间图片（含预签名访问地址） */
    private List<ImageRespVO> images;

    /** 发布状态：0 未发布、1 已发布 */
    private Integer publishStatus;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
