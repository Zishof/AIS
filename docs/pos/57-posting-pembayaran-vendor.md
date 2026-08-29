# Posting Massal Trio Pembayaran Vendor (Tagihan, DP, Termin)

Tanggal: 29 Agustus 2026. Kode masuk SVN **r78536** dan **r78540** — keduanya commit
sesi paralel TANPA pesan log (r78536 sapuan campuran yang juga membawa berkas kantin
di luar trio); dokumen ini adalah pengganti catatannya, ditulis dari audit kode HEAD
r78543. Berkas inti: `PostingPembayaranAction`, `PostingPembayaranDpAction`,
`PostingPembayaranTerminAction` (mesin), `DraftJurnalRingkasanUtil` (kunci + kriteria),
`DraftJurnalApiHelper` (ruting), `PostingJurnalHelper`. Mirror `java/` selaras
byte-identik (diverifikasi 29 Agu). Lanjutan peta [53 §6](53-posting-jurnal-umum.md)
setelah [54-posting-pengembalian-uang-muka.md](54-posting-pengembalian-uang-muka.md).

## 1. Apa yang ditambahkan

Tiga baris dasbor draft jurnal POS — sisi kas-keluar rantai vendor: kunci
`pembayaran_tagihan_vendor`, `pembayaran_dp_vendor`, `pembayaran_termin_vendor`,
masing-masing dengan mesin `postingSemua`/`batalkanPostingSemua` statis di action
ZK-nya dan ruting di `DraftJurnalApiHelper`. Kriteria dokumen dasbor disamakan dengan
kriteria mesin massal masing-masing (lihat komentar "Tiga kriteria pembayaran vendor"
di `DraftJurnalRingkasanUtil`).

## 2. Jurnal per modul

| Modul (entitas detail) | Debet | Kredit |
|---|---|---|
| Tagihan (`PembayaranPengadaanMasterAssetDetail`) | per-rincian akun anggaran pemesanan bila `jenisPemesananPengadaanAsset.akunUtangDariAnggaran`; selain itu `jenisPenerimaanBarang.akunHutangPenyedia`, dioverride `penyedia.akunUtang` bila ada | `jenisPembayaranBarang.akun`, dioverride `caraPembayaranTransfer.akun` / `.akunTransitori` sesuai bendera transfer/transitori pengajuan |
| DP (`PembayaranDpMasterAssetDetail`) | `jenisPemesananPengadaanAsset.akunDp` | sama seperti Tagihan |
| Termin (`PembayaranTerminMasterAssetDetail`) | `jenisPemesananPengadaanAsset.akunUtangPekerjaan` | `penyedia.akunUtang` (tanpa override transfer) |

Semua jalur (tombol ZK maupun mesin) memakai idiom lama `nilai > 0.1` — di bawah
ambang itu pasangan Dr/Cr DIBALIK. Mesin sengaja mewarisinya demi paritas penuh
dengan tombol yang sudah berjalan produksi.

## 3. Audit tombol ZK trio: BERSIH (pola cacat dok 54 §2 tidak ada di sini)

Diaudit 29 Agu 2026 dengan pola pemeriksaan yang sama dengan perbaikan layar ZK
Pengembalian (r78539, dok 54 §2a):

- Pasangan akun kedua jalur tulis (massal dan per baris) = tampilan grid = mesin API,
  pada ketiga action. Tidak ada jurnal Dr X / Cr X.
- Semua SQL batal (massal, per baris, mesin) berbentuk benar:
  `delete from akunting.grup_transaksi where <kolom_detail>=<id> and closing is null` —
  tidak ada `AND` yang hilang.
- Awas nama menyesatkan saat membaca `onBatalkanPostingSemua` di
  `PostingPembayaranAction`: variabel loop `pembayaranPengadaanMasterAsset` sebenarnya
  bertipe `PembayaranPengadaanMasterAssetDetail`, jadi id yang dipakai SQL memang id
  detail — bukan bug.

Tidak ada perubahan kode yang diperlukan dari audit ini.

## 4. Verifikasi

Kompilasi ketiga action `javac -source 1.7 -target 1.7 -encoding UTF-8` (classpath
`webapp/WEB-INF/lib/*`) bersih. Ruting `DraftJurnalApiHelper` dan kunci dasbor
diperiksa pada HEAD r78543. Belum ada harness runtime khusus trio yang tercatat —
r78536/r78540 masuk tanpa pesan dan tanpa dokumen; bila sesi penulis mesinnya punya
bukti uji, tambahkan di sini.

## 5. Sisa peta modul

Dari peta 53 §6: **Perjanjian Kerjasama** sedang dikerjakan (29 Agu sore, WC src
menunjukkan modifikasi lokal `PostingPerjanjianKerjasamaAction` + helper oleh sesi
paralel — jangan disentuh sesi lain sampai masuk). Berikutnya: payroll
Pegawai/Penggajian, Saldo Awal Kas Kecil (`PostingJenisKasKecilAction`), dan trio
kantin HPP/Penjualan/Toko (batch per periode, perlu desain endpoint sendiri).
