# Fase 7-10 — JSP/Metaspace, ukuran artifact, efisiensi, observability (2026-08-19)

## FASE 7 — JSP, scaffolding, filter, Metaspace

### Diterapkan: konfigurasi JasperServlet untuk produksi (`WEB-INF/web.xml`)

Aplikasi punya **10.252 berkas JSP**. Secara default Tomcat menahan kelas hasil kompilasi SETIAP JSP yang pernah diakses, tanpa batas — Metaspace tumbuh terus seiring makin banyak layar dibuka, dan redeploy makin lambat. Sebelumnya aplikasi TIDAK menimpa setelan Jasper sama sekali.

| Parameter | Nilai | Alasan |
|---|---|---|
| `development` | `false` | Menghentikan pengecekan perubahan berkas .jsp pada SETIAP request |
| `checkInterval` | `0` | Eksplisit (hanya relevan saat development=true) |
| `mappedfile` | `false` | Jasper tak lagi membuat satu `write()` per baris JSP → kelas jauh lebih kecil |
| `maxLoadedJsps` | `500` | **Pengaman utama**: JSP paling lama tak dipakai dibongkar (LRU) |
| `jspIdleTimeout` | `1800` | JSP menganggur lebih dari 30 menit dibongkar dari memori |

`maxLoadedJsps` dan `jspIdleTimeout` didukung Tomcat 7+; server di sini **Tomcat 9.0.82**.

> **PERHATIAN mesin pengembangan:** dengan `development=false`, perubahan berkas JSP TIDAK otomatis termuat — perlu redeploy/restart. Set kembali `true` di lingkungan dev.

### Tidak diubah: bypass asset statis di filter

`FilterJSP` **sudah** melewatkan asset statis (.css/.js/.png/.jpg/.svg) langsung ke rantai filter tanpa routing, dan `springSecurityFilterChain` berjalan LEBIH DULU sehingga otorisasi tidak terlewat. Sisa beban untuk asset statis hanya 2 set ThreadLocal + header CORS. **Sengaja tidak dipindah lebih awal** karena `addCorsHeader` dibutuhkan web-font lintas origin — memindahkannya berisiko mematahkan pemuatan font demi penghematan yang kecil.

Catatan untuk pemilik: predikat `isIgnoredPath` memakai pencocokan sangat longgar (`p.contains("al")`, `p.contains("pdf")`) sehingga banyak URL sah ikut terlewat dari routing. Ini tidak menimbulkan lubang keamanan (Spring Security jalan lebih dulu), tetapi sebaiknya dipersempit — **belum diubah** karena perlu uji regresi routing menyeluruh.

### DITUNDA (butuh keputusan + uji parity): konsolidasi 8.989 JSP scaffolding

Dari 10.252 JSP, **8.989 (87,7%) tidak memiliki logika unik**:
- 7.871 berkas hasil `generate_new_jsp_scaffold.py` — hanya metadata `request.setAttribute("nui*")` lalu `jsp:include` ke dispatcher bersama `WEB-INF/new/_shared/ui/page.jsp`;
- 1.118 berkas one-liner `DynamicJspCrudGenerator.generate(<Entity>.class)`.

Hanya sekitar 1.260 JSP berisi logika nyata. Secara teori semuanya bisa diganti satu route registry + servlet dispatcher. **Tidak dikerjakan sekarang** karena: (a) wajib parity test URL/permission/output per modul; (b) tanpa lingkungan uji, risiko mematahkan ribuan URL bookmark terlalu besar. `maxLoadedJsps` di atas sudah memberi sebagian besar manfaatnya (batas Metaspace) dengan risiko mendekati nol.

---

## FASE 8 — Ukuran artifact dan dependency

### DITEMUKAN & DITUTUP: berkas internal dapat diunduh publik (P0 keamanan)

Sebelum perubahan ini **TIDAK ADA satu pun `security-constraint` di web.xml**. Berkas berikut ada di ROOT webapp (bukan di bawah WEB-INF) sehingga dapat diunduh siapa pun lewat HTTP:

| Berkas | Isi |
|---|---|
| `update-web-auto.sh`, `update.sh`, `commit.sh` | **kredensial repositori dalam teks biasa** |
| `cascade.sql` (64 KB) | struktur dan relasi basis data |

**Diterapkan:** `security-constraint` dengan `auth-constraint` KOSONG (idiom "deny all") untuk pola `*.sh`, `*.sql`, `*.bak`. Skrip tetap berfungsi karena dijalankan dari filesystem, bukan HTTP.

**WAJIB ditindaklanjuti pemilik sistem** (tidak bisa dikerjakan dari sini):
1. **ROTASI kredensial repositori** yang terlanjur tertulis di skrip tersebut.
2. Pindahkan skrip deployment ke luar direktori webapp.
3. Pertimbangkan pembersihan riwayat SVN sebagai pekerjaan keamanan terpisah.

### Diterapkan: perangkat deployment ramping

- `docs/performance/deploy-exclude.txt` — daftar pengecualian terverifikasi (sekitar 345 MB).
- `docs/performance/deploy-lean.sh` — penyalin berbasis rsync; **hanya menyalin, tidak menghapus apa pun di sumber**. Jalankan dengan `MODE_UJI=1` dulu untuk melihat rencana salinnya.

Isi pengecualian: microsite pemasaran `WEB-INF/website/` (128 MB, nol referensi kode, di bawah WEB-INF sehingga memang tak terjangkau browser), dokumen panduan `help/` (196 MB), materi penawaran (19 MB), staging `lib-zk9-ce/` (6,5 MB, tidak di classpath), dump pribadi `sapto/`, source map, video demo, serta berkas skrip/SQL.

