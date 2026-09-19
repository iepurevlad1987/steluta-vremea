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
StelutaWidget.kt   widget-ul: desenează, cere date, leagă cele două apăsări
RefreshService.kt  butonul ↻: locația de acum, vremea de acolo, alt comentariu
Locator.kt         unde ești, cu LocationManager (fără Google Play Services)
PlaceStore.kt      locul ales, păstrat între porniri
WeatherApi.kt      Open-Meteo, cu HttpURLConnection și org.json
WeatherStore.kt    ultima vreme știută, în SharedPreferences
Weather.kt         model + codurile WMO + ce iconiță se desenează
Quips.kt           care comentariu, când; textele sunt în res/values*/quips.xml
MainActivity.kt    ecranul, opt rânduri de text și două butoane
```

## Limba

Aplicația vorbește **limba telefonului**: româna dacă telefonul e pe română, **engleza
pentru orice altă limbă**. Nu există setare în aplicație. Android alege singur folderul de
resurse (`values-ro/` sau `values/`), iar pe Android 13+ se poate schimba doar pentru
Steluța din Setări → Aplicații → Steluța → Limbă.

Comentariile sunt în `res/values/quips.xml` (engleză) și `res/values-ro/quips.xml`
(română), câte 15–20 pe fiecare fel de vreme, cam jumătate cu vocea lui Claude: un AI
care știe că e AI. O limbă nouă înseamnă un folder nou, `values-de/` de exemplu, cu
aceleași nume de liste; codul nu se atinge.

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

**Butonul mic ↻**, de lângă comentariu, face trei lucruri deodată: află **unde ești
acum**, aduce vremea de acolo și alege **alt comentariu**. Cât lucrează, iconița devine
„⋯" și locul scrie „Se caută…". Prima apăsare, înainte să fi dat aplicației voie la
locație, deschide aplicația ca s-o ceară; de-atunci butonul merge direct din widget.

**Un tap în rest pe widget deschide aplicația.**

Singur, widget-ul se reîmprospătează o dată la 30 de minute — minimul pe care Android îl
acceptă — cu locul salvat. Comentariul se schimbă singur din oră în oră.

## Patru decizii scrise aici, ca să nu fie schimbate din greșeală

**Widget-ul se desenează de două ori la fiecare reîmprospătare.** Întâi cu ce se știa
deja, imediat; apoi cu datele noi, când răspunde rețeaua. Un widget care se golește cât
așteaptă e mai rău decât unul care arată vremea de acum zece minute.

**Timeout-ul de rețea e 8 secunde, nu 15.** Widget-ul cheamă API-ul din `goAsync()`, al
cărui buget e în jur de zece secunde. Un timeout mai lung ar însemna că Android taie
procesul exact când răspunsul era pe drum.

**Widget-ul nu te urmărește singur.** Locația se caută doar când o ceri tu: din aplicație
sau cu ↻. Altfel ar trebui permisiunea „Permite tot timpul", pe care Android o îngroapă în
două ecrane de setări și o poate retrage singur. Apăsarea pe ↻ pornește un serviciu în
prim-plan (`RefreshService`), iar pentru că apăsarea vine de la om, Android îi dă voie la
locație cu permisiunea obișnuită, „cât folosesc aplicația". Pe Android 12+ notificarea
lui apare abia după 10 secunde, deci de obicei nici nu se vede.

**Noaptea, „senin" nu se desenează cu un soare** — se desenează cu luna. Rândul de jos,
care rezumă zile întregi, cere mereu varianta de zi: o zi n-are cum să fie „noapte".

## Ce n-a fost verificat — 1.1.0

Compilează (`assembleDebug`), iar `lintDebug` trece fără erori. **Nu a fost încercat pe
un telefon**, deci trei lucruri rămân de văzut acolo:

- cum arată butonul ↻ lângă comentariu, pe un ecran adevărat;
- dacă ↻ chiar primește locația din widget pe telefonul tău. Dacă Android nu socotește
  apăsarea ca venind de la om, butonul tot aduce vremea și alt comentariu, dar pentru
  locul salvat;
- textele în engleză, cu telefonul pus pe engleză.
