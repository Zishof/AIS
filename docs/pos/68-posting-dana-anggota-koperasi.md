# Posting Dana Anggota Koperasi — Tahap 1: Topup Saldo & Pencairan Diskon

Tanggal: 31 Agustus 2026. Kode masuk SVN **r78646**. Membuka butir **B** gap analysis
[61-gap-analysis-posting.md](61-gap-analysis-posting.md) — butir terakhir yang tersisa dari peta
posting, dan satu-satunya yang dahulu ditandai "menunggu keputusan akuntansi".

## 1. Kenapa butir ini sempat tertahan, dan bagaimana dilepas

Dok 61 mencatat keluarga dana anggota sebagai celah yang "perlu KEPUTUSAN AKUNTANSI dulu (akun
kewajiban saldo anggota, akun modal, akun SHU) sebelum mesin posting dibuat". Betul bahwa
pemilihan akunnya bukan keputusan teknis — tetapi menunggu keputusan itu untuk mulai menulis
mesinnya ternyata tidak perlu: **akunnya dibuat dapat diatur**, persis pola yang sudah terbukti
pada jurnal balik pembatalan kantin (dok 64, akun lewat Konfigurasi) dan simpan-pinjam (dok 62,
akun lewat master produk).

Jadi kebijakan akuntansi tetap milik lembaga, sementara jalur jurnalnya sudah siap dan teruji.
Selama akunnya belum diisi, mesin **tidak menjurnal apa pun** — bukan menebak akun — dan
dokumennya tetap tampil sebagai draf di dasbor sehingga kekurangan setup terlihat.

## 2. Yang dijurnal pada tahap ini

| Dokumen | Kriteria | Jurnal | Tanggal |
|---|---|---|---|
| **Topup Saldo Anggota** (`PembayaranAnggotaKoperasi`) | sudah dibayar (`tanggal_bayar` terisi), nominal ≠ 0 | **Dr** akun kas/bank cara pembayaran / **Cr** akun kewajiban saldo anggota | tanggal bayar |
| **Pencairan Diskon Anggota** (`PencairanDiskon`) | status `BERHASIL`, nominal cair ≠ 0 | **Dr** akun beban pencairan / **Cr** akun kas/bank cara pembayaran | waktu pencairan |

Dua kunci Konfigurasi baru:

- `akun_kewajiban_saldo_anggota_id` — utang koperasi kepada anggota atas saldo/deposit mereka.
- `akun_beban_pencairan_diskon_id` — beban saat saldo cashback benar-benar dicairkan.

Akun **kas/bank tidak dikonfigurasi** karena datanya sudah ada: diambil dari
`CaraPembayaranKoperasi.akun` milik dokumen. Dokumen yang cara pembayarannya belum ber-akun
dilewati mesin tetapi tetap terhitung draf — sama seperti dokumen berjenis tanpa akun pada
penghapusan aset (dok 65).

Kedua baris dasbor masuk kategori **simpan_pinjam** (serumpun perputaran dana milik anggota),
dengan kunci izin deskriptif fail-closed `topup_saldo_anggota` dan `pencairan_diskon`.

Satu hal yang sengaja berbeda dari mesin-mesin sebelumnya: metode kriteria di
`DraftJurnalRingkasanUtil` **memanggil kriteria mesinnya langsung**, bukan menyalin ulang
syaratnya. Dengan begitu angka draf di dasbor dan dokumen yang benar-benar diproses mesin mustahil
berselisih — kelemahan yang selama ini hanya dijaga oleh kedisiplinan menyalin.

## 3. Pengujian

Harness `TesPostingDanaAnggota` (scratchpad, DB UAT), fixture rentang **1–31 Januari 2092**:

| Skenario | Hasil |
|---|---|
| Dasbor | draf topup 2 (T1 & T3), draf pencairan 1 (P1); T2 belum dibayar & P2 masih PENDING tidak terpilih |
| Topup terjurnal | 1 dokumen; Dr bank 500rb / Cr kewajiban saldo 500rb; bertanggal tanggal bayar |
| Cara bayar tanpa akun | T3 dilewati mesin, tetap draf, tidak tercap |
| Pencairan terjurnal | 1 dokumen; Dr beban cashback 150rb / Cr bank 150rb; bertanggal waktu pencairan |
| Dasbor sesudah posting | topup 1 terposting + 1 draf, pencairan 1 terposting |
| Idempoten & batal | posting/batal ulang 0; jurnal habis; dokumen kembali draf |

**LULUS 14, GAGAL 0.**

### Jebakan baru yang ditemukan harness

`PembayaranAnggotaKoperasi.getNama()` adalah **getter turunan**: nilainya ditulis ulang menjadi
`"<kode anggota> - <nama anggota>-<nominal>"` begitu entitas di-flush. Akibatnya penanda fixture
`nama LIKE 'UATDNA-%'` lenyap sesudah posting dan pembersihan berbasis nama meninggalkan sampah
yang lalu memblokir penghapusan akun karena FK. Penanda diganti jendela tanggal. Ini keluarga
jebakan yang sama dengan `getTanggalTransaksi()`, `getDibayar()`, dan `getKodeInvoice()` pada
modul lain — **jangan pernah memakai kolom bergetter turunan sebagai penanda data uji.**

## 4. Sisa butir B (tahap 2) dan alasannya belum dikerjakan

| Dokumen | Keadaan | Catatan |
|---|---|---|
| `PenyesuaianSaldoAnggota` | belum | Jurnal non-kas: selisih opname saldo e-wallet vs kewajiban saldo anggota. Butuh satu akun selisih (dapat memakai pola Konfigurasi yang sama). |
| `ModalPenyertaanKoperasi` | belum | Ada DUA peristiwa uang pada satu baris (masuk saat `tanggal_masuk`, kembali saat `tanggal_kembali` / status DITARIK). Satu kolom `posting_history` tidak cukup — perlu dua cap atau dua baris dasbor. |
| `PembagianShu` / `ShuAnggota` | belum | Paling berat kebijakannya: satu dokumen memecah SHU ke tujuh pos (cadangan, jasa modal, jasa usaha, pengurus, pendidikan, sosial, lain) menurut persentase. Agar jurnal tetap seimbang, semua pos berpersentase ≠ 0 wajib punya akun — kalau tidak, dokumennya harus dilewati utuh, bukan sebagian. |
| `DepositoRolloverKoperasi` | **tidak perlu** | Rollover hanya memperpanjang jangka waktu deposito yang sudah ada; tidak ada uang berpindah. Yang perlu dijurnal adalah penempatan/pencairan pokoknya (lewat simpan-pinjam, dok 62) dan akrual bunganya — bukan peristiwa rollover-nya. |

## 5. Bagi admin

Isi dua Konfigurasi berikut sebelum memakai baris posting yang baru:

- `akun_kewajiban_saldo_anggota_id` → akun kewajiban "Saldo/Deposit Anggota".
- `akun_beban_pencairan_diskon_id` → akun beban "Pencairan Cashback/Diskon Anggota".

Selain itu pastikan setiap **Cara Pembayaran Koperasi** yang dipakai topup dan pencairan sudah
ditautkan ke akun kas/bank-nya; tanpa itu dokumennya akan menetap sebagai draf.

Tidak ada tabel baru; dua kolom `posting_history` dan dua kolom referensi pada
`akunting.grup_transaksi` dibuat `hbm2ddl update` saat Tomcat mulai.
