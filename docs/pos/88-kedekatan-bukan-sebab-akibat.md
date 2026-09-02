# 88 — Kedekatan bukan sebab-akibat

Tanggal: 2026-09-02

Dok. 87 menutup dengan menyebut dua kelas pintu yang belum tersentuh:

> Alat ini juga tidak menyentuh bentuk pintu yang lain: penolakan yang dibuka
> oleh nilai non-boolean (mis. sebuah alasan teks yang wajib diisi), atau oleh
> peran pengguna.

Batch ini mengerjakan yang pertama. Hasilnya bukan cuma kelas baru yang terjaga,
melainkan **koreksi atas penjaga yang baru dipasang kemarin.**

## 1. Percobaan pertama: tiga tuduhan, ketiganya salah

Pintu bernilai dicari dengan teknik yang sama seperti pintu boolean —
pemeriksaan "kosong", lalu penolakan dalam ~900 karakter sesudahnya. Hasilnya
tiga kandidat buntu:

```
brandId               "Brand tidak ditemukan atau bukan milik Anda."
jenis_pembayaran_id   "Cara pembayaran yang dipilih tidak valid."
jenis_tabungan_id     "Jenis tabungan yang dipilih tidak valid."
```

Ketiganya salah, dan sebabnya sama:

```java
if (!request.isNull("jenis_pembayaran_id")
        && !request.optString("jenis_pembayaran_id", "").trim().isEmpty()) {
```

Blok itu berjalan justru **ketika nilainya ADA**. Penolakan di dalamnya berbunyi
"kalau dikirim harus valid", bukan "wajib dikirim". Itu bukan pintu.

`brandId` lebih halus lagi: pemeriksaan kosongnya ada di dalam sebuah ternary
yang menghasilkan `null`, dan penolakan di dekatnya milik kondisi yang sama
sekali lain (`if (brandId != null)` → "Brand bukan milik Anda").

**Kedekatan bukan sebab-akibat.** Sebuah `status 9x` yang muncul beberapa baris
sesudah sebuah pemeriksaan tidak berarti pemeriksaan itu yang menyebabkannya.

## 2. Diganti dengan pencocokan blok

Syaratnya kini tegas: pemeriksaannya harus berada di dalam **kondisi** sebuah
`if`, dan penolakannya di dalam **badan** `if` itu — keduanya ditentukan dengan
mencocokkan kurung, bukan menghitung jarak.

10.828 blok `if (...) { ... }` diurai di pohon servlet. Sisa: **satu** pintu
bernilai, `alasan_supervisor` ("Alasan input atau pemulihan supervisor minimal 5
karakter"), dan ia **dapat** dibuka — `riwayat_penjualan_screen.dart`
mengirimnya di dua tempat.

Ketiga tuduhan tadi gugur seluruhnya.

## 3. Koreksi atas dok. 87

Cacat yang sama ternyata ada di penjaga yang dipasang kemarin. Ia memakai
kedekatan (~1.500 karakter), dan karena itu menghitung `termasuk_nonaktif`:

```java
if (!request.optBoolean("termasuk_nonaktif", false))
    c.add(Restrictions.eq("aktif", Boolean.TRUE));
```

Itu menjaga sebuah **filter daftar**, bukan penolakan. Penanda itu sama sekali
bukan pintu darurat.

Jadi angka "3 pintu" di dok. 87 benar jumlahnya tetapi salah isinya. Susunan
yang benar:

| Pintu | Bentuk | Berkas |
|---|---|---|
| `izin_harga_modal_tinggi` | penanda | `KantinHelper` |
| `pengiriman_pending` | penanda | `KantinHelper` |
| `alasan_supervisor` | nilai | `KantinHelper` |

`termasuk_nonaktif` keluar, `alasan_supervisor` masuk. Vonis akhirnya tidak
berubah — nol pintu buntu — tetapi angka yang kebetulan benar bukan angka yang
terbukti benar, dan dok. 87 memakainya sebagai alasan mengapa kelas ini boleh
menjadi gerbang. Alasan itu perlu berdiri di atas himpunan yang benar.

## 4. Kedua bentuk dibuktikan bisa gagal

Bukan cacat buatan; keduanya memakai pengirim yang sungguh ada:

| Kontrol | Tindakan | Hasil |
|---|---|---|
| penanda | cabut baris `izin_harga_modal_tinggi` dari `produk_screen.dart` | `- izin_harga_modal_tinggi (penanda)` — rc=1 |
| nilai | ganti nama `'alasan_supervisor'` di `riwayat_penjualan_screen.dart` | `- alasan_supervisor (nilai)` + pesannya — rc=1 |

Keduanya dikembalikan, kedua berkas diperiksa bersih (`git status` kosong),
alatnya kembali `rc=0`.

## 5. Yang dipelajari

**Penjaga yang baru dipasang tetap perlu diperiksa dengan disiplin yang
memasangnya.** Dok. 87 menegaskan alatnya "dibuktikan bisa gagal" — dan itu
benar: ia memang menyala pada cacat yang nyata. Tetapi terbukti bisa gagal pada
satu kasus tidak berarti setiap anggota himpunannya sudah benar. Kontrol negatif
memeriksa kepekaan, bukan ketepatan.

**Teknik yang cukup untuk satu bentuk belum tentu cukup untuk bentuk
berikutnya.** Kedekatan kebetulan bekerja pada pintu boolean karena bentuknya
seragam; pada pintu bernilai ia langsung menghasilkan tiga tuduhan palsu. Justru
mengerjakan bentuk kedua itulah yang menyingkap kelemahan pada bentuk pertama.

## 6. Yang masih terbuka

Kelas kedua dari dok. 87 §3 — **pintu yang dibuka oleh peran pengguna** — belum
disentuh. Bentuknya berbeda secara mendasar: yang membukanya bukan sesuatu yang
dikirim klien, melainkan siapa yang login, sehingga pertanyaan "adakah klien
yang bisa mengirimnya?" tidak berlaku. Pertanyaan yang setara di sana adalah
"adakah peran yang benar-benar dapat dimiliki seseorang?", dan itu menuntut
pembacaan tabel hak akses, bukan pemindaian sumber.
