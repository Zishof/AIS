# Posting Dana Anggota Koperasi (dok 61 butir B)

Tanggal: 31 Agustus 2026. Kode masuk SVN **r78646** (tahap 1) dan **r78649** (tahap 2). Membuka butir **B** gap analysis
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

## 2. Yang dijurnal

| Dokumen | Kriteria | Jurnal | Tanggal |
|---|---|---|---|
| **Topup Saldo Anggota** (`PembayaranAnggotaKoperasi`) | sudah dibayar (`tanggal_bayar` terisi), nominal ≠ 0 | **Dr** akun kas/bank cara pembayaran / **Cr** akun kewajiban saldo anggota | tanggal bayar |
| **Pencairan Diskon Anggota** (`PencairanDiskon`) | status `BERHASIL`, nominal cair ≠ 0 | **Dr** akun beban pencairan / **Cr** akun kas/bank cara pembayaran | waktu pencairan |
| **Penyesuaian Saldo Anggota** (`PenyesuaianSaldoAnggota`) | selisih ≠ 0 | memorial tanpa kas: selisih POSITIF **Dr** akun selisih / **Cr** kewajiban saldo anggota; NEGATIF kebalikannya, nilainya mutlak | waktu opname |
| **Modal Penyertaan Masuk** (`ModalPenyertaanKoperasi`) | aktif, nominal ≠ 0, `tanggal_masuk` terisi | **Dr** akun kas penerimaan modal / **Cr** akun modal penyertaan | tanggal masuk |

Lima kunci Konfigurasi baru:

- `akun_kewajiban_saldo_anggota_id` — utang koperasi kepada anggota atas saldo/deposit mereka.
- `akun_beban_pencairan_diskon_id` — beban saat saldo cashback benar-benar dicairkan.
- `akun_selisih_saldo_anggota_id` — kerugian/keuntungan selisih opname saldo e-wallet.
- `akun_kas_modal_penyertaan_id` — kas/bank penerima setoran modal penyertaan.
- `akun_modal_penyertaan_id` — pos modal penyertaan itu sendiri.

Modal penyertaan memakai akun kas dari Konfigurasi (bukan dari cara pembayaran) karena
entitasnya memang tidak menyimpan cara pembayaran.

Akun **kas/bank tidak dikonfigurasi** karena datanya sudah ada: diambil dari
`CaraPembayaranKoperasi.akun` milik dokumen. Dokumen yang cara pembayarannya belum ber-akun
dilewati mesin tetapi tetap terhitung draf — sama seperti dokumen berjenis tanpa akun pada
penghapusan aset (dok 65).

Keempat baris dasbor masuk kategori **simpan_pinjam** (serumpun perputaran dana milik anggota),
dengan kunci izin deskriptif fail-closed `topup_saldo_anggota`, `pencairan_diskon`,
`penyesuaian_saldo_anggota`, dan `modal_penyertaan`.

Satu hal yang sengaja berbeda dari mesin-mesin sebelumnya: metode kriteria di
`DraftJurnalRingkasanUtil` **memanggil kriteria mesinnya langsung**, bukan menyalin ulang
syaratnya. Dengan begitu angka draf di dasbor dan dokumen yang benar-benar diproses mesin mustahil
berselisih — kelemahan yang selama ini hanya dijaga oleh kedisiplinan menyalin.

## 3. Pengujian

Harness `TesPostingDanaAnggota` (scratchpad, DB UAT), fixture rentang **1–31 Januari 2092**:

