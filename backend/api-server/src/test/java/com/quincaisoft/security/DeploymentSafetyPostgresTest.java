package com.quincaisoft.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="QUINCAI_PG_TEST_URL", matches=".+")
class DeploymentSafetyPostgresTest {
    @Test void rejectsOwnershipDdlAndUnprotectedTenantTables() throws Exception {
        String adminUrl=System.getenv("QUINCAI_PG_TEST_URL");
        String user=System.getenv().getOrDefault("QUINCAI_PG_TEST_USER","agent");
        String suffix=UUID.randomUUID().toString().replace("-","");
        String db="deploy_"+suffix, role="runtime_"+suffix;
        String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+db;
        try (var admin=DriverManager.getConnection(adminUrl,user,"")) {
            sql(admin,"CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS");
            sql(admin,"CREATE DATABASE "+db);
            try (var owner=DriverManager.getConnection(url,user,"")) {
                sql(owner,"CREATE TABLE tenant(id uuid primary key)");
                sql(owner,"CREATE TABLE test_fact(tenant_id uuid not null)");
                sql(owner,"REVOKE CREATE ON SCHEMA public FROM PUBLIC");
                sql(owner,"GRANT USAGE ON SCHEMA public TO "+role);
                var guard=new DeploymentSafetyVerifier(new JdbcTemplate(new DriverManagerDataSource(url,role,"")));
                assertThrows(IllegalStateException.class,()->guard.run(null));
                sql(owner,"ALTER TABLE tenant ENABLE ROW LEVEL SECURITY; ALTER TABLE tenant FORCE ROW LEVEL SECURITY");
                sql(owner,"ALTER TABLE test_fact ENABLE ROW LEVEL SECURITY; ALTER TABLE test_fact FORCE ROW LEVEL SECURITY");
                assertDoesNotThrow(()->guard.run(null));
                sql(owner,"GRANT CREATE ON SCHEMA public TO "+role);
                assertThrows(IllegalStateException.class,()->guard.run(null));
                sql(owner,"REVOKE CREATE ON SCHEMA public FROM "+role);
                sql(owner,"GRANT CREATE ON DATABASE "+db+" TO "+role);
                assertThrows(IllegalStateException.class,()->guard.run(null));
                sql(owner,"REVOKE CREATE ON DATABASE "+db+" FROM "+role);
                sql(owner,"ALTER TABLE test_fact NO FORCE ROW LEVEL SECURITY");
                assertThrows(IllegalStateException.class,()->guard.run(null));
                sql(owner,"ALTER TABLE test_fact FORCE ROW LEVEL SECURITY");
                sql(owner,"ALTER TABLE test_fact OWNER TO "+role);
                assertThrows(IllegalStateException.class,()->guard.run(null));
            } finally {
                sql(admin,"DROP DATABASE "+db); sql(admin,"DROP ROLE "+role);
            }
        }
    }
    private static void sql(Connection c,String query) throws SQLException {
        try (var s=c.createStatement()) { s.execute(query); }
    }
}
