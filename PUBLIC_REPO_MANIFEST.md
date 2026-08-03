# Myszy — manifest bezpiecznej publikacji

## Do pierwszego publicznego repozytorium

- kod aplikacji i testy;
- `README.md`, `PORTFOLIO_CASE_STUDY.md`, `PORTFOLIO_SCREENSHOT_PLAN.md`;
- dokumentacja testów i checklisty;
- fikcyjny PDF w `output/pdf/`;
- screeny DEV z `portfolio/screenshots-dev/`;
- migracje SQL opisujące schemat i dane demonstracyjne.

## Poza repozytorium

- `keystore.properties` i pliki kluczy podpisujących;
- `app/src/private/google-services.json`;
- `app/src/dev/google-services.json` (konfiguracja lokalna; repozytorium korzysta z instrukcji i placeholdera);
- `supabase.properties`, `supabase-dev.properties`;
- lokalne APK/AAB, cache Gradle i dane emulatorów;
- prawdziwe adresy e-mail, tokeny, sekrety FCM i dane prywatnej pary.

## Kontrola przed push

1. Uruchomić `git status --short` i sprawdzić, czy nie ma konfiguracji lokalnej.
2. Uruchomić skan sekretów po plikach przeznaczonych do commita.
3. Obejrzeć miniatury wszystkich screenów z `portfolio/screenshots-dev/`.
4. Upewnić się, że opis jasno oznacza funkcję odczytu powiadomień jako roadmapę kolejnego update'u.
5. Dopiero wtedy wykonać pierwszy commit i push do GitHuba.

Publikacja nie została jeszcze wykonana.

