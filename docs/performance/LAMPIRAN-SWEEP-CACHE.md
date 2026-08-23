# Lampiran Fase 0 — Sweep Static Collection & Cache

Hasil inventaris statis `src/main/java` (2026-08-19). ±184 static field bertipe koleksi; di bawah ini hanya yang berdampak pada pertumbuhan heap.

## (a) Map ber-key session/user

### Sudah ada cleanup (baik, dengan catatan)
- `SecurityFilter.dataOnline` (`SecurityFilter.java:46`, ConcurrentHashMap key=sessionId) — dibersihkan di `SessionCounter.sessionDestroyed`, logout (`LogoutListener.java:49,86`), dan eviksi single-device (`FilterJSP.java:213`). **Catatan:** value = entity graph Hibernate detached besar (Tbmuser/Mahasiswa/Siswa/Dosen/Fakultas/Jurusan/Sekolah/Yayasan/LogLogin); penghapusan lewat pool 3 thread queue 2000 → saat mass-expiry penghapusan tertunda.

### TANPA cleanup di session destroy — set leak nyata
| Field | Lokasi | Key | Masalah |
|---|---|---|---|
| `SecurityFilter.dataLogin` | `SecurityFilter.java:40` | username | **HashMap tanpa sinkronisasi**; hanya dihapus saat login ulang user sama atau logout eksplisit. Tumbuh = 1 entri per user distinct sejak JVM start, tiap entri memegang entity graph detached. Juga hazard resize konkuren |
| `Common.mapSession` | `Common.java:15774` | sessionId | HashMap plain; hanya dibersihkan via `hapusSession()` (8 alur PMB/PPDB); form yang ditinggalkan bocor permanen |
| `MainHelper.logins` | `MainHelper.java:101` | **LogLogin.getId() (ID baru per login!)** | synchronizedMap, di-put dari 4 call site, **tidak pernah remove** → tumbuh monoton per event login. **Leak per-session tercepat** |
| `MainAction.desktopWidths/Heights/mapChat` + duplikat `MainAction2` | `MainAction.java:198-200`, `MainAction2.java:214-216` | userId | Tidak pernah remove; `mapChat` memegang `ChatThread` hidup (entry ditimpa, tak dihapus) |
| `ApiTokenManager.tokens` | `ApiTokenManager.java:34` | API token | Bounded jumlah token, tapi memegang entity penuh; `removeToken` ada tapi tak diwire ke lifecycle |
| `LinimasaApi.mapsLinimasa` | `LinimasaApi.java:72` | API token | HashMap plain, put 3 titik, **tanpa remove** |
| `AngketUtilApi.maps/maps1` | `AngketUtilApi.java:255,257` | entity ID | Nested HashMap tanpa eviksi |
| `UserOnlineCounter.mapTanyaJawab` | `UserOnlineCounter.java:236` | nomor WhatsApp | Tanpa remove; tumbuh per nomor masuk distinct |
| `ProsesUjianHelper.kuotaUjian` | `ProsesUjianHelper.java:211` | kunci hasil ujian | Dihapus saat submit; ujian yang ditinggalkan bocor sampai clear manual |
| `AmbilDataLampiranFileLain.fotoDrive` | `AmbilDataLampiranFileLain.java:306` | **Long random per request** | **Tumbuh monoton murni, tanpa remove** |
| `AmbilDataPertemuanFileContent.mapFileUpload` | `...:1323` | content ID | HashMap<String,File>, di-put dari `DoUpload.java:233`, tanpa remove |
| `PengumumanAkademisAction.pertemuansHarian` | `...:1602` | tanggal | Nested map entity, 1 entri/hari kalender, tak pernah diprune |
| `PerguruanTinggiAction.perguruanTinggiByDomain` dkk. | `...:199,202` (+Sekolah/Yayasan) | domain | Bounded jumlah tenant (kecil) tapi memegang entity dan tak pernah invalidated |

