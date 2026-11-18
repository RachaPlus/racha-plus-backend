package br.com.rachaplus.api.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaMigrationRunner.class);

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public DatabaseSchemaMigrationRunner(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    @Override
    public void run(ApplicationArguments args) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            return;
        }

        try {
            jdbcTemplate.execute("ALTER TABLE usuarios ALTER COLUMN nome DROP NOT NULL");
            log.info("Migracao de schema: constraint NOT NULL removida da coluna legada usuarios.nome");
        } catch (Exception ex) {
            log.debug("Coluna legada usuarios.nome ja ajustada ou inexistente: {}", ex.getMessage());
        }

        try {
            jdbcTemplate.execute("UPDATE usuarios SET username = nome WHERE username IS NULL AND nome IS NOT NULL");
        } catch (Exception ex) {
            log.debug("Sincronizacao opcional usuarios.nome -> username ignorada: {}", ex.getMessage());
        }
    }
}
