/**
 * rental 的消息生产者：目前预留给房源浏览记录。
 *
 * <p>按契约稿，App 房间详情接口在返回详情的同时发一条浏览消息（{@code RentalBrowseHistoryProducer}），
 * 发送失败只记 warn、不影响详情返回。生产者的实现与 App 端接口一起，等 infra 的
 * App 用户表与登录就绪后补——现在写出来没有鉴权上下文可取 userId，只能编造数据。
 *
 * @author wxy
 * @date 2026/10/04
 */
package com.wxy.rental.biz.mq.producer;