### BELUM dikerjakan: pembersihan JAR (butuh persetujuan)

| JAR | Masalah | Perkiraan |
|---|---|---|
| **`servlet_.jar`** | **KRITIS** — membundel `javax.servlet.*` di dalam WAR; harus disediakan container. Berisiko `LinkageError` | — |
| `ooxml-schemas-1.1.jar` | Tumpang tindih dengan `poi-ooxml-schemas-3.17.jar` (dua generasi, package sama) | −14 MB |
| `jasperreports-javaflow-6.4.1.jar` | Build alternatif dari artifact yang sama | −5,4 MB |
| `jetty-6.1.26` + `jetty-util` | Servlet container tertanam di dalam WAR | — |
| `styles-1.0.1-SNAPSHOT.jar` | 7,3 MB, **nol berkas .class**, versi SNAPSHOT di produksi | −7,3 MB |

Total sekitar 45–50 MB. **Tidak dihapus** karena runbook melarang menghapus JAR berdasarkan analisis statis saja — reflection, taglib, ServiceLoader, dan ekspresi JasperReports tidak terlihat oleh pencarian `import`. Prosedur aman: buang SATU kelompok kecil → build → startup + smoke test → lanjut.

---

## FASE 9 — Efisiensi kode

Sesuai urutan runbook, fase ini baru dikerjakan setelah kebocoran, cache, sesi, laporan, query, dan artifact ditangani. Yang sudah tercapai sebagai efek samping fase sebelumnya:

- thread mentah per operasi diganti pool daemon berbatas (Fase 5);
- sekitar 55 ThreadLocal formatter di `Common.java` tetap dipertahankan (idiom thread-safe yang benar, bukan kebocoran);
- `Common.java` 21.019 baris — pemecahan kelas **sengaja tidak dilakukan**: manfaatnya pada maintainability, bukan RAM, sementara risiko regresinya tinggi pada kelas yang disentuh hampir seluruh aplikasi. Runbook sendiri melarang mengklaim ini sebagai penghematan heap.

Rekomendasi berbasis bukti (butuh profiler lebih dulu, jangan tebak): jalankan allocation sampling pada 3 layar tersibuk, baru optimalkan hot path yang benar-benar muncul.

---

## FASE 10 — Observability dan konfigurasi JVM

### Sudah tersedia di aplikasi (tidak perlu dibuat baru)

`DatabasePerformanceSampler`, `PerformaSnapshotUtil`, `ErrorAuditUtil` (dengan dedup), dan layar monitoring flag cache. Semuanya sudah di-shutdown rapi sejak Fase 1.

### Yang perlu diukur operator (metrik kunci pasca-optimasi)

| Metrik | Cara ambil | Kenapa penting |
|---|---|---|
| Thread http-nio | `jstack <PID>` lalu hitung baris http-nio | Membuktikan kebocoran server push (Fase 5) berhenti |
| Koneksi per pool | `SELECT count(*) FROM pg_stat_activity WHERE datname='<db>'` | Membuktikan batas pool (Fase 4) berlaku |
| Metaspace | `jstat -gcutil <PID> 5000 12` (kolom M/CCS) | Membuktikan `maxLoadedJsps` (Fase 7) membatasi kelas JSP |
| Heap setelah full GC | `jmap -histo:live` (dev/staging saja) | Live-set sebenarnya untuk menentukan `-Xmx` |
| JSP termuat | Tomcat Manager / JMX Jasper | Harus berhenti di sekitar 500 |

### Rekomendasi JVM (JANGAN diterapkan sebelum diukur)

Runtime nyata: **Java 8 + Tomcat 9.0.82** (bukan Java 7 seperti asumsi runbook; tidak ada PermGen, yang berlaku adalah Metaspace).

```
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/ais/dump
-Xloggc:/opt/ais/log/gc.log -XX:+UseGCLogFileRotation
-XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=20M
-XX:MaxMetaspaceSize=512m
```

`-Xmx` **sengaja tidak direkomendasikan angkanya**: harus ditentukan dari live-set setelah warm-up + concurrency + margin, BUKAN persentase RAM. Menaikkan heap tanpa mengukur hanya menyamarkan kebocoran — dan itu justru yang dilarang runbook.

---

## Validasi Fase 7-10

- `web.xml` tervalidasi **XML VALID** setelah kedua perubahan (Jasper + security-constraint).
- Tidak ada perubahan kode Java pada fase ini; tidak ada risiko regresi kompilasi.
- **Belum diukur:** seluruh angka Metaspace/ukuran deploy. Perintah pengukuran ada di atas.

### Smoke test wajib

1. **Paling kritis:** start Tomcat, aplikasi harus jalan normal. Bila `web.xml` bermasalah, aplikasi gagal start total.
2. Buka 10–15 layar JSP berbeda, semua harus tampil normal (bukti `development=false` + `mappedfile=false` tidak mengubah render).
3. Coba akses `https://host/<context>/update.sh` dan `/cascade.sql` — **harus 403**, bukan unduhan.
4. Pantau Metaspace selama sesi panjang — harus datar, tidak menanjak terus.
5. Jalankan `MODE_UJI=1 ./deploy-lean.sh <sumber> <tujuan>` — periksa daftar yang akan dilewati sebelum dipakai sungguhan.

## Rollback

- `web.xml`: hapus blok `<servlet>` bernama `jsp` dan/atau blok `<security-constraint>` yang ditambahkan (keduanya berdiri sendiri, aman dihapus terpisah).
- Skrip deployment bersifat tambahan — tidak mengubah apa pun bila tidak dijalankan.
