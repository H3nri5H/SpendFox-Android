package de.h3nri5h.spendfox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun parsesGermanDecimalAmounts() {
        assertEquals(1L, Money.centsFrom("0,01"))
        assertEquals(1250L, Money.centsFrom("12,50"))
        assertEquals(1250L, Money.centsFrom("12.50"))
        assertEquals(129999L, Money.centsFrom("1.299,99 €"))
        assertEquals(129999L, Money.centsFrom("1.299,99\u00A0€"))
    }

    @Test
    fun rejectsInvalidAmounts() {
        assertNull(Money.centsFrom(""))
        assertNull(Money.centsFrom("abc"))
        assertNull(Money.centsFrom("0"))
        assertNull(Money.centsFrom("-12,50"))
        assertNull(Money.centsFrom("12,345"))
        assertNull(Money.centsFrom("12,"))
        assertNull(Money.centsFrom("1e3"))
    }
}
