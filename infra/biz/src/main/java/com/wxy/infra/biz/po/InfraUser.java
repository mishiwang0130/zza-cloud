package com.wxy.infra.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理后台用户表 {@code infra_user} 的实体。
 *
 * <p>只承载 admin 端账号；app 端用户表与登录由后续需求单独提供，通过 {@code infra_token.user_type} 区分端。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("infra_user")
public class InfraUser extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 登录用户名，唯一索引 {@code uk_infra_user_username}，创建后不允许修改 */
    private String username;

    /** 登录密码：存 BCrypt 哈希，禁止存明文 */
    private String password;

    /** 昵称：展示用，可与用户名不同 */
    private String nickname;

    /** 手机号，唯一索引 {@code uk_infra_user_mobile} */
    private String mobile;

    /** 状态：0 启用、1 停用，取值见 {@code CommonStatusEnum}；停用后不允许登录 */
    private Integer status;
}
