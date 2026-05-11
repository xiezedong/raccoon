package com.raccoon.datacleaning.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 目标数据库服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TargetDatabaseService {

    private final SystemConfigService systemConfigService;

    /**
     * 测试数据库连接
     */
    public boolean testConnection(String host, String port, String database, String username, String password) {
        String url = buildJdbcUrl(host, port, database);
        
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            log.info("数据库连接测试成功: {}", url);
            return true;
        } catch (Exception e) {
            log.error("数据库连接测试失败: {}", url, e);
            return false;
        }
    }

    /**
     * 获取数据库列表
     */
    public List<String> getDatabaseList(String host, String port, String username, String password) {
        List<String> databases = new ArrayList<>();
        
        // 连接到 postgres 默认数据库来查询所有数据库
        String url = buildJdbcUrl(host, port, "postgres");
        
        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT datname FROM pg_database WHERE datistemplate = false ORDER BY datname")) {
            
            while (rs.next()) {
                databases.add(rs.getString("datname"));
            }
            
            log.info("获取数据库列表成功，共 {} 个数据库", databases.size());
            
        } catch (Exception e) {
            log.error("获取数据库列表失败", e);
            throw new RuntimeException("获取数据库列表失败: " + e.getMessage());
        }
        
        return databases;
    }

    /**
     * 获取当前配置的目标数据库连接
     */
    public Connection getTargetConnection() {
        try {
            String host = systemConfigService.getConfig("target.db.host", "localhost");
            String port = systemConfigService.getConfig("target.db.port", "5432");
            String database = systemConfigService.getConfig("target.db.database", "raccoon_db");
            String username = systemConfigService.getConfig("target.db.username", "postgres");
            String password = systemConfigService.getConfig("target.db.password", "postgres");
            
            String url = buildJdbcUrl(host, port, database);
            
            return DriverManager.getConnection(url, username, password);
            
        } catch (Exception e) {
            log.error("获取目标数据库连接失败", e);
            throw new RuntimeException("获取目标数据库连接失败: " + e.getMessage());
        }
    }

    /**
     * 构建 JDBC URL
     */
    private String buildJdbcUrl(String host, String port, String database) {
        return String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
    }
}
