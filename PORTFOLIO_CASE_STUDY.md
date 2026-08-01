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

## Krótki opis do karty portfolio

Myszy to aplikacja Android do wspólnych wydatków dla pary. Skupiłem się na
prostym przepływie rozliczeń oraz prywatności: dane są chronione przez RLS,
paragony są prywatne, a powiadomienia nie ujawniają danych finansowych.
