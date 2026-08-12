# UAT JSP Inventory & Sales — 48 layar

Tanggal: 13 Agustus 2026  
Server: `http://localhost:18080/ais`  
Database: PostgreSQL `ais` pada `localhost:5432`, pengguna `root`  
Aktor UI: `muklis` — role `Pemilik Usaha Sales`  
Aktor pembanding API: `agung` — role `Sales Keliling`

## Kesimpulan

Seluruh 48 halaman JSP berhasil dibuka melalui login web nyata, melewati pemeriksaan sesi dan RBAC, memilih nomor layar yang benar, menjalankan JavaScript Inventory & Sales, serta menghubungi servlet `/Api_eBisnis`. Bukti PNG beresolusi 2576×1408 disimpan di folder `screenshots/`. Setiap halaman sekarang memakai nama fungsi, bukan lagi `layar_XX.jsp`.

Audit struktur menemukan tepat 48 wrapper JSP yang valid, masing-masing menetapkan `inventoryInitialScreen` dan menyertakan workspace utama. UI memanggil 72 aksi `si_*`; seluruh nama aksi tersebut memiliki implementasi pada helper server. Uji smoke terhadap endpoint baca menemukan dua ketidaksesuaian yang kemudian diperbaiki: laporan pembelian membutuhkan alias parameter `dari`/`sampai`, dan query COA masih memakai nama kolom lama `debet_credit` alih-alih kolom eksplisit `debit_credit`.

## Akun contoh

| Pengguna | Password lokal | Role | Landing setelah login |
|---|---|---|---|
| `muklis` | `muklis123` | Pemilik Usaha Sales | `/WEB-INF/baru/modul/inventory/index.jsp` |
| `agung` | `agung123` | Sales Keliling | `/WEB-INF/baru/modul/inventory/index.jsp` |

Seed akun bersifat idempoten dan hanya berjalan bila JVM diaktifkan dengan `-Dais.inventory.uat.seed=true`; deployment biasa tidak membuat kredensial contoh.

## Matriks 48 layar

