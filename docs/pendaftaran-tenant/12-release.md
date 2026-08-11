# 12 — Release Checklist

1. **Deploy**: merge/pull `feat/new-ui-rbac-role-user` di server dev → build → restart Tomcat
   (operator). Startup: hbm2ddl membuat 16 tabel + audit; seed jenis usaha; worker hidup.
2. **Konfigurasi** (opsional, default aman): `pendaftaran_tenant_mode`=LEGACY,
   `pendaftaran_base_url`=URL publik (utk tautan email), `aktfikan_pengiriman_email` + SMTP
   (`default_mailhost` dst.), `pendaftaran_terms_version`/`privacy_version` sesuai dokumen legal
   terpublikasi, `pendaftaran_paket_json` harga terkini.
3. **Smoke test Tomcat**: GET `/pendaftaran` 200; GET `/` landing tetap; login ebisnis lama tetap;
   PosApi/Api_eBisnis tetap; ZK login staf tetap.
4. **UAT**: jalankan 11-uat.md (UAT-1..7) + `VerifikasiKonkurensiPendaftaran`. Simpan evidence.
5. **Backfill** (§16.4 / 09-migration.md): jalankan aturan backfill yang disetujui + terbitkan
   `migration-exceptions.csv`.
6. **Go-live bertahap**: LEGACY → (setelah stabil & keputusan produk) HYBRID utk tenant baru.
7. **Rollback**: `git revert` commit program ini (additive); tabel telanjur dibuat dibiarkan.

Status DoD saat rilis dokumen ini: implementasi+compile+unit test DONE; UAT/Tomcat smoke =
**UAT_REQUIRED** (butuh deploy — di luar wewenang workspace build).
