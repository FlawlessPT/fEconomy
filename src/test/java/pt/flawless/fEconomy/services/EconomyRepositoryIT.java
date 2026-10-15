package pt.flawless.fEconomy.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.flawless.fEconomy.TestDatabaseService;
import pt.flawless.fEconomy.TestDb;
import pt.flawless.fEconomy.exceptions.AccountNotFoundException;
import pt.flawless.fEconomy.exceptions.InsufficientFundsException;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyRepositoryIT {

    private static final long TIMEOUT_S = 30;

    private TestDatabaseService db;
    private EconomyRepository repo;

    @BeforeEach
    void setUp() throws Exception {
        db = TestDb.service();
        TestDb.resetSchema(db);
        repo = new EconomyRepository(db);
    }

    @AfterEach
    void tearDown() {
        if (db != null) {
            db.close();
        }
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(TIMEOUT_S, TimeUnit.SECONDS);
    }

    private static Throwable failureOf(CompletableFuture<?> future) {
        ExecutionException e = assertThrows(ExecutionException.class, () -> future.get(TIMEOUT_S, TimeUnit.SECONDS));
        return e.getCause();
    }

    private String storedName(UUID uuid) throws Exception {
        return await(db.supplyAsync(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT username FROM feconomy_balances WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (var rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        }));
    }

    @Test
    void ensureAccount_createsWithZeroBalance() throws Exception {
        UUID id = UUID.randomUUID();
        await(repo.ensureAccount(id, "Alice"));
        assertEquals(Optional.of(0L), await(repo.findBalance(id)));
        assertEquals("Alice", storedName(id));
    }

    @Test
    void ensureAccount_isIdempotentAndUpdatesNameWithoutResettingBalance() throws Exception {
        UUID id = UUID.randomUUID();
        await(repo.ensureAccount(id, "Alice"));
        await(repo.deposit(id, 500));
        await(repo.ensureAccount(id, "Alicia"));
        assertEquals(Optional.of(500L), await(repo.findBalance(id)));
        assertEquals("Alicia", storedName(id));
    }

    @Test
    void findBalance_absentAccountIsEmpty() throws Exception {
        assertEquals(Optional.empty(), await(repo.findBalance(UUID.randomUUID())));
    }

    @Test
    void deposit_returnsNewBalance() throws Exception {
        UUID id = UUID.randomUUID();
        await(repo.ensureAccount(id, "Alice"));
        assertEquals(100L, await(repo.deposit(id, 100)));
        assertEquals(350L, await(repo.deposit(id, 250)));
        assertEquals(Optional.of(350L), await(repo.findBalance(id)));
    }

    @Test
    void deposit_unknownAccountFailsWithAccountNotFound() {
        Throwable cause = failureOf(repo.deposit(UUID.randomUUID(), 10));
        assertInstanceOf(AccountNotFoundException.class, cause);
    }

    @Test
    void deposit_overflowFailsAndLeavesBalanceIntact() throws Exception {
        UUID id = UUID.randomUUID();
        await(repo.ensureAccount(id, "Alice"));
        await(repo.setBalance(id, 1));
        Throwable cause = failureOf(repo.deposit(id, Long.MAX_VALUE));
        assertInstanceOf(SQLException.class, cause);
        assertEquals(Optional.of(1L), await(repo.findBalance(id)));
    }

    @Test
    void withdraw_sufficientFundsReturnsNewBalance() throws Exception {
        UUID id = UUID.randomUUID();
        await(repo.ensureAccount(id, "Alice"));
        await(repo.setBalance(id, 1000));
        assertEquals(700L, await(repo.withdraw(id, 300)));
        assertEquals(0L, await(repo.withdraw(id, 700)));
        assertEquals(Optional.of(0L), await(repo.findBalance(id)));
    }

    @Test
    void withdraw_nonPositiveAmountFailsWithIllegalArgumentAndLeavesBalanceIntact() throws Exception {
        UUID id = UUID.randomUUID();
        await(repo.ensureAccount(id, "Alice"));
        await(repo.setBalance(id, 100));
        for (long invalid : new long[]{0L, -5L}) {
            assertInstanceOf(IllegalArgumentException.class, failureOf(repo.withdraw(id, invalid)));
        }
        assertEquals(Optional.of(100L), await(repo.findBalance(id)));
    }

    @Test
    void deposit_nonPositiveAmountFailsWithIllegalArgumentAndLeavesBalanceIntact() throws Exception {
        UUID id = UUID.randomUUID();
        await(repo.ensureAccount(id, "Alice"));
        await(repo.setBalance(id, 100));
        for (long invalid : new long[]{0L, -5L}) {
            assertInstanceOf(IllegalArgumentException.class, failureOf(repo.deposit(id, invalid)));
        }
        assertEquals(Optional.of(100L), await(repo.findBalance(id)));
    }

    @Test
    void setBalance_negativeFailsWithIllegalArgument() throws Exception {
        UUID id = UUID.randomUUID();
        await(repo.ensureAccount(id, "Alice"));
        await(repo.setBalance(id, 100));
        assertInstanceOf(IllegalArgumentException.class, failureOf(repo.setBalance(id, -1)));
        assertEquals(Optional.of(100L), await(repo.findBalance(id)));
    }

    @Test
    void withdraw_insufficientFundsFailsAndLeavesBalanceIntact() throws Exception {
        UUID id = UUID.randomUUID();
        await(repo.ensureAccount(id, "Alice"));
        await(repo.setBalance(id, 100));
        Throwable cause = failureOf(repo.withdraw(id, 101));
        assertInstanceOf(InsufficientFundsException.class, cause);
        assertTrue(cause.getMessage().contains("saldo 100"), "a mensagem deve indicar o saldo");
        assertEquals(Optional.of(100L), await(repo.findBalance(id)));
    }

    @Test
    void withdraw_unknownAccountFailsWithAccountNotFound() {
        Throwable cause = failureOf(repo.withdraw(UUID.randomUUID(), 10));
        assertInstanceOf(AccountNotFoundException.class, cause);
    }

    @Test
    void setBalance_updatesAndReturnsValue() throws Exception {
        UUID id = UUID.randomUUID();
        await(repo.ensureAccount(id, "Alice"));
        assertEquals(777L, await(repo.setBalance(id, 777)));
        assertEquals(Optional.of(777L), await(repo.findBalance(id)));
    }

    @Test
    void setBalance_unknownAccountFailsWithAccountNotFound() {
        Throwable cause = failureOf(repo.setBalance(UUID.randomUUID(), 5));
        assertInstanceOf(AccountNotFoundException.class, cause);
    }

    @Test
    void setBalance_toSameValueDoesNotThrowAccountNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        await(repo.ensureAccount(id, "Alice"));
        await(repo.setBalance(id, 42));
        assertEquals(42L, await(repo.setBalance(id, 42)));
        assertEquals(Optional.of(42L), await(repo.findBalance(id)));
        UUID other = UUID.randomUUID();
        await(repo.ensureAccount(other, "Bob"));
        assertEquals(0L, await(repo.setBalance(other, 0)));
    }

    @Test
    void deposit_concurrentDepositsSumExactly() throws Exception {
        UUID id = UUID.randomUUID();
        await(repo.ensureAccount(id, "Alice"));
        int threads = 20;
        int perThread = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> tasks = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                tasks.add(pool.submit(() -> {
                    start.await(TIMEOUT_S, TimeUnit.SECONDS);
                    for (int i = 0; i < perThread; i++) {
                        repo.deposit(id, 1).get(TIMEOUT_S, TimeUnit.SECONDS);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> task : tasks) {
                task.get(120, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(Optional.of((long) threads * perThread), await(repo.findBalance(id)));
    }

    @Test
    void check_constraintRejectsNegativeBalance() throws Exception {
        UUID id = UUID.randomUUID();
        await(repo.ensureAccount(id, "Alice"));
        CompletableFuture<Void> direct = db.runAsync(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE feconomy_balances SET balance = -1 WHERE uuid = ?")) {
                ps.setString(1, id.toString());
                ps.executeUpdate();
            }
        });
        assertInstanceOf(SQLException.class, failureOf(direct));
        assertEquals(Optional.of(0L), await(repo.findBalance(id)));
    }
}
