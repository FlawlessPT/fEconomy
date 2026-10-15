package pt.flawless.fEconomy.services;

import org.junit.jupiter.api.Test;
import pt.flawless.fEconomy.exceptions.InsufficientFundsException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class EconomyServiceTest {

    /** Duplo em memória do repositório: conta chamadas e guarda saldos. */
    static class FakeRepository extends EconomyRepository {
        final Map<UUID, Long> balances = new HashMap<>();
        final Map<UUID, String> names = new HashMap<>();
        int calls = 0;

        FakeRepository() {
            super(null);
        }

        @Override
        public CompletableFuture<Void> ensureAccount(UUID uuid, String username) {
            calls++;
            balances.putIfAbsent(uuid, 0L);
            names.put(uuid, username);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Optional<Long>> findBalance(UUID uuid) {
            calls++;
            return CompletableFuture.completedFuture(Optional.ofNullable(balances.get(uuid)));
        }

        @Override
        public CompletableFuture<Long> deposit(UUID uuid, long cents) {
            calls++;
            return CompletableFuture.completedFuture(balances.merge(uuid, cents, Long::sum));
        }

        @Override
        public CompletableFuture<Long> withdraw(UUID uuid, long cents) {
            calls++;
            long current = balances.getOrDefault(uuid, 0L);
            if (current < cents) {
                return CompletableFuture.failedFuture(new InsufficientFundsException(uuid, current, cents));
            }
            balances.put(uuid, current - cents);
            return CompletableFuture.completedFuture(current - cents);
        }

        @Override
        public CompletableFuture<Long> setBalance(UUID uuid, long cents) {
            calls++;
            balances.put(uuid, cents);
            return CompletableFuture.completedFuture(cents);
        }
    }

    private final FakeRepository repo = new FakeRepository();
    private final EconomyService service = new EconomyService(repo);
    private final UUID id = UUID.randomUUID();

    private static Throwable failureOf(CompletableFuture<?> future) {
        ExecutionException e = assertThrows(ExecutionException.class, future::get);
        return e.getCause();
    }

    @Test
    void getBalance_absentAccountIsZero() throws Exception {
        assertEquals(0L, service.getBalance(id).get());
    }

    @Test
    void getBalance_returnsStoredBalance() throws Exception {
        repo.balances.put(id, 1250L);
        assertEquals(1250L, service.getBalance(id).get());
    }

    @Test
    void deposit_nonPositiveFailsWithoutTouchingRepository() {
        for (long invalid : new long[]{0L, -5L}) {
            Throwable cause = failureOf(service.deposit(id, invalid));
            assertInstanceOf(IllegalArgumentException.class, cause);
            assertTrue(cause.getMessage().contains("montante"));
        }
        assertEquals(0, repo.calls);
    }

    @Test
    void withdraw_nonPositiveFailsWithoutTouchingRepository() {
        for (long invalid : new long[]{0L, -5L}) {
            Throwable cause = failureOf(service.withdraw(id, invalid));
            assertInstanceOf(IllegalArgumentException.class, cause);
            assertTrue(cause.getMessage().contains("montante"));
        }
        assertEquals(0, repo.calls);
    }

    @Test
    void setBalance_negativeFails() {
        Throwable cause = failureOf(service.setBalance(id, -1L));
        assertInstanceOf(IllegalArgumentException.class, cause);
        assertTrue(cause.getMessage().contains("montante"));
        assertEquals(0, repo.calls);
    }

    @Test
    void setBalance_zeroIsAllowed() throws Exception {
        repo.balances.put(id, 500L);
        assertEquals(0L, service.setBalance(id, 0L).get());
        assertEquals(0L, repo.balances.get(id));
    }

    @Test
    void deposit_delegatesAndReturnsNewBalance() throws Exception {
        repo.balances.put(id, 100L);
        assertEquals(350L, service.deposit(id, 250L).get());
        assertEquals(1, repo.calls);
    }

    @Test
    void withdraw_propagatesInsufficientFunds() {
        repo.balances.put(id, 100L);
        Throwable cause = failureOf(service.withdraw(id, 250L));
        assertInstanceOf(InsufficientFundsException.class, cause);
        assertEquals(100L, repo.balances.get(id));
    }

    @Test
    void ensureAccount_delegates() throws Exception {
        service.ensureAccount(id, "Joao").get();
        assertEquals("Joao", repo.names.get(id));
        assertEquals(0L, repo.balances.get(id));
        assertEquals(1, repo.calls);
    }
}
