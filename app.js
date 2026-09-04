/* ==========================================================================
   Steluța — vremea, pe scurt.

   Fara framework, fara build, fara chei de API. Open-Meteo e gratuit si nu cere
   inregistrare, deci aplicatia asta poate sta pe orice gazduire statica si nu
   are ce secret sa piarda.
   ========================================================================== */

'use strict';

/* Zalău, Sălaj. Rezerva cand geolocatia e refuzata sau nu merge (pe `file://`
   Chrome nu o da deloc - contextul nu e sigur). Aplicatia trebuie sa arate
   ceva, nu o eroare. */
const FALLBACK = { lat: 47.1911, lon: 23.0574, name: 'Zalău' };

/* La cat se reincarca singura. Open-Meteo actualizeaza din ora in ora; sub 10
   minute ar fi doar trafic degeaba. */
const REFRESH_MS = 10 * 60 * 1000;

const CACHE_KEY = 'steluta:last';
const PLACE_KEY = 'steluta:place';

/* ==========================================================================
   Iconițe
   ========================================================================== */

/* Ziua, sunburst-ul portocaliu tine locul soarelui. **Noaptea e luna** - un soare
   la ora doua dimineata ar fi prima minciuna a ecranului.

   Portocaliul apare doar aici si pe fulgerul din furtuna; restul iconitelor sunt
   gri, ca sa se vada dintr-o privire cand e frumos afara. */
