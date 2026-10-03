package com.wxy.infra.biz.vo.admin;

import com.wxy.common.core.vo.PageReqVO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户分页查询入参：模糊条件都允许为空，为空表示不参与过滤。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageReqVO extends PageReqVO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户名，前缀模糊匹配 */
    private String username;

    /** 昵称，前后模糊匹配 */
    private String nickname;

    /** 手机号，前缀模糊匹配 */
    private String mobile;

    /** 状态：0 启用、1 停用，为空表示全部 */
    private Integer status;
}
