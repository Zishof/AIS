# Gap Analysis Posting: Transaksi Keuangan yang BELUM Punya Jalur Jurnal

Tanggal: 29 Agustus 2026, pada HEAD r78580. Menjawab "semua posting sudah diterapkan —
posting apa yang belum?" Analisis dilakukan dari SISI DATA, bukan dari daftar tombol:
(1) seluruh kolom referensi dokumen pada `akunting.grup_transaksi`, (2) seluruh
pemanggil `CommonAkunting.saveTransaksi`, (3) entitas keuangan per modul yang tidak
menyentuh jurnal sama sekali.

## 1. Yang sudah TUNTAS (baseline)

48 baris dasbor Draft Jurnal semuanya ber-mesin (dok 53–59, diaudit dok 60); baris
"Closing" memang bukan posting. Semua kolom referensi `grup_transaksi` tercakup jalur
posting **kecuali satu** (§2.A). Empat helper API menjurnal langsung saat input (bukan
batch, bukan celah): JurnalPenyesuaianHelper, SaldoAwalAkunHelper, TutupBukuHelper,
PostingKantinLanjutanHelper.

## 2. CELAH — transaksi keuangan tanpa jalur posting/jurnal

### A. Simpan-Pinjam Koperasi (`koperasi.transaksi_koperasi`) — kerangka posting YATIM
Satu-satunya dokumen yang jelas DIRANCANG untuk diposting tetapi tidak pernah
diselesaikan: entitas punya field `postingHistory`, `grup_transaksi` punya kolom
referensi `transaksi_koperasi`, dan layar `TransaksiKoperasiAction` mengunci
tanggal-persetujuan begitu "terposting" — tetapi TIDAK ADA satu baris kode pun yang
mengecap `postingHistory`-nya, menjurnalnya, atau menyediakan layar/baris posting.
Kaki PENCAIRAN pinjaman yang lewat pengajuan transfer memang terjurnal (baris "Jurnal
Pengajuan Transfer"), tetapi simpanan masuk, angsuran, dan bunganya tidak pernah
menyentuh buku besar. **Prioritas tertinggi — niat desainnya sudah setengah jadi.** **SELESAI r78584** —
mesin + baris dasbor + dua akun master produk, teruji 14/14, dok
[62-posting-simpan-pinjam-koperasi.md](62-posting-simpan-pinjam-koperasi.md).

### B. Keluarga dana anggota koperasi — TANPA integrasi akunting sama sekali
Semua entitas berikut menggerakkan uang/kewajiban (saldo anggota = utang koperasi
kepada anggota) tanpa satu pun referensi akun/jurnal:

| Entitas | Alur uang |
|---|---|
| `PembayaranAnggotaKoperasi(+Detail)` | setoran/pembayaran anggota |
| topup saldo online (VA Esmartlink → saldo anggota) | kas masuk + kewajiban bertambah |
| `PenyesuaianSaldoAnggota` | koreksi saldo e-wallet (kewajiban berubah tanpa jejak buku besar) |
| `PencairanDiskon` | kas keluar |
| `DepositoRolloverKoperasi` | penempatan/perpanjangan deposito |
| `ModalPenyertaanKoperasi` | setoran modal (ekuitas) |
| `PembagianShu` / `ShuAnggota` | distribusi SHU (ekuitas → kewajiban/kas) |

Semula ditandai "perlu KEPUTUSAN AKUNTANSI dulu (akun kewajiban saldo anggota, akun modal,
akun SHU)". **SELESAI r78646/r78649/r78651** — pemilihan akun dibuat DAPAT DIATUR lewat
Konfigurasi sehingga kebijakan tetap milik lembaga tanpa menahan mesinnya. Lima dokumen kini
berjurnal (topup saldo, pencairan diskon, penyesuaian saldo, modal penyertaan masuk, pembagian
SHU), teruji 31/31, dok [68-posting-dana-anggota-koperasi.md](68-posting-dana-anggota-koperasi.md).
Yang tersisa hanya kaki PENGEMBALIAN modal penyertaan (butuh cap posting kedua); deposito
rollover dinyatakan TIDAK perlu jurnal.

### C. `PembatalanTransaksiKantin` — refund tanpa jurnal balik
Pembatalan penjualan kantin tidak menjurnal apa pun. Selama headernya BELUM masuk
batch "Penjualan Kantin", itu benar (header batal keluar dari kriteria). Tetapi bila
pembatalan terjadi SETELAH batch terposting, tidak ada jurnal balik → pendapatan dan
kas/piutang lebih catat. Padanannya di sisi toko sudah benar (modul Toko memakai
dokumen pembalik yang ikut dijurnal — dok 59 §2); kantin belum. **SELESAI r78603** —
baris "Pembatalan Penjualan Kantin" + mesin jurnal balik, teruji 9/9, dok
[64-jurnal-balik-pembatalan-kantin.md](64-jurnal-balik-pembatalan-kantin.md).

### D. `PenghapusanMasterAsset(+Detail)` — disposal aset tanpa posting
Penghapusan/pelepasan aset tidak punya jalur jurnal (standarnya: lepas nilai buku,
lepas akumulasi penyusutan, akui rugi/laba pelepasan). Modul penyusutannya sendiri
sudah terposting, jadi buku besar akan menyimpan aset yang fisiknya sudah dihapus. **SELESAI r78620** —
baris "Penghapusan Aset" + mesin memakai pasangan akun JenisPengapusanBarang, teruji
10/10, dok [65-posting-penghapusan-aset.md](65-posting-penghapusan-aset.md).

### E. Modul Inventory Sales — DUGAAN "BUKU TERPISAH" TERNYATA KELIRU
Penelusuran ulang (dok [69](69-penutup-peta-posting.md)) menunjukkan modul ini TIDAK memakai buku
tandingan: layar `kas_jurnal` membaca langsung `akunting.transaksi` dan Master Akun-nya adalah
`akunting.akun` yang sama. `NotaSalesPembelian` sekadar TAUTAN ke faktur kulakan yang sudah punya
jalur posting; `NotaSalesKas` adalah catatan laci kas sesi (kontrol operasional, sejenis sesi kas
kasir yang memang tidak dijurnal per sesi). **Celah nyatanya satu: biaya sesi sales** —
**SELESAI r78666**, akun beban diambil dari master Kategori Biaya Sales.

### Catatan operasional (bukan celah akuntansi)
Sesi kas kasir (modal awal, setoran akhir) adalah kontrol kas operasional, tidak lazim
dijurnal per sesi; kas-nya sudah terwakili batch Penjualan Kantin.

## 3. Rekomendasi urutan

1. **A — Simpan-Pinjam** (kerangka sudah ada; tinggal mesin + baris dasbor, pola dok 53–58).
2. **C — jurnal balik pembatalan kantin** (kebenaran angka yang SUDAH diposting).
3. **D — disposal aset** (kebutuhan akuntansi standar, berpasangan dengan penyusutan).
4. **B — keluarga dana anggota** (menunggu keputusan bagan akun dari tim akuntansi).
5. **E — Inventory Sales**: putuskan lingkup (tetap buku terpisah + konsolidasi manual,
   atau jembatan posting).
