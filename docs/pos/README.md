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
| [79-penjaga-hapus-riwayat-kurang-lengkap.md](79-penjaga-hapus-riwayat-kurang-lengkap.md) | Penjaga doc 74 hanya menghitung 2 dari 64 entitas perujuk posting_history; dipertegas dgn jenis riwayat |
| [80-layar-jurnal-menyentuh-dokumen-modul.md](80-layar-jurnal-menyentuh-dokumen-modul.md) | unpost menolak jurnal bikinan modul; deleteAll menyisakan baris yatim; tiga pertanyaan terbuka soal cleanDuplicates |
| [73-stok-minus-tiga-nilai-dan-pemulihan-member.md](73-stok-minus-tiga-nilai-dan-pemulihan-member.md) | "STOK MINUS" pd verifikasi pesanan: `null` diperlakukan sbg "dikunci admin"; + pemulihan nama pemesan dari audit Envers |
| [74-sql-pemulihan-member-pesanan.sql](74-sql-pemulihan-member-pesanan.sql) | Skrip hitung-dulu-baru-perbaiki utk mengembalikan member yang tertimpa NULL |
| [75-halaman-pesanan-tiga-celah-sunyi.md](75-halaman-pesanan-tiga-celah-sunyi.md) | `id_member` yang diambil lalu dibuang, "Bayar Semua" yang bilang sukses saat gagal, dan `peringatanStok` tanpa pembaca |
| [76-peringatan-stok-terbaca-dan-nota-terparkir.md](76-peringatan-stok-terbaca-dan-nota-terparkir.md) | `peringatanStok` yang belum dibaca klien Flutter, dan nota terparkir GAGAL oleh penolakan stok yang keliru |
| [77-gerbang-oversell-dan-penjaga-field-yatim.md](77-gerbang-oversell-dan-penjaga-field-yatim.md) | Sakelar "Cegah Oversell" yang tak pernah dibaca jalur API + penjaga otomatis field yang dikirim server tanpa pembaca |
| [78-penjaga-arah-sebaliknya.md](78-penjaga-arah-sebaliknya.md) | Penjaga kunci payload yang dikirim klien tetapi tidak pernah dibaca server (arah cacat dok. 45) |
| [79-enam-belas-utang-ditelusuri.md](79-enam-belas-utang-ditelusuri.md) | Vonis 16 field yatim satu per satu: 5 peringatan rekonsiliasi kas yang tak sampai ke siapa pun, 11 sisanya tidak merugikan |
| [80-satu-saluran-peringatan-pasca-transaksi.md](80-satu-saluran-peringatan-pasca-transaksi.md) | Enam peringatan lepas jadi satu saluran; biaya menambah peringatan berikutnya turun dari 9 titik jadi 1 |
| [81-lima-salinan-payload-jadi-satu.md](81-lima-salinan-payload-jadi-satu.md) | Lima salinan payload bayar yang sudah menyebabkan tiga cacat, disatukan; payloadnya diverifikasi dijalankan, bukan dibaca |
| [82-penjaga-yang-tidak-ikut-terversi.md](82-penjaga-yang-tidak-ikut-terversi.md) | Harness yang diklaim menjaga ternyata tak terversi; `src/test` (18 UAT Java) di luar SVN |
| [83-penjaga-yang-hanya-berjalan-di-satu-mesin.md](83-penjaga-yang-hanya-berjalan-di-satu-mesin.md) | Alat penjaga ternyata hanya jalan di satu mesin; ujinya menemukan dua cara alat itu melapor salah tanpa suara |
| [84-sakelar-yang-tidak-menyalakan-apa-apa.md](84-sakelar-yang-tidak-menyalakan-apa-apa.md) | Sakelar Konfigurasi yang tak dibaca siapa pun; POS bersih, 218 kandidat di modul lain, dan alasan penjaganya sengaja tidak dibuat |
| [85-bukti-verifikasi-yang-dibuang.md](85-bukti-verifikasi-yang-dibuang.md) | Sidik jari terverifikasi lalu idnya dibuang, pembayaran ditolak "ulangi PIN"; uji lama hijau selama cacatnya hidup |
| [86-jalan-keluar-yang-tidak-ada.md](86-jalan-keluar-yang-tidak-ada.md) | Server menyuruh "simpan ulang dengan persetujuan", padahal tak ada klien yang bisa memberikannya; arah keempat diukur |
| [87-janji-yang-kini-dijaga.md](87-janji-yang-kini-dijaga.md) | Pintu darurat yang penandanya tak bisa dikirim klien mana pun kini dijaga; himpunannya tiga, jadi boleh jadi gerbang |
| [88-kedekatan-bukan-sebab-akibat.md](88-kedekatan-bukan-sebab-akibat.md) | Pintu bernilai ikut dijaga; kedekatan diganti pencocokan blok, dan isi himpunan dok. 87 dikoreksi |
| [89-gerbang-yang-tidak-pernah-menutup.md](89-gerbang-yang-tidak-pernah-menutup.md) | Empat endpoint yang gerbangnya meloloskan setiap peran; kunci menu tak terdaftar membuat bolehAksi selalu true |
| [90-angka-cakupan-yang-salah.md](90-angka-cakupan-yang-salah.md) | Batas "113 di luar jangkauan" ternyata 30; 68 gerbang berkonstanta kini terresolusi dan terbukti benar |

