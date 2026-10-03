package com.wxy.infra.biz.controller.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.webmvc.exception.GlobalExceptionHandler;
import com.wxy.infra.biz.service.InfraTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * 服务间接口的 Web 层测试：验证路径与参数校验确实来自 client 接口（实现类里不再重复声明）。
 *
 * <p>这两个点是「契约只写一份」的关键前提：接口方法上的 {@code @PostMapping} 与
 * {@code @Validated @RequestBody} 能否被实现类继承。写不动就会静默出现「路径变了 404」
 * 或「校验没生效」——正好是这类重构最容易翻车的地方，所以用 MockMvc 实测一遍。
 *
 * @author wxy
 * @date 2026/10/03
 */
class InfraTokenClientImplWebTest {

    /** 凭证服务 */
    private InfraTokenService infraTokenService;

    /** 被测接口的 MockMvc */
    private MockMvc mockMvc;

    /**
     * 构造独立 MockMvc：只注册被测实现与全局异常处理器
     */
    @BeforeEach
    void setUp() {
        infraTokenService = mock(InfraTokenService.class);
        InfraTokenClientImpl infraTokenClientImpl = new InfraTokenClientImpl();
        ReflectionTestUtils.setField(infraTokenClientImpl, "infraTokenService", infraTokenService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(infraTokenClientImpl)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    /**
     * 路径来自 client 接口，且接口上的参数校验生效：令牌为空时 400
     */
    @Test
    @DisplayName("checkToken：路径与参数校验都继承自 client 接口（空令牌 400）")
    void shouldUseMappingAndValidationFromClientInterface() throws Exception {
        mockMvc.perform(post("/internal-api/auth/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * 校验通过时返回身份三要素
     */
    @Test
    @DisplayName("checkToken：校验通过时返回身份三要素")
    void shouldReturnLoginUser() throws Exception {
        when(infraTokenService.validate("token", UserTypeEnum.ADMIN))
                .thenReturn(new LoginUser(1L, UserTypeEnum.ADMIN.getValue(), "admin"));

        mockMvc.perform(post("/internal-api/auth/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token\",\"userType\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    /**
     * 令牌无效时沿用全局异常处理的约定返回 401
     */
    @Test
    @DisplayName("checkToken：令牌无效时返回 401")
    void shouldReturn401WhenTokenInvalid() throws Exception {
        when(infraTokenService.validate(any(), any())).thenThrow(new UnauthorizedException());

        mockMvc.perform(post("/internal-api/auth/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token\"}"))
                .andExpect(status().isUnauthorized());
    }
}
