/**
 * ai-agent 对外发布包：服务名常量，以及将来的跨服务 DTO 与 Feign 客户端接口。
 *
 * <p>本包只放「别的服务需要知道」的东西：接口契约、传输对象与对外常量。
 * 业务实现（Controller、Service、Mapper、PO、VO）都在 {@code ai-agent-biz} 里，不对外暴露。
 *
 * @author wxy
 * @date 2026/10/05
 */
package com.wxy.ai.agent.api;
