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

        // racha_members.posicao era texto livre e agora e o enum Posicao.
        // Padroniza para maiusculo e limpa valores que nao existem no enum, para nao quebrar a leitura.
        try {
            String posicoesValidas = java.util.Arrays.stream(br.com.rachaplus.api.domain.Posicao.values())
                    .map(p -> "'" + p.name() + "'")
                    .collect(java.util.stream.Collectors.joining(","));
            jdbcTemplate.execute("UPDATE racha_members SET posicao = UPPER(TRIM(posicao)) WHERE posicao IS NOT NULL");
            int limpas = jdbcTemplate.update(
                    "UPDATE racha_members SET posicao = NULL WHERE posicao IS NOT NULL AND posicao NOT IN (" + posicoesValidas + ")");
            if (limpas > 0) {
                log.info("Migracao de schema: {} posicoes legadas invalidas foram limpas em racha_members", limpas);
            }
        } catch (Exception ex) {
            log.debug("Ajuste de racha_members.posicao ignorado: {}", ex.getMessage());
        }

        // Preenche tipo = 'GERAL' nas avaliações legadas criadas antes do Sistema de Rating v2
        try {
            int atualizadas = jdbcTemplate.update("UPDATE avaliacoes SET tipo = 'GERAL' WHERE tipo IS NULL");
            if (atualizadas > 0) {
                log.info("Migracao de schema: {} avaliacoes legadas atualizadas para tipo = 'GERAL'", atualizadas);
            }
        } catch (Exception ex) {
            log.debug("Ajuste de avaliacoes.tipo ignorado: {}", ex.getMessage());
        }
    }
}
