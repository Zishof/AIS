/*
 * ais_pos_pwa.js — menjadikan POS sebagai aplikasi yang BISA DI-INSTALL (PWA) di Windows & Linux
 * (lewat Chrome/Edge/Chromium) maupun Android. Tugasnya:
 *   1. Menyisipkan <link rel="manifest"> (manifest.webmanifest) ke <head>.
 *   2. Mendaftarkan Service Worker (agar halaman POS termuat saat offline).
 *   3. Menampilkan tombol "Install Aplikasi POS" saat browser menyatakan dapat di-install.
 *
 * Dimuat dari halaman POS:
 *   <script src="<ROOT>/js/ais_pos_pwa.js"
 *           data-root="<ROOT>" data-manifest="<ROOT>/manifest.webmanifest" data-sw="<ROOT>/ais_pos_sw.js"></script>
 *
 * Syarat install (PWA): dilayani via HTTPS (atau http://localhost) + manifest + service worker.
 */
(function () {
	var sc = document.currentScript || (function () { var s = document.getElementsByTagName('script'); return s[s.length - 1]; })();
	var root = (sc && sc.getAttribute('data-root')) || '';
	var manifestUrl = (sc && sc.getAttribute('data-manifest')) || (root + '/manifest.webmanifest');
	var swUrl = (sc && sc.getAttribute('data-sw')) || (root + '/ais_pos_sw.js');

	// 1) Sisipkan manifest (halaman POS adalah fragmen, jadi tautkan manifest via JS).
	try {
		if (!document.querySelector('link[rel="manifest"]')) {
			var l = document.createElement('link'); l.rel = 'manifest'; l.href = manifestUrl; document.head.appendChild(l);
		}
	} catch (e) { }

	// 2) Daftarkan Service Worker (scope seluas ROOT agar mencakup halaman POS).
	if ('serviceWorker' in navigator) {
		try { navigator.serviceWorker.register(swUrl, { scope: root + '/' }).catch(function () { }); } catch (e) { }
	}

	// 3) Tombol "Install Aplikasi POS".
	var deferred = null;
	function sudahTerinstall() {
		try { return window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true; } catch (e) { return false; }
	}
	function tampilTombol() {
		if (sudahTerinstall() || document.getElementById('aisPosInstallBtn')) return;
		var b = document.createElement('button');
		b.id = 'aisPosInstallBtn'; b.type = 'button';
		b.innerHTML = '⬇ Install Aplikasi POS';
		b.style.cssText = 'position:fixed;bottom:16px;left:16px;z-index:9999;background:#16a34a;color:#fff;border:0;'
			+ 'font-weight:700;padding:10px 16px;border-radius:999px;box-shadow:0 2px 10px rgba(0,0,0,.25);cursor:pointer;';
		b.onclick = function () {
			if (!deferred) return;
			deferred.prompt();
			deferred.userChoice.then(function () { deferred = null; b.remove(); });
		};
		document.body.appendChild(b);
	}
	window.addEventListener('beforeinstallprompt', function (e) { e.preventDefault(); deferred = e; tampilTombol(); });
	window.addEventListener('appinstalled', function () { var b = document.getElementById('aisPosInstallBtn'); if (b) b.remove(); });
})();
