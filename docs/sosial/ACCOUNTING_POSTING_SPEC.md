# Spesifikasi Posting Akuntansi

Status resmi: `STUB_NOOP + DISABLED_BY_DEFAULT + OUT_OF_SCOPE_V1_PILOT`.

`SocialAccountingAdapter` sekarang selalu fail-closed meskipun flag salah diaktifkan. Tidak tersedia journal header/detail, debit/kredit, balancing, idempotency posting, settlement, distribution, refund/reversal, correction, period lock, atau general-ledger reconciliation.

Implementasi fase berikutnya wajib menentukan mapping akun per tenant/fund/channel/fee; balanced entries; immutable posting key; payment/settlement/distribution/refund/reversal/correction journal; retry dan reversal; approval; period lock; serta reconciliation ke buku besar. Flag tidak boleh aktif sebelum finance UAT dan sign-off.
