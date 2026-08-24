# Handoff AI — Modernisasi Perpustakaan AIS V2

Tanggal baseline: 24 Agustus 2026  
Workspace: `C:\opt\AIS\ais\src\main`

## 1. Tujuan dan cakupan

Modernisasi Perpustakaan Digital AIS diterapkan pada dua jalur sekaligus:

1. ZKoss: `src/ais/action/master/library`
2. JSP publik: `webapp/WEB-INF/baru/modul/pustaka`

Keduanya memakai model bisnis AIS yang sama. Tidak dibuat basis data, katalog, atau framework frontend paralel.

## 2. Prinsip implementasi

- Tema mengikuti CSS variable PerguruanTinggi/Sekolah aktif.
- Search, facet, paging, sort, dan scope dilakukan server-side.
- Browser tidak boleh mengirim SQL atau nama class.
- Endpoint mutasi memakai autentikasi, capability, tenant scope, dan CSRF.
- Data rak, file digital, ruang, serta integrasi eksternal tidak dibuat-buat apabila belum dikonfigurasi.
- JSP mempertahankan kompatibilitas stack lama; tidak memakai template literal JavaScript yang dapat dibaca Jasper sebagai EL.

## 3. UI dan tema yang telah dikerjakan

- Warna, gradient, tombol, chip, status aktif, header, hero, dan card memakai variable tema institusi.
- Kontras teks hero diperjelas dan judul panjang tidak lagi terpotong.
- Ukuran judul responsif dengan `clamp`.
- Header/navigation, dialog, loading, empty, dan error state diseragamkan.
- Layout desktop, tablet, dan mobile tersedia.
- Tidak menambah CDN atau framework frontend baru.

## 4. Discovery dan katalog

- Hero search dan compact sticky search setelah scroll.
- Sticky search mempertahankan query serta menyediakan Filter dan Urutkan.
- Bidang pencarian: semua, judul, penulis, ISBN/ISSN, subjek, penerbit, nomor panggil, dan barcode.
- DB paging, page size, sorting, dan facet server-side.
- URL menyimpan query, filter, sort, halaman, dan view sehingga dapat refresh/share.
- Filter aktif berbentuk chip, dapat dihapus satu per satu atau seluruhnya.
- Facet collapsible, menyembunyikan nilai nol, dan mempunyai “Lihat lainnya”.
- Facet desktop menghindari nested scroll; mobile memakai bottom sheet.
- List/grid view memiliki active state, tooltip, `aria-pressed`, dan state tersimpan.
- Statistik Judul, Digital, Eksemplar, dan Cabang dapat diklik dan mempunyai tooltip.
- Angka Digital hanya berasal dari file/tautan yang lolos policy akses.
- Popular search tidak hard-coded; memakai data subjek/katalog aktual.
- Autocomplete dikelompokkan per judul, penulis, subjek, nomor panggil, dan ISBN.
- Debounce, pembatalan request sebelumnya, cache suggestion, partial match, fuzzy/edit distance terbatas, serta sinonim konfigurabel.
- Empty state menyediakan reset filter, saran kata, dan tautan usulan koleksi.
- Error/offline/session/access state memakai pesan berbeda dan live region.

## 5. Pencarian lanjutan

- AND/OR melalui `matchMode`.
- NOT melalui `exclude`.
- Judul, penulis, penerbit, ISBN/ISSN, nomor panggil, catatan/teks terindeks.
- Rentang tahun, bahasa, jenis koleksi, format, cabang, sekolah, dan program studi.
- Seluruh kondisi disimpan pada URL dan saved search.
- Sinonim runtime: konfigurasi `library_search_synonyms`.

Catatan: full-text isi PDF/EPUB dan semantic/vector search penuh masih membutuhkan search index eksternal. Implementasi saat ini mencari metadata dan teks yang telah tersedia pada model AIS.

## 6. Result card dan aksi katalog

