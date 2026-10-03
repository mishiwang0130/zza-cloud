package com.wxy.infra.biz.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.webmvc.config.WebProperties;
import com.wxy.infra.biz.config.WebConfig.EndApi;
import com.wxy.infra.biz.service.InfraPermissionService;
import com.wxy.infra.biz.service.InfraTokenService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Web 配置单元测试：锁定「端前缀与端类型一一对应、显式配置」这一约定。
 *
 * <p>重点防的是兜底推断回归：曾经写成「不是 admin 前缀就当 app 端」，
 * 那样以后新增端（内部接口、开放平台等）会被静默按 app 端校验，属于安全默认值错误。
 * 现在有配置才有校验、没有配置就没有兜底，所以这里把这张配置表锁死。
 *
 * @author wxy
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class WebConfigTest {

    /** 凭证服务 */
    @Mock
    private InfraTokenService infraTokenService;

    /** 权限服务 */
    @Mock
    private InfraPermissionService infraPermissionService;

    /** 被测配置 */
    private WebConfig webConfig;

    /**
     * 装配被测配置
     */
    @BeforeEach
    void setUp() {
        webConfig = new WebConfig();
        ReflectionTestUtils.setField(webConfig, "infraTokenService", infraTokenService);
        ReflectionTestUtils.setField(webConfig, "infraPermissionService", infraPermissionService);
        ReflectionTestUtils.setField(webConfig, "webProperties", new WebProperties());
    }

    /**
     * 每个端前缀都绑定到明确的端类型，不依赖请求路径推断
     */
    @Test
    @DisplayName("endApis：端前缀与端类型一一对应地显式配置")
    void shouldBindUserTypePerEndPrefix() {
        List<EndApi> endApis = webConfig.endApis();

        assertThat(endApis)
                .extracting(EndApi::apiPrefix, EndApi::userType)
                .containsExactly(
                        tuple("/admin-api", UserTypeEnum.ADMIN),
                        tuple("/app-api", UserTypeEnum.APP));
    }

    /**
     * 端前缀与端类型都不能重复：重复说明同一端被配了两次，注册出的拦截器会互相覆盖排除规则
     */
    @Test
    @DisplayName("endApis：端前缀与端类型都不重复")
    void shouldNotDuplicateEndPrefix() {
        List<EndApi> endApis = webConfig.endApis();

        assertThat(endApis).extracting(EndApi::apiPrefix).doesNotHaveDuplicates();
        assertThat(endApis).extracting(EndApi::userType).doesNotHaveDuplicates();
    }
}