const ICONS = {
  /* Ziua, cand e senin: sunburst-ul. Unsprezece raze, ascutite spre centru si
     rotunjite la varf - forma e generata, nu desenata din ochi, ca sa fie simetrica
     la orice marime. */
  clear: (cls = '') => `
    <svg viewBox="0 0 64 64" class="${cls}">
      <path fill="#DA7756" d="M32.00,30.40Q30.61,14.27 28.90,5.00A3.10,3.10 0 0 1 35.10,5.00Q33.40,14.27 32.00,30.40ZM32.87,30.65Q40.41,16.33 43.99,7.61A3.10,3.10 0 0 1 49.21,10.96Q42.76,17.84 32.87,30.65ZM33.46,31.34Q47.55,23.36 55.27,17.96A3.10,3.10 0 0 1 57.85,23.60Q48.71,25.90 33.46,31.34ZM33.58,32.23Q49.75,33.14 59.17,32.77A3.10,3.10 0 0 1 58.28,38.91Q49.35,35.90 33.58,32.23ZM33.21,33.05Q46.31,42.56 54.44,47.34A3.10,3.10 0 0 1 50.38,52.02Q44.49,44.67 33.21,33.05ZM32.45,33.54Q38.33,48.62 42.58,57.03A3.10,3.10 0 0 1 36.63,58.78Q35.66,49.41 32.45,33.54ZM31.55,33.54Q28.34,49.41 27.37,58.78A3.10,3.10 0 0 1 21.42,57.03Q25.67,48.62 31.55,33.54ZM30.79,33.05Q19.51,44.67 13.62,52.02A3.10,3.10 0 0 1 9.56,47.34Q17.69,42.56 30.79,33.05ZM30.42,32.23Q14.65,35.90 5.72,38.91A3.10,3.10 0 0 1 4.83,32.77Q14.25,33.14 30.42,32.23ZM30.54,31.34Q15.29,25.90 6.15,23.60A3.10,3.10 0 0 1 8.73,17.96Q16.45,23.36 30.54,31.34ZM31.13,30.65Q21.24,17.84 14.79,10.96A3.10,3.10 0 0 1 20.01,7.61Q23.59,16.33 31.13,30.65Z"/>
    </svg>`,

  /* Noaptea, cand e senin: luna. Decupata dintr-un cerc cu al doilea cerc, prin
     `evenodd` - o singura forma, fara masca si fara al doilea element care sa se
     dezalinieze la scalare. */
  clearNight: () => `
    <svg viewBox="0 0 64 64">
      <path fill="#E8D9C5" fill-rule="evenodd" d="M32 6a26 26 0 1 0 0 52 26 26 0 0 1
        0-52zm6 4.6A21.6 21.6 0 0 0 38 53.4 26 26 0 0 1 38 10.6z"/>
    </svg>`,

  /* Noros noaptea: aceeasi luna, mai mica, in spatele norului. */
  partlyNight: () => `
    <svg viewBox="0 0 64 64">
      <path fill="#E8D9C5" fill-rule="evenodd" d="M24 4a17 17 0 1 0 0 34 17 17 0 0 1
        0-34zm4 3a14.1 14.1 0 0 0 0 28 17 17 0 0 1 0-28z"/>
      <path fill="#8A8079" d="M45 30a11 11 0 0 0-10.6 8.1A9 9 0 0 0 36 56h20a9 9 0 0 0
        1.1-17.9A11 11 0 0 0 45 30z"/>
    </svg>`,

  /* Partial noros, ziua: acelasi sunburst, mai mic, in spatele norului. */
  partly: () => `
    <svg viewBox="0 0 64 64">
      <path fill="#DA7756" d="M23.00,20.00Q22.10,9.84 21.00,4.00A2.00,2.00 0 0 1 25.00,4.00Q23.90,9.84 23.00,20.00ZM23.54,20.16Q28.28,11.13 30.51,5.62A2.00,2.00 0 0 1 33.87,7.78Q29.79,12.10 23.54,20.16ZM23.91,20.58Q32.78,15.55 37.63,12.12A2.00,2.00 0 0 1 39.29,15.76Q33.53,17.18 23.91,20.58ZM23.99,21.14Q34.17,21.70 40.11,21.44A2.00,2.00 0 0 1 39.54,25.40Q33.92,23.48 23.99,21.14ZM23.76,21.65Q32.02,27.63 37.16,30.62A2.00,2.00 0 0 1 34.54,33.64Q30.84,28.99 23.76,21.65ZM23.28,21.96Q27.01,31.45 29.71,36.75A2.00,2.00 0 0 1 25.87,37.87Q25.28,31.96 23.28,21.96ZM22.72,21.96Q20.72,31.96 20.13,37.87A2.00,2.00 0 0 1 16.29,36.75Q18.99,31.45 22.72,21.96ZM22.24,21.65Q15.16,28.99 11.46,33.64A2.00,2.00 0 0 1 8.84,30.62Q13.98,27.63 22.24,21.65ZM22.01,21.14Q12.08,23.48 6.46,25.40A2.00,2.00 0 0 1 5.89,21.44Q11.83,21.70 22.01,21.14ZM22.09,20.58Q12.47,17.18 6.71,15.76A2.00,2.00 0 0 1 8.37,12.12Q13.22,15.55 22.09,20.58ZM22.46,20.16Q16.21,12.10 12.13,7.78A2.00,2.00 0 0 1 15.49,5.62Q17.72,11.13 22.46,20.16Z"/>
      <path fill="#8A8079" d="M45 30a11 11 0 0 0-10.6 8.1A9 9 0 0 0 36 56h20a9 9
        0 0 0 1.1-17.9A11 11 0 0 0 45 30z"/>
    </svg>`,

  cloudy: () => `
    <svg viewBox="0 0 64 64">
      <path fill="#8A8079" d="M44 14a15 15 0 0 0-14.4 10.9A12 12 0 0 0 32 48h24a12
        12 0 0 0 1.4-23.9A15 15 0 0 0 44 14z"/>
      <path fill="#5E5751" d="M20 26a13 13 0 0 1 4.6.8A10 10 0 0 0 20 44H14a9 9 0
        0 1-1.2-17.9A13 13 0 0 1 20 26z"/>
    </svg>`,

  fog: () => `
    <svg viewBox="0 0 64 64">
      <path fill="#8A8079" d="M42 10a15 15 0 0 0-14.4 10.9A12 12 0 0 0 30 44h24a12
        12 0 0 0 1.4-23.9A15 15 0 0 0 42 10z"/>
      <g stroke="#6F6862" stroke-width="4" stroke-linecap="round">
        <line x1="8"  y1="52" x2="56" y2="52"/>
        <line x1="14" y1="60" x2="50" y2="60"/>
      </g>
    </svg>`,

  drizzle: () => `
    <svg viewBox="0 0 64 64">
      <path fill="#8A8079" d="M42 8a15 15 0 0 0-14.4 10.9A12 12 0 0 0 30 42h24a12
        12 0 0 0 1.4-23.9A15 15 0 0 0 42 8z"/>
      <g stroke="#7FB0D8" stroke-width="3.5" stroke-linecap="round">
        <line x1="24" y1="48" x2="21" y2="56"/>
        <line x1="38" y1="48" x2="35" y2="56"/>
      </g>
    </svg>`,

  rain: () => `
    <svg viewBox="0 0 64 64">
      <path fill="#8A8079" d="M42 8a15 15 0 0 0-14.4 10.9A12 12 0 0 0 30 42h24a12
        12 0 0 0 1.4-23.9A15 15 0 0 0 42 8z"/>
      <g stroke="#7FB0D8" stroke-width="4" stroke-linecap="round">
        <line x1="20" y1="47" x2="16" y2="59"/>
        <line x1="33" y1="47" x2="29" y2="59"/>
        <line x1="46" y1="47" x2="42" y2="59"/>
      </g>
    </svg>`,

  snow: () => `
    <svg viewBox="0 0 64 64">
      <path fill="#8A8079" d="M42 8a15 15 0 0 0-14.4 10.9A12 12 0 0 0 30 42h24a12
        12 0 0 0 1.4-23.9A15 15 0 0 0 42 8z"/>
      <g fill="#CFE3F2">
        <circle cx="20" cy="51" r="3.2"/>
        <circle cx="33" cy="57" r="3.2"/>
        <circle cx="46" cy="51" r="3.2"/>
      </g>
    </svg>`,

  storm: () => `
    <svg viewBox="0 0 64 64">
      <path fill="#6F6862" d="M42 6a15 15 0 0 0-14.4 10.9A12 12 0 0 0 30 40h24a12
        12 0 0 0 1.4-23.9A15 15 0 0 0 42 6z"/>
      <path fill="#DA7756" d="M34 42l-12 12h8l-4 12 14-15h-8z"/>
    </svg>`,
};

