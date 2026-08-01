package pl.razem.myszy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AliorParserTest {
    @Test fun parsesCardAuthorizationFromAliorMessage() {
        val payment = AliorParser.parse(
            "Alior Bank",
            "Autoryzacja transakcji bezgotowkowej karta. Kwota transakcji: 7,98 PLN. Miejsce transakcji: ZABKA Z6872 K.1 PL"
        )
        assertNotNull(payment)
        assertEquals(7.98, payment!!.amount, 0.001)
        assertEquals("ZABKA Z6872 K.1 PL", payment.merchant)
    }

    @Test fun ignoresMessagesWithoutAmount() {
        assertEquals(null, AliorParser.parse("Alior Bank", "Nowa oferta dla Ciebie"))
    }

    @Test fun parsesGoogleWalletPayment() {
        val payment = AliorParser.parseGoogleWallet(
            "Portfel Google",
            "APTEKA S\u0141ONECZNA 15\nKwota 24,99 z\u0142 \u2013 karta Debit Mastercard zbli\u017ceniowa KJO \u2022 \u2022 8648"
        )
        assertNotNull(payment)
        assertEquals(24.99, payment!!.amount, 0.001)
        assertEquals("APTEKA S\u0141ONECZNA 15", payment.merchant)
    }

    @Test fun parsesThousandsSeparatorInAliorPayment() {
        val payment = AliorParser.parse(
            "Alior Bank",
            "Autoryzacja transakcji. Kwota transakcji: 1 234,56 PLN. Miejsce transakcji: TESTOWY SKLEP"
        )

        assertNotNull(payment)
        assertEquals(1234.56, payment!!.amount, 0.001)
    }

    @Test fun ignoresGoogleWalletMessageWithoutAmount() {
        assertEquals(null, AliorParser.parseGoogleWallet("Portfel Google", "Brak szczegółów płatności"))
    }
}
