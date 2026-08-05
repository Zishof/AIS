/*
 * ais_pos_sw.js — Service Worker (opsional) agar halaman POS tetap bisa dimuat saat OFFLINE.
 *
 * Strategi KONSERVATIF (aman utk aplikasi besar):
 *  - Permintaan ke endpoint layanan (/baru?...s=...) : TIDAK disentuh (network-only) → ditangani
 *    mesin ais_pos_offline.js (antrian lokal saat offline).
 *  - Aset statis (js/css/img/font) : cache-first (cepat + tersedia offline).
 *  - Navigasi halaman : network-first, fallback ke cache (online normal; offline tampilkan versi terakhir).
 *
 * Catatan penting (aktifkan dengan hati-hati): cakupan (scope) SW = folder tempat berkas ini diakses.
 * Agar mencakup POS, daftarkan dengan scope yang sesuai + header `Service-Worker-Allowed` bila perlu.
 * Selama halaman POS tetap terbuka, transaksi offline TETAP jalan tanpa SW (mesin + IndexedDB);
 * SW hanya menambah kemampuan memuat ulang halaman ketika offline.
 */
var AIS_POS_CACHE = 'ais-pos-shell-v1';
var CORE = []; // diisi saat install (opsional). Biarkan kosong agar instalasi tak gagal bila URL berbeda.

self.addEventListener('install', function (e) {
	self.skipWaiting();
	e.waitUntil(caches.open(AIS_POS_CACHE).then(function (c) { return c.addAll(CORE).catch(function () { }); }));
});

self.addEventListener('activate', function (e) {
	e.waitUntil(caches.keys().then(function (keys) {
		return Promise.all(keys.map(function (k) { return k === AIS_POS_CACHE ? null : caches.delete(k); }));
	}));
	self.clients.claim();
});

self.addEventListener('fetch', function (e) {
	var req = e.request;
	if (req.method !== 'GET') return; // hanya GET yang di-cache
	var url;
	try { url = new URL(req.url); } catch (x) { return; }

	// Endpoint layanan dinamis: jangan disentuh (biar mesin offline yang urus).
	if (url.search.indexOf('hanya_tampil_jsp') >= 0 || (url.pathname.indexOf('/baru') >= 0 && url.search.indexOf('s=') >= 0)) {
		return;
	}

	// Aset statis: cache-first.
	if (/\.(js|css|png|jpe?g|gif|svg|webp|woff2?|ttf|eot)$/i.test(url.pathname)) {
		e.respondWith(caches.match(req).then(function (hit) {
			return hit || fetch(req).then(function (res) {
				var copy = res.clone(); caches.open(AIS_POS_CACHE).then(function (c) { c.put(req, copy); });
				return res;
			}).catch(function () { return hit; });
		}));
		return;
	}

	// Navigasi halaman: network-first, fallback cache.
	if (req.mode === 'navigate') {
		e.respondWith(fetch(req).then(function (res) {
			var copy = res.clone(); caches.open(AIS_POS_CACHE).then(function (c) { c.put(req, copy); });
			return res;
		}).catch(function () { return caches.match(req); }));
	}
});