/* WMO → ce desenam si cum se numeste. Codurile sunt cele din documentatia
   Open-Meteo; ce nu e in tabel cade pe „cloudy", care nu minte niciodata prea
   tare. */
const WMO = {
  0:  ['clear',   'senin'],
  1:  ['clear',   'aproape senin'],
  2:  ['partly',  'parțial noros'],
  3:  ['cloudy',  'înnorat'],
  45: ['fog',     'ceață'],
  48: ['fog',     'ceață cu chiciură'],
  51: ['drizzle', 'burniță slabă'],
  53: ['drizzle', 'burniță'],
  55: ['drizzle', 'burniță deasă'],
  56: ['drizzle', 'burniță înghețată'],
  57: ['drizzle', 'burniță înghețată'],
  61: ['rain',    'ploaie slabă'],
  63: ['rain',    'ploaie'],
  65: ['rain',    'ploaie torențială'],
  66: ['rain',    'ploaie înghețată'],
  67: ['rain',    'ploaie înghețată'],
  71: ['snow',    'ninsoare slabă'],
  73: ['snow',    'ninsoare'],
  75: ['snow',    'ninsoare abundentă'],
  77: ['snow',    'măzăriche'],
  80: ['rain',    'averse slabe'],
  81: ['rain',    'averse'],
  82: ['rain',    'averse puternice'],
  85: ['snow',    'averse de zăpadă'],
  86: ['snow',    'averse de zăpadă'],
  95: ['storm',   'furtună'],
  96: ['storm',   'furtună cu grindină'],
  99: ['storm',   'furtună cu grindină'],
};

function decode(code) {
  return WMO[code] || ['cloudy', 'înnorat'];
}

/* Noaptea, „senin" nu se deseneaza cu un soare. Doar cele doua feluri de vreme
   senina au varianta de noapte; ploaia arata la fel la orice ora.

   Lista pe zile foloseste mereu varianta de zi: un rand care rezuma o zi intreaga
   n-are cum sa fie „noapte". */
function iconFor(kind, isDay) {
  if (!isDay && kind === 'clear') return ICONS.clearNight();
  if (!isDay && kind === 'partly') return ICONS.partlyNight();
  return ICONS[kind](kind === 'clear' ? 'star-anim' : '');
}

/* ==========================================================================
   Comentariile
   ========================================================================== */

