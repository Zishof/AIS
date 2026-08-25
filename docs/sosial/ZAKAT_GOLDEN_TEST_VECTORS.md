# Golden Test Vectors Kalkulator Zakat

Engineering fixtures menggunakan policy contoh; nilai nisab/harga/fitrah produksi wajib berasal dari policy APPROVED dan owner syariah.

| Formula | Policy contoh | Input | Expected |
|---|---|---|---|
| GOLD | nisab 85 g, harga 1.000.000, rate 2,5% | 84,999 g | tidak mencapai; 0 |
| GOLD | sama | 85 g | mencapai; 2.125.000 |
| INCOME_ANNUAL | nisab 85×1.000.000, rate 2,5% | gross 100 jt, deduction 10 jt | 2.250.000 |
| INCOME_MONTHLY | nisab annual/12, rate 2,5% | gross 8 jt | 200.000 |
| FITRAH | 45.000/orang | 4 orang | 180.000 |

`CASH_SAVINGS`, `TRADE_BUSINESS`, dan `MAAL_GENERIC` memakai base amount/gross-deduction serta policy nisab/rate yang sama dan wajib ditambahkan oleh reviewer dengan below/at/above threshold. Test juga wajib mencakup zero/negative, upper bound, invalid decimal, rounding modes, effective policy overlap, dan snapshot policy lama.

Executable test: `ais.action.master.sosial.test.ZakatCalculatorGoldenSelfTest`.
