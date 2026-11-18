import org.flywaydb.core.Flyway;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Set;
/** One-shot schema tool, deliberately independent of Spring/runtime startup. */
public final class MigrationRunner {
    static String required(String key) {var value=System.getenv(key);if(value==null||value.isBlank())throw new IllegalArgumentException("Missing "+key);return value;}
    public static void main(String[] args) throws Exception {
        String command=args.length==1?args[0]:"";
        if(!Set.of("validate","migrate","info").contains(command))throw new IllegalArgumentException("Use validate, migrate or info; clean/repair/baseline are not available");
        String url=required("QUINCAI_MIGRATION_URL"),user=required("QUINCAI_MIGRATION_USER");
        if(!url.startsWith("jdbc:postgresql://")||!url.matches(".*[?&]sslmode=verify-full(?:&.*)?"))throw new IllegalArgumentException("Verified PostgreSQL TLS required");
        if(user.equals(required("QUINCAI_DATABASE_USER")))throw new IllegalArgumentException("Migration and runtime roles must differ");
        String password=Files.readString(Path.of(required("QUINCAI_MIGRATION_PASSWORD_FILE"))).strip();
        if(password.length()<20)throw new IllegalArgumentException("Generated migration secret required");
        try(var connection=DriverManager.getConnection(url,user,password)) {
            if(connection.getMetaData().getDatabaseMajorVersion()!=18)throw new IllegalStateException("PostgreSQL 18 required");
        }
        var flyway=Flyway.configure().dataSource(url,user,password).locations("classpath:db/migration")
            .cleanDisabled(true).baselineOnMigrate(false).outOfOrder(false).validateMigrationNaming(true).validateOnMigrate(true).ignoreMigrationPatterns("*:pending").load();
        switch(command) {
            case "validate" -> flyway.validate();
            case "migrate" -> {flyway.validate();var result=flyway.migrate();System.out.println("Migrations applied: "+result.migrationsExecuted);}
            case "info" -> {for(var migration:flyway.info().all())System.out.println(migration.getVersion()+" "+migration.getState());}
            default -> throw new IllegalArgumentException("Unsupported command");
        }
    }
}
