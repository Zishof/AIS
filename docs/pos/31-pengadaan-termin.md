# Pengadaan bertermin — penerimaan, tagihan, cetak, dan status PO

> Gambaran modulnya secara utuh ada di **[10-pengadaan.md](10-pengadaan.md)**.
> Dokumen ini khusus perilaku **bertermin**, yang aturannya berbeda dari pesanan biasa.

Pesanan bertermin ditagih **per termin**, bukan sekaligus. Empat perbaikan menyusul aturan
itu, semuanya menyamakan POS dengan perilaku ZKoss yang sudah berjalan.

## 1. Satu penerimaan untuk satu termin

Pesanan dengan tiga termin melahirkan **tiga BAST**, masing-masing dengan fakturnya sendiri
— jadi tagihannya juga tiga kali.

### Tabelnya sudah siap sejak lama

`PenerimaanPengadaanMasterAsset` sudah punya tiga kolom yang dipakai ZKoss lewat
`PenerimaanPengadaanMasterAssetAction`:

| Kolom | Isi |
|---|---|
| `kodeTermin` | kunci termin (`key` pada JSON `formula` milik PO) |
| `jsonTermin` | isi terminnya |
| `keteranganTermin` | namanya, supaya layar tidak perlu mengurai JSON lagi |

**POS-lah yang tidak pernah mengisinya.** Akibatnya penerimaan dari POS tidak dapat
dibedakan terminnya dan hanya bisa ditagih sekali. Tidak ada kolom baru yang dibuat.

### Penjagaan di `bastSimpan`

1. Termin **wajib dipilih** bila pesanannya bertermin.
2. Termin yang bukan milik pesanan itu ditolak.
3. Termin yang **sudah diterima lewat penerimaan aktif lain** ditolak.

Penjagaan ketiga yang paling penting: dua penerimaan aktif pada termin yang sama membuat
satu termin **tertagih dua kali**. Jangan dilonggarkan.

### Yang dikirim ke layar

`bastDariPo` mengirim jadwal termin berikut penanda mana yang sudah diterima **dan lewat
penerimaan mana**. Layar menampilkan termin yang sudah diterima namun tidak dapat dipilih
lagi, lengkap dengan nomor BAST-nya — kemajuannya terlihat tanpa membuka dokumen satu per satu.

`bastDaftar`, `tagihanDaftar`, dan `bastDetail` menyertakan `terminKey` dan nama terminnya.

### Data lama

Penerimaan yang dibuat sebelum aturan ini tidak menyimpan terminnya, sehingga kolom
**"Termin ke"** menampilkan `-`. Bila disunting, terminnya harus dipilih ulang. Ini tidak
dapat ditebak mundur: data lamanya memang tidak menyimpan termin mana yang dimaksud.

## 2. Cetak PO menampilkan rincian termin

`webapp/report/asset/pemesanan_pengadaan.jrxml`

PO bertermin selama ini tercetak **tanpa menyebut jadwal pembayarannya sama sekali**,
padahal termin bagian dari kesepakatan yang ditandatangani.

Datanya sudah lama disiapkan: `PemesananPengadaanMasterAssetAction.paramTermin` mengisi
parameter `mapsTermin` beserta totalnya, dan templatnya bahkan sudah mendeklarasikan
parameternya — hanya tidak pernah menampilkannya. Deklarasinya pun bertipe salah
(`String`, bukan `List`).

### Band tersendiri, bukan disisipkan ke ringkasan

Tabelnya diletakkan pada **`groupFooter` ber-`printWhenExpression`**.

Percobaan pertama menyisipkannya ke band ringkasan dan menggeser tanda tangan 41px ke bawah.
Hasil render membuktikan pergeseran itu **tetap terjadi pada PO tanpa termin**, karena elemen
`positionType="Float"` tidak naik kembali ketika elemen di atasnya tidak tercetak. Band
tersendiri runtuh sepenuhnya.

### Cara memverifikasinya tanpa server

Templat dirender di luar server memakai JasperReports 6.4.1, dua keadaan, lalu **posisi
setiap teks dibandingkan** terhadap hasil revisi sebelumnya:

| Keadaan | Hasil |
|---|---|
| Bertermin | judul, baris kepala, dan tiap termin tercetak (nomor, uraian, jatuh tempo, nilai) |
| Tanpa termin | blok termin nihil; tanda tangan pada y **413/469/484** — sama persis seperti sebelumnya |

