package pt.flawless.fEconomy.money;

import java.math.BigDecimal;
import java.util.OptionalLong;
import java.util.regex.Pattern;

public final class Money {

    // dígitos com no máximo 2 casas decimais (vírgula ou ponto); sem sinal nem separador de milhares
    private static final Pattern AMOUNT = Pattern.compile("[0-9]+(?:[.,][0-9]{1,2})?");

    private Money() {
    }

    /**
     * Lê um montante escrito pelo jogador ({@code 12}, {@code 12,5}, {@code 12.50}) e devolve-o em cêntimos.
     *
     * @return vazio se o texto for nulo ou inválido, se o montante não for maior que 0 ou se não couber num
     * {@code long} de cêntimos
     */
    public static OptionalLong parse(String text) {
        if (text == null || !AMOUNT.matcher(text).matches()) {
            return OptionalLong.empty();
        }
        try {
            long cents = new BigDecimal(text.replace(',', '.')).movePointRight(2).longValueExact();
            return cents > 0 ? OptionalLong.of(cents) : OptionalLong.empty();
        } catch (ArithmeticException overflow) {
            return OptionalLong.empty();
        }
    }

    /** Formata um montante em cêntimos como euros, ex.: 1250 -> "12,50€". */
    public static String format(long cents) {
        if (cents < 0) {
            throw new IllegalArgumentException("montante negativo: " + cents);
        }
        long euros = cents / 100;
        long rest = cents % 100;
        return euros + "," + (rest < 10 ? "0" : "") + rest + "€";
    }
}
