package com.wxy.common.redis.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Redis 各数据类型读写测试：用 mock 的 StringRedisTemplate 验证 JSON 存取格式与类型转换。
 *
 * <p>测试不连接真实 Redis，符合「测试不得依赖外部环境」的规范。
 *
 * @author wxy
 * @date 2026/10/02
 */
@ExtendWith(MockitoExtension.class)
class RedisUtilTest {

    /** 字符串 Redis 客户端 */
    @Mock
    private StringRedisTemplate stringRedisTemplate;

    /** String 类型操作 */
    @Mock
    private ValueOperations<String, String> valueOperations;

    /** Hash 类型操作 */
    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    /** List 类型操作 */
    @Mock
    private ListOperations<String, String> listOperations;

    /** Set 类型操作 */
    @Mock
    private SetOperations<String, String> setOperations;

    /** ZSet 类型操作 */
    @Mock
    private ZSetOperations<String, String> zSetOperations;

    /** 被测 Redis 工具 */
    private RedisUtil redisUtil;

    /**
     * 构造被测对象并注入 mock 客户端
     */
    @BeforeEach
    void setUp() {
        redisUtil = new RedisUtil();
        ReflectionTestUtils.setField(redisUtil, "stringRedisTemplate", stringRedisTemplate);
    }

    @Test
    @DisplayName("写入 String 时按 JSON 字符串存储，并带上过期时间")
    void shouldSetStringWithTimeoutAndUnit() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        redisUtil.set("zza:test:string", "value", 10L, TimeUnit.SECONDS);

        verify(valueOperations).set("zza:test:string", "\"value\"", 10L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("读取 String 时把 JSON 字符串还原成原值")
    void shouldGetString() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("zza:test:string")).thenReturn("\"value\"");

        assertThat(redisUtil.get("zza:test:string", String.class)).isEqualTo("value");
    }

    @Test
    @DisplayName("读取对象时按目标类型反序列化")
    void shouldGetObject() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("zza:test:object")).thenReturn("{\"name\":\"张三\"}");

        TestObject value = redisUtil.get("zza:test:object", TestObject.class);

        assertThat(value).isNotNull();
        assertThat(value.getName()).isEqualTo("张三");
    }

    @Test
    @DisplayName("key 不存在时返回 null")
    void shouldReturnNullWhenKeyAbsent() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        assertThat(redisUtil.get("zza:test:missing", String.class)).isNull();
    }

    @Test
    @DisplayName("读取 Hash 字段")
    void shouldGetHashValue() {
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("zza:test:hash", "field")).thenReturn("\"value\"");

        assertThat(redisUtil.getHash("zza:test:hash", "field", String.class)).isEqualTo("value");
    }

    @Test
    @DisplayName("读取 List 全部元素")
    void shouldGetListValue() {
        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range("zza:test:list", 0, -1)).thenReturn(List.of("\"a\"", "\"b\""));

        assertThat(redisUtil.getList("zza:test:list", String.class)).containsExactly("a", "b");
    }

    @Test
    @DisplayName("读取 Set 全部元素")
    void shouldGetSetValue() {
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("zza:test:set")).thenReturn(Set.of("\"a\"", "\"b\""));

        assertThat(redisUtil.getSet("zza:test:set", String.class)).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    @DisplayName("读取 ZSet 指定排名范围")
    void shouldGetZSetRange() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.range("zza:test:zset", 0, -1)).thenReturn(Set.of("\"a\"", "\"b\""));

        assertThat(redisUtil.getZSetRange("zza:test:zset", 0, -1, String.class))
                .containsExactlyInAnyOrder("a", "b");
    }

    /**
     * 测试用对象：验证对象与 JSON 字符串的往返转换
     *
     * @author wxy
     * @date 2026/10/02
     */
    static class TestObject {

        /** 名称 */
        private String name;

        /**
         * 获取名称
         *
         * @return 名称
         */
        public String getName() {
            return name;
        }

        /**
         * 设置名称
         *
         * @param name 名称
         */
        public void setName(String name) {
            this.name = name;
        }
    }
}
