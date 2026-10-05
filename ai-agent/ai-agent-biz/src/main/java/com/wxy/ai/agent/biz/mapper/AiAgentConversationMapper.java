package com.wxy.ai.agent.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxy.ai.agent.biz.po.AiAgentConversation;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 会话表 Mapper。
 *
 * <p>统计字段用「库内自增」而不是「查出来 +1 再写回」：同一会话的问答可能并发提交，
 * 读改写会丢更新，{@code message_count = message_count + #{delta}} 由数据库保证原子性。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Mapper
public interface AiAgentConversationMapper extends BaseMapper<AiAgentConversation> {

    /**
     * 累加消息条数并刷新最后消息时间
     *
     * @param id              会话 ID
     * @param delta           本次新增条数（一次问答为 2：用户 + AI）
     * @param lastMessageTime 最后一条消息时间
     * @return 影响行数
     */
    int increaseMessageCount(@Param("id") Long id,
                             @Param("delta") int delta,
                             @Param("lastMessageTime") LocalDateTime lastMessageTime);
}
