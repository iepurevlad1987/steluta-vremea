# Steluța — widget de vreme pentru Android

Aplicație Android nativă, făcută pentru un singur lucru: **un widget de vreme pe ecranul
de start**, care să înlocuiască pe cel din fabrică.

Ecranul aplicației există, dar e secundar — arată aceleași date, ca să se vadă dintr-o
privire că vin.

## De ce nativ și nu PWA

Varianta web a acestei aplicații ([steluta-vremea](https://github.com/iepurevlad1987/steluta-vremea))
face tot ce face asta, în afară de un singur lucru: **nu poate pune un widget pe ecranul
de start.** Widget-urile Android cer un `AppWidgetProvider`, adică cod nativ. Nu e o
limitare a implementării, e a platformei — nicio aplicație web nu poate.

## Ce e înăuntru

```
StelutaWidget.kt   widget-ul: desenează, cere date, răspunde la tap
WeatherApi.kt      Open-Meteo, cu HttpURLConnection și org.json
WeatherStore.kt    ultima vreme știută, în SharedPreferences
Weather.kt         model + codurile WMO + ce iconiță se desenează
Quips.kt           98 de comentarii, pe 13 feluri de vreme
MainActivity.kt    ecranul, opt rânduri de text și un buton
```

**Nicio dependință în afară de `core-ktx`.** Fără Compose, Hilt, Room, Retrofit sau
Moshi: rețeaua se face cu `HttpURLConnection`, JSON-ul cu `org.json`, widget-ul cu
`RemoteViews` — toate sunt în Android de la început. Mai puține piese înseamnă mai puține
lucruri care se strică la primul build.

Versiunile de Gradle, AGP și Kotlin sunt aceleași ca la VFit, tot dinadins: sunt deja
descărcate pe calculatorul pe care se compilează.

## Cum îl compilezi

**În Android Studio:** File → Open → alegi folderul `steluta-android` → aștepți Gradle
sync → **View → Tool Windows → Terminal**, apoi:

```
gradlew.bat assembleDebug
```

APK-ul iese în `app\build\outputs\apk\debug\app-debug.apk`. Îl trimiți pe telefon și îl
deschizi.

## Cum pui widget-ul

Ții apăsat pe un loc gol de pe ecranul de start → **Widget-uri** → cauți **Steluța** →
tragi widget-ul unde vrei. Se poate redimensiona trăgând de colțuri.

**Un tap pe widget cere date noi.** În rest se reîmprospătează singur o dată la 30 de
minute — ăsta e minimul pe care Android îl acceptă, orice valoare mai mică e ridicată
tăcut la 30.

## Patru decizii scrise aici, ca să nu fie schimbate din greșeală

**Widget-ul se desenează de două ori la fiecare reîmprospătare.** Întâi cu ce se știa
deja, imediat; apoi cu datele noi, când răspunde rețeaua. Un widget care se golește cât
așteaptă e mai rău decât unul care arată vremea de acum zece minute.

**Timeout-ul de rețea e 8 secunde, nu 15.** Widget-ul cheamă API-ul din `goAsync()`, al
cărui buget e în jur de zece secunde. Un timeout mai lung ar însemna că Android taie
procesul exact când răspunsul era pe drum.

**Locul e scris în cod** (`Place` din `WeatherApi.kt`), nu cerut de la GPS. Un widget de
pe ecranul de start ar trebui să ceară permisiune de localizare *și* să o poată folosi în
fundal, ceea ce Android restricționează tot mai tare. Pentru o aplicație personală,
într-un oraș, coordonatele nu se schimbă niciodată. Dacă te muți, schimbi acolo.

**Noaptea, „senin" nu se desenează cu un soare** — se desenează cu luna. Rândul de jos,
care rezumă zile întregi, cere mereu varianta de zi: o zi n-are cum să fie „noapte".

## Ce n-a fost verificat

**Codul ăsta n-a fost compilat niciodată** înainte să ajungă la tine: a fost scris
într-un mediu fără SDK Android. Ce s-a putut verifica:

- toate cele 22 de fișiere XML sunt valide, și fiecare referință `@drawable`, `@color`,
  `@string`, `@layout` chiar există;
- `Weather.kt` și `Quips.kt` compilează pe kotlinc curat, iar alegerea comentariului și
  a iconiței a fost rulată pe nouă feluri de vreme.

Ce **nu** s-a putut verifica: layout-ul widget-ului pe un ecran adevărat, și apelul la
Open-Meteo. Prima compilare e și primul test real.
