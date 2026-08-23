# Fase 1 — Perubahan yang diterapkan (2026-08-19)

Working copy SVN (`svn://.../ais/src` → `src/main/java`), basis rev 77629. Semua file juga disalin ke mirror `src/main/src`.

**CATATAN COMMIT:** crontab auto-commit pemilik meng-commit perubahan secara otomatis: **r77630** (11 file Tema 1–3), **r77631** (`AmbilDataLampiranFileLain.java`), dan 3 file Tema 4 menyusul pada commit otomatis berikutnya. Artinya perubahan masuk repo SEBELUM smoke test runtime — jalankan checklist validasi di bawah segera; rollback per tema: `svn merge -c -77630 .` (dst.) lalu commit.

## Tema 1 — Pembersihan terpusat registry session/user (P0-3)
| File | Perubahan |
|---|---|
| `ais/common/SessionCounter.java` | `SessionDeleter.run()` kini membersihkan `Common.mapSession` (SEBELUM cek null `onlineUsers`, karena sesi anonim PMB/PPDB tidak ada di `dataOnline`) dan memanggil helper baru `bersihkanRegistryPerUser()`: hapus `MainHelper.logins` (unconditional — key unik per login), lalu bila TIDAK ada sesi aktif lain milik username sama → hapus `SecurityFilter.dataLogin`, `MainAction(.2).desktopWidths/Heights`, dan `mapChat` (dengan `onExit()` untuk menghentikan loop ChatThread) |
| `ais/common/Common.java` | `mapSession` → `Collections.synchronizedMap` (BUKAN ConcurrentHashMap — jalur lama mengizinkan key/value null); method baru `hapusSessionById(String)` |
| `ais/common/SecurityFilter.java` | `dataLogin` → `Collections.synchronizedMap` (perbaikan hazard resize konkuren HashMap; semantik null dipertahankan) |

## Tema 2 — Lifecycle executor (P0-4, P0-6)
| File | Perubahan |
|---|---|
| `ais/common/AsyncTaskManager.java` | ThreadFactory bernama `ais-async-task-N` + daemon; method baru `shutdown()` (shutdown → await 5 dtk → shutdownNow) |
| `ais/action/servlet/DoUpload.java` | `RETRY_SCHEDULER` kini daemon + bernama `doupload-retry`; method baru `hentikanRetryScheduler()` |
| `ais/common/AppStartupListener.java` | `stopSaverSchedulers()` kini juga memanggil `StokThresholdScheduler.hentikan()`, `AsyncTaskManager.shutdown()`, `DoUpload.hentikanRetryScheduler()` |

## Tema 3 — Eksternalisasi kredensial DB (P0-1, tahap 1)
| File | Perubahan |
|---|---|
| `ais/database/hibernate/DbCredentialOverride.java` (BARU) | Membaca `/opt/.g/.h/db.properties` (override via `-Dais.db.override.file`) dan menimpa `hibernate.connection.url/username/password` per prefix factory. File tidak ada → no-op total (kompatibel penuh). Tidak pernah mencetak nilai rahasia |
| `Streaming/Radius/OjsHibernateUtil.java` | Panggil `DbCredentialOverride.terapkan(cfg, "<prefix>")` sebelum `buildSessionFactory()` |
| `ais/database/hibernate/HibernateUtil.java` | Sama untuk prefix `utama` pada kedua jalur build milik aplikasi (mode interceptor + fallback non-zkplus) |
| `docs/performance/db.properties.template` (BARU) | Template tanpa nilai rahasia |

**Batasan yang disadari:** factory utama pada jalur DEFAULT dibangun `org.zkoss.zkplus.hibernate.HibernateSessionFactoryListener` (library ZK) langsung dari `hibernate.cfg.xml` — override `utama` belum efektif di jalur itu. Opsi lanjutan (butuh keputusan + uji): listener kustom pengganti di `zk.xml` yang membangun factory dengan override lalu menyuntik `zkplus._factory` via refleksi (pola yang sudah dipakai `HibernateUtil:290`). Kredensial di `hibernate*.cfg.xml` BELUM dihapus — menunggu berkas eksternal terpasang dan teruji; setelah itu pemilik WAJIB merotasi kredensial.

