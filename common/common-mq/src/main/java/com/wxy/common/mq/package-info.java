/**
 * 消息公共能力：只服务 common 自身的 topic / tag 常量，不提供工具类。
 *
 * <p>包名约定：{@code common-mq} 对应 {@code com.wxy.common.mq}。
 * 服务有自己的 {@code XxxMqConstant}，不要跨模块共用同一份常量；
 * 本模块没有第三方依赖：常量是纯字符串，真正收发消息的服务自行声明 RocketMQ 客户端。
 */
package com.wxy.common.mq;
