package com.wxy.common.redis.util;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 常用操作：业务代码统一通过它访问缓存，不直接使用 {@code StringRedisTemplate}。
 *
 * <p>统一用 {@code StringRedisTemplate} 保存 JSON 字符串，调用方无需感知序列化细节，
 * 缓存内容用 {@code redis-cli} 就能直接看懂；JSON 转换统一用 Fastjson2，与项目整体的 JSON 规范一致。
 *
 * <p>按 Redis 数据类型提供独立方法：String、Hash、List、Set、ZSet。
 * 过期时间参数为「数值 + 时间单位」，规范要求除确实不需要过期的数据外，所有 key 都必须设置过期时间。
 *
 * @author wxy
 * @date 2026/10/02
 */
public class RedisUtil {

    /** SCAN 单次迭代建议元素数量，仅作为服务端遍历提示，不保证单次返回数量 */
    private static final long SCAN_COUNT = 200L;

    /** 字符串 Redis 客户端，由 Spring Boot 自动配置提供 */
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 写入 String 类型缓存，不设置过期时间
     *
     * <p>仅用于确实不需要过期的数据（如固定字典），使用前需确认符合缓存规范。
     *
     * @param key   缓存 key
     * @param value 缓存值
     */
    public void set(String key, Object value) {
        stringRedisTemplate.opsForValue().set(key, toJson(value));
    }

    /**
     * 写入带过期时间的 String 类型缓存
     *
     * @param key     缓存 key
     * @param value   缓存值
     * @param timeout 过期时间数值
     * @param unit    过期时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, toJson(value), timeout, unit);
    }

    /**
     * 获取 String 类型缓存
     *
     * @param key   缓存 key
     * @param clazz 缓存值类型
     * @param <T>   缓存值泛型
     * @return 缓存值，不存在时返回 null
     */
    public <T> T get(String key, Class<T> clazz) {
        return fromJson(stringRedisTemplate.opsForValue().get(key), clazz);
    }

