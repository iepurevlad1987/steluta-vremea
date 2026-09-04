# Steluța

O aplicație de vreme pentru mine, în stilul CARROT Weather: oră mare, temperatură mare,
și un comentariu care nu se poartă frumos.

Instalabilă pe telefon ca aplicație normală (PWA), fără magazin și fără cont.

## Ideea, așa cum a fost cerută — 2026-09-04

- **Stack:** HTML + CSS + JS simplu. Fără framework, fără pas de build, fără `node_modules`.
  Peste un an trebuie să se poată deschide un fișier și înțeles ce face.
- **Date:** [Open-Meteo](https://open-meteo.com) — gratuit și **fără cheie de API**. Asta e
  ce face proiectul ăsta ușor: n-are niciun secret de ascuns, deci poate sta pe orice
  găzduire statică, publică.
- **Locație:** geolocation la prima deschidere, cu rezervă pe **Zalău, Sălaj**
  (47.1911, 23.0574).
- **Design:** portocaliul Claude (`#DA7756`) ca accent, **sunburst-ul în loc de soare** —
  unsprezece raze, forma generată din cod ca să fie simetrică la orice mărime — și
  **luna noaptea**, fiindcă un soare la ora două dimineața ar fi prima minciună a
  ecranului. Restul iconițelor minimaliste și gri. Dark mode din start, nu ca opțiune.
- **Comentarii cu personalitate**, 8–10 variante per fel de vreme, ca să nu se repete.
- **PWA adevărată:** `manifest.json` + service worker, „Add to Home Screen" pe Android
  să pornească fullscreen, cu iconiță proprie.

## Fișiere

```
index.html      structura ecranului
style.css       tot ce ține de aspect; culorile sunt variabile CSS sus de tot
app.js          vremea, ceasul, comentariile, locația
manifest.json   ca Android s-o trateze ca aplicație
sw.js           pornire fără internet
icons/          192, 512 și una „maskable" pentru Android
```

## Două implementări, în același loc

| Unde | Ce e | Ce poate în plus |
|---|---|---|
| rădăcina | **PWA** — HTML + CSS + JS | se instalează din Chrome, merge pe orice telefon, fără build |
| [`android/`](android/) | **aplicație Android nativă**, Kotlin | **widget pe ecranul de start**, plus locație salvată |
| [`wallpapers/`](wallpapers/) | patru fundaluri 1440×3120 | aceeași paletă, același sunburst |

**De ce există și varianta nativă.** PWA-ul face tot ce face cea nativă, în afară de un
singur lucru: **nu poate pune un widget pe ecranul de start.** Widget-urile Android cer un
`AppWidgetProvider`, adică cod nativ. Nu e o limitare a implementării, e a platformei —
nicio aplicație web nu poate.

Detaliile aplicației native, inclusiv cum se compilează, sunt în [`android/README.md`](android/README.md).

## Cum o rulezi

**Nu deschide `index.html` direct cu dublu-clic.** Din `file://`, service worker-ul nu
pornește (îi trebuie context sigur) și nici geolocation-ul nu merge — ai vedea vremea din
Zalău și n-ai putea instala aplicația. Îți trebuie un server, oricât de mic:

```bash
cd vremea
python -m http.server 8000
```

Apoi `http://localhost:8000`. Chrome tratează `localhost` ca sigur, deci merge tot.

**Pe telefon**, cel mai simplu e GitHub Pages: pui folderul într-un repo, Settings → Pages →
branch `main`, și în două minute ai o adresă `https://` de pe care Chrome îți oferă
„Instalează aplicația".

## Trei decizii scrise aici, ca să nu fie schimbate din greșeală

**Vremea nu se pune niciodată în cache-ul service worker-ului.** Fișierele aplicației da —
de aia pornește instant. Dar un răspuns meteo servit din cache ar fi exact defectul pe care
nu-l vrei: o aplicație care arată încrezătoare temperatura de ieri. Când nu e internet,
`app.js` scoate ce a salvat el în `localStorage` și **scrie pe ecran de când sunt datele**.

**Geolocation-ul nu blochează nimic.** Dacă omul nu răspunde la permisiune în 8 secunde, se
pleacă pe Zalău. Un ecran gol care așteaptă o permisiune e mai rău decât vremea dintr-un
oraș apropiat.

**Comentariul se schimbă din oră în oră, nu la fiecare reîmprospătare.** Unul care sare la
fiecare 10 minute distrage; unul care nu se schimbă deloc devine invizibil în două zile.
Alegerea vine dintr-o sămânță făcută din dată + oră, deci e stabilă și totuși variază.

Și o regulă pentru comentarii: **vremea grea bate temperatura.** Când plouă cu găleata, pe
nimeni nu-l interesează că sunt 18 grade plăcute.

## De adăugat, dacă merită

- grafic pe ore pentru ziua curentă (Open-Meteo dă `hourly` în același apel)
- răsărit / apus (`daily.sunrise`, `daily.sunset`)
- mai multe locuri salvate, cu swipe între ele
- comentarii speciale pentru zile anume (prima ninsoare, 1 martie, ziua ta)
