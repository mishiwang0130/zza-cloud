package com.wxy.infra.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxy.common.core.enums.CommonStatusEnum;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.infra.biz.convert.InfraAppUserConvert;
import com.wxy.infra.biz.mapper.InfraAppUserMapper;
import com.wxy.infra.biz.po.InfraAppUser;
import com.wxy.infra.biz.vo.AppUserRespVO;
import com.wxy.infra.biz.vo.admin.AppUserPageReqVO;
import java.util.Arrays;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 用户端用户服务单元测试：按 ID 去重批量查询、后台分页过滤。
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
     * 初始化实体的 TableInfo
     *
     * <p>MyBatis-Plus 把 lambda 方法引用解析成列名依赖实体的 TableInfo，这个初始化平时由启动扫描完成；纯 Mockito 单测里要显式做一次，否则读 wrapper 的 SQL 片段会抛「找不到 lambda 缓存」。
     */
    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), InfraAppUser.class);
    }

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

    /**
     * 分页：关键字同时过滤昵称与手机号，状态条件独立生效
     */
    @Test
    @DisplayName("pageAppUser：关键字匹配昵称或手机号，并按状态过滤")
    void pageAppUserShouldFilterByKeywordAndStatus() {
        AppUserPageReqVO reqVO = new AppUserPageReqVO();
        reqVO.setKeyword("138");
        reqVO.setStatus(CommonStatusEnum.ENABLED.getValue());
        InfraAppUser user = new InfraAppUser();
        user.setId(100L);
        Page<InfraAppUser> page = new Page<>(1, 20);
        page.setRecords(List.of(user));
        page.setTotal(1L);
        when(infraAppUserMapper.selectPage(any(), any())).thenReturn(page);
        AppUserRespVO vo = new AppUserRespVO();
        vo.setId(100L);
        when(infraAppUserConvert.toRespVOList(List.of(user))).thenReturn(List.of(vo));

        PageRespVO<AppUserRespVO> result = appUserService.pageAppUser(reqVO);

        assertThat(result.getRecords()).containsExactly(vo);
        assertThat(result.getTotal()).isEqualTo(1L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<InfraAppUser>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(infraAppUserMapper).selectPage(any(), wrapperCaptor.capture());
        LambdaQueryWrapper<InfraAppUser> wrapper = wrapperCaptor.getValue();
        // 先取一次 SQL 片段：MyBatis-Plus 的查询参数是懒加载写进 paramNameValuePairs 的
        assertThat(wrapper.getSqlSegment())
                .contains("nickname")
                .contains("mobile")
                .contains("status");
        // 昵称走前后模糊、手机号走前缀模糊，MyBatis-Plus 把通配符拼进了参数值里
        assertThat(wrapper.getParamNameValuePairs().values())
                .contains("%138%", "138%", CommonStatusEnum.ENABLED.getValue());
    }
}
