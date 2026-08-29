# Test Evidence Modul Sosial

## Evidence source-level 25 Agustus 2026

| Test | Status | Catatan |
|---|---|---|
| Financial invariant pure test | PASS | `SocialFinancialInvariantSelfTest OK` |
| Smartlink HMAC test | PASS | RFC HMAC vector, `SocialSmartlinkSecuritySelfTest OK` |
| Zakat golden test | PASS | `ZakatCalculatorGoldenSelfTest OK` |
| Java targeted compile | PASS | JDK 1.8.0_502, seluruh Java yang diubah; tanpa package WAR |
| SQL 002–004 read-only | PASS_EXPECTED_MISSING | PostgreSQL 16.4; melaporkan 18 tabel basis dan 18 audit belum terbentuk |
| SQL 005–006 | NOT_RUN | memerlukan tabel baru setelah deployment |
| Runtime JSP/ZK/API/receipt | NOT_RUN_BY_REQUEST | dilakukan setelah deploy user |
| Smartlink sandbox | BLOCKED_EXTERNAL | credential/contract provider diperlukan |
| Security UAT | RUNTIME_PENDING | checklist tersedia |

Setiap evidence lanjutan harus mencatat revision, environment, timestamp, pelaksana, command/scenario, expected, actual, status, log/screenshot teredaksi, defect reference, dan checksum artifact.

Tidak ada WAR yang dibangun dan tidak ada deployment sesuai permintaan pengguna.

## Evidence source-level 29 Agustus 2026

| Test | Status | Catatan |
|---|---|---|
| Java targeted compile | PASS | JDK 8; service donasi/payment/correction/reconciliation/distribution, servlet API, dan self-test; `-implicit:none`; tanpa WAR |
| Financial invariant self-test | PASS | Termasuk skenario refund 100 - 20, alokasi 80, distribusi 30 |
| Smartlink security self-test | PASS | `SocialSmartlinkSecuritySelfTest OK` |
| Zakat golden self-test | PASS | `ZakatCalculatorGoldenSelfTest OK` |
| Refund vs allocation review | PASS_SOURCE | Row lock, pengurangan saldo belum disalurkan, dan penolakan saldo tidak cukup diterapkan |
| Reconciliation idempotency review | PASS_SOURCE | Referensi settlement dengan payment/received/fee berbeda ditolak |
| Distribution source-fund review | PASS_SOURCE | Tenant, status, jenis dana sumber, saldo, dan CSV restricted-fund divalidasi |
| Runtime/database/provider test | NOT_RUN_BY_REQUEST | Menunggu build/deploy oleh pemilik sistem dan credential/kontrak Smartlink |

Working copy source pada saat evidence berada di baseline SVN r78523 dengan perubahan sosial belum di-commit. File non-sosial milik pengguna tidak disentuh.