| [35-lampiran-gambar.md](35-lampiran-gambar.md) | Lampiran gambar — blob, maksimum 500 KB, dikecilkan di klien |
| [52-fase-b-satuan-jual.md](52-fase-b-satuan-jual.md) | 52. Fase B — Satuan Jual per Baris Transaksi |
| [53-fase-c-reordering-lengkap.md](53-fase-c-reordering-lengkap.md) | 53. Fase C — Reordering Lengkap (Min-Max, Rute BELI/PRODUKSI) |
| [53-posting-jurnal-umum.md](53-posting-jurnal-umum.md) | Posting Massal "Jurnal Umum" dari Dasbor Draft Jurnal POS |
| [54-fase-d-reservasi-kekurangan-unbuild.md](54-fase-d-reservasi-kekurangan-unbuild.md) | 54. Fase D — Reservasi Komponen, Kekurangan → Pengajuan, UNBUILD |
| [54-posting-pengembalian-uang-muka.md](54-posting-pengembalian-uang-muka.md) | Posting Massal "Pengembalian Uang Muka" + Perbaikan Hitung Dasbor Bendera-Null |
| [55-fase-e-mto-dan-qc.md](55-fase-e-mto-dan-qc.md) | 55. Fase E — MTO (Make-To-Order) dan QC Hasil Produksi |
| [55-posting-trio-pembayaran-vendor.md](55-posting-trio-pembayaran-vendor.md) | Posting Massal Trio Pembayaran Vendor (Tagihan / DP / Termin) |
| [56-posting-perjanjian-kerjasama.md](56-posting-perjanjian-kerjasama.md) | Posting Massal "Perjanjian Kerjasama" (DP Kerjasama Aset) |
| [56-ringkasan-fase-0-e-untuk-pemilik.md](56-ringkasan-fase-0-e-untuk-pemilik.md) | 56. Ringkasan Program UOM–Packaging–Manufaktur (Fase 0–E) — untuk Pemilik Sistem |
| [57-fase-penutup-pratinjau-grosir-web-dan-reservasi.md](57-fase-penutup-pratinjau-grosir-web-dan-reservasi.md) | 57. Fase Penutup — Pratinjau Grosir Kanal Web + Tampilan Reservasi WO |
| [57-posting-payroll-pegawai-penggajian.md](57-posting-payroll-pegawai-penggajian.md) | Posting Massal Payroll: Transaksi Pegawai + Penggajian Pegawai |
| [57-posting-pembayaran-vendor.md](57-posting-pembayaran-vendor.md) | Posting Massal Trio Pembayaran Vendor (Tagihan, DP, Termin) |
| [58-pindah-skema-produksi-distribusi-ke-koperasi.md](58-pindah-skema-produksi-distribusi-ke-koperasi.md) | 58. Tabel Produksi & Distribusi Pindah ke Skema `koperasi` |
| [58-posting-saldo-awal-kas-kecil.md](58-posting-saldo-awal-kas-kecil.md) | Posting Massal Saldo Awal Kas Kecil |
| [59-posting-kantin-dasbor.md](59-posting-kantin-dasbor.md) | Dasbor Draft Jurnal: Keluarga Kantin/Toko (HPP, Penjualan, 4 Posting Toko) |
| [59-stok-uom-pengadaan-pr-po-bast.md](59-stok-uom-pengadaan-pr-po-bast.md) | 59. PDF "stok & uom" — Rantai Pengadaan (PR→PO→BAST) Sadar-UOM |
| [60-audit-silang-kantin-kaskecil.md](60-audit-silang-kantin-kaskecil.md) | Audit Silang Dok 58–59: Empat Cacat Ditemukan & Diperbaiki |
| [60-metode2-kelipatan-reservasi-galat-foto-label.md](60-metode2-kelipatan-reservasi-galat-foto-label.md) | 60. Butir Terbuka Dituntaskan — Metode 2, Kelipatan Wajib, Saklar Reservasi, Galat, Foto Member, Label PR/PO |
| [61-gap-analysis-posting.md](61-gap-analysis-posting.md) | Gap Analysis Posting: Transaksi Keuangan yang BELUM Punya Jalur Jurnal |
| [61-settingan-pack-combo.md](61-settingan-pack-combo.md) | 61. Settingan Pack/Combo — Jual per Pack dengan Harga Tetap |
| [62-posting-simpan-pinjam-koperasi.md](62-posting-simpan-pinjam-koperasi.md) | Posting Simpan-Pinjam Koperasi: Kerangka Yatim Akhirnya Dilengkapi |
| [62-umpan-balik-layar-grosir-satuan-bast.md](62-umpan-balik-layar-grosir-satuan-bast.md) | 62. Umpan Balik Layar (31-08) — Editor Grosir Dapat Diedit, Nominal Satuan Jual, Hasil Sinkron BAST |
| [63-gap-analysis-zk-vs-pos.md](63-gap-analysis-zk-vs-pos.md) | Gap Analysis Posting: Hub ZKoss "Posting Jurnal" vs Dasbor Draft Jurnal POS Flutter |
| [63-rapikan-master-uom-per-kategori.md](63-rapikan-master-uom-per-kategori.md) | 63. Merapikan Master UOM per Kategori |
| [64-jurnal-balik-pembatalan-kantin.md](64-jurnal-balik-pembatalan-kantin.md) | Jurnal Balik Pembatalan Kantin |
| [64-rincian-produk-terjual-laporan-kasir.md](64-rincian-produk-terjual-laporan-kasir.md) | 64. Rincian Produk Terjual pada Laporan Kasir |
| [65-perbaikan-laporan-penjualan-web-dan-rekap-produk.md](65-perbaikan-laporan-penjualan-web-dan-rekap-produk.md) | 65. Perbaikan Laporan Penjualan Web, Filter, dan Rekap Produk |
| [65-posting-penghapusan-aset.md](65-posting-penghapusan-aset.md) | Posting Penghapusan Aset: Pasangan Akun Jenis Penghapusan Akhirnya Dipakai |
| [66-laporan-keuangan-standar-yayasan.md](66-laporan-keuangan-standar-yayasan.md) | Gap Analysis Laporan Keuangan: Paket Standar Yayasan vs AIS/POS |
| [66-uji-internal-1-34-17-apk-debug.md](66-uji-internal-1-34-17-apk-debug.md) | 66. Uji Internal 1.34.17 — APK Bertanda Tangan Debug |
| [67-konsistensi-angka-laporan-dan-filter-lanjutan.md](67-konsistensi-angka-laporan-dan-filter-lanjutan.md) | 67. Konsistensi Angka Laporan, Satuan Jual, dan Filter Lanjutan |
| [67-laporan-aktivitas-dan-pemilih-unit.md](67-laporan-aktivitas-dan-pemilih-unit.md) | Laporan Aktivitas (Surplus/Defisit) & Pemilih Unit pada Laporan Keuangan |
| [68-batas-baris-laporan-selftest-dan-nilai-dashboard.md](68-batas-baris-laporan-selftest-dan-nilai-dashboard.md) | 68. Batas Baris Laporan, Self-Test Aturan SQL, dan Nilai Dashboard |
| [68-posting-dana-anggota-koperasi.md](68-posting-dana-anggota-koperasi.md) | Posting Dana Anggota Koperasi (dok 61 butir B) |
| [69-audit-tombol-zk-menyeluruh.md](69-audit-tombol-zk-menyeluruh.md) | Audit Menyeluruh Tombol Posting Layar ZK (47 Layar) + Penutupan Celah Hub |
| [69-panel-ringkasan-menyaring-baris-dibatalkan.md](69-panel-ringkasan-menyaring-baris-dibatalkan.md) | 69. Panel Ringkasan Ikut Menyaring Baris Penjualan yang Dibatalkan |
| [69-penutup-peta-posting.md](69-penutup-peta-posting.md) | Penutup Peta Posting & Laporan: Butir E, Buku Kas Umum, dan Diagnosa Aktivitas |
| [70-audit-dokumen-berkaki-ganda.md](70-audit-dokumen-berkaki-ganda.md) | Audit Dokumen Ber-Kaki Posting Ganda: Tiga Cacat Pembatalan |
| [70-permukaan-sql-klien-dan-penjaganya.md](70-permukaan-sql-klien-dan-penjaganya.md) | 70. Permukaan SQL dari Klien dan Penjaganya |
| [71-sql-tulis-anonim-ditutup.md](71-sql-tulis-anonim-ditutup.md) | 71. Jalur SQL Tulis Anonim pada `/Data` Ditutup |
| [71-tabrakan-kodeunik-antar-kaki.md](71-tabrakan-kodeunik-antar-kaki.md) | Tabrakan `kodeUnik` Antar-Kaki Jurnal: Mekanisme Terbukti, Kaki Siswa Diperbaiki |
| [72-blokir-kolom-kredensial-tanpa-syarat.md](72-blokir-kolom-kredensial-tanpa-syarat.md) | 72. Blokir Kolom Kredensial dari Endpoint SQL Klien — Tanpa Syarat |
| [72-jurnal-tak-seimbang-dan-tanggal.md](72-jurnal-tak-seimbang-dan-tanggal.md) | Dua Kelas Sisa: Jurnal Tak Seimbang dan Tanggal Jurnal di Luar Rentang |
| [73-amankan-resolusi-kelas-unggahan.md](73-amankan-resolusi-kelas-unggahan.md) | 73. Amankan Resolusi Kelas pada Unggahan Berkas |
| [81-satu-klik-membalik-sebatch.md](81-satu-klik-membalik-sebatch.md) | Satu klik membalik status se-batch, dan kompilasi penuh sebagai alat |
| [82-gerbang-yang-hampir-berbohong.md](82-gerbang-yang-hampir-berbohong.md) | Gerbang kompilasi cepat (hanya berkas berubah), dan cacat pada gerbang itu sendiri yang melaporkan BERSIH tanpa mengompilasi apa pun |
| [83-kanal-jsp-tanpa-gerbang.md](83-kanal-jsp-tanpa-gerbang.md) | JSP tak pernah dikompilasi sebelum dibuka pengguna; gerbang Jasper luring, dan sapuan 10.374 JSP bersih dalam 82 detik |
| [84-scriptlet-jsp-yang-tak-pernah-dikompilasi.md](84-scriptlet-jsp-yang-tak-pernah-dikompilasi.md) | Java di dalam scriptlet tidak pernah dikompilasi; tiga halaman yang pasti gagal dibuka, ditemukan dan diperbaiki |
| [85-gerbang-keempat-dan-koreksi-doc-84.md](85-gerbang-keempat-dan-koreksi-doc-84.md) | Gerbang scriptlet JSP; koreksi klaim doc 84 yang salah; tiga halaman rusak lagi |
| [86-mengejar-hantu-pohon-kelas-basi.md](86-mengejar-hantu-pohon-kelas-basi.md) | Empat halaman diperbaiki; sebagian galat ternyata hantu dari pohon kelas basi; tiga halaman menunjuk kelas yang sudah lenyap |
| [87-pohon-kelas-basi-dalam-satu-menit.md](87-pohon-kelas-basi-dalam-satu-menit.md) | Pohon kelas basi satu menit sesudah dibangun; gerbang kini mengukur kesegarannya sendiri |
| [88-kanal-zul-tanpa-gerbang.md](88-kanal-zul-tanpa-gerbang.md) | 1.557 berkas ZUL tak pernah diperiksa; 15 rujukan kelas menggantung, dua diperbaiki, delapan halaman mati masih dirujuk |
| [ATURAN-NATIVE-SQL-CAST.md](ATURAN-NATIVE-SQL-CAST.md) | Aturan wajib cast pada native SQL |
| [BANKALTIMTARA-KADALUARSA-H2H-2026-08-26.md](BANKALTIMTARA-KADALUARSA-H2H-2026-08-26.md) | Pengamanan Pembayaran Kedaluwarsa Bankaltimtara |
| [PERBAIKAN_ERROR_2026-08-24_1936.md](PERBAIKAN_ERROR_2026-08-24_1936.md) | Perbaikan Error POS/eBisnis 24 Agustus 2026 19:36 |
| [ZUL-GRID-FULL-WIDTH-2026-08-26.md](ZUL-GRID-FULL-WIDTH-2026-08-26.md) | Normalisasi Grid/Tabel ZUL Full Width |

> **Dicocokkan ulang dengan isi folder.** Lima puluh dokumen di atas sudah ada di
> direktori ini tetapi belum pernah masuk daftar isi, sehingga praktis tidak dapat
> ditemukan dari pintu depan. Ringkasannya diambil apa adanya dari judul H1 masing-masing
> dokumen -- kata penulisnya sendiri, bukan tafsiran ulang.

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
