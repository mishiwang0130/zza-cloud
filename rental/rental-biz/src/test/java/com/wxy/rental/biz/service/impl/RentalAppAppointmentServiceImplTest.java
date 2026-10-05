package com.wxy.rental.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.vo.PageReqVO;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import com.wxy.rental.biz.convert.RentalAppAppointmentConvert;
import com.wxy.rental.biz.enums.RentalAppointmentStatusEnum;
import com.wxy.rental.biz.enums.RentalPublishStatusEnum;
import com.wxy.rental.biz.mapper.RentalApartmentMapper;
import com.wxy.rental.biz.mapper.RentalViewAppointmentMapper;
import com.wxy.rental.biz.po.RentalApartment;
import com.wxy.rental.biz.po.RentalViewAppointment;
import com.wxy.rental.biz.vo.app.AppViewAppointmentCreateReqVO;
import com.wxy.rental.biz.vo.app.AppViewAppointmentRespVO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
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
 * 用户端看房预约单元测试：只记预约人 ID、只查自己、只能取消自己的待看房预约。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class RentalAppAppointmentServiceImplTest {

    /** 预约 Mapper */
    @Mock
    private RentalViewAppointmentMapper rentalViewAppointmentMapper;

    /** 公寓 Mapper */
    @Mock
    private RentalApartmentMapper rentalApartmentMapper;

    /** App 预约转换器 */
    @Mock
    private RentalAppAppointmentConvert rentalAppAppointmentConvert;

    /** 被测服务 */
    private RentalAppAppointmentServiceImpl appAppointmentService;

    /**
     * 初始化实体的 TableInfo
     *
     * <p>MyBatis-Plus 把 lambda 方法引用解析成列名依赖实体的 TableInfo，这个初始化平时由启动扫描完成；纯 Mockito 单测里要显式做一次，否则读 wrapper 的 SQL 片段会抛「找不到 lambda 缓存」。
     */
    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                RentalViewAppointment.class);
    }

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        appAppointmentService = new RentalAppAppointmentServiceImpl();
        ReflectionTestUtils.setField(appAppointmentService, "rentalViewAppointmentMapper", rentalViewAppointmentMapper);
        ReflectionTestUtils.setField(appAppointmentService, "rentalApartmentMapper", rentalApartmentMapper);
        ReflectionTestUtils.setField(appAppointmentService, "rentalAppAppointmentConvert", rentalAppAppointmentConvert);
        UserContextHolder.set(new LoginUser(100L, UserTypeEnum.APP.getValue(), "app-user"));
    }

    /**
     * 清理登录上下文
     */
    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    /**
     * 提交：公寓不存在时拒绝
     */
    @Test
    @DisplayName("createAppointment：公寓不存在时拒绝")
    void createShouldRejectUnknownApartment() {
        when(rentalApartmentMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> appAppointmentService.createAppointment(buildCreateReq()))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.APARTMENT_NOT_FOUND.code()));
        verify(rentalViewAppointmentMapper, never()).insert(any(RentalViewAppointment.class));
    }

    /**
     * 提交：未发布（含已下架）的公寓不能预约
     */
    @Test
    @DisplayName("createAppointment：公寓未发布时拒绝")
    void createShouldRejectUnpublishedApartment() {
        RentalApartment apartment = new RentalApartment();
        apartment.setId(1L);
        apartment.setPublishStatus(RentalPublishStatusEnum.UNPUBLISHED.getValue());
        when(rentalApartmentMapper.selectById(1L)).thenReturn(apartment);

        assertThatThrownBy(() -> appAppointmentService.createAppointment(buildCreateReq()))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.APARTMENT_NOT_FOUND.code()));
        verify(rentalViewAppointmentMapper, never()).insert(any(RentalViewAppointment.class));
    }

    /**
     * 提交：预约人取上下文，只落 user_id，初始状态为待看房
     */
    @Test
    @DisplayName("createAppointment：只记预约人 ID 并以待看房状态落库")
    void createShouldSaveUserIdOnly() {
        RentalApartment apartment = new RentalApartment();
        apartment.setId(1L);
        apartment.setPublishStatus(RentalPublishStatusEnum.PUBLISHED.getValue());
        when(rentalApartmentMapper.selectById(1L)).thenReturn(apartment);
        when(rentalViewAppointmentMapper.insert(any(RentalViewAppointment.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, RentalViewAppointment.class).setId(9L);
            return 1;
        });

        Long id = appAppointmentService.createAppointment(buildCreateReq());

        assertThat(id).isEqualTo(9L);
        ArgumentCaptor<RentalViewAppointment> captor = ArgumentCaptor.forClass(RentalViewAppointment.class);
        verify(rentalViewAppointmentMapper).insert(captor.capture());
        RentalViewAppointment saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(100L);
        assertThat(saved.getApartmentId()).isEqualTo(1L);
        assertThat(saved.getAppointmentTime()).isEqualTo(LocalDateTime.of(2026, 10, 6, 10, 0));
        assertThat(saved.getRemark()).isEmpty();
        assertThat(saved.getStatus()).isEqualTo(RentalAppointmentStatusEnum.PENDING.getValue());
    }

    /**
     * 提交：未登录时按未登录处理（接口本应被登录拦截器挡在前面）
     */
    @Test
    @DisplayName("createAppointment：未登录时抛未登录异常")
    void createShouldRejectAnonymous() {
        UserContextHolder.clear();

        assertThatThrownBy(() -> appAppointmentService.createAppointment(buildCreateReq()))
                .isInstanceOf(UnauthorizedException.class);
    }

    /**
     * 提交：服务间调用按入参 userId 落库，不依赖登录上下文
     *
     * <p>AI 工具跑在弹性线程池里，登录上下文是空的，预约人只能由契约显式传进来。
     */
    @Test
    @DisplayName("createAppointment：服务间调用按入参 userId 落库")
    void createShouldUseExplicitUserIdForInternalCall() {
        UserContextHolder.clear();
        RentalApartment apartment = new RentalApartment();
        apartment.setId(1L);
        apartment.setPublishStatus(RentalPublishStatusEnum.PUBLISHED.getValue());
        when(rentalApartmentMapper.selectById(1L)).thenReturn(apartment);

        appAppointmentService.createAppointment(buildCreateReq(), 200L);

        ArgumentCaptor<RentalViewAppointment> captor = ArgumentCaptor.forClass(RentalViewAppointment.class);
        verify(rentalViewAppointmentMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(200L);
    }

    /**
     * 提交：服务间调用没带 userId 时拒绝，不能写出没有主人的预约
     */
    @Test
    @DisplayName("createAppointment：服务间调用未传 userId 时拒绝")
    void createShouldRejectInternalCallWithoutUserId() {
        assertThatThrownBy(() -> appAppointmentService.createAppointment(buildCreateReq(), null))
                .isInstanceOf(UnauthorizedException.class);
        verify(rentalViewAppointmentMapper, never()).insert(any(RentalViewAppointment.class));
    }

    /**
     * 列表：只查自己，并回填公寓名与状态中文名
     */
    @Test
    @DisplayName("pageAppointment：只查自己并回填公寓名与状态中文名")
    void pageShouldFilterSelfAndFillNames() {
        RentalViewAppointment po = new RentalViewAppointment();
        po.setId(9L);
        po.setUserId(100L);
        po.setApartmentId(1L);
        po.setStatus(RentalAppointmentStatusEnum.PENDING.getValue());
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<RentalViewAppointment> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20);
        page.setRecords(List.of(po));
        page.setTotal(1L);
        when(rentalViewAppointmentMapper.selectPage(any(), any())).thenReturn(page);
        AppViewAppointmentRespVO record = new AppViewAppointmentRespVO();
        record.setApartmentId(1L);
        record.setStatus(RentalAppointmentStatusEnum.PENDING.getValue());
        when(rentalAppAppointmentConvert.toRespVOList(List.of(po))).thenReturn(List.of(record));
        RentalApartment apartment = new RentalApartment();
        apartment.setId(1L);
        apartment.setName("文三路公寓");
        when(rentalApartmentMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(apartment));

        PageRespVO<AppViewAppointmentRespVO> result = appAppointmentService.pageAppointment(new PageReqVO());

        assertThat(result.getRecords().get(0).getApartmentName()).isEqualTo("文三路公寓");
        assertThat(result.getRecords().get(0).getStatusName()).isEqualTo("待看房");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<RentalViewAppointment>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(rentalViewAppointmentMapper).selectPage(any(), wrapperCaptor.capture());
        LambdaQueryWrapper<RentalViewAppointment> wrapper = wrapperCaptor.getValue();
        // 先取一次 SQL 片段：MyBatis-Plus 的查询参数是懒加载写进 paramNameValuePairs 的
        assertThat(wrapper.getSqlSegment()).contains("user_id");
        assertThat(wrapper.getParamNameValuePairs().values()).contains(100L);
    }

    /**
     * 取消：别人的预约不能取消
     */
    @Test
    @DisplayName("cancelAppointment：别人的预约不能取消")
    void cancelShouldRejectOtherUserAppointment() {
        RentalViewAppointment po = new RentalViewAppointment();
        po.setId(9L);
        po.setUserId(999L);
        po.setStatus(RentalAppointmentStatusEnum.PENDING.getValue());
        when(rentalViewAppointmentMapper.selectById(9L)).thenReturn(po);

        assertThatThrownBy(() -> appAppointmentService.cancelAppointment(9L))
                .isInstanceOfSatisfying(BizException.class, ex -> assertThat(ex.getCode())
                        .isEqualTo(RentalErrorConstant.APPOINTMENT_CANCEL_FORBIDDEN.code()));
        verify(rentalViewAppointmentMapper, never()).updateById(any(RentalViewAppointment.class));
    }

    /**
     * 取消：已看房的预约不能取消
     */
    @Test
    @DisplayName("cancelAppointment：已看房的预约不能取消")
    void cancelShouldRejectNonPendingAppointment() {
        RentalViewAppointment po = new RentalViewAppointment();
        po.setId(9L);
        po.setUserId(100L);
        po.setStatus(RentalAppointmentStatusEnum.VIEWED.getValue());
        when(rentalViewAppointmentMapper.selectById(9L)).thenReturn(po);

        assertThatThrownBy(() -> appAppointmentService.cancelAppointment(9L))
                .isInstanceOfSatisfying(BizException.class, ex -> assertThat(ex.getCode())
                        .isEqualTo(RentalErrorConstant.APPOINTMENT_CANCEL_FORBIDDEN.code()));
    }

    /**
     * 取消：自己的待看房预约可以取消
     */
    @Test
    @DisplayName("cancelAppointment：自己的待看房预约可以取消")
    void cancelShouldUpdateStatusToCanceled() {
        RentalViewAppointment po = new RentalViewAppointment();
        po.setId(9L);
        po.setUserId(100L);
        po.setStatus(RentalAppointmentStatusEnum.PENDING.getValue());
        when(rentalViewAppointmentMapper.selectById(9L)).thenReturn(po);

        appAppointmentService.cancelAppointment(9L);

        assertThat(po.getStatus()).isEqualTo(RentalAppointmentStatusEnum.CANCELED.getValue());
        verify(rentalViewAppointmentMapper).updateById(po);
    }

    /**
     * 构造预约入参
     *
     * @return 预约入参
     */
    private AppViewAppointmentCreateReqVO buildCreateReq() {
        AppViewAppointmentCreateReqVO reqVO = new AppViewAppointmentCreateReqVO();
        reqVO.setApartmentId(1L);
        reqVO.setAppointmentTime(LocalDateTime.of(2026, 10, 6, 10, 0));
        return reqVO;
    }
}