- Cover asli atau generated cover.
- Jenis koleksi, format, digital badge, judul, penulis, penerbit, tahun, bahasa, ISBN, edisi, halaman, ringkasan, subjek, dan nomor panggil.
- Ringkasan holdings, jumlah tersedia, cabang, rak sebenarnya, status, dan tanggal jatuh tempo jika ada.
- CTA kontekstual `Reservasi` atau `Masuk Antrean`.
- Favorit, Baca Online, Peta Rak, Salin Nomor Panggil, Sitasi, Bagikan, dan Detail.
- Peta Rak disembunyikan jika `RakDetail/Rak` belum tersedia; call number tidak dianggap sebagai rak.
- Multi-select: daftar bacaan, perbandingan, reservasi massal, ekspor RIS, share, dan clear selection.

## 7. Detail koleksi

Source utama: `webapp/WEB-INF/baru/modul/pustaka/_item_rinci.jsp`.

- Read model typed melalui `LibraryItemDetailService`.
- Navigasi Ringkasan, Ketersediaan, Isi/Lampiran, Metadata, Ulasan, dan Koleksi Terkait.
- Holdings per eksemplar: barcode, cabang, rak, status, jatuh tempo, dan antrean tanpa identitas peminjam.
- Reader link hanya diberikan jika policy file mengizinkan.
- Rating 1–5, ulasan terbit, dan antrean moderasi.
- Rekomendasi koleksi terkait.
- Redesign terakhir:
  - background foto rak yang mengurangi kontras dihilangkan;
  - header mengikuti gradient tema institusi;
  - cover, aksi, judul, byline, dan statistik mempunyai hierarki baru;
  - metadata menjadi grid card tanpa tanda titik dua berulang;
  - metadata bernilai `-` otomatis disembunyikan;
  - holdings dan tombol reservasi/antrean dirapikan;
  - panel rekomendasi dan ulasan dipisahkan;
  - layout responsif sampai mobile.

## 8. Portal anggota dan sirkulasi

- Header login menampilkan nama dan status anggota Aktif/Belum Aktif.
- Dashboard: pinjaman aktif, jatuh tempo, reservasi, denda, kunjungan, dan notifikasi.
- Pinjaman, riwayat, perpanjangan, reservasi, pembatalan reservasi, favorit/daftar bacaan.
- Perpanjangan menolak transaksi yang telah kembali, melebihi batas, atau mempunyai antrean.
- Reservasi memeriksa status anggota, koleksi publik, stok cabang, read-only policy, duplikasi, dan tenant scope.

## 9. Saved search dan alert

- Pengguna anonim diarahkan login.
- Nama pencarian dan seluruh filter disimpan pada `SearchHistory` existing.
- URL canonical dibentuk kembali dari state tersimpan.
- Cadence: NONE, NEW, DAILY, WEEKLY.
- Channel: APP, EMAIL, WHATSAPP.
- Baseline count, indikator hasil baru, toggle alert, serta hapus saved search.
- Kolom `text_query` dan `text_result` bertipe `text`, sehingga metadata preference tidak dipotong.

Pengiriman Email/WhatsApp tetap membutuhkan worker notifikasi dan gateway tenant di server.

## 10. Reader digital

- Dokumen/PDF atau URL aman melalui iframe.
- Audio/video HTML5.
- Playback speed.
- Bookmark posisi dan continue reading.
- Catatan pribadi lokal pada perangkat.
- Watermark identitas pengguna.
- Hanya menerima URL HTTP(S), path lokal aman, atau attachment yang lolos `bolehDiDownload`.

EPUB reader penuh, penyimpanan catatan lintas perangkat, subtitle, transkrip, chapter marker, audio description, dan adaptive streaming memerlukan data/infrastruktur tambahan.

## 11. Layanan anggota

UI: `layanan_anggota.jsp`; API: `LibraryEngagementApi` dan `_engagement_api.jsp`.

