package com.wxy.infra.biz.constant;

import com.wxy.common.core.result.ErrorCode;

/**
 * infra 业务错误码常量：服务位固定 {@code 02}，模块位 001 用户、002 角色、003 菜单、004 认证、005 文件。
 *
 * <p>公共错误码（参数错误、未登录、无权限、系统异常等）用 {@code CommonErrorConstant}，
 * 这里只放 infra 自己的业务错误；业务代码只引用常量，禁止出现数字字面量。
 *
 * @author wxy
 * @date 2026/10/03
 */
public final class InfraErrorConstant {

    // ==================== 001 用户 ====================

    /** 用户不存在：按 ID 查不到未删除的用户 */
    public static final ErrorCode USER_NOT_FOUND = new ErrorCode(1_02_001_0001, "用户不存在");

    /** 用户名已存在：新增或改名时与既有用户冲突 */
    public static final ErrorCode USERNAME_EXISTS = new ErrorCode(1_02_001_0002, "用户名已存在");

    /** 手机号已被使用：手机号建了唯一索引，重复会直接报这个错 */
    public static final ErrorCode MOBILE_EXISTS = new ErrorCode(1_02_001_0003, "手机号已被使用");

    /** 用户已停用：停用后不允许登录 */
    public static final ErrorCode USER_DISABLED = new ErrorCode(1_02_001_0004, "用户已停用，请联系管理员");

    /** 不允许删除当前登录用户：避免把自己删掉后无法继续操作 */
    public static final ErrorCode USER_SELF_DELETE_FORBIDDEN = new ErrorCode(1_02_001_0005, "不能删除当前登录用户");

    /** 超级管理员账号保护：不允许删除、停用或重置超级管理员的密码 */
    public static final ErrorCode SUPER_ADMIN_PROTECTED = new ErrorCode(1_02_001_0006, "超级管理员账号不允许该操作");

    /** 原密码不正确：修改自己密码时旧密码校验失败 */
    public static final ErrorCode OLD_PASSWORD_ERROR = new ErrorCode(1_02_001_0007, "原密码不正确");

    /** 不允许停用当前登录用户：避免把自己停用后无法继续操作 */
    public static final ErrorCode USER_SELF_DISABLE_FORBIDDEN = new ErrorCode(1_02_001_0008, "不能停用当前登录用户");

    // ==================== 002 角色 ====================

    /** 角色不存在 */
    public static final ErrorCode ROLE_NOT_FOUND = new ErrorCode(1_02_002_0001, "角色不存在");

    /** 角色编码已存在 */
    public static final ErrorCode ROLE_CODE_EXISTS = new ErrorCode(1_02_002_0002, "角色编码已存在");

    /** 角色已分配给用户，不能删除 */
    public static final ErrorCode ROLE_IN_USE = new ErrorCode(1_02_002_0003, "角色已分配给用户，不能删除");

    /** 超级管理员角色保护：不允许删除或改编码 */
    public static final ErrorCode SUPER_ADMIN_ROLE_PROTECTED = new ErrorCode(1_02_002_0004, "超级管理员角色不允许该操作");

    // ==================== 003 菜单 ====================

    /** 菜单不存在 */
    public static final ErrorCode MENU_NOT_FOUND = new ErrorCode(1_02_003_0001, "菜单不存在");

    /** 存在子菜单，不能删除 */
    public static final ErrorCode MENU_HAS_CHILDREN = new ErrorCode(1_02_003_0002, "存在子菜单，不能删除");

    /** 菜单已分配给角色，不能删除 */
    public static final ErrorCode MENU_IN_USE = new ErrorCode(1_02_003_0003, "菜单已分配给角色，不能删除");

    /** 上级菜单不合法：上级不存在，或把自己/自己的子孙设为上级会形成环 */
    public static final ErrorCode MENU_PARENT_INVALID = new ErrorCode(1_02_003_0004, "上级菜单不合法");

    // ==================== 004 认证（admin 端与 app 端共用） ====================

    /** 用户名或密码错误：不区分是账号不存在还是密码错，避免被枚举账号 */
    public static final ErrorCode LOGIN_FAILED = new ErrorCode(1_02_004_0001, "用户名或密码错误");

    /** 续期凭证无效或已过期 */
    public static final ErrorCode REFRESH_TOKEN_INVALID = new ErrorCode(1_02_004_0002, "续期凭证无效或已过期");

    /** 登录端类型不匹配：admin 端签发的凭证不能用于 app 端，反之亦然 */
    public static final ErrorCode TOKEN_USER_TYPE_MISMATCH = new ErrorCode(1_02_004_0003, "登录端类型不匹配");

