# Raport regresji DEV

Data: 2026-08-01

Zakres: wspolny kod wariantow Private i DEV oraz dzialajace uslugi chmurowe wylacznie projektu DEV. Nie zmieniano chmury prywatnej i niczego nie publikowano.

## Wynik

- kompilacja `PrivateDebug` i `DevDebug`: pozytywna;
- testy jednostkowe `PrivateDebug`: 15/15;
- testy jednostkowe `DevDebug`: 15/15;
- Android Lint: 0 bledow, 61 ostrzezen na wariant;
- start po aktualizacji APK z zachowaniem obu sesji: pozytywny;
- zgodnosc kontrolnego wydatku i bilansu na obu emulatorach: pozytywna;
- wywolanie funkcji FCM z konta A do konta B: `sent: 1`, odbior pozytywny;
- wywolanie funkcji FCM z konta B do konta A: `sent: 1`, odbior pozytywny;
- prywatnosc powiadomienia: pozytywna (`VISIBILITY_PRIVATE`, bez kwoty, sklepu i platnika).

## Naprawy wykonane podczas audytu

1. Ponowna rejestracja FCM nie blokuje sie juz do restartu po chwilowym bledzie Firebase lub Supabase.
2. Rekord wydatku jest odpinany/usuwany przed kasowaniem pliku paragonu. Awaria sieci nie pozostawi juz aktywnego wydatku ze wskazaniem na skasowane zdjecie.
3. Edycja samego opisu, daty lub komentarza historycznego wpisu zachowuje dokladne udzialy groszowe z Settle Up. Zmiana kwoty lub procentu swiadomie przelicza podzial.
4. Dodano bezpieczne logi odbioru FCM bez kwot, nazw sklepow, tokenow i danych kont.

## Uwaga o emulatorach

Po starcie oba emulatory mialy wylaczone Wi-Fi. Synchronizacja Supabase dzialala przez symulowana siec komorkowa, ale kanal FCM nie dostarczal wiadomosci. Po wlaczeniu zwalidowanego Wi-Fi oba kierunki powiadomien przeszly natychmiast. To byla cecha srodowiska testowego, nie blad danych ani parowania Domu.

## Test na fizycznych telefonach

- aktualizacja APK na dwoch telefonach bez utraty sesji i danych: pozytywna;
- synchronizacja dodawania i edycji wydatku: pozytywna;
- ogolne powiadomienie push oraz przejscie z niego do edycji: pozytywne;
- zdjecie, podglad i usuniecie paragonu: pozytywne;
- poprawka kolejnosci historii i bezpiecznego odstepu od nawigacji systemowej: zweryfikowana;

## Pozostalo

- import kontrolnego PDF/XLSX oraz wznowienie przerwanej kolejki;
- wylogowanie i potwierdzenie braku danych poprzedniego konta;
- zachowanie prywatnego powiadomienia na prawdziwym ekranie blokady;
- opcjonalny odczyt zatwierdzonego powiadomienia Alior/Portfela Google.
