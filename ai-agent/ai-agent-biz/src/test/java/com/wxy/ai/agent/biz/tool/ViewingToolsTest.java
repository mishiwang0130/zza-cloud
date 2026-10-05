package com.wxy.ai.agent.biz.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.wxy.ai.agent.biz.constant.AiAgentConstant;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.result.Result;
import com.wxy.common.redis.util.RedisUtil;
import com.wxy.rental.api.client.RentalViewAppointmentClient;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

/**
 * 看房预约工具单元测试：用户身份只认工具上下文，取不到就提示登录，绝不带着 null 去调下游。
 *
 * @author wxy
 * @date 2026/10/06
 */
@ExtendWith(MockitoExtension.class)
class ViewingToolsTest {

    /** 预约远程客户端 */
    @Mock
    private RentalViewAppointmentClient rentalViewAppointmentClient;

    /** Redis 工具 */
    @Mock
    private RedisUtil redisUtil;

    /** 被测工具 */
    @InjectMocks
    private ViewingTools viewingTools;

    /**
     * 每个用例都从「当前线程没有登录上下文」开始：工具跑在弹性线程池上就是这种状态
     */
    @BeforeEach
    void clearUserContext() {
        UserContextHolder.clear();
    }

    /**
     * 提交：工具上下文里没有用户时只提示登录，不调远程、不占幂等位
     */
    @Test
    @DisplayName("提交预约：未登录时提示先登录且不调用远程")
    void createShouldRejectAnonymous() {
        String result = viewingTools.createViewAppointment(
                "文三路公寓", "2026-10-08 15:00:00", null, new ToolContext(Map.of()));

        assertThat(result).contains("先登录");
        verifyNoInteractions(rentalViewAppointmentClient, redisUtil);
    }

    /**
     * 查询：工具上下文里没有用户时只提示登录，不调远程
     */
    @Test
    @DisplayName("查询预约：未登录时提示先登录且不调用远程")
    void listShouldRejectAnonymous() {
        String result = viewingTools.listMyAppointments(new ToolContext(Map.of()));

        assertThat(result).contains("先登录");
        verifyNoInteractions(rentalViewAppointmentClient);
    }

    /**
     * 提交：远程调用期间线程上要有登录身份，Feign 拦截器才会带 {@code X-User-Id}，调用结束后必须恢复
     */
    @Test
    @DisplayName("提交预约：远程调用期间临时补上登录身份，结束后清理")
    void createShouldSetUserContextDuringRemoteCall() {
        when(redisUtil.setIfAbsent(anyString(), any(), anyLong(), any())).thenReturn(Boolean.TRUE);
        when(rentalViewAppointmentClient.create(any())).thenAnswer(invocation -> {
            assertThat(UserContextHolder.getUserId()).isEqualTo(77L);
            return Result.success();
        });

        String result = viewingTools.createViewAppointment("文三路公寓", "2026-10-08 15:00:00", null,
                new ToolContext(Map.of(AiAgentConstant.TOOL_CONTEXT_USER_ID, 77L)));

        assertThat(result).contains("成功");
        assertThat(UserContextHolder.getUserId()).isNull();
    }
}
