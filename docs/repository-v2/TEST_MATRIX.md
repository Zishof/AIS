# Matriks pengujian Repository AIS

Self-test source tersedia pada package `ais.action.master.repository.test`. Build dan eksekusi tidak
dilakukan dalam sesi pengembangan ini. Jalankan pada staging menggunakan classpath aplikasi yang
sama dengan WAR.

## Self-test tanpa mutasi database

- `RepositoryFaqCatalogSelfTest`: 300 FAQ, 20 kategori, pencarian, dan koreksi halaman.
- `RepositoryWorkspacePaginationSelfTest`: batas page size serta allow-list status.
- `RepositoryAlertParserSelfTest`: parsing URL alert dan penolakan parameter invalid.
- `RepositoryFileSecuritySelfTest`: signature berkas, nama file, MIME authoritative, dan akses fail-closed.
- `RepositoryOaiProtocolSelfTest`: token OAI bertanda tangan, binding verb, dan penolakan tamper.

## Matriks staging wajib

1. **Anonim:** portal, pencarian, koleksi, author, FAQ, bantuan, metadata-only, embargo, withdrawn,
   OAI-PMH, sitemap, robots, rate limit, dan penolakan full text terautentikasi.
2. **Depositor:** buat draf, autosave, filter/paging Deposit Saya, metadata wajib, duplikat, upload,
   watermark, signature, checksum, scanner, submit, revisi, resubmit, dan optimistic lock.
3. **Reviewer:** filter/paging antrean, claim, komentar, return, reject, approve, serta larangan edit
   metadata/berkas milik depositor.
4. **Administrator:** publish, withdraw, restore, koleksi, authority merge, fixity, bulk repair,
   retry sync, search alert manual, readiness, ekspor, dan audit integrasi.
5. **Tenant:** ulangi role matrix pada dua tenant dan pastikan ID dari tenant A selalu 403/404 pada B.
6. **Integrasi:** sandbox DataCite, ORCID OAuth state, ROR, COAR Notify, antivirus, AI gateway,
   timeout, retry, invalid credential, dan audit payload aman.
7. **Restore:** ikuti `BACKUP_RESTORE.md`, jalankan smoke validator, lalu cocokkan checksum/identifier.
8. **Aksesibilitas:** keyboard-only, pembaca layar, zoom 200/400%, fokus, kontras, target sentuh,
   reduced motion, tabel, diagram bantuan, serta pesan error.
9. **Responsif:** 1920×1080, 1366×768, tablet, dan 360–390 px untuk seluruh state utama.
10. **Beban:** pencarian, semantic candidate search, OAI paging, detail populer, download, scheduler
    sinkron, scheduler alert, antrean 10.000 item, dan degradasi integrasi eksternal.
11. **Hardening akhir:** coba path bitstream di luar storage, nama/MIME dengan karakter kontrol,
    scanner yang melewati timeout, berkas berstatus `ERROR`/`INFECTED`, form tanpa versi dan versi
    usang, serta `publicBaseUrl` berisi path/query/kredensial. Pastikan semuanya ditolak aman.
12. **Isolasi lintas tenant:** selain halaman detail, verifikasi export XLSX, fixity, metadata
    quality, daftar notifikasi, mark-as-read, dan statistik alert tidak memuat ID tenant lain.
13. **Origin publik:** cocokkan canonical HTML, robots, sitemap, RSS/Atom, URL DataCite, dan COAR
    dengan `ais.repository.publicBaseUrl`; uji pula port nonstandar serta konfigurasi IPv6.
14. **Konsol ZK Repository:** login sebagai pengelola perpustakaan dengan privilege UPDATE, pastikan
    tombol `Unggah Karya Ilmiah` membuka draft baru; simpan metadata lalu unggah PDF. Pada tab Item,
    pastikan judul dan `Buka` menuju detail publik, `Kelola file` membuka item yang sama, serta jumlah
    item/collection/bitstream hanya berasal dari tenant aktif. Ulangi dengan role read-only dan
    pastikan `Kelola file` tidak muncul.

Setiap kegagalan harus mencatat request ID, tenant, role, URL, waktu, input nonrahasia, hasil aktual,
hasil yang diharapkan, dan bukti log. Jangan memakai data produksi untuk test destruktif.
