package com.wxy.ai.agent.biz.tool;

import com.wxy.ai.agent.biz.constant.AiAgentConstant;
import com.wxy.common.core.result.Result;
import com.wxy.common.core.util.RemoteCallUtil;
import com.wxy.rental.api.client.RentalLeaseClient;
import com.wxy.rental.api.dto.LeaseRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 租约工具（需要调 rental 的服务间接口，当前为空实现）。
 *
 * <p>只提供「查我的租约」这类只读能力：确认签约、申请退租这类涉及合同的动作必须由用户在小程序里
 * 自己点，不能让 AI 代劳。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Slf4j
@Component
public class LeaseTools {


    /**
     * 租赁远程调用客户端
     */
    @Resource
    private RentalLeaseClient rentalLeaseClient;

    /**
     * 查询我的租约
     *
     * @param toolContext 工具上下文（当前登录用户）
     * @return 给模型看的租约列表文本
     */
    @Tool(name = "listMyLeases",
            description = "查询当前用户自己的最近的租约（租期、租金、状态）。"
                    + "当用户问「我的租约什么时候到期」「我租的哪套房」时调用。")
    public String listMyLeases(ToolContext toolContext) {
        Long userId = resolveUserId(toolContext);
        if ( userId == null){
            return "用户未登录，提醒用户请先登录";
        }

        try {
            Result<LeaseRespDTO> result = rentalLeaseClient.getLeaseInfoByUserId(userId);
            LeaseRespDTO leaseRespDTO = result.requireData();
            if (leaseRespDTO == null) {
                return "当前用户在平台没有租约";
            }
            return String.format("当前用户存在租约，公寓名：%s, 房间号： %s, 租约开始时间: %s, 租约结束时间：%s, 租金: %b, 押金：%b",
                    leaseRespDTO.getApartmentName(), leaseRespDTO.getRoomNumber(), leaseRespDTO.getLeaseStartDate(),
                    leaseRespDTO.getLeaseEndDate(), leaseRespDTO.getRent(), leaseRespDTO.getDeposit());
        } catch (Exception e) {
            return "告知用户查询租约失败，暂时无法查询，请稍后";
        }
    }

    /**
     * 从工具上下文取当前登录用户 ID
     *
     * @param toolContext 工具上下文
     * @return 用户 ID，取不到返回 null
     */
    private Long resolveUserId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object userId = toolContext.getContext().get(AiAgentConstant.TOOL_CONTEXT_USER_ID);
        return userId instanceof Long value ? value : null;
    }
}