const QUIPS = {
  furtuna: [
    'Furtună. Ăsta e momentul în care „ies doar un minut" devine o decizie de viață.',
    'Tună. Dacă aveai planuri afară, acum ai planuri înăuntru.',
    'Cerul are o părere și ți-o spune tare. Stai în casă.',
    'Furtună. Umbrela nu te ajută, doar îți dă iluzia că faci ceva.',
    'E genul de vreme care rupe crengi și programe. Amână.',
    'Descărcări electrice afară, descărcare de baterie înăuntru. Alege înțelept.',
    'Furtună. Momentul perfect ca internetul să pice exact acum.',
    'Afară e spectacol. Bilet la geam, nu la scenă.',
    'Nu e „o ploicică". Rămâi unde ești.',
  ],

  ninsoare: [
    'Ninge. Arată frumos vreo douăzeci de minute, apoi devine problema ta.',
    'Zăpadă. Toată lumea conduce ca și cum ar fi prima iarnă din istorie.',
    'Ninge. Ia-ți bocancii, nu adidașii pe care-i priveai acum.',
    'E alb afară. E și alunecos, dar asta nu se vede în poze.',
    'Ninsoare. Cinci minute de magie, două ore de curățat mașina.',
    'Ninge liniștit. Bucură-te acum, mâine e gheață.',
    'Zăpadă proaspătă. Primul care calcă e erou, al doilea e doar ud.',
    'Ninge. Ceaiul nu e opțional azi.',
    'Iarna și-a adus aminte de tine.',
  ],

  ploaie: [
    'Plouă. Ia umbrela pe care oricum o s-o uiți undeva.',
    'Ploaie. Perfect pentru cei care nu voiau să iasă oricum.',
    'Plouă. Șosetele ude sunt la o pereche de pantofi greșiți distanță.',
    'Apă din cer. Gratis, dar nimeni n-a cerut-o.',
    'Plouă. Zi bună pentru geam, ceai și zero responsabilități.',
    'Ploaie. Mașina ta tocmai a fost spălată. Prost, dar spălată.',
    'Plouă. Gluga nu e o rușine, e o strategie.',
    'Cerul se descarcă emoțional. Lasă-l.',
    'Plouă. Dacă azi conta părul tău, ghinion.',
    'Ploaie. Alergatul se amână, iar tu știi asta deja.',
  ],

  ceata: [
    'Ceață. Vezi cam până la capătul brațului și nici acolo cu încredere.',
    'Ceață densă. Condu ca și cum ai avea ceva de pierdut.',
    'Afară e ștearsă lumea. Revine mai târziu.',
    'Ceață. Totul arată misterios până când calci în ceva.',
    'Vizibilitate mică, răbdare și mai mică. Ai grijă pe drum.',
    'Ceață. Farurile aprinse, viteza jos, muzica mai încet.',
    'Orașul a intrat în modul incognito.',
  ],

  vant: [
    'Vânt puternic. Coafura de azi e o sugestie, nu o decizie.',
    'Bate tare. Ține-ți gluga și demnitatea.',
    'Vânt. Umbrela ta va deveni sculptură modernă.',
    'Rafale serioase. Nu parca sub copaci.',
    'Vânt puternic. Ușile se trântesc singure, nu e casa bântuită.',
    'Afară te împinge cineva. E doar aerul.',
    'Vânt. Frigul de pe termometru și frigul real sunt două lucruri diferite azi.',
  ],

  ger: [
    'Ger. Aerul te înțeapă, nu te mângâie.',
    'E frig de crapă pietrele. Straturi, nu curaj.',
    'Ger serios. Mașina va porni, dar cu resentimente.',
    'Sub zero binișor. Nu ieși „așa, repede", că nu ține.',
    'Frig de-ăla care te trezește mai bine decât cafeaua.',
    'Ger. Fiecare centimetru de piele descoperit e o greșeală.',
    'Iarnă adevărată, nu decor. Îmbracă-te ca atare.',
    'E atât de frig încât până și frigiderul pare o opțiune caldă.',
  ],

  frig: [
    'Frig. Geaca aia subțire nu e o idee bună, deși arată bine.',
    'Rece. Mâinile în buzunare, planurile scurte.',
    'E frig. Nu polar, dar suficient cât să regreți.',
    'Frig. Ceaiul e mai mult decât o băutură azi.',
    'Rece afară. Zece minute sunt suportabile, treizeci nu.',
    'Frig. Ia fularul, nu-l lăsa pe scaun.',
    'Nu e ger, dar nici primăvară. Îmbracă-te pentru varianta rea.',
  ],

  racoare: [
    'Răcoare. Vremea aia care nu se hotărăște ce vrea.',
    'Rece-plăcut. Un strat în plus și ești câștigător.',
    'Răcoros. Perfect de mers pe jos, prost de stat pe loc.',
    'Nici cald, nici frig. Vremea diplomatului.',
    'Răcoare. Geaca subțire își face în sfârșit treaba.',
    'E vremea în care pleci îmbrăcat gros și te întorci cărând haina.',
    'Răcoros și limpede. Se putea mult mai rău.',
  ],

  placut: [
    'Vreme bună. Serios, chiar bună. Ieși puțin.',
    'Perfect afară. Nicio scuză nu ține azi.',
    'Temperatura ideală. Se întâmplă rar, folosește-o.',
    'Vreme de plimbat fără plan.',
    'Așa ar trebui să fie mereu. Nu e, deci profită.',
    'Nici cald, nici frig, nimic de reclamat. Ciudat, nu?',
    'Zi bună. Chiar și pentru cei care nu ies niciodată.',
    'Vremea nu are azi nicio scuză să te țină în casă.',
  ],

  cald: [
    'Cald. Umbra devine brusc o resursă.',
    'E cald. Apa, nu cafeaua.',
    'Vreme de terasă, nu de birou. Îmi pare rău.',
    'Cald bine. Asfaltul o simte înaintea ta.',
    'Soare tare. Cinci minute par zece.',
    'Cald. Mașina lăsată la soare e un cuptor cu volan.',
    'Vară adevărată. Hidratare, nu eroism.',
  ],

  canicula: [
    'Caniculă. Nu ești obosit, ești copt.',
    'E prea cald pentru orice. Inclusiv pentru asta.',
    'Caniculă. Ieși dimineața sau seara, la mijloc nu.',
    'Peste 35. Asta nu mai e vreme, e o pedeapsă.',
    'Arșiță. Apă, umbră, zero ambiții.',
    'Caniculă. Mașina nu se conduce, se negociază.',
    'E atât de cald încât și vântul e cald. Genial.',
    'Zi de stat nemișcat lângă ceva rece.',
  ],

  senin: [
    'Senin complet. Nici măcar un nor de vină.',
    'Cer curat. Rar, dar se întâmplă.',
    'Fără nori. Ochelarii de soare nu sunt fițe azi.',
    'Senin. Cerul și-a făcut curat.',
    'Zi limpede. Se vede până departe și merită privit.',
    'Nicio pată pe cer. Bucură-te, mâine se schimbă.',
  ],

  noapte: [
    'E noapte. Vremea de mâine e problema lui mâine.',
    'Noapte liniștită. Cerul nu are planuri cu tine.',
    'Târziu. Verifici vremea sau eviți somnul?',
    'Noapte. Dacă tot ești treaz, măcar e frumos afară.',
    'Ora la care singurul plan bun e patul.',
  ],
};

