package pt.flawless.fEconomy.money;

public final class Money {

    private Money() {
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
