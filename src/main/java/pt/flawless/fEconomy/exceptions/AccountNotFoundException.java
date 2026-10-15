package pt.flawless.fEconomy.exceptions;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(UUID uuid) {
        super("conta de economia não encontrada: " + uuid);
    }
}
