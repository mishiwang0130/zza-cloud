package com.wxy.rental.biz.constant;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 权限标识常量单元测试：perm 串必须能被前端、种子数据与接口注解共用同一份。
 *
 * <p>只校验两点：前缀固定 {@code rental:}（与 {@code infra_menu.perms} 的命名规则一致），
 * 且类内不重复——重复的 perm 会让两个不同按钮命中同一个权限，前端改一个按钮的授权会连带影响另一个。
 *
 * @author wxy
 * @date 2026/10/04
 */
class RentalPermissionConstantTest {

    /**
     * 所有 perm 串都以 rental: 开头且互不重复
     */
    @Test
    @DisplayName("权限标识：前缀固定 rental: 且类内不重复")
    void allPermissionsShouldBeUniqueWithRentalPrefix() throws IllegalAccessException {
        List<String> permissions = new ArrayList<>();
        for (Field field : RentalPermissionConstant.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != String.class) {
                continue;
            }
            String permission = (String) field.get(null);
            assertThat(permission).startsWith("rental:");
            assertThat(permissions).as("权限标识重复：%s", permission).doesNotContain(permission);
            permissions.add(permission);
        }
        assertThat(permissions).isNotEmpty();
    }
}
