# 13 — Rilis v1.33.68 dan catatan operasional working copy

Dua hal yang bukan fitur, tetapi menentukan apakah pekerjaan di dokumen lain benar-benar
sampai ke pengguna dan bisa ditelusuri.

---

## 1. Rilis v1.33.68 — tujuh varian sekaligus

| | |
|---|---|
| Repositori | `C:\opt\CodeBaseDesktopDanMobile` (GitHub `Zishof/zishof-platform`) |
| Skrip | `apps/ebisnis/tool/build_semua_varian.ps1` |
| Rilis | <https://github.com/Zishof/zishof-platform/releases/tag/v1.33.68> |
| Commit | `eb6636d` (versi + skrip), `fb3dfb5` (perbaikan sidik jari) |

**Kenapa satu skrip.** Sebelumnya tiap varian punya skrip sendiri, sehingga rilis "semua
varian" mudah timpang — satu varian terbangun dari commit lain daripada varian berikutnya.
Skrip ini membangun seluruh varian **berurutan dari pohon kerja yang sama**, memakai versi yang
dibaca sekali dari `pubspec.yaml`.

| Varian | Windows | Android |
|---|---|---|
| eBisnis | `eBisnis-Setup-1.33.68.exe` | `app-ebisnis-release.apk` |
| Al-Bahjah | `Al-Bahjah-POS-Setup-1.33.68.exe` | `app-albahjah-release.apk` |
| Inventory & Sales | `eBisnis-Inventory-Sales-Setup-1.33.68.exe` | `app-inventorysales-release.apk` |
| Apotik | `eBisnis-POS-Apotik-Setup-1.33.68.exe` | `app-apotik-release.apk` |
| eMedik | `eBisnis-POS-eMedik-Setup-1.33.68.exe` | `app-emedik-release.apk` |
| eKantin Petra | `eKantin-Petra-Setup-1.33.68.exe` | `app-petra-release.apk` |
| MitraInap | — (memang tanpa installer) | `app-mitrainap-release.apk` |

13 artefak + 13 berkas `.sha256.txt` = 26 aset terunggah.

**Cacat yang ditemukan saat menjalankannya**: skrip berhenti tepat setelah seluruh varian
selesai dibangun, karena `Get-FileHash` tidak tersedia di sesi PowerShell yang menjalankannya —
artefaknya lengkap tetapi tanpa berkas sidik jari dan tanpa ringkasan. Diperbaiki dengan jalur
cadangan `certutil` (`fb3dfb5`); hasil keduanya diverifikasi cocok.

**Sisi server**: rilis itu meminta AIS revisi r77828 atau lebih baru. Kolom dan tabel baru
dibuat otomatis oleh Hibernate — tidak ada ALTER manual (basis data tetap perlu dicadangkan
sebelum boot pertama).

---

## 2. Working copy SVN ini disapu alat otomatis

**Gejala**: pada rentang r77980–r78013, **29 dari 33 revisi berpesan KOSONG**, masing-masing
memborong berkas lintas modul milik beberapa sesi kerja sekaligus. Kadensinya 1–7 menit.
Akibatnya alasan tiap perubahan hilang, dan pekerjaan yang belum selesai ikut terkirim.

**Yang ditemukan dan dihapus** (r78015): berkas `commit.sh` di akar working copy — satu baris
`svn commit` dengan pesan kosong atas wildcard seluruh isi direktori, dan **kata sandi SVN
tertulis di dalamnya**. Berkas itu versioned sejak r73553 (14 Juni 2026) pada dua path repo
(`^/src/commit.sh` dan `^/web/commit.sh`) dan muncul di lima lokasi checkout. Seluruh salinan
lokal di `C:\opt` (31 berkas) juga sudah dibersihkan atas instruksi pengguna.

**Tetapi bukan itu mekanismenya.** Commit berpesan kosong **berlanjut** sesudah penghapusan
(r78016 s.d. r78022, terakhir 06:14). Bukti yang menutup kemungkinan `commit.sh`: beberapa
revisi kosong memuat berkas di `^/src` **dan** `^/web` dalam satu revisi, padahal keduanya
adalah working copy terpisah — `svn commit` di satu direktori tidak bisa menghasilkan itu.

Yang sudah diperiksa dan nihil: scheduled task, folder Startup, registry Run (HKCU+HKLM),
proses IDE/klien SVN, proses ber-`CommandLine` `svn`, hook `svn` pada settings Claude, skrip
yang menyentuh kedua working copy, dan (oleh sesi lain) berkas state Codex CLI yang tidak
berubah pada jendela waktu itu. Audit pembuatan proses Windows (event 4688) tidak aktif,
sehingga pelakunya tidak dapat dilacak surut.

### Konsekuensi untuk cara kerja

1. **Tulis alasan di kode** (JavaDoc/komentar), bukan hanya di pesan commit — pesan itu sering
   tidak sempat terbentuk. Seluruh dokumen di folder ini juga berfungsi sebagai penggantinya.
2. **Verifikasi hasil dengan `svn cat -r HEAD`**, bukan `svn status` — working copy sering
   sudah bersih sebelum sempat commit.
3. **Commit selalu dengan menyebut berkas eksplisit** dan pesan; commit bermakna masih bisa
   mendarat (r78015 buktinya).

### Yang masih terbuka

- **Kata sandi SVN perlu diganti.** Menghapus berkas tidak mencabut nilainya dari riwayat
  repositori; nilainya ada di sana sejak Juni. Ini keputusan dan tindakan pengguna.
- Pelaku commit kosong belum teridentifikasi. Langkah yang paling menentukan: mengaktifkan
  audit pembuatan proses Windows (mengubah setelan keamanan mesin — keputusan pengguna), atau
  memantau proses secara baca-saja saat sapuan berikutnya terjadi.
