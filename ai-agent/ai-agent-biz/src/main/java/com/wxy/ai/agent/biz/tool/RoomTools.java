package com.wxy.ai.agent.biz.tool;

import cn.hutool.core.collection.CollUtil;
import com.wxy.ai.agent.biz.constant.AiAgentConstant;
import com.wxy.common.core.result.Result;
import com.wxy.rental.api.client.RentalRoomClient;
import com.wxy.rental.api.dto.RoomDetailDTO;
import com.wxy.rental.api.dto.RoomSearchReqDTO;
import com.wxy.rental.api.dto.RoomSummaryDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

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
    @Resource
    private RentalRoomClient rentalRoomClient;


    /**
     * 查询可租房源
     *
     * @param minRent  月租金下限（元），可空
     * @param maxRent  月租金上限（元），可空
     * @param roomCount 户型室数，可空
     * @param toolContext 工具上下文：本轮登录用户身份（框架注入，不暴露给模型）
     * @return 给模型看的房源列表文本
     */
    @Tool(name = "searchAvailableRooms",
            description = "查询当前可租房源列表，可按城市、预算、户型、面积、关键字筛选。"
                    + "当用户问「有哪些房子、多少钱、几室几厅、还有没有空房」时调用。")
    public String searchAvailableRooms(
            @ToolParam(required = false, description = "城市名称，不知道就留空") String cityName,
            @ToolParam(required = false, description = "月租金下限，单位元") Integer minRent,
            @ToolParam(required = false, description = "月租金上限，单位元") Integer maxRent,
            @ToolParam(required = false, description = "户型室数，2 表示两室") Integer roomCount,
            @ToolParam(required = false, description = "公寓名称") String apartmentName,
            ToolContext toolContext) {
        RoomSearchReqDTO roomSearchReqDTO = new RoomSearchReqDTO();
        roomSearchReqDTO.setCityName(cityName);
        roomSearchReqDTO.setRoomCount(roomCount);
        roomSearchReqDTO.setMaxRent(maxRent);
        roomSearchReqDTO.setMinRent(minRent);
        roomSearchReqDTO.setApartmentName(apartmentName);

        try {
            Result<List<RoomSummaryDTO>> listResult = rentalRoomClient.searchAvailableRooms(roomSearchReqDTO);
            List<RoomSummaryDTO> roomSummaryDTOList = listResult.requireData();
            if (CollUtil.isEmpty(roomSummaryDTOList)){
                return "告知用户未查到合适房间";
            }
            StringBuilder stringBuilder = new StringBuilder();
            for (int i=0;i<roomSummaryDTOList.size();i++) {
                RoomSummaryDTO roomSummaryDTO = roomSummaryDTOList.get(i);
                String format = String.format("公寓名称：%s ,房间号： %s ,租金: %b", roomSummaryDTO.getApartmentName(), roomSummaryDTO.getRoomNumber()
                        , roomSummaryDTO.getRent());
                stringBuilder.append(format);
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            return "告知用户查房间失败";
        }

    }

    /**
     * 查询房源详情
     *
     * @param roomNumber      房间 ID（必须来自 searchAvailableRooms 的结果）
     * @param toolContext 工具上下文
     * @return 给模型看的房源详情文本
     */
    @Tool(name = "getRoomDetail",
            description = "按房间 ID 查询房源详情（租金、面积、朝向、配套、描述）。房间 ID 必须来自 searchAvailableRooms 的结果。")
    public String getRoomDetail(
            @ToolParam(description = "房间号，来自房源查询结果") String roomNumber,
            ToolContext toolContext) {

        try {
            Result<RoomDetailDTO> roomDetail = rentalRoomClient.getRoomDetail(roomNumber);
            if (roomDetail == null){
                return "告知用户房间号可能有误，没有查到该房间";
            }
            RoomDetailDTO roomDetailDTO = roomDetail.requireData();

            return String.format("房间号：%s，租金：%s，配套：%s", roomDetailDTO.getRoomNumber(), roomDetailDTO.getRent(), roomDetailDTO.getLabels());
        } catch (Exception e) {
            return "告知用户查房间详情失败";
        }
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
