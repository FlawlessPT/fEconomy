package pt.flawless.fEconomy.services;

import pt.flawless.fEconomy.money.Money;
import pt.flawless.fapi.economy.EconomyApi;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Regras de negócio da economia sobre o {@link EconomyRepository}. Todos os montantes são em cêntimos.
 *
 * <p>Violações de validação (montante inválido, argumento nulo) devolvem um futuro já falhado, com
 * {@link IllegalArgumentException} (montante) ou {@link NullPointerException} (uuid/nome nulo); nunca lançam
 * na thread chamadora e o repositório não é tocado.
 */
public class EconomyService implements EconomyApi {

    private final EconomyRepository repository;

    public EconomyService(EconomyRepository repository) {
        this.repository = repository;
    }

    /** Saldo em cêntimos; 0 se a conta não existir. */
    @Override
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
    @Override
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
    @Override
    public CompletableFuture<Long> withdraw(UUID uuid, long cents) {
        if (uuid == null) {
            return nullArgument("uuid");
        }
        if (cents <= 0) {
            return invalidAmount(cents);
        }
        return repository.withdraw(uuid, cents);
    }

    /**
     * Exige {@code cents > 0} e {@code from != to}; falha com InsufficientFundsException se o saldo não chegar.
     *
     * @return o novo saldo de {@code from}
     */
    @Override
    public CompletableFuture<Long> transfer(UUID from, UUID to, long cents) {
        if (from == null) {
            return nullArgument("from");
        }
        if (to == null) {
            return nullArgument("to");
        }
        if (cents <= 0) {
            return invalidAmount(cents);
        }
        if (from.equals(to)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("origem e destino iguais: " + from));
        }
        return repository.transfer(from, to, cents);
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

    @Override
    public String format(long cents) {
        return Money.format(cents);
    }

    private static <T> CompletableFuture<T> invalidAmount(long cents) {
        return CompletableFuture.failedFuture(new IllegalArgumentException("montante inválido: " + cents));
    }

    private static <T> CompletableFuture<T> nullArgument(String name) {
        return CompletableFuture.failedFuture(new NullPointerException(name + " não pode ser nulo"));
    }
}
