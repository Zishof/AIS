# Code Review Lanjutan Modul Sosial — 29 Agustus 2026

Scope review adalah source utama `src/main/src` dan dokumentasi `src/main/docs/sosial`. Mirror `src/main/java`, build WAR, dan deployment tidak termasuk scope.

## Temuan yang sudah diperbaiki

1. Respons rate limit API dapat tertimpa menjadi HTTP 409. Cabang rate limit sekarang langsung menghasilkan HTTP 429 dan berhenti.
2. Dua permintaan pembayaran paralel dapat membuat lebih dari satu attempt aktif. Transaksi donasi sekarang dikunci sebelum pencarian/pembuatan attempt.
3. Idempotency key donasi sebelumnya dapat mengembalikan transaksi milik pengguna atau payload lain. Owner dan payload finansial/program sekarang harus sama.
4. Hasil create-order Smartlink dapat menimpa callback `PAID`. Update gateway sekarang memakai row lock dan mempertahankan status terminal `PAID`.
5. Refund/reversal yang diposting tidak mengurangi alokasi, sehingga invariant finansial menjadi negatif. Posting koreksi sekarang mengurangi saldo alokasi yang belum disalurkan dan menolak koreksi jika dana sudah tersalurkan.
6. Referensi settlement dapat dipakai ulang dengan payment atau nominal berbeda. Replay sekarang hanya idempoten untuk payment, received amount, dan fee yang sama.
7. Posting penyaluran belum memastikan jenis dana detail sama dengan jenis dana alokasi dan detail yang sama dapat diproses paralel dengan state lama. Pemeriksaan fund sekarang wajib, detail dikunci dan replay `POSTED` idempoten; parsing daftar compatible fund juga mengabaikan spasi secara aman.

## Verifikasi

- Targeted compilation JDK 8: PASS.
- `SocialFinancialInvariantSelfTest`: PASS.
- `SocialSmartlinkSecuritySelfTest`: PASS.
- `ZakatCalculatorGoldenSelfTest`: PASS.
- Build WAR dan deploy: tidak dijalankan sesuai permintaan.

## Batas yang masih memerlukan deployment atau keputusan eksternal

- Pembuatan dan audit schema PostgreSQL/Envers serta eksekusi index.
- Konfirmasi endpoint, payload, signature, retry, timeout, inquiry, refund, dan settlement berdasarkan kontrak Smartlink resmi.
- Uji runtime ZKoss/JSP/API, menu/RBAC, isolasi tenant, callback duplikat/paralel, serta browser UAT.
- Mapping jurnal akuntansi dan persetujuan Finance; adapter tetap fail-closed.
- Worker notifikasi dan registrasi pengguna umum tetap di luar scope V1 yang disepakati.

Tidak ada source-level defect kritis lain yang diketahui setelah review ini, tetapi status production-ready belum dapat diberikan sebelum seluruh evidence runtime di atas tersedia.
