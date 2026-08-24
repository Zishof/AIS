# 24 — Rantai DP vendor: tiga tombol mati sekaligus

POS sudah dapat memposting **Penerimaan Tagihan Vendor** dan **Pekerjaan Vendor** (dokumen 15),
tetapi tiga baris tetangganya di Draft Jurnal tidak: **DP Vendor**, **DP Pekerjaan Vendor**, dan
**Jurnal Balik DP Pekerjaan**. Angkanya tampil, tombolnya mati. Artinya pengguna dapat menjurnal
tagihan sebuah pemesanan tetapi tidak uang mukanya sendiri — separuh rantai.

Dua belas dari 35 baris Draft Jurnal punya mesin posting sebelum ini; sekarang **lima belas**.

| Baris | Dokumen | Mesin ZK | ref jurnal |
|---|---|---|---|
| DP Vendor | `PemesananPengadaanMasterAsset` | `PostingPemesananDpAction` | **null** |
| DP Pekerjaan Vendor | `SaldoAwalMasterAsset` | `PostingDpPemesananPekerjaanAction` | `DP_PEKERJAAN` |
| Jurnal Balik DP Pekerjaan | `PemesananPengadaanMasterAsset` | `PostingJurnalBalikDpPemesananPekerjaanAction` | `DP_BALIK_PEKERJAAN` |

---

## 1. Kolom `ref` adalah satu-satunya pemisah

Perhatikan baris pertama dan ketiga: **dokumennya tabel yang sama**. Satu pemesanan dapat memuat
jurnal DP Vendor *dan* jurnal baliknya sekaligus, dan yang membedakannya hanya `ref` —
yang satu **null**, yang satu `'DP_BALIK_PEKERJAAN'`.

Konsekuensinya keras: pembatalan yang lupa menyaring `ref` akan menghapus jurnal modul
tetangganya, diam-diam, pada dokumen yang sama. Karena itu ada uji khusus untuk ini —
membatalkan DP Vendor lalu memastikan jurnal Jurnal Balik masih utuh.

Jurnalnya sendiri sederhana:

| Modul | Debet | Kredit | Nilai |
|---|---|---|---|
| DP Vendor | `akunDp` | `akunUtangDp` | `getDptotal()` = DP + PPN DP |
| DP Pekerjaan | `akunDp` | `akunUtangDp`, ditimpa `penyedia.akunUtang` bila ada | `\|penagihan + PPN − pinalti\|` |
| Jurnal Balik | `akunUtangPekerjaan` | `akunUtangDp` | `\|penagihan + PPN − pinalti\|` |

Ketiganya dilewati bila salah satu akunnya belum diisi di master Jenis Pemesanan. Jurnal Balik
juga dilewati bila debet dan kreditnya **akun yang sama** — jurnal semacam itu tidak mengubah
apa pun selain meramaikan buku besar; penjagaan itu ada di layar dan dipertahankan.

DP Pekerjaan punya satu kerumitan tambahan: bila terminnya memuat jenis pajak berakun, sisi
kreditnya dipecah dua — `nilai − nilaiPajak` ke akun utang dan `nilaiPajak` ke akun pajak.
`nilaiPajak` dihitung dari **penagihan**, bukan dari nilai setelah pinalti. Disalin apa adanya.

---

## 2. Getter yang menulis saat dibaca — sekarang ada tiga

Dokumen 22 (`Closing.getDikunci`) dan 23 (`Pajak.getJenisPajakBarang`, `getSatuanKerja`) sudah
mencatat pola ini. Rantai DP menambah dua lagi:

- `PemesananPengadaanMasterAsset.getDptotal()` — menghitung ulang lalu **menyimpannya** ke field;
- `SaldoAwalMasterAsset.getJsonTermin()` — mengambil nilai dari induknya, dan **menulis `null`**
  bila pemesanan induknya bukan `byTermin`.

