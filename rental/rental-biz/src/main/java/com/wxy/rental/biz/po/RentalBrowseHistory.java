package com.wxy.rental.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 房源浏览记录表 {@code rental_browse_history} 的实体。
 *
 * <p>表里刻意没有单独的浏览时间字段：浏览时间就是 {@code create_time}（建表脚本里也是这么注释的），
 * 多一个字段就有两个时间口径，对不上时没人说得清以哪个为准。
 *
 * <p>记录由 MQ 异步写入、只提供查询：同一用户看同一房间只留一条，重复浏览刷新浏览时间（见 {@code RentalBrowseHistoryService}）。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rental_browse_history")
public class RentalBrowseHistory extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 浏览用户 App 用户 ID */
    private Long userId;

    /** 浏览房间 ID */
    private Long roomId;
}
