# Fase 10 — Observability dan konfigurasi JVM (2026-08-20)

## Sudah ada sebelumnya (sesi paralel + optimasi terdahulu)
`ais/common/PerformaSnapshotUtil.java` (1.203 baris) sudah merangkum, via JMX tanpa alat luar:
- Heap, Eden/Survivor/Old/Metaspace/Code Cache (setara kolom `jstat`)
- GC count/durasi per collector
- Jumlah thread, grup thread berdasar pola nama, thread mencurigakan (RUNNABLE di kode aplikasi)
- Kontensi lock (mendeteksi antrean pada pool koneksi)
- Class histogram opsional (BERAT — memicu Full GC, hanya untuk investigasi)
- **Pool c3p0 per factory** lewat refleksi `C3P0Registry` (sibuk/idle) — tidak mengikat versi c3p0
- **Pool async** (`AsyncTaskManager.statistikPool`)
- **Sesi online** (`SecurityFilter.dataOnline.size()`)
- **Slot laporan** (`ReportThrottle.statistik` — membuktikan batas Fase 6 berlaku)
- **Cache memori** (`MemoryCacheUtil.statistik`)

Sampler query lambat: `DatabasePerformanceSampler` merekam query >1 detik ke
`database_performance_sample`, retensi 30 hari (lihat FASE4-CHANGES.md untuk query analisisnya).

## Ditambahkan pada fase ini
**`ais/common/DesktopCounterListener.java` (BARU)** — satu-satunya metrik dari daftar runbook
yang belum terpantau: **jumlah ZK Desktop**.

Mengapa penting: Fase 5 membatasi `max-desktops-per-session` dan `max-pushes-per-session`
menjadi 15. Tanpa pengukuran tidak ada bukti apakah angka itu terlalu ketat (mengganggu
pengguna) atau terlalu longgar (memori terbuang). Tiap desktop menahan seluruh pohon
komponennya di memori sesi, sehingga jumlah desktop hidup = indikator langsung RAM sisi UI.

Yang dilaporkan: `aktif`, `puncak`, `puncak/sesi`, `total dibuat`.
`puncak/sesi` adalah pembanding langsung terhadap ambang 15.

Keamanan rancangan:
- HANYA menyimpan angka (`AtomicInteger`/`AtomicLong`). TIDAK pernah menyimpan referensi
  Desktop/sesi/komponen/entitas → tidak mungkin menjadi sumber kebocoran baru.
- Hitungan per-sesi disimpan sebagai satu `Integer` pada atribut ZK Session → ikut mati
  bersama sesinya.
- TIDAK mencatat ID pengguna, ID sesi, atau parameter URL (bebas PII, bebas label
  ber-cardinality tinggi) sesuai aturan observability runbook.
- Seluruh kegagalan internal ditelan (`try/catch Throwable`) supaya observability tidak
  pernah menggagalkan pembuatan desktop.

Berkas yang berubah:
- `ais/common/DesktopCounterListener.java` (baru)
- `ais/common/PerformaSnapshotUtil.java` — baris `ZK desktop : ...` pada snapshot
- `webapp/WEB-INF/zk.xml` — pendaftaran `<listener>`

## Konfigurasi JVM — REKOMENDASI, bukan default
Runbook menetapkan: jangan memasang flag berdasarkan contoh internet, dan JANGAN menyamarkan
leak dengan heap lebih besar. Ukuran heap final harus ditentukan dari live-set setelah
warm-up + concurrency + margin, BUKAN persentase RAM.

Lingkungan terverifikasi: **Java 8 + Tomcat 9.0.82** (bukan Java 7 seperti asumsi awal runbook,
sehingga PermGen tidak berlaku — yang relevan Metaspace).

Usulan untuk diuji di staging lebih dulu, satu per satu:

| Opsi | Tujuan | Catatan |
|---|---|---|
| `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/var/log/ais` | bukti akar OOM | Pastikan disk cukup (dump ≈ ukuran heap) |
| `-Xlog:gc*` (Java 9+) **atau** `-XX:+PrintGCDetails -Xloggc:... -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=20M` (Java 8) | riwayat GC | Sintaks BERBEDA antar versi — pakai yang sesuai Java 8 |
| `-XX:MaxMetaspaceSize=...` | batas Metaspace | Tetapkan HANYA setelah mengukur puncak Metaspace nyata (10.252 JSP + 184 JAR) |

**Belum diukur** = belum boleh ditetapkan. Jalankan snapshot performa pada beban puncak
dahulu, catat di `BEFORE_AFTER.md`, baru tentukan angkanya.

## Rollback
- Listener: hapus blok `<listener>` di `zk.xml` (kelasnya menjadi tidak aktif tanpa efek lain).
- Snapshot: baris `ZK desktop` berdiri sendiri dalam `try/catch`, aman dihapus.
