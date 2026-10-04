package com.wxy.infra.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.wxy.infra.biz.convert.InfraAppUserConvert;
import com.wxy.infra.biz.mapper.InfraAppUserMapper;
import com.wxy.infra.biz.po.InfraAppUser;
import com.wxy.infra.biz.vo.AppUserRespVO;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 用户端用户服务单元测试：按 ID 去重批量查询。
 *
 * @author wxy
 * @date 2026/10/05
 */
@ExtendWith(MockitoExtension.class)
class InfraAppUserServiceImplTest {

    /** 用户端用户 Mapper */
    @Mock
    private InfraAppUserMapper infraAppUserMapper;

    /** 用户端用户转换器 */
    @Mock
    private InfraAppUserConvert infraAppUserConvert;

    /** 被测服务 */
    private InfraAppUserServiceImpl appUserService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        appUserService = new InfraAppUserServiceImpl();
        ReflectionTestUtils.setField(appUserService, "infraAppUserMapper", infraAppUserMapper);
        ReflectionTestUtils.setField(appUserService, "infraAppUserConvert", infraAppUserConvert);
    }

    /**
     * 重复 ID 与 null 先去重，再按 ID 查库并转成返回体
     */
    @Test
    @DisplayName("listByIds：去重后批量查询并转换")
    void listByIdsShouldDistinctAndConvert() {
        InfraAppUser user = new InfraAppUser();
        user.setId(100L);
        user.setNickname("小张");
        user.setMobile("13800001111");
        AppUserRespVO vo = new AppUserRespVO();
        vo.setId(100L);
        when(infraAppUserMapper.selectBatchIds(List.of(100L))).thenReturn(List.of(user));
        when(infraAppUserConvert.toRespVOList(List.of(user))).thenReturn(List.of(vo));

        List<AppUserRespVO> result = appUserService.listByIds(Arrays.asList(100L, 100L, null));

        assertThat(result).containsExactly(vo);
    }

    /**
     * 空入参直接返回空列表，不碰数据库
     */
    @Test
    @DisplayName("listByIds：入参为空时返回空列表")
    void listByIdsShouldReturnEmptyForBlankInput() {
        assertThat(appUserService.listByIds(null)).isEmpty();
        assertThat(appUserService.listByIds(List.of())).isEmpty();
        verifyNoInteractions(infraAppUserMapper);
    }
}
