package com.wxy.infra.biz.constant;

/**
 * 菜单权限标识常量：与 {@code infra_menu.perms} 中的取值一一对应。
 *
 * <p>控制器上的 {@code @RequiresPermission} 与种子数据里的按钮权限必须引用同一份常量，
 * 否则会出现「菜单配了权限但接口校验的不是同一个串」这类只能靠人工比对才能发现的问题。
 *
 * @author wxy
 * @date 2026/10/03
 */
public final class InfraPermissionConstant {

    /** 用户管理：查询 */
    public static final String USER_QUERY = "infra:user:query";

    /** 用户管理：新增 */
    public static final String USER_CREATE = "infra:user:create";

    /** 用户管理：修改 */
    public static final String USER_UPDATE = "infra:user:update";

    /** 用户管理：删除 */
    public static final String USER_DELETE = "infra:user:delete";

    /** 用户管理：重置密码 */
    public static final String USER_RESET_PASSWORD = "infra:user:reset-password";

    /** 用户管理：修改状态 */
    public static final String USER_UPDATE_STATUS = "infra:user:update-status";

    /** 角色管理：查询 */
    public static final String ROLE_QUERY = "infra:role:query";

    /** 角色管理：新增 */
    public static final String ROLE_CREATE = "infra:role:create";

    /** 角色管理：修改 */
    public static final String ROLE_UPDATE = "infra:role:update";

    /** 角色管理：删除 */
    public static final String ROLE_DELETE = "infra:role:delete";

    /** 角色管理：修改状态 */
    public static final String ROLE_UPDATE_STATUS = "infra:role:update-status";

    /** 菜单管理：查询 */
    public static final String MENU_QUERY = "infra:menu:query";

    /** 菜单管理：新增 */
    public static final String MENU_CREATE = "infra:menu:create";

    /** 菜单管理：修改 */
    public static final String MENU_UPDATE = "infra:menu:update";

    /** 菜单管理：删除 */
    public static final String MENU_DELETE = "infra:menu:delete";

    /** 字典类型：查询 */
    public static final String DICT_TYPE_QUERY = "infra:dict-type:query";

    /** 字典类型：新增 */
    public static final String DICT_TYPE_CREATE = "infra:dict-type:create";

    /** 字典类型：修改 */
    public static final String DICT_TYPE_UPDATE = "infra:dict-type:update";

    /** 字典类型：删除 */
    public static final String DICT_TYPE_DELETE = "infra:dict-type:delete";

    /** 字典数据：查询 */
    public static final String DICT_DATA_QUERY = "infra:dict-data:query";

    /** 字典数据：新增 */
    public static final String DICT_DATA_CREATE = "infra:dict-data:create";

    /** 字典数据：修改 */
    public static final String DICT_DATA_UPDATE = "infra:dict-data:update";

    /** 字典数据：删除 */
    public static final String DICT_DATA_DELETE = "infra:dict-data:delete";

    /**
     * 工具类常量类，禁止实例化
     */
    private InfraPermissionConstant() {
    }
}
