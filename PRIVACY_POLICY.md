# Polityka prywatności — Myszy

_Ostatnia aktualizacja: przed pierwszym wydaniem testerskim należy wpisać datę
oraz dane kontaktowe wydawcy._

Myszy to aplikacja do prowadzenia wspólnego rozliczenia przez dwie sparowane
osoby. Aplikacja nie jest usługą bankową i nie pobiera danych logowania do banku.

## Jakie dane są przetwarzane

- adres e-mail i identyfikator konta używane do logowania;
- pseudonim w ramach wspólnego Domu;
- wydatki: opis, kwota, data, kategoria, komentarz, osoba płacąca i ustalony
  podział;
- opcjonalne zdjęcia paragonów;
- token techniczny urządzenia używany do wysłania powiadomienia push;
- opcjonalnie dane odczytane z wybranych powiadomień płatności, zanim użytkownik
  zdecyduje, czy dodać je jako wydatek.

## Po co są używane

Dane służą wyłącznie do synchronizacji wspólnego Domu, obliczania rozliczenia,
przechowywania paragonów oraz wysyłania ogólnego powiadomienia o nowym wydatku.
Powiadomienie push nie zawiera kwoty, nazwy sklepu ani osoby płacącej.

## Gdzie dane są przechowywane

Dane kont i wydatków są przechowywane w Supabase. Paragony są przechowywane w
prywatnym zasobie plików tego samego środowiska. Dostęp jest ograniczony do
członków danego Domu przez reguły Row Level Security.

Firebase Cloud Messaging służy wyłącznie do dostarczenia sygnału powiadomienia.
Nie otrzymuje szczegółów finansowych wydatku.

## Opcjonalny dostęp do powiadomień

Funkcja propozycji płatności jest domyślnie wyłączona. Po włączeniu aplikacja
przetwarza powiadomienia tylko z Alior Mobile, Gmaila dotyczące Alior oraz
Portfela Google. Użytkownik może odebrać ten dostęp w ustawieniach Androida.

Aplikacja nie odczytuje kontaktów, SMS-ów ani haseł bankowych.

## Dane lokalne i wylogowanie

Na urządzeniu mogą tymczasowo znaleźć się kolejka importu, oczekująca propozycja
płatności oraz obraz paragonu przed wysłaniem. Po wylogowaniu aplikacja usuwa
lokalną kolejkę importu i oczekujące płatności. Po udanym wysłaniu aplikacja
usuwa własną lokalną kopię zdjęcia paragonu.

## Usunięcie danych

Przed wersją publiczną aplikacja otrzyma jasną procedurę usunięcia konta i danych
Domu. W obecnej zamkniętej wersji testerskiej prośbę o usunięcie należy kierować
do wydawcy aplikacji.

## Kontakt

Przed publicznym udostępnieniem należy tu wpisać aktualny adres kontaktowy
wydawcy oraz opublikować tę politykę pod stałym publicznym adresem.
