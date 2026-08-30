# Posting Penghapusan Aset: Pasangan Akun Jenis Penghapusan Akhirnya Dipakai

Tanggal: 29 Agustus 2026. Kode masuk SVN **r78620** (commit sesi ini, 8 berkas).
Mirror `java/` selaras. Menutup butir **D** gap analysis
[61-gap-analysis-posting.md](61-gap-analysis-posting.md).

## 1. Kerangka yatim ketiga yang dilengkapi

Master `JenisPengapusanBarang` sejak lama menyimpan pasangan akun `debet`/`kredit` —
dirawat lengkap di layar masternya, ditampilkan di grid — tetapi TIDAK ADA satu jalur
pun yang memakainya untuk menjurnal (pola yang sama dengan simpan-pinjam/dok 62 dan
pembatalan kantin/dok 64). Kini:

- Baris dasbor baru **"Penghapusan Aset"** (kunci `penghapusan_aset`, kategori Fixed
  Asset & Penyusutan; izin kunci deskriptif fail-closed).
- Mesin `postingSemua`/`batalkanPostingSemua` di `PenghapusanMasterAssetAction` +
  dispatch API.
- Entitas `PenghapusanMasterAsset` mendapat field `posting_history`; `grup_transaksi`
  mendapat kolom referensi `penghapusan_master_asset`; `CommonAkunting` mendapat cabang
  referensinya; `PostingHistory` mendapat `JENIS_PENGHAPUSAN_ASET`. Kolom baru dibuat
  `hbm2ddl update`.

## 2. Desain jurnal

Kriteria (mesin = dasbor): disetujui, aktif, `nilai` ≠ 0, rentang `tanggal_pembuatan`.
Jurnal per dokumen: **Dr akun `debet` jenis / Cr akun `kredit` jenis**, senilai `nilai`
(total harga perolehan detail), bertanggal **persetujuan** (fallback pembuatan).
Konvensi keluarga mesin berlaku penuh.

**Batasan yang disengaja**: SATU pasang akun per jenis penghapusan berarti pelepasan
akumulasi penyusutan dan pengakuan rugi tidak dipecah otomatis. Praktik yang didukung:
master diisi Dr "Beban/Rugi Penghapusan Aset" / Cr akun aset; bila kebijakan akuntansi
menghendaki pemecahan (Dr Akumulasi Penyusutan + Dr Rugi / Cr Aset), porsi akumulasi
dipindahkan lewat Jurnal Penyesuaian. Pemecahan otomatis (memakai
`detail.penyusutanAsset.nilaiBuku` yang datanya sebenarnya ada) dicatat sebagai
peningkatan lanjutan bila tim akuntansi memintanya.

## 3. Pengujian

Harness `TesPostingPenghapusanAset` (scratchpad, DB UAT `ais`), fixture `UATPHA-`
rentang **10–20 November 2091** (dipastikan kosong):

| Skenario | Hasil |
|---|---|
| H1: disetujui, jenis lengkap, 1,5jt | Dr akun debet jenis / Cr akun kredit jenis; bertanggal persetujuan; tercap |
| H2: belum disetujui | tidak pernah terpilih |
| H3: jenis tanpa akun kredit | dihitung draf, dilewati mesin |
| Dasbor | draf 2 → terposting 1 + draf 1, konsisten mesin |
| Idempoten + batal | posting/batal ulang 0; jurnal habis, dokumen kembali draf |

**LULUS 10, GAGAL 0.** Kompilasi `javac -source 1.7 -target 1.7` bersih (8 berkas;
`-sourcepath .` menyeret dua helper penghapusan yang tak ada di fullcompile basi).

Catatan harness baru: sesi paralel menambah entitas mapping baru sepanjang hari —
jangan berburu satu-satu; ekstrak seluruh `class="..."` dari `hibernate.cfg.xml`,
hitung yang hilang dari classpath (di sini 281), kompilasikan sekaligus dengan
`-sourcepath .`. Berkas resource (`myehcache.xml` dkk.) di direktori kelas harness bisa
raib disapu proses lain — selalu salin ulang sebelum run.

## 4. Keadaan peta gap dok 61

- **A Simpan-Pinjam** — selesai (dok 62, r78584).
- **C Pembatalan kantin** — selesai (dok 64, r78603).
- **D Penghapusan aset** — selesai (dok ini, r78620).
- **B Keluarga dana anggota** — menunggu keputusan bagan akun tim akuntansi.
- **E Inventory Sales** — menunggu keputusan lingkup.

Semua butir yang bisa dikerjakan tanpa keputusan akuntansi kini TUNTAS.
