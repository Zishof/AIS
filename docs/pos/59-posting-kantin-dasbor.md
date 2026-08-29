# Dasbor Draft Jurnal: Keluarga Kantin/Toko (HPP, Penjualan, 4 Posting Toko)

Tanggal: 29 Agustus 2026. Kode masuk SVN **r78560** — commit sapuan sesi paralel TANPA
pesan log yang membawa kelima berkas pekerjaan ini di tengah sesi (plus dua berkas newui
milik sesi penyapu); dokumen ini adalah catatan penggantinya, ditulis oleh sesi yang
mengerjakan kodenya. Berkas: `PostingHppKantinAction`, `PostingPenjualanKantinAction`,
`PostingKantinLanjutanHelper`, `DraftJurnalRingkasanUtil`, `DraftJurnalApiHelper`.
Mirror `java/` selaras byte-identik. Penutup peta [53 §6](53-posting-jurnal-umum.md)
setelah [58-posting-saldo-awal-kas-kecil.md](58-posting-saldo-awal-kas-kecil.md).

## 1. Apa yang ditambahkan

Mesin posting kantin SUDAH ada sebelumnya (`prosesApi` HPP/Penjualan per periode,
`PostingKantinLanjutanHelper` per dokumen untuk 4 jenis toko — dipakai Desktop/Android
lewat PosApi). Yang belum ada adalah INTEGRASI DASBOR draft jurnal POS: baris "Posting
HPP" tampil tetapi `bisaPosting=false`, dan baris lain tidak ada. Kini:

| Baris (kunci) | Hitung draf / terposting | Posting dari dasbor | Batal dari dasbor |
|---|---|---|---|
| Posting HPP (`posting_hpp`, sudah ada) | penghitung lama action | `prosesApi(mulai, sampai, true)` — 1 jurnal agregat, maju-saja | `batalkanPeriode` BARU — mundur-saja |
| Penjualan Kantin (`posting_penjualan_kantin`) BARU | `hitungDraftPending/-Terposting` action | `prosesApi` — agregat + cap header | `batalkanPeriode` BARU — mundur-saja + LEPAS tanda header |
| Kulakan Toko (`posting_kulakan`) BARU | penghitung SQL BARU di helper | `posting_kulakan_terapkan` helper (per dokumen siap) | DITOLAK → dokumen pembalik |
| Pembayaran Hutang Toko (`posting_bayar_hutang`) BARU | idem | idem | idem |
| Penerimaan Piutang Toko (`posting_terima_piutang`) BARU | idem | idem | idem |
| Penyesuaian Persediaan Toko (`posting_penyesuaian`) BARU | idem (jumlah 4 sumber: retur beli/jual, opname selisih≠0, mutasi) | idem | idem |

Semua baris kategori `posting_penjualan` (yang memang dicadangkan untuk keluarga ini),
kondisional `POSTING_JURNAL_TAB_PREFIX + <kunci izin>` (default tampil, admin bisa
mematikan), izin per baris memakai kunci posting POS masing-masing — semuanya
fail-closed lewat `KUNCI_DEFAULT_NONAKTIF`. Keluarga ini TIDAK lewat jalur
`hitungDokumen` generik (dokumennya milik modul koperasi): `jalankanPosting` bercabang
ke `jalankanKantinBatch` / `jalankanKantinToko` sebelum jalur generik.

## 2. Desain pembatalan

- **HPP & Penjualan (batch)**: `batalkanPeriode(mulai, sampai)` menghapus batch
  (transaksi anak → grup → riwayat ber-jenis) yang TANGGAL riwayatnya jatuh pada
  rentang, **mundur-saja (LIFO)** — ditolak bila masih ada batch lebih baru daripada
  `sampai`. Ini cermin aturan maju-saja postingnya: tanpa itu, membatalkan periode
  tengah menciptakan lubang yang tidak akan pernah bisa diposting ulang (posting
  mensyaratkan mulai SETELAH batch terakhir). Batch yang grupnya sudah closing
  dilewati utuh. Penjualan juga MELEPAS tanda `posting_history` pada header
  (`koperasi.pembelian_anggota_koperasi`) sebelum riwayatnya dihapus, sehingga
  dokumen kembali draf dan dapat diposting ulang.
- **4 posting toko (per dokumen)**: batal dari dasbor DITOLAK dengan penjelasan —
  modul Toko mengoreksi lewat DOKUMEN PEMBALIK bernominal negatif (menu reversal
  AP/AR) yang ikut dijurnal; menghapus jurnal akan menabrak desain itu.

Penghitung draf toko sengaja murah (COUNT dokumen belum bertanda pada rentang,
predikat disalin dari draf helper) dan TIDAK memeriksa kesiapan akun: dokumen yang
akunnya belum lengkap memang harus tetap terhitung draf yang menunggu dibereskan.

## 3. Pengujian

Harness `TesPostingKantinDasbor` (scratchpad, DB UAT `ais`), fixture `UATKTN-` rentang
**10–20 Mei 2091** (Maret = pengembalian, April = kas kecil, Juni = payroll):

| Skenario | Hasil |
|---|---|
| Penghitung toko bayar_hutang / terima_piutang | draf = 1 pada rentang, tepat |
| Batal toko via ruting dasbor (admin) | status 91 + arahan dokumen pembalik |
| Posting HPP via ruting saat ada batch fixture | 91 + pesan mesin "Periode tumpang tindih; posting terakhir sampai 18-05-2091" — validasi maju-saja & penerusan pesan teruji |
| Batal HPP 10–15 Mei (batch 18 Mei masih ada) | DITOLAK — mundur-saja bekerja |
| Batal HPP 10–20 Mei | n=2; grup + transaksi anak + riwayat terhapus; batal ulang n=0 |
| Batal Penjualan 10–20 Mei | n=1; tanda header TERLEPAS; grup & riwayat terhapus |

**LULUS 16, GAGAL 0.** Kompilasi `javac -source 1.7 -target 1.7` kelima berkas bersih.
Dua fixture penghitung (kulakan, opname) ter-skip di UAT — kolom `produk` NOT NULL —
predikat SQL-nya identik pola dengan dua jenis yang teruji.

Catatan harness baru: `akunting.posting_history` mewajibkan `nama` dan `tbmuser`;
`koperasi.pembelian_anggota_koperasi` mewajibkan `kode`;
`pembayaran_hutang_supplier.supplier` dan `penerimaan_piutang_customer.customer` wajib
(pinjam id master yang ada via subquery); cek EOL selalu dengan `grep -U` biner —
label `file(1)` ("Java source"/"HTML document") tidak menandakan EOL.

## 4. Peta modul: SELESAI

Dengan ini seluruh peta dok 53 §6 tuntas: 53 Jurnal Umum, 54 Pengembalian UM,
55/57 trio pembayaran vendor, 56 Perjanjian Kerjasama, 57 payroll, 58 Saldo Awal Kas
Kecil, 59 keluarga kantin/toko (dok ini). Semua baris dasbor draft jurnal yang punya
mesin kini juga punya tombol yang benar-benar bekerja — atau penolakan yang
menjelaskan dirinya.
