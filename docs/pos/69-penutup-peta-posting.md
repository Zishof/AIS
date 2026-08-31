# Penutup Peta Posting & Laporan: Butir E, Buku Kas Umum, dan Diagnosa Aktivitas

Tanggal: 31 Agustus 2026. Kode masuk SVN **r78658** (Buku Kas Umum + Diagnosa Aktivitas Arus Kas)
dan **r78666** (posting Biaya Sesi Sales). Menutup butir **E** gap analysis
[61-gap-analysis-posting.md](61-gap-analysis-posting.md) sekaligus dua sisa catatan pada dok
[66](66-laporan-keuangan-standar-yayasan.md) §5.

## 1. Butir E: dugaan "buku terpisah" ternyata keliru

Dok 61 mencatat modul Inventory & Sales sebagai "buku terpisah, konsolidasi manual" dan
menyerahkannya sebagai keputusan lingkup. Penelusuran ulang menunjukkan premis itu salah:

| Yang diduga | Kenyataannya |
|---|---|
| Punya bagan akun sendiri | Layar **Master Akun** pada Kas & Jurnal membaca `akunting.akun` yang sama — `SalesInventoryFinanceHelper.coaList/coaSave` |
| Punya jurnal mini sendiri | Aksi `si_cash_journal_list` membaca langsung `akunting.transaksi` — buku besar yang sama |
| `NotaSalesPembelian` dokumen pembelian terpisah | JavaDoc-nya sendiri menyatakan **TAUTAN** ke `PengadaanFaktur` (kulakan), "bukan duplikasi" — jalur postingnya sudah ada |
| `NotaSalesKas` buku kas tandingan | Catatan **laci kas sesi** (opening advance, collection, cash sale, expense, dst). Ini kontrol kas operasional, sejenis sesi kas kasir yang — menurut dok 61 sendiri — memang tidak lazim dijurnal per sesi |

Jadi tidak ada keputusan lingkup yang perlu diambil, dan tidak ada jembatan konsolidasi yang perlu
dibangun. Yang benar-benar tersisa hanya **satu** dokumen ekonomi tanpa jurnal.

## 2. `PostingBiayaSalesUtil` — Biaya Sesi Sales

Pengeluaran nyata sales di lapangan (BBM, tol, konsumsi, dsb.) tidak pernah menyentuh buku besar,
sehingga beban operasional pada Laba Rugi lebih kecil daripada yang sesungguhnya terjadi.

- **Kriteria**: dokumen berstatus AKTIF (bukan hasil reversal) dan bernilai, rentang `tanggal`.
- **Jurnal**: **Dr** akun kategori biaya / **Cr** akun kas sesi sales, bertanggal tanggal biaya.
- **Sumber akun beban**: kolom `akun` **baru** pada master `KategoriBiayaSales` — satu akun per
  kategori, supaya rincian beban di buku besar sedetail kategori yang sudah dipakai lapangan.
  Master ini punya API pemeliharaan sendiri (`expenseCategoryList` / `expenseCategorySave`), yang
  ikut diperluas agar bisa membaca dan menyimpan akunnya. **Lebih penting lagi**, kategori biaya
  didaftarkan sebagai tipe baru pada **Master Data Keuangan** (`kategori_biaya_sales`) — layar itu
  digerakkan metadata server (`tipe` + `medanAkun`), jadi tabnya beserta pemilih "Akun Beban",
  penanda "belum lengkap", dan hitungan pemakaian muncul sendiri **tanpa satu baris pun perubahan
  klien**. Tanpa langkah ini, kolom akun yang baru tidak akan punya UI mana pun untuk diisi.
- **Sumber akun kas**: Konfigurasi `akun_kas_sesi_sales_id`, karena dokumen biaya hanya menyimpan
  metode pembayaran berupa teks, bukan rujukan akun.
- Kategori yang belum ber-akun **dilewati** mesin tetapi tetap terhitung draf — kekurangan setup
  terlihat di dasbor, bukan menghasilkan jurnal ke akun yang salah.

Baris dasbor **"Biaya Sesi Sales"**, kategori `posting_penjualan`, kunci izin `biaya_sales`
(serumpun dengan menu Biaya Sales pada modul Inventory & Sales).

## 3. Dua laporan penutup (r78658)

### 3.1 `akn_buku_kas_umum` — Buku Kas Umum (Mutasi Kas & Bank)
Buku kas per rekening: tiap mutasi berurut tanggal beserta **akun lawan** dan **saldo berjalan**
yang sudah memperhitungkan saldo sebelum Tgl Mulai, sehingga baris terakhir tiap rekening bertemu
dengan saldo akhir pada Posisi Dana (diuji silang). Inilah padanan native lembar *Mutasi Kas &
Bank* pada paket laporan yayasan — sekaligus menggantikan kelas yatim `LaporanBukuKasUmum` yang
JRXML-nya ada tetapi tidak pernah terpasang di menu mana pun.

### 3.2 `akn_diagnosa_aktivitas` — Diagnosa Pemetaan Aktivitas Arus Kas
Menampilkan akun **penggerak kas** yang belum punya aktivitas arus kas — baik lewat Kelompok
Laporan jenis "Arus Kas" maupun kolom "Aktifitas (Arus Kas)" pada master Kode Akun — diurut dari
penyumbang terbesar. Ini menutup satu-satunya hal yang selama ini hanya bisa dititipkan sebagai
"pekerjaan admin" tanpa alat bantu: sekarang admin bisa melihat persis apa yang harus dipetakan
supaya keranjang "(Belum dipetakan)" pada Arus Kas per Aktivitas mengosong.

## 4. Pengujian

- `TesLaporanYayasan` diperluas menjadi **40 pemeriksaan, GAGAL 0** — termasuk saldo berjalan Buku
  Kas Umum yang meneruskan saldo awal, kolom akun lawan yang terisi, pemeriksaan silang dengan
  Posisi Dana, dan diagnosa yang hanya melaporkan akun yang memang belum dipetakan (akun yang
  sudah dipetakan, yang ber-kolom aktifitas, dan akun kas/bank sendiri tidak ikut).
- `TesPostingBiayaSales`: **LULUS 10, GAGAL 0** — dasbor 2 draf (dokumen REVERSED tidak terpilih),
  jurnal Dr beban BBM / Cr kas sesi sales bertanggal biaya, kategori tanpa akun dilewati dan tetap
  draf, idempoten, siklus batal penuh, fixture bersih.

## 5. Bagi admin

- Isi Konfigurasi **`akun_kas_sesi_sales_id`**, lalu isi **Akun Beban** tiap kategori lewat
  **Master Data Keuangan → Kategori Biaya Sales**. Kategori yang belum ber-akun ditandai "belum
  lengkap" di layar itu dan biayanya menetap sebagai draf di dasbor jurnal.
- Untuk Arus Kas per Aktivitas: jalankan **Diagnosa Pemetaan Aktivitas Arus Kas**, lalu petakan
  akun yang muncul lewat Akuntansi > Setup Laporan (jenis "Arus Kas") atau kolom Aktifitas pada
  Kode Akun. Tambahkan grup **Pendanaan** bila format yayasan menghendakinya — grup yang ada baru
  Operasional dan Investasi.

Tidak ada tabel baru; kolom `akun` pada `kategori_biaya_sales`, `posting_history` pada
`nota_sales_biaya`, dan `nota_sales_biaya` pada `akunting.grup_transaksi` dibuat `hbm2ddl update`.