| Skenario | Hasil |
|---|---|
| Dasbor | draf topup 2 (T1 & T3), pencairan 1 (P1), penyesuaian 2, modal 1 — dokumen belum dibayar / PENDING / berselisih nol / bernominal nol tidak terpilih |
| Topup terjurnal | Dr bank 500rb / Cr kewajiban saldo 500rb; bertanggal tanggal bayar |
| Cara bayar tanpa akun | T3 dilewati mesin, tetap draf, tidak tercap |
| Pencairan terjurnal | Dr beban cashback 150rb / Cr bank 150rb; bertanggal waktu pencairan |
| Penyesuaian selisih POSITIF | Dr akun selisih 30rb / Cr kewajiban saldo 30rb |
| Penyesuaian selisih NEGATIF | arah membalik dan nilainya MUTLAK (20rb) — dipastikan pula tidak ada satu pun baris jurnal bernilai negatif di seluruh tabel |
| Modal penyertaan | Dr bank penerimaan 7jt / Cr modal penyertaan 7jt; bertanggal tanggal masuk |
| Dasbor sesudah posting | angka draf/terposting bergeser konsisten dengan mesin |
| Idempoten & batal | posting/batal ulang 0 untuk keempatnya; jurnal habis; dokumen kembali draf |

**LULUS 24, GAGAL 0.**

### Jebakan baru yang ditemukan harness

`PembayaranAnggotaKoperasi.getNama()` adalah **getter turunan**: nilainya ditulis ulang menjadi
`"<kode anggota> - <nama anggota>-<nominal>"` begitu entitas di-flush. Akibatnya penanda fixture
`nama LIKE 'UATDNA-%'` lenyap sesudah posting dan pembersihan berbasis nama meninggalkan sampah
yang lalu memblokir penghapusan akun karena FK. Penanda diganti jendela tanggal. Ini keluarga
jebakan yang sama dengan `getTanggalTransaksi()`, `getDibayar()`, dan `getKodeInvoice()` pada
modul lain — **jangan pernah memakai kolom bergetter turunan sebagai penanda data uji.**

## 4. Sisa butir B dan alasannya

| Dokumen | Keadaan | Catatan |
|---|---|---|
| `ModalPenyertaanKoperasi` — kaki KEMBALI | belum | Satu baris memuat DUA peristiwa uang (setoran saat `tanggal_masuk`, pengembalian saat status DITARIK / `tanggal_kembali`) sementara satu dokumen hanya punya satu cap `posting_history`. Perlu cap kedua (`posting_history_kembali`) plus baris dasbor sendiri; sengaja tidak dipaksakan ke dalam pola satu-cap yang dipakai seluruh mesin lain. |
| `PembagianShu` / `ShuAnggota` | belum | Paling berat kebijakannya: satu dokumen memecah SHU ke TUJUH pos (cadangan, jasa modal, jasa usaha, pengurus, pendidikan, sosial, lain) menurut persentase. Agar jurnal tetap seimbang, semua pos berpersentase ≠ 0 wajib punya akun — kalau salah satu belum diisi, dokumennya harus dilewati UTUH, bukan sebagian. Perlu tujuh kunci Konfigurasi plus satu akun SHU ditahan, dan pembulatan sisa pembagian harus dibebankan ke satu pos agar debet = kredit persis. Ini keputusan akuntansi yang sebaiknya ditetapkan lebih dahulu. |
| `DepositoRolloverKoperasi` | **tidak perlu** | Rollover hanya memperpanjang jangka waktu deposito yang sudah ada; tidak ada uang berpindah. Yang perlu dijurnal adalah penempatan/pencairan pokoknya (lewat simpan-pinjam, dok 62) dan akrual bunganya — bukan peristiwa rollover-nya. |

## 5. Bagi admin

Isi kelima Konfigurasi di §2 sebelum memakai baris posting yang baru. Selama sebuah kunci masih
kosong, baris posting terkait tidak menjurnal apa pun dan dokumennya menetap sebagai draf —
alasannya tercatat di ErrorAudit.

Pastikan pula setiap **Cara Pembayaran Koperasi** yang dipakai topup dan pencairan sudah
ditautkan ke akun kas/bank-nya; tanpa itu dokumennya juga menetap sebagai draf.

Tidak ada tabel baru; empat kolom `posting_history` dan empat kolom referensi pada
`akunting.grup_transaksi` dibuat `hbm2ddl update` saat Tomcat mulai.
