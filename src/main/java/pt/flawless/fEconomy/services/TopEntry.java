package pt.flawless.fEconomy.services;

/** Uma linha do ranking: nome do jogador e saldo em cêntimos. */
public record TopEntry(String username, long balance) {
}
