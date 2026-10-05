package com.wxy.ai.agent.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxy.ai.agent.biz.po.AiAgentMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话消息表 Mapper。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Mapper
public interface AiAgentMessageMapper extends BaseMapper<AiAgentMessage> {
}
