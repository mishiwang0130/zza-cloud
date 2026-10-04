package com.wxy.rental.biz.vo.app;

import com.wxy.rental.biz.vo.DictItemVO;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 * App 房间列表行返回体：只带列表卡片要展示的字段。
 *
 * <p>押金月数、付款方式、起租月数来自所属公寓：房间自己并不配置这些，但列表卡片要用来做筛选说明。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class AppRoomItemRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 房间 ID */
    private Long id;

    /** 所属公寓 ID */
    private Long apartmentId;

    /** 所属公寓名称 */
    private String apartmentName;

    /** 所在区县 ID（公寓的区县） */
    private Long districtId;

    /** 所在区县名称，由 Service 查 infra 区划回填 */
    private String districtName;

    /** 房间号 */
    private String roomNumber;

    /** 月租金（元/月） */
    private BigDecimal rent;

    /** 面积（㎡） */
    private BigDecimal area;

    /** 户型室数：1 一室、2 两室 */
    private Integer roomCount;

    /** 朝向编码 */
    private String orientation;

    /** 朝向中文名，由 Service 查 infra 字典回填 */
    private String orientationName;

    /** 楼层，如 3、3/18 */
    private String floorNo;

    /** 公寓押金月数 */
    private Integer depositMonths;

    /** 公寓付款方式：1 月付、2 季付、3 半年付、4 年付 */
    private Integer paymentMethod;

    /** 公寓最低起租月数 */
    private Integer minLeaseMonths;

    /** 封面图文件 ID：取该房间图片里排序最靠前的一张，没有图片时为 null */
    private Long coverFileId;

    /** 封面图预签名访问地址：由 Service 按 coverFileId 批量换取，无图或文件查不到时为 null */
    private String coverFileUrl;

    /** 房间标签（编码 + 中文名） */
    private List<DictItemVO> labelCodes;

    /** 房间配套（编码 + 中文名） */
    private List<DictItemVO> facilityCodes;
}