Perlakuannya sama seperti dua dokumen sebelumnya: penanda posting ditulis lewat SQL, lalu
entitasnya di-`evict`. Tanpa itu, flush pada dokumen berikutnya menulis balik `postingHistory`
null dan menghapus penanda yang baru dipasang — dokumennya muncul lagi sebagai draft padahal
jurnalnya sudah ada, lalu terposting dua kali.

### Yang kedua punya akibat yang tidak terduga

`getJsonTermin()` mengembalikan `null` bila PO induknya bukan `byTermin`. Tetapi kriteria dasbor
menyaring di tingkat **SQL**, langsung pada kolom `json_object` yang tetap terisi. Jadi:

> Tagihan bertermin yang pemesanan induknya BUKAN `byTermin` akan **terhitung sebagai draft**
> tetapi **tidak akan pernah dapat diposting**. Sisa drafnya tidak akan pernah turun ke nol.

Ini ditemukan lewat kecelakaan — fixture uji pertama saya kebetulan memakai PO non-termin, dan
mesinnya melewatinya tanpa galat apa pun. Keadaan itu kini menjadi kasus uji tersendiri, dan
yang menyelamatkannya adalah kalimat "N dokumen lain dilewati" dari dokumen 23: penggunanya
diberi tahu, bukan dibiarkan menatap angka yang tidak mau turun.

Tidak diperbaiki di sini. Menyelaraskan keduanya berarti memilih salah satu — menyaring dokumen
dari hitungan, atau membaca terminnya langsung dari kolom — dan keduanya mengubah dokumen mana
yang terjurnal.

---

## 3. Dua perilaku layar yang disalin apa adanya

Keduanya ada di `PostingJurnalBalikDpPemesananPekerjaanAction`, keduanya menentukan **jumlah uang**
yang masuk buku besar, dan keduanya sengaja **tidak** saya perbaiki:

1. **`merupakan_dp` tidak diperiksa per termin.** Kriteria dokumennya menuntut `formula` memuat
   `"merupakan_dp":true`, tetapi perulangan terminnya hanya memeriksa `key` dan `setuju`.
   Akibatnya pemesanan yang punya satu termin DP akan membalik **seluruh** termin yang disetujui,
   bukan hanya termin DP-nya.
2. **Penjaga duplikat per-termin tidak pernah cocok.** Ia mencari `GrupTransaksi` ber-`ref = key`,
   padahal yang ditulis selalu `REF_DP_BALIK`. Hal yang sama ada di `PostingDpPemesananPekerjaanAction`.

Keduanya mungkin disengaja, mungkin tidak. Menebaknya dari sini akan mengubah angka pembukuan
tanpa ada yang meminta — dan salahnya tidak akan berbunyi, hanya menghasilkan jurnal yang keliru.
Perilakunya dipertahankan, alasannya ditulis di Javadoc masing-masing, dan keputusannya diserahkan
kepada yang tahu maksud aslinya.

---

## 4. Satu hal yang sengaja BERBEDA dari layar

Cabang berpajak pada `PostingDpPemesananPekerjaanAction` **tidak** memeriksa akun debet/kredit
sebelum menjurnal — hanya cabang tanpa pajaknya yang memeriksa. Akun yang belum diisi karena itu
menjadi baris jurnal berakun kosong. Di jalur API dokumennya dilewati, sama seperti cabang
satunya. Jurnal berakun kosong tidak pernah menjadi maksud siapa pun, dan diamnya jauh lebih
mahal daripada dokumen yang tertunda.

Selain itu berlaku tiga perbedaan yang sama dengan dokumen 23: riwayat posting dibuat setelah
dipastikan ada yang perlu dikerjakan, penanda ditulis lewat SQL, dan baris `akunting.transaksi`
dihapus lebih dulu saat pembatalan. Jurnal yang sudah closing tidak pernah ikut terhapus.

---

## 5. Gerbang haknya

Kuncinya mengikuti **dokumen** yang dijurnal, bukan jenis jurnalnya:

