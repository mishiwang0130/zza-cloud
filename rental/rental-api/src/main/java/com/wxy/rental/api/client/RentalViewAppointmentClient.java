package com.wxy.rental.api.client;

import com.wxy.common.core.result.Result;
import com.wxy.rental.api.client.fallback.RentalViewAppointmentClientFallbackFactory;
import com.wxy.rental.api.constant.RentalApiConstant;
import com.wxy.rental.api.dto.ViewAppointmentRespDTO;
import com.wxy.rental.api.dto.ViewAppointmentCreateReqDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * rental 对外发布的看房预约服务间接口。
 *
 * <p>预约人（userId）写在入参里而不是读请求头：调用方可能不在 Web 请求线程上，
 * 透传的登录上下文不一定存在；服务侧按入参落库，任何调用方都不会写出没有主人的预约。
 *
 * @author wxy
 * @date 2026/10/05
 */
@FeignClient(name = RentalApiConstant.SERVICE_NAME, contextId = "rentalViewAppointmentClient",
        fallbackFactory = RentalViewAppointmentClientFallbackFactory.class)
public interface RentalViewAppointmentClient {

    /**
     * 提交看房预约
     *
     * @param viewAppointmentCreateReqDTO 预约入参，预约人必填
     * @return 空响应
     */
    @PostMapping("/internal-api/view-appointment/create")
    Result<Void> create(@Validated @RequestBody ViewAppointmentCreateReqDTO viewAppointmentCreateReqDTO);

    /**
     * 查询用户最近的待看房预约
     *
     * @param userId 用户 ID
     * @return 最近一条待看房预约，没有时 data 为 null
     */
    @GetMapping("/internal-api/view-appointment/get")
    Result<ViewAppointmentRespDTO> getByUserId(@RequestParam("userId") Long userId);

}
