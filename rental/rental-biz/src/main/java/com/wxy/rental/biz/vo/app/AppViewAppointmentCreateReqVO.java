package com.wxy.rental.biz.vo.app;

import com.wxy.common.webmvc.validation.Mobile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * App 看房预约提交入参。
 *
 * <p>姓名与手机号必填并做校验：它们是前台联系看房人的唯一凭据，落库时按提交值快照下来，之后用户改资料也不影响这条预约。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class AppViewAppointmentCreateReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 预约公寓 ID，必填 */
    @NotNull(message = "预约公寓不能为空")
    private Long apartmentId;

    /** 预约人姓名，必填 */
    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名长度不能超过 64")
    private String name;

    /** 预约人手机号，必填，且必须符合中国大陆手机号格式 */
    @NotBlank(message = "手机号不能为空")
    @Mobile
    private String mobile;

    /** 预约看房时间，必填 */
    @NotNull(message = "预约看房时间不能为空")
    private LocalDateTime appointmentTime;

    /** 备注，如「希望看朝南的房间」 */
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
