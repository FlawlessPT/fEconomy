package pt.flawless.fEconomy.services;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Regras de negócio da economia sobre o {@link EconomyRepository}. Todos os montantes são em cêntimos.
 *
 * <p>Violações de validação (montante inválido, argumento nulo) devolvem um futuro já falhado, com
 * {@link IllegalArgumentException} (montante) ou {@link NullPointerException} (uuid/nome nulo); nunca lançam
 * na thread chamadora e o repositório não é tocado.
 */
public class EconomyService {

    private final EconomyRepository repository;

    public EconomyService(EconomyRepository repository) {
        this.repository = repository;
    }

    /** Saldo em cêntimos; 0 se a conta não existir. */
    public CompletableFuture<Long> getBalance(UUID uuid) {
        if (uuid == null) {
            return nullArgument("uuid");
        }
        return repository.findBalance(uuid).thenApply(balance -> balance.orElse(0L));
    }

    public CompletableFuture<Void> ensureAccount(UUID uuid, String username) {
        if (uuid == null) {
            return nullArgument("uuid");
        }
        if (username == null) {
            return nullArgument("username");
        }
        return repository.ensureAccount(uuid, username);
    }

    /** Exige {@code cents > 0}. */
    public CompletableFuture<Long> deposit(UUID uuid, long cents) {
        if (uuid == null) {
            return nullArgument("uuid");
        }
        if (cents <= 0) {
            return invalidAmount(cents);
        }
        return repository.deposit(uuid, cents);
    }

    /** Exige {@code cents > 0}; falha com InsufficientFundsException se o saldo não chegar. */
    public CompletableFuture<Long> withdraw(UUID uuid, long cents) {
        if (uuid == null) {
            return nullArgument("uuid");
        }
        if (cents <= 0) {
            return invalidAmount(cents);
        }
        return repository.withdraw(uuid, cents);
    }

    /** Exige {@code cents >= 0}. */
    public CompletableFuture<Long> setBalance(UUID uuid, long cents) {
        if (uuid == null) {
            return nullArgument("uuid");
        }
        if (cents < 0) {
            return invalidAmount(cents);
        }
        return repository.setBalance(uuid, cents);
    }

    private static <T> CompletableFuture<T> invalidAmount(long cents) {
        return CompletableFuture.failedFuture(new IllegalArgumentException("montante inválido: " + cents));
    }

    private static <T> CompletableFuture<T> nullArgument(String name) {
        return CompletableFuture.failedFuture(new NullPointerException(name + " não pode ser nulo"));
    }
}
