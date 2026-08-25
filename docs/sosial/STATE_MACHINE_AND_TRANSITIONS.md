# State Machine dan Transisi

Rules fail-closed berada di `SocialStateMachine`.

## Transaksi

| Dari | Ke | Actor/guard |
|---|---|---|
| DRAFT | PENDING_PAYMENT | owner + collection/compliance gate |
| DRAFT | CANCELLED | owner/operator |
| PENDING_PAYMENT | ALLOCATED | callback Smartlink tervalidasi |
| PENDING_PAYMENT | CANCELLED/EXPIRED | payment workflow |
| ALLOCATED | PARTIALLY_DISTRIBUTED/DISTRIBUTED/COMPLETED | finance workflow |
| PARTIALLY_DISTRIBUTED | DISTRIBUTED/COMPLETED | finance workflow |

Transaksi non-DRAFT immutable pada CRUD umum.

## Payment attempt

`CREATED -> VA_ISSUED/PENDING/PAID/MISMATCH/FAILED`; `VA_ISSUED|PENDING -> PAID/FAILED/EXPIRED/MISMATCH`; `EXPIRED -> PAID` diizinkan untuk callback sah yang datang terlambat. `PAID` terminal dan callback berikutnya hanya idempoten/reconciliation exception.

## Allocation/distribution/correction

- Allocation dibuat `PLANNED`, menjadi `POSTED` hanya oleh callback payment sukses.
- Distribution hanya `APPROVED -> POSTED`, dengan row lock dan saldo/restricted-fund check.
- Correction `REQUESTED -> POSTED` dengan maker-checker; pembuat tidak boleh menyetujui sendiri.
- Receipt dibuat hanya sesudah payment sukses tervalidasi.
