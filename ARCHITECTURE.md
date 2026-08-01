# Architektura Myszy

## Cel

Myszy wspiera jedną parę w zapisywaniu i rozliczaniu wspólnych wydatków.
Projekt jest świadomie ograniczony do jednego Domu z maksymalnie dwiema osobami.

## Aplikacja Android

- Kotlin i Jetpack Compose odpowiadają za interfejs oraz lokalny stan ekranu.
- Supabase Auth obsługuje sesję użytkownika.
- Supabase PostgREST synchronizuje Dom, członków, wydatki i historię aktywności.
- Supabase Storage przechowuje prywatne zdjęcia paragonów.
- Firebase Cloud Messaging dostarcza techniczny sygnał, że drugi użytkownik
  powinien odświeżyć dane.

Warianty `private` i `dev` mają osobne identyfikatory pakietów, konfiguracje
Firebase i konfiguracje Supabase. Mogą współistnieć na jednym telefonie.

## Przepływ danych

```text
Android -> Supabase Auth -> RLS -> Postgres / Storage
Android -> Edge Function -> Firebase FCM -> drugi Android
```

Po dodaniu wydatku aplikacja zapisuje go w Supabase. Edge Function wyszukuje
token drugiego członka tego samego Domu i wysyła FCM wyłącznie z
`expense_id`. Druga aplikacja po otrzymaniu sygnału pobiera dane ponownie
zgodnie z RLS.

## Granice bezpieczeństwa

- RLS jest włączone na wszystkich tabelach `razem_*`.
- Funkcje tworzenia i dołączania do Domu wymagają zalogowania, blokują drugie
  członkostwo tego samego konta i ograniczają Dom do dwóch osób.
- Polityki wydatków wymagają, aby płacący był członkiem tego samego Domu.
- Bucket `receipts` nie jest publiczny; ścieżka pliku zaczyna się od identyfikatora
  Domu i jest sprawdzana przez RLS.
- Edge Function wymaga JWT oraz dodatkowo potwierdza członkostwo wywołującego.
- Klucz konta usługi Firebase występuje tylko jako sekret Edge Function.

## Dane lokalne

Aplikacja może chwilowo przechowywać kolejkę importu, oczekującą propozycję
płatności i zdjęcie paragonu przed wysłaniem. Wylogowanie usuwa wrażliwy stan,
a własna kopia zdjęcia jest usuwana po poprawnym wysłaniu.

## Granice demonstracji

Środowisko DEV używa fikcyjnych kont i danych. Jest jedynym środowiskiem
do zrzutów ekranu, GitHuba i portfolio. Prywatne konfiguracje, dane oraz
historia importu są poza repozytorium.
