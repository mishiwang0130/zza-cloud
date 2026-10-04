package com.wxy.rental.biz.service;

import com.wxy.infra.api.dto.AppUserSimpleDTO;
import java.util.Collection;
import java.util.Map;

/**
 * 用户档案服务：把 rental 库里存的 {@code userId} 换成昵称与手机号。
 *
 * <p>rental 的预约、租约表都只存 {@code userId}，不落姓名与手机：联系方式属于用户档案，
 * 用户改了资料就该以最新为准，快照一份下来既会过期，也把同一份敏感信息复制到了业务库里。
 *
 * <p>映射的键是用户 ID，值为 infra 发布的 {@link AppUserSimpleDTO}（只含 ID、昵称、手机号）：
 * 调用方一次批量拿到本页要展示的人，不需要逐行发起远程调用。
 *
 * @author wxy
 * @date 2026/10/05
 */
public interface RentalAppUserService {

    /**
     * 按 ID 批量查询用户档案
     *
     * @param userIds 用户 ID 集合，可以为 null
     * @return 用户 ID 到用户档案的映射；查不到或已删除的用户不放进映射
     */
    Map<Long, AppUserSimpleDTO> getAppUserMap(Collection<Long> userIds);
}
