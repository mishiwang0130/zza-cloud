package com.wxy.common.webmvc.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * HTTP 层的 Jackson 定制：把 {@code Long} 序列化成字符串。
 *
 * <p><b>为什么需要</b>：数据库主键用雪花算法时是 19 位数字，超出 JavaScript
 * {@code Number} 的安全整数范围（2^53-1），前端直接解析会静默丢精度。
 *
 * <p><b>代价</b>：所有 {@code Long}（包括分页总数 {@code total}）都会变成字符串，
 * 前端按字符串处理即可，需要比较数值时自行转换。
 *
 * <p>只处理 HTTP 层（Jackson）。业务代码里的 JSON 转换统一走 Fastjson2，
 * 那里由调用方自己决定要不要用 {@code JSONWriter.Feature.WriteLongAsString}。
 *
 * @author wxy
 * @date 2026/10/02
 */
public class JacksonConfig implements Jackson2ObjectMapperBuilderCustomizer {

    /**
     * 注册 Long 到字符串的序列化器
     *
     * @param builder Jackson 构造器
     */
    @Override
    public void customize(Jackson2ObjectMapperBuilder builder) {
        builder.serializerByType(Long.class, ToStringSerializer.instance);
        builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
    }
}
