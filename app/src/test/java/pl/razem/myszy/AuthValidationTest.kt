package pl.razem.myszy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {
    @Test fun registration_acceptsMatchingStrongPassword() {
        assertNull(validateRegistration("tester@example.com", "bezpieczne123", "bezpieczne123"))
    }

    @Test fun registration_rejectsInvalidEmail() {
        assertEquals("Podaj poprawny adres e-mail.", validateRegistration("tester", "bezpieczne123", "bezpieczne123"))
    }

    @Test fun registration_rejectsShortPassword() {
        assertEquals("Hasło musi mieć co najmniej 8 znaków.", validateRegistration("tester@example.com", "1234567", "1234567"))
    }

    @Test fun registration_rejectsDifferentPasswords() {
        assertEquals("Hasła nie są takie same.", validateRegistration("tester@example.com", "bezpieczne123", "innehaslo123"))
    }

    @Test fun passwordChange_usesTheSameSafetyRules() {
        assertNull(validateNewPassword("nowehaslo123", "nowehaslo123"))
        assertEquals("Hasła nie są takie same.", validateNewPassword("nowehaslo123", "innehaslo123"))
    }

    @Test fun passwordReset_explainsEmailRateLimit() {
        assertEquals(
            "Wysłaliśmy już kilka wiadomości. Odczekaj chwilę i spróbuj ponownie.",
            passwordResetErrorMessage(IllegalStateException("over_email_send_rate_limit"))
        )
    }
}
