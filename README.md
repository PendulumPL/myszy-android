# Myszy — Android

Myszy to aplikacja Android dla dwóch osób prowadzących wspólne rozliczenia.
Pozwala dodawać wydatki, ustalać podział, przechowywać opcjonalne paragony oraz
sprawdzać bieżące saldo pary.

## Stan projektu

Projekt ma dwa całkowicie oddzielone warianty:

- **private** — prywatna instalacja pary, pakiet `pl.razem.myszy`;
- **dev** — bezpieczne środowisko demo/testowe, pakiet
  `pl.razem.myszy.dev`, widoczne jako „Myszy DEV”.

Wersja DEV używa osobnych projektów Supabase i Firebase. Nie należy przenosić
do niej prywatnych kont, wydatków, importów ani paragonów.

## Najważniejsze funkcje

- logowanie przez Supabase Auth;
- jeden wspólny Dom dla maksymalnie dwóch osób;
- wydatki, kategorie, komentarze, własne udziały i historia aktywności;
- opcjonalne zdjęcia paragonów w prywatnym Storage;
- import plików bankowych do ręcznego zatwierdzenia;
- opcjonalne propozycje płatności z wybranych powiadomień;
- techniczne powiadomienia push Firebase bez kwoty, sklepu i płatnika.

## Prywatność i bezpieczeństwo

- Row Level Security ogranicza dane do członków danego Domu;
- konto nie może należeć do więcej niż jednego Domu, a Dom ma maksymalnie
  dwóch członków;
- po wylogowaniu aplikacja czyści lokalną kolejkę importu i oczekujące
  propozycje płatności;
- systemowy backup danych aplikacji jest wyłączony;
- importy i zdjęcia paragonów mają limity wielkości;
- powiadomienia na ekranie blokady nie ujawniają danych finansowych.

Szczegóły znajdują się w [PRIVACY_POLICY.md](PRIVACY_POLICY.md) oraz
[ARCHITECTURE.md](ARCHITECTURE.md).

## Lokalna konfiguracja

Konfiguracja i klucze pozostają wyłącznie lokalnie i są ignorowane przez Git:

- `supabase.properties` — prywatny Supabase;
- `supabase-dev.properties` — Supabase DEV;
- `app/src/private/google-services.json` — prywatny Firebase;
- `app/src/dev/google-services.json` — Firebase DEV.

Szablony bez prawdziwych wartości:

- `supabase.properties.example`;
- `supabase-dev.properties.example`;
- `supabase/config.example`.

## Budowanie

Wymagane są Android Studio, Android SDK i lokalne konfiguracje odpowiedniego
wariantu.

```powershell
.\gradlew.bat testDevDebugUnitTest assembleDevDebug
.\gradlew.bat assemblePrivateDebug
.\gradlew.bat bundleDevRelease
```

APK DEV powstaje w:
`app/build/outputs/apk/dev/debug/app-dev-debug.apk`.
Podpisany pakiet AAB DEV powstaje w:
`app/build/outputs/bundle/devRelease/app-dev-release.aab`.

## Dokumentacja robocza

- [DEV_SETUP.md](DEV_SETUP.md) — odizolowane środowisko demo;
- [TEST_PLAN.md](TEST_PLAN.md) — jedna zbiorcza sesja testowa na dwóch telefonach;
- [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) — status wydania testerskiego;
- [ARCHITECTURE.md](ARCHITECTURE.md) — opis techniczny do przyszłego portfolio.
- [PLAY_STORE_DRAFT.md](PLAY_STORE_DRAFT.md) — robocze materiały do Google Play.
- [PORTFOLIO_CASE_STUDY.md](PORTFOLIO_CASE_STUDY.md) — szkic opisu do portfolio.
- [PORTFOLIO_SCREENSHOT_PLAN.md](PORTFOLIO_SCREENSHOT_PLAN.md) — plan screenów DEV.

Projekt nie jest jeszcze publicznie wydany. Przed publikacją potrzebne są testy
na fikcyjnych danych, stały adres polityki prywatności, końcowy skan repozytorium
i materiały portfolio bez danych prywatnych.
