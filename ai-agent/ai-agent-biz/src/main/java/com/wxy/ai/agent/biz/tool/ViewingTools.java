package com.wxy.ai.agent.biz.tool;

import com.wxy.ai.agent.biz.constant.AiAgentConstant;
import com.wxy.ai.agent.biz.util.AiAgentRedisKeyUtil;
import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.UserTypeEnum;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.TimeUnit;

/**
 * 看房预约工具。
 *
 * <p>写操作必须幂等：工具侧按「用户 + 预约时间」占一个 10 分钟的幂等位，
 * 避免模型在同一轮里重复调用产生两条预约；远程提交失败会释放幂等位，允许用户立即重试。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Slf4j
@Component
public class ViewingTools {

    /**
     * 看房时间格式：工具描述里给模型的是 {@code yyyy-MM-dd HH:mm:ss}，秒可省略
     *
     * <p>不能用 {@code LocalDateTime.parse(text)}：那是 ISO 格式，要求日期与时间之间是 {@code T}，
     * 模型按描述传 {@code 2026-10-08 15:00:00} 会直接解析失败。
     */
    private static final DateTimeFormatter APPOINTMENT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]");

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
            description = "帮用户提交看房预约。必须先与用户确认公寓和看房时间，再调用本工具；"
                    + "同一用户、同一时间不得重复预约。")
    public String createViewAppointment(
            @ToolParam(description = "公寓 名称，来自房源查询结果") String apartmentName,
            @ToolParam(description = "期望看房时间，格式 yyyy-MM-dd HH:mm:ss") String appointmentTime,
            @ToolParam(required = false, description = "备注，可留空") String remark,
            ToolContext toolContext) {

        Long userId = resolveUserId(toolContext);
        if (userId == null) {
            return "用户未登录，提醒用户先登录后再预约看房";
        }
        // 工具跑在弹性线程池上，当前线程没有登录上下文：临时补上身份，Feign 才能把 X-User-Id 透传给 rental，
        // 下游落库时 create_by 才不会退化成系统用户 0；callWith 在 finally 里恢复，不会串到下一个请求。
        return UserContextHolder.callWith(buildLoginUser(userId),
                () -> doCreateViewAppointment(userId, apartmentName, appointmentTime, remark));
    }

    /**
     * 提交看房预约的实际逻辑（此时当前线程已经带上登录身份）
     *
     * @param userId          预约人 ID
     * @param apartmentName   公寓名称
     * @param appointmentTime 期望看房时间
     * @param remark          备注
     * @return 给模型看的提交结果文本
     */
    private String doCreateViewAppointment(Long userId, String apartmentName, String appointmentTime, String remark) {
        String key = AiAgentRedisKeyUtil.viewAppointmentKey(userId, appointmentTime);
        // setIfAbsent 一次完成「查重 + 占位」：并发调用时只有一个能占到 key，不会两条都通过查重
        if (!Boolean.TRUE.equals(redisUtil.setIfAbsent(key, 1, 10, TimeUnit.MINUTES))) {
            return "该工具同一用户同一参数在十分钟内调用过一次";
        }

        ViewAppointmentCreateReqDTO viewAppointmentCreateReqDTO = new ViewAppointmentCreateReqDTO();
        viewAppointmentCreateReqDTO.setUserId(userId);
        viewAppointmentCreateReqDTO.setApartmentName(apartmentName);
        viewAppointmentCreateReqDTO.setRemark(remark);
        try {
            viewAppointmentCreateReqDTO.setAppointmentTime(parseAppointmentTime(appointmentTime));
        } catch (DateTimeParseException ex) {
            evictAppointmentDedupeKey(key);
            return "告知用户预约时间格式不正确，请按 yyyy-MM-dd HH:mm:ss 重新提供";
        }
        try {
            Result<Void> result = rentalViewAppointmentClient.create(viewAppointmentCreateReqDTO);
            if (!result.isSuccess()) {
                evictAppointmentDedupeKey(key);
                return "告知用户创建预约记录失败，请稍后重试";
            }
        } catch (Exception e) {
            // 没落库成功就不能占着幂等位，否则用户十分钟内重试会被误判成重复预约
            evictAppointmentDedupeKey(key);
            return "告知用户创建预约记录失败，请稍后重试";
        }

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
        Long userId = resolveUserId(toolContext);
        if (userId == null) {
            return "用户未登录，提醒用户先登录后再查看预约记录";
        }
        return UserContextHolder.callWith(buildLoginUser(userId), () -> doListMyAppointments(userId));
    }

    /**
     * 查询我的看房预约的实际逻辑（此时当前线程已经带上登录身份）
     *
     * @param userId 当前登录用户 ID
     * @return 给模型看的预约列表文本
     */
    private String doListMyAppointments(Long userId) {
        try {
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
    private Long resolveUserId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object userId = toolContext.getContext().get(AiAgentConstant.TOOL_CONTEXT_USER_ID);
        return userId instanceof Long value ? value : null;
    }

    /**
     * 用工具上下文里的用户 ID 构造登录身份
     *
     * <p>智能客服只服务用户端，端类型固定为 APP；用户名不参与鉴权与审计，这里不传。
     *
     * @param userId 用户 ID
     * @return 登录身份
     */
    private LoginUser buildLoginUser(Long userId) {
        return new LoginUser(userId, UserTypeEnum.APP.getValue(), null);
    }

    /**
     * 解析看房时间：先按工具描述里的 {@code yyyy-MM-dd HH:mm[:ss]} 解析，失败再兼容 ISO 格式
     *
     * @param appointmentTime 模型给的时间文本
     * @return 看房时间
     * @throws DateTimeParseException 两种格式都解析失败时抛出
     */
    private LocalDateTime parseAppointmentTime(String appointmentTime) {
        try {
            return LocalDateTime.parse(appointmentTime, APPOINTMENT_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            // 模型偶尔会按 ISO 格式带 T 返回，这里兜一下，避免把能识别的时间也判成格式错误
            return LocalDateTime.parse(appointmentTime);
        }
    }

    /**
     * 远程提交失败时删掉幂等占位，让用户可以立即重试
     *
     * @param key 幂等 key
     */
    private void evictAppointmentDedupeKey(String key) {
        try {
            redisUtil.delete(key);
        } catch (Exception ex) {
            log.warn("清理预约幂等 key 失败：{}", key, ex);
        }
    }
}
