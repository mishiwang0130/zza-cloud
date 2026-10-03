package com.wxy.infra.biz.bo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 用户权限缓存对象：缓存「用户有哪些角色编码、有哪些权限标识」，一次读缓存即可同时满足超管判断与权限校验。
 *
 * <p>角色与菜单授权关系变更时按用户或全量清理缓存，避免出现「菜单刚配好但接口仍然无权限」。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class InfraPermissionCacheBO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 角色编码集合，例如 {@code super_admin} */
    private List<String> roleCodes;

    /** 权限标识集合，例如 {@code infra:user:create} */
    private List<String> perms;
}
