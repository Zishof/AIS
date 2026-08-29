# Posting Massal Trio Pembayaran Vendor (Tagihan / DP / Termin)

Tanggal: 29 Agustus 2026. Kode masuk SVN **r78540** (terbawa commit sapu sesi paralel;
keenam berkas diverifikasi byte-identik dengan hasil kerja sesi ini di HEAD). Mirror
`java/` selaras pada r78541. Lanjutan dari [54-posting-pengembalian-uang-muka.md](54-posting-pengembalian-uang-muka.md).

## 1. Apa yang ditambahkan

Tiga baris dasbor baru pada kategori Transaksi Vendor — sisi KAS-KELUAR rantai
pengadaan, melengkapi baris akrualnya yang sudah ada:

| Baris (kunci) | Entitas | Jurnal per dokumen |
|---|---|---|
| Pembayaran Tagihan Vendor (`pembayaran_tagihan_vendor`) | `PembayaranPengadaanMasterAssetDetail` | Dr utang penyedia (akun utang penyedia menimpa akun hutang jenis penerimaan; bila jenis pemesanan ber-utang-dari-anggaran, dipecah per akun anggaran detail pemesanan) / Cr akun jenis pembayaran barang, ditimpa akun cara pembayaran transfer/transitori — senilai `dibayar` |
| Pembayaran DP Vendor (`pembayaran_dp_vendor`) | `PembayaranDpMasterAssetDetail` | Dr akun DP jenis pemesanan / Cr akun jenis pembayaran (timpaan transfer/transitori sama) |
| Pembayaran Termin Vendor (`pembayaran_termin_vendor`) | `PembayaranTerminMasterAssetDetail` | Dr akun utang pekerjaan jenis pemesanan / Cr akun utang penyedia pemesanan; satuan kerja dari pengguna yang memposting (mengikuti layar) |

Mesin `postingSemua`/`batalkanPostingSemua` di masing-masing action ZK
(`PostingPembayaranAction`, `PostingPembayaranDpAction`, `PostingPembayaranTerminAction`),
dispatch di `DraftJurnalApiHelper` (kunci izin mengikuti dokumen: `pengadaan_tagihan`
untuk tagihan/termin, `pengadaan_po` untuk DP). `dibayar` ≤ 0.1 memutar posisi
debet/kredit (jurnal balik), mengikuti tombol layar. Whitelist `ENTITAS_CLOSING`
mendapat tiga properti detail; `petaDokumen` mendapat `getDibayar` di daftar pencarian
nilai rincian. Client Flutter tidak diubah.

## 2. Kekhususan semantik yang ditemukan dan diikuti

1. **`pilih` menentukan nilai efektif.** `getDibayar()` ketiga entitas MENOLKAN baris
   yang `pilih=false` (kolomnya bisa tetap terisi). Kriteria mesin dan dasbor karena itu
   menyaring `pilih=true` — tanpa ini mesin akan menjurnal 0 untuk baris tak terpilih
   (yang juga dilakukan tombol massal layar ZK, karena initCriteria-nya tidak menyaring
   pilih). Ditambah penjaga per dokumen: nilai efektif nol dilewati.
2. **`tanggal_transaksi` hanyalah cache turunan.** `getTanggalTransaksi()` menurunkannya
   dari tanggal realisasi transfer / `tanggaldibayar` setiap kali dibaca, dan
   `session.update` menulis hasil turunannya ke kolom. Konsekuensi nyata: dokumen
   ber-kolom-tanggal kosong yang diposting akan "berpindah" ke tanggal turunannya.
   Kriteria tetap menyaring kolom itu (`is null or between`), mengikuti layar.
3. **Kurung pada klausa tanggal.** initCriteria layar menulis
   `"this_.tanggal_transaksi is null or date(...) between ..."` telanjang di dalam
   `sqlRestriction` — Hibernate tidak membungkusnya, sehingga presedensi AND/OR SQL
   membuat cabang `between` LOLOS dari seluruh filter lain layar (bug layar,
   pre-existing, tidak disentuh). Kriteria mesin/dasbor memakai kurung eksplisit.
4. **Cap hanya bila jurnal benar-benar tersimpan.** `saveTransaksi` bisa menolak
   (mis. tanggal sebelum closing) sambil mengembalikan `false`; tombol layar tetap
   mencap sehingga dokumen tampak terposting tanpa jurnal. Mesin memeriksa nilai
   baliknya — gagal berarti dilewati, dokumen tetap draf.
5. **`PenyediaAsset.getAkunUtang()` punya fallback konfigurasi**
   (`akun_utang_id_default_data`) — penyedia tanpa akun utang bisa tetap terjurnal ke
   akun default tenant. Perilaku entitas, dibiarkan.

## 3. Pengujian

Harness `TesPostingTrioPembayaran` (scratchpad, DB UAT lokal `ais`), fixture berprefiks
`UATPTV-` pada rentang **10–20 April 2091** (dipastikan kosong; rantai lengkap: akun →
jenis bayar/terima/pesan → `penyedia_asset` → pemesanan → header → detail):

| Skenario | Hasil |
|---|---|
| X: tagihan 500k, pilih, dalam rentang | Dr 500k akun hutang jenis penerimaan / Cr 500k akun jenis pembayaran |
| Y: DP 300k ber-tanggal + Y2: DP 100k TANPA `tanggal_transaksi` | keduanya terpilih (cabang `is null`) dan terjurnal ke akun DP |
| Z: termin 200k | Dr akun utang pekerjaan / Cr akun utang penyedia |
| Z2: jenis pemesanan tanpa akun utang pekerjaan | dilewati, tetap draf |
| Z3: header belum disetujui | tidak pernah terpilih |
| Hitung dasbor | draf 1/2/2 → terposting 1/2/1, konsisten dengan mesin |
| Idempoten + batal | posting/batal ulang 0; batal menghapus seluruh jurnal (baris transaksi dulu, lalu grup), dokumen kembali draf |

**LULUS 21, GAGAL 0.** Kompilasi `javac -source 1.7 -target 1.7` bersih (6 berkas).

Catatan harness (menambah catatan dok 53/54): vendor pengadaan memakai
`asset.penyedia_asset`, BUKAN `library.penyedia`; kolom turunan tanpa `@Column` memakai
penamaan default yang dilipat Postgres (`kodeinvoice`, `kodetagihan`, `nilaidibayar`,
`tanggaldibayar`); fixture detail wajib mengisi `pilih=true` dan `tanggaldibayar`
(sumber asli tanggal) — mengisi `tanggal_transaksi` saja akan "berpindah" saat posting
(lihat §2.2); header-header pengadaan wajib `kode` + `dibuat_oleh`.

## 4. Sisa peta modul

Selesai: Jurnal Umum (dok 53), Pengembalian Uang Muka (dok 54), trio pembayaran vendor
(dok ini). Berikutnya: Perjanjian Kerjasama, payroll Transaksi Pegawai/Penggajian,
Saldo Awal Kas Kecil (`PostingJenisKasKecilAction`), dan trio kantin
(HPP/Penjualan/Toko — batch per periode, perlu desain endpoint sendiri).
