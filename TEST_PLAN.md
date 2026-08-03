# Myszy — zbiorczy test na dwóch telefonach

Wykonaj ten plan dopiero po przygotowaniu jednego wspólnego builda testerskiego.
Nie używaj realnych wrażliwych opisów ani zdjęć dokumentów podczas testu.

## Przygotowanie

- dwa telefony z aktualnym buildem tej samej wersji aplikacji;
- dwa istniejące konta należące do jednego Domu;
- połączenie z internetem na obu telefonach;
- jedno fikcyjne zdjęcie paragonu i mały testowy PDF lub XLSX.

### Fikcyjny plik do importu

Do testu importu uĹĽyj pliku `output/pdf/portfolio-demo-bank-statement.pdf`.
To plik wyĹ‚Ä…cznie demonstracyjny: zawiera cztery sztuczne pĹ‚atnoĹ›ci i nie
zawiera danych bankowych ani osobowych.

## 1. Logowanie i synchronizacja

1. Zaloguj każde konto na innym telefonie.
2. Na telefonie A dodaj wydatek testowy.
3. Sprawdź na telefonie B, czy pojawia się po odświeżeniu.
4. Edytuj opis na telefonie B i sprawdź zmianę na telefonie A.

**Wynik oczekiwany:** oba telefony widzą tę samą pozycję; druga osoba może
edytować wpis, ale osoba spoza Domu nie ma dostępu.

## 2. Bilans i spłata

1. Dodaj fikcyjny wydatek z podziałem 60/40.
2. Dodaj wydatek z własnymi udziałami.
3. Dodaj spłatę.
4. Sprawdź saldo na obu telefonach.

**Wynik oczekiwany:** saldo po obu stronach jest jednakowe, a zwykły wydatek ze
słowem „rozliczenie” w opisie nie jest traktowany jak spłata.

## 3. Paragon

1. Dodaj zdjęcie testowego paragonu do nowego wydatku.
2. Otwórz paragon na drugim telefonie.
3. Zmień zdjęcie, a potem usuń je.

**Wynik oczekiwany:** aktualne zdjęcie jest dostępne dla pary; stare zdjęcie nie
zostaje jako zbędna kopia po podmianie lub usunięciu.

## 4. Import pliku

1. Zaimportuj mały testowy PDF albo XLSX.
2. Zatwierdź jedną pozycję, zmodyfikuj drugą i odrzuć trzecią.
3. Wyloguj się przed zakończeniem kolejki, a potem zaloguj ponownie.

**Wynik oczekiwany:** dodana pozycja pojawia się tylko raz; lokalna kolejka
importu znika po wylogowaniu.

## 5. Powiadomienia

1. Zezwól na zwykłe powiadomienia Androida na obu telefonach.
2. Dodaj wydatek na telefonie A.
3. Sprawdź ogólne powiadomienie na telefonie B, także na ekranie blokady.
4. Nie włączaj odczytu powiadomień bankowych — funkcja jest wyszarzona i
   odłożona do kolejnego update'u.

**Wynik oczekiwany:** powiadomienie nie pokazuje kwoty, sklepu ani danych
drugiej osoby. Automatyczne odczytywanie płatności nie jest obecnie częścią
wydania testerskiego.

## 6. Wylogowanie

1. Na jednym telefonie rozpocznij import albo pozostaw oczekującą propozycję.
2. Wyloguj się.
3. Zamknij aplikację i otwórz ją ponownie.

**Wynik oczekiwany:** aplikacja pokazuje ekran logowania; nie pokazuje poprzednich
wydatków, kolejki importu ani oczekującej propozycji.

## Raport po teście

Zapisz tylko:

- numer builda;
- wynik każdego z sześciu obszarów: zaliczone / problem;
- krótki opis problemu bez prawdziwych danych finansowych;
- zrzut ekranu wyłącznie z fikcyjnymi danymi, jeśli jest potrzebny.
## Test fizyczny: profil i kolor

Po instalacji prywatnego release na telefonie sprawdzić ręcznie:

1. `Mój profil` → `Zmień motyw myszy`: wybrany motyw ma być widoczny po powrocie do norki.
2. `Mój profil` → `Zmień kolor profilu`: kolor ma zmienić akcent profilu i przyciski także dla Pawła.
3. Zamknąć i ponownie otworzyć aplikację: avatar i kolor muszą pozostać zapisane.

**Wynik 2026-08-02:** kolor profilu na fizycznym telefonie zapisuje się poprawnie
po zamknięciu i ponownym uruchomieniu aplikacji.

**Wynik 2026-08-02:** avatar profilu również pozostaje zapisany po restarcie.
