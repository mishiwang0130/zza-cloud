package com.wxy.rental.biz.constant;

/**
 * 菜单权限标识常量：与 {@code infra_menu.perms} 中的取值一一对应。
 *
 * <p>控制器上的 {@code @RequiresPermission} 与菜单种子里的按钮权限必须引用同一份常量，
 * 否则会出现「菜单配了权限但接口校验的不是同一个串」这类只能靠人工比对才能发现的问题。
 *
 * <p><b>刻意没有定义 delete 权限</b>：契约稿 §5 的 perm 清单里还列了
 * {@code rental:apartment:delete} / {@code rental:room:delete} / {@code rental:lease:delete}，
 * 但本服务不提供删除接口——公寓、房间用下架（{@code updatePublishStatus}）代替，
 * 租约用状态置为 3 已取消代替。定义用不到的权限只会让菜单里出现点了没反应的按钮，
 * 故这里只保留真正被接口引用的权限标识。费用项是唯一的例外，它确实有删除接口。
 *
 * @author wxy
 * @date 2026/10/04
 */
public final class RentalPermissionConstant {

    /** 公寓管理：查询 */
    public static final String APARTMENT_QUERY = "rental:apartment:query";

    /** 公寓管理：新增 */
    public static final String APARTMENT_CREATE = "rental:apartment:create";

    /** 公寓管理：修改 */
    public static final String APARTMENT_UPDATE = "rental:apartment:update";

    /** 公寓管理：上架 / 下架 */
    public static final String APARTMENT_UPDATE_PUBLISH_STATUS = "rental:apartment:update-publish-status";

    /** 房间管理：查询 */
    public static final String ROOM_QUERY = "rental:room:query";

    /** 房间管理：新增 */
    public static final String ROOM_CREATE = "rental:room:create";

    /** 房间管理：修改 */
    public static final String ROOM_UPDATE = "rental:room:update";

    /** 房间管理：上架 / 下架 */
    public static final String ROOM_UPDATE_PUBLISH_STATUS = "rental:room:update-publish-status";

    /** 费用项管理：查询 */
    public static final String FEE_ITEM_QUERY = "rental:fee-item:query";

    /** 费用项管理：新增 */
    public static final String FEE_ITEM_CREATE = "rental:fee-item:create";

    /** 费用项管理：修改 */
    public static final String FEE_ITEM_UPDATE = "rental:fee-item:update";

    /** 费用项管理：删除 */
    public static final String FEE_ITEM_DELETE = "rental:fee-item:delete";

    /** 租约管理：查询 */
    public static final String LEASE_QUERY = "rental:lease:query";

    /** 租约管理：新增 */
    public static final String LEASE_CREATE = "rental:lease:create";

    /** 租约管理：修改 */
    public static final String LEASE_UPDATE = "rental:lease:update";

    /** 租约管理：状态流转 */
    public static final String LEASE_UPDATE_STATUS = "rental:lease:update-status";

    /** 看房预约管理：查询 */
    public static final String VIEW_APPOINTMENT_QUERY = "rental:view-appointment:query";

    /** 看房预约管理：状态流转 */
    public static final String VIEW_APPOINTMENT_UPDATE_STATUS = "rental:view-appointment:update-status";

    /**
     * 工具类常量类，禁止实例化
     */
    private RentalPermissionConstant() {
    }
}
