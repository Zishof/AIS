# Privacy dan Retention Policy

Status: baseline engineering; legal/privacy owner harus menetapkan angka retensi final.

- Anonim publik hanya menyembunyikan identitas pada output publik; identitas internal tetap tersedia bagi role berwenang untuk audit/legal.
- Kontak donor, credential, raw callback, session, dan beneficiary PII tidak boleh masuk log/public export.
- Callback yang disimpan hanya payload teredaksi. Receipt publik memakai opaque 256-bit token.
- Doa/pesan selalu `PENDING` dan tidak dipublikasikan tanpa moderasi.
- Consent komunikasi terpisah dari consent transaksi; default false untuk legacy migration.
- Permintaan export/delete harus mematuhi legal hold transaksi dan audit. Data finansial tidak dihapus keras; identitas dapat dipseudonimkan setelah keputusan legal.
- Retensi yang harus diisi owner: donor profile, transaction, receipt, callback redacted, reconciliation, prayer, logs, backups, dan audit.
