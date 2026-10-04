/**
 * rental 对外发布的契约：跨服务调用的 DTO、Feign 客户端接口与对外常量。
 *
 * <p>本模块只放「别的服务依赖 rental 时要用到的东西」。rental 自己的业务实现
 * （Controller、Service、Mapper、PO、VO）全部在 {@code rental-biz}，
 * 其他服务禁止依赖 {@code rental-biz}。
 *
 * <p>本期的内容只有服务名常量：rental 还没有对外发布的服务间接口，
 * 用户端租约、浏览记录写入等功能等 infra 的 App 用户与登录就绪后再按契约稿补。
 *
 * @author wxy
 * @date 2026/10/04
 */
package com.wxy.rental.api;
