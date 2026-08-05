/*
 * ais_pos_offline.js — Mesin POS Offline-First (dipakai BERSAMA versi JSP & ZK, browser maupun shell
 * desktop Electron).
 *
 * Mengimplementasikan instruksi kasir offline-first:
 *  1. Unduh data POS (produk, harga) ke browser saat sesi kasir dibuka  -> cacheProduk() ke IndexedDB
 *     (katalog produk SELALU di IndexedDB, di kedua mode -- ini cache murni yg boleh hilang/diunduh
 *     ulang, bukan data transaksional, jadi tak perlu jaminan SQLite).
 *  2. Penyimpanan PERSISTEN transaksi -- backend berbeda tergantung lingkungan (lihat pilihBackend()):
 *       - Shell desktop Electron: SQLite via window.electronAPI.localDb (lihat local-db.js di
 *         desktop-pos-electron/) -- tahan mati listrik (mode WAL), berkas .db bisa diperiksa manual.
 *       - Browser biasa (tanpa Electron): IndexedDB seperti sebelumnya.
 *     KEDUA backend memakai semantik yang SAMA: transaksi SELALU ditulis lokal DULU (status PENDING)
 *     sebelum dicoba dikirim ke server -- baik saat online maupun offline -- dan baris TIDAK PERNAH
 *     DIHAPUS setelah berhasil sinkron (hanya status berubah jadi SYNCED), sehingga tetap berfungsi
 *     sebagai cadangan/riwayat lokal permanen yang bisa diperiksa/dire-sinkronkan ulang kapan saja.
 *  3. Sinkronisasi online/offline:
 *     - submit(trx): simpan lokal (PENDING) lalu (bila online) langsung coba kirim ke server.
 *     - offline: tetap tersimpan aman lokal; saat online kembali -> auto flush (syncNow()).
 *  4. Pembagian beban: katalog/hitung/cetak di lokal; persist/stok/akuntansi di server.
 *
 * API (TIDAK BERUBAH terlepas dari backend yang dipakai -- pemanggil di _pos.jsp/
 * _riwayat_sinkronisasi.jsp tidak perlu tahu/peduli backend mana yang aktif):
 *   AisPosOffline.init({ tokoId, kasir, dataUrl, syncUrl, onStatus });
 *   AisPosOffline.getProducts()            -> Promise([{id,kode,nama,hargaJual,hargaBeli}])  (dari cache, jalan offline)
 *   AisPosOffline.findByBarcode(kode)      -> Promise(produk|null)
 *   AisPosOffline.refreshProducts()        -> Promise (unduh ulang dari server -> cache)
 *   AisPosOffline.submit(trx)              -> Promise({queued, synced})   (trx: {clientTrxId?, items:[{produkId,qty,harga}], total, bayar, kembalian, member, caraBayar, waktu})
 *   AisPosOffline.syncNow()                -> Promise(jumlahTersinkron)  (jalan otomatis; boleh juga dipanggil manual dari tombol "Sinkronkan Sekarang")
 *   AisPosOffline.pendingCount()           -> Promise(n)
 *   AisPosOffline.listPending()            -> Promise([trx])  daftar LENGKAP transaksi yang masih tertahan di perangkat ini (belum sinkron) -- dipakai panel "Status Sinkronisasi"
 *   AisPosOffline.getLastSyncAt()          -> Promise(isoString|null)  kapan perangkat ini TERAKHIR KALI berhasil bicara ke server lewat syncNow() (null = belum pernah)
 *   AisPosOffline.isOnline()               -> boolean
 *
 * Tanpa dependensi eksternal (IndexedDB + fetch native, atau IPC ke local-db.js di Electron). Aman
 * dipanggil berkali-kali.
 *
 * CATATAN CAKUPAN "status sinkron": penyimpanan lokal ini HANYA ada di perangkat kasir yang
 * bersangkutan -- server sama sekali tidak tahu keberadaan transaksi yang belum berhasil sinkron
 * (karena memang belum pernah dikirim). Jadi "belum sinkron" HANYA bisa dilihat dari perangkat
 * asalnya sendiri (lewat listPending() di sini); untuk melihat riwayat yang SUDAH sinkron --
 * termasuk dari perangkat/kasir lain -- pakai laporan server-side terpisah
 * (riwayat_sinkronisasi.jsp), yang membaca kolom sumberTransaksi/waktuSinkron di
 * PembelianAnggotaKoperasi.
 */
