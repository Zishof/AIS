# 11 — Skenario UAT (dijalankan operator pada server dev ter-deploy)

Prasyarat: deploy commit terakhir → restart Tomcat (wewenang operator) → cek startup log bersih
(16 tabel dibuat hbm2ddl; seed 14 jenis usaha; worker "tenant-provisioning" hidup). Konfigurasi
`aktfikan_pengiriman_email` aktif bila ingin uji email nyata; bila nonaktif gunakan tombol
"Verifikasi Manual" backoffice.

## UAT-1: E2E Apotek + Inventory/Sales (§21.6)
1. Buka `<ctx>/pendaftaran` TANPA login → wizard tampil, katalog 14 jenis dari DB.
2. Isi Langkah 1-3; Langkah 4 pilih **Apotek + Inventory / Sales**; Langkah 5 username baru
   (lihat status "Tersedia" real-time + preview subdomain); Langkah 6 pilih paket; Langkah 7 isi
   email+password (min 10)+centang 2 consent; Langkah 8 tinjau → Kirim (tombol disabled saat proses).
3. Halaman status: nomor REG-..., status EMAIL_VERIFICATION_PENDING, email ter-mask.
4. Klik tautan verifikasi di email (atau admin_verify_manual) → status PROVISIONING → (≤2 menit,
   tick worker) → READY.
5. Acceptance SQL ERD §8: 1 pendaftar (aktif=true setelah READY), 1 profile, 1 permohonan,
   2 baris jenis usaha, 1 reservation CONSUMED, 1 job SUCCESS + step (schema SKIPPED sah di mode
   LEGACY), 1 registry READY + trial 30 hari dari ready_at, 1 membership owner, entitlement union
   (POS..NOTA_SALES + FARMASI...) tanpa duplikat.
6. Login dari ebisnis.jsp → dashboard: panel tenant tampil (status ACTIVE setelah login, modul aktif
   + "belum tersedia"), buat Brand → Toko → Mesin POS (kredensial tampil sekali) → sukses.
7. Logout/login ulang → data tetap; tenant lain tidak terlihat (coba kode REG milik akun lain).

## UAT-2: E2E Bengkel Mobil
Sama seperti UAT-1 dgn jenis **Bengkel Mobil** saja → entitlement WORK_ORDER/KENDARAAN/... muncul
berstatus **PLANNED/belum tersedia** di panel tenant (JUJUR, tanpa tombol) — POS/INVENTORY aktif.

## UAT-3: Gating pra-READY
Submit baru TANPA verifikasi → login GAGAL ("akun tidak aktif"). Verifikasi tapi matikan worker
(`pendaftaran_provisioning_interval_detik` besar) → login masih gagal (aktif=false) → tidak mungkin
membuat Brand/Toko/POS sebelum READY. Akun ebisnis LAMA (pra-program) → dashboard tetap berfungsi
penuh (fail-open legacy).

## UAT-4: Duplikat & race
- Daftar dgn email yang sudah punya akun → "Email telah memiliki akun. Silakan masuk...".
- Username yang sudah dipakai → check_username "Tidak tersedia" + submit USERNAME_NOT_AVAILABLE.
- `VerifikasiKonkurensiPendaftaran <baseUrl> 50` → [LULUS] dua skenario.

## UAT-5: Backoffice admin
Login admin platform (root/role "am") → `<ctx>/pendaftaran?mode=admin`: daftar+filter; Setujui/Tolak
(REVIEW_PENDING — daftar dgn jenis SEKOLAH utk memicunya), Verifikasi Manual, Retry (padamkan DB
sesaat utk memicu FAILED), Lepas Username (hanya utk REJECTED/CANCELLED tanpa tenant), panel Step.
Non-admin akses URL sama → dilempar ke landing; POST admin_* → ADMIN_ONLY.

## UAT-6: Bridge kompatibilitas
POST lama `aksi=daftar` (modal lama/bookmark) → JSON redirect ke wizard; `aksi=login` lama tetap
berfungsi; CTA "Daftar Sekarang" landing → wizard.

## UAT-7: Anti-automation
Submit <5 detik setelah buka form / honeypot terisi → REQUEST_REJECTED generik; submit ke-11
dalam 1 jam dari IP sama → RATE_LIMITED; resend >3×/jam per kode → RATE_LIMITED.

Hasil tiap butir dicatat di `evidence/uat/` (screenshot + SQL output) sebelum status DoD → UAT_PASSED.