    /**
     * 仅当缓存不存在时写入，用于防重复提交与分布式锁的初次占位
     *
     * @param key     缓存 key
     * @param value   缓存值
     * @param timeout 过期时间数值
     * @param unit    过期时间单位
     * @return true 表示写入成功，false 表示 key 已存在
     */
    public Boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        return stringRedisTemplate.opsForValue().setIfAbsent(key, toJson(value), timeout, unit);
    }

    /**
     * 自增计数，用于接口限流、失败次数统计
     *
     * @param key   缓存 key
     * @param delta 增量
     * @return 自增后的值
     */
    public Long increment(String key, long delta) {
        return stringRedisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 删除缓存
     *
     * @param key 缓存 key
     * @return 是否删除成功
     */
    public Boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }

    /**
     * 设置缓存过期时间
     *
     * @param key     缓存 key
     * @param timeout 过期时间数值
     * @param unit    过期时间单位
     * @return 是否设置成功
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return stringRedisTemplate.expire(key, timeout, unit);
    }

    /**
     * 判断缓存 key 是否存在
     *
     * @param key 缓存 key
     * @return 是否存在
     */
    public Boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    /**
     * 按通配模式扫描 key
     *
     * <p>使用 SCAN 而非 KEYS，避免在大键空间上阻塞 Redis；仅建议在键空间可控的场景使用，
     * 例如按服务前缀批量刷新缓存过期时间。
     *
     * @param pattern 通配模式，例如 {@code zza:user:token:*}
     * @return 命中的 key 集合
     */
    public Set<String> scanKeys(String pattern) {
        Set<String> keys = new LinkedHashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(SCAN_COUNT).build();
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            cursor.forEachRemaining(keys::add);
        }
        return keys;
    }

    /**
     * 写入 Hash 类型的一个字段
     *
     * @param key     缓存 key
     * @param hashKey Hash 字段名
     * @param value   Hash 字段值
     */
    public void setHash(String key, String hashKey, Object value) {
        stringRedisTemplate.opsForHash().put(key, hashKey, toJson(value));
    }

    /**
     * 获取 Hash 类型的一个字段
     *
     * @param key     缓存 key
     * @param hashKey Hash 字段名
     * @param clazz   字段值类型
     * @param <T>     字段值泛型
     * @return 字段值，不存在时返回 null
     */
    public <T> T getHash(String key, String hashKey, Class<T> clazz) {
        Object value = stringRedisTemplate.opsForHash().get(key, hashKey);
        return value == null ? null : fromJson(value.toString(), clazz);
    }

    /**
     * 获取 Hash 类型全部字段
     *
     * @param key   缓存 key
     * @param clazz 字段值类型
     * @param <T>   字段值泛型
     * @return Hash 字段映射，保持 Redis 返回顺序
     */
    public <T> Map<String, T> getHashAll(String key, Class<T> clazz) {
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        Map<String, T> result = new LinkedHashMap<>(entries.size());
        entries.forEach((hashKey, value) -> result.put(hashKey.toString(), fromJson(value.toString(), clazz)));
        return result;
    }

    /**
     * 判断 Hash 类型字段是否存在
     *
     * @param key     缓存 key
     * @param hashKey Hash 字段名
     * @return 是否存在
     */
    public Boolean hasHashKey(String key, String hashKey) {
        return stringRedisTemplate.opsForHash().hasKey(key, hashKey);
    }

    /**
     * 删除 Hash 类型字段
     *
     * @param key      缓存 key
     * @param hashKeys Hash 字段名
     * @return 删除数量
     */
    public Long deleteHash(String key, String... hashKeys) {
        if (hashKeys == null || hashKeys.length == 0) {
            return 0L;
        }
        return stringRedisTemplate.opsForHash().delete(key, (Object[]) hashKeys);
    }

    /**
     * 覆盖写入 List 类型缓存，不设置过期时间
     *
     * @param key    缓存 key
     * @param values 列表值
     */
    public void setList(String key, Collection<?> values) {
        delete(key);
        if (values == null || values.isEmpty()) {
            return;
        }
        stringRedisTemplate.opsForList().rightPushAll(key, toJsonCollection(values));
    }

    /**
     * 覆盖写入带过期时间的 List 类型缓存
     *
     * @param key     缓存 key
     * @param values  列表值
     * @param timeout 过期时间数值
     * @param unit    过期时间单位
     */
    public void setList(String key, Collection<?> values, long timeout, TimeUnit unit) {
        setList(key, values);
        expire(key, timeout, unit);
    }

    /**
     * 获取 List 类型缓存
     *
     * @param key   缓存 key
     * @param clazz 元素类型
     * @param <T>   元素泛型
     * @return 列表值，不存在时返回空列表
     */
    public <T> List<T> getList(String key, Class<T> clazz) {
        List<String> values = stringRedisTemplate.opsForList().range(key, 0, -1);
        if (values == null) {
            return List.of();
        }
        return values.stream().map(value -> fromJson(value, clazz)).collect(Collectors.toList());
    }

    /**
     * 从右侧追加一个 List 元素
     *
     * @param key   缓存 key
     * @param value 元素值
     * @return 列表长度
     */
    public Long pushList(String key, Object value) {
        return stringRedisTemplate.opsForList().rightPush(key, toJson(value));
    }

    /**
     * 获取 List 长度
     *
     * @param key 缓存 key
     * @return 列表长度
     */
    public Long sizeList(String key) {
        return stringRedisTemplate.opsForList().size(key);
    }

    /**
     * 向 Set 类型缓存新增元素
     *
     * @param key    缓存 key
     * @param values 集合值
     * @return 新增元素数量
     */
    public Long addSet(String key, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }
        return stringRedisTemplate.opsForSet().add(key, toJsonCollection(values).toArray(String[]::new));
    }

    /**
     * 获取 Set 类型缓存
     *
     * @param key   缓存 key
     * @param clazz 元素类型
     * @param <T>   元素泛型
     * @return 集合值，不存在时返回空集合
     */
    public <T> Set<T> getSet(String key, Class<T> clazz) {
        Set<String> values = stringRedisTemplate.opsForSet().members(key);
        if (values == null) {
            return Set.of();
        }
        return values.stream()
                .map(value -> fromJson(value, clazz))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 判断 Set 成员是否存在
     *
     * @param key   缓存 key
     * @param value 成员值
     * @return 是否存在
     */
    public Boolean hasSetMember(String key, Object value) {
        return stringRedisTemplate.opsForSet().isMember(key, toJson(value));
    }

    /**
     * 删除 Set 成员
     *
     * @param key    缓存 key
     * @param values 成员值
     * @return 删除数量
     */
    public Long removeSet(String key, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }
        return stringRedisTemplate.opsForSet().remove(key, toJsonCollection(values).toArray());
    }

    /**
     * 获取 Set 元素数量
     *
     * @param key 缓存 key
     * @return 元素数量
     */
    public Long sizeSet(String key) {
        return stringRedisTemplate.opsForSet().size(key);
    }

    /**
     * 向 ZSet 类型缓存写入成员
     *
     * @param key   缓存 key
     * @param value 成员值
     * @param score 分值
     * @return true 表示新增成功
     */
    public Boolean addZSet(String key, Object value, double score) {
        return stringRedisTemplate.opsForZSet().add(key, toJson(value), score);
    }

    /**
     * 获取 ZSet 指定排名范围
     *
     * @param key   缓存 key
     * @param start 起始排名，从 0 开始
     * @param end   结束排名，-1 表示最后一位
     * @param clazz 成员类型
     * @param <T>   成员泛型
     * @return 成员集合，按分值升序，不存在时返回空集合
     */
    public <T> Set<T> getZSetRange(String key, long start, long end, Class<T> clazz) {
        Set<String> values = stringRedisTemplate.opsForZSet().range(key, start, end);
        if (values == null) {
            return Set.of();
        }
        return values.stream()
                .map(value -> fromJson(value, clazz))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 获取 ZSet 成员分值
     *
     * @param key   缓存 key
     * @param value 成员值
     * @return 分值，成员不存在时返回 null
     */
    public Double getZSetScore(String key, Object value) {
        return stringRedisTemplate.opsForZSet().score(key, toJson(value));
    }

    /**
     * 删除 ZSet 成员
     *
     * @param key    缓存 key
     * @param values 成员值
     * @return 删除数量
     */
    public Long removeZSet(String key, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }
        return stringRedisTemplate.opsForZSet().remove(key, toJsonCollection(values).toArray());
    }

    /**
     * 获取 ZSet 成员数量
     *
     * @param key 缓存 key
     * @return 成员数量
     */
    public Long sizeZSet(String key) {
        return stringRedisTemplate.opsForZSet().size(key);
    }

    /**
     * 把集合元素逐个转成 JSON 字符串
     *
     * @param values 集合元素
     * @return JSON 字符串列表，入参为 null 时返回空列表
     */
    private List<String> toJsonCollection(Collection<?> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().map(this::toJson).collect(Collectors.toList());
    }

    /**
     * 对象转 JSON 字符串
     *
     * @param value 对象值
     * @return JSON 字符串
     */
    private String toJson(Object value) {
        return JSON.toJSONString(value);
    }

    /**
     * JSON 字符串转对象
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   目标泛型
     * @return 反序列化结果，入参为 null 时返回 null
     */
    private <T> T fromJson(String json, Class<T> clazz) {
        if (json == null) {
            return null;
        }
        return JSON.parseObject(json, clazz);
    }
}