- Tanya Pustakawan disimpan sebagai `Pesan` bertanda `[ASK_LIBRARIAN]`.
- Usulan koleksi disimpan sebagai `PermintaanPengadaanItem` bertanda `[USULAN ANGGOTA]`.
- Form usulan: judul, penulis, ISBN, mata kuliah, urgensi, tautan penerbit, dan alasan.
- Booking fasilitas memakai `Ruang`/`PesanRuangan`, collision check, serta validasi ulang Yayasan/Sekolah/Fakultas/Jurusan di server.
- Interlibrary loan disimpan sebagai `Pesan` bertanda `[INTERLIBRARY_LOAN]`.
- Riwayat permintaan anggota tersedia.
- Tombol bantuan mengambang menjadi “Tanya Pustakawan”.

## 12. Mobile dan accessibility

- Sticky search serta toolbar Filter, Urutkan, Mode, dan Scan.
- BarcodeDetector untuk ISBN/barcode dengan fallback input manual.
- Result card satu kolom dan filter bottom sheet.
- Focus-visible, keyboard navigation, scroll margin untuk sticky header.
- Label/tooltip tombol ikon, `aria-pressed`, live region, heading semantik, alt cover.
- Target sentuh, reflow, dan horizontal overflow telah diperhatikan.

## 13. Backend, keamanan, dan performa

- Typed Java facade/API untuk katalog, member, operasional, workspace, layanan, login, MARC, OAI, dan integrasi.
- Parameter binding/Criteria/HQL; browser tidak menentukan SQL/class.
- Scope institusi dan cabang melalui `LibraryScopeResolver`.
- Capability melalui `LibraryPermissionGuard` dan API terkait.
- CSRF pada mutasi serta rate limit search/suggestion.
- Cache facet/statistik 30 detik berdasarkan scope dan filter.
- Batch holdings/availability, lazy cover, dan paging database.
- Policy file digital dan audit operasi penting.
- Booking memvalidasi ulang tenant meskipun ID ruang dimanipulasi dari browser.

## 14. ZKoss

- `KatalogOnlineAction` memakai `LibraryCatalogSearchService` yang sama dengan JSP.
- Search metadata/paging server-side, metadata edisi/halaman, serta link reader.
- `ItemAction` dan `KaryaTulisItemAction` mempunyai workflow penerbitan katalog publik.
- Workspace operasional modern dan katalogisasi MARC tersedia.
- Status terbit publik menjadi sumber yang sama bagi ZKoss dan JSP.

## 15. Integrasi petugas

- Dashboard discovery analytics.
- MARC dan OAI-PMH.
- SRU/Z39.50 bridge, NCIP, SIP2, COUNTER/SUSHI.
- RFID/self-check typed gateway, disabled-by-default.
- Tidak ada integrasi eksternal yang dianggap aktif sebelum URL/kredensial tersedia.

## 16. Perbaikan kompatibilitas JSP EL

Error Jasper `Failed to parse the expression [${}]` berasal dari regex JavaScript pada `katalog.jsp` yang mempunyai urutan literal `${}`. Regex telah ditulis ulang tanpa urutan tersebut.

Audit terakhir:

- seluruh 30 JSP pada `webapp/WEB-INF/baru/modul/pustaka` diperiksa;
- entry point `webapp/WEB-INF/baru/pustaka.jsp` diperiksa;
- tidak ada lagi `${` pada JSP modul Pustaka;
- struktur tag JSP utama dan `git diff --check` diperiksa secara statis.

## 17. Temuan runtime/deployment yang belum selesai

Pada server STTIF, request endpoint katalog:

`/pustaka?hanya_tampil_jsp=true&p=pustaka&s=_catalog_api&action=search`

mengembalikan HTTP 200 dengan body HTML “Halaman versi JSP belum tersedia”, bukan JSON. Akibatnya `response.json()` gagal, statistik tetap `—`, facet kosong, dan UI menampilkan “Katalog belum dapat dimuat”.

Kemungkinan penyebab deployment:

- `_catalog_api.jsp` atau class `ais.action.master.library.modern` belum ikut deploy/build;
- `WEB-INF/baru/pustaka.jsp` pada server masih versi lama;
- JSP API gagal compile dan router menampilkan fallback;
- file JSP berawalan `_` tidak ikut paket deployment.

Selain itu, whitelist lokal `webapp/WEB-INF/baru/pustaka.jsp` sudah memuat `_catalog_api`, tetapi belum memuat rute baru berikut:

- `reader`
- `layanan_anggota`
- `_engagement_api`

AI/implementer berikutnya harus menyelaraskan whitelist tersebut sebelum menganggap reader dan layanan anggota dapat diroute penuh.

## 18. Pekerjaan yang bergantung pada data/infrastruktur

- Worker notifikasi Email/WhatsApp dan gateway resmi tenant.
- Bridge/kredensial RFID serta perangkat self-check.
- Full-text extraction/index dan semantic/vector search.
- Denah lantai, marker rak, jalur, akses kursi roda, QR lokasi, dan kiosk map.
- EPUB/subtitle/transkrip/adaptive streaming.
- Reading list per mata kuliah dan mapping LMS/akademik.
- Storage bookmark/catatan reader lintas perangkat.
- Master data ruang, rak, file digital, embargo, dan lisensi tiap tenant.

## 19. Verifikasi yang telah dan belum dilakukan

Telah dilakukan:

- audit source dan model existing;
- pencarian pola SQL/class unsafe pada browser;
- pemeriksaan literal `${` pada JSP Pustaka;
- pemeriksaan keseimbangan tag utama;
- `git diff --check` pada file yang disentuh;
- pemeriksaan respons endpoint katalog server secara read-only.

Belum dilakukan sesuai instruksi pemilik:

- build WAR;
- compile/test lokal;
- migration database baru (memang tidak dibuat);
- deployment;
- end-to-end browser test sesudah deployment.

## 20. Urutan kerja yang disarankan untuk AI berikutnya

1. Perbaiki whitelist `pustaka.jsp` untuk `reader`, `layanan_anggota`, dan `_engagement_api`.
2. Pastikan seluruh adapter JSP berawalan `_` dan class package `modern` masuk build/deployment server.
3. Periksa log server `library-modern route adapter` dan `library typed catalog API`.
4. Verifikasi endpoint API benar-benar JSON sebelum mengevaluasi koleksi kosong.
5. Uji katalog desktop/mobile, detail, login anggota, reservasi, saved search, reader, dan layanan anggota.
6. Baru lakukan review visual berbasis screenshot runtime terbaru.

## 21. Prompt siap pakai untuk AI berikutnya

```text
Pelajari seluruh dokumentasi pada:
C:\opt\AIS\ais\src\main\docs\pustaka-v2

Mulai dari ai-handoff-2026-08-24.md. Anggap daftar fitur yang ditandai selesai sebagai baseline dan jangan mengulang rekomendasi generik. Pertama, audit pekerjaan tersisa pada bagian “Temuan runtime/deployment yang belum selesai” dan “Pekerjaan yang bergantung pada data/infrastruktur”.

Jika diminta melakukan review desain, gunakan screenshot runtime terbaru dan berikan temuan konkret per komponen, dampak, solusi, prioritas P0–P3, serta acceptance criteria. Bedakan perbaikan CSS/UI, frontend behavior, backend/data, deployment, dan integrasi eksternal.

Jika diminta mengimplementasikan, pertahankan dua jalur JSP dan ZKoss, gunakan service/model AIS existing, jangan membuat database paralel, wajib menjaga tenant scope, CSRF, typed endpoint, tema PerguruanTinggi/Sekolah, accessibility, dan kompatibilitas JSP lama. Jangan menulis urutan literal `${` di file JSP karena Jasper dapat membacanya sebagai EL.

Jangan menjalankan build WAR atau test lokal kecuali pemilik sistem memberikan instruksi baru. Laporkan secara jujur fitur yang masih membutuhkan worker, gateway, search index, master data, atau konfigurasi server.
```