## (b) Kelas cache dan kebijakan batasnya

**Bounded (baik):** `DashboardCache` (TTL 90 dtk/30 mnt + purge sweep — terbaik), `DashboardCacheUtil` (TTL, purge tak terjadwal), `RingkasanKampusCache` (TTL 5 mnt), `AiTerjemah.CACHE` (cap 60.000, stop-when-full), `TerjemahAiHelper.MEM` (cap 20.000), `EntityAccessCache` (cap 5.000/kelas, TTL 3 hari, prune nyata), `FlagAccessCache` (TTL 7 hari + prune).

**Terlihat bounded tapi TIDAK:**
- `GenericRevisiHelper.COUNT_CACHE` (`:102`) — TTL 120 dtk dicek saat read tapi **entry tak pernah di-remove** → akumulasi selamanya per kunci query distinct.
- `DasboardObeElearningHelper.DATA_CACHE/CACHE_EXPIRY` (`:78,79`) dan `DashboardTrenAktivitasPerkuliahan.DASH_CACHE/DASH_EXPIRY` (`:103,104`) — expiry dicek, entry kadaluarsa yang tak dikunjungi tak pernah keluar; key mengandung kombinasi user/filter.

**Unbounded murni:**
- `MemoryCacheUtil.MAPS` (`:68`) — mirror SELURUH TABEL per kelas, strong reference, tanpa eviksi (diakui Javadoc). **Retainer potensial terbesar.**
- `ElearningRingkasanCache.CACHE` (`:122`) — per pertemuan ID, tanpa TTL/max, hanya `clear()` wholesale.
- `NotifikasiCache.readKeys/legacyBukaIds` (`:163,170`) — marker baca per user×notifikasi, tanpa eviksi (snapshot-nya sendiri self-bounding TTL 45 dtk).
- `EntityIdentityMap.REGISTRY` (`:37`) — WeakReference untuk VALUE, tapi **key String + wrapper WeakReference strong dan tak pernah direap** (tanpa ReferenceQueue) → leak klasik key map weak-value.

**Reference-data statis** (`Common.programs`, `rencanaTahunAkademiks`, `ConstantValues.treeMapFormSop/Ppdb`, `nilaiHurufs`, enam `TreeSet<JenisKegiatan>` di `CommonHelperClass:235-240`, dll.) — bounded ukuran tabel, tapi entity graph di-pin seumur JVM tanpa invalidation.

## (c) Ehcache (`myehcache.xml`, ehcache-core 2.6.11)
Semua region heap-bounded → Ehcache BUKAN risiko heap. Temuan:
1. `StandardQueryCache` maxEntries **5** — query cache praktis tak berguna (thrash konstan).
2. `UpdateTimestampsCache` eternal 5.000 — benar per aturan Hibernate.
3. `maxEntriesLocalDisk=10.000.000` pada defaultCache — risiko disk, bukan heap.
4. `updateCheck="true"` — phone-home saat startup; matikan di produksi.
5. Dua salinan identik (`java/myehcache.xml` dan `src/myehcache.xml` — mirror source).

## Prioritas perbaikan versi sweep ini
1. `MainHelper.logins` — key per event login, nol removal.
2. `AmbilDataLampiranFileLain.fotoDrive` — key random per request.
3. `SecurityFilter.dataLogin` — HashMap non-sync + entity graph + tanpa cleanup session.
4. `MemoryCacheUtil.MAPS` — mirror seluruh tabel tanpa eviksi.
5. `Common.mapSession` — bocor tiap form PMB/PPDB yang ditinggalkan.
6. **Satu titik perbaikan:** wire pembersihan `dataLogin`, `mapSession`, `desktopWidths/Heights`, `mapChat`, `MainHelper.logins` ke `SessionCounter.sessionDestroyed` (saat ini hanya `dataOnline`).
7. `EntityIdentityMap` — tambah drain `ReferenceQueue`.
