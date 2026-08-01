# Myszy — kierunek redesignu UI/UX

## Jedno zdanie o produkcie

**Dwie myszy, jedna norka, wspólne życie do ogarnięcia.**

Myszy to prywatna, mobilna aplikacja dla pary lub małego domu. Pomaga szybko
zapisywać wspólne wydatki, rozumieć bieżący bilans i wyrównywać go bez tonu
banku, firmy ani księgowości.

## Źródła inspiracji

- Mini Finance UI Kit: układ, miękkie karty, przestrzeń, lista transakcji,
  formularze i spokojna hierarchia;
- aplikacje do dzielenia rachunków: logika płatnika, podziału 50/50, procentów,
  własnych kwot, spłat i edycji;
- Official Figma Mobile UI Kit: standardowe zachowania pól, przycisków,
  modali, bottom sheets i stref dotyku.

Materiały są inspiracją, nie wzorem do kopiowania. Myszy nie mogą wyglądać jak
klon Splitwise ani bankowy fintech.

## Osobowość wizualna

- jasne, kremowe lub ciepłe tło;
- ciepły grafit dla tekstu;
- zgaszona zieleń/sage jako kolor wiodący;
- beż i delikatna terakota jako akcenty;
- dwa subtelne kolory dla dwóch osób;
- duże promienie kart, delikatne obramowania, bardzo lekkie cienie;
- dużo oddechu, prosty czytelny font, bez neonów i fintechowych gradientów.

Motyw myszy ma być subtelnym podpisem marki: avatary, ilustracje empty state,
onboarding, ekran sukcesu i drobne easter eggi. Nie może konkurować z kwotami,
bilansami i kluczowymi działaniami.

## Architektura ekranów

### Norka (Home)

1. Nagłówek „Nasza Norka”, dwa avatary i krótki stan domu.
2. Duża karta bilansu: kwota, jasne „jesteś do przodu” / „do wyrównania” oraz
   jedno CTA „Wyrównaj”. Przy zerze: „Myszy są kwita”.
3. „Ostatnio w norce” — prosta lista ostatnich wydatków.
4. Wyraźne CTA `+ Dodaj wydatek`.

### Nawigacja

`Norka | Historia | + | Spłaty | My`

Środkowy przycisk jest główną drogą szybkiego dodawania wydatku.

### Dodawanie wydatku

Szybki bottom sheet: **kwota → opis → płatnik → podział → zapisz**.
Domyślnie 50/50. Kwota jest pierwszym i największym elementem. Dostępne muszą
pozostać podział procentowy i własne kwoty.

### Historia

Czytelne wiersze/karty, bez tabel księgowych. Filtry: wszystko, wydatki,
spłaty, miesiąc, osoba i kategoria. Wiersz zawiera nazwę, kwotę, płatnika,
podział, datę i miniaturę paragonu, jeśli istnieje.

### Spłaty

Oddzielny ekran z jedną informacją nadrzędną „Do wyrównania” i CTA
„Wyrównaj bilans”. Historia spłat jest niżej.

### Paragony i import

Paragon jest załącznikiem wydatku, nie osobnym systemem dokumentów. Import
pozostaje bardziej neutralny językowo: najpierw wybór operacji, potem dopiero
lekki charakter marki w komunikacie sukcesu.

## Mikrocopy

Zasada: **80% jasnej informacji, 20% ciepłego mysiowego charakteru**.

- nie używać: „dług”, „zalega”, „jest winna”, „nie oddała”;
- preferować: „do wyrównania”, „jesteś do przodu”, „druga osoba jest do
  przodu”, „myszy są kwita”;
- humor dopiero po informacji, nigdy kosztem drugiej osoby.

Przykłady: „Okruszek zapisany”, „W norce na razie cisza”, „Paragon schowany w
spiżarni”.

## Kolejność wdrożenia

1. Tokeny wizualne: kolory, typografia, odstępy, promienie, cienie i komponenty.
2. Norka oraz dolna nawigacja — najważniejszy ekran portfolio.
3. Bottom sheet dodawania i wybór podziału.
4. Historia, spłaty i detal wydatku z paragonem.
5. Empty states, ekrany sukcesu, mikrocopy i dostępność.
6. Screenshoty DEV oraz końcowa regresja funkcji.

Nie zmieniamy logiki bezpieczeństwa, Supabase, powiadomień ani rozliczeń tylko
po to, by zmienić wygląd. Redesign jest osobnym etapem po domknięciu testu
odzyskiwania hasła.
