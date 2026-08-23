# Baseline Performa AIS — Fase 0

Tanggal: 2026-08-19
Lokasi kerja: `C:\opt\AIS\ais` (working copy TANPA `.git` — commit/branch belum bisa dibuat; keputusan menunggu pemilik repo)
Sumber: `src/main/java` (mirror identik di `src/main/src`) dan `src/main/webapp`

## Lingkungan
- IDE/build: Eclipse (`.classpath`, `.settings/org.eclipse.jdt.core.prefs`), TANPA `build.xml`/`pom.xml` di root proyek.
- **Source/target compiler aktual: 1.8** (bukan 1.6/1.7 seperti asumsi runbook). Kode juga sudah memakai API Java 8 (`ThreadLocalRandom.nextLong(origin,bound)` di `MemoryCacheUtil`).
- Server dev: **Apache Tomcat 9.0.82** (`C:\opt\AIS\Servers`) → butuh Java 8+; tidak ada PermGen (Metaspace). Asumsi "Java 7 + PermGen" pada runbook TIDAK berlaku untuk lingkungan dev ini; versi produksi perlu dikonfirmasi operator.
- Database: PostgreSQL (utama), Hibernate 3.6, ZK 5.5, JasperReports, Spring Security.

## Inventaris file (src/main)
| Jenis | Jumlah |
|---|---:|
| Java | 6.860 |
| JSP | 10.252 |
| ZUL | 1.539 |
| JRXML / Jasper | 806 / 816 |
| HTML | 2.781 |
| JS / CSS | 618 / 339 |

Ukuran: `java/` 370 MB, `webapp/` ±2,2 GB (WEB-INF 474 MB; component 198 MB; help 197 MB; report 67 MB; img 29 MB). `WEB-INF/lib`: **184 JAR, 158 MB**. Direktori besar lain di WEB-INF: `website` 130 MB, `bantuan` 69 MB, `baru` 41 MB, `new` 37 MB, `lib-zk9-ce` 6,5 MB, `o` 13 MB, `z` 9,2 MB, `uiux` 7,4 MB.

## Konfigurasi Hibernate (kredensial DIREDAKSI — tidak dicantumkan)
| File | Mapping | hbm2ddl.auto | Pool |
|---|---:|---|---|
| hibernate.cfg.xml (utama) | **1.533** | **update** | c3p0 min 10 / max 80, timeout 300, statement cache mati |
| hibernate.streaming.cfg.xml | 50 | update | c3p0 min 3 / max 20, unreturnedConnectionTimeout 3600 + debug stack trace AKTIF |
| hibernate.ojs.cfg.xml | 6 | none | `connection.pool_size=2000` (pool bawaan Hibernate, TANPA c3p0) |
| hibernate.radius.cfg.xml | 7 | update | `connection.pool_size=2000` (pool bawaan) |
| hibernate.openfire.cfg.xml | 2 | update | `connection.pool_size=2000` (pool bawaan) |

Catatan: komentar dalam file menunjukkan optimasi terdahulu (pool utama diturunkan 1000→80, keputusan L2 cache streaming dimatikan per 02-07-2026, dsb.).

