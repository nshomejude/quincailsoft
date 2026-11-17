package com.quincaisoft.security;
import org.springframework.stereotype.Component;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
/** A pooled business connection must never bypass PostgreSQL row-level security. */
@Component
public class DatabaseSafetyVerifier implements ApplicationRunner {
 private final JdbcTemplate jdbc;
 public DatabaseSafetyVerifier(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @Override public void run(ApplicationArguments arguments){
  Integer version=jdbc.queryForObject("select current_setting('server_version_num')::integer",Integer.class);
  Boolean unsafe=jdbc.queryForObject("select rolsuper or rolbypassrls from pg_roles where rolname=current_user",Boolean.class);
  String rowSecurity=jdbc.queryForObject("show row_security",String.class);
  if(version==null||version<180000||version>=190000)throw new IllegalStateException("PostgreSQL 18 is required by the approved baseline");
  if(!Boolean.FALSE.equals(unsafe)||!"on".equals(rowSecurity))throw new IllegalStateException("Runtime database identity must enforce RLS and must not be superuser/BYPASSRLS");
 }
}
