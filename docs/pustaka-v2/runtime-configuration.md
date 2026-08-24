# Konfigurasi Runtime Pustaka V2

Fitur tidak membuat skema paralel dan memakai data AIS existing.

- `library_search_synonyms`: grup sinonim dipisahkan `;`, istilah dipisahkan `,`. Contoh: `farmasi=apotik,pharmacy;skripsi=tugas akhir,thesis`.
- Peta rak hanya muncul jika item mempunyai `RakDetail` yang terhubung ke `Rak`; nomor panggil bukan pengganti lokasi rak.
- Booking fasilitas mengambil `Ruang` aktif dalam scope Yayasan/Sekolah/Fakultas/Jurusan dan menolak jadwal yang bertabrakan pada `PesanRuangan`.
- Tanya Pustakawan dan interlibrary loan dicatat sebagai `Pesan` bertanda layanan. Usulan anggota dicatat sebagai `PermintaanPengadaanItem` bertanda `[USULAN ANGGOTA]`.
- Reader hanya membuka URL HTTP(S), path lokal aman, atau lampiran yang lolos policy `bolehDiDownload`. Posisi dan catatan reader disimpan lokal di perangkat sampai tersedia storage catatan anggota lintas perangkat.
- Preferensi saved-search menyimpan cadence dan channel pada record `SearchHistory`. Pengiriman Email/WhatsApp tetap mengikuti worker notifikasi AIS dan konfigurasi gateway tenant yang aktif.
- RFID/self-check tetap disabled-by-default sampai URL bridge HTTPS dan kredensial server dikonfigurasi.
- Deployment harus membawa `WEB-INF/baru/pustaka.jsp`, seluruh `WEB-INF/baru/modul/pustaka/*.jsp` termasuk file berawalan `_`, asset `library-modern`, dan seluruh class `ais.action.master.library.modern`.
- Endpoint dengan suffix `_api`/`_service` harus mengembalikan JSON, bukan halaman fallback HTML. Periksa log `library-modern route adapter` dan `library typed catalog API` apabila katalog menampilkan “Katalog belum dapat dimuat”.
- Whitelist route `pustaka.jsp` harus diselaraskan dengan route di `landing_page.jsp`; baseline 24 Agustus 2026 masih perlu menambahkan `reader`, `layanan_anggota`, dan `_engagement_api`.

Tidak ada fallback yang mengarang denah, ruang, berkas digital, atau status layanan apabila master data belum dikonfigurasi.
