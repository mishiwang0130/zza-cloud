package com.wxy.common.core.result;

/**
 * 公共错误码常量：所有服务共用，服务位固定为 {@code 00}。
 *
 * <p>业务错误码不要写在这里，各服务在自己的模块里定义 {@code <服务名>ErrorConstant}；
 * 业务代码只引用常量，禁止直接写数字字面量。
 *
 * @author wxy
 * @date 2026/10/02
 */
public final class CommonErrorConstant {

    /** 成功：业务成功时统一使用该错误码 */
    public static final ErrorCode SUCCESS = new ErrorCode(1_00_000_0000, "成功");

    /** 参数校验失败：请求参数缺失、格式不合法等 */
    public static final ErrorCode PARAM_ERROR = new ErrorCode(1_00_000_0001, "参数校验失败");

    /** 未登录：未携带凭证或凭证已失效，对应 HTTP 401 */
    public static final ErrorCode UNAUTHORIZED = new ErrorCode(1_00_000_0002, "未登录或登录已过期");

    /** 无权限：已登录但不具备访问该资源的权限 */
    public static final ErrorCode FORBIDDEN = new ErrorCode(1_00_000_0003, "没有访问权限");

    /** 资源不存在：路由或目标资源找不到，对应 HTTP 404 */
    public static final ErrorCode NOT_FOUND = new ErrorCode(1_00_000_0004, "请求的资源不存在");

    /** 系统异常：未捕获的异常统一返回该错误码，对应 HTTP 500 */
    public static final ErrorCode SYSTEM_ERROR = new ErrorCode(1_00_000_0005, "系统繁忙，请稍后重试");

    /** 远端调用失败：服务间调用（Feign）返回异常状态时使用 */
    public static final ErrorCode REMOTE_CALL_ERROR = new ErrorCode(1_00_000_0006, "服务调用失败，请稍后重试");

    /** 文件操作失败：对象存储上传、下载、删除等操作失败时使用 */
    public static final ErrorCode FILE_OPERATION_ERROR = new ErrorCode(1_00_000_0007, "文件操作失败");

    /**
     * 工具类常量类，禁止实例化
     */
    private CommonErrorConstant() {
    }
}
