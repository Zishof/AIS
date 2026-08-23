# Completion Checklist Perpustakaan AIS V2

## P0

- Typed Java API menggantikan SQL/class request dari browser.
- Scope institusi dan cabang diterapkan oleh `LibraryScopeResolver` pada search, facet, suggestion, detail, holdings, rekomendasi, favorit, dan reservasi.
- Role/capability server diterapkan oleh `LibraryPermissionGuard`, `LibraryCatalogApi`, dan `LibraryMemberApi`.
- Compact hero, server facet count, filter chips, DB paging/sort, mobile bottom sheet, state lengkap, generated cover, keyboard/focus, dan query timeout tersedia.

## P1

- Metadata card, holdings cabang/rak, reservasi, favorit, akses digital, autocomplete, recent/popular query, save/share search, header state-aware, dan detail bibliografi tersedia.

## P2

- Citation export: RIS dari hasil katalog.
- Author/subject pages: URL katalog terfilter dan tautan dari metadata hasil.
- Recommendation: endpoint typed dan panel koleksi terkait pada detail.
- Shelf map: pencari lokasi cabang → rak/klasifikasi → status.
- Rating/review: rating 1–5, antrean moderasi, tampilan ulasan terbit.
- Saved search alert: baseline jumlah hasil, toggle alert, indikator hasil baru pada portal anggota.
- Analytics discovery: pencarian, subjek, dan koleksi paling dilihat pada dashboard sesuai hak.
- RFID/self-check: perintah typed checkout/check-in menuju bridge RFID HTTPS yang disabled-by-default.
- Multi-select hasil: daftar bacaan/favorit, perbandingan, reservasi massal, ekspor RIS, dan berbagi tautan.
- Saved-search preference: cadence NONE/NEW/DAILY/WEEKLY dan channel APP/EMAIL/WHATSAPP disimpan pada record audited existing.
- Reader digital: PDF/external document, audio/video HTML5, playback speed, posisi baca, catatan perangkat, dan watermark pengguna.
- Layanan anggota: Tanya Pustakawan, usulan koleksi, booking fasilitas dengan collision check, serta interlibrary loan.
- Detail bernavigasi: ringkasan, ketersediaan, isi/lampiran, metadata, ulasan, dan koleksi terkait.
- Mobile scan: ISBN/barcode melalui BarcodeDetector browser dengan fallback input manual.
- Reservasi kontekstual: `Reservasi` untuk eksemplar tersedia dan `Masuk Antrean` jika stok cabang sedang dipinjam.
- Advanced search: ISBN/ISSN, rentang tahun, bidang bibliografi, AND/OR, dan pengecualian NOT tersimpan pada URL/saved search.

## P3

- Peta rak hanya ditampilkan jika `RakDetail`/`Rak` benar-benar dikonfigurasi; call number tidak lagi diperlakukan sebagai lokasi rak.
- Fuzzy suggestion menggunakan fallback prefix dan edit distance terbatas tanpa mengirim SQL/class dari browser.
- Facet/statistics memiliki cache server 30 detik yang dipisahkan berdasarkan scope dan filter.
- Booking dan permintaan layanan menggunakan entitas AIS existing yang teraudit; RFID/self-check tetap melalui typed gateway.
- Validasi booking memeriksa kembali scope Yayasan/Sekolah/Fakultas/Jurusan di server sehingga manipulasi ID ruang tidak dapat menembus tenant.

Tidak ada skema database baru. Fitur menggunakan model AIS yang telah tersedia (`SearchHistory`, `ItemKomentar`, `ItemHasStatus`, holdings, dan konfigurasi integrasi).

Prasyarat tenant dan perilaku fallback dijelaskan pada `runtime-configuration.md`.
