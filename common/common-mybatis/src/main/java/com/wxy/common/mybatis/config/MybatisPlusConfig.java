package com.wxy.common.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.wxy.common.mybatis.handler.AuditMetaObjectHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * MyBatis-Plus 公共配置：分页插件、防全表更新删除插件、审计字段填充器。
 *
 * <p>分页插件统一在这里配置，各服务不要再自己加 {@code MybatisPlusInterceptor}，
 * 否则同一个应用里会出现多个分页拦截器导致分页参数被重复处理。
 *
 * <p>Mapper 扫描（{@code @MapperScan}）由各服务的启动类声明，本模块不负责，
 * 因为扫描范围必须落在各服务自己的 {@code mapper} 包上。
 *
 * @author wxy
 * @date 2026/10/02
 */
@AutoConfiguration
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 插件链
     *
     * @return 插件拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件：数据库固定为 MySQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 防全表更新与删除：拦截没有 where 条件的 update/delete，避免误操作清库
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }

    /**
     * 注册审计字段自动填充器
     *
     * @return 审计字段填充器
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditMetaObjectHandler auditMetaObjectHandler() {
        return new AuditMetaObjectHandler();
    }
}
