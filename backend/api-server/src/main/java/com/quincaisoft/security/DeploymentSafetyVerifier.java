package com.quincaisoft.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Reject deploy-time privileges that let a runtime account remove tenant isolation. */
@Component
public final class DeploymentSafetyVerifier implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public DeploymentSafetyVerifier(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments arguments) {
        Boolean ownsTables = jdbc.queryForObject("""
            select exists (
              select 1 from pg_class c join pg_namespace n on n.oid=c.relnamespace
              where n.nspname='public' and c.relkind in ('r','p')
                and pg_has_role(current_user,c.relowner,'MEMBER'))
            """, Boolean.class);
        Boolean canCreate = jdbc.queryForObject("""
            select has_schema_privilege(current_user,'public','CREATE')
                or has_database_privilege(current_user,current_database(),'CREATE')
            """, Boolean.class);
        Long unprotected = jdbc.queryForObject("""
            select count(*) from pg_class c join pg_namespace n on n.oid=c.relnamespace
            where n.nspname='public' and c.relkind in ('r','p')
              and (c.relname='tenant' or exists (
                select 1 from pg_attribute a where a.attrelid=c.oid
                  and a.attname='tenant_id' and not a.attisdropped))
              and (not c.relrowsecurity or not c.relforcerowsecurity)
            """, Long.class);
        if (!Boolean.FALSE.equals(ownsTables) || !Boolean.FALSE.equals(canCreate)) {
            throw new IllegalStateException("Runtime identity must not own application tables, inherit their owner, or create schemas/objects");
        }
        if (unprotected == null || unprotected != 0L) {
            throw new IllegalStateException("Every tenant table requires enabled and forced row-level security");
        }
    }
}
