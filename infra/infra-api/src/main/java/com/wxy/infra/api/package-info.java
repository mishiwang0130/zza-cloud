/**
 * infra 服务对外发布包：存放跨服务传输的 DTO、供其他服务调用的 Feign 客户端接口与对外常量。
 *
 * <p>目前发布的契约分三类：凭证校验与权限判断（其他服务没有自己的用户表时用它做鉴权）、
 * 字典与区划读取（业务服务回填中文名与三级联动）、文件按 ID 批量查询（把 fileId 换成预签名地址）。
 * 业务实现一律放在 {@code infra-biz}，其他服务只允许依赖 {@code infra-api}。
 *
 * @author wxy
 * @date 2026/10/03
 */
package com.wxy.infra.api;
