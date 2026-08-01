# Myszy — przygotowanie srodowiska DEV/demo

Cel: osobna aplikacja i osobna baza do zrzutow ekranu, GitHuba oraz
portfolio. Nie przenosimy do niej prawdziwych wydatkow, paragonow ani kont
prywatnej pary.

## 1. Oddzielny Supabase

1. Utworz nowy projekt Supabase przeznaczony tylko dla DEV/demo.
2. Przy tworzeniu projektu wylacz automatyczne wystawianie nowych tabel przez
   Data API i wlacz automatyczne RLS.
3. W SQL Editor projektu DEV uruchom raz plik
   [supabase/schema-dev.sql](supabase/schema-dev.sql).

Ten jeden plik tworzy aktualna strukture Myszy, prywatny bucket na paragony,
RLS oraz zasady dostepu. Nie uruchamiaj historycznych plikow `schema.sql`
i `v12`–`v25`: sa zapisem rozwoju prywatnej bazy, a nie instalatorem DEV.

## 2. Oddzielny Firebase

1. Utworz nowy projekt Firebase dla DEV.
2. Dodaj aplikacje Android z osobnym identyfikatorem pakietu:
   `pl.razem.myszy.dev`.
3. Pobierz nowy `google-services.json` tylko dla tej aplikacji.
4. Utworz osobne konto uslugi dla funkcji push i skonfiguruj je tylko jako
   sekret funkcji Supabase DEV.

Nie dodawaj `google-services.json` ani konta uslugi do Git.

## 3. Konfiguracja lokalna

1. Skopiuj `supabase-dev.properties.example` jako `supabase-dev.properties`.
2. Wpisz adres i klucz publishable projektu DEV.
3. Zachowaj prywatne wartosci poza repozytorium. Wersja prywatna nadal uzywa
   osobnego pliku `supabase.properties`.

## 4. Rejestracja i linki e-mail

1. W Supabase DEV wejdz w `Authentication` -> `URL Configuration`.
2. Dodaj do dozwolonych adresow przekierowania:
   `myszy-dev://auth-callback`.
3. W `Authentication` -> `Providers` pozostaw wlaczony provider Email i
   wymagaj potwierdzenia adresu przed pierwszym logowaniem.
4. Nie dodawaj adresu wariantu prywatnego do projektu DEV. Wariant Private
   uzywa osobnego przekierowania `myszy://auth-callback` i musi byc
   skonfigurowany w swoim projekcie Supabase.

Po kliknieciu linku potwierdzajacego aplikacja DEV otworzy sie automatycznie.
Link odzyskiwania hasla otworzy bezpieczny ekran ustawienia nowego hasla.

## 5. Dane demonstracyjne

- utworz dwa nowe konta testowe;
- utworz jeden Dom z fikcyjna nazwa;
- dodaj kilka bezpiecznych, fikcyjnych wydatkow i sztuczny paragon;
- nie importuj historii banku ani prawdziwych powiadomien o platnosci.

## 6. Kontrola przed publicznym uzyciem

- sprawdz RLS wszystkich tabel `razem_*`;
- wdroz funkcje `send-expense-push` z
  `supabase/functions/send-expense-push`;
- sprawdz, czy powiadomienie DEV nie zawiera kwoty ani sklepu;
- dopiero wtedy rob zrzuty ekranu i przygotowuj repozytorium publiczne.
