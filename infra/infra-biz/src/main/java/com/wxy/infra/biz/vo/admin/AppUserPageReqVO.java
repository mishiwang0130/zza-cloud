package com.wxy.infra.biz.vo.admin;

import com.wxy.common.core.vo.PageReqVO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * App 用户分页查询入参：给后台「App 用户」列表与租约表单的承租人选择器用。
 *
 * <p>只留一个 {@code keyword} 而不是昵称、手机号两个框：运营手上通常只有其中一项
 * （要么记得名字、要么记着号码），一个输入框就能查，前端也不用判断用户填的是哪一类。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppUserPageReqVO extends PageReqVO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 关键字：昵称前后模糊匹配，或手机号前缀匹配；为空表示不过滤 */
    private String keyword;

    /** 状态：0 启用、1 停用，为空表示全部 */
    private Integer status;
}
