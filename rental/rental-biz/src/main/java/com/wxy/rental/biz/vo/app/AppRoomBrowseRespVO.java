package com.wxy.rental.biz.vo.app;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * App 浏览记录返回体：留的是浏览流水，所以同房间多次浏览会出现多条，按浏览时间倒序返回。
 *
 * <p>带上房间号、公寓名、租金与封面图是为了这一屏能直接渲染卡片，不必再逐条回查房间详情；房间详情里更细的信息（图片、配套、费用项）不在这里返回。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class AppRoomBrowseRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 浏览记录 ID */
    private Long id;

    /** 浏览房间 ID */
    private Long roomId;

    /** 房间号 */
    private String roomNumber;

    /** 房间所属公寓 ID */
    private Long apartmentId;

    /** 房间所属公寓名称 */
    private String apartmentName;

    /** 房间月租金（元/月） */
    private BigDecimal rent;

    /** 封面图文件 ID */
    private Long coverFileId;

    /** 浏览时间（即记录的创建时间） */
    private LocalDateTime createTime;
}