> Baris `jr:list` dirender ke dalam `JRPrintFrame`. Dump teks yang tidak menelusuri frame
> akan melaporkannya hilang padahal ada — sempat mengecoh saat verifikasi.

Runtime mengompilasi ulang `.jasper` sendiri ketika `.jrxml` lebih baru
(`Report.recompileJasperJikaJrxmlLebihBaru`), jadi tidak perlu kompilasi manual saat deploy.

## 3. Status PO awal: DISETUJUI, bukan DITUTUP

`PengadaanPosApiHelper.statusPo`

Label `DITUTUP` keliru menggambarkan keadaannya: dokumennya sah, sudah disetujui, dan
barangnya sebagian sudah diterima. Yang berhenti hanyalah **sisa** kiriman, dan sisa itu
pindah ke pesanan susulan.

Label itu juga **tidak pernah ada pada penyaring status di layar mana pun**
(Draft/Disetujui/Ditolak/Lunas), sehingga pesanan yang menyandangnya justru tidak dapat
ditemukan lewat penyaring.

Keadaan "ditutup" tetap terkirim lewat medan `tutup` dan `alasanTutup` yang terpisah.

### Penjagaan yang menjadi WAJIB karena perubahan ini

Pesanan seperti itu kini tampil `DISETUJUI`, dan layar menyalakan tombol **"Batalkan
keputusan"** untuk status tersebut. `poPutusan` sebelumnya hanya memeriksa pembayaran.

Tanpa penjagaan tambahan, persetujuan dapat ditarik pada pesanan yang barangnya **telah
diterima** dan yang sisanya sudah berpindah ke pesanan susulan — penerimaan yang sah
menggantung pada pesanan yang tidak lagi disetujui. Pembatalan kini ditolak bila
`tutup = true`, dengan pesan yang menunjuk jalan benarnya: revisi atau batalkan dahulu
keputusan Back Order.

## 4. Penerimaan ikut disetujui saat Back Order

`PengadaanPosApiHelper.poBackOrder`

Sesudah sisa pesanan ditutup, tidak ada lagi barang yang menyusul ke penerimaan itu —
isinya sudah final. Sebelumnya BAST tetap DRAF, sehingga petugas harus menyetujui sekali
lagi dokumen yang keputusannya baru saja ia ambil; bila lupa, penerimaan yang sah
menggantung sebagai draf sementara pesanannya sudah tertutup.

**Dijaga hak akses.** Hanya berlaku bila pengguna memang berhak menyetujui penerimaan
(`KUNCI_BAST, "approve"`). Petugas gudang tanpa hak itu tidak menyetujui dokumennya sendiri
lewat jalan belakang; baginya dokumen tetap DRAF menunggu atasan. **Jangan hilangkan
penjagaan ini.**

Parameter `bast_id` (opsional) menunjuk penerimaan yang memicu, supaya draf lain pada
pesanan yang sama tidak ikut disetujui. Pemanggil lama yang tidak mengirimnya memakai
cadangan: seluruh draf pesanan itu.

## Kolom baru pada Terima Tagihan Vendor

| Kolom | Isi |
|---|---|
| **Barang** | apakah penerimaannya sudah disetujui (`statusBast`) |
| **Termin ke** | nama terminnya |
| **Lampiran** | nama berkas yang sudah diunggah + hitungannya, merah bila Invoice belum ada |

Kolom **Barang** berbeda dari kolom **Faktur** di sebelahnya: yang satu tentang barang, yang
satu tentang tagihan. Keduanya kerap tidak sejalan karena faktur sering datang lebih dulu.

Medan `status` (SUDAH/BELUM ditagih) sebenarnya sudah lama ada di server, tetapi **hanya
dipakai sebagai penyaring** — tidak pernah tampil sebagai kolom.

**Lampiran ditanyakan sekali borongan** untuk seluruh id BAST pada halaman itu. Berkasnya
tersimpan di basis data **berbeda** (`StreamingHibernateUtil`) sehingga tidak dapat digabung
lewat join; menanyakannya per baris per slot berarti lima kueri lintas basis data untuk
setiap baris layar. Kegagalannya sengaja ditelan dan dicatat: penyimpanan berkas bisa sedang
tidak dapat dihubungi, dan daftar tagihan harus tetap tampil walau kolom lampirannya kosong.
