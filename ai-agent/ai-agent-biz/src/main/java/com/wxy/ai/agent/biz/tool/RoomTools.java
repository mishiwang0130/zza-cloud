package com.wxy.ai.agent.biz.tool;

import com.wxy.ai.agent.biz.constant.AiAgentConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 房源查询工具（需要调 rental 的服务间接口，当前为空实现）。
 *
 * <p><b>为什么是空实现</b>：房源数据在 rental 服务，本服务只能通过 {@code /internal-api/**}
 * 调它，而 rental 目前还没有发布这类服务间接口。工具的参数校验、描述与返回格式已经定好，
 * 接入时只需要把方法体里的 TODO 补完（含 rental 侧接口与 Feign 客户端）。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Slf4j
@Component
public class RoomTools {

    /** 远程能力未就绪时的统一提示：模型会据此如实告诉用户，而不是编造房源 */
    private static final String REMOTE_NOT_READY = "房源实时查询功能正在接入中，请稍后再试；"
            + "如需了解租赁规则与费用口径，我可以先为你解答。";

    /**
     * 查询可租房源
     *
     * @param cityId   城市 ID，可空
     * @param minRent  月租金下限（元），可空
     * @param maxRent  月租金上限（元），可空
     * @param roomCount 户型室数，可空
     * @param minArea  面积下限（㎡），可空
     * @param keyword  关键字（房间号 / 公寓名），可空
     * @param limit    最多返回条数，可空
     * @param toolContext 工具上下文：本轮登录用户身份（框架注入，不暴露给模型）
     * @return 给模型看的房源列表文本
     */
    @Tool(name = "searchAvailableRooms",
            description = "查询当前可租房源列表，可按城市、预算、户型、面积、关键字筛选。"
                    + "当用户问「有哪些房子、多少钱、几室几厅、还有没有空房」时调用。")
    public String searchAvailableRooms(
            @ToolParam(required = false, description = "城市 ID，不知道就留空") Long cityId,
            @ToolParam(required = false, description = "月租金下限，单位元") Integer minRent,
            @ToolParam(required = false, description = "月租金上限，单位元") Integer maxRent,
            @ToolParam(required = false, description = "户型室数，2 表示两室") Integer roomCount,
            @ToolParam(required = false, description = "面积下限，单位平方米") Integer minArea,
            @ToolParam(required = false, description = "关键字，按房间号或公寓名称模糊匹配") String keyword,
            @ToolParam(required = false, description = "最多返回条数，默认 5，最大 10") Integer limit,
            ToolContext toolContext) {
        // TODO wxy 接入 rental 的房源查询服务间接口，分三步：
        //   1) rental-api 发布契约：RentalRoomClient#searchAvailableRooms(RoomSearchReqDTO)，
        //      路径建议 POST /internal-api/room/searchAvailableRooms，入参用本方法的参数（外加 userId 可选），
        //      出参 List<RoomSummaryDTO>（房间 ID、公寓名、房间号、租金、面积、室数、朝向、封面地址）。
        //   2) rental-biz 在 controller/internal 下写 RentalRoomClientImpl implements RentalRoomClient，
        //      复用 RentalAppRoomService#pageRoom 的查询条件（只查已发布公寓下的已发布房间）。
        //   3) 本服务：pom 引 rental-api，启动类 @EnableFeignClients 增加 "com.wxy.rental.api.client"，
        //      组件扫描增加 "com.wxy.rental.api"（降级工厂是 @Component），本类注入该客户端后：
        //      校验 minRent <= maxRent、limit 收敛到 1~10，把结果按「- 房源 ID x：公寓 房间号，N室，租金 x 元/月」格式化。
        //   4) 远端调用失败要返回友好提示（参考 RentalErrorConstant 的错误码语义），不要把异常抛给模型。
        Long userId = resolveUserId(toolContext);
        log.debug("searchAvailableRooms 尚未接入 rental 接口：userId={}, cityId={}", userId, cityId);
        return REMOTE_NOT_READY;
    }

    /**
     * 查询房源详情
     *
     * @param roomId      房间 ID（必须来自 searchAvailableRooms 的结果）
     * @param toolContext 工具上下文
     * @return 给模型看的房源详情文本
     */
    @Tool(name = "getRoomDetail",
            description = "按房间 ID 查询房源详情（租金、面积、朝向、配套、描述）。房间 ID 必须来自 searchAvailableRooms 的结果。")
    public String getRoomDetail(
            @ToolParam(description = "房间 ID，来自房源查询结果") Long roomId,
            ToolContext toolContext) {
        // TODO wxy 接入 rental 的服务间详情接口：
        //   1) rental-api 加 RentalRoomClient#getRoomDetail(Long roomId)，路径 POST /internal-api/room/getDetail，
        //      出参 RoomDetailDTO（房间、公寓、图片地址、配套、费用项）。
        //   2) rental-biz 的 RentalRoomClientImpl 复用 RentalAppRoomService#getRoom 的组装逻辑（注意：服务间调用不写浏览记录）。
        //   3) 本类注入客户端后调用，把结果格式化成多行文本；roomId 为空或远端报「房间不存在」时返回对应提示。
        Long userId = resolveUserId(toolContext);
        log.debug("getRoomDetail 尚未接入 rental 接口：userId={}, roomId={}", userId, roomId);
        return REMOTE_NOT_READY;
    }

    /**
     * 从工具上下文取当前登录用户 ID
     *
     * <p>用户身份由 ChatService 在请求线程从令牌里取出后放进 toolContext，
     * 不由模型生成——否则用户可以用一句提示词冒用别人的身份。
     *
     * @param toolContext 工具上下文，可以为 null
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