/* Ce comentariu se potriveste, in ordinea in care conteaza.
   **Vremea grea bate temperatura**: cand ploua cu galeata, nu-l intereseaza pe
   nimeni ca sunt 18 grade placute. */
function pickCategory(w) {
  const [kind] = decode(w.code);

  if (kind === 'storm') return 'furtuna';
  if (kind === 'snow') return 'ninsoare';
  if (kind === 'rain' || kind === 'drizzle') return 'ploaie';
  if (kind === 'fog') return 'ceata';
  if (w.wind >= 35) return 'vant';

  if (w.temp <= -5) return 'ger';
  if (w.temp < 5) return 'frig';
  if (w.temp < 15) return 'racoare';
  if (w.temp < 24) return kind === 'clear' && w.isDay ? 'senin' : 'placut';
  if (w.temp < 31) return 'cald';
  return 'canicula';
}

/* Alegerea nu e la intamplare de tot: se schimba din ora in ora, nu la fiecare
   reimprospatare. Un text care sare la fiecare 10 minute distrage; unul care
   nu se schimba deloc devine invizibil in doua zile. */
function pickQuip(w, now) {
  let category = pickCategory(w);
  if (!w.isDay && (category === 'placut' || category === 'senin')) category = 'noapte';

  const list = QUIPS[category] || QUIPS.placut;
  const seed = now.getFullYear() * 10000 + (now.getMonth() + 1) * 100 + now.getDate();
  const index = (seed + now.getHours() * 7 + category.length) % list.length;
  return list[index];
}

