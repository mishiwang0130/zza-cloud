package com.wxy.rental.biz.vo.admin;

import com.wxy.rental.biz.vo.DictItemVO;
import com.wxy.rental.biz.vo.FeeItemSimpleRespVO;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 公寓详情返回体：只带详情页要展示的内容。
 *
 * <p>刻意不含房间列表与租约：房间和租约是各自独立的功能，由各自的接口按需查询，硬塞进公寓详情会让详情接口随房间字段变化而不断修改。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class ApartmentRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 公寓 ID */
    private Long id;

    /** 公寓名称 */
    private String name;

    /** 公寓介绍 */
    private String introduction;

    /** 所在区县 ID */
    private Long districtId;

    /** 所在区县名称，由 Service 查 infra 区划回填 */
    private String districtName;

    /** 详细地址（不含省市区前缀） */
    private String addressDetail;

    /** 公寓前台电话 */
    private String phone;

    /** 最低起租月数 */
    private Integer minLeaseMonths;

    /** 押金月数 */
    private Integer depositMonths;

    /** 付款方式：1 月付、2 季付、3 半年付、4 年付 */
    private Integer paymentMethod;

    /** 付款方式中文名 */
    private String paymentMethodName;

    /** 发布状态：0 未发布、1 已发布 */
    private Integer publishStatus;

    /** 标签（编码 + 中文名） */
    private List<DictItemVO> labelCodes;

    /** 配套（编码 + 中文名） */
    private List<DictItemVO> facilityCodes;

    /** 公寓包含的费用项 */
    private List<FeeItemSimpleRespVO> feeItems;

    /** 公寓图片（含预签名访问地址） */
    private List<ImageRespVO> images;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