| No. | Fungsi / JSP | Endpoint utama | Hasil | Bukti |
|---:|---|---|---|---|
| 01 | `data_supplier.jsp` | `si_supplier_list` | PASS | [PNG](screenshots/01-data_supplier.png) |
| 02 | `daftar_supplier.jsp` | `si_supplier_list` | PASS | [PNG](screenshots/02-daftar_supplier.png) |
| 03 | `detail_supplier_aktif.jsp` | `si_supplier_list` | PASS | [PNG](screenshots/03-detail_supplier_aktif.png) |
| 04 | `data_customer.jsp` | `si_customer_list` | PASS | [PNG](screenshots/04-data_customer.png) |
| 05 | `daftar_customer.jsp` | `si_customer_list` | PASS | [PNG](screenshots/05-daftar_customer.png) |
| 06 | `detail_customer_aktif.jsp` | `si_customer_list` | PASS | [PNG](screenshots/06-detail_customer_aktif.png) |
| 07 | `data_sales.jsp` | `si_sales_list` | PASS | [PNG](screenshots/07-data_sales.png) |
| 08 | `data_stok_barang.jsp` | `si_inventory_balance` | PASS | [PNG](screenshots/08-data_stok_barang.png) |
| 09 | `laporan_opname.jsp` | `si_inventory_balance` | PASS | [PNG](screenshots/09-laporan_opname.png) |
| 10 | `cetak_laporan_opname.jsp` | `si_inventory_balance` | PASS | [PNG](screenshots/10-cetak_laporan_opname.png) |
| 11 | `harga_beli_jual.jsp` | `si_price_analysis` | PASS | [PNG](screenshots/11-harga_beli_jual.png) |
| 12 | `cetak_harga_beli_jual.jsp` | `si_price_analysis` | PASS | [PNG](screenshots/12-cetak_harga_beli_jual.png) |
| 13 | `cetak_harga_jual.jsp` | `si_customer_price_list` | PASS | [PNG](screenshots/13-cetak_harga_jual.png) |
| 14 | `ekspor_harga_stok.jsp` | `si_price_analysis` | PASS | [PNG](screenshots/14-ekspor_harga_stok.png) |
| 15 | `cetak_daftar_stok.jsp` | `si_inventory_balance` | PASS | [PNG](screenshots/15-cetak_daftar_stok.png) |
| 16 | `hasil_cetak_stok.jsp` | `si_inventory_balance` | PASS | [PNG](screenshots/16-hasil_cetak_stok.png) |
| 17 | `menu_master_harga.jsp` | `si_price_analysis` | PASS | [PNG](screenshots/17-menu_master_harga.png) |
| 18 | `harga_beli_supplier.jsp` | `si_supplier_price_list` | PASS | [PNG](screenshots/18-harga_beli_supplier.png) |
| 19 | `harga_jual_customer.jsp` | `si_customer_price_list` | PASS | [PNG](screenshots/19-harga_jual_customer.png) |
| 20 | `pembelian_supplier.jsp` | `si_purchase_report` | PASS | [PNG](screenshots/20-pembelian_supplier.png) |
| 21 | `hutang_pembelian.jsp` | `si_payable_list` | PASS | [PNG](screenshots/21-hutang_pembelian.png) |
| 22 | `data_hutang_supplier.jsp` | `si_payable_list` | PASS | [PNG](screenshots/22-data_hutang_supplier.png) |
| 23 | `hutang_dengan_lunas.jsp` | `si_payable_list` | PASS | [PNG](screenshots/23-hutang_dengan_lunas.png) |
| 24 | `pembayaran_hutang.jsp` | `si_payable_list` | PASS | [PNG](screenshots/24-pembayaran_hutang.png) |
| 25 | `riwayat_pembayaran_hutang.jsp` | `si_payable_payment_history` | PASS | [PNG](screenshots/25-riwayat_pembayaran_hutang.png) |
| 26 | `cetak_pembayaran_hutang.jsp` | `si_payable_payment_history` | PASS | [PNG](screenshots/26-cetak_pembayaran_hutang.png) |
| 27 | `analisis_hutang.jsp` | `si_payable_aging` | PASS | [PNG](screenshots/27-analisis_hutang.png) |
| 28 | `cetak_faktur_pembelian.jsp` | `si_purchase_report` | PASS | [PNG](screenshots/28-cetak_faktur_pembelian.png) |
| 29 | `laporan_pembelian_periode.jsp` | `si_purchase_report` | PASS | [PNG](screenshots/29-laporan_pembelian_periode.png) |
| 30 | `penjualan_sales.jsp` | `si_sales_order_list` | PASS | [PNG](screenshots/30-penjualan_sales.png) |
| 31 | `piutang_penjualan.jsp` | `si_receivable_list` | PASS | [PNG](screenshots/31-piutang_penjualan.png) |
| 32 | `data_piutang_customer.jsp` | `si_receivable_list` | PASS | [PNG](screenshots/32-data_piutang_customer.png) |
| 33 | `piutang_dengan_lunas.jsp` | `si_receivable_list` | PASS | [PNG](screenshots/33-piutang_dengan_lunas.png) |
| 34 | `pembayaran_piutang.jsp` | `si_receivable_list` | PASS | [PNG](screenshots/34-pembayaran_piutang.png) |
| 35 | `riwayat_pembayaran_piutang.jsp` | `si_collection_history` | PASS | [PNG](screenshots/35-riwayat_pembayaran_piutang.png) |
| 36 | `cetak_pembayaran_piutang.jsp` | `si_collection_history` | PASS | [PNG](screenshots/36-cetak_pembayaran_piutang.png) |
| 37 | `analisis_piutang_customer.jsp` | `si_receivable_aging_customer` | PASS | [PNG](screenshots/37-analisis_piutang_customer.png) |
| 38 | `analisis_piutang_sales.jsp` | `si_receivable_aging_sales` | PASS | [PNG](screenshots/38-analisis_piutang_sales.png) |
| 39 | `surat_perintah_sales.jsp` | `si_spj_list` | PASS | [PNG](screenshots/39-surat_perintah_sales.png) |
| 40 | `nota_sales.jsp` | `si_trip_list` | PASS | [PNG](screenshots/40-nota_sales.png) |
| 41 | `laporan_piutang.jsp` | `si_receivable_report` | PASS | [PNG](screenshots/41-laporan_piutang.png) |
| 42 | `cetak_laporan_piutang.jsp` | `si_receivable_report` | PASS | [PNG](screenshots/42-cetak_laporan_piutang.png) |
| 43 | `kas_jurnal.jsp` | `si_cash_journal_list` | PASS | [PNG](screenshots/43-kas_jurnal.png) |
| 44 | `data_perkiraan.jsp` | `si_coa_list` | PASS | [PNG](screenshots/44-data_perkiraan.png) |
| 45 | `parameter_laba_rugi.jsp` | `si_profit_loss_params` | PASS | [PNG](screenshots/45-parameter_laba_rugi.png) |
| 46 | `cetak_laba_rugi_kotor.jsp` | `si_gross_profit_report` | PASS | [PNG](screenshots/46-cetak_laba_rugi_kotor.png) |
| 47 | `laporan_laba_rugi.jsp` | `si_profit_loss_report` | PASS | [PNG](screenshots/47-laporan_laba_rugi.png) |
| 48 | `cetak_laporan_laba_rugi.jsp` | `si_profit_loss_print` | PASS | [PNG](screenshots/48-cetak_laporan_laba_rugi.png) |

## Catatan harness transaksi lama

Harness `uat_runtime_48layar.ps1` juga dijalankan. Login Pemilik dan Sales, deteksi deployment, RBAC negatif, impor DBF idempoten, laporan hutang/piutang, kas, serta laba-rugi berhasil. Harness lama melaporkan banyak kegagalan berantai karena masih membaca kontrak respons datar, sedangkan API sekarang mengembalikan beberapa objek bersarang; contoh konkret: harness membaca `actorType` di root, tetapi respons aktual menyimpannya pada `aktor.actorType`. Hasil tersebut tidak dipakai untuk memberi label gagal pada render JSP. UAT di dokumen ini berfokus pada 48 halaman, routing, RBAC, render, dan endpoint baca yang benar-benar dipakai saat halaman dibuka.

## Reproduksi

Jalankan AIS lokal dengan database yang disebutkan di atas, aktifkan seed lokal, lalu:

```powershell
powershell -ExecutionPolicy Bypass -File .\run-ui-capture.ps1 `
  -Username muklis -Password muklis123
```

Skrip membuka jendela Edge khusus, login lewat `j_spring_security_check`, mengunjungi 48 URL bernama fungsi, menunggu respons API, lalu menyimpan satu PNG per layar. Kredensial hanya ditulis ke file HTML sementara di `%TEMP%` untuk auto-submit dan segera dihapus setelah login.
