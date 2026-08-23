# Full-scale performance, load, and soak result — 2026-08-23

## Keputusan

`PASS_LOCAL_SIT_V1`. Dataset penuh dan seluruh threshold lokal yang ditetapkan sebelum run final terpenuhi. Ini bukan pengganti SLA production atau tanda tangan performance owner.

## Environment dan isolasi

- Target: PostgreSQL 16, database clone `ais_jurnal_sit` pada `localhost:5432`.
- Runtime benchmark: Eclipse Adoptium JDK 8, heap `-Xms512m -Xmx2048m`, Windows lokal/shared host.
- Baseline `ais` dan `streaming_ais` tidak dimutasi.
- Dataset ditempatkan dalam empat tabel `UNLOGGED` production-shaped di schema dedicated `jurnal_perf`; tidak ada BLOB content dan tidak ada WAL durability benchmark.
- Ukuran dataset beserta index setelah distribusi diperbaiki: 2.673 MB.
- Generator dan cleanup mempunyai guard `current_database()='ais_jurnal_sit'`.

## Dataset terverifikasi

| Dimensi | Row | Distribusi |
|---|---:|---|
| Jurnal/artikel | 100 jurnal / 100.000 artikel | 1.000 artikel per jurnal, 10 status per jurnal, 100 published per jurnal |
| Metadata file/revisi | 1.000.000 | 10 file per artikel, 5 stage/bundle, version 1–10 |
| Usage event | 10.000.000 | VIEW/DOWNLOAD, browser/bot, 5 negara, 4 referrer, rentang 30 hari |
| Pengguna multi-role | 10.000 | 100 jurnal, 10 role |

Run awal dibuang sebagai evidence final setelah `EXPLAIN` menemukan korelasi fixture antara journal dan status. Formula generator diperbaiki, cardinality/distribusi diverifikasi ulang, lalu seluruh benchmark dan soak 300 detik diulang tanpa mengubah threshold.

## Threshold SIT lokal v1 dan hasil final

| Metrik | Threshold | Hasil | Status |
|---|---:|---:|---|
| Warm OLTP p95 | ≤ 250 ms | 2,23 ms | PASS |
| Analytics 10 juta row p95 | ≤ 3.000 ms | 1.652,22 ms | PASS |
| Load campuran p95 | ≤ 500 ms | 7,18 ms | PASS |
| Throughput 8 thread | ≥ 50 operasi/detik | 5.344,88 operasi/detik | PASS |
| Error | 0 | 0 | PASS |
| Heap peak | ≤ 1.610.612.736 byte | 203.459.480 byte | PASS |

Evidence tambahan:

- cold-process query: 31,35 ms;
- warm samples: 200; p50 0,43 ms;
- analytic samples: 10;
- soak: 8 thread × 300 detik;
- load operations: 1.608.291;
- query published journal nyata: 25 dari 100 row, execution 0,852 ms, bitmap index scan;
- query metadata file per item: 10 row, execution 0,185 ms;
- query usage per item: 100 row, execution 1,540 ms;
- aggregate 10 juta row: parallel sequential scan, execution 1.873,689 ms pada evidence plan sebelum run final.

Lima pola OLTP yang dicampur selama load adalah dashboard status jurnal, daftar published, metadata file per artikel, usage per artikel, dan role user per jurnal. Analytics mengagregasi event/country pada 10 juta row non-bot.

## Cleanup dan batas klaim

Schema `jurnal_perf` sudah di-drop melalui script guarded setelah evidence diambil; ukuran `ais_jurnal_sit` kembali 516 MB. Pengujian ini menutup dataset/cardinality, SQL read performance, concurrent load, error rate, dan soak lokal lima menit. Yang tetap memerlukan environment production-like dan persetujuan owner: WAL/write throughput, network latency, connection pool/container HTTP, long soak 4–24 jam, peak concurrent user business journey, serta SLA production resmi.
