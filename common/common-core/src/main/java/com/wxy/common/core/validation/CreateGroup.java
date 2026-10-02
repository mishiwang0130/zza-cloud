package com.wxy.common.core.validation;

/**
 * 新增场景的校验分组：同一个 VO 在新增与修改时校验规则不同，用它区分。
 *
 * <p>用法：{@code @NotNull(groups = UpdateGroup.class)}，接口上写
 * {@code @Validated(CreateGroup.class)}。
 *
 * @author wxy
 * @date 2026/10/02
 */
public interface CreateGroup {
}
