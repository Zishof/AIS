# Posting Massal Saldo Awal Kas Kecil

Tanggal: 29 Agustus 2026. Kode masuk SVN **r78554–r78557** (empat commit per berkas:
mesin `PostingJenisKasKecilAction`, `PostingJurnalHelper`, `DraftJurnalRingkasanUtil`,
`DraftJurnalApiHelper`). Mirror `java/` selaras byte-identik. Lanjutan peta
[53 §6](53-posting-jurnal-umum.md) setelah
[57-posting-payroll-pegawai-penggajian.md](57-posting-payroll-pegawai-penggajian.md).

## 1. Apa yang ditambahkan

Baris dasbor **baru** "Saldo Awal Kas Kecil" (kunci `saldo_awal_kas_kecil`, kategori
`uang_muka_kas`, ditempatkan setelah Penggantian Kas Kecil): saldo awal
`JenisKasKecil` yang terhubung proses transfer dipantau dari draf sampai menjadi
jurnal pembukaan, dengan posting/batal massal dari POS. Izin menumpang kunci
`kas_kecil` — saldo awal menempel pada master jenis kas kecil, satu keluarga dengan
Kas Kecil. Status posting dibaca dari properti `postingHistory` baku (jalur
`terapkanStatusPostingHistory`, tanpa pemetaan khusus di `propertiPosting`).

Kriteria (disamakan mesin ↔ dasbor, syarat wajib layar): `daftarPengajuanTransfer.
prosesTransfer` terisi, `saldoAwal` tidak nol/null, rentang `date(this_.tanggal)`.
Filter satuan kerja dan kata kunci layar tidak ikut — dasbor menghitung global.

## 2. Jurnal per dokumen

Dr akun jenis kas kecil (`jenisKasKecil.akun`) / Cr akun cara pembayaran transfer
pengajuannya — transitori → `caraPembayaranTransfer.akunTransitori`, transfer →
`.akun`, keduanya false → dokumen dilewati — senilai `saldoAwal` pada tanggal
pembukaan. Idiom lama `nilai <= 0.1` membalik pasangan, diwarisi dari tombol layar.

Dua pengetatan mesin dibanding tombol layar (perilaku tombol TIDAK diubah):
dokumen dicap hanya bila `saveTransaksi` mengembalikan true (tombol mengecap meski
gagal — idiom seluruh keluarga layar, tercatat, tidak disentuh), dan rantai
pengajuan transfer di-null-guard penuh (tombol membiarkan NPE ditelan catch per
baris).

## 3. Audit tombol ZK: BERSIH

Dengan pola pemeriksaan dok 54 §2: pasangan akun konsisten di massal / per-baris /
tampilan grid; SQL batal (massal 319, per-baris 787) ber-`and closing is null`
benar; kedua jalur mengecap dokumen. Tidak ada perubahan tombol.

## 4. Pengujian

Harness `TesPostingSaldoAwalKasKecil` (scratchpad, DB UAT `ais`), fixture
`UATSAKK-` rentang **10–20 April 2091** (April dipilih agar tak beririsan dengan
rentang harness lain: Maret = pengembalian, Juni = payroll):

| Skenario | Hasil |
|---|---|
| A: transfer, saldo 500k | terposting; Dr 500k akun jenis, Cr 500k akun cara bayar; riwayat `posting=true` |
| B: transitori, saldo 300k | terposting; Cr 300k jatuh ke akun transitori |
| C: kedua bendera false | dihitung draf dasbor, dilewati mesin, tetap draf |
| D: saldo 0 / E: tanpa pengajuan | tidak dihitung dan tidak disentuh |
| Dasbor sebelum/sesudah | draf 3→1, terposting 0→2 — konsisten mesin |
| Idempoten + batal | posting/batal ulang n=0; riwayat kosong run kedua terhapus; jurnal terhapus saat batal, dokumen kembali draf |

**LULUS 21, GAGAL 0.** Kompilasi `javac -source 1.7 -target 1.7` keempat berkas
bersih.

Catatan harness baru (melengkapi dok 08 dan catatan dok 54/57):

- Kolom `public.jenis_kas_kecil.saldoawal` — properti `saldoAwal` TANPA `@Column`
  dipetakan lowercase polos, BUKAN snake_case; `tanggal_bayar_gaji` dkk. yang
  snake_case adalah `@Column`/`@JoinColumn` eksplisit. Jangan menebak nama kolom
  dari gaya properti — cek anotasinya.
- `setenv.bat` di `C:\opt\Codex-Worspace\.uat-tomcat-inventory\bin` menyuntik
  `-Durl/-Dusername/-Dpassword` milik DB INVENTORY (user root) — DITOLAK untuk DB
  `ais`. Kredensial DB `ais` yang benar ada di `hibernate.cfg.xml` blok aktif pada
  direktori kelas harness sesi payroll (`kelas-jurnal-umum` di scratchpad-nya);
  harness ini menyalinnya ke direktori cfg sendiri dan membaca url/username/password
  langsung dari resource `/hibernate.cfg.xml` classpath bila system property `url`
  kosong.
- `build/uat-77608` (kelas r77608) tidak memuat entitas yang lahir belakangan;
  276 kelas mapping cfg yang hilang dikompilasi dari sumber ke direktori kelas
  patch (yang selalu di DEPAN classpath) sebelum session factory mau berdiri.

## 5. Sisa peta modul

Dari peta 53 §6 tersisa satu: **trio kantin HPP/Penjualan/Toko** — SELESAI r78560,
terdokumentasi dan teruji 16/16 di
[59-posting-kantin-dasbor.md](59-posting-kantin-dasbor.md). Peta 53 §6 dengan ini
TUNTAS seluruhnya.
