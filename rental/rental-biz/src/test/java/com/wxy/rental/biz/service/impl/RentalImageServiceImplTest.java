package com.wxy.rental.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.wxy.rental.biz.enums.RentalImageItemTypeEnum;
import com.wxy.rental.biz.mapper.RentalImageMapper;
import com.wxy.rental.biz.po.RentalImage;
import com.wxy.rental.biz.service.RentalFileService;
import com.wxy.rental.biz.vo.admin.ImageItemReqVO;
import com.wxy.rental.biz.vo.admin.ImageRespVO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 房源图片服务单元测试：覆盖写、入参为 null 不改动、详情回填地址。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class RentalImageServiceImplTest {

    /** 图片 Mapper */
    @Mock
    private RentalImageMapper rentalImageMapper;

    /** 文件服务 */
    @Mock
    private RentalFileService rentalFileService;

    /** 被测服务 */
    private RentalImageServiceImpl imageService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        imageService = new RentalImageServiceImpl();
        ReflectionTestUtils.setField(imageService, "rentalImageMapper", rentalImageMapper);
        ReflectionTestUtils.setField(imageService, "rentalFileService", rentalFileService);
    }

    /**
     * 入参为 null 表示本次没提交图片，不动库里的数据
     */
    @Test
    @DisplayName("replaceImages：入参为 null 时不改动画册")
    void replaceImagesShouldSkipWhenNull() {
        imageService.replaceImages(RentalImageItemTypeEnum.APARTMENT, 10L, null);

        verifyNoInteractions(rentalImageMapper);
        verifyNoInteractions(rentalFileService);
    }

    /**
     * 覆盖写：先校验文件、再物理删除旧图、最后按顺序插入
     */
    @Test
    @DisplayName("replaceImages：校验文件后物理删除旧图并整体插入")
    void replaceImagesShouldReplaceAll() {
        imageService.replaceImages(RentalImageItemTypeEnum.APARTMENT, 10L, List.of(
                buildImageReq(1L, null),
                buildImageReq(2L, 5)));

        verify(rentalFileService).validateFilesExist(List.of(1L, 2L));
        verify(rentalImageMapper).deleteByItemTypeAndItemId(RentalImageItemTypeEnum.APARTMENT.getValue(), 10L);
        ArgumentCaptor<RentalImage> captor = ArgumentCaptor.forClass(RentalImage.class);
        verify(rentalImageMapper, times(2)).insert(captor.capture());
        List<RentalImage> saved = captor.getAllValues();
        assertThat(saved).extracting(RentalImage::getFileId).containsExactly(1L, 2L);
        assertThat(saved).extracting(RentalImage::getSort).containsExactly(0, 5);
    }

    /**
     * 空列表表示显式清空：仍然要物理删除，但不再插入
     */
    @Test
    @DisplayName("replaceImages：空列表表示清空图片")
    void replaceImagesShouldClearWhenEmpty() {
        imageService.replaceImages(RentalImageItemTypeEnum.ROOM, 20L, List.of());

        verify(rentalImageMapper).deleteByItemTypeAndItemId(RentalImageItemTypeEnum.ROOM.getValue(), 20L);
        verify(rentalImageMapper, never()).insert(any(RentalImage.class));
    }

    /**
     * 详情回填：图片按排序号返回，并带上按 fileId 换来的地址
     */
    @Test
    @DisplayName("listImages：回填预签名地址")
    void listImagesShouldFillUrl() {
        RentalImage po = new RentalImage();
        po.setId(7L);
        po.setItemType(RentalImageItemTypeEnum.APARTMENT.getValue());
        po.setItemId(10L);
        po.setFileId(3L);
        po.setSort(1);
        when(rentalImageMapper.selectList(any())).thenReturn(List.of(po));
        when(rentalFileService.getFileUrlMap(List.of(3L))).thenReturn(Map.of(3L, "http://minio/3"));

        List<ImageRespVO> images = imageService.listImages(RentalImageItemTypeEnum.APARTMENT, 10L);

        assertThat(images).hasSize(1);
        assertThat(images.get(0).getId()).isEqualTo(7L);
        assertThat(images.get(0).getUrl()).isEqualTo("http://minio/3");
    }

    /**
     * 详情查询：没有图片时不调文件服务
     */
    @Test
    @DisplayName("listImages：没有图片时返回空列表")
    void listImagesShouldReturnEmptyWhenNoImage() {
        when(rentalImageMapper.selectList(any())).thenReturn(List.of());

        assertThat(imageService.listImages(RentalImageItemTypeEnum.APARTMENT, 10L)).isEmpty();
        verifyNoInteractions(rentalFileService);
    }

    /**
     * 构造图片提交项
     *
     * @param fileId 文件 ID
     * @param sort   排序号
     * @return 提交项
     */
    private ImageItemReqVO buildImageReq(Long fileId, Integer sort) {
        ImageItemReqVO reqVO = new ImageItemReqVO();
        reqVO.setFileId(fileId);
        reqVO.setSort(sort);
        return reqVO;
    }
}
