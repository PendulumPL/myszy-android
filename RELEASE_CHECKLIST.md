# Myszy — kontrola przed wydaniem testerskim

## Gotowe

- [x] wylogowanie czysci lokalne dane wrazliwe;
- [x] Android backup jest wylaczony;
- [x] powiadomienia na ekranie blokady sa ogolne;
- [x] FCM z aplikacji nie przekazuje kwoty, sklepu ani platnika;
- [x] import i paragony maja limity oraz sprzatanie kopii;
- [x] RLS aktualnych tabel i blokada trzeciego czlonka Domu sa wdrozone;
- [x] stare tabele z danymi zostaly zamkniete przez RLS;
- [x] testy jednostkowe przechodza, a oba warianty Android kompiluje sie lokalnie;
- [x] Android Lint dla wariantow Private i DEV przechodzi bez bledow;
- [x] pakiet DEV release kompiluje sie lokalnie;
- [x] utworzono lokalny klucz uploadu i zbudowano podpisany pakiet DEV AAB;
- [x] przygotowano robocze materiały do wpisu Google Play;
- [x] przygotowano polityke prywatnosci i zbiorczy plan testow;
- [x] utworzono oddzielne Supabase DEV z aktualnym schematem i RLS;
- [x] utworzono oddzielne Firebase DEV oraz pakiet `pl.razem.myszy.dev`;
- [x] wdrozono funkcje `send-expense-push` i sekret FCM tylko w Supabase DEV;
- [x] nazwa widoczna pod ikona: Myszy (wariant DEV: Myszy DEV).

## Przed pierwszym buildem testerskim

- [x] dodac w aplikacji rejestracje e-mail oraz zadanie resetu hasla;
- [x] dodac obsluge powrotu z linku potwierdzajacego i odzyskiwania hasla;
- [ ] dodac `myszy-dev://auth-callback` do dozwolonych przekierowan Supabase DEV;
- [ ] przetestowac zalozenie konta, potwierdzenie e-maila i zmiane hasla w DEV;
- [x] przygotowac dwa fikcyjne konta i fikcyjny wydatek dla DEV;
- [x] przygotowac i przetestowac fikcyjny paragon;
- [x] sprawdzic w obie strony, ze powiadomienie DEV nie zawiera kwoty ani sklepu;
- [ ] wpisac dane kontaktowe i opublikowac polityke prywatnosci pod stalym adresem;
- [ ] utworzyc konto/konfiguracje Google Play Console, jesli jeszcze nie istnieje;
- [ ] zachowac zaszyfrowana kopie plikow klucza uploadu poza katalogiem projektu;
- [ ] podniesc wersje aplikacji dopiero przy faktycznym buildzie testerskim.

## Sesja testowa na telefonach - 2026-08-01

- [x] zainstalowac aktualizacje na dwoch telefonach bez utraty sesji i danych;
- [x] sprawdzic synchronizacje dodawania i edycji wydatku miedzy telefonami;
- [x] sprawdzic ogolne powiadomienie push oraz przejscie z niego do edycji;
- [x] sprawdzic zdjecie, podglad i usuniecie paragonu;
- [x] potwierdzic, ze dolne przyciski nie nachodza na nawigacje systemowa;
- [x] umiescic historie wydatkow przed historia splat;
- [ ] sprawdzic wylogowanie i brak danych poprzedniego konta;
- [ ] sprawdzic prywatnosc powiadomienia na zablokowanym ekranie;
- [ ] sprawdzic import kontrolnego PDF/XLSX i wznowienie przerwanej kolejki;
- [ ] sprawdzic opcjonalna propozycje platnosci, jesli pozostaje w zakresie wydania.

## Regresja DEV na emulatorach - 2026-08-01

- [x] oba konta pozostaly w jednym Domu po aktualizacji APK;
- [x] ten sam wydatek i bilans sa widoczne na obu Pixelach;
- [x] edycja wydatku synchronizuje sie przez Supabase;
- [x] token kazdego emulatora jest zgodny z jego wpisem DEV w bazie;
- [x] funkcja push zwraca `sent: 1` w obu kierunkach;
- [x] oba emulatory odebraly sygnal FCM i wyswietlily prywatne powiadomienie;
- [x] powiadomienie zawiera tylko ogolny tytul i prosbe o otwarcie aplikacji;
- [x] testy jednostkowe obu wariantow: 15/15, bez bledow;
- [x] lint obu wariantow: 0 bledow (61 ostrzezen technicznych na wariant).

## Przed publicznym GitHubem i portfolio

- [x] wykonac skan plikow przeznaczonych do pierwszego commita pod katem sekretow i danych prywatnych;
- [ ] opublikowac wylacznie kod i konfiguracje DEV/demo;
- [ ] zrobic screeny tylko na fikcyjnych danych;
- [ ] dopisac README, opis architektury i case study;
- [ ] dodac projekt do istniejacej strony portfolio na GitHubie.
