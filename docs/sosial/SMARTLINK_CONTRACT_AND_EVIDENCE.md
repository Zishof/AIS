# Smartlink Contract dan Evidence

## Status

`SOURCE_IMPLEMENTED + BLOCKED_EXTERNAL_CONTRACT`. Dokumen ini mencatat contract implementasi; bagian provider harus dikonfirmasi pada sandbox sebelum flag aktif.

## Create order implementasi

- Transport melalui `VirtualAccountBank.curlSmartlink(url, username, password, json)`.
- JSON: `order_id`, integer-string `amount`, `description`, `customer`, `item`, `channel`, `type=payment-page`, `payment_mode=CLOSE`, expiry, callback dan redirect URL.
- Response sementara dianggap sukses bila `code="0"`; `data.payment_url` dan `data.reference` disimpan.
- Credential diambil dari SosialChannel yang disnapshot pada transaksi/payment; perubahan credential ditolak setelah channel mempunyai payment.

## Callback implementasi

- Header: `X-Smartlink-Signature`.
- Signature: lowercase hex HMAC-SHA256(secret channel, raw request body).
- Source: `request.remoteAddr` harus sama dengan salah satu IP eksplisit channel. `X-Forwarded-For` tidak dipercaya.
- Payload: `data.order_id`, `amount`, `currency`, `status`, transaction/reference ID.
- Sukses hanya status case-insensitive `success`; amount dan currency harus tepat.
- Payment dan allocation memakai row lock/idempotency; mismatch masuk reconciliation exception.

## Konfirmasi provider yang masih wajib

Canonical string/body, encoding, header name, timestamp/replay window, trusted proxy topology, response code/body, retry/backoff, timeout, expiry/inquiry, settlement, partial payment, out-of-order event, refund/reversal, secret rotation grace period, dan daftar IP resmi.

Evidence sandbox harus mencakup sukses, gagal, timeout, duplicate identik/berbeda, paralel, invalid HMAC/IP/amount/currency, unknown order, expired-then-paid, secret lintas channel, serta regresi `VirtualAccountBank` r78252. Secret dan PII wajib dihapus dari fixture.
