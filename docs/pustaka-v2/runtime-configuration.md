# Konfigurasi Runtime Pustaka V2

Fitur tidak membuat skema paralel dan memakai data AIS existing.

- `library_search_synonyms`: grup sinonim dipisahkan `;`, istilah dipisahkan `,`. Contoh: `farmasi=apotik,pharmacy;skripsi=tugas akhir,thesis`.
- Peta rak hanya muncul jika item mempunyai `RakDetail` yang terhubung ke `Rak`; nomor panggil bukan pengganti lokasi rak.
- Booking fasilitas mengambil `Ruang` aktif dalam scope Yayasan/Sekolah/Fakultas/Jurusan dan menolak jadwal yang bertabrakan pada `PesanRuangan`.
- Tanya Pustakawan dan interlibrary loan dicatat sebagai `Pesan` bertanda layanan. Usulan anggota dicatat sebagai `PermintaanPengadaanItem` bertanda `[USULAN ANGGOTA]`.
- Pesan layanan baru membawa marker `LIBRARY_ID` agar antrean petugas dan penyelesaian tetap tenant-scoped. Pesan legacy tanpa marker tidak dimasukkan otomatis ke antrean modern.
- Reader hanya membuka URL HTTP(S), path lokal aman, atau lampiran yang lolos policy `bolehDiDownload`. Posisi dan catatan reader disimpan lokal di perangkat sampai tersedia storage catatan anggota lintas perangkat.
- Preferensi saved-search menyimpan cadence, channel, dan `LIBRARY_ID` pada record `SearchHistory`. Worker `LibrarySavedSearchNotificationWorker` tersedia tetapi default-nya nonaktif. Aktifkan dengan `library.saved_search.worker.enabled=true`; interval polling dapat diatur melalui `library.saved_search.worker.interval_minutes` (default 15 menit). Channel EMAIL/WHATSAPP tetap membutuhkan konfigurasi `MailSender`/gateway tenant yang benar. Record legacy diberi marker saat alert diaktifkan kembali; worker melewati record tanpa scope.
- RFID/self-check tetap disabled-by-default sampai URL bridge HTTPS dan kredensial server dikonfigurasi.
- Endpoint petugas `health` menampilkan telemetry per-node tanpa query, payload, atau identitas pengguna. Pada deployment multi-node, agregasi dan rate limit lintas-node tetap harus disediakan oleh reverse proxy/observability platform bersama.
- Security filter menambahkan request ID, `nosniff`, pembatasan frame/referrer/permission, CSP kompatibel JSP legacy, no-store untuk API/OAI, HSTS ketika HTTPS, dan `Server-Timing`.
- Deployment harus membawa `WEB-INF/baru/pustaka.jsp`, seluruh `WEB-INF/baru/modul/pustaka/*.jsp` termasuk file berawalan `_`, asset `library-modern`, dan seluruh class `ais.action.master.library.modern`.
- Endpoint dengan suffix `_api`/`_service` harus mengembalikan JSON, bukan halaman fallback HTML. Periksa log `library-modern route adapter` dan `library typed catalog API` apabila katalog menampilkan “Katalog belum dapat dimuat”.
- Whitelist route `pustaka.jsp` telah diselaraskan dengan `landing_page.jsp` untuk `login_pustaka`, `reader`, `layanan_anggota`, dan `_engagement_api` pada 25 Agustus 2026.

Tidak ada fallback yang mengarang denah, ruang, berkas digital, atau status layanan apabila master data belum dikonfigurasi.
