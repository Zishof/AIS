# Financial Invariants Modul Sosial

Status: implemented in `SocialFinancialInvariantService`; persetujuan Finance masih diperlukan sebelum produksi.

## Definisi

Semua nilai IDR memakai `BigDecimal`, scale 2, `HALF_UP`. Fee gateway dan kontribusi layanan bukan dana sosial.

```text
total_charged = gross_donation + voluntary_platform_contribution + gateway_fee
settled_donation = sum(payment.request_amount where payment_status=PAID)
returned_donation = sum(correction.amount where status=POSTED and type in REFUND,REVERSAL)
net_settled = settled_donation - returned_donation
unallocated_available = net_settled - posted_allocation
allocation_available = posted_allocation - posted_distribution
```

Invariant gagal bila returned > settled, allocation > net settled, distribution > allocation, nominal negatif, redirect browser mengubah status menjadi PAID, callback duplikat memposting dua kali, atau receipt terbit sebelum callback sukses tervalidasi.

Dashboard sekarang memakai formula tersebut. Nilai negatif tidak ditutupi menjadi nol; `financialException=true` ditampilkan agar selisih dapat direkonsiliasi. Jalankan `sql/005_verify_financial_invariants.sql` setelah schema tersedia.

Refund/reversal memakai maker-checker `SocialCorrectionService`: role FINANCE meminta, role APPROVE yang berbeda memposting. Saat koreksi diposting, nominal alokasi `POSTED` dikurangi secara deterministik hanya dari bagian yang belum disalurkan; alokasi menjadi `REVERSED` bila saldonya menjadi nol. Koreksi ditolak bila dana sudah disalurkan atau saldo alokasi tidak cukup. Dengan demikian `posted_allocation` tetap sama dengan `net_settled` setelah koreksi. Eksekusi refund ke provider masih bagian kontrak eksternal dan tidak boleh diasumsikan selesai.
