# 101 — Kredensial yang ternyata tidak pernah dibutuhkan

Tanggal: 2026-09-03

Dok. 100 menemukan bahwa satu harness bersandar-basis-data membuat sembilan
belas lainnya ikut dianggap tak dapat dijalankan. Batch ini menyelesaikan sisa
satunya — dan menemukan bahwa ia pun tidak pernah benar-benar terhalang.

## 1. Kendala yang salah alamat

Sejak dok. 82, kalimat ini berdiri: *"kredensial basis data UAT ditolak,
sehingga harness bersandar-basis-data belum pernah berjalan."*

Kredensialnya memang ditolak. Tetapi harness itu **tidak pernah meminta
kredensial UAT.** Membacanya menunjukkan ia dirancang persis untuk keadaan ini:

```java
String url = bersihkan(System.getProperty(URL_PROPERTY));
if (url.length() == 0) { ... SKIPPED ... return; }
validasiTargetUat(url);
...
String schema = "inventory_uat_" + String.valueOf(System.currentTimeMillis());
buatSchemaUat(url, user, password, schema);
try { ujiDuaKoneksi(...); } finally { hapusSchemaUat(...); }
```

* Tanpa properti, ia melewati diri sendiri dengan tenang.
* Ia membuat **schema temporer** bernama unik, tidak pernah menyentuh schema
  aplikasi, dan menghapusnya di `finally`.
* Penjaganya, `validasiTargetUat`, secara eksplisit **mengizinkan
  `//localhost`** — target jauh justru yang ditolak kecuali dinyatakan tegas.

Jadi yang dibutuhkan bukan kredensial UAT, melainkan PostgreSQL apa pun.
Penulisnya sudah menyiapkan jalannya; yang tidak pernah terjadi adalah ada yang
membaca sampai ke sana.

## 2. Dijalankan

PostgreSQL 16 sudah terpasang di mesin ini. Klaster **sekali-pakai** dibuat di
direktori sementara, di port 55432, tanpa menyentuh klaster mana pun yang sudah
ada:

```
initdb -D <dir> -U uat -A trust -E UTF8
pg_ctl -D <dir> -o "-p 55432 -c listen_addresses=localhost" -l log start
psql   -h localhost -p 55432 -U uat -d postgres -c "CREATE DATABASE inventory_uat;"
```

Hasilnya:

```
UAT PostgreSqlInventoryLedgerIntegration: LULUS
```

Klaster dihentikan dan dibuang sesudahnya. Layanan PostgreSQL utama mesin ini
tidak pernah disentuh — diperiksa masih `Running` setelah pembongkaran.

**Dua puluh dari dua puluh harness kini terbukti lulus.**

## 3. Diteruskan ke alatnya

`alat/uji-uat-java.py` menerima `--db-url`/`--db-user`. Tanpa itu, perilakunya
persis seperti sebelumnya: harness ber-DB dilewati dan namanya disebut. Dengan
itu, ia ikut berjalan.

Barisnya juga diperbaiki: sebelumnya alat tetap mencetak "dilewati (bersandar
DB)" padahal harness itu sedang dijalankan. Alat yang keliru menggambarkan
larinya sendiri persis kelas cacat yang dikejar dok. 90, 92, dan 93 — di alat
yang dibuat untuk mengejarnya.

Resep klaster sekali-pakainya ditulis di kepala berkas itu, bukan di dokumen
ini saja, supaya yang menjalankannya tidak perlu menemukannya lagi.

## 4. Tiga kali berturut-turut

| Dok. | Kalimatnya | Kenyataannya |
|---|---|---|
| 98 | "tidak ada toolchain Dart/Flutter" | ada di `C:\opt\flutter`, hanya tidak di PATH |
| 100 | "harness UAT tidak dapat dijalankan" | 19 dari 20 tidak menyentuh basis data sama sekali |
| 101 | "kredensial UAT ditolak, jadi harness ber-DB terhalang" | harness itu hanya butuh PostgreSQL mana pun, dan menyediakan jalannya sendiri |

Ketiganya berbentuk sama: **sebuah kalimat yang menjelaskan mengapa sesuatu
tidak dikerjakan, yang tidak pernah diperiksa ulang.** Ketiganya salah.
Ketiganya membebaskan saya dari pekerjaan.

Dan ketiganya runtuh oleh usaha yang sama kecilnya: satu pencarian berkas, satu
`grep`, satu pembacaan sumber sampai selesai.

## 5. Yang dipelajari

**Alasan menumpuk bunga.** "Kredensial ditolak" ditulis di dok. 82. Sejak itu ia
disalin ke dok. 97, dipakai untuk menjelaskan mengapa A.1 dan A.5 tidak dapat
dinilai, dan menempel ke seluruh direktori `src/test`. Satu kalimat yang tidak
diperiksa menjadi dasar bagi empat kesimpulan lain.

**Kode yang dirancang untuk dijalankan biasanya memberi tahu caranya.** Harness
itu punya properti, penjaga localhost, schema temporer, dan pembersihan di
`finally` — seluruh kelengkapan sebuah berkas yang menunggu dijalankan. Semua
itu terbaca dalam dua puluh baris pertama. Yang kurang bukan aksesnya, melainkan
membaca sampai ke sana.
