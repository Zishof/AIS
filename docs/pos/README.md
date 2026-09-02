# POS Desktop & Android (eBisnis) — Modul Akuntansi & Keuangan

Dokumentasi pekerjaan pemindahan menu ZK ke aplikasi **POS Desktop/Android (eBisnis)**
pada repositori Flutter `C:\opt\CodeBaseDesktopDanMobile` (GitHub `Zishof/zishof-platform`,
paket `apps/ebisnis`), dengan sisi servernya berupa API JSON di
`src/ais/action/servlet/api/` pada working copy SVN ini.

> Semua nama kelas, metode, kolom, dan aturan bisnis di dokumen ini **diverifikasi dari
> source dan dari basis data UAT**, bukan asumsi. Angka hasil uji diambil dari harness
> yang benar-benar dijalankan; setiap harness membersihkan data ujinya sendiri.

---

## Daftar isi

| Berkas | Isi |
|---|---|
| [01-grup-menu-akuntansi.md](01-grup-menu-akuntansi.md) | Menu "Akuntansi", submenu, hak akses per aksi, CRUD Kode Akun |
| [02-grup-menu-keuangan.md](02-grup-menu-keuangan.md) | Enam modul Keuangan, dasbor, cetak, lokal-dulu & hapus lunak |
| [03-penggunaan-anggaran.md](03-penggunaan-anggaran.md) | Pemotongan anggaran per baris rincian, seperti layar ZK |
| [04-muara-dpc.md](04-muara-dpc.md) | Pengajuan proses transfer (Daftar Pengajuan Transfer) |
| [05-uang-muka-dari-pr.md](05-uang-muka-dari-pr.md) | "Diambil dari Permintaan Pengadaan" pada Uang Muka |
| [06-posting-jurnal-keuangan.md](06-posting-jurnal-keuangan.md) | Mesin posting jurnal dokumen Keuangan dari dasbor Draft Jurnal |
| [07-temuan-dan-jebakan.md](07-temuan-dan-jebakan.md) | Cacat yang ditemukan (termasuk sesi Hibernate tertutup di tengah posting), jebakan data, dan hal yang mudah salah |
| [08-harness-uji.md](08-harness-uji.md) | Daftar harness, cara menjalankan, dan hasil terakhirnya |
| [15-dana-talangan.md](15-dana-talangan.md) | Dana talangan atas uang muka yang transfernya sudah cair |
| [16-reimbursement-pegawai.md](16-reimbursement-pegawai.md) | Penggantian biaya pegawai, lima status & keputusan tiga arah |
| [17-master-data-keuangan.md](17-master-data-keuangan.md) | Master data keenam jenis & cara pembayaran: pemetaan akun yang menentukan jurnal |
| [18-proses-transfer.md](18-proses-transfer.md) | Pencairan DPC: empat tahap, tanda Transfer/Transitori, dan penyaring kategori yang tidak menyembunyikan |
| [19-dasbor-cetak-keuangan-yang-mati.md](19-dasbor-cetak-keuangan-yang-mati.md) | Tiga cacat dari log produksi: helper yatim, dasbor salah modul, cetak dokumen orang lain |
| [20-proses-transitori.md](20-proses-transitori.md) | Jalan keluar rekening transitori, catatan transitori yang tak pernah lahir, dan dua jebakan skema |
| [21-penomoran-dokumen-keuangan.md](21-penomoran-dokumen-keuangan.md) | Templat nomor per jenis dokumen: sembilan dari sepuluh alur masih terbit berkode barcode |
| [22-closing-penutupan-periode.md](22-closing-penutupan-periode.md) | Penutupan periode: penautan jurnal ke closing paling awal, dan getter yang menulis saat dibaca |
| [23-posting-pajak.md](23-posting-pajak.md) | Baris Pajak di Draft Jurnal: tombol posting yang mati, hitungan yang melebih-lebihkan, dan penanda yang dapat tertimpa flush |
| [24-posting-dp-vendor.md](24-posting-dp-vendor.md) | Rantai DP vendor: tiga tombol mati sekaligus, dan kolom ref sebagai satu-satunya pemisah antar modul pada dokumen yang sama |
| [25-posting-bast.md](25-posting-bast.md) | Jurnal saat BAST untuk dua kelompok aset, dan utilitas yang menutup sesi pemanggilnya |
| [26-posting-penyusutan.md](26-posting-penyusutan.md) | Jurnal penyusutan bulanan, pola dua fase dipakai sejak awal, dan kunci hak posting yang berdiri sendiri |
| [09-draft-jurnal-dasbor.md](09-draft-jurnal-dasbor.md) | Dasbor Draft Jurnal di POS: ringkasan 31 baris, rincian dokumen, tombol posting |
| [10-pesan-galat-dan-detail-error.md](10-pesan-galat-dan-detail-error.md) | Alasan penolakan yang tampil apa adanya, dan penyingkap "Detail Error" |
| [11-navigasi-menu-akuntansi.md](11-navigasi-menu-akuntansi.md) | Deretan tab di layar Akuntansi diganti dropdown grup |
| [12-penyesuaian-saldo-jalur-web.md](12-penyesuaian-saldo-jalur-web.md) | Opname saldo voucher/deposit: padanan web dari tombol POS |
| [13-rilis-dan-operasional.md](13-rilis-dan-operasional.md) | Rilis v1.33.68 tujuh varian, dan catatan penyapu working copy |
| [14-mesin-posting-per-modul.md](14-mesin-posting-per-modul.md) | Pola porting mesin posting per modul, dua penyimpangan sadar, jebakan akhir baris, dan bukti jalur tulis |
| [15-kunci-sesi-dan-login-luring.md](15-kunci-sesi-dan-login-luring.md) | Batas waktu lokal mengunci layar, bukan membuang token; bukti sandi untuk buka kunci tanpa jaringan |
| [16-menu-pencairan-satu-pintu.md](16-menu-pencairan-satu-pintu.md) | Proses Transfer, Pembayaran Vendor, dan Transitori disatukan menjadi satu menu berisi lima tab |
| [17-bagan-akun-format-accurate.md](17-bagan-akun-format-accurate.md) | Unduh/unggah bagan akun berformat Accurate, kolom `tipe_akun`, dan Bersihkan Akun khusus admin |
| [18-mesin-posting-lengkap.md](18-mesin-posting-lengkap.md) | 32 dari 35 baris dasbor punya mesin posting; tiga baris yang sengaja tidak dibuatkan mesin |
| [40-pengadaan.md](40-pengadaan.md) | Alur PR-PO-BAST-Tagihan-Bayar-Pajak, Back Order, lampiran, cetak |
| [41-pengajuan-anda-sop.md](41-pengajuan-anda-sop.md) | Modul "Pengajuan Anda" (Workflow/SOP) berbasis API, tanpa iframe/WebView |
| [42-lokal-dulu-dan-hapus-lunak.md](42-lokal-dulu-dan-hapus-lunak.md) | Keputusan hapus lunak lokal vs audit Hibernate, dan yang tetap daring |
| [43-perbaikan-ecampus-2026-08.md](43-perbaikan-ecampus-2026-08.md) | Enam akar masalah dari dua belas laporan galat ECAMPUS |
| [44-uji-regresi.md](44-uji-regresi.md) | Menjalankan regresi, hasil 22-08-2026, dan jebakan yang sudah memakan waktu |
| [45-penyaring-dasbor-dan-layani-semua.md](45-penyaring-dasbor-dan-layani-semua.md) | Penyaring "Jenis pembayaran" yang tak pernah dibaca server, dan "Layani Semua" yang menyapu lebih luas daripada yang dilihat |
| [46-angka-tanpa-rincian.md](46-angka-tanpa-rincian.md) | Bendera `bisaRincian` yang tak pernah dibaca layar, dan alasan yang ikut dikirim bersamanya |
| [47-sesi-kas-transaksi-terlambat.md](47-sesi-kas-transaksi-terlambat.md) | Sesi kas dan transaksi yang tiba terlambat |
| [48-gap-analisis-uom-packaging-manufaktur.md](48-gap-analisis-uom-packaging-manufaktur.md) | Peta kemampuan vs tiga PDF ERP: UoM, packaging, harga grosir, produksi, reordering |
| [49-produksi-eksekusi-stok-dan-rencana-rinci.md](49-produksi-eksekusi-stok-dan-rencana-rinci.md) | Koreksi peta 48, temuan dokumen produksi tidak menggerakkan stok, dan langkah pengerjaan per fase |
| [50-fase-0-produksi-menggerakkan-stok.md](50-fase-0-produksi-menggerakkan-stok.md) | Fase 0 terlaksana: ledger mutasi_stok_produksi, rumus 9 suku, harness 19/0 |
| [51-fase-a-harga-grosir.md](51-fase-a-harga-grosir.md) | Fase A inti: mesin harga grosir ber-ambang, kait bayar+pratinjau, harness 13/0 |
| [20-ikhtisar-kantin-multi-toko.md](20-ikhtisar-kantin-multi-toko.md) | Kantin, multi-toko, audit & proses otomatis |
| [21-hak-akses-ubah-harga.md](21-hak-akses-ubah-harga.md) | Hak akses ubah harga |
| [22-filter-toko-dan-pendaftar.md](22-filter-toko-dan-pendaftar.md) | Filter toko lintas-toko & pembatasan per pendaftar |
| [23-toko-pada-payload.md](23-toko-pada-payload.md) | "Toko tidak diketahui" — toko yang tidak pernah dikirim |
| [24-proses-otomatis-jam-24.md](24-proses-otomatis-jam-24.md) | Bayar/layani otomatis: konfigurasi global & per toko, penjadwal harian |
| [25-riwayat-audit-history.md](25-riwayat-audit-history.md) | Tombol History: jelajah tabel audit + restore satuan/massal |
| [26-produk-kulakan-laporan.md](26-produk-kulakan-laporan.md) | Stok per tanggal, ekspor laporan, salinan tersimpan, cetak faktur |
| [27-ecanteen-aplikasi-anggota.md](27-ecanteen-aplikasi-anggota.md) | Aplikasi member kantin (Android + Desktop) |
| [28-harness-uji-dan-temuan.md](28-harness-uji-dan-temuan.md) | Uji yang dijalankan, hasilnya, dan jebakan yang ditemukan |
| [30-menu-aksi-baris.md](30-menu-aksi-baris.md) | Menu aksi baris "…" pada Desktop, Android, JSP, dan ZKoss |
| [31-pengadaan-termin.md](31-pengadaan-termin.md) | Pengadaan bertermin: penerimaan & tagihan per termin, cetak PO, status PO |
| [32-transfer-transitori.md](32-transfer-transitori.md) | Transfer vs Transitori per baris, dan realisasinya |
| [33-audit-lokal-dulu.md](33-audit-lokal-dulu.md) | Audit lokal-dulu: yang dikonversi, yang tetap wajib online |
| [34-hak-akses-menu-pos.md](34-hak-akses-menu-pos.md) | Kunci CRUD baru berikut penegakannya, dan tab Hak Akses Pedagang |
| [35-anggaran-id-negatif.md](35-anggaran-id-negatif.md) | Modul Anggaran: id negatif pada data warisan, ringkasan yang berlipat 3x, dan id 19 digit di kanal JSP |
| [36-gerbang-impor-dan-kanal-jsp.md](36-gerbang-impor-dan-kanal-jsp.md) | Impor massal Kode Akun yang tak bergerbang, paritas kanal JSP, dan kompilasi Java 7 |
| [37-angka-laporan-dapat-diklik.md](37-angka-laporan-dapat-diklik.md) | Angka subtotal & grand total Laporan-Laporan ikut dapat diklik di keempat kanal |
| [70-panduan-membuka-laporan-keuangan.md](70-panduan-membuka-laporan-keuangan.md) | Panduan staf: dua pintu menu ke katalog laporan, tujuh kebutuhan keuangan yayasan, dan tiga sebab laporan tampil kosong |
| [70-panduan-laporan-keuangan-an-nahl.pdf](70-panduan-laporan-keuangan-an-nahl.pdf) | Versi PDF bergambar dari panduan di atas, siap dibagikan ke staf; pembuatnya di [panduan-ilustrasi/](panduan-ilustrasi/README.md) |
| [74-riwayat-bersama-dan-tristate-dipipihkan.md](74-riwayat-bersama-dan-tristate-dipipihkan.md) | `unpost` menghapus riwayat yang masih dipakai dokumen lain; tri-state produk dipipihkan di payload PosApi; hasil sapuan tri-state se-basis-kode |
| [75-termin-berpajak-dan-javadoc-yatim.md](75-termin-berpajak-dan-javadoc-yatim.md) | Termin berpajak tak pernah ditandai terposting; JavaDoc yatim (175 pasangan) dan alasan ia tidak boleh disapu otomatis |
| [76-impor-excel-tanpa-batas-dan-anotasi-ganda.md](76-impor-excel-tanpa-batas-dan-anotasi-ganda.md) | Impor Excel didekode tanpa batas (ditutup); @ManyToOne ganda yang mematahkan kompilasi di HEAD; koreksi angka 110 yang ternyata 2 |
| [77-kompilasi-penuh-dan-javadoc-dikembalikan.md](77-kompilasi-penuh-dan-javadoc-dikembalikan.md) | Kompilasi seluruh pohon (7.430 berkas, 40.949 kelas, nol galat); dua JavaDoc dikembalikan ke metode yang dijelaskannya |
| [78-javadoc-yatim-tiga-remedi.md](78-javadoc-yatim-tiga-remedi.md) | Tiga belas blok yatim diselesaikan; remedi ketiga (HAPUS) muncul: boilerplate yang menyebut kelas induk keliru |
| [73-stok-minus-tiga-nilai-dan-pemulihan-member.md](73-stok-minus-tiga-nilai-dan-pemulihan-member.md) | "STOK MINUS" pd verifikasi pesanan: `null` diperlakukan sbg "dikunci admin"; + pemulihan nama pemesan dari audit Envers |
| [74-sql-pemulihan-member-pesanan.sql](74-sql-pemulihan-member-pesanan.sql) | Skrip hitung-dulu-baru-perbaiki utk mengembalikan member yang tertimpa NULL |
| [75-halaman-pesanan-tiga-celah-sunyi.md](75-halaman-pesanan-tiga-celah-sunyi.md) | `id_member` yang diambil lalu dibuang, "Bayar Semua" yang bilang sukses saat gagal, dan `peringatanStok` tanpa pembaca |
| [76-peringatan-stok-terbaca-dan-nota-terparkir.md](76-peringatan-stok-terbaca-dan-nota-terparkir.md) | `peringatanStok` yang belum dibaca klien Flutter, dan nota terparkir GAGAL oleh penolakan stok yang keliru |
| [77-gerbang-oversell-dan-penjaga-field-yatim.md](77-gerbang-oversell-dan-penjaga-field-yatim.md) | Sakelar "Cegah Oversell" yang tak pernah dibaca jalur API + penjaga otomatis field yang dikirim server tanpa pembaca |
| [78-penjaga-arah-sebaliknya.md](78-penjaga-arah-sebaliknya.md) | Penjaga kunci payload yang dikirim klien tetapi tidak pernah dibaca server (arah cacat dok. 45) |
| [79-enam-belas-utang-ditelusuri.md](79-enam-belas-utang-ditelusuri.md) | Vonis 16 field yatim satu per satu: 5 peringatan rekonsiliasi kas yang tak sampai ke siapa pun, 11 sisanya tidak merugikan |
| [80-satu-saluran-peringatan-pasca-transaksi.md](80-satu-saluran-peringatan-pasca-transaksi.md) | Enam peringatan lepas jadi satu saluran; biaya menambah peringatan berikutnya turun dari 9 titik jadi 1 |

