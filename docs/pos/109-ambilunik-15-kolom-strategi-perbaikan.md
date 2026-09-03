# 109. `GrupTransaksi.ambilUnik()` — 15 kolom referensi tidak dikenali: strategi perbaikan

Tindak lanjut dari sesi Javadoc r83906 (`ais/database/model/akunting/GrupTransaksi.java`),
yang mendokumentasikan tanpa mengubah logika. Dokumen ini menutup permintaan verifikasi
dampak + strategi perbaikan; lihat juga [[ais-mesin-posting-pattern]] (memori sesi) butir
"Tabrakan `kodeUnik` antar-kaki" untuk latar cacat sejenis yang sudah diperbaiki (dok
pos/71).

## 1. Ringkasan cacat

`GrupTransaksi` punya 41 kolom referensi dokumen sumber. `ambilUnik()` menyusun kunci
idempotensi jurnal = `<nama kelas dokumen>_<id>` + `ref`, tetapi rantai `if/else`-nya
hanya mengenali 26 kolom. Lima belas tidak dikenali:

```
logPembayaran, penerimaanPengadaanMasterAsset, saldoAwalMasterAsset,
pertangungjawabanKasBesar, transaksiKoperasi, pajak, pembayaranGaji,
pembatalanTransaksiKantin, penghapusanMasterAsset, pembayaranAnggotaKoperasi,
pencairanDiskon, penyesuaianSaldoAnggota, modalPenyertaanKoperasi, pembagianShu,
notaSalesBiaya
```

Untuk kolom-kolom ini, `kodeUnik` runtuh menjadi **hanya nilai `ref`** (atau `null` bila
`ref` juga kosong). `CommonAkunting.saveTransaksi` (baris ~700) memakai `kodeUnik` sebagai
kunci dedup: bila sudah ada `GrupTransaksi` ber-`kodeUnik` sama, grup lama hanya dicap
ulang — **jurnal baru tidak ditulis**.

Dua jalur nyata terbukti dari pembacaan kode (bukan asumsi):

| Jalur | `ref` yang dipakai | Akibat |
|---|---|---|
| `PostingPertangungjawabanPajakAction.java:1409-1410` (`reference=Pajak`) | `REF_PAJAK` = `"pajak"` | `kodeUnik` selalu string `"pajak"` untuk **seluruh** dokumen pajak LPJ di instalasi |
| `PostingDpPemesananPekerjaanAction.java:1180-1197,1724-1729` (`reference=SaldoAwalMasterAsset`) | literal `"DP_PEKERJAAN"` | `kodeUnik` selalu `"DP_PEKERJAAN"` untuk **seluruh** dokumen DP pekerjaan vendor |

Dua akibat kebalikan (lihat Javadoc kelas untuk uraian lengkap):

- **`ref` terisi** → kunci global lintas dokumen/tenant → hanya jurnal **pertama** yang
  pernah tertulis dengan `ref` itu yang benar-benar ada; posting berikutnya dianggap
  duplikat, jurnalnya **hilang tanpa galat**, cap posting dokumen lain ikut tertimpa.
- **`ref` kosong** → `kodeUnik = null` → `kode_unik = NULL` tidak pernah cocok di SQL →
  posting ulang dokumen yang sama menerbitkan jurnal **duplikat**.

## 2. Verifikasi dampak — TIDAK bisa diselesaikan dari mesin kerja ini

Mesin kerja sesi ini (dan seluruh sesi paralel yang tercatat di riwayat, sejak
2026-08-19) hanya berhasil tersambung ke instans Postgres **lokal efemeral** yang dibuat
sendiri per sesi (`initdb` + fixture, kosong dari jurnal nyata). Kredensial
`jdbc:postgresql://localhost:5432/ais` user `root` yang tercatat di
`C:\opt\AIS\.metadata\...\META-INF\context.xml` **ditolak otentikasi** — dicoba ulang
dua kali dengan hasil identik, dan riwayat pencarian transkrip sesi menunjukkan kegagalan
yang sama berulang di lebih dari 10 sesi berbeda sejak Agustus. Tidak ada kredensial DB
UAT/produksi bermuatan data nyata yang bisa diakses dari lingkungan kerja Claude Code
manapun sejauh ini.

