package com.wxy.rental.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 公寓精简返回体：房间表单里的公寓下拉用。
 *
 * <p>只给 ID、名称与详细地址：下拉要能区分同名公寓（不同门店常见同名），所以带上地址；其余字段这一屏用不到，不返。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class ApartmentSimpleRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 公寓 ID */
    private Long id;

    /** 公寓名称 */
    private String name;

    /** 详细地址（不含省市区前缀） */
    private String addressDetail;
}
