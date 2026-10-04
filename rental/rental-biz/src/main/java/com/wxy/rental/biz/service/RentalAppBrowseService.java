package com.wxy.rental.biz.service;

import com.wxy.common.core.vo.PageReqVO;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.biz.vo.app.AppRoomBrowseRespVO;

/**
 * 用户端浏览记录服务：只查自己的浏览流水。
 *
 * <p>没有写入接口：记录由 App 房间详情接口通过 MQ 异步补写，避免前端为了「记一次浏览」多调一个接口、也避免它被漏调。
 *
 * @author wxy
 * @date 2026/10/04
 */
public interface RentalAppBrowseService {

    /**
     * 分页查询我的浏览记录，按浏览时间倒序
     *
     * @param reqVO 分页入参
     * @return 分页结果
     */
    PageRespVO<AppRoomBrowseRespVO> pageBrowse(PageReqVO reqVO);
}