(function () {
	var DB_NAME = 'aisPosDB', DB_VER = 1, ST_PRODUK = 'produk', ST_PENDING = 'pending', ST_META = 'meta';
	var cfg = { tokoId: null, kasir: null, dataUrl: null, syncUrl: null, onStatus: null };
	var _db = null, _syncing = false, _bound = false, _backend = null;

	function openDb() {
		return new Promise(function (resolve, reject) {
			if (_db) { resolve(_db); return; }
			var req = indexedDB.open(DB_NAME, DB_VER);
			req.onupgradeneeded = function (e) {
				var db = e.target.result;
				if (!db.objectStoreNames.contains(ST_PRODUK)) db.createObjectStore(ST_PRODUK, { keyPath: 'id' });
				if (!db.objectStoreNames.contains(ST_PENDING)) db.createObjectStore(ST_PENDING, { keyPath: 'clientTrxId' });
				if (!db.objectStoreNames.contains(ST_META)) db.createObjectStore(ST_META, { keyPath: 'k' });
			};
			req.onsuccess = function (e) { _db = e.target.result; resolve(_db); };
			req.onerror = function (e) { reject(e.target.error); };
		});
	}
	function tx(store, mode) { return openDb().then(function (db) { return db.transaction(store, mode).objectStore(store); }); }
	function reqP(r) { return new Promise(function (res, rej) { r.onsuccess = function () { res(r.result); }; r.onerror = function () { rej(r.error); }; }); }
	function allOf(store) { return tx(store, 'readonly').then(function (os) { return reqP(os.getAll()); }); }
	function putInto(store, val) { return tx(store, 'readwrite').then(function (os) { return reqP(os.put(val)); }); }
	function delFrom(store, key) { return tx(store, 'readwrite').then(function (os) { return reqP(os.delete(key)); }); }
	function clearStore(store) { return tx(store, 'readwrite').then(function (os) { return reqP(os.clear()); }); }
	function countOf(store) { return tx(store, 'readonly').then(function (os) { return reqP(os.count()); }); }

	function uuid() {
		return 'POS-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 10).toUpperCase();
	}
	function notify() {
		if (typeof cfg.onStatus !== 'function') return;
		pendingCount().then(function (n) { try { cfg.onStatus({ online: navigator.onLine, pending: n }); } catch (e) { } });
	}

	// ---- Produk cache (unduh saat sesi dibuka; dipakai katalog offline) ----
	function refreshProducts() {
		if (!cfg.dataUrl || !navigator.onLine) return Promise.resolve(false);
		return fetch(cfg.dataUrl + (cfg.dataUrl.indexOf('?') >= 0 ? '&' : '?') + 'aksi=data&toko=' + encodeURIComponent(cfg.tokoId || ''))
			.then(function (r) { return r.json(); })
			.then(function (j) {
				if (!j || j.status !== '00' || !j.data) return false;
				return clearStore(ST_PRODUK).then(function () {
					return openDb().then(function (db) {
						return new Promise(function (resolve) {
							var os = db.transaction(ST_PRODUK, 'readwrite').objectStore(ST_PRODUK);
							for (var i = 0; i < j.data.length; i++) os.put(j.data[i]);
							os.transaction.oncomplete = function () { resolve(true); };
						});
					});
				}).then(function () { return putInto(ST_META, { k: 'produkSyncAt', v: Date.now() }); }).then(function () { return true; });
			}).catch(function () { return false; });
	}
	function getProducts() { return allOf(ST_PRODUK); }
	function findByBarcode(kode) {
		if (kode == null) return Promise.resolve(null);
		var k = String(kode).trim().toUpperCase();
		return getProducts().then(function (list) {
			for (var i = 0; i < list.length; i++) {
				if (String(list[i].kode || '').trim().toUpperCase() === k) return list[i];
			}
			return null;
		});
	}

	// ---- Backend penyimpanan transaksi: SQLite (Electron) atau IndexedDB (browser biasa) ----
	// Dipilih SEKALI (memoized di _backend) saat pertama dibutuhkan -- window.electronAPI (diinjeksi
	// preload.js shell desktop) sudah pasti ada/tidak-ada sejak awal load halaman, tidak berubah di
	// tengah sesi, jadi aman dipilih sekali dan dipakai ulang.
	function pilihBackend() {
		if (_backend) return _backend;
		if (typeof window !== 'undefined' && window.electronAPI && window.electronAPI.isElectron && window.electronAPI.localDb) {
			var ldb = window.electronAPI.localDb;
			_backend = {
				simpanBaru: function (trx) { return ldb.simpanBaru(trx); },
				tandaiSinkron: function (clientTrxId) { return ldb.tandaiSinkron(clientTrxId); },
				tandaiGagal: function (clientTrxId, pesan) { return ldb.tandaiGagal(clientTrxId, pesan); },
				listPending: function () { return ldb.listPending(); },
				waktuSinkronTerakhir: function () { return ldb.waktuSinkronTerakhir(); },
				catatKontakServer: function () { return Promise.resolve(); } // tak perlu -- waktuSinkronTerakhir sudah berasal dari baris yg benar2 tersinkron di SQLite
			};
			return _backend;
		}
		// Browser biasa (tanpa Electron): tetap IndexedDB, TAPI dgn semantik yg SAMA dgn SQLite di atas
		// -- baris TIDAK PERNAH dihapus setelah sinkron, hanya field _status berubah jadi 'synced'.
		// Sebelumnya baris dihapus (delFrom) begitu server konfirmasi -- diubah supaya perilaku
		// "simpan permanen sbg cadangan" berlaku konsisten di kedua backend, bukan cuma di Electron.
		_backend = {
			simpanBaru: function (trx) {
				trx._status = 'pending';
				return putInto(ST_PENDING, trx).then(function () { return true; });
			},
			tandaiSinkron: function (clientTrxId) {
				return tx(ST_PENDING, 'readwrite').then(function (os) {
					return new Promise(function (resolve) {
						var getReq = os.get(clientTrxId);
						getReq.onsuccess = function () {
							var row = getReq.result;
							if (!row) { resolve(); return; }
							row._status = 'synced';
							row._syncedAt = Date.now();
							var putReq = os.put(row);
							putReq.onsuccess = function () { resolve(); };
							putReq.onerror = function () { resolve(); };
						};
						getReq.onerror = function () { resolve(); };
					});
				});
			},
			tandaiGagal: function () { return Promise.resolve(); }, // pesan error per-baris blm disimpan di IndexedDB -- tak kritis, hanya dipakai panel diagnosis di shell desktop
			listPending: function () {
				return allOf(ST_PENDING).then(function (list) {
					return list.filter(function (r) { return r._status !== 'synced'; });
				});
			},
			waktuSinkronTerakhir: function () {
				return tx(ST_META, 'readonly').then(function (os) { return reqP(os.get('lastSyncAt')); })
					.then(function (row) { return row ? new Date(row.v).toISOString() : null; }).catch(function () { return null; });
			},
			catatKontakServer: function () { return putInto(ST_META, { k: 'lastSyncAt', v: Date.now() }); }
		};
		return _backend;
	}

	// ---- Transaksi: penyimpanan lokal permanen + sinkron ----
	function submit(trx) {
		trx = trx || {};
		if (!trx.clientTrxId) trx.clientTrxId = uuid();
		if (!trx.waktu) trx.waktu = new Date().toISOString();
		trx.tokoId = trx.tokoId || cfg.tokoId;
		trx.kasir = trx.kasir || cfg.kasir;
		return pilihBackend().simpanBaru(trx).then(function () {
			notify();
			if (navigator.onLine) {
				return syncNow().then(function () { return { queued: true, synced: navigator.onLine, clientTrxId: trx.clientTrxId }; });
			}
			return { queued: true, synced: false, clientTrxId: trx.clientTrxId };
		});
	}

	function syncNow() {
		if (_syncing || !cfg.syncUrl || !navigator.onLine) return Promise.resolve(0);
		_syncing = true;
		var backend = pilihBackend();
		return backend.listPending().then(function (list) {
			if (!list.length) { _syncing = false; return 0; }
			return fetch(cfg.syncUrl + (cfg.syncUrl.indexOf('?') >= 0 ? '&' : '?') + 'aksi=sync', {
				method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
				body: new URLSearchParams({ transaksi: JSON.stringify(list) })
			}).then(function (r) { return r.json(); }).then(function (j) {
				var done = 0;
				var chain = Promise.resolve();
				if (j && j.status === '00' && j.hasil) {
					j.hasil.forEach(function (h) {
						if (h && h.clientTrxId && (h.ok === true || h.duplikat === true)) {
							done++; chain = chain.then(function () { return backend.tandaiSinkron(h.clientTrxId); });
						} else if (h && h.clientTrxId && h.error) {
							chain = chain.then(function () { return backend.tandaiGagal(h.clientTrxId, h.error); });
						}
					});
				}
				// Server berhasil dihubungi (terlepas dari berapa item yang benar2 tersinkron) -> catat
				// sebagai "terakhir kali sukses bicara ke server", dipakai panel Status Sinkronisasi.
				return chain.then(function () { return backend.catatKontakServer(); })
					.then(function () { _syncing = false; notify(); return done; });
			}).catch(function () { _syncing = false; return 0; });
		});
	}
	function pendingCount() { return pilihBackend().listPending().then(function (list) { return list.length; }); }
	function listPending() { return pilihBackend().listPending(); }
	function getLastSyncAt() { return pilihBackend().waktuSinkronTerakhir(); }

	// ---- Online/offline auto-sync ----
	function bindConnectivity() {
		if (_bound) return; _bound = true;
		window.addEventListener('online', function () { notify(); syncNow(); });
		window.addEventListener('offline', function () { notify(); });
		// jaring pengaman: coba sinkron berkala (mis. setelah sinyal lemah)
		setInterval(function () { if (navigator.onLine) syncNow(); }, 30000);
	}

	window.AisPosOffline = {
		init: function (c) {
			c = c || {};
			cfg.tokoId = c.tokoId; cfg.kasir = c.kasir; cfg.dataUrl = c.dataUrl; cfg.syncUrl = c.syncUrl; cfg.onStatus = c.onStatus;
			bindConnectivity();
			notify();
			return refreshProducts().then(function () { notify(); syncNow(); });
		},
		/** Ubah toko sesi (mis. kasir memilih toko lain) lalu unduh ulang cache produknya. */
		setSession: function (c) {
			c = c || {};
			if (c.tokoId !== undefined) cfg.tokoId = c.tokoId;
			if (c.kasir !== undefined) cfg.kasir = c.kasir;
			return refreshProducts().then(function () { notify(); });
		},
		getProducts: getProducts,
		findByBarcode: findByBarcode,
		refreshProducts: refreshProducts,
		submit: submit,
		syncNow: syncNow,
		pendingCount: pendingCount,
		listPending: listPending,
		getLastSyncAt: getLastSyncAt,
		isOnline: function () { return navigator.onLine; }
	};
})();