**Karena itu angka dampak nyata (berapa dokumen kena, berapa nilai rupiah) tidak bisa
diisi dari sesi ini.** Skrip hanya-baca sudah disiapkan di
[`docs/sql/2026-09-03-diagnosa-ambilunik-15-kolom-hilang.sql`](sql/2026-09-03-diagnosa-ambilunik-15-kolom-hilang.sql)
mengikuti pola skrip diagnosa r78713/r78717 (`2026-08-31-diagnosa-tabrakan-kodeunik-tagihan.sql`).
**Seseorang dengan akses baca ke database UAT/produksi wajib menjalankannya** sebelum
langkah pemulihan data dieksekusi. Yang skrip itu jawab:

- **Q0** — apakah `kode_unik` benar-benar punya *unique index* di database (anotasi
  `@Column(unique = true)` di Java tidak otomatis berarti constraint itu berhasil
  ditegakkan `hbm2ddl.auto=update` bila data lama sudah melanggarnya saat pertama kali
  dicoba).
- **Q1/Q4** — per kolom: berapa dokumen bercap posting dengan `ref IS NULL` (kandidat
  kasus duplikat), dan berapa dokumen di tabel sumbernya pernah diposting sama sekali
  (kolom dengan nol dokumen bercap = tidak ada dampak nyata, aman ditunda).
- **Q3** — bukti langsung tabrakan global: berapa kali `ref = 'pajak'` /
  `'DP_PEKERJAAN'` dipakai ulang oleh **dokumen sumber yang berbeda** (`distinct <kolom>
  referensi> 1` sekaligus `jumlah_grup > 1`).
- **Q5** — tabrakan id `TransaksiPegawai` vs `PembayaranGajiPunyaPegawai` (lihat §4).

## 3. Strategi perbaikan `ambilUnik()` untuk 15 kolom — BELUM diterapkan

**Kesimpulan analisis kode (tanpa perlu data produksi):** menambah 15 cabang baru pada
`ambilUnik()` **aman diterapkan langsung dari sisi constraint database**, karena:

1. `getKodeUnik()` adalah getter *write-back* — nilai lama di kolom `kode_unik` untuk
   suatu baris **hanya berubah** saat entity itu dimuat ulang lewat Hibernate dan
   sesinya di-*flush*. Baris lama yang tidak pernah disentuh lagi tetap menyimpan nilai
   lamanya tanpa error.
2. Nilai baru yang dihasilkan (`<kelas>_<id>` + `ref`) **unik secara konstruksi** —
   tidak mungkin bertabrakan dengan nilai lama (`ref` telanjang) milik baris lain, juga
   tidak mungkin bertabrakan dengan nilai baru baris lain (id per kelas entity unik).
   Karena itu **tidak ada migrasi backfill yang WAJIB dijalankan lebih dulu** hanya
   untuk menghindari pelanggaran *unique constraint* saat kode baru di-deploy.

**Yang TIDAK diperbaiki otomatis oleh sekadar menambah cabang** (ini bagian yang butuh
keputusan bagian keuangan, bukan sekadar tempel kode):

- Dokumen yang jurnalnya **hilang** akibat kasus tabrakan global (kasus *a*) tetap
  hilang sampai capnya dilepas dan diposting ulang secara manual — pola pemulihan yang
  sama seperti sudah didokumentasikan di r78717 §"LANGKAH PEMULIHAN". Begitu `ambilUnik()`
  sudah diperbaiki, posting ulang akan menghasilkan `kodeUnik` baru yang tidak lagi
  bertabrakan, sehingga jurnalnya benar-benar tertulis kali ini.
- Dokumen yang sudah telanjur mendapat jurnal **duplikat** akibat kasus `ref IS NULL`
  (kasus *b*) butuh rekonsiliasi manual (baris mana yang dibuang) — TIDAK boleh
  diotomatiskan tanpa verifikasi bagian keuangan, karena baris mana yang "asli" tidak
  bisa ditentukan murni dari data (keduanya identik strukturnya).

