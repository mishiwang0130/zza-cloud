package com.wxy.common.core.security;

import com.wxy.common.core.context.LoginUser;
import java.util.Collection;

/**
 * 权限校验接口（SPI）：判断登录用户是否拥有指定权限，由各服务提供实现。
 *
 * <p>与 {@link TokenValidator} 的分工：那个回答「你是谁」，这个回答「你能不能访问这个接口」。
 *
 * <p><b>各服务实现什么</b>：
 * <ul>
 *   <li>自己持有角色菜单数据的服务（例如 infra）：按「用户 → 角色 → 菜单」算出权限集合，并处理超管之类的特例；</li>
 *   <li>没有权限数据的服务：调 infra 的服务间接口查询，或在自己服务内维护；</li>
 *   <li>完全不需要权限控制的服务：不用实现（前提是没有任何接口标注 {@link RequiresPermission}）。</li>
 * </ul>
 *
 * <p>被标注了 {@link RequiresPermission} 却没有提供实现的服务会启动失败，
 * 所以「本服务不做权限校验」这件事不会悄无声息地发生。
 *
 * @author wxy
 * @date 2026/10/03
 */
public interface PermissionChecker {

    /**
     * 判断登录用户是否拥有其中任意一个权限
     *
     * @param loginUser   当前登录用户（含用户 ID 与登录端类型，端相关的规则由实现自己判断）
     * @param permissions 接口要求的权限标识，至少一个
     * @return 拥有任意一个权限时返回 true
     */
    boolean hasAnyPermission(LoginUser loginUser, Collection<String> permissions);
}
