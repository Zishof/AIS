# Audit Silang Dok 58–59: Empat Cacat Ditemukan & Diperbaiki

Tanggal: 29 Agustus 2026. Kode masuk SVN **r78574** (`PostingPenjualanKantinAction`,
terbawa commit sapu sesi paralel, diverifikasi byte-identik) dan **r78575**
(`PostingHppKantinAction`). Mirror `java/` selaras (r78575).

## 1. Metode

Audit independen atas [58-posting-saldo-awal-kas-kecil.md](58-posting-saldo-awal-kas-kecil.md)
dan [59-posting-kantin-dasbor.md](59-posting-kantin-dasbor.md): tinjau kode di HEAD +
harness `TesAudit5859` (scratchpad) yang SENGAJA menguji apa yang tidak diuji harness
aslinya — dok 58: cabang pembalik (saldo awal negatif); dok 59: **posting sukses
ujung-ke-ujung** HPP dan Penjualan lewat `prosesApi` (harness asli hanya menguji
penolakan dan pembatalan atas riwayat buatan tanpa baris ber-cap), termasuk siklus
posting → batal → posting ulang atas batch NYATA. Fixture `UATA58-` (Juli 2091) dan
`UATA59-` (Agustus 2091). Jalur headless menyuntik `RequestContext` dengan proxy
`HttpServletRequest` supaya `Common.getCurrentUser()` menemukan pengguna uji.

## 2. Hasil dok 58 (Saldo Awal Kas Kecil): BERSIH

Semua klaim terverifikasi ulang dengan fixture segar: kedua cabang kredit
(transfer → akun cara bayar; transitori → akun transitori), hitung dasbor, idempoten,
batal. Cabang PEMBALIK yang belum pernah diuji (saldo −400k) benar: Dr akun cara /
Cr akun jenis, 400k. Tidak ada perubahan kode.

## 3. Hasil dok 59 (keluarga kantin): EMPAT CACAT — semuanya kini diperbaiki

1. **Pembatalan HPP atas batch nyata selalu gagal diam-diam.**
   `koperasi.pembelian.posting_hpp` ber-FK ke `posting_history`; `batalkanPeriode`
   menghapus riwayat tanpa melepas cap → pelanggaran FK → transaksi batch rollback,
   `n=0`, error hanya ke error-log. Lolos dari harness dok 59 karena fixture batalnya
   tidak punya baris pembelian ber-cap. Fix: `UPDATE ... SET posting_hpp = NULL`
   sebelum `DELETE posting_history` (cermin pelepasan header Penjualan).
2. **Label kolom `coalesce` ganda merusak angka lewat Hibernate.**
   `SQLQuery.list()` membaca kolom per LABEL; dua+ ekspresi `COALESCE(...)` tanpa
   alias sama-sama berlabel `coalesce` dan SEMUANYA mengembalikan nilai kolom pertama.
   Akibat nyata: qty HPP tertukar menjadi hargabeli → **nilai jurnal HPP = hargabeli²**
   (di UAT: 3 × 20.000 terjurnal 400 juta); rata-rata kulakan selalu 1; pajak Penjualan
   tertukar total_biaya → transaksi non-PPN ditolak "belum ada Akun PPN Keluaran".
   JDBC murni membaca posisional sehingga verifikasi SQL manual TIDAK memergokinya.
   Fix: alias unik pada semua kolom terhitung di query pratinjau HPP, rata-rata
   kulakan, dan Q1/Q2 Penjualan.
3. **Posting Penjualan selalu `SQLGrammarException`**: Q1 menulis `pb0.cara_bayar`,
   kolomnya `carabayar` — persis jebakan penamaan yang diperingatkan dok 58 §4 sendiri.
   Fix: nama kolom dibetulkan.
4. **`prosesApi` HPP tidak mencap `posting_hpp`** (pengecap hanya ada di jalur ZK
   lama) — penghitung draf dasbor tidak pernah turun dan filter anti-dobel
   `posting_hpp IS NULL` kehilangan lapisannya. Fix: pengecap ditambahkan di transaksi
   `prosesApi`, predikatnya sama dengan query pratinjau.

Desain yang DIKONFIRMASI benar oleh audit: maju-saja/mundur-saja bekerja; pembatalan
melepas cap header Penjualan; setelah perbaikan, siklus posting → batal → posting ulang
pulih penuh.

## 4. Pengujian

`TesAudit5859`: **LULUS 20, GAGAL 0** — 8 skenario kas kecil (termasuk pembalik) + 12
skenario kantin (HPP total 60k dengan akun benar, cap terpasang, tolak tumpang tindih,
batal batch nyata n=1, cap terlepas, posting ulang pulih; Penjualan Dr kas 250k /
Cr pendapatan 250k, header tercap dan terlepas saat batal).

Catatan metodologi untuk harness berikutnya:

- **Verifikasi SQL manual tidak membuktikan perilaku Hibernate** — selalu uji lewat
  mesin aslinya; label ganda `coalesce` tak terlihat dari JDBC posisional.
- Jalur `prosesApi` butuh konteks pengguna: `RequestContext.set(proxy)` dengan
  `getSession().getAttribute("mytbmuser")`.
- Fixture jalur sukses (dokumen ber-cap, baris penyumbang nyata) WAJIB untuk menguji
  pembatalan — pembatalan atas riwayat buatan saja tidak menyentuh FK/pelepasan cap.
- EOL campuran di keluarga kantin: `PostingHppKantinAction` CRLF murni,
  `PostingPenjualanKantinAction` LF murni — periksa per berkas.
