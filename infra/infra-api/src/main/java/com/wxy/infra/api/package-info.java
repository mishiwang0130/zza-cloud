/**
 * infra 服务对外发布包：存放跨服务传输的 DTO、供其他服务调用的 Feign 客户端接口与对外常量。
 *
 * <p>当前为空壳：infra 的管理后台接口只被前端调用，其他服务暂不需要依赖本模块。
 * 将来有服务间调用需求时，在这里按 {@code dto}、{@code client} 包补充；
 * 业务实现一律放在 {@code infra-biz}，其他服务只允许依赖 {@code infra-api}。
 *
 * @author wxy
 * @date 2026/10/03
 */
package com.wxy.infra.api;
