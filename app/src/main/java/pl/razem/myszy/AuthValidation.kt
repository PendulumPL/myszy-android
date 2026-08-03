package pl.razem.myszy

internal fun validateRegistration(email: String, password: String, repeatedPassword: String): String? = when {
    email.isBlank() || '@' !in email || email.substringAfterLast('@').length < 3 ->
        "Podaj poprawny adres e-mail."
    password.length < 8 ->
        "Hasło musi mieć co najmniej 8 znaków."
    password != repeatedPassword ->
        "Hasła nie są takie same."
    else -> null
}

internal fun validateNewPassword(password: String, repeatedPassword: String): String? = when {
    password.length < 8 -> "Hasło musi mieć co najmniej 8 znaków."
    password != repeatedPassword -> "Hasła nie są takie same."
    else -> null
}

internal fun passwordUpdateErrorMessage(error: Throwable): String {
    val message = error.message.orEmpty().lowercase()
    return when {
        message.contains("session") || message.contains("jwt") || message.contains("token") ->
            "Sesja odzyskiwania wygasła. Poproś o nowy link i otwórz go od razu na tym urządzeniu."
        message.contains("same") || message.contains("different") ->
            "Nowe hasło musi różnić się od poprzedniego."
        else -> "Nie udało się zmienić hasła. Spróbuj ponownie z nowym linkiem."
    }
}

internal fun passwordResetErrorMessage(error: Throwable): String =
    if (error.message?.contains("over_email_send_rate_limit") == true) {
        "Wysłaliśmy już kilka wiadomości. Odczekaj chwilę i spróbuj ponownie."
    } else {
        "Nie udało się wysłać wiadomości. Sprawdź połączenie i spróbuj ponownie."
    }
