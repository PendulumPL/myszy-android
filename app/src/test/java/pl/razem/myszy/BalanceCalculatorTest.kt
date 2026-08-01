package pl.razem.myszy

import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceCalculatorTest {
    @Test fun editingOnlyMetadataPreservesImportedExactShares() {
        val imported = Expense(
            id = "1",
            merchant = "Zakupy",
            amount = 100.0,
            pawel = 60,
            payer = "Paweł",
            receipt = null,
            pawelShare = 37.0,
            aniaShare = 63.0
        )

        assertEquals(37.0, exactSharesAfterEdit(imported, 100.0, 60).first!!, 0.001)
        assertEquals(63.0, exactSharesAfterEdit(imported, 100.0, 60).second!!, 0.001)
    }

    @Test fun changingAmountDropsImportedExactSharesSoNewSplitCanBeCalculated() {
        val imported = Expense("1", "Zakupy", 100.0, 60, "Paweł", null, pawelShare = 37.0, aniaShare = 63.0)

        assertEquals(null, exactSharesAfterEdit(imported, 120.0, 60).first)
        assertEquals(null, exactSharesAfterEdit(imported, 120.0, 60).second)
    }

    @Test fun assignsTheOtherPersonsShareWhenPawelPaid() {
        val expense = Expense("1", "Zakupy", 100.0, 60, "Pawe\u0142", null)

        assertEquals(40.0, calculatePawelBalance(listOf(expense)), 0.001)
    }

    @Test fun assignsPawelsShareWhenAniaPaid() {
        val expense = Expense("1", "Zakupy", 100.0, 60, "Ania", null)

        assertEquals(-60.0, calculatePawelBalance(listOf(expense)), 0.001)
    }

    @Test fun keepsExactHistoricalSharesInsteadOfRecalculatingThePercentage() {
        val expense = Expense(
            id = "1",
            merchant = "Zakupy",
            amount = 100.0,
            pawel = 60,
            payer = "Pawe\u0142",
            receipt = null,
            pawelShare = 37.0,
            aniaShare = 63.0
        )

        assertEquals(63.0, calculatePawelBalance(listOf(expense)), 0.001)
    }

    @Test fun settlementByPawelReducesHisDebt() {
        val settlement = Expense("1", "Spłata rozliczenia", 25.0, 100, "Pawe\u0142", null)

        assertEquals(25.0, calculatePawelBalance(listOf(settlement)), 0.001)
    }

    @Test fun ordinaryExpenseWithSettlementWordIsNotTreatedAsARepayment() {
        val expense = Expense("1", "Rozliczenie rachunku za prąd", 100.0, 60, "Pawe\u0142", null)

        assertEquals(40.0, calculatePawelBalance(listOf(expense)), 0.001)
    }

    @Test fun acceptsExplicitSettlementWithoutPolishCharacters() {
        val settlement = Expense("1", "Splata rozliczenia", 20.0, 100, "Pawe\u0142", null)

        assertEquals(20.0, calculatePawelBalance(listOf(settlement)), 0.001)
    }
}
