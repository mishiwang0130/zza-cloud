package com.wxy.common.core.constant;

/**
 * 全项目通用常量：不依赖任何中间件，所有服务都可以直接引用。
 *
 * <p>业务常量放各服务自己的常量类，Redis、MQ 前缀放对应能力模块的常量类，不要往这里堆。
 *
 * @author wxy
 * @date 2026/10/02
 */
public final class CommonConstant {

    /** 系统用户 ID：写入 create_by / update_by 时，0 表示系统操作或未登录 */
    public static final long SYSTEM_USER_ID = 0L;

    /** 根节点父 ID：树形结构（菜单、组织等）中顶级节点的 parentId */
    public static final long ROOT_PARENT_ID = 0L;

    /** 英文逗号：批量查询、批量删除时拼接 ID 的分隔符 */
    public static final String COMMA = ",";

    /**
     * 工具类常量类，禁止实例化
     */
    private CommonConstant() {
    }
}
