package com.wxy.infra.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 上传来源端：决定文件在对象存储里的目录前缀，便于按端归档与排查。
 *
 * <p>admin 端与 app 端共用同一个上传实现与同一张文件表，端类型不上库，
 * 只体现在对象名的目录前缀上（{@code admin/yyyyMMdd/...} 与 {@code app/yyyyMMdd/...}）。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Getter
@AllArgsConstructor
public enum InfraFileSourceEnum {

    /** 管理后台：由 admin 端上传接口传入 */
    ADMIN("admin", "管理后台"),

    /** 用户端：由 app 端上传接口传入 */
    APP("app", "用户端");

    /** 对象名的目录前缀 */
    private final String dirPrefix;

    /** 中文描述，用于日志与页面展示 */
    private final String label;
}