## Tema 4 — Batas eksplisit map monoton murni (P0-5)
Keempat map berikut diganti LRU ber-batas (`Collections.synchronizedMap(LinkedHashMap access-order + removeEldestEntry)`) — API caller tidak berubah, semantik null dipertahankan:
| File | Map | Batas |
|---|---|---|
| `ais/action/master/helper/generic/AmbilDataLampiranFileLain.java` | `fotoDrive` (slot handoff upload foto/GDrive, key Long acak per dialog) | 500 |
| `ais/action/master/helper/generic/AmbilDataPertemuanFileContent.java` | `mapFileUpload` (handle File konten dari DoUpload) | 1000 |
| `ais/action/servlet/api/LinimasaApi.java` | `mapsLinimasa` (posisi linimasa per token API) | 1000 |
| `ais/action/master/helper/UserOnlineCounter.java` | `mapTanyaJawab` (state tanya-jawab per nomor WhatsApp) | 2000 |

## Tema 5 — Sisa Fase 1: ThreadLocal & reap registry (2026-08-19, lanjutan)
| File | Perubahan |
|---|---|
| `ais/common/EntityIdentityMap.java` | Pola `ReferenceQueue` standar: `KeyedWeakReference` (ingat key sendiri) + `reapStaleEntries()` (poll non-blocking) dipanggil di awal `canonical()/get()/evict()`; `remove(key, value)` dua-argumen agar canonical baru tidak ikut terhapus. Sebelumnya key String + wrapper WeakReference tidak pernah dihapus meski referent sudah di-GC (±100 byte permanen per entity yang pernah dikanonikalisasi) |
| `ais/common/ErrorAuditUtil.java` | `RECORDING.set(null)` → `remove()`; method baru `clearLastResult()` — `LAST_RESULT` sebelumnya menahan `ErrorAuditResult` (content hingga 250 ribu karakter) per worker thread selamanya; `getLastResult()` terverifikasi TIDAK punya caller di Java/JSP/ZUL, API dipertahankan |
| `ais/action/servlet/FilterJSP.java` | `finally` kini juga memanggil `ErrorAuditUtil.clearLastResult()` per akhir request |
| `ais/action/master/helper/profile/ProfileUiHelper.java` | `setSembunyikanPegawai(false)` kini `remove()` alih-alih `set(false)` — semantik identik (pembaca pakai `Boolean.TRUE.equals`), entri ThreadLocal dilepas; pola pemanggilan `finally` di `ProfileGabunganPengguna:265-267` terverifikasi |
| `ais/action/servlet/api/LinimasaApi.java` | `mapsPaging` (7 titik put per token, tanpa remove — terverifikasi) → LRU ber-batas 1000, pola sama dengan `mapsLinimasa` |

## Validasi
- `javac -source 8 -target 8` semua file yang diubah + `-sourcepath` resolusi dependensi: **bersih tanpa error** (JDK 1.8.0_502).
- Belum diuji runtime (Tomcat start, login/logout, redeploy, upload, report) — WAJIB smoke test sebelum commit/deploy:
  1. Start Tomcat → login admin, mahasiswa; logout; tunggu timeout sesi.
  2. Verifikasi menu utama & chat tetap normal setelah login ulang.
  3. Login 2 perangkat user sama (multi-device) → logout salah satu → sesi lain harus tetap hidup.
  4. Redeploy webapp → periksa log Tomcat: warning "failed to stop thread" untuk `ais-async-task-*`/`doupload-retry` harus HILANG.
  5. Upload file (DoUpload) dan jalankan 1 report async (AsyncTaskManager).
  6. Pasang `db.properties` percobaan di dev → verifikasi log `DbCredentialOverride: override eksternal diterapkan ...` muncul untuk streaming/ojs/radius dan koneksi tetap jalan.

## Rollback
`svn revert -R .` pada `src/main/java` (dan salin balik mirror), atau per file. Perubahan tidak mengubah skema DB, URL, maupun format data apa pun.