**Urutan yang direkomendasikan** (sebelum menyentuh kode `ambilUnik()`):

1. Jalankan `2026-09-03-diagnosa-ambilunik-15-kolom-hilang.sql` di UAT/produksi.
2. Bila Q1/Q3/Q4 menunjukkan nol dampak untuk suatu kolom (modul tidak pernah dipakai),
   cabang untuk kolom itu boleh ditambah tanpa rencana pemulihan — tidak ada data lama
   yang perlu direkonsiliasi.
3. Untuk kolom berdampak nyata, siapkan daftar dokumen terdampak (Q2) dan minta
   persetujuan bagian keuangan sebelum: (a) menambah cabang `ambilUnik()`-nya, (b)
   melepas cap dokumen yang jurnalnya hilang lalu posting ulang, (c) merekonsiliasi
   dokumen berjurnal duplikat.
4. Setelah setiap cabang ditambah dan diverifikasi kompilasi, mutakhirkan Javadoc kelas
   (bagian "PERINGATAN INTEGRITAS TAMBAHAN") agar tidak lagi menyebut kolom itu sebagai
   tidak dikenali — jangan biarkan komentar berdusta.

## 4. Cabang `transaksiPegawai` — DIPERBAIKI dalam sesi ini

Sebelum sesi ini, cabang tersebut menulis
`PembayaranGajiPunyaPegawai.class.getName()` alih-alih `TransaksiPegawai.class.getName()`,
sehingga kunci jurnal transaksi pegawai ber-id N bertabrakan dengan kunci slip gaji
ber-id N.

**Ini berbeda dari 15 kolom di §3** karena dua alasan yang membuatnya aman diperbaiki
langsung tanpa menunggu data produksi:

- `CommonAkunting.saveTransaksi(TransaksiPegawai, ...)` (overload khusus, baris ~389)
  **tidak melewati** pengecekan dedup `Restrictions.eq("kodeUnik", ...)` sama sekali —
  ia langsung `session.save(grupTransaksi)` tanpa syarat. Jadi cacat lama ini tidak
  pernah menyebabkan "jurnal transaksi pegawai hilang karena dianggap duplikat" lewat
  jalur ini; risikonya murni **kegagalan `INSERT`** (pelanggaran *unique constraint*
  `kode_unik`, bila constraint itu ada — lihat Q0) setiap kali id `TransaksiPegawai`
  kebetulan sama dengan id `PembayaranGajiPunyaPegawai` yang jurnalnya sudah ada dengan
  `ref` yang sama (biasanya sama-sama `null`). Kegagalan itu tertangkap
  `catch (Exception e)` di `PostingTransaksiPegawaiAction.postingSemua`, dicatat ke
  `ErrorAuditUtil`, dan dokumen tidak tercap (`postingHistory` tetap `null`) — bukan
  data yang salah tercatat, "hanya" posting yang gagal senyap sampai diselidiki log
  error.
- Tidak ada kode lain di repo yang mereferensikan nilai string `kodeUnik` secara
  hardcode (`kodeUnik`/`kode_unik` dicek di seluruh `ais/action/master/payroll`: nihil).
  Memperbaiki nama kelas hanya mengubah STRING yang dihasilkan — tidak ada pemanggil
  yang bergantung pada bentuk lamanya.

**Perbaikan yang diterapkan** (`GrupTransaksi.java`, cabang `ambilUnik()`):

```java
} else if (transaksiPegawai != null) {
    ko = TransaksiPegawai.class.getName() + "_" + transaksiPegawai.getId();
```