    /** 短信服务未配置：没有注入 APP_SMS_ACCESS_KEY_ID / APP_SMS_ACCESS_KEY_SECRET */
    public static final ErrorCode SMS_NOT_CONFIGURED = new ErrorCode(1_02_004_0004, "短信服务未配置，请联系管理员");

    /** 验证码发送过于频繁：同一手机号在发送间隔内重复请求 */
    public static final ErrorCode SMS_SEND_TOO_FREQUENT = new ErrorCode(1_02_004_0005, "验证码发送过于频繁，请稍后再试");

    /** 短信发送失败：阿里云返回失败或调用异常 */
    public static final ErrorCode SMS_SEND_ERROR = new ErrorCode(1_02_004_0006, "短信发送失败，请稍后再试");

    /** 验证码已过期或不存在：没发过、已过期，或已经用掉 */
    public static final ErrorCode SMS_CODE_EXPIRED = new ErrorCode(1_02_004_0007, "验证码已过期，请重新获取");

    /** 验证码不正确：与缓存里的验证码对不上 */
    public static final ErrorCode SMS_CODE_ERROR = new ErrorCode(1_02_004_0008, "验证码不正确");

    // ==================== 005 文件 ====================

    /** 上传文件为空 */
    public static final ErrorCode FILE_EMPTY = new ErrorCode(1_02_005_0001, "上传文件不能为空");

    /** 上传文件超过大小限制 */
    public static final ErrorCode FILE_SIZE_EXCEEDED = new ErrorCode(1_02_005_0002, "上传文件超过大小限制");

    /** 文件上传失败：对象存储写入异常 */
    public static final ErrorCode FILE_UPLOAD_ERROR = new ErrorCode(1_02_005_0003, "文件上传失败");

    /** 分片上传会话不存在或已过期：uploadId 查不到，或不属于当前用户与上传端 */
    public static final ErrorCode FILE_CHUNK_SESSION_NOT_FOUND = new ErrorCode(1_02_005_0004, "分片上传会话不存在或已过期，请重新上传");

    /** 分片序号不合法：小于 1 或超过总分片数 */
    public static final ErrorCode FILE_CHUNK_NUMBER_INVALID = new ErrorCode(1_02_005_0005, "分片序号不合法");

    /** 分片大小不合法：分片为空、非末片小于 5MiB，或超过约定的分片大小 */
    public static final ErrorCode FILE_CHUNK_SIZE_INVALID = new ErrorCode(1_02_005_0006, "分片大小不合法");

    /** 分片未全部上传：合并时对象存储里的分片数量或总大小与声明不一致 */
    public static final ErrorCode FILE_CHUNK_INCOMPLETE = new ErrorCode(1_02_005_0007, "分片未全部上传，无法合并");

    /** 分片上传失败：分片转发对象存储异常 */
    public static final ErrorCode FILE_CHUNK_UPLOAD_ERROR = new ErrorCode(1_02_005_0008, "分片上传失败");

    /** 合并分片失败：对象存储合并异常 */
    public static final ErrorCode FILE_CHUNK_COMPLETE_ERROR = new ErrorCode(1_02_005_0009, "合并分片失败");

    /** 取消分片上传失败：对象存储取消异常 */
    public static final ErrorCode FILE_CHUNK_ABORT_ERROR = new ErrorCode(1_02_005_0010, "取消上传失败");

    // ==================== 006 字典 ====================

    /** 字典类型不存在 */
    public static final ErrorCode DICT_TYPE_NOT_FOUND = new ErrorCode(1_02_006_0001, "字典类型不存在");

    /** 字典类型编码已存在：编码唯一，按未删除的数据判断 */
    public static final ErrorCode DICT_TYPE_CODE_EXISTS = new ErrorCode(1_02_006_0002, "字典类型编码已存在");

    /** 字典类型下还有字典数据，不能删除 */
    public static final ErrorCode DICT_TYPE_IN_USE = new ErrorCode(1_02_006_0003, "该字典类型下存在字典数据，不能删除");

    /** 字典数据不存在 */
    public static final ErrorCode DICT_DATA_NOT_FOUND = new ErrorCode(1_02_006_0004, "字典数据不存在");

    /** 同一字典类型下的字典值已存在：值唯一，按未删除的数据判断 */
    public static final ErrorCode DICT_DATA_VALUE_EXISTS = new ErrorCode(1_02_006_0005, "同一字典类型下字典值已存在");

    /**
     * 工具类常量类，禁止实例化
     */
    private InfraErrorConstant() {
    }
}
