# Observability dan Incident Runbook

Gunakan correlation ID tanpa PII untuk API, payment, callback, receipt, dan reconciliation. Jangan log secret/raw contact.

Metric minimum: create-order success/error/latency; callback valid/invalid/mismatch/duplicate; pending dan expired payment; reconciliation exception age; receipt failure; distribution failure; financial invariant exception; tenant routing error.

Alert P0: invariant negatif, duplicate posting, callback signature bypass, cross-tenant access, atau settlement mismatch material. Tindakan awal: matikan `sosial_public_collection_enabled` dan `sosial_smartlink_enabled`, pertahankan data, catat order in-flight, rekonsiliasi gateway/bank, jangan edit paid record dengan SQL.

Alert P1: callback backlog, pending melewati SLA, receipt failure, reconciliation exception. Owner/SLA/escalation masih harus diisi pada RACI.

DR wajib menguji restore database, konfigurasi/secret version, receipt metadata, dan reconciliation state; restart/retry tidak boleh membuat double posting. RPO/RTO harus disetujui sebelum produksi.
