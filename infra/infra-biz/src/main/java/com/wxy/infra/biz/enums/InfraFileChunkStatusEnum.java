package com.wxy.infra.biz.enums;

/**
 * 分片上传会话状态：区分「还在传分片」与「已经合并完成」。
 *
 * <p>合并完成后会话不立刻删除，而是改成 {@link #COMPLETED} 再保留一小段时间：
 * 客户端重试 complete 时能直接拿到同一份结果，不会因为网络抖动重复落库。
 *
 * @author wxy
 * @date 2026/10/05
 */
public enum InfraFileChunkStatusEnum {

    /** 上传中：还可以继续上传分片与合并 */
    UPLOADING,

    /** 已完成：分片已合并并落库，后续 complete 直接返回缓存结果 */
    COMPLETED
}
