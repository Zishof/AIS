# Security Gap Review Perpustakaan AIS V2

## Ditutup pada alur pustaka modern

- Tidak ada raw SQL dari JavaScript.
- Tidak ada nama entity/class dari browser.
- Tidak ada query generik `tanpaLogin=true` pada katalog V2.
- Sort dan field pencarian memakai allow-list.
- Nilai filter dibatasi panjang/range dan di-bind melalui Hibernate.
- Mutasi anggota hanya POST dan CSRF tervalidasi.
- Ownership reservasi/favorit/perpanjangan diperiksa server-side.
- Item Draft, NULL, Ditolak, atau Disetujui tidak dapat dibaca melalui katalog/detail publik.
- Digital URL hanya menerima HTTP(S) atau path lokal.
- Path lokal digital menolak URL protocol-relative, backslash, CR/LF, dan traversal `..`.
- Akses reader/digital memerlukan anggota aktif atau role petugas; pengguna login biasa tidak otomatis memperoleh URL.
- Router `/pustaka` hanya dapat menginklusikan modul `pustaka` dan nama fragmen yang masuk allow-list.
- Kegagalan adaptor JSON/XML tidak lagi dialihkan menjadi fallback HTML.
- Error internal operasi petugas, integrasi, dan MARC dicatat pada audit server tanpa mengirim pesan exception mentah ke browser.
- Filter portal memberikan request ID, header hardening, CSP kompatibel JSP lama, HSTS pada HTTPS, no-store/noindex untuk API/OAI, dan server timing.
- Rate limiter server mempunyai batas key dan gagal tertutup; cakupannya per-node, bukan klaim sebagai proteksi cluster-wide.
- Operasi booking, hold/reservasi, stocktake, dan denda mengunci record terkait untuk mengurangi double-submit/race condition.
- Telemetry hanya menyimpan counter dan durasi per route, tanpa isi query, payload, atau identitas pengguna.

## Batas audit

AIS masih mempunyai service generik untuk modul legacy di luar alur `WEB-INF/baru/modul/pustaka`. Service tersebut tidak dipanggil oleh asset atau JSP pustaka modern. Penghapusan global perlu proyek lintas modul tersendiri agar tidak memutus kompatibilitas legacy.

## Tidak dilakukan pada perubahan ini

- Tidak ada perubahan skema database.
- Tidak ada migration karena facet/holdings memakai tabel existing.
- Build dan pengujian lokal tidak dijalankan sesuai arahan pemilik aplikasi.
- Pengetatan CSP tanpa `unsafe-inline` belum dapat dilakukan sebelum script/style inline JSP legacy dipindahkan atau diberi nonce secara menyeluruh.
