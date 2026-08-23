# Rencana Optimasi AIS — hasil Fase 0

Tanggal: 2026-08-19. Semua temuan berbasis analisis statis (belum ada pengukuran runtime — lihat `BEFORE_AFTER.md`). Detail bukti di `BASELINE.md` + tiga lampiran sweep (`LAMPIRAN-SWEEP-THREADING.md`, `LAMPIRAN-SWEEP-CACHE.md`, `LAMPIRAN-SWEEP-WEBAPP.md`).

Catatan git: working copy `C:\opt\AIS\ais` TIDAK memiliki `.git`. Sebelum mengedit kode perlu keputusan pemilik: (a) hubungkan kembali ke clone git yang benar, atau (b) kerjakan langsung di working copy dengan backup manual per file. Sampai itu diputuskan, tidak ada perubahan kode yang dilakukan.

## P0 — keamanan & leak deterministik
| # | Item | Bukti | Perubahan terkecil | Risiko |
|---|---|---|---|---|
| P0-1 | Kredensial DB plaintext di 5 `hibernate*.cfg.xml` | Terkonfirmasi (nilai diredaksi) | Eksternalisasi via placeholder + file properti di luar repo/WAR (atau JNDI Tomcat); template tanpa nilai rahasia; `.gitignore`; **owner merotasi kredensial** | Rendah (config-only), perlu smoke test startup |
| P0-2 | ThreadLocal `RequestContext`/`ResponseContext` tak pernah remove — tiap worker Tomcat mem-pin request/response terakhir | `RequestContext.java:7`, `ResponseContext.java:7`, di-set di `FilterJSP.java:53-54`, `remove()` tanpa caller | Tambah `RequestContext.remove(); ResponseContext.remove();` di blok `finally` FilterJSP (yang sudah membersihkan ThreadLocal Hibernate) | Sangat rendah; verifikasi tak ada pemakaian pasca-response |
| P0-3 | Registry per-session/user tanpa cleanup: `SecurityFilter.dataLogin`, `Common.mapSession`, `MainHelper.logins`, `MainAction(.2).desktopWidths/Heights/mapChat` | Lampiran cache §(a) | Wire pembersihan terpusat di `SessionCounter.SessionDeleter` (satu titik); ganti HashMap → ConcurrentHashMap; `MainHelper.logins` juga butuh perbaikan key (per-login-event → per-session) | Rendah–sedang; butuh test create/destroy session |
| P0-4 | `AsyncTaskManager.EXECUTOR` non-daemon tanpa shutdown + queue tak terbatas | `AsyncTaskManager.java:22` | Tambah shutdown di `AppStartupListener.contextDestroyed`; beri nama thread + daemon; pertimbangkan bounded queue + CallerRunsPolicy | Sedang (pool tersibuk; perilaku antrean berubah — ukur dulu) |
| P0-5 | Leak monoton murni: `AmbilDataLampiranFileLain.fotoDrive` (key random/request), `AmbilDataPertemuanFileContent.mapFileUpload`, `LinimasaApi.mapsLinimasa`, `UserOnlineCounter.mapTanyaJawab` | Lampiran cache §(a) | Per kasus: hapus setelah dipakai / TTL kecil / pindah ke scope request-session | Rendah per item; perlu caller analysis singkat |
| P0-6 | `StokThresholdScheduler` di-start tanpa stop; `DoUpload` executor yatim (baris 74) + `RETRY_SCHEDULER` non-daemon tanpa shutdown | Lampiran threading §(b) | `hentikan()` di stop list; hapus baris yatim; wire shutdown | Sangat rendah |

