package pt.miniFormiga.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.ResultSet;

@Configuration
public class SchemaCompatConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaCompatConfig.class);

    @org.springframework.context.annotation.Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    CommandLineRunner compatibilidadeSchemaUtilizadores(JdbcTemplate jdbcTemplate) {
        return args -> {
            if (!colunaExiste(jdbcTemplate, "utilizadores", "perfil_id")) {
                return;
            }

            String database = nomeBaseDados(jdbcTemplate);
            if (database.contains("PostgreSQL")) {
                jdbcTemplate.execute("alter table utilizadores drop column if exists perfil_id cascade");
            } else {
                jdbcTemplate.execute("alter table utilizadores drop column perfil_id");
            }
            LOGGER.info("Coluna legada utilizadores.perfil_id removida do schema");
        };
    }

    private boolean colunaExiste(JdbcTemplate jdbcTemplate, String tabela, String coluna) {
        return Boolean.TRUE.equals(jdbcTemplate.execute((Connection connection) -> {
            try (ResultSet columns = connection.getMetaData().getColumns(null, null, tabela, coluna)) {
                return columns.next();
            }
        }));
    }

    private String nomeBaseDados(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.execute((Connection connection) -> connection.getMetaData().getDatabaseProductName());
    }
}
