# Fase 4 — Hibernate, query, dan connection pool

Tanggal: 2026-08-19

## Kondisi awal: sebagian besar SUDAH dikerjakan sesi lain

Saat memulai fase ini, verifikasi menunjukkan item konfigurasi Fase 4 sudah selesai dan ter-commit
oleh sesi kerja paralel. Dicatat di sini supaya tidak dikerjakan ulang:

| Item Fase 4 | Status | Bukti |
|---|---|---|
| Pool OJS/Radius/Openfire `connection.pool_size=2000` | **SELESAI** | Ketiganya kini memakai c3p0 `max_size=5`, `min_size=0` |
| Pool utama & streaming | **SELESAI** | c3p0 80 (utama), 20 (streaming) |
| `debugUnreturnedConnectionStackTraces` (diagnostik mahal) | **SELESAI** | Kini `false` di config utama |
| SessionFactory OJS/Radius tidak ditutup saat shutdown | **SELESAI** | `closeFactoryQuietly()` ditambahkan + diwire ke `AppStartupListener` |
| Satu SessionFactory bersama (bukan per-request) | **TERVERIFIKASI** | Pola double-checked locking di `HibernateUtil` |

Total koneksi maksimum per JVM sekarang: **80 + 20 + 5 + 5 + 5 = 115** (sebelumnya secara teori
bisa meledak karena tiga pool ber-nilai 2000). Angka ini masih perlu dicek terhadap `max_connections`
PostgreSQL dikali jumlah JVM — lihat bagian pengukuran di bawah.

## Yang dikerjakan pada sesi ini

### Metrik connection pool (`ais/common/DatabasePerformanceSampler.java`)

Runbook Fase 4 meminta metrik "active, idle, pending/wait". Sampler yang ada hanya merekam
query lambat dari `pg_stat_activity`, belum ada metrik pool sama sekali — padahal justru itu
yang dibutuhkan untuk MEMBUKTIKAN apakah ukuran pool baru sudah tepat.

Ditambahkan `ambilSampelPool()` yang merekam SELURUH pool c3p0 dalam JVM sekaligus (utama,
streaming, OJS, Radius, Openfire), berjalan menumpang siklus sampling yang sudah ada
(tiap 5 menit, mulai 2 menit setelah boot).

- Diakses lewat **refleksi** (`com.mchange.v2.c3p0.C3P0Registry`) agar kelas ini tetap jalan bila
  provider pool berganti atau c3p0 tidak ada; seluruh kegagalan ditelan karena murni diagnostik.
- **Tanpa migrasi schema baru** — memakai kolom yang sudah ada di `database_performance_sample`.
- `source='POOL'` sengaja dibedakan agar analisis query lambat (`ACTIVE_QUERY`) tidak tercampur.

Pemetaan kolom: `query_fingerprint`=nama pool, `duration_ms`=thread MENUNGGU koneksi,
`calls`=koneksi terpakai, `rows_count`=total koneksi, `detail`=ringkasan terbaca.

### Cara membaca hasilnya

```sql
SELECT captured_at, query_fingerprint AS pool, duration_ms AS menunggu,
       calls AS terpakai, rows_count AS total, detail
FROM public.database_performance_sample
WHERE source = 'POOL' AND captured_at > now() - interval '24 hours'
ORDER BY captured_at DESC;
```

Cara menafsirkan:
- `menunggu` konsisten **> 0** → pool KEKECILAN, naikkan `hibernate.c3p0.max_size` pool tersebut.
- `menunggu` selalu 0 DAN `terpakai` jauh di bawah max → pool masih bisa diperkecil (hemat RAM & koneksi DB).
- Bandingkan `SUM(total)` seluruh pool × jumlah JVM terhadap `SHOW max_connections` PostgreSQL.

## TIDAK dikerjakan — dan alasannya

### 1. `hbm2ddl.auto=update` (masih aktif di 4 dari 5 config) — BUTUH KEPUTUSAN PEMILIK

Status saat ini: utama=`update`, streaming=`update`, radius=`update`, openfire=`update`, ojs=`none`.

Runbook menetapkan ini sebagai **gate deployment terpisah** dan masuk *stopping rule*: mematikannya
tanpa migration SQL yang versioned lebih dulu akan membuat kolom baru BERHENTI dibuat otomatis,
sehingga fitur yang mengandalkan kolom baru langsung rusak di produksi. Prasyaratnya:
inventaris perubahan schema yang selama ini terjadi otomatis, migration SQL versioned, backup,
dan rollback plan. Infrastruktur migrasi sudah ada (`RetailDatabaseMigrations` + `ais_schema_history`),
jadi jalurnya tersedia — tinggal keputusan dan jadwal maintenance window Anda.

### 2. Batas/pagination pada 5.329 pemanggilan `.list()`

Menambahkan `setMaxResults` secara massal TIDAK dilakukan. Sebagian besar query itu melayani
ekspor, laporan, dan proses batch yang memang HARUS memuat seluruh baris — memotongnya berarti
menghasilkan **data yang salah secara diam-diam**, kerusakan yang jauh lebih mahal daripada
query lambat. Runbook juga melarang optimasi berbasis asumsi ("cari caller, ukur dampak").

Jalur yang benar: pakai data `ACTIVE_QUERY` dari sampler (query > 1 detik, sudah berjalan) untuk
menemukan query yang benar-benar bermasalah, lalu perbaiki per kasus dengan bukti.

```sql
SELECT count(*) AS kemunculan, max(duration_ms) AS terlama_ms, min(detail) AS contoh_query
FROM public.database_performance_sample
WHERE source = 'ACTIVE_QUERY' AND captured_at > now() - interval '7 days'
GROUP BY query_fingerprint ORDER BY kemunculan DESC LIMIT 20;
```

## Validasi

- Kompilasi bersih (`javac -source 8 -target 8`), Java 7/8 style (tanpa lambda/stream/diamond).
- Belum diuji runtime. Setelah deploy: tunggu ±7 menit, lalu jalankan query `source='POOL'` di atas —
  harus muncul satu baris per pool aktif tiap 5 menit.
- Risiko: sangat rendah (murni tambahan baca-saja, seluruh kegagalan ditelan, tidak mengubah perilaku).
- Rollback: `svn merge -c -<rev>` pada `DatabasePerformanceSampler.java`.
