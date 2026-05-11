package com.raccoon.datacleaning.config;

import com.raccoon.datacleaning.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 动态数据源配置
 * 优先级：数据库配置 > application.yml
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DynamicDataSourceConfig {

    private final DataSourceProperties dataSourceProperties;

    /**
     * 创建数据源
     * 注意：这里只能使用 application.yml 的配置
     * 因为 SystemConfigService 依赖数据源，会造成循环依赖
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        log.info("初始化数据源: {}", dataSourceProperties.getUrl());
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }
}
