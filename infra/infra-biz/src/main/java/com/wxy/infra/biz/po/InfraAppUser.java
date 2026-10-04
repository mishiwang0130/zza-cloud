package com.wxy.infra.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户端用户表 {@code infra_app_user} 的实体：一个 app 用户一条记录。
 *
 * <p>只存账号档案；登录用的验证码在 Redis、会话凭证在 infra_token，都不落这张表。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("infra_app_user")
public class InfraAppUser extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 登录手机号，唯一索引 {@code uk_infra_app_user_mobile}，app 端以此为账号 */
    private String mobile;

    /** 昵称：注册时用手机号脱敏值兜底 */
    private String nickname;

    /** 头像文件 ID，0 表示未设置；只存 ID，展示地址由文件接口按需签发 */
    private Long avatarFileId;

    /** 状态：0 启用、1 停用，取值见 {@code CommonStatusEnum}；停用后不允许登录 */
    private Integer status;
}
