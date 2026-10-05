package com.wxy.ai.agent.biz.tool;

import com.wxy.ai.agent.biz.constant.AiAgentConstant;
import com.wxy.ai.agent.biz.util.AiAgentRedisKeyUtil;
import com.wxy.common.core.result.Result;
import com.wxy.common.redis.util.RedisUtil;
import com.wxy.rental.api.client.RentalViewAppointmentClient;
import com.wxy.rental.api.dto.ViewAppointmentCreateReqDTO;
import com.wxy.rental.api.dto.ViewAppointmentRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

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


    @Resource
    private RentalViewAppointmentClient rentalViewAppointmentClient;

    @Resource
    private RedisUtil redisUtil;
    /**
     * 提交看房预约
     *
     * @param apartmentName 公寓 名称
     * @param appointmentTime 期望看房时间，格式 yyyy-MM-dd HH:mm
     * @param remark      备注，可空
     * @param toolContext 工具上下文（当前登录用户）
     * @return 给模型看的提交结果文本
     */
    @Tool(name = "createViewAppointment",
            description = "帮用户提交看房预约。必须先与用户确认公寓、联系人和看房时间，再调用本工具；"
                    + "同一用户、同一公寓、同一时间重复提交不会产生重复预约。")
    public String createViewAppointment(
            @ToolParam(description = "公寓 名称，来自房源查询结果") String apartmentName,
            @ToolParam(description = "期望看房时间，格式 yyyy-MM-dd HH:mm") String appointmentTime,
            @ToolParam(required = false, description = "备注，可留空") String remark,
            ToolContext toolContext) {

        Long userId = requireUserId(toolContext);
        String key = AiAgentRedisKeyUtil.viewAppointmentKey(userId, appointmentTime);
        Boolean hasKey = redisUtil.hasKey(key);
        if (hasKey){
            return "该工具同一用户同一参数在十分钟内调用过一次";
        }

        ViewAppointmentCreateReqDTO viewAppointmentCreateReqDTO = new ViewAppointmentCreateReqDTO();
        viewAppointmentCreateReqDTO.setUserId(userId);
        viewAppointmentCreateReqDTO.setApartmentName(apartmentName);
        viewAppointmentCreateReqDTO.setAppointmentTime(appointmentTime);
        viewAppointmentCreateReqDTO.setRemark(remark);

        try {
            rentalViewAppointmentClient.create(viewAppointmentCreateReqDTO);
        } catch (Exception e) {
            return "创建预约记录失败，请稍后重试";
        }
        redisUtil.set(key,1,10, TimeUnit.MINUTES);

        return "预约存入成功";

    }

    /**
     * 查询我的看房预约
     *
     * @param toolContext 工具上下文（当前登录用户）
     * @return 给模型看的预约列表文本
     */
    @Tool(name = "listMyAppointments",
            description = "查询当前用户自己将来的看房预约记录。当用户问「我约了几号看房」「我的预约」时调用。")
    public String listMyAppointments(ToolContext toolContext) {
        try {
            Long userId = requireUserId(toolContext);
            Result<ViewAppointmentRespDTO> result = rentalViewAppointmentClient.getByUserId(userId);
            ViewAppointmentRespDTO viewAppointmentRespDTO = result.requireData();
            if (viewAppointmentRespDTO == null) {
                return "告知用户他名下没有未看房的预约记录";
            }
            return String.format("预约看房总数：%s，最新一条预约记录：公寓名：%s，预约时间：%s, 若要看全部可以让用户去我的里面查看",viewAppointmentRespDTO.getApartmentName()
            ,viewAppointmentRespDTO.getAppointmentTime(),viewAppointmentRespDTO.getUnViewCount());
        } catch (Exception e) {
            return "告知用户查询预约看房记录失败，可以去我的里面查看";
        }
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
