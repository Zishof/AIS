# Gap Analysis Posting: Hub ZKoss "Posting Jurnal" vs Dasbor Draft Jurnal POS Flutter

Tanggal: 29 Agustus 2026, pada HEAD r78601+. Menjawab pertanyaan (screenshot hub ZK):
"semua posting sudah diterapkan — posting apa yang belum?", kali ini dengan sumbu
perbandingan **tab layar ZKoss** (`PostingJurnalAction` + composite ZUL-nya) vs
**baris dasbor POS Flutter** (`DraftJurnalRingkasanUtil`, 49 baris saat ini).

## 1. Kesimpulan satu kalimat

**POS Flutter kini SUPERSET penuh dari hub ZK** — semua kemampuan posting yang bisa
dibuka dari hub ZK ada padanannya di POS, sedangkan hub ZK menampilkan hanya
±26 kemampuan dari 49 baris POS; tidak ada satu pun jenis posting yang "cuma ada di
ZK dan belum di POS".

## 2. Arah ZK → POS: tidak ada celah

Inventaris tab hub ZK (dinamis dari `PostingJurnalAction`, kondisional konfigurasi
`POSTING_JURNAL_TAB_PREFIX`):

| Tab ZK | Isi | Padanan POS |
|---|---|---|
| Draft Jurnal | dasbor ZK (`DrafJurnalAction`) | layar Draft Jurnal POS (API yang sama) |
| Jurnal Umum | `grup_transaksi.zul` | baris Jurnal Umum |
| Uang Muka dan Kas | sub: PJ Uang Muka, Saldo Awal Kas Kecil, Kas Kecil, Kas Besar, (PJ Kas Besar `visible=false`) | 5 baris padanannya |
| Pajak | posting PJ pajak | baris Pajak |
| Transaksi Vendor | sub: Penerimaan Tagihan, Pekerjaan, **Termin Vendor (= pembayaran termin)**, DP Vendor, DP Pekerjaan, Jurnal Balik DP | 6 baris padanannya |
| Gaji | SATU halaman (`PostingTransaksiPembayaranGajiAction`) | baris Gaji |
| Siswa dan Mahasiswa | HANYA 2 leaf: Piutang Siswa + Cicilan Mahasiswa | 2 dari 13 baris |
| Penyusutan | 3 sub (2 BAST + penyusutan) | 3 baris padanannya |
| Pengajuan Transfer / Transitori / Closing | masing-masing | baris padanannya |
| Posting HPP / Penjualan / Kulakan / Bayar Hutang / Terima Piutang / Penyesuaian | keluarga kantin-toko (dok 59) | 6 baris padanannya |
| Jurnal Penyesuaian / Saldo Awal / Tutup Buku | siklus koperasi — jurnal LANGSUNG saat input (helper API), bukan posting batch | layar POS tersendiri (bukan baris dasbor; memang bukan posting) |

## 3. Arah POS → ZK: 23 baris POS TIDAK punya tab di hub ZK

Dikelompokkan menurut keadaannya:

**a. Layar ZK-nya ADA tetapi tidak dipasang di hub** (dibuka dari menu ZK lain, atau
tab-nya disembunyikan):

- Uang Muka, Penggantian Kas Kecil, Dana Talangan (layar `PostingUangMukaAction`,
  `PostingPenggantianKasKecilAction`, `PostingDanaTalanganAction`)
- Pertanggungjawaban Kas Besar — tab-nya ADA tetapi `visible="false"` di
  `uang_muka_dan_kas_kecil.zul`
- Pengembalian Uang Muka (`PostingPertangungjawabanPengembalianAction`)
- Pembayaran Tagihan Vendor & Pembayaran DP Vendor — `posting_pembayaran.zul` dan
  `posting_pembayaran_dp.zul` tidak di-include tab Transaksi Vendor (hanya Termin yang
  masuk)
- Perjanjian Kerjasama (`PostingPerjanjianKerjasamaAction`)
- Transaksi Pegawai & Penggajian per Pegawai (`PostingTransaksiPegawaiAction`,
  `PostingTransaksiPenggajianAction`) — tab Gaji hub hanya memuat Pembayaran Gaji
- 11 baris granular Siswa/Mahasiswa (Dibayar Dimuka, Deposit, Denda, Diskon, Biaya
  Adm, Payment Gateway, dst.) — hub hanya memuat 2 leaf

**b. POS-only sejati (tidak ada layar ZK sama sekali):**

- **Simpan Pinjam Koperasi** (baru, dok 62/r78584) — mesin + baris dasbor POS;
  layar ZK `TransaksiKoperasiAction` adalah layar INPUT-nya, bukan layar posting.

## 4. Apa artinya (dan apa yang TIDAK perlu dikerjakan)

Celah arah POS→ZK ini **bukan celah akuntansi** — semua mesin posting itu satu kode
dengan yang dipakai POS, dan penggunanya bisa memposting dari POS. Menambah ±23 tab ke
hub ZK adalah pekerjaan UI ZK murni yang nilainya menurun karena arah produk ke POS.
Rekomendasi: JANGAN menduplikasi ke hub ZK; bila kelengkapan hub ZK tetap diinginkan,
prioritas kecil: (1) menyalakan tab PJ Kas Besar yang disembunyikan, (2) menambah dua
include pembayaran vendor yang tertinggal di `transaksi_vendor.zul` — keduanya
pekerjaan menit-an; sisanya biarkan POS yang memimpin.

**Tindak lanjut 31 Agustus 2026** ([69-audit-tombol-zk-menyeluruh.md](69-audit-tombol-zk-menyeluruh.md) §4):
butir (2) **SELESAI r78661** — kedua include dipasang di ujung daftar karena ZK
memasangkan tab↔tabpanel secara posisional. Butir (1) **sengaja tidak dikerjakan**:
`svn blame` menunjukkan tab PJ Kas Besar dan tab DP Pekerjaan Vendor (yang luput dari
inventaris §2 dokumen ini — tab-nya ada tetapi `visible="false"`) disembunyikan
BERSAMA dalam satu commit r74892, jadi itu keputusan produk, bukan kelalaian; keputusan
menyalakannya dikembalikan ke pemilik produk.

Celah AKUNTANSI yang sesungguhnya tetap yang tercatat di
[61-gap-analysis-posting.md](61-gap-analysis-posting.md): butir **A selesai** (dok 62),
**C selesai** (dok 64), **D selesai** (dok 65), **B selesai** (dok 68; kaki kembali
modal penyertaan menyusul di r78651). Yang tersisa hanya **E** — keputusan lingkup
Inventory Sales, bukan pekerjaan teknis.