/* ==========================================================================
   Ceasul
   ========================================================================== */

const el = (id) => document.getElementById(id);

const DAYS_LONG = ['duminică', 'luni', 'marți', 'miercuri', 'joi', 'vineri', 'sâmbătă'];
const DAYS_SHORT = ['dum', 'lun', 'mar', 'mie', 'joi', 'vin', 'sâm'];
const MONTHS = ['ianuarie', 'februarie', 'martie', 'aprilie', 'mai', 'iunie',
  'iulie', 'august', 'septembrie', 'octombrie', 'noiembrie', 'decembrie'];

function tick() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  el('clock').textContent = `${pad(d.getHours())}:${pad(d.getMinutes())}`;
  el('seconds').textContent = pad(d.getSeconds());
  el('date').textContent = `${DAYS_LONG[d.getDay()]}, ${d.getDate()} ${MONTHS[d.getMonth()]}`;
}

/* ==========================================================================
   Locul
   ========================================================================== */

/* Geolocatia se cere o singura data si **nu blocheaza nimic**: daca omul nu
   raspunde in 8 secunde, plecam pe rezerva. Un ecran gol care asteapta o
   permisiune e mai rau decat o vreme dintr-un oras apropiat. */
function findPlace() {
  return new Promise((resolve) => {
    const saved = localStorage.getItem(PLACE_KEY);
    if (saved) {
      try { return resolve(JSON.parse(saved)); } catch (_) { /* cade pe rest */ }
    }

    if (!navigator.geolocation || !window.isSecureContext) return resolve(FALLBACK);

    let done = false;
    const finish = (place) => { if (!done) { done = true; resolve(place); } };
    setTimeout(() => finish(FALLBACK), 8000);

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const place = {
          lat: +pos.coords.latitude.toFixed(4),
          lon: +pos.coords.longitude.toFixed(4),
          name: null,
        };
        localStorage.setItem(PLACE_KEY, JSON.stringify(place));
        finish(place);
      },
      () => finish(FALLBACK),
      { timeout: 7500, maximumAge: 30 * 60 * 1000 },
    );
  });
}

/* Numele locului, cand avem doar coordonate. E un moft, deci daca pica nu se
   intampla nimic - ramane „Locația ta". */
async function nameFor(place) {
  if (place.name) return place.name;
  try {
    const url = `https://geocoding-api.open-meteo.com/v1/search?count=1&language=ro`
      + `&latitude=${place.lat}&longitude=${place.lon}`;
    const res = await fetch(url);
    const data = await res.json();
    return data?.results?.[0]?.name || 'Locația ta';
  } catch (_) {
    return 'Locația ta';
  }
}

/* ==========================================================================
   Vremea
   ========================================================================== */

async function fetchWeather(place) {
  const url = 'https://api.open-meteo.com/v1/forecast'
    + `?latitude=${place.lat}&longitude=${place.lon}`
    + '&current=temperature_2m,apparent_temperature,is_day,weather_code,wind_speed_10m'
    + '&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max'
    + '&timezone=auto&forecast_days=7';

  const res = await fetch(url);
  if (!res.ok) throw new Error(`Open-Meteo a răspuns ${res.status}`);
  const data = await res.json();

  return {
    temp: Math.round(data.current.temperature_2m),
    feels: Math.round(data.current.apparent_temperature),
    code: data.current.weather_code,
    wind: Math.round(data.current.wind_speed_10m),
    isDay: data.current.is_day === 1,
    tmin: Math.round(data.daily.temperature_2m_min[0]),
    tmax: Math.round(data.daily.temperature_2m_max[0]),
    rain: data.daily.precipitation_probability_max[0] ?? 0,
    days: data.daily.time.map((iso, i) => ({
      iso,
      code: data.daily.weather_code[i],
      min: Math.round(data.daily.temperature_2m_min[i]),
      max: Math.round(data.daily.temperature_2m_max[i]),
      rain: data.daily.precipitation_probability_max[i] ?? 0,
    })),
    at: Date.now(),
  };
}

