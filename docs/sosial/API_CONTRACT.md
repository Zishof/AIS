# Kontrak API Sosial

Base endpoint: `/sosial-api`. Response JSON selalu memiliki `ok`; error menggunakan `code` dan `message`. Mutasi memerlukan POST, `X-CSRF-Token`/`csrf`, rate limit, dan `Cache-Control: no-store`.

| Action | Method | Auth | Input utama | Idempotency/result |
|---|---|---|---|---|
| `calculate` | POST | sesuai policy tenant | `jenisZakatId`, formula inputs | snapshot `calculationId`, policy version |
| `donation` | POST | member AIS | fund/program/calculation, amount, contribution, identity display, `idempotencyKey` | unique tenant+key, transaction number |
| `payment` | POST | owner | transaction number, gateway=`smartlink` | active attempt reused |
| `payment-status` | GET | owner | transaction number | status/payment URL/time only |

Status: 403 forbidden, 409 not ready/state conflict, 422 validation, 429 rate limit, 500 internal dengan request ID. Browser tidak dapat mengirim tenant, status sukses, total final, credential, atau accounting state.

Callback `/SosialSmartlinkCallback` hanya POST, maksimum 256 KiB, rate limited, IP allow-list dan HMAC per SosialChannel. Receipt `/sosial-receipt-pdf?token=<64 hex>` hanya GET dan tidak memuat kontak donor.