## Status hipotesis runbook (hasil verifikasi Fase 0)
| # | Hipotesis | Status | Bukti |
|---|---|---|---|
| 1 | Kredensial DB plaintext di config | **TERKONFIRMASI (P0)** | 5 file `hibernate*.cfg.xml` berisi url/username/password plaintext (nilai diredaksi) |
| 2 | Static session map di `Common.java` tanpa cleanup otomatis | **TERKONFIRMASI** | `Common.java:15774` `static HashMap mapSession` key=sessionId; cleanup HANYA via `hapusSession()` eksplisit (28 pemakaian di 17 file, mayoritas form PPDB/PMB); `SessionCounter.sessionDestroyed` TIDAK membersihkannya; HashMap juga tidak thread-safe. Juga ada static map lain: `programs`, `hariLiburPerpustakaans`, `rencanaTahunAkademiks`, `responseCode` (bounded kecil, perlu review ringan) |
| 3 | `MemoryCacheUtil` strong-ref tanpa eviction | **TERKONFIRMASI (sebagian dimitigasi)** | Sudah ditulis-ulang: MapDB/Hazelcast dihapus, kini `ConcurrentHashMap` murni in-JVM; TANPA TTL/eviction/limit (diakui di Javadoc kelas) |
| 4 | Preload "cache semua bila count < N" di `InitDataHelper` | **TERKONFIRMASI (sebagian dimitigasi)** | Pola `rowCount` → `criteria.list()` seluruh tabel masih ada (mis. baris 2436–2548, 3109–3144); mitigasi sudah ada: `statement_timeout`, skip saat COUNT gagal, flag preload utk `EntityAccessCache` |
| 5 | `zk.xml` desktop/push longgar | **SEBAGIAN** | `max-desktops-per-session` sudah 15 (wajar); **`max-pushes-per-session = -1` (tak terbatas) — perlu dibatasi**; `max-requests-per-session=10` |
| 6 | `Report.java` compile runtime, tahan `JasperPrint`, tanpa virtualizer, preview HTML | **SEBAGIAN — virtualizer TIDAK ADA (terkonfirmasi)** | Compile runtime hanya bila JRXML lebih baru dari `.jasper` (compile-to-file, di-reuse); lock kompilasi sudah granular per-file (`lockKompilasiJasperPerNama`, bounded ±816 entri); preview HTML pendamping ada tapi di-skip bila >300 halaman; `JRSwapFileVirtualizer` tidak ditemukan di seluruh codebase |
| 7 | Hibernate utama 1.400+ mapping, hbm2ddl=update, pool longgar | **TERKONFIRMASI dengan koreksi** | 1.533 mapping; `hbm2ddl.auto=update` MASIH aktif di 4 dari 5 config; pool utama SUDAH c3p0 80 (bukan longgar); pool ojs/radius/openfire masih `2000` (pool bawaan Hibernate — nilai ekstrem, tapi pool bawaan hanya membuat koneksi sesuai permintaan) |
| 8 | Ribuan JSP scaffolding | Lihat laporan sweep webapp (Fase 0 lampiran) |
| 9 | WAR ±1 GB berisi dokumentasi/video/bundle duplikat | **TERKONFIRMASI** (webapp source 2,2 GB; rincian di lampiran sweep webapp) |
| 10 | Filter `/*` memproses asset statis | **TERKONFIRMASI (perlu ukur)** | 3 filter `/*`: `ErrorAuditFilter`, `springSecurityFilterChain`, `FilterJSP` (web.xml:31–54); belum diverifikasi apakah ada bypass asset |
| 11 | Java 7 / PermGen | **TIDAK TERKONFIRMASI utk dev** | Compiler 1.8, Tomcat 9.0.82; produksi perlu konfirmasi |

## Lifecycle yang sudah baik (tidak perlu diubah dulu)
- `AppStartupListener.contextDestroyed` sudah men-shutdown SessionFactory (`HibernateUtil.shutdownFactoryQuietly`, `StreamingHibernateUtil.closeFactoryQuietly`), Ehcache CacheManagers, Spring timer.
- `InitData.executor` = `newFixedThreadPool(5)` dengan `shutdown()` + `awaitTermination` + `shutdownNow` fallback (bounded, deterministik).
- `SessionCounter.sessionDestroyed` membersihkan `SecurityFilter.dataOnline` via executor `SESSION_POOL` dengan retry terbatas dan session Hibernate ditutup rapi.

## Metrik runtime
**BELUM DIUKUR** — aplikasi tidak dijalankan pada fase ini. Semua metrik runtime (startup time, RSS, heap, GC, thread, koneksi, latency, report) menunggu instrumen operator. Lihat `BEFORE_AFTER.md` untuk template pengukuran.
