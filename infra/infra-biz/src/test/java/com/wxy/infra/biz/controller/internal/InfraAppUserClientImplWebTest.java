package com.wxy.infra.biz.controller.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wxy.infra.api.dto.AppUserSimpleDTO;
import com.wxy.infra.biz.convert.InfraAppUserConvert;
import com.wxy.infra.biz.service.InfraAppUserService;
import com.wxy.infra.biz.vo.AppUserRespVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 用户端用户服务间接口的 Web 层测试：验证路径与参数绑定确实来自 client 接口（实现类里不再重复声明）。
 *
 * <p>路径一旦没继承过来，调用方会稳定收到 404，而单测直接调方法却完全正常，
 * 所以这里用 MockMvc 实测一遍。
 *
 * @author wxy
 * @date 2026/10/05
 */
class InfraAppUserClientImplWebTest {

    /** 用户端用户服务 */
    private InfraAppUserService infraAppUserService;

    /** 用户端用户转换器 */
    private InfraAppUserConvert infraAppUserConvert;

    /** 被测接口的 MockMvc */
    private MockMvc mockMvc;

    /**
     * 构造独立 MockMvc：只注册被测实现
     */
    @BeforeEach
    void setUp() {
        infraAppUserService = mock(InfraAppUserService.class);
        infraAppUserConvert = mock(InfraAppUserConvert.class);
        InfraAppUserClientImpl clientImpl = new InfraAppUserClientImpl();
        ReflectionTestUtils.setField(clientImpl, "infraAppUserService", infraAppUserService);
        ReflectionTestUtils.setField(clientImpl, "infraAppUserConvert", infraAppUserConvert);
        mockMvc = MockMvcBuilders.standaloneSetup(clientImpl).build();
    }

    /**
     * 路径来自 client 接口，按 ID 查询返回昵称与手机号
     */
    @Test
    @DisplayName("listByIds：路径继承自 client 接口并返回用户档案")
    void shouldUseMappingFromClientInterface() throws Exception {
        AppUserRespVO vo = new AppUserRespVO();
        vo.setId(100L);
        when(infraAppUserService.listByIds(List.of(100L))).thenReturn(List.of(vo));
        when(infraAppUserConvert.toDTOList(List.of(vo)))
                .thenReturn(List.of(new AppUserSimpleDTO(100L, "小张", "13800001111")));

        mockMvc.perform(get("/internal-api/app-user/listByIds").param("ids", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(100))
                .andExpect(jsonPath("$.data[0].nickname").value("小张"))
                .andExpect(jsonPath("$.data[0].mobile").value("13800001111"));
    }

    /**
     * 参数绑定同样来自 client 接口：不传 ids 时按缺参返回 400
     */
    @Test
    @DisplayName("listByIds：不传 ids 时返回 400")
    void shouldRejectMissingIds() throws Exception {
        mockMvc.perform(get("/internal-api/app-user/listByIds"))
                .andExpect(status().isBadRequest());
    }
}