## P1 — retention besar & concurrency
| # | Item | Perubahan |
|---|---|---|
| P1-1 | `MemoryCacheUtil.MAPS` mirror seluruh tabel tanpa eviksi + preload `InitDataHelper` "count<N → list() semua" | Allowlist data master + limit/TTI per kategori; jangan simpan entity besar; koordinasikan dengan `DataUtil`/`MemoryDbUtil` caller |
| P1-2 | Cache "TTL dicek tapi entry tak dihapus": `GenericRevisiHelper.COUNT_CACHE`, `DasboardObeElearningHelper`, `DashboardTrenAktivitasPerkuliahan`; `ElearningRingkasanCache` & `NotifikasiCache.readKeys` unbounded | Tambah removal saat expired + sweep berkala kecil; batas maksimum |
| P1-3 | `EntityIdentityMap.REGISTRY` — key String + WeakReference wrapper tak direap | Tambah `ReferenceQueue` drain |
| P1-4 | Report tanpa virtualizer (`JRSwapFileVirtualizer` tidak ada di codebase); preview HTML pendamping default (guard >300 hal. sudah ada) | Virtualizer per job + cleanup `finally`; batasi concurrency report (semaphore); preview jadi opsional |
| P1-5 | Raw `new Thread` per request: `Wa.java` (5 titik), controller `newui`, 20 window laporan; pool per-request belum lewat `DbThreadPool.safe` (mis. `PembayaranOnline.java:2813`) | Arahkan ke executor terkelola bersama; sisanya bertahap |
| P1-6 | `zk.xml` `max-pushes-per-session=-1` | Beri batas eksplisit; uji modul yang memakai server push |
| P1-7 | `SecurityFilter.dataOnline` value = entity graph besar | Simpan DTO kecil/ID, bukan entity graph — perubahan lebih besar, butuh caller analysis |
| P1-8 | ThreadLocal `ProfileUiHelper.sembunyikanPegawai` (flag basi lintas request — bug korektness), `ErrorAuditUtil.LAST_RESULT` | Tambah remove di akhir pemakaian/finally |

## P2 — konfigurasi & artifact
| # | Item |
|---|---|
| P2-1 | `hbm2ddl.auto=update` di 4 config → migrasi SQL versioned + `validate` (gate deployment terpisah, risiko tinggi — perlu inventaris perubahan otomatis dulu) |
| P2-2 | Pool `2000` di ojs/radius/openfire (pool bawaan Hibernate) → nilai konservatif; total semua pool < batas PostgreSQL |
| P2-3 | `hibernate.c3p0.debugUnreturnedConnectionStackTraces=true` di streaming → matikan setelah leak diperbaiki (sisakan flag) |
| P2-4 | Ehcache: `updateCheck=false`; `StandardQueryCache` maxEntries 5 → naikkan bila query cache memang dipakai; review `maxEntriesLocalDisk=10 juta` |
| P2-5 | WAR produksi: exclude `WEB-INF/website` (128 MB), `help/` (196 MB), `tbu_penawaran/` (19 MB), `lib-zk9-ce`, `sapto`, `report/**.bak`, source map, video demo → **±345 MB, risiko hampir nol** |
| P2-6 | JAR: keluarkan `servlet_.jar` (KRITIS — Servlet API di WAR), jetty 6, `ooxml-schemas-1.1` (dobel dgn poi-ooxml-schemas-3.17), salah satu jasperreports/javaflow, `styles-1.0.1-SNAPSHOT` (7 MB tanpa class) → ±45–50 MB; hapus per kelompok kecil + smoke test |
| P2-7 | Filter `/*` (`ErrorAuditFilter`, springSecurityFilterChain, `FilterJSP`): bypass asset statis publik sedini mungkin (verifikasi path dulu) |

## P3 — konsolidasi & maintainability
- 8.989 dari 10.252 JSP (87,7%) adalah scaffolding generated (7.871 via `generate_new_jsp_scaffold.py` → dispatcher `WEB-INF/new/_shared/ui/page.jsp`; 1.118 one-liner `DynamicJspCrudGenerator`) → route registry/servlet bersama, migrasi per modul dengan parity test URL.
- ±55 ThreadLocal formatter di `Common.java` → konsolidasi bertahap.
- Pecah `Common.java` (21.019 baris) — hanya setelah P0/P1 selesai, tanpa mengubah kontrak publik.

## Gate antar fase
Sesuai runbook: setiap kelompok perubahan harus compile + smoke test sebelum lanjut; perubahan berisiko tinggi (hbm2ddl, pool, JSP consolidation) menunggu keputusan pemilik dan lingkungan uji.
