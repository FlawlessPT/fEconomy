package pt.flawless.fEconomy;

import org.junit.jupiter.api.Assumptions;
import pt.flawless.fapi.database.DatabaseService;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

/** Ajudante dos testes de integracao: so corre contra a base literalmente chamada flawless_test. */
public final class TestDb {

    private static final String[] VARS = {"FLAWLESS_DB_HOST", "FLAWLESS_DB_PORT", "FLAWLESS_DB_NAME",
            "FLAWLESS_DB_USER", "FLAWLESS_DB_PASSWORD"};

    private TestDb() {
    }

    /** Ignora o teste se faltarem variaveis ou se a base nao for flawless_test (os testes fazem DROP). */
    public static TestDatabaseService service() {
        for (String v : VARS) {
            String value = System.getenv(v);
            Assumptions.assumeTrue(value != null && !value.isEmpty(), v + " nao definida");
        }
        Assumptions.assumeTrue("flawless_test".equals(System.getenv("FLAWLESS_DB_NAME")),
                "FLAWLESS_DB_NAME nao e flawless_test");
        return new TestDatabaseService(System.getenv("FLAWLESS_DB_HOST"), System.getenv("FLAWLESS_DB_PORT"),
                System.getenv("FLAWLESS_DB_NAME"), System.getenv("FLAWLESS_DB_USER"),
                System.getenv("FLAWLESS_DB_PASSWORD"));
    }

    /** Apaga so feconomy_balances (nunca schema_version) e recria-a com o ficheiro V001. */
    public static void resetSchema(DatabaseService database) throws Exception {
        String ddl = readMigration().trim();
        if (ddl.endsWith(";")) {
            ddl = ddl.substring(0, ddl.length() - 1);
        }
        String create = ddl;
        database.runAsync(connection -> {
            try (Statement st = connection.createStatement()) {
                st.execute("DROP TABLE IF EXISTS feconomy_balances");
                st.execute(create);
            }
        }).get(30, TimeUnit.SECONDS);
    }

    private static String readMigration() {
        try (InputStream in = TestDb.class.getResourceAsStream("/db/feconomy/V001__create_balances.sql")) {
            if (in == null) {
                throw new IllegalStateException("V001 nao encontrado no classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
