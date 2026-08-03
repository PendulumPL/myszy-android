# Myszy — szablony maili Supabase Auth

Ten plik zawiera bezpieczne, gotowe treści do wklejenia w Supabase:
`Authentication → Email Templates`.

Nie wpisujemy tutaj haseł, tokenów ani prawdziwych adresów prywatnej pary.
Linki pozostają generowane przez Supabase przez zmienną `{{ .ConfirmationURL }}`.

## Potwierdzenie adresu e-mail

**Subject**

`Myszy — potwierdź adres i wejdź do swojej norki`

**Body (HTML)**

```html
<div style="font-family:Arial,sans-serif;max-width:560px;margin:0 auto;background:#fff8f2;color:#2f2933;padding:32px;border-radius:24px">
  <div style="font-size:42px">🐭 🐭</div>
  <h1 style="color:#bf6f52">Witaj w norce Myszy!</h1>
  <p>Jeszcze jeden mały krok: potwierdź swój adres e-mail, żeby spokojnie zapisywać wydatki i rozliczać się we dwoje.</p>
  <p><a href="{{ .ConfirmationURL }}" style="display:inline-block;background:#bf6f52;color:white;text-decoration:none;padding:14px 22px;border-radius:14px;font-weight:bold">Potwierdź adres</a></p>
  <p style="color:#756d70;font-size:13px">Jeśli to nie Ty zakładałeś konto, możesz spokojnie zignorować tę wiadomość.</p>
  <p style="color:#756d70;font-size:13px">Spokojnie, Myszo 🐭<br>Paweł Karolak — twórca Myszy</p>
</div>
```

## Reset hasła

**Subject**

`Myszy — wracamy do Twojej norki`

**Body (HTML)**

```html
<div style="font-family:Arial,sans-serif;max-width:560px;margin:0 auto;background:#fff8f2;color:#2f2933;padding:32px;border-radius:24px">
  <div style="font-size:42px">🐭 🔑</div>
  <h1 style="color:#bf6f52">Odzyskaj dostęp do norki</h1>
  <p>Wygląda na to, że hasło schowało się w mysiej dziurze. Ustaw nowe, żeby wrócić do wspólnych rozliczeń.</p>
  <p><a href="{{ .ConfirmationURL }}" style="display:inline-block;background:#bf6f52;color:white;text-decoration:none;padding:14px 22px;border-radius:14px;font-weight:bold">Ustaw nowe hasło</a></p>
  <p style="color:#756d70;font-size:13px">Link jest jednorazowy. Jeśli nie prosiłeś o zmianę hasła, zignoruj tę wiadomość.</p>
  <p style="color:#756d70;font-size:13px">Spokojnie, Myszo 🐭<br>Paweł Karolak — twórca Myszy</p>
</div>
```

## Ważne przed użyciem

1. Najpierw wklejamy szablony w projekcie **DEV**, nie w prywatnym Supabase.
2. Wysyłamy najwyżej jeden testowy link na konto, aby nie uruchomić limitu
   `over_email_send_rate_limit`.
3. Własny adres nadawcy i pełny branding wymagają później własnego SMTP.
4. Po wklejeniu wykonujemy jeden test rejestracji i jeden test resetu hasła.
