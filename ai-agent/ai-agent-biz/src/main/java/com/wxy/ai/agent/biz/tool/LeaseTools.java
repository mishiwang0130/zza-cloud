package com.wxy.ai.agent.biz.tool;

import com.wxy.ai.agent.biz.constant.AiAgentConstant;
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

    /** 远程能力未就绪时的统一提示 */
    private static final String REMOTE_NOT_READY = "租约查询功能正在接入中，请稍后再试。"
            + "你可以在小程序「我的租约」里查看当前租约状态。";

    /**
     * 查询我的租约
     *
     * @param pageNum     页码，可空
     * @param pageSize    每页条数，可空
     * @param toolContext 工具上下文（当前登录用户）
     * @return 给模型看的租约列表文本
     */
    @Tool(name = "listMyLeases",
            description = "查询当前用户自己的租约（租期、租金、状态）。"
                    + "当用户问「我的租约什么时候到期」「我租的哪套房」时调用。")
    public String listMyLeases(
            @ToolParam(required = false, description = "页码，从 1 开始") Integer pageNum,
            @ToolParam(required = false, description = "每页条数，默认 10") Integer pageSize,
            ToolContext toolContext) {
        // TODO wxy 接入 rental 的「我的租约」服务间接口：
        //   1) rental-api 加 RentalLeaseClient#pageMine(Long userId, PageReqDTO)，
        //      路径 POST /internal-api/lease/pageMine。
        //   2) rental-biz 的实现复用 RentalAppLeaseService#pageLease（userId 由入参传入）。
        //   3) 本类注入后调用，把结果格式化成「- 租约 ID x：公寓名 房间号，租期 a ~ b，租金 x 元/月，状态 z」。
        //   4) 合同文件地址这类字段不要返回给模型：它属于敏感信息，用户要看就去小程序里看。
        Long userId = resolveUserId(toolContext);
        log.debug("listMyLeases 尚未接入 rental 接口：userId={}", userId);
        return REMOTE_NOT_READY;
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
