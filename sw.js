/* Service worker: doar cat sa porneasca fara internet.
 *
 * **Doua strategii, si diferenta conteaza.** Fisierele aplicatiei se iau din
 * cache (nu se schimba des, si asa pornirea e instantanee), dar vremea se ia
 * mereu de pe retea. Un raspuns meteo servit din cache ar fi exact defectul pe
 * care nu-l vrei: o aplicatie care arata increzatoare temperatura de ieri.
 * Cand reteaua lipseste, `app.js` scoate ce a salvat el si **spune de cand e**.
 */

const CACHE = 'steluta-v1';

const SHELL = [
  './',
  './index.html',
  './style.css',
  './app.js',
  './manifest.json',
  './icons/icon-192.png',
  './icons/icon-512.png',
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE).then((c) => c.addAll(SHELL)).then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const { request } = event;
  if (request.method !== 'GET') return;

  const url = new URL(request.url);

  // Vremea si numele locului: numai de pe retea. Nu se pun niciodata in cache.
  if (url.hostname.endsWith('open-meteo.com')) return;

  event.respondWith(
    caches.match(request).then((hit) => hit || fetch(request).then((res) => {
      // Se pastreaza doar ce e al aplicatiei si a venit intreg.
      if (res.ok && url.origin === self.location.origin) {
        const copy = res.clone();
        caches.open(CACHE).then((c) => c.put(request, copy));
      }
      return res;
    }).catch(() => caches.match('./index.html')))
  );
});
