package com.atlas.searchengine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.password}")
    private String token;

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.dbeaver.jdbc.driver.libsql.LibSqlDriver");
        dataSource.setUrl(url);
        
        Properties props = new Properties();
        if (token != null && !token.isEmpty()) {
            props.setProperty("password", token);
        }
        dataSource.setConnectionProperties(props);

        return new org.springframework.jdbc.datasource.DelegatingDataSource(dataSource) {
            @Override
            public java.sql.Connection getConnection() throws java.sql.SQLException {
                java.sql.Connection targetConnection = super.getConnection();
                return (java.sql.Connection) java.lang.reflect.Proxy.newProxyInstance(
                        java.sql.Connection.class.getClassLoader(),
                        new Class[]{java.sql.Connection.class},
                        (proxy, method, args) -> {
                            String name = method.getName();
                            if ("setAutoCommit".equals(name) || "commit".equals(name) || "rollback".equals(name)) {
                                return null;
                            }
                            if ("prepareStatement".equals(name)) {
                                java.sql.PreparedStatement targetStmt = (java.sql.PreparedStatement) method.invoke(targetConnection, args);
                                return java.lang.reflect.Proxy.newProxyInstance(
                                        java.sql.PreparedStatement.class.getClassLoader(),
                                        new Class[]{java.sql.PreparedStatement.class},
                                        (stmtProxy, stmtMethod, stmtArgs) -> {
                                            if ("executeUpdate".equals(stmtMethod.getName())) {
                                                targetStmt.execute();
                                                return 1;
                                            }
                                            if ("getUpdateCount".equals(stmtMethod.getName())) {
                                                return 1;
                                            }
                                            return stmtMethod.invoke(targetStmt, stmtArgs);
                                        }
                                );
                            }
                            return method.invoke(targetConnection, args);
                        }
                );
            }
        };
    }
}
