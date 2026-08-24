# Dokumentasi Modernisasi Perpustakaan AIS V2

Dokumentasi ini menjadi baseline implementasi dan handoff untuk pengembangan lanjutan modul Perpustakaan Digital AIS.

## Mulai dari sini

- [`ai-handoff-2026-08-24.md`](ai-handoff-2026-08-24.md) — ringkasan menyeluruh sesi, status aktual, pekerjaan tersisa, dan prompt untuk AI berikutnya.
- [`completion-checklist.md`](completion-checklist.md) — daftar kemampuan yang telah tersedia.
- [`current-parity.md`](current-parity.md) — paritas JSP dan ZKoss.
- [`source-manifest.md`](source-manifest.md) — lokasi source utama.
- [`runtime-configuration.md`](runtime-configuration.md) — konfigurasi dan dependensi server.
- [`ui-route-map.md`](ui-route-map.md) — rute UI.
- [`api-map.md`](api-map.md) — endpoint typed.
- [`permission-map.md`](permission-map.md) — autentikasi dan capability.
- [`security-gap.md`](security-gap.md) — catatan keamanan dan integrasi.

## Batas verifikasi

Sesi implementasi tidak menjalankan build WAR atau test lokal sesuai instruksi pemilik sistem. Pemeriksaan yang dilakukan hanya audit source, struktur JSP, pencarian pola bermasalah, dan `git diff --check`. Verifikasi runtime dilakukan setelah build/deployment di server.

