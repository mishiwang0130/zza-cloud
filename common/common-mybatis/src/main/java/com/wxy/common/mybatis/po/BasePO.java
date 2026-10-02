package com.wxy.common.mybatis.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.wxy.common.core.enums.DeleteStatusEnum;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 数据库实体基类：每张表都有的公共字段放在这里，业务 PO 继承它即可。
 *
 * <p>公共字段包含主键、创建/更新人、创建/更新时间与逻辑删除标记；
 * 其中四个审计字段由 {@code AuditMetaObjectHandler} 自动填充，业务代码不要手工赋值。
 *
 * <p><b>子类注意</b>：Lombok 的 {@code @Data} 不会把父类字段算进 equals/hashCode，
 * 子类需要标注 {@code @EqualsAndHashCode(callSuper = true)}，否则两个 ID 相同但审计字段不同的对象会被判定相等。
 *
 * @author wxy
 * @date 2026/10/02
 */
@Data
public abstract class BasePO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键：数据库自增，跨服务传递时注意前端精度问题（HTTP 层已把 Long 序列化成字符串） */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 创建时间：插入时自动填充 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 创建人 ID：插入时自动填充，0 表示系统或未登录 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    /** 更新时间：插入与更新时自动填充 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 更新人 ID：插入与更新时自动填充，0 表示系统或未登录 */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /** 逻辑删除标记：0 未删除、1 已删除，取值见 {@code DeleteStatusEnum} */
    @TableLogic
    @TableField(value = "is_delete")
    private Integer isDelete = DeleteStatusEnum.NOT_DELETED.getValue();
}
