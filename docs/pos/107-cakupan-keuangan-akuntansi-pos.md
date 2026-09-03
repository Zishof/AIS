# Cakupan modul keuangan & akuntansi di POS Desktop/Android — apa yang belum

Pertanyaannya: adakah bagian modul keuangan dan akuntansi yang belum ada di POS Desktop dan POS
Android? Dokumen ini menjawabnya dengan pengukuran, bukan ingatan.

---

## 1. Cara mengukurnya

Seluruh aksi yang disalurkan `PosApi` **dan** dispatcher di `action/servlet/api/` dikumpulkan
(644 aksi), lalu disaring ke yang bertema keuangan/akuntansi: **181 aksi**. Daftar itu
dibandingkan dengan seluruh literal string di kode Dart kedua aplikasi klien.

**169 dari 181 punya pemanggil di klien.** Dua belas sisanya diperiksa satu per satu — karena
"tidak ada pemanggil" belum tentu berarti "fiturnya hilang".

Layar keuangan/akuntansi di klien saat ini: **27 dari 102 layar**, termasuk Jurnal Umum, Kas
Besar, Kas Kecil, Penggantian Kas Kecil, Draft Jurnal, Siklus Akuntansi, Closing, Kode Akun,
Anggaran, Laba Rugi, Piutang, Hutang Supplier, Pengadaan Pajak, Nomor Surat Keuangan, Master
Keuangan, dan katalog laporan.

## 2. Yang benar-benar belum ada (terverifikasi)

| Yang belum | Aksi server tanpa pemanggil | Akibatnya |
|---|---|---|
| **Buka/Tutup Kas Apotik** | `apotik_sesi_kas_buka`, `_tutup`, `_list`, `_status` | Varian apotik punya layar kasir tetapi tidak punya sesi kas — modal awal, uang fisik, dan selisih tidak tercatat. Dicek: tidak ada satu pun rujukan `sesi_kas` di seluruh layar apotik. |
| **Hapus pembayaran hutang** | `hutang_bayar_hapus` | Klien bisa mencatat pembayaran hutang (`hutang_bayar_simpan`, dipakai di `tab_mutasi_hutang.dart`) tetapi tidak bisa membatalkan yang salah. |
| **Sisa saldo anggaran** | `kas_besar_saldo_anggaran`, `kas_kecil_saldo_anggaran`, `penggantian_kas_kecil_saldo_anggaran`, `reimbursement_saldo_anggaran` | Layarnya sudah bisa MEMILIH mata anggaran (`*_cari_anggaran`) tetapi tidak pernah menampilkan sisanya. Pengaju tidak tahu pagunya masih cukup atau tidak sampai ditolak. |
| **Buat kategori biaya** | `si_expense_category_save` | Klien hanya membaca (`si_expense_category_list`). Kategori baru harus dibuat lewat web. |

## 3. Yang tampak belum, ternyata tercakup

Dua kandidat gugur setelah diperiksa — dan keduanya akan menjadi laporan palsu kalau
pengukurannya berhenti di "tidak ada pemanggil":

- **`si_profit_loss_print`.** `laba_rugi_screen.dart` **punya** fungsi cetak; ia memakai
  `si_profit_loss_report` (plus `_detail` dan `_params`). Aksi `_print` adalah jalur server yang
  tidak dipakai klien, bukan kemampuan yang hilang.
- **`pengadaan_pajak_opsi`.** Layar Pengadaan Pajak memakai empat aksi lain
  (`_daftar`, `_terutang`, `_setor`, `_batal`) dan berfungsi tanpa aksi opsi ini.

## 4. Batas ukuran ini

Dua hal yang TIDAK dibuktikan dokumen ini:

1. **Ada pemanggil ≠ fiturnya lengkap.** Yang diukur keterjangkauan aksi, bukan kelengkapan
   layarnya. Sebuah layar bisa memanggil aksinya tetapi hanya memakai sebagian medannya.
2. **Literal berkutip ganda tidak terhitung.** Daftar sisi klien dikumpulkan dari literal
   berkutip tunggal (gaya baku Dart). Kalau ada aksi ditulis dengan kutip ganda, ia akan tampak
   "tanpa pemanggil" secara keliru — karena itu kedua belas temuan diperiksa satu per satu di
   sumbernya, bukan dipercayai apa adanya dari selisih daftar.

## 5. Sisa lain di luar hitungan aksi

- `master_asset.zul` — satu-satunya pemanggilan `.zul` yang tersisa (doc 103, 108).
- `lk_dashakun` — dasbor akuntansi ZK, sengaja dibiarkan (doc 102).
- Notifikasi eCanteen lewat `mobile_auth.jsp` — butuh aksi baru per-pengguna, bukan
  penyambungan (doc 106).