| Baris | Kunci menu |
|---|---|
| DP Vendor, Jurnal Balik DP Pekerjaan | `pengadaan_po` (dokumennya pemesanan) |
| DP Pekerjaan Vendor | `pengadaan_tagihan` (dokumennya tagihan) |

Keduanya ada di `KUNCI_CRUD`, jadi hak `create`-nya benar-benar dapat dibatasi admin per peran —
bukan gerbang fail-open seperti yang diperbaiki di dokumen 20.

---

## 6. Uji

`TesPostingDpVendor` — **31 lulus, 0 gagal**.

UAT tidak punya satu pun kandidat untuk ketiga modul, dan ketiga baris
`asset.jenis_pemesanan_pengadaan_asset` yang ada **tidak berakun sama sekali**, jadi harness
merangkai jenis pemesanan berakun sendiri beserta seluruh rantai dokumennya
(jenis → pemesanan → penerimaan → tagihan) dan menghapusnya di akhir.

Yang diuji, selain ketiga tombol hidup dan jurnalnya terbentuk pada akun serta `ref` yang benar:

- membatalkan DP Vendor **tidak** menghapus jurnal Jurnal Balik pada pemesanan yang sama;
- tagihan di bawah PO non-termin terhitung sebagai draft tetapi dilewati, dan **disebut**;
- tidak ada baris `akunting.transaksi` yang tertinggal yatim;
- memposting rentang tanpa dokumen tidak meninggalkan riwayat posting kosong.

Seluruh data uji dihapus dan diverifikasi nol. Sekalian dibersihkan satu baris
`akunting.posting_history` kosong yang tertinggal dari pengujian Dana Talangan sebelumnya —
persis jenis sampah yang kini dicegah oleh "riwayat dibuat belakangan".

---

## 7. Cabang berpajak: celah uji yang ditutup belakangan

Ditambahkan **24 Agustus 2026**, setelah pemindaian menyeluruh atas seluruh 51 kelas
`Posting*`.

Uji semula memakai termin **tanpa** pajak, sehingga cabang berpajak pada DP Pekerjaan —
yang memecah kredit menjadi dua dan memanggil `ConstantValues.ambil` — tidak pernah
dilewati sama sekali. Harness lulus 31/31 tanpa menyentuhnya.

Kini ada termin berpajak (persen 10) dan dua penjaga baru: jurnalnya harus punya **tiga**
baris transaksi (satu debet, dua kredit) dan akun pajaknya harus terpakai. Harness menjadi
**33 lulus, 0 gagal**.

Kode fase-1/fase-2 pada jalur ini juga diseragamkan dengan modul BAST dan Penyusutan.
Perlu dicatat jujur: itu **penyeragaman defensif, bukan perbaikan cacat**. Pemeriksaan
langsung menunjukkan `ConstantValues.ambil` TIDAK menutup sesi pemanggilnya — yang menutup
adalah `AssetUtil.ambilDataAkun`, dan jalur ini tidak memanggilnya. Yang benar-benar
bertambah di sini adalah cakupan ujinya.

---

## 8. Yang masih terbuka

Lima belas dari 35 baris Draft Jurnal kini punya mesin posting di POS. Tiga baris lain bukan
pekerjaan posting massal (Jurnal Umum dan Posting HPP punya layarnya sendiri; Closing adalah
penguncian). Sisa **17 baris**:

| Baris | Jumlah |
|---|---|
| Mahasiswa | 7 |
| Siswa | 6 |
| Jurnal Penyusutan | 1 |
| Fix Aset dan Aset dalam Pekerjaan (jurnal saat BAST) | 2 |
| Gaji | 1 |

Selain itu, **jurnal yang dihasilkan ketiga mesin ini belum pernah diperiksa akuntan.** Yang
terbukti adalah akun kedua sisinya sesuai rantai yang disalin dari layar dan angkanya dihitung
dengan rumus yang sama — bukan bahwa perlakuan akuntansinya tepat.