/* ==========================================================================
   Desenatul
   ========================================================================== */

function render(w, placeName, fromCache) {
  const [kind, label] = decode(w.code);

  el('place').textContent = placeName;
  el('icon').innerHTML = iconFor(kind, w.isDay);
  el('temp').textContent = w.temp;
  el('desc').textContent = label;
  el('feels').textContent = w.feels !== w.temp ? `se simte ca ${w.feels}°` : '';
  el('quip').textContent = pickQuip(w, new Date());

  el('tmin').textContent = `${w.tmin}°`;
  el('tmax').textContent = `${w.tmax}°`;
  el('wind').textContent = `${w.wind} km/h`;
  el('rain').textContent = `${w.rain}%`;

  const today = new Date().toDateString();
  el('days').innerHTML = w.days.map((d) => {
    const date = new Date(`${d.iso}T12:00:00`);
    const isToday = date.toDateString() === today;
    const [dk] = decode(d.code);
    return `
      <li class="day">
        <span class="day-name${isToday ? ' today' : ''}">${isToday ? 'azi' : DAYS_SHORT[date.getDay()] + ' ' + date.getDate()}</span>
        <span class="day-icon">${ICONS[dk]()}</span>
        <span class="day-rain">${d.rain >= 20 ? d.rain + '%' : ''}</span>
        <span class="day-temps"><span class="day-min">${d.min}°</span><span class="day-max">${d.max}°</span></span>
      </li>`;
  }).join('');

  const at = new Date(w.at);
  const pad = (n) => String(n).padStart(2, '0');
  el('updated').textContent = fromCache
    ? `date salvate, de la ${pad(at.getHours())}:${pad(at.getMinutes())}`
    : `actualizat la ${pad(at.getHours())}:${pad(at.getMinutes())}`;

  document.getElementById('app').classList.remove('is-loading');
}

let toastTimer = null;
function toast(text) {
  const t = el('toast');
  t.textContent = text;
  t.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { t.hidden = true; }, 4000);
}

/* ==========================================================================
   Pornirea
   ========================================================================== */

let place = null;
let placeName = '…';

async function load(userAsked = false) {
  const btn = el('refresh');
  if (userAsked) btn.classList.add('is-busy');

  try {
    const w = await fetchWeather(place);
    localStorage.setItem(CACHE_KEY, JSON.stringify({ w, placeName }));
    render(w, placeName, false);
    if (userAsked) toast('Gata, date proaspete.');
  } catch (err) {
    // Fara internet nu inventam nimic: aratam ce stiam si spunem de cand e.
    const cached = localStorage.getItem(CACHE_KEY);
    if (cached) {
      try {
        const { w, placeName: pn } = JSON.parse(cached);
        render(w, pn || placeName, true);
        toast('Fără conexiune. Vezi ultimele date salvate.');
      } catch (_) {
        toast('Nu am putut lua vremea și nici nu am date salvate.');
      }
    } else {
      toast('Nu am putut lua vremea. Verifică internetul.');
    }
  } finally {
    btn.classList.remove('is-busy');
  }
}

async function start() {
  tick();
  setInterval(tick, 1000);

  // Ce stiam de data trecuta se arata imediat, ca ecranul sa nu fie gol cat se
  // asteapta reteaua. Se inlocuieste singur cand vin datele noi.
  const cached = localStorage.getItem(CACHE_KEY);
  if (cached) {
    try {
      const { w, placeName: pn } = JSON.parse(cached);
      render(w, pn || '…', true);
    } catch (_) { /* nimic - continuam normal */ }
  }

  place = await findPlace();
  placeName = await nameFor(place);
  el('place').textContent = placeName;

  await load();
  setInterval(() => load(), REFRESH_MS);

  // Cand telefonul revine din buzunar, datele de acum o ora nu mai sunt bune.
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden) load();
  });

  el('refresh').addEventListener('click', () => load(true));
}

if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('sw.js').catch(() => { /* merge si fara */ });
  });
}

start();
