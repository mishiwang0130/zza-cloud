package com.wxy.common.core.validation;

/**
 * 修改场景的校验分组：与 {@link CreateGroup} 配合区分新增与修改的校验规则。
 *
 * <p>用法：{@code @NotNull(groups = UpdateGroup.class)}，接口上写
 * {@code @Validated(UpdateGroup.class)}。
 *
 * @author wxy
 * @date 2026/10/02
 */
public interface UpdateGroup {
}