> Catatan penomoran: ada **dua** berkas bernomor 10 — `10-pengadaan.md` dan
> `10-pesan-galat-dan-detail-error.md` — karena ditulis dua sesi kerja yang berjalan
> bersamaan. Isinya berbeda dan keduanya berlaku; penomorannya saja yang berbenturan.

---

## Prinsip yang dipegang di seluruh pekerjaan ini

**Mesinnya satu, bukan tiruan.** Setiap kali sebuah aturan sudah ada di layar ZK,
API memanggil kode yang sama persis — `JenisUangMukaAction.hitungSaldo`,
`CommonAkunting.saveTransaksi`, `DaftarPengajuanTransfer.simpanXxx`,
`PenggunaanAnggaran`. Yang dipindahkan adalah tampilannya, bukan perhitungannya.
Urutan validasi dan bunyi pesan penolakan pun disamakan supaya pengguna melihat hal
yang sama di web maupun di Desktop/Android.

**Alasan ditulis di kode.** Working copy SVN ini disapu alat otomatis yang meng-commit
apa pun yang kotor dengan pesan KOSONG (lihat [07](07-temuan-dan-jebakan.md)), sehingga
pesan commit sering tidak sempat terbentuk. Karena itu setiap keputusan yang tidak
kelihatan dari kodenya ditulis sebagai Javadoc/komentar di tempatnya.

**Cermin SVN.** `src/main/src` dan `src/main/java` adalah dua salinan berkas yang sama
dan harus selalu identik. Setiap perubahan di satu sisi disalin ke sisi lain sebelum
dianggap selesai.

**Java 1.7.** Seluruh kode server dikompilasi dengan `-source 1.7 -target 1.7` dan
ditulis bergaya Java 1.6: tanpa lambda, tanpa diamond operator, tanpa stream. Sesi
Hibernate yang dibuka dengan `openSession()` selalu ditutup di blok `finally`.

**Tidak ada migrasi SQL manual.** Perubahan skema diserahkan ke Hibernate.
