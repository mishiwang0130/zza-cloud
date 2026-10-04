package com.wxy.rental.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wxy.rental.biz.enums.RentalImageItemTypeEnum;
import com.wxy.rental.biz.mapper.RentalImageMapper;
import com.wxy.rental.biz.po.RentalImage;
import com.wxy.rental.biz.service.RentalFileService;
import com.wxy.rental.biz.service.RentalImageService;
import com.wxy.rental.biz.vo.admin.ImageItemReqVO;
import com.wxy.rental.biz.vo.admin.ImageRespVO;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 房源图片服务实现：覆盖写 + 按需签发地址。
 *
 * <p>覆盖写用物理删除，且与主表写在同一个事务里：图片是主表的一部分，
 * 主表保存失败时图片也不该留下新的一份。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Slf4j
@Service
public class RentalImageServiceImpl implements RentalImageService {

    /** 图片关系 Mapper */
    @Resource
    private RentalImageMapper rentalImageMapper;

    /** 文件服务：校验文件存在并按 ID 换地址 */
    @Resource
    private RentalFileService rentalFileService;

    /**
     * 覆盖写某个对象的图片
     *
     * @param itemType 所属对象类型
     * @param itemId   所属对象 ID
     * @param images   图片列表，可以为 null（不改动）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceImages(RentalImageItemTypeEnum itemType, Long itemId, List<ImageItemReqVO> images) {
        if (images == null) {
            return;
        }
        // 先确认文件真的存在，避免把查不到文件的 file_id 写进关系表
        rentalFileService.validateFilesExist(images.stream().map(ImageItemReqVO::getFileId).toList());
        // 关联表覆盖写必须物理删除：逻辑删除会让旧行继续存在，图片越查越多
        rentalImageMapper.deleteByItemTypeAndItemId(itemType.getValue(), itemId);
        for (ImageItemReqVO image : images) {
            RentalImage po = new RentalImage();
            po.setItemType(itemType.getValue());
            po.setItemId(itemId);
            po.setFileId(image.getFileId());
            // 不传排序号按 0 处理：相同排序号时靠主键升序兜住，也就是提交顺序
            po.setSort(image.getSort() == null ? 0 : image.getSort());
            rentalImageMapper.insert(po);
        }
    }

    /**
     * 查询某个对象的图片，按排序号升序
     *
     * @param itemType 所属对象类型
     * @param itemId   所属对象 ID
     * @return 图片列表（含预签名访问地址），没有图片时返回空列表
     */
    @Override
    public List<ImageRespVO> listImages(RentalImageItemTypeEnum itemType, Long itemId) {
        if (itemId == null) {
            return List.of();
        }
        List<RentalImage> images = rentalImageMapper.selectList(new LambdaQueryWrapper<RentalImage>()
                .eq(RentalImage::getItemType, itemType.getValue())
                .eq(RentalImage::getItemId, itemId)
                .orderByAsc(RentalImage::getSort)
                .orderByAsc(RentalImage::getId));
        if (images.isEmpty()) {
            return List.of();
        }
        Map<Long, String> urlMap = rentalFileService.getFileUrlMap(
                images.stream().map(RentalImage::getFileId).toList());
        List<ImageRespVO> result = new ArrayList<>(images.size());
        for (RentalImage image : images) {
            ImageRespVO vo = new ImageRespVO();
            vo.setId(image.getId());
            vo.setFileId(image.getFileId());
            vo.setSort(image.getSort());
            String url = urlMap.get(image.getFileId());
            if (url == null) {
                // 正常不会发生（写入时已校验文件存在），真出现说明文件记录被删了，记下来便于排查
                log.warn("[listImages][图片文件查不到，返回空地址] itemType={}, itemId={}, fileId={}",
                        itemType.getValue(), itemId, image.getFileId());
            }
            vo.setUrl(url);
            result.add(vo);
        }
        return result;
    }
}