Javadoc kelas, method `ambilUnik()`, dan getter `getTransaksiPegawai()`/
`getPembayaranGajiPunyaPegawai()` yang sebelumnya mendokumentasikan cacat ini sebagai
belum diperbaiki sudah dimutakhirkan mengikuti perbaikan (deskripsi historis "sebelum
diperbaiki" dipertahankan agar konteks tidak hilang).

**Dampak pada baris lama:** baris `grup_transaksi` yang sudah punya `transaksi_pegawai`
terisi akan mendapat `kode_unik` BARU (`...TransaksiPegawai_<id>` alih-alih
`...PembayaranGajiPunyaPegawai_<id>`) begitu entity itu dimuat ulang dan sesinya
di-*flush* — nilai barunya tidak mungkin bentrok dengan baris manapun (lihat alasan §3
butir 2), jadi tidak perlu backfill. Q5 pada skrip diagnosa mengukur berapa banyak id
yang sebelumnya benar-benar bertabrakan, untuk keperluan audit riwayat (bukan syarat
sebelum deploy).

## 5. Rekomendasi tambahan: sertakan kolom referensi eksplisit di Criteria dedup

Usulan dari pemberi tugas, dianalisis di sini: alih-alih (atau sebagai tambahan)
mengandalkan `kodeUnik` — satu string opak yang HARUS dijaga manual agar konsisten
dengan `ambilUnik()` — `CommonAkunting.saveTransaksi` bisa menyertakan kesetaraan pada
kolom referensi yang sebenarnya dipakai (mis. `Restrictions.eq("pajak", pajakInstance)`
digabung `Restrictions.eq/isNull("ref", ref)`) sebagai syarat dedup, memakai rantai
`instanceof` yang **sudah ada** di `saveTransaksi` (baris ~600-697) untuk menentukan
kolom mana yang relevan.

**Manfaat:** kelas cacat "tabrakan global lintas dokumen" (kasus *a* di §1) menjadi
**mustahil secara struktural** — bahkan bila suatu kolom baru lupa ditambahkan ke
pemeriksaan ini di kemudian hari, pencocokan tetap ter-*scope* ke kolom FK yang benar,
tidak pernah menyamakan dua dokumen berbeda hanya karena `ref`-nya kebetulan sama.

**Batasan dan alasan TIDAK diterapkan dalam sesi ini:**

- `saveTransaksi` dipakai oleh puluhan mesin posting berbeda (lihat pola tiga titik
  registrasi di [[ais-mesin-posting-pattern]]); mengubah bentuk query dedup adalah
  perubahan mekanisme inti berdampak luas yang perlu diuji lewat harness UAT nyata,
  bukan sekadar dibaca — tepat jenis pekerjaan yang butuh akses database yang sedang
  tidak tersedia (§2).
- Perubahan ini TIDAK menghapus kebutuhan `kodeUnik`/`unique index`-nya sebagai
  penjaga idempotensi tingkat DATABASE terhadap *race condition* (dua klik posting
  bersamaan) — Criteria di level aplikasi saja tidak atomik. Jadi ini pelengkap, bukan
  pengganti, `ambilUnik()` yang benar.
- Kasus *b* (`ref` kosong → `kodeUnik = null` → duplikat) TIDAK ikut terselesaikan oleh
  perubahan ini — Criteria berbasis kolom FK yang sama akan tetap menemukan baris lama
  dan tetap perlu tahu bahwa baris itu representasi dokumen yang sama; masalahnya bukan
  di pencarian tapi di ekspektasi bahwa dokumen sudah punya PERSIS SATU jurnal (lihat
  dok pos/71 tentang aturan `ref` wajib berbeda per kaki).

**Rekomendasi:** jangan diterapkan sebagai tempelan cepat pada sesi mendatang manapun
tanpa rencana uji regresi lintas-modul; jadikan proyek tersendiri dengan harness UAT
nyata per modul (pola resep di [[ais-mesin-posting-pattern]] §"Resep harness DB UAT"),
dan pertimbangkan bersamaan dengan §3 karena keduanya menyentuh `saveTransaksi` di titik
yang sama.

## 6. Berkas yang berubah dalam sesi ini

- `ais/database/model/akunting/GrupTransaksi.java` — cabang `transaksiPegawai` pada
  `ambilUnik()` (§4) + Javadoc terkait dimutakhirkan. **15 kolom di §3 TIDAK diubah.**
- `docs/sql/2026-09-03-diagnosa-ambilunik-15-kolom-hilang.sql` (baru) — skrip hanya-baca
  untuk dijalankan terhadap UAT/produksi nyata.
- Dokumen ini.
