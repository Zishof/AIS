# Handover Integrasi eBisnis ke AIS `/ebisnis/*`

Tanggal: **15 Agustus 2026 (Asia/Jakarta)**

Branch kerja: `codex/ebisnis-servlet-integration`

Baseline: `origin/feat/new-ui-rbac-role-user` commit `84168280`

## Tujuan fase ini

Menempatkan pengalaman eBisnis di namespace stabil `/ebisnis/*` tanpa menyalin
ulang logika transaksi yang sudah ada di AIS. Front controller hanya melakukan
canonicalization, keamanan, autentikasi dan dispatch. Operasi bisnis tetap
memakai helper/API AIS yang sama dengan UI lama dan klien POS.

Worktree lama `C:\opt\AIS\ais\src\main` tidak disentuh karena berisi puluhan
ribu perubahan lokal/untracked. Implementasi dikerjakan pada worktree bersih
`C:\opt\AIS\ebisnis-servlet-integration`.

## Yang telah diimplementasikan

1. `EBisnisFrontController` dan mapping servlet `/ebisnis/*`.
2. Registry route statis; path request tidak pernah diubah langsung menjadi
   nama JSP/class.
3. Penolakan traversal, encoded separator, backslash, double slash dan path
   yang tidak terdaftar.
4. Header `X-Request-ID`, `nosniff`, `SAMEORIGIN`, Referrer Policy dan
   `no-store` untuk area terautentikasi.
5. Gerbang sesi AIS untuk route tenant/platform; request JSON mendapat `401`
   JSON dan browser diarahkan ke login.
6. CSRF session-bound untuk login, logout dan command dashboard pendaftar.
7. API Inventory JSP dipindahkan dari URL lama `/Api_eBisnis` ke
   `/ebisnis/api/v1`; dispatcher/action lama tetap menjadi satu sumber aturan.
8. Seluruh 48 layar Inventory dapat dipanggil dengan nomor maupun slug:
   `/ebisnis/inventory/01`, `/ebisnis/inventory/screen/01`, dan
   `/ebisnis/inventory/data_supplier`.
9. Dokumen komersial memakai servlet nyata `presentasi`, `proposal`, `pks` dan
   `penawaran`, bukan placeholder.
10. Route POS, Apotik, eMedik, koperasi, storefront dan aplikasi memakai JSP/
    servlet AIS existing dengan guard sesi/RBAC existing.
11. Layar 20 sekarang mempunyai tombol **Pembelian Baru** dan form faktur
    supplier multi-item. Faktur, dua tingkat diskon item, harga beli neto,
    penerimaan stok, batch/expiry, jenis
    `CASH/DP/CREDIT`, DP, termin dan baris AP disimpan dalam satu transaksi.
12. Sales Order baru otomatis berstatus `PESAN` (disetujui). Draft hanya dibuat
    bila klien mengirim `simpan_draft=true`. Daftar/riwayat, detail, perubahan
    status dan pembatalan tetap memakai endpoint server existing.

## Matriks route aktif

| Namespace baru | Target AIS aktual | Akses |
| --- | --- | --- |
| `/ebisnis/` | landing `WEB-INF/baru/ebisnis.jsp` | publik |
| `/ebisnis/daftar`, `/auth/daftar` | `PendaftaranTenantServlet` | publik |
| `/ebisnis/masuk`, `/auth/login` | `EbisnisPublicServlet` | publik + CSRF POST |
| `/ebisnis/dashboard`, `/auth/session` | dashboard pendaftar | sesi pendaftar |
| `/ebisnis/app`, `/app/dashboard` | `/main` | sesi AIS |
| `/ebisnis/inventory/*` | 48 JSP + `SalesInventoryApiDispatcher` | sesi AIS + RBAC |
| `/ebisnis/api/v1/*` | `ApiEBisnis`/`PosApi` | token API existing |
| `/ebisnis/pos/*`, `/ekoperasi/*` | modul Kantin/Koperasi JSP | sesi AIS + RBAC |
| `/ebisnis/apotik/*` | modul Apotik JSP | sesi AIS + RBAC |
| `/ebisnis/emedik/*` | modul eMedik JSP | sesi AIS + RBAC |
| `/ebisnis/belanja` | servlet `/kantin` | publik |
| `/ebisnis/dokumen/*` | servlet dokumen komersial | publik |
| `/ebisnis/platform` | servlet `/new` | sesi AIS |

## Verifikasi yang sudah dijalankan

- kompilasi Java 7 target pada JDK 8 terhadap 184 library AIS: **PASS**;
- `EBisnisRouteRegistrySelfTest`: **PASS**;
- `EBisnisFrontControllerSelfTest`: **PASS**;
- tepat 48 pasangan nomor/slug dan seluruh wrapper JSP ada: **PASS**;
- `node --check inventory.js`: **PASS**;
- parse `web.xml` dan Spring Security XML: **PASS**;
- `git diff --check`: **PASS**.

Evidence UAT render 48 layar sebelum namespace baru tetap berada di
`docs/pos-inventory-sales/evidence/uat-jsp-48-2026-08-13/`. Evidence tersebut
tidak diubah karena merupakan rekaman baseline historis.

## Yang belum boleh diklaim selesai

Fase ini adalah integrasi namespace dan vertical slice Inventory/Sales, bukan
big-bang port seluruh React/NestJS eBisnis. Berikut tetap membutuhkan fase dan
UAT terpisah:

- CMS berita/detail berita, halaman legal penuh, demo dan reset password;
- portal tenant umum di luar modul AIS yang memang sudah tersedia;
- pemetaan platform-super-admin per fungsi, bukan hanya shell `/new`;
- kontrak REST path-native; `/api/v1/*` saat ini adapter kompatibilitas ke
  dispatcher JSON-action AIS;
- tenant/schema bridge eBisnis lama bila database schema-per-tenant lama tetap
  dipakai;
- rekonsiliasi data produksi, browser UAT di Tomcat, database UAT dan smoke WAR;
- build/release Flutter Windows dan Android (tidak berada di repository AIS);
- evidence Web/Windows/Android/offline/print/reconciliation/UAT per layar yang
  harus ditandatangani setelah deploy.

Jangan menghapus endpoint lama atau backend eBisnis sebelum seluruh konsumen
Flutter, callback, worker dan tenant melewati compatibility test.

## Langkah deploy/UAT berikutnya

1. Merge branch ini ke branch deploy AIS setelah review.
2. Kompilasi seluruh `src` ke `WEB-INF/classes` menggunakan JDK 8 dan library
   `WEB-INF/lib`; jangan memakai artefak class lama sebagai hasil akhir.
3. Deploy ke Tomcat dev dan restart terkontrol.
4. Uji landing/login/daftar, 401 JSON, 403 RBAC dan route traversal.
5. Uji `/ebisnis/inventory/01` sampai `48` dengan Pemilik dan Sales.
6. Uji Pembelian Baru CASH, DP dan CREDIT; pastikan stok/batch/AP sama-sama
   commit atau sama-sama rollback.
7. Uji Order Baru otomatis `PESAN`, daftar order, detail dan pembatalan dengan
   alasan audit.
8. Rekonsiliasi saldo stok, hutang, piutang, kas dan laba-rugi terhadap baseline.
