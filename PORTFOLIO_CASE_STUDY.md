# Myszy — case study do portfolio

## Jednozdaniowy opis

Prywatna aplikacja Android do wspólnych wydatków i rozliczeń dla dwóch osób,
z odizolowanymi danymi, prywatnymi paragonami i powiadomieniami bez szczegółów
finansowych.

## Problem

Pary często zapisują wspólne wydatki w notatkach, arkuszach lub kilku różnych
aplikacjach. Trudno wtedy ustalić, kto zapłacił, jaki był podział i czy saldo
jest aktualne. Dodatkowym problemem jest prywatność: zwykłe powiadomienie
o wydatku może ujawnić kwotę lub sklep na ekranie blokady.

## Rozwiązanie

Myszy pozwala jednej parze prowadzić wspólny Dom:

- dodawać wydatki i ustalać podział;
- śledzić saldo oraz historię zmian;
- przechowywać opcjonalne zdjęcia paragonów;
- importować dane bankowe tylko do ręcznego zatwierdzenia;
- otrzymywać techniczne powiadomienie o zmianie bez przesyłania kwoty lub sklepu.

W najnowszym odświeżeniu interfejsu każdy wydatek pokazuje również avatar i nazwę
myszy, która go wpisała. Wydatki bieżącego użytkownika i drugiej osoby mają
odrębne, delikatne kolory, a wyrównania są oznaczone osobnym lawendowym stylem,
żeby historia była czytelna już przy szybkim przewijaniu.

Automatyczny odczyt powiadomień bankowych jest świadomie odłożony do kolejnego
update'u. W bieżącej wersji beta aplikacja pozostawia tę funkcję wyszarzoną,
aby najpierw domknąć stabilność i prywatność podstawowego przepływu.

## Najważniejsze decyzje techniczne

- Kotlin i Jetpack Compose dla aplikacji Android;
- Supabase Auth, Postgres, Storage i Edge Functions;
- Firebase Cloud Messaging wyłącznie jako sygnał odświeżenia;
- Row Level Security ograniczające dane do członków konkretnego Domu;
- limit dwóch osób na Dom i jednego Domu na konto;
- oddzielone środowiska private i DEV/demo z różnymi pakietami Android,
  Firebase i Supabase.

## Prywatność jako element projektu

W projekcie ograniczono dane na każdym etapie:

- wylogowanie czyści lokalny stan wrażliwy;
- backup systemowy aplikacji jest wyłączony;
- importy i obrazy paragonów mają limity wielkości;
- zdjęcie paragonu jest usuwane lokalnie po wysłaniu;
- FCM nie zawiera kwoty, sklepu ani osoby płacącej;
- klucze Firebase i Supabase pozostają poza repozytorium.

## Walidacja jakości

- testy jednostkowe parserów, bilansu i bezpieczeństwa importu;
- buildy debug dla private i DEV;
- build podpisanego pakietu DEV AAB;
- Android Lint dla wariantu DEV;
- plan jednej zbiorczej sesji testowej na dwóch telefonach.

## Materiały do dodania po testach

- dwa lub trzy zrzuty ekranu z fikcyjnych danych DEV;
- wykonanie zgodnie z PORTFOLIO_SCREENSHOT_PLAN.md;
- link do repozytorium GitHub;
- link do polityki prywatności;
- link do strony portfolio.

## Roadmapa produktu

Najbliższy większy update obejmie angielski interfejs i angielskie komunikaty.
W dalszej perspektywie rozważymy delikatny, nieinwazyjny model reklamowy albo
dobrowolne wsparcie projektu. Reklamy nie są częścią obecnej wersji i nie będą
oparte na treści wydatków, paragonów ani powiadomień.

## Krótki opis do karty portfolio

Myszy to aplikacja Android do wspólnych wydatków dla pary. Skupiłem się na
prostym przepływie rozliczeń oraz prywatności: dane są chronione przez RLS,
paragony są prywatne, a powiadomienia nie ujawniają danych finansowych.
## OCR paragonow — planowany kolejny update

W kolejnej iteracji chcemy dodac OCR paragonow: odczyt sklepu, kwoty i prostych
pozycji ze zdjecia, zawsze z ekranem podgladu i zatwierdzeniem przez uzytkownika.
Obecna wersja pokazuje fikcyjny paragon DEV oraz reczne dodawanie zalacznika,
ale celowo nie rozpoznaje jeszcze tresci automatycznie.

## Roadmapa po wersji Android

Po ustabilizowaniu polskiej wersji Android i zakończeniu testów planujemy:

- dodać angielski interfejs oraz angielskie komunikaty;
- przygotować wersję iOS na bazie sprawdzonego modelu wspólnej norki;
- dopiero potem rozszerzać funkcję odczytu płatności, która obecnie pozostaje oznaczona jako kolejny update.
