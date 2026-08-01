package pl.razem.myszy

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ImportSafetyTest {
    @Test fun acceptsInputExactlyAtConfiguredLimit() {
        val data = ByteArray(4) { it.toByte() }

        assertEquals(4, ByteArrayInputStream(data).readAtMost(4, "too large").size)
    }

    @Test fun rejectsInputAboveConfiguredLimit() {
        try {
            ByteArrayInputStream(ByteArray(5)).readAtMost(4, "too large")
            fail("Expected an exception for input above the limit")
        } catch (expected: IllegalArgumentException) {
            assertEquals("too large", expected.message)
        }
    }
}
