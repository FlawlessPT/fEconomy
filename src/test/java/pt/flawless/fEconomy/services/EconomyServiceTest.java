package pt.flawless.fEconomy.services;

import org.junit.jupiter.api.Test;
import pt.flawless.fapi.economy.EconomyApi;
import pt.flawless.fapi.economy.InsufficientFundsException;

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
        public CompletableFuture<Long> transfer(UUID from, UUID to, long cents) {
            calls++;
            long current = balances.getOrDefault(from, 0L);
            if (current < cents) {
                return CompletableFuture.failedFuture(new InsufficientFundsException(from, current, cents));
            }
            balances.put(from, current - cents);
            balances.merge(to, cents, Long::sum);
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

    @Test
    void transfer_nullArgumentsFailWithNullPointer() {
        UUID other = UUID.randomUUID();
        assertInstanceOf(NullPointerException.class, failureOf(service.transfer(null, other, 10)));
        assertInstanceOf(NullPointerException.class, failureOf(service.transfer(id, null, 10)));
        assertEquals(0, repo.calls);
    }

    @Test
    void transfer_nonPositiveFailsWithoutTouchingRepository() {
        UUID other = UUID.randomUUID();
        for (long invalid : new long[]{0L, -5L}) {
            Throwable cause = failureOf(service.transfer(id, other, invalid));
            assertInstanceOf(IllegalArgumentException.class, cause);
            assertTrue(cause.getMessage().contains("montante"));
        }
        assertEquals(0, repo.calls);
    }

    @Test
    void transfer_toSelfFailsWithoutTouchingRepository() {
        repo.balances.put(id, 100L);
        assertInstanceOf(IllegalArgumentException.class, failureOf(service.transfer(id, id, 10)));
        assertEquals(0, repo.calls);
        assertEquals(100L, repo.balances.get(id));
    }

    @Test
    void transfer_delegatesAndReturnsSenderBalance() throws Exception {
        UUID other = UUID.randomUUID();
        repo.balances.put(id, 500L);
        repo.balances.put(other, 20L);
        assertEquals(300L, service.transfer(id, other, 200L).get());
        assertEquals(220L, repo.balances.get(other));
        assertEquals(1, repo.calls);
    }

    @Test
    void transfer_propagatesInsufficientFunds() {
        UUID other = UUID.randomUUID();
        repo.balances.put(id, 100L);
        repo.balances.put(other, 0L);
        assertInstanceOf(InsufficientFundsException.class, failureOf(service.transfer(id, other, 250L)));
        assertEquals(100L, repo.balances.get(id));
        assertEquals(0L, repo.balances.get(other));
    }

    @Test
    void implementsEconomyApi_andFormatsCents() {
        EconomyApi api = new EconomyService(new FakeRepository());
        assertEquals("12,50€", api.format(1250));
        assertEquals("0,05€", api.format(5));
    }

    @Test
    void getRichestDelegaNoRepositorio() throws Exception {
        UUID rico = UUID.randomUUID();
        FakeRepository repo = new FakeRepository() {
            @Override
            public CompletableFuture<Optional<UUID>> findRichest() {
                return CompletableFuture.completedFuture(Optional.of(rico));
            }
        };
        assertEquals(Optional.of(rico), new EconomyService(repo).getRichest().get());
    }
}
