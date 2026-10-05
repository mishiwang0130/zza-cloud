package com.wxy.ai.agent.biz.tool;

import com.wxy.ai.agent.biz.constant.AiAgentConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 看房预约工具（需要调 rental 的服务间接口，当前为空实现）。
 *
 * <p>写操作必须幂等：提交预约时由 rental 侧按「用户 + 公寓 + 时间」去重，
 * 避免模型重复调用产生两条预约。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Slf4j
@Component
public class ViewingTools {

    /** 远程能力未就绪时的统一提示 */
    private static final String REMOTE_NOT_READY = "预约功能正在接入中，请稍后再试。"
            + "你可以在小程序「看房预约」页手动提交，或稍后让我帮你预约。";

    /**
     * 提交看房预约
     *
     * @param apartmentId 公寓 ID
     * @param name        联系人称呼
     * @param mobile      联系人手机号
     * @param appointmentTime 期望看房时间，格式 yyyy-MM-dd HH:mm
     * @param remark      备注，可空
     * @param toolContext 工具上下文（当前登录用户）
     * @return 给模型看的提交结果文本
     */
    @Tool(name = "createViewAppointment",
            description = "帮用户提交看房预约。必须先与用户确认公寓、联系人和看房时间，再调用本工具；"
                    + "同一用户、同一公寓、同一时间重复提交不会产生重复预约。")
    public String createViewAppointment(
            @ToolParam(description = "公寓 ID，来自房源查询结果") Long apartmentId,
            @ToolParam(description = "联系人称呼") String name,
            @ToolParam(description = "联系人手机号，11 位中国大陆手机号") String mobile,
            @ToolParam(description = "期望看房时间，格式 yyyy-MM-dd HH:mm") String appointmentTime,
            @ToolParam(required = false, description = "备注，可留空") String remark,
            ToolContext toolContext) {
        // TODO wxy 接入 rental 的预约服务间接口：
        //   1) rental-api 加 RentalViewAppointmentClient#create(ViewAppointmentCreateReqDTO)，
        //      路径 POST /internal-api/view-appointment/create，入参含 userId（由本工具从 toolContext 取）、
        //      apartmentId、name、mobile、appointmentTime、remark。
        //   2) rental-biz 的 RentalViewAppointmentClientImpl 复用 RentalAppAppointmentService#createAppointment，
        //      但 userId 由入参传入（App 端是从登录上下文取，服务间调用没有上下文，必须显式带）。
        //   3) 本类注入客户端后调用；参数校验：手机号 1[3-9]\\d{9}、时间不能早于当前时间、
        //      时间格式 yyyy-MM-dd HH:mm，格式不对返回提示让模型重新与用户确认。
        //   4) 幂等：接口侧按「用户 + 公寓 + 时间」去重，重复提交返回同一条预约，本工具据此提示「已提交过」。
        Long userId = requireUserId(toolContext);
        log.debug("createViewAppointment 尚未接入 rental 接口：userId={}, apartmentId={}", userId, apartmentId);
        return REMOTE_NOT_READY;
    }

    /**
     * 查询我的看房预约
     *
     * @param pageNum     页码，可空
     * @param pageSize    每页条数，可空
     * @param toolContext 工具上下文（当前登录用户）
     * @return 给模型看的预约列表文本
     */
    @Tool(name = "listMyAppointments",
            description = "查询当前用户自己的看房预约记录。当用户问「我约了几号看房」「我的预约」时调用。")
    public String listMyAppointments(
            @ToolParam(required = false, description = "页码，从 1 开始") Integer pageNum,
            @ToolParam(required = false, description = "每页条数，默认 10") Integer pageSize,
            ToolContext toolContext) {
        // TODO wxy 接入 rental 的「我的预约」服务间接口：
        //   1) rental-api 加 RentalViewAppointmentClient#pageMine(Long userId, PageReqDTO)，
        //      路径 POST /internal-api/view-appointment/pageMine。
        //   2) rental-biz 的实现复用 RentalAppAppointmentService#pageAppointment（把 userId 从入参传入）。
        //   3) 本类注入后调用，把结果格式化成「- 预约 ID x：公寓名，时间 yyyy-MM-dd HH:mm，状态 z」。
        Long userId = requireUserId(toolContext);
        log.debug("listMyAppointments 尚未接入 rental 接口：userId={}", userId);
        return REMOTE_NOT_READY;
    }

    /**
     * 从工具上下文取当前登录用户 ID
     *
     * @param toolContext 工具上下文
     * @return 用户 ID，取不到返回 null
     */
    private Long requireUserId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object userId = toolContext.getContext().get(AiAgentConstant.TOOL_CONTEXT_USER_ID);
        return userId instanceof Long value ? value : null;
    }
}
