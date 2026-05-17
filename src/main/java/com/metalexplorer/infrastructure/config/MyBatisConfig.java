package com.metalexplorer.infrastructure.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.*;
import java.util.UUID;

@Configuration
public class MyBatisConfig {

    @Bean
    public ConfigurationCustomizer mybatisUuidTypeHandlerCustomizer() {
        return configuration -> {
            var handler = new UUIDTypeHandler();
            var registry = configuration.getTypeHandlerRegistry();
            registry.register(UUID.class, JdbcType.OTHER, handler);
            registry.register(UUID.class, JdbcType.BINARY, handler);
            registry.register(UUID.class, JdbcType.VARCHAR, handler);
        };
    }

    public static class UUIDTypeHandler extends BaseTypeHandler<UUID> {

        @Override
        public void setNonNullParameter(PreparedStatement ps, int i, UUID parameter, JdbcType jdbcType)
                throws SQLException {
            ps.setObject(i, parameter, Types.OTHER);
        }

        @Override
        public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
            return toUUID(rs.getObject(columnName));
        }

        @Override
        public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            return toUUID(rs.getObject(columnIndex));
        }

        @Override
        public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
            return toUUID(cs.getObject(columnIndex));
        }

        private static UUID toUUID(Object obj) {
            if (obj == null) return null;
            if (obj instanceof UUID) return (UUID) obj;
            return UUID.fromString(obj.toString());
        }
    }
}
