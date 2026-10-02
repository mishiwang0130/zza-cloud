package com.wxy.common.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.wxy.common.core.context.UserContextHolder;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;

/**
 * 审计字段自动填充：插入与更新时把创建/更新人、创建/更新时间写进实体。
 *
 * <p>只对 {@code BasePO} 中标注了 {@code FieldFill} 的字段生效，
 * 且仅在字段值为 null 时填充——业务代码显式赋过值就尊重业务值。
 *
 * <p>操作人取自 {@link UserContextHolder}：未登录或服务间内部调用时写入 0，
 * 避免 {@code create_by} 出现 null 导致审计与统计口径混乱。
 *
 * @author wxy
 * @date 2026/10/02
 */
public class AuditMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时填充创建人、创建时间、更新人、更新时间
     *
     * @param metaObject 实体元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long userId = UserContextHolder.getUserIdOrDefault();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "createBy", Long.class, userId);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateBy", Long.class, userId);
    }

    /**
     * 更新时填充更新人、更新时间
     *
     * @param metaObject 实体元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        strictUpdateFill(metaObject, "updateBy", Long.class, UserContextHolder.getUserIdOrDefault());
    }
}
