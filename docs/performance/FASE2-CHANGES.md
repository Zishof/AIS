# Fase 2 — Cache bounded (2026-08-19)

Lanjutan dari `FASE1-CHANGES.md`. Semua file di-mirror ke `src/main/src`; commit terjadi otomatis via cron pemilik.

## Perubahan
| File | Perubahan |
|---|---|
| `ais/action/master/helper/GenericRevisiHelper.java` | `COUNT_CACHE` → LRU ber-batas 2000 (TTL 120 dtk sebelumnya hanya dicek saat read key sama; kombinasi filter yang tak kembali menumpuk selamanya) |
| `ais/action/master/dashboard/admin/DasboardObeElearningHelper.java` | `purgeExpiredCache()` — sweep entry kadaluarsa dari pasangan `DATA_CACHE`/`CACHE_EXPIRY`, dipanggil saat put (maks. sekali per key per TTL 5 mnt). Race dgn put paralel = cache-miss sekali, bukan masalah correctness |
| `ais/action/master/dashboard/admin/DashboardTrenAktivitasPerkuliahan.java` | `purgeExpiredDashCache()` — pola sama untuk `DASH_CACHE`/`DASH_EXPIRY` |
| `ais/common/ElearningRingkasanCache.java` | Katup pengaman kapasitas `MAKS_ENTRI_CACHE=200000`: saat tercapai, cache di-reset dan agregat dibangun ulang on-demand (`hitungDanSimpan` idempoten); muat-dari-persisten juga berhenti di batas |
| `myehcache.xml` | `updateCheck=false` (phone-home startup), `statistics=false` (tanpa konsumen — diverifikasi grep `getStatistics/LiveCacheStatistics`: nol pemakai), `maxEntriesLocalDisk` 10.000.000 → 100.000 (risiko disk penuh) |

## Keputusan TANPA perubahan kode (hasil verifikasi)
1. **`MemoryCacheUtil.MAPS` — eviksi TIDAK aman.** Kontrak `DataUtil.ambilData(clazz, key)` default `jikaNggakKetemucari=false`: miss dikembalikan `null` TANPA fallback DB, sehingga caller memperlakukan "ada di cache" sebagai "data ada". Menambah TTL/eviksi di lapisan ini dapat membuat data yang ada di DB terlihat hilang. Batas harus dikontrol dari SISI PENGISIAN: preload allowlist di `InitDataHelper` (Fase 3) yang menentukan class mana yang layak full-mirror. Pemakai `MemoryCacheUtil.get` terbatas di: MemoryDbUtil, ConstantValues, DataUtil, InitDataHelper, HasilUjianMahasiswa, ApiTokenManager.
2. **`NotifikasiCache.readKeys`/`legacyBukaIds` — tidak perlu diubah.** Keduanya di-REASSIGN utuh setiap rebuild snapshot (baris 625-626; TTL 45 dtk, jendela `MAKS_SNAPSHOT=1500`) — self-bounding. Temuan sweep "tanpa eviksi" tidak akurat.
3. `StandardQueryCache maxEntriesLocalHeap=5` DIBIARKAN — menaikkannya perlu bukti bahwa query cache Hibernate benar-benar dipakai efektif (kandidat Fase 4 dengan pengukuran hit/miss).

## Validasi
- `javac -source 8 -target 8`: bersih tanpa error (4 file Java; `myehcache.xml` tidak dikompilasi — validasi = startup Tomcat tanpa error konfigurasi Ehcache).
- Smoke test yang relevan: buka dashboard OBE e-learning dan dashboard tren aktivitas dengan beberapa kombinasi filter (hasil harus identik; kunjungan kedua < TTL harus instan); layar revisi generik dengan paging; modul e-learning ringkasan; startup tanpa warning Ehcache.

## Rollback
Per file via `svn merge -c -<rev>`; `myehcache.xml` cukup kembalikan atribut lama.
