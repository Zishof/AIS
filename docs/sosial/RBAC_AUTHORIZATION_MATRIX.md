# Matriks RBAC dan Otorisasi

Capability dikonfigurasi melalui `sosial_roles_view|operate|approve|finance|audit|admin`; admin platform tetap melalui pemeriksaan admin AIS. Menu visibility bukan authorization boundary.

| Aksi | Capability | Tenant | Ownership | State/guard |
|---|---|---|---|---|
| Lihat portal publik | anonymous | host-derived | N/A | flag portal/transparency |
| Riwayat/status payment | member | wajib | donor identity | no-store |
| Buat donasi/payment | member | wajib | current user | CSRF, rate limit, idempotency |
| Dashboard sosial | VIEW | wajib | N/A | server-side guard |
| CRUD transaksi manual | OPERATE + menu privilege | wajib | N/A | DRAFT only |
| Kelola SosialChannel | ADMIN + menu privilege | wajib | N/A | credential immutable setelah dipakai |
| Posting penyaluran | FINANCE | wajib | N/A | APPROVED, row lock, balance |
| Input settlement | FINANCE | wajib | N/A | idempotent settlement reference |
| Minta refund/reversal | FINANCE | wajib | N/A | paid transaction, capped amount |
| Setujui refund/reversal | APPROVE | wajib | maker != checker | REQUESTED only |
| Legacy backfill | ADMIN | explicit tenant | N/A | preview + execute=true |
| Audit/read evidence | AUDIT | wajib | N/A | read-only |

Direct endpoint/service denial, cross-tenant ID, cross-owner ID, CSRF, dan role kosong wajib diuji; lihat `UAT_SECURITY_CHECKLIST.md`.
