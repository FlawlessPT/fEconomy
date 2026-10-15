package pt.flawless.fEconomy.money;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    void format_zero() {
        assertEquals("0,00€", Money.format(0));
    }

    @Test
    void format_fiveCents() {
        assertEquals("0,05€", Money.format(5));
    }

    @Test
    void format_exactEuro() {
        assertEquals("1,00€", Money.format(100));
    }

    @Test
    void format_ninetyNineAndOneHundredOne() {
        assertEquals("0,99€", Money.format(99));
        assertEquals("1,01€", Money.format(101));
    }

    @Test
    void format_regularAmount() {
        assertEquals("12,50€", Money.format(1250));
    }

    @Test
    void format_noThousandsSeparator() {
        assertEquals("1000,00€", Money.format(100000));
    }

    @Test
    void format_longMaxValue() {
        assertEquals("92233720368547758,07€", Money.format(Long.MAX_VALUE));
    }

    @Test
    void format_negativeThrows() {
        assertThrows(IllegalArgumentException.class, () -> Money.format(-1));
    }
}
