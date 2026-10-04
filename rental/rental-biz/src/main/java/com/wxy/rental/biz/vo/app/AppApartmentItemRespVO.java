package com.wxy.rental.biz.vo.app;

import com.wxy.rental.biz.vo.DictItemVO;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 * App 公寓列表行返回体：只带列表卡片要展示的字段（不含介绍、电话、图片明细）。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class AppApartmentItemRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 公寓 ID */
    private Long id;

    /** 公寓名称 */
    private String name;

    /** 所在区县 ID */
    private Long districtId;

    /** 所在区县名称，由 Service 查 infra 区划回填 */
    private String districtName;

    /** 详细地址 */
    private String addressDetail;

    /** 最低起租月数 */
    private Integer minLeaseMonths;

    /** 押金月数 */
    private Integer depositMonths;

    /** 付款方式：1 月付、2 季付、3 半年付、4 年付 */
    private Integer paymentMethod;

    /** 付款方式中文名 */
    private String paymentMethodName;

    /** 该公寓已发布房间的最低月租金；没有已发布房间时为 null */
    private BigDecimal minRent;

    /** 封面图文件 ID：取该公寓图片里排序最靠前的一张，没有图片时为 null */
    private Long coverFileId;

    /** 公寓标签（编码 + 中文名） */
    private List<DictItemVO> labelCodes;

    /** 公寓配套（编码 + 中文名） */
    private List<DictItemVO> facilityCodes;
}
