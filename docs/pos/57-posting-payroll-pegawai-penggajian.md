# Posting Massal Payroll: Transaksi Pegawai + Penggajian Pegawai

Tanggal: 29 Agustus 2026. Kode masuk SVN **r78548** (commit sesi ini, 5 berkas).
Mirror `java/` selaras (r78548). Lanjutan dari
[56-posting-perjanjian-kerjasama.md](56-posting-perjanjian-kerjasama.md).

## 1. Apa yang ditambahkan

Dua baris dasbor baru pada kategori **Gaji** (izin menumpang kunci `gaji`):

| Baris (kunci) | Entitas | Jurnal per dokumen |
|---|---|---|
| Transaksi Pegawai (`transaksi_pegawai_payroll`) | `TransaksiPegawai` | pasangan akun `JenisTransaksiPegawai.akunDebet`/`akun` senilai nilai, arah dibalik untuk nilai negatif — lewat overload khusus `CommonAkunting.saveTransaksi(TransaksiPegawai, ...)`, riwayat `JENIS_TRANSAKSI_LAIN` |
| Penggajian Pegawai (`penggajian_pegawai`) | `PembayaranGajiPunyaPegawai` | per pegawai: tiap item gaji > 0.1 ke sisi `akunDebet`/`akun`-nya, neto ke akun bank pegawai (bila ada) atau akun cara pembayaran gaji; ditulis hanya bila total debet == kredit (perbandingan `intValue`, mengikuti layar); riwayat `JENIS_PENGGAJIAN` |

Kriteria: Transaksi Pegawai = nilai ≠ 0 pada rentang `tanggal` (layar tidak menyaring
nilai; dasbor menambahkannya agar dokumen ber-jurnal-nol tidak dihitung); Penggajian =
induk `pembayaranGaji.disetujuiOleh` terisi, rentang `tanggal_bayar_gaji`. Whitelist
`ENTITAS_CLOSING` mendapat `transaksiPegawai` + `pembayaranGajiPunyaPegawai`. Client
Flutter tidak diubah.

## 2. Cacat tombol layar yang TIDAK diwarisi

1. **Penggajian massal: akumulator kredit cabang bank tidak dijumlahkan.** Cabang bank
   menambah akun+nilai ke daftar kredit tetapi lupa menambah `nilaiKredit` — pegawai
   ber-rekening-bank karena itu SELALU gagal penjaga keseimbangan dan tidak pernah
   terjurnal dari tombol massal. Mesin menjumlahkannya.
2. **Penggajian massal tidak pernah MENCAP dokumen** (`setPostingHistory` hanya ada di
   jalur per baris) — hasil posting massal terhitung draf selamanya dan diproses ulang
   setiap klik (deduplikasi `kodeUnik` di `saveTransaksi` yang menahannya dari
   menduplikasi jurnal). Mesin mencap begitu jurnal benar tersimpan.
3. **Transaksi Pegawai per baris memakai riwayat `JENIS_MAHASISWA`** (salah tempel);
   tombol massalnya benar (`JENIS_TRANSAKSI_LAIN`) — mesin mengikuti yang benar.

Catatan semantik yang diikuti setia: overload `saveTransaksi(TransaksiPegawai, ...)`
menerima `apakahUangMasuk = nilai < 0` dari layar, sehingga transaksi bernilai POSITIF
justru menempatkan nilai di sisi kredit baris akun-debet (dan sebaliknya) — perilaku
layar sejak lama, tidak diubah. `PembayaranGajiPunyaPegawai.getTanggalBayar()`
diturunkan dari `pembayaranGaji.waktuBayar` setiap dibaca (kolom `tanggal_bayar_gaji`
hanyalah cache, pola yang sama dengan temuan dok 55).

## 2a. Perbaikan tombol layar ZK (r78551 Pegawai, r78552 Penggajian, 29 Agustus 2026)

Ketiga cacat §2 diperbaiki di layarnya sendiri; mirror `java/` selaras byte-identik.

- **Transaksi Pegawai per baris**: riwayat kini `JENIS_TRANSAKSI_LAIN` (r78551).
- **Penggajian massal**: cabang bank menjumlahkan `nilaiKredit`, dan dokumen dicap
  `setPostingHistory` begitu `saveTransaksi` mengembalikan true — mengikuti bentuk
  mesin §1 (r78552). Koreksi kecil atas pesan commit r78552: run massal berulang atas
  dokumen tak tercap TIDAK menggandakan jurnal — deduplikasi `kodeUnik` di
  `saveTransaksi` menahannya (lihat §2 butir 2); kerugian nyatanya status draf abadi
  dan kerja ulang tiap klik.
- **Temuan KEEMPAT saat perbaikan, belum tercatat di §2**: tombol BATAL massal
  Penggajian menampung hasil `initCriteria` (query `PembayaranGajiPunyaPegawai`)
  sebagai `List<PembayaranItemGajiPegawai>`, sehingga loop-nya selalu melempar
  ClassCastException pada elemen pertama — tombol mati total; tidak pernah ada
  pembatalan massal yang jalan dari layar ini. Tipe dibetulkan, dan id yang masuk SQL
  kini id dokumen yang memang dirujuk kolom
  `grup_transaksi.pembayaran_gaji_punya_pegawai` (r78552).

Verifikasi: kompilasi `javac -source 1.7 -target 1.7` kedua berkas bersih; tombol
belum diuji runtime ZK — dasar kebenarannya paritas dengan mesin §1 yang lulus
harness §3.

## 3. Pengujian

Harness `TesPostingPayroll` (scratchpad, DB UAT lokal `ais`), fixture `UATPAY-` rentang
**10–20 Juni 2091**; tabel `public.pegawai` UAT ternyata kosong sehingga harness membuat
pegawai minimal sendiri (tanpa bank → cabang kredit neto deterministik ke akun cara
pembayaran gaji):

| Skenario | Hasil |
|---|---|
| t1: transaksi 150k jenis lengkap | terjurnal 2 baris 150k pada pasangan akun jenis |
| t2: nilai 0 / t3: jenis tanpa akun debet | t2 tak dihitung; t3 dilewati, tetap draf |
| g1: item beban 1jt + potongan 200rb, neto 800rb | Dr beban 1jt; Cr potongan 200rb + Cr kas 800rb; dokumen TERCAP (perbaikan §2.2) |
| g2: neto 750rb (tak seimbang int) | dilewati, tanpa jurnal |
| g3: induk belum disetujui | tidak pernah terpilih |
| Dasbor | draf 2/2 → terposting 1/1, konsisten mesin |
| Idempoten + batal | posting/batal ulang 0; jurnal terhapus, dokumen kembali draf |

**LULUS 16, GAGAL 0.** Kompilasi `javac -source 1.7 -target 1.7` bersih (5 berkas).

Catatan harness baru: `pembayaran_item_gaji_pegawai.format_item_gaji` NOT NULL (buat
`format_item_gaji` fixture); `public.pegawai` boleh diinsert minimal `(id, nama)`.

## 4. Sisa peta modul

Selesai: 53 Jurnal Umum, 54 Pengembalian UM, 55 trio pembayaran vendor, 56 Perjanjian
Kerjasama, 57 payroll (dok ini). Tersisa: **Saldo Awal Kas Kecil**
(`PostingJenisKasKecilAction`) dan **trio kantin** (HPP/Penjualan/Toko — batch per
periode, perlu desain endpoint sendiri).
