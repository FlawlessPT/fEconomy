package pt.flawless.fEconomy.money;

import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

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
    void format_abbreviates() {
        assertEquals("999,99€", Money.format(99999));
        assertEquals("1k€", Money.format(100000));
        assertEquals("1,5k€", Money.format(150000));
        assertEquals("1,05k€", Money.format(105000));
        assertEquals("999,99k€", Money.format(99_999_999));
        assertEquals("1M€", Money.format(100_000_000));
        assertEquals("2,34M€", Money.format(234_567_890));
        assertEquals("1B€", Money.format(100_000_000_000L));
    }

    @Test
    void format_longMaxValue() {
        assertEquals("92233720,36B€", Money.format(Long.MAX_VALUE));
    }

    @Test
    void format_negativeThrows() {
        assertThrows(IllegalArgumentException.class, () -> Money.format(-1));
    }

    @Test
    void parse_validAmounts() {
        assertEquals(OptionalLong.of(1200), Money.parse("12"));
        assertEquals(OptionalLong.of(1250), Money.parse("12,5"));
        assertEquals(OptionalLong.of(1250), Money.parse("12.50"));
        assertEquals(OptionalLong.of(1250), Money.parse("12,50"));
        assertEquals(OptionalLong.of(1), Money.parse("0,01"));
        assertEquals(OptionalLong.of(5), Money.parse("0.05"));
        assertEquals(OptionalLong.of(1250), Money.parse("012,50"));
        assertEquals(OptionalLong.of(100000), Money.parse("1k"));
        assertEquals(OptionalLong.of(150000), Money.parse("1,5K"));
        assertEquals(OptionalLong.of(250_000_000), Money.parse("2.5m"));
        assertEquals(OptionalLong.of(300_000_000_000L), Money.parse("3B"));
        assertEquals(OptionalLong.of(123450), Money.parse("1,2345k"));
    }

    @Test
    void parse_invalidAmounts() {
        for (String invalid : new String[]{null, "", " ", "0", "0,00", "-5", "-0,5", "+5", "1,234", "1.234,5",
                "1,", ",5", "1,2,3", "abc", "k", "1,2345678k", "1kk", "1e3", "12€", " 12", "12 ", "NaN", "٣"}) {
            assertEquals(OptionalLong.empty(), Money.parse(invalid), "devia rejeitar: " + invalid);
        }
    }

    @Test
    void parseAllowZero_acceptsZeroAndPositive() {
        assertEquals(OptionalLong.of(0), Money.parseAllowZero("0"));
        assertEquals(OptionalLong.of(0), Money.parseAllowZero("0,00"));
        assertEquals(OptionalLong.of(50), Money.parseAllowZero("0,5"));
        assertEquals(OptionalLong.of(1250), Money.parseAllowZero("12.50"));
        assertEquals(OptionalLong.empty(), Money.parse("0"));
        assertEquals(OptionalLong.empty(), Money.parse("0,00"));
    }

    @Test
    void parseAllowZero_rejectsGarbageAndNegatives() {
        for (String invalid : new String[]{null, "", "-1", "-0", "+1", "abc", "1,234", "1e3", " 1", "99999999999999999999999"}) {
            assertEquals(OptionalLong.empty(), Money.parseAllowZero(invalid), "devia rejeitar: " + invalid);
        }
    }

    @Test
    void parse_overflowIsRejected() {
        assertEquals(OptionalLong.of(Long.MAX_VALUE), Money.parse("92233720368547758,07"));
        assertEquals(OptionalLong.empty(), Money.parse("92233720368547758,08"));
        assertEquals(OptionalLong.empty(), Money.parse("99999999999999999999999"));
    }
}
