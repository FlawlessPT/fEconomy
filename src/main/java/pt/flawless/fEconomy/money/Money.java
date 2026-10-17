package pt.flawless.fEconomy.money;

import java.util.OptionalLong;
import pt.flawless.fapi.economy.Amounts;

public final class Money {

    private Money() {
    }

    /**
     * Lê um montante escrito pelo jogador ({@code 12}, {@code 12,5}, {@code 12.50}, {@code 1k}, {@code 2,5M}) e
     * devolve-o em cêntimos.
     *
     * @return vazio se o texto for nulo ou inválido, se o montante não for maior que 0 ou se não couber num
     * {@code long} de cêntimos
     */
    public static OptionalLong parse(String text) {
        OptionalLong cents = parseAllowZero(text);
        return cents.isPresent() && cents.getAsLong() > 0 ? cents : OptionalLong.empty();
    }

    /** Como {@link #parse(String)}, mas aceita 0 (por exemplo {@code 0}, {@code 0,00}). */
    public static OptionalLong parseAllowZero(String text) {
        return Amounts.parse(text);
    }

    /** Formata um montante em cêntimos como euros, ex.: 1250 -> "12,50€"; 1500000 -> "15k€". */
    public static String format(long cents) {
        return Amounts.format(cents);
    }
}
