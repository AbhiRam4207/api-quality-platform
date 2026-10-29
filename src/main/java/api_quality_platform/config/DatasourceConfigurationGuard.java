package api_quality_platform.config;

import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Arrays;
import java.util.List;

/**
 * Fail-fast guard for missing datasource credentials.
 *
 * <p>No database credential is stored in this repository. The credential must be
 * supplied through environment variables or a gitignored local profile file.
 * Without it, PostgreSQL rejects the connection and the failure surfaces as a
 * long, opaque Hibernate stack trace. This guard detects the condition before
 * Hibernate builds the EntityManagerFactory and prints an actionable message.
 *
 * <p>This class only reads configuration. It never prints, copies, or stores a
 * credential value.
 *
 * <p>Registered through {@code META-INF/spring.factories} under the key
 * {@code org.springframework.boot.EnvironmentPostProcessor}.
 */
public class DatasourceConfigurationGuard implements org.springframework.boot.EnvironmentPostProcessor, Ordered {

    private static final String DATASOURCE_URL_PROPERTY = "spring.datasource.url";
    private static final String DATASOURCE_PASSWORD_PROPERTY = "spring.datasource.password";

    private static final List<String> EMBEDDED_URL_PREFIXES = Arrays.asList(
            "jdbc:h2:",
            "jdbc:hsqldb:",
            "jdbc:derby:",
            "jdbc:sqlite:",
            "jdbc:mysql:"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        String url = environment.getProperty(DATASOURCE_URL_PROPERTY);

        if (url == null || url.isBlank()) {
            // No datasource configured at all; leave existing behaviour untouched.
            return;
        }

        if (EMBEDDED_URL_PREFIXES.stream().anyMatch(url::startsWith)) {
            // Embedded/passwordless engines do not need credential validation.
            return;
        }

        String password = environment.getProperty(DATASOURCE_PASSWORD_PROPERTY);

        if (password != null && !password.isBlank()) {
            // A credential was supplied through configuration.
            return;
        }

        String message = String.join(System.lineSeparator(),
                "",
                "============================================================",
                " STARTUP ABORTED: DATABASE PASSWORD NOT CONFIGURED",
                "============================================================",
                "",
                " No database credential is stored in this repository.",
                " Your PostgreSQL server requires one, so none was sent.",
                "",
                " Supply it locally using one of the following:",
                "",
                " 1) Environment variable (recommended, no rebuild needed)",
                "",
                "    SPRING_DATASOURCE_PASSWORD=<your local database password>",
                "",
                "    Windows PowerShell (current session only):",
                "      $env:SPRING_DATASOURCE_PASSWORD=\"<your local database password>\"",
                "",
                "    Windows (persist for your user):",
                "      setx SPRING_DATASOURCE_PASSWORD \"<your local database password>\"",
                "",
                "    Linux / macOS:",
                "      export SPRING_DATASOURCE_PASSWORD='<your local database password>'",
                "",
                " 2) Gitignored local profile file",
                "",
                "    Create src/main/resources/application-local.properties with:",
                "      spring.datasource.password=<your local database password>",
                "    Then start with the local profile:",
                "      ./mvnw spring-boot:run -Dspring-boot.run.profiles=local",
                "    Rebuild if the file is not picked up (resources are copied on build).",
                "",
                " Optional overrides also supported:",
                "   SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME",
                "",
                " IntelliJ IDEA: Run Configuration -> Environment variables,",
                " add SPRING_DATASOURCE_PASSWORD, then restart the run.",
                "",
                " Never commit the real credential value to source control.",
                "============================================================");

        System.err.println(message);

        throw new IllegalStateException(
                "spring.datasource.password is not set. Startup aborted: see the "
                        + DATASOURCE_PASSWORD_PROPERTY + " configuration instructions logged above.");
    }

    @Override
    public int getOrder() {
        // Run late so application.properties and all other sources are merged.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
