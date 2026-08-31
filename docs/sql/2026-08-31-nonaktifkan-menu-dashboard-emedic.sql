/*
 * NONAKTIFKAN MENU MATI: "Dasbor eMedic" (id 1710004)
 * Tanggal   : 31 Agustus 2026
 *
 * LATAR BELAKANG
 * --------------
 * Audit cakupan menu (docs/native_menu/AUDIT_CAKUPAN_MENU_2026-08-31.md)
 * menemukan bahwa menu ini menunjuk /pages/master/sirs/dashboard_emedic.zul,
 * sedangkan berkas ZUL tersebut TIDAK ADA di repositori — berbeda dengan
 * saudara-saudaranya di direktori yang sama (dashboard_pendaftaran_overview,
 * dashboard_okupansi_tempat_tidur, dashboard_pendapatan, dan lain-lain yang
 * semuanya ada). Akibatnya menu ini selalu gagal dibuka: pada UI lama ZK tidak
 * menemukan halaman, dan pada New UI resolver menjawab
 * NATIVE_ADAPTER_NOT_CONFIGURED.
 *
 * Menu ini adalah SATU-SATUNYA sisa dari 816 route milik role admin yang tidak
 * dapat dipetakan; 815 lainnya sudah punya halaman. Tidak ada adaptor yang
 * dapat dibuat tanpa mengarang layar yang belum pernah ada.
 *
 * KEPUTUSAN
 * ---------
 * Baris TIDAK dihapus, hanya dinonaktifkan (aktif = false), sehingga:
 *   - pengguna tidak lagi menemukan pintu yang selalu gagal;
 *   - jejak menu tetap ada untuk penelusuran;
 *   - mudah dikembalikan bila layar eMedic menyusul dibuat.
 *
 * Menu ini TIDAK terdaftar di ais.common.MenuSnapshotData, sehingga
 * MenuHelper.ensureMenusDariSnapshot() pada startup TIDAK akan membuatnya
 * kembali. Bila kelak dimasukkan ke snapshot, perhatikan bahwa snapshot hanya
 * meng-INSERT baris yang belum ada dan tidak pernah menimpa baris yang sudah
 * ada — jadi status nonaktif di sini tetap terjaga.
 *
 * CARA PAKAI
 * ----------
 * Jalankan bagian A lebih dulu (read-only) untuk memastikan sasarannya benar,
 * baru jalankan bagian B. Bagian C adalah pengembaliannya.
 */

/* ------------------------------------------------------------------ */
/* A. VERIFIKASI (read-only) — pastikan hanya satu baris yang disasar. */
/* ------------------------------------------------------------------ */
SELECT id, root, child, label, url, aktif
FROM public.menu
WHERE id = 1710004
   OR url LIKE '%dashboard_emedic%';

/* Periksa juga apakah masih ada role yang menautkannya; menonaktifkan menu
   tidak menghapus tautan job_has_menu, dan memang tidak perlu. */
/* Nama kolomnya `menu`, bukan `menu_id` — lihat MenuHelper yang memakai
   "DELETE FROM public.job_has_menu WHERE menu IN (...)". */
SELECT COUNT(*) AS jumlah_tautan_role
FROM public.job_has_menu
WHERE menu = 1710004;

/* ------------------------------------------------------------------ */
/* B. TERAPKAN — nonaktifkan menu.                                     */
/*    Syarat url disertakan agar salah id tidak berakibat fatal.        */
/* ------------------------------------------------------------------ */
UPDATE public.menu
SET aktif = false
WHERE id = 1710004
  AND url LIKE '%dashboard_emedic%'
  AND coalesce(aktif, true) = true;

/* Harus melaporkan tepat 1 baris terpengaruh. Bila 0, periksa kembali
   bagian A: kemungkinan menu sudah nonaktif atau id berbeda pada tenant ini. */

/* ------------------------------------------------------------------ */
/* C. PENGEMBALIAN — bila layar eMedic akhirnya dibuat.                 */
/* ------------------------------------------------------------------ */
-- UPDATE public.menu
-- SET aktif = true
-- WHERE id = 1710004
--   AND url LIKE '%dashboard_emedic%';
