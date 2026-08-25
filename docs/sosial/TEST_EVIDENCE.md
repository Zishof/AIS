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
