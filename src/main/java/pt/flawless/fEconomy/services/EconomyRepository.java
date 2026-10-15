package pt.flawless.fEconomy.services;

import pt.flawless.fEconomy.exceptions.AccountNotFoundException;
import pt.flawless.fEconomy.exceptions.InsufficientFundsException;
import pt.flawless.fapi.database.DatabaseService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Acesso aos saldos (em cêntimos) na tabela {@code feconomy_balances}.
 *
 * <p>Todos os métodos são assíncronos: correm no pool do {@link DatabaseService} e devolvem um futuro que
 * completa excepcionalmente com a excepção original ({@link SQLException}, {@link AccountNotFoundException},
 * {@link InsufficientFundsException}). Nunca se deve esperar ({@code join()}/{@code get()}) por outro futuro de
 * base de dados dentro de uma tarefa do pool.
 *
 * <p>A classe não é {@code final} e o construtor não valida o argumento: os testes unitários do serviço passam
 * {@code null} num duplo que sobrepõe todos os métodos.
 *
 * <p>Nota: com o Connector/J por omissão ({@code useAffectedRows=false}) um {@code UPDATE} devolve as linhas
 * encontradas e não as alteradas, por isso pôr o mesmo valor numa conta existente não conta como "conta em falta".
 */
public class EconomyRepository {

    private static final String ENSURE =
            "INSERT INTO feconomy_balances (uuid, username) VALUES (?, ?) AS new "
                    + "ON DUPLICATE KEY UPDATE username = new.username";
    private static final String SELECT_BALANCE = "SELECT balance FROM feconomy_balances WHERE uuid = ?";
    private static final String DEPOSIT = "UPDATE feconomy_balances SET balance = balance + ? WHERE uuid = ?";
    private static final String WITHDRAW =
            "UPDATE feconomy_balances SET balance = balance - ? WHERE uuid = ? AND balance >= ?";
    private static final String SET_BALANCE = "UPDATE feconomy_balances SET balance = ? WHERE uuid = ?";

    private final DatabaseService database;

    public EconomyRepository(DatabaseService database) {
        this.database = database;
    }

    /**
     * Cria a conta com saldo 0 se não existir; se existir só actualiza o nome (o saldo mantém-se).
     *
     * @return futuro que falha com {@link SQLException} em erro de base de dados
     */
    public CompletableFuture<Void> ensureAccount(UUID uuid, String username) {
        return database.runAsync(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(ENSURE)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, username);
                ps.executeUpdate();
            }
        });
    }

    /**
     * @return o saldo em cêntimos, ou vazio se a conta não existir; falha com {@link SQLException}
     */
    public CompletableFuture<Optional<Long>> findBalance(UUID uuid) {
        return database.supplyAsync(connection -> Optional.ofNullable(selectBalance(connection, uuid)));
    }

    /**
     * Soma {@code cents} ao saldo, de forma atómica.
     *
     * <p>Pré-condição: {@code cents > 0} (validado também no serviço); caso contrário o futuro falha já com
     * {@link IllegalArgumentException} sem tocar na base de dados.
     *
     * @return o novo saldo; falha com {@link AccountNotFoundException} se a conta não existir e com
     * {@link SQLException} em erro de base de dados (incluindo overflow de BIGINT, caso em que o saldo não muda)
     */
    public CompletableFuture<Long> deposit(UUID uuid, long cents) {
        if (cents <= 0) {
            return invalidAmount(cents);
        }
        return database.supplyAsync(connection -> inTransaction(connection, () -> {
            int rows;
            try (PreparedStatement ps = connection.prepareStatement(DEPOSIT)) {
                ps.setLong(1, cents);
                ps.setString(2, uuid.toString());
                rows = ps.executeUpdate();
            }
            Long balance = rows == 0 ? null : selectBalance(connection, uuid);
            if (balance == null) {
                throw new AccountNotFoundException(uuid);
            }
            return balance;
        }));
    }

    /**
     * Subtrai {@code cents} ao saldo, de forma atómica, sem nunca ficar negativo.
     *
     * <p>Pré-condição: {@code cents > 0} (validado também no serviço); caso contrário o futuro falha já com
     * {@link IllegalArgumentException} sem tocar na base de dados.
     *
     * @return o novo saldo; falha com {@link AccountNotFoundException} se a conta não existir, com
     * {@link InsufficientFundsException} se o saldo for menor que {@code cents} e com {@link SQLException}
     * em erro de base de dados
     */
    public CompletableFuture<Long> withdraw(UUID uuid, long cents) {
        if (cents <= 0) {
            return invalidAmount(cents);
        }
        return database.supplyAsync(connection -> inTransaction(connection, () -> {
            int rows;
            try (PreparedStatement ps = connection.prepareStatement(WITHDRAW)) {
                ps.setLong(1, cents);
                ps.setString(2, uuid.toString());
                ps.setLong(3, cents);
                rows = ps.executeUpdate();
            }
            Long balance = selectBalance(connection, uuid);
            if (balance == null) {
                throw new AccountNotFoundException(uuid);
            }
            if (rows == 0) {
                throw new InsufficientFundsException(uuid, balance, cents);
            }
            return balance;
        }));
    }

    /**
     * Define o saldo para {@code cents}.
     *
     * <p>Pré-condição: {@code cents >= 0} (validado também no serviço); caso contrário o futuro falha já com
     * {@link IllegalArgumentException} sem tocar na base de dados.
     *
     * @return {@code cents}; falha com {@link AccountNotFoundException} se a conta não existir e com
     * {@link SQLException} em erro de base de dados (por exemplo valor negativo, recusado pelo CHECK)
     */
    public CompletableFuture<Long> setBalance(UUID uuid, long cents) {
        if (cents < 0) {
            return invalidAmount(cents);
        }
        return database.supplyAsync(connection -> {
            int rows;
            try (PreparedStatement ps = connection.prepareStatement(SET_BALANCE)) {
                ps.setLong(1, cents);
                ps.setString(2, uuid.toString());
                rows = ps.executeUpdate();
            }
            if (rows == 0) {
                throw new AccountNotFoundException(uuid);
            }
            return cents;
        });
    }

    private static <T> CompletableFuture<T> invalidAmount(long cents) {
        return CompletableFuture.failedFuture(new IllegalArgumentException("montante inválido: " + cents));
    }

    private static Long selectBalance(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_BALANCE)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    @FunctionalInterface
    private interface TxWork<R> {
        R run() throws SQLException;
    }

    /** Corre o trabalho numa transacção: commit se correr bem, rollback antes de propagar qualquer falha. */
    private static <R> R inTransaction(Connection connection, TxWork<R> work) throws SQLException {
        connection.setAutoCommit(false);
        try {
            R result = work.run();
            connection.commit();
            return result;
        } catch (SQLException | RuntimeException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackFailure) {
                e.addSuppressed(rollbackFailure);
            }
            throw e;
        }
    }
}
