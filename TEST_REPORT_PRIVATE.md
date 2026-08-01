# Raport testu wariantu Private

Data: 2026-08-01

Zakres: aktualizacja wariantu Private i wspolne uzycie aplikacji na dwoch fizycznych telefonach. Raport nie zawiera adresow kont, identyfikatorow Domu, kwot ani prawdziwych danych wydatkow.

## Wynik

- instalacja aktualizacji bez usuwania poprzedniej aplikacji: pozytywna;
- zachowanie sesji, Domu i dotychczasowych danych: pozytywne;
- dodanie wydatku i synchronizacja na drugim telefonie: pozytywne;
- edycja kwoty i synchronizacja zmiany: pozytywne;
- odbior ogolnego powiadomienia push: pozytywny;
- przejscie z powiadomienia do edycji wydatku: pozytywne;
- dodanie i podglad paragonu na drugim telefonie: pozytywne;
- usuniecie paragonu i brak dostepu do usunietego pliku: pozytywne;
- kolejnosc sekcji: historia wydatkow znajduje sie przed historia splat;
- dolna nawigacja aplikacji zachowuje odstep od przyciskow systemowych Androida.

## Pozostalo przed zamknieciem pelnej regresji

- import kontrolnego PDF i XLSX oraz wznowienie przerwanej kolejki;
- wylogowanie i potwierdzenie usuniecia lokalnych danych poprzedniego konta;
- podglad tresci powiadomienia na prawdziwym zablokowanym ekranie;
- opcjonalny test odczytu zatwierdzonego powiadomienia Alior/Portfela Google.

Te punkty nie blokuja dalszego dopracowania interfejsu, ale powinny zostac wykonane przed udostepnieniem aplikacji szerszej grupie testerow.
