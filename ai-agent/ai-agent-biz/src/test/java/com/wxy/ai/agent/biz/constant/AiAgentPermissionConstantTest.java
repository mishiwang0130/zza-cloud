package com.wxy.ai.agent.biz.constant;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 权限标识常量单元测试：前缀固定 {@code ai-agent:}，且类内不重复。
 *
 * <p>这些字符串要与 {@code sql/ai-agent.sql} 里 {@code infra_menu.perms} 的种子数据完全一致，
 * 不一致时后台按钮会 403 或按钮点不动。
 *
 * @author wxy
 * @date 2026/10/05
 */
class AiAgentPermissionConstantTest {

    /**
     * 所有 perm 串都以 ai-agent: 开头且互不重复
     */
    @Test
    @DisplayName("权限标识：前缀固定 ai-agent: 且类内不重复")
    void allPermissionsShouldBeUniqueWithAiAgentPrefix() throws IllegalAccessException {
        List<String> permissions = new ArrayList<>();
        for (Field field : AiAgentPermissionConstant.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != String.class) {
                continue;
            }
            String permission = (String) field.get(null);
            assertThat(permission).startsWith("ai-agent:");
            assertThat(permissions).as("权限标识重复：%s", permission).doesNotContain(permission);
            permissions.add(permission);
        }
        assertThat(permissions).isNotEmpty();
    }
}
