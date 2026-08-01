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
