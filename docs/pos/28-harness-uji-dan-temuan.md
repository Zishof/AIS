# 28 — Uji yang dijalankan & jebakan yang ditemukan

## Hasil uji

| Yang diuji | Hasil | Cara |
|---|---|---|
| Matriks izin ubah harga | **21/21** | JDBC, data disemai lalu ROLLBACK |
| Agregasi lintas toko per pendaftar | **4/4** | JDBC, dua pendaftar disemai lalu ROLLBACK |
| Saringan toko opsional (`COALESCE`) | 3/5 → **5/5** | JDBC, setelah perbaikan NULL |
| Waktu jadwal berikutnya (penjadwal) | **6/6** | refleksi, tanpa basis data |
| Batas rentang tanggal audit | **11/11** | refleksi, tanpa basis data |
| Saringan audit (kata kunci & kolom) | **19/19** | `ClassMetadata` tiruan via `java.lang.reflect.Proxy` |
| Penyisipan toko ke payload | **18/18** | `flutter test` |
| Mesin diskon eCanteen | **17/17** | `flutter test` |
| Seluruh suite klien POS | **152/152** | `flutter test` |

Setiap harness JDBC membersihkan data ujinya sendiri (ROLLBACK). Kredensial dibaca dari
konfigurasi, tidak pernah dicetak, dan tidak pernah masuk commit atau dokumen.

### Apa yang sengaja dikejar tiap uji

Uji di sini tidak mengejar cakupan baris, melainkan **kegagalan yang tidak berteriak**:

- **Batas atas rentang audit** harus 23:59:59.999. Kalau berhenti di tengah malam,
  permintaan "sampai 31 Agustus" kehilangan seisi hari terakhir — alat pencari data hilang
  justru ikut kehilangan data, dan hasilnya tetap terlihat wajar.
- **`LIKE` pada kolom angka.** Uji memastikan entitas tanpa kolom teks menghasilkan **nol**
  kriteria kata kunci, bukan `LIKE` pada kolom numerik yang meledak saat dieksekusi.
- **Substring peran.** `SPV` tidak boleh cocok dengan `SPV2`; izin yang bocor tidak
  menimbulkan galat, hanya akses yang tidak pernah diberikan.
- **`sesi_kas_*` tidak menerima toko dari filter.** Uji mengunci **pengecualian**, bukan
  hanya isi daftar — daftar yang benar hari ini gampang jadi salah saat dirapikan orang
  lain.
- **Waktu jadwal berikutnya selalu positif.** Salah tanda membuat siklus berjalan seketika
  terus-menerus; lupa menambah hari membuatnya tidak pernah berjalan.

---

## Jebakan yang ditemukan

### Basis data & Hibernate

- **`COALESCE(?, toko) = toko` menghilangkan baris ber-`toko` NULL.** Perbandingannya
  UNKNOWN saat NULL, dan barisnya lenyap tanpa jejak. Di UAT, 85% baris `koperasi.produk`
  ber-`toko` NULL. Bentuk yang benar: `COALESCE(?::bigint, COALESCE(toko,-1)) = COALESCE(toko,-1)`.
- **`session.get(Toko.class, null)` melempar**, bukan mengembalikan `null`. Perlu penjagaan
  di setiap tempat yang boleh tidak menyebut toko (enam tempat).
- **Pengisi parameter mengubah `null` menjadi string `"null"`.** Harus
  `ps.setNull(idx, Types.OTHER)` eksplisit.
- **Envers `LIKE` pada kolom non-teks** meledak saat `getResultList()`, bukan saat query
  disusun. Lihat [25](25-riwayat-audit-history.md).

### Logika lintas lapisan

- **Ambil dan Simpan menentukan toko dengan urutan berbeda** → membaca toko A, menulis ke
  toko B, tanpa galat. Lihat [23](23-toko-pada-payload.md). Setiap pasangan baca/tulis
  wajib memakai resolver yang sama.
- **Klien tidak pernah mengirim toko** untuk 13 aksi yang membutuhkannya. Diperbaiki
  terpusat, karena tambalan per pemanggilan adalah cara yang sama yang melahirkan bug itu.
- **Sumber pendaftar yang keliru.** `akun_manajemen.userid` berpola `mgr-<nama>` dan tidak
  pernah sama dengan `Tbmuser.userid`; cocokannya selalu kosong, sehingga setiap pengguna
  akan terlihat "boleh melihat semua". Diganti relasi langsung `Tbmuser.pendaftar`.
- **Gerbang harga terlewat di impor Excel.** Memblokir kolom harga di layar tidak berguna
  bila harga yang sama bisa masuk lewat unggahan berkas.

### Perkakas & proses

- **Kelas kembar di berkas Dart.** Beberapa berkas memuat lebih dari satu `State` dengan
  prolog metode identik; penyisipan berdasarkan jangkar teks pernah mendarat di kelas yang
  salah lebih dari sekali. Pakai jangkar yang memuat nama unik kelasnya, dan **selalu baca
  diff sebelum commit**.
- **Jangkar teks yang tidak unik di Java.** Penyisipan gerbang harga sempat mendarat di
  metode pembuatan Pedagang karena kalimat `"Toko tidak ditemukan."` muncul di banyak
  tempat. Tertangkap di `svn diff` sebelum commit, lalu diulang dengan jangkar unik.
- **Heredoc bash memakan garis miring terbalik.** `\\0` berubah menjadi byte NUL di berkas
  keluaran, dan `\'` merusak parsing skrip. Untuk skrip Python bantu, tulis ke berkas lebih
  dulu, jangan pipa lewat heredoc.
- **ISCC terlupa setelah build ulang** → `.exe` installer tidak berubah. Bandingkan hash
  sebelum unggah.
- **Working copy dipakai bersama.** Sesi lain menyapu dan meng-commit apa pun yang kotor,
  sering dengan pesan kosong. Karena itu: verifikasi di HEAD, commit **per berkas** (jangan
  `-A`), jangan menyunting berkas yang sedang kotor milik pekerjaan lain, dan tulis alasan
  di kode karena pesan commit tidak bisa diandalkan.

---

## Yang masih terbuka

| Hal | Keterangan |
|---|---|
| Deploy server **r77896+** | Tanpa ini seluruh pekerjaan di dokumen 22–25 diam |
| Build ulang POS Desktop | Rilis v1.33.68 mendahului semua commit di sini |
| Pesanan Agustus yang hilang | Menunggu hasil kueri audit ([25](25-riwayat-audit-history.md)); belum diketahui apakah penghapusannya lewat aplikasi atau langsung di basis data |
| Jalur "terikat pendaftar" | Belum teruji dengan data nyata; UAT belum punya `Tbmuser` yang ditautkan |
| Restore audit terhadap data nyata | Pemilihan revisi, dedupe id, dan `replicate` belum pernah dijalankan di basis data |
| Penjadwal multi-instance | Bayar otomatis perlu penguncian bila server lebih dari satu |
| Topup native & penandatanganan APK eCanteen | Sengaja ditunda ([27](27-ecanteen-aplikasi-anggota.md)) |
