package com.wxy.rental.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.result.Result;
import com.wxy.infra.api.client.InfraAppUserClient;
import com.wxy.infra.api.dto.AppUserSimpleDTO;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 用户档案服务单元测试：批量换档案与远程异常口径。
 *
 * @author wxy
 * @date 2026/10/05
 */
@ExtendWith(MockitoExtension.class)
class RentalAppUserServiceImplTest {

    /** infra 用户端用户接口 */
    @Mock
    private InfraAppUserClient infraAppUserClient;

    /** 被测服务 */
    private RentalAppUserServiceImpl appUserService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        appUserService = new RentalAppUserServiceImpl();
        ReflectionTestUtils.setField(appUserService, "infraAppUserClient", infraAppUserClient);
    }

    /**
     * 重复 ID 与 null 先去重，再按 ID 换成用户档案映射
     */
    @Test
    @DisplayName("getAppUserMap：去重后按 ID 换取用户档案")
    void getAppUserMapShouldReturnUserMap() {
        AppUserSimpleDTO user = new AppUserSimpleDTO(100L, "小张", "13800001111");
        when(infraAppUserClient.listByIds(List.of(100L))).thenReturn(Result.success(List.of(user)));

        Map<Long, AppUserSimpleDTO> userMap = appUserService.getAppUserMap(Arrays.asList(100L, 100L, null));

        assertThat(userMap).containsEntry(100L, user);
    }

    /**
     * 空入参直接返回空映射，不调 infra
     */
    @Test
    @DisplayName("getAppUserMap：入参为空时返回空映射")
    void getAppUserMapShouldReturnEmptyForBlankInput() {
        assertThat(appUserService.getAppUserMap(null)).isEmpty();
        assertThat(appUserService.getAppUserMap(List.of())).isEmpty();
        verifyNoInteractions(infraAppUserClient);
    }

    /**
     * 调用本身异常（连接失败、序列化失败等）统一换成「依赖的基础服务调用失败」
     */
    @Test
    @DisplayName("getAppUserMap：调用异常时换成 REMOTE_SERVICE_ERROR")
    void getAppUserMapShouldWrapUnexpectedException() {
        when(infraAppUserClient.listByIds(List.of(100L))).thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> appUserService.getAppUserMap(List.of(100L)))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.REMOTE_SERVICE_ERROR.code()));
    }
}
