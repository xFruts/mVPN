package ru.maxow.mvpn.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.maxow.mvpn.minio.MinioService;
import ru.maxow.mvpn.model.BroadcastRequestDto;

/**
 * Liquibase database migration and JPA schema validation contract tests.
 * Guarantees that changelogs are properly declared, syntactically valid,
 * and keep the database schema in 100% synchronization with Hibernate JPA entities.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(LiquibaseSchemaValidationTest.SchemaInitConfiguration.class)
@TestPropertySource(properties = {
    "spring.liquibase.enabled=true",
    "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@DisplayName("Liquibase Migrations & JPA Schema Validation Contract Test")
class LiquibaseSchemaValidationTest {

  @TestConfiguration
  static class SchemaInitConfiguration {
    @Bean
    public static BeanPostProcessor dataSourceBaseSchemaInitializer() {
      return new BeanPostProcessor() {
        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
          if (bean instanceof DataSource dataSource) {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("db/schema-base-test.sql")
            );
            populator.execute(dataSource);
          }
          return bean;
        }
      };
    }
  }

  @MockitoBean
  private MinioService minioService;

  @MockitoBean
  private ClientRegistrationRepository clientRegistrationRepository;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @MockitoBean
  private KafkaTemplate<String, BroadcastRequestDto> kafkaTemplate;

  @Autowired
  private DataSource dataSource;

  @Test
  @DisplayName("Verify master changelog includes every changeset file in changes directory")
  void masterChangelogShouldIncludeAllChangesetFiles() throws IOException {
    Path changesDir = Paths.get("src/main/resources/db/changelog/changes");
    if (!Files.exists(changesDir)) {
      changesDir = Paths.get("backend/src/main/resources/db/changelog/changes");
    }
    assertThat(Files.exists(changesDir))
        .as("Changeset directory must exist")
        .isTrue();

    Path masterChangelog = Paths.get("src/main/resources/db/changelog/db.changelog-master.yaml");
    if (!Files.exists(masterChangelog)) {
      masterChangelog = Paths.get(
          "backend/src/main/resources/db/changelog/db.changelog-master.yaml"
      );
    }
    assertThat(Files.exists(masterChangelog))
        .as("Master changelog must exist")
        .isTrue();

    String masterContent = Files.readString(masterChangelog, StandardCharsets.UTF_8);

    try (Stream<Path> stream = Files.list(changesDir)) {
      List<String> changeFiles = stream
          .filter(Files::isRegularFile)
          .map(p -> p.getFileName().toString())
          .filter(name -> name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".sql"))
          .toList();

      assertThat(changeFiles)
          .as("Changeset directory must contain changelog files")
          .isNotEmpty();

      for (String changeFile : changeFiles) {
        assertThat(masterContent)
            .as("Master changelog must include changeset file: %s", changeFile)
            .contains(changeFile);
      }
    }
  }

  @Test
  @DisplayName("Verify Liquibase executes cleanly on a completely empty database")
  void liquibaseShouldExecuteCleanlyOnFreshDatabase() throws Exception {
    DriverManagerDataSource freshDs = new DriverManagerDataSource();
    freshDs.setDriverClassName("org.h2.Driver");
    freshDs.setUrl("jdbc:h2:mem:fresh_liquibase_test_db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
    freshDs.setUsername("sa");
    freshDs.setPassword("");

    SpringLiquibase springLiquibase = new SpringLiquibase();
    springLiquibase.setDataSource(freshDs);
    springLiquibase.setChangeLog("classpath:db/changelog/db.changelog-master.yaml");
    springLiquibase.setResourceLoader(new DefaultResourceLoader());
    springLiquibase.afterPropertiesSet();

    Set<String> tableNames = new HashSet<>();
    try (Connection connection = freshDs.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      try (ResultSet tables = metaData.getTables(null, null, "%", new String[] {"TABLE"})) {
        while (tables.next()) {
          tableNames.add(tables.getString("TABLE_NAME").toLowerCase());
        }
      }
    }

    assertThat(tableNames).contains(
        "databasechangelog",
        "subscription_traffic_state",
        "subscription_server_traffic"
    );
  }

  @Test
  @DisplayName("Verify Liquibase migrations applied base alterations and created traffic tables")
  void shouldVerifyLiquibaseAppliedAllSchemaModifications() throws Exception {
    Set<String> tableNames = new HashSet<>();
    Set<String> userColumns = new HashSet<>();

    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();

      try (ResultSet tables = metaData.getTables(null, null, "%", new String[] {"TABLE"})) {
        while (tables.next()) {
          tableNames.add(tables.getString("TABLE_NAME").toLowerCase());
        }
      }

      try (ResultSet columns = metaData.getColumns(null, null, "users", "%")) {
        while (columns.next()) {
          userColumns.add(columns.getString("COLUMN_NAME").toLowerCase());
        }
      }
      if (userColumns.isEmpty()) {
        try (ResultSet columns = metaData.getColumns(null, null, "USERS", "%")) {
          while (columns.next()) {
            userColumns.add(columns.getString("COLUMN_NAME").toLowerCase());
          }
        }
      }
    }

    // 1. Verify Liquibase changelog tracking table exists
    assertThat(tableNames).contains("databasechangelog");

    // 2. Verify traffic tracking tables created by Liquibase 004-traffic-tables.yaml exist
    assertThat(tableNames).contains(
        "subscription_traffic_state",
        "subscription_server_traffic"
    );

    // 3. Verify user table altered with is_deleted (001) and telegram_username (005)
    assertThat(userColumns).contains(
        "is_deleted",
        "telegram_username"
    );
  }
}
