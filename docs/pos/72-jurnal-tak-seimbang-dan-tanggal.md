# Dua Kelas Sisa: Jurnal Tak Seimbang dan Tanggal Jurnal di Luar Rentang

Tanggal: 31 Agustus 2026, pada HEAD r78717. **Tanpa perubahan kode**: keduanya temuan
struktural yang pembuktiannya menuntut data produksi, dan alat ukurnya sudah disiapkan.
Penutup rangkaian sapuan [69](69-audit-tombol-zk-menyeluruh.md) →
[70](70-audit-dokumen-berkaki-ganda.md) → [71](71-tabrakan-kodeunik-antar-kaki.md).

## 1. Buku besar tidak punya penjaga keseimbangan sama sekali

`CommonAkunting.saveTransaksi` menghitung `totalDebet` dan `totalKredit`, menyimpannya
pada grup, lalu **menulis jurnalnya apa adanya** — tidak ada satu baris pun yang
membandingkan keduanya. Penjaga yang ada hanya tanggal closing. Artinya setiap mesin yang
membangun sisi debet dan sisi kredit dari **sumber yang berbeda** dapat menulis jurnal
timpang tanpa galat, dan buku besar diam saja.

Sebagian besar mesin aman secara konstruksi (menambahkan nilai yang sama ke kedua sisi
secara berpasangan). Yang menyusun kedua sisi dari sumber berbeda dan sudah punya penjaga
sendiri: Penggajian Pegawai (perbandingan `intValue`), HPP dan Penjualan Kantin (agregat
peta akun), `SaldoAwalAkunHelper`.

**Yang perlu diperiksa dengan data:** pembayaran tagihan vendor pada cabang
`akunUtangDariAnggaran`. Di sana sisi DEBET disusun dari **seluruh rincian pemesanan**
(`jumlah × hargaBeli − potongan` per baris), sedangkan sisi KREDIT sebesar **yang
dibayar** (`detail.getDibayar()`). Dua angka itu tidak harus sama — pembayaran sebagian
atau pembayaran satu tagihan dari beberapa akan membuatnya berselisih. Struktur ini ada
di ketiga jalurnya (massal ZK, per baris, mesin API) dan sudah lama begitu, jadi ini
bukan akibat pekerjaan sesi ini.

Saya **tidak mengubahnya**: memperbaiki berarti memutuskan perlakuan akuntansi yang benar
untuk pembayaran sebagian (memotong debet secara proporsional, atau memang menjurnal
penuh dengan selisih ke akun lain) — itu keputusan tim akuntansi, bukan tebakan yang
pantas diambil dari pembacaan kode. Dan tanpa data produksi saya tidak bisa membuktikan
apakah divergensi itu benar-benar terjadi.

**Alat ukurnya sudah ada**: Q7 dan Q8 pada
[`docs/sql/2026-08-31-diagnosa-tabrakan-kodeunik-tagihan.sql`](../sql/2026-08-31-diagnosa-tabrakan-kodeunik-tagihan.sql)
(r78717) menghitung seluruh grup jurnal yang tidak seimbang dan memecahnya per jenis
posting, sehingga modul penyebabnya langsung tertunjuk. Bila jenis pembayaran vendor
muncul di sana, dugaan di atas terbukti; bila tidak, berarti dalam praktiknya pembayaran
selalu penuh dan strukturnya aman.

## 2. Tanggal jurnal bukan tanggal penyaring

Setiap mesin menyaring dokumen dengan satu kolom tanggal, tetapi menulis jurnalnya dengan
tanggal lain — dan itu memang disengaja serta terdokumentasi per modul (mis. dok 56:
Perjanjian Kerjasama menyaring `tanggal_pembuatan` tetapi menjurnal pada
`tanggal_persetujuan`). Konsekuensi yang tidak selalu disadari operator: memposting
"periode Agustus" dapat menulis jurnal bertanggal Juli, dan bila Juli sudah ditutup buku,
`saveTransaksi` menolaknya — mesin melaporkan dokumen "dilewati" tanpa menyebut sebabnya
tanggal.

Ini bukan cacat kode; penjaga closing bekerja persis sebagaimana mestinya. Yang kurang
adalah kejelasan pesannya. Dicatat di sini supaya keluhan lapangan bergaya "sudah saya
posting tapi angkanya tidak turun" tidak dikejar sebagai bug hitung: periksa lebih dulu
apakah tanggal jurnal dokumen itu jatuh sebelum closing terakhir.

## 3. Status seluruh rangkaian sapuan

| Kelas cacat | Hasil |
|---|---|
| Lima kelas layar ZK (dok 69) | bersih pada 47 layar; satu wart tipe dirapikan |
| Dokumen ber-kaki ganda (dok 70) | 3 cacat diperbaiki + teruji |
| Penjaga closing pada hapus (dok 70 §5.1) | 1 cacat diperbaiki + teruji |
| Empat kelas lain (dok 70 §5.2) | bersih, dengan alasan positif palsunya |
| Tabrakan `kodeUnik` (dok 71) | kaki siswa diperbaiki + teruji; kolom ber-pemilik ganda disapu tuntas |
| Jurnal tak seimbang (dok ini §1) | struktur berisiko teridentifikasi; menunggu bukti data lewat Q7/Q8 |
| Tanggal jurnal vs penyaring (dok ini §2) | bukan cacat; dicatat sebagai penjelasan gejala |

Yang tersisa seluruhnya menunggu **akses baca ke basis data produksi** — bukan pekerjaan
kode. Jalankan skrip diagnosa r78717, lalu putuskan pemulihannya bersama bagian keuangan.
