# 82 — Penjaga yang tidak ikut terversi

Dokumen [77](77-gerbang-oversell-dan-penjaga-field-yatim.md), [80](80-satu-saluran-peringatan-pasca-transaksi.md),
dan [81](81-lima-salinan-payload-jadi-satu.md) menyebut angka-angka sebagai jaminan
berjalan: *"21/21 lulus"*, *"13/13 lulus"*. Pemeriksaan lanjutan menemukan bahwa
**harness-nya hidup di direktori kerja sementara** — begitu sesi berakhir, jaminannya
lenyap dan dokumennya menjanjikan sesuatu yang tidak ada.

Bentuknya persis yang dikejar sepanjang [45](45-penyaring-dasbor-dan-layani-semua.md)–[81](81-lima-salinan-payload-jadi-satu.md):
**sesuatu yang diklaim tetapi tidak benar-benar tersambung.** Kali ini, kedua kalinya,
pada dokumentasi sendiri.

---

## 1. Temuan yang lebih luas: `src/test` tidak di bawah SVN

Saat memindahkan harness ke tempat yang benar, ditemukan hal yang berlaku jauh lebih
luas daripada pekerjaan ini:

| Direktori | SVN |
|---|---|
| `src/main/src` | `^/src` |
| `src/main/java` | `^/src` (cermin) |
| `src/main/docs` | `^/docs` |
| **`src/test`** | **bukan working copy — tidak terversi sama sekali** |

`src/test/java/ais/**` berisi **18 harness UAT Java** yang sudah ada sebelum pekerjaan ini
(`EbisnisMenuKatalogAksiUat`, `InventoryLedgerDomainContractUat`, `ProductionServiceUat`,
dan seterusnya). **Tidak satu pun terversi.**

Itu bukan temuan tentang pekerjaan ini, melainkan tentang repositorinya. Konsekuensinya:
seluruh harness itu hanya ada di mesin tempat ia ditulis, tidak ikut saat repositori
di-checkout di tempat lain, dan tidak punya riwayat perubahan. Dokumen yang menyebut
"TesStokUomUat 14/14" ([59](59-stok-uom-pengadaan-pr-po-bast.md)) atau "TesButir12Uat
17/17" ([60](60-metode2-kelipatan-reservasi-galat-foto-label.md)) merujuk berkas yang
tidak dapat ditemukan siapa pun selain penulisnya.

**Ini keputusan yang bukan milik saya** — memasukkan `src/test` ke SVN menyentuh tata
letak repositori dan kebiasaan tim. Yang dilakukan di sini hanya memastikan penjaga milik
pekerjaan ini berada di tempat yang **memang** terversi, dan mencatat temuannya.

---

## 2. Yang dipindahkan

Dua penjaga baru di `docs/pos/alat/` (yang berada di bawah `^/docs`), mengikuti konvensi
yang sudah ada — Python berdiri sendiri, baris `Pakai:`, melaporkan saja, kode keluar 1
bila dilanggar:

| Alat | Menjaga | Periksaan |
|---|---|---|
| [`kontrak-payload-pesanan.py`](alat/kontrak-payload-pesanan.py) | kontrak halaman Pesanan ([75](75-halaman-pesanan-tiga-celah-sunyi.md), [80](80-satu-saluran-peringatan-pasca-transaksi.md), [81](81-lima-salinan-payload-jadi-satu.md)) | 21 |
| [`aturan-stok-tiga-nilai.py`](alat/aturan-stok-tiga-nilai.py) | bentuk aturan tri-state ([73](73-stok-minus-tiga-nilai-dan-pemulihan-member.md), [77](77-gerbang-oversell-dan-penjaga-field-yatim.md)) | 9 |

Versi Java-nya tetap ada di `src/test/java/ais/action/servlet/api/` bersama 18 harness
lain — itu memang lokasi rumahnya untuk UAT Java, dan versi Java menguji **perilaku**
(memanggil `KantinHelper.wajibDiblokirKarenaStok` sungguhan). Tetapi karena direktori itu
tidak terversi, versi Python-lah yang menjadi penjaga yang benar-benar bertahan.

### 2.1 Yang bisa dan tidak bisa dijaga versi Python

`aturan-stok-tiga-nilai.py` menjaga **bentuk**, bukan perilaku — ia membaca
`KantinHelper.java` dan menuntut ketiga nilai diperlakukan berbeda.

Itu lebih lemah daripada uji perilaku, dan perlu dikatakan terus terang. Tetapi untuk
regresi yang benar-benar terjadi, ia justru tepat sasaran: r77493 adalah **satu
perbandingan boolean yang ditulis inline**, dan alat ini menolak bentuk itu secara
eksplisit —

```
== Bentuk yang MENYEBABKAN regresi r77493 tidak boleh kembali ==
  OK    syarat blokir TIDAK memakai !Boolean.TRUE.equals(...) yang menelan null
  OK    tidak ada varian berspasi dari bentuk itu
```

Kolom `Boolean` yang bermakna **tiga** nilai adalah jebakan berulang: `null` tampak
"kosong", sehingga naluri pertama menulisnya sebagai `!Boolean.TRUE.equals(x)` — dan itu
diam-diam memindahkan seluruh default ke sisi yang salah, tanpa galat kompilasi dan tanpa
uji merah.

---

## 3. Hasil uji

| Alat | Hasil |
|---|---|
| `alat/kontrak-payload-pesanan.py` | **21/21**, exit 0 |
| `alat/aturan-stok-tiga-nilai.py` | **9/9**, exit 0 |
| `PesananPayloadKontrakUat` (Java) | LULUS (21) |
| `StokMinusTigaNilaiUat` (Java) | LULUS (13) |

### 3.1 Keempatnya dibuktikan bisa GAGAL

Angka lulus dari alat yang belum pernah terbukti menolak tidak berarti apa-apa.

**Bentuk r77493 dikembalikan** ke `KantinHelper.java`, lalu:

```
  GAGAL FALSE "Wajib Diblokir" diperiksa eksplisit -> memblokir
  GAGAL syarat blokir TIDAK memakai !Boolean.TRUE.equals(...) yang menelan null
  GAGAL tidak ada varian berspasi dari bentuk itu
3 BENTUK MENYIMPANG            (exit 1)
```

dan versi Java-nya:

```
IllegalStateException: null "Ikut Pengaturan Toko" TIDAK boleh memblokir saat sakelar mati
```

**`idMember` dihilangkan** dari satu jalur JSP:

```
IllegalStateException: SETIAP pemanggil harus membawa idMember (4/5)
```

Pesan terakhir itu memperlihatkan gunanya bentuk "hitung, jangan daftar": ia menyebut
**4/5**, bukan sekadar "ada yang salah".

Seluruh berkas dikembalikan sesudahnya, dan keadaan bersihnya diverifikasi.

### 3.2 Angka pada dokumen lama adalah angka SAAT ITU

Dokumen [75](75-halaman-pesanan-tiga-celah-sunyi.md) dan [80](80-satu-saluran-peringatan-pasca-transaksi.md)
menyebut "16/16"; harness yang sama kini berisi **21** periksaan karena bertambah pada
[80](80-satu-saluran-peringatan-pasca-transaksi.md) dan [81](81-lima-salinan-payload-jadi-satu.md).
Angka lama tidak diubah — ia benar untuk keadaan saat dokumen itu ditulis. Nama
harness-nya yang diperbarui di seluruh dokumen, supaya rujukannya menunjuk berkas yang
memang ada.

---

## 4. Yang perlu diperiksa lain kali

Dua kali dalam satu rangkaian, penjaga yang dibangun untuk mencegah "diklaim tetapi tidak
tersambung" **sendiri** tidak tersambung — pertama karena hidup di direktori sementara
([79](79-enam-belas-utang-ditelusuri.md) batch), kedua karena ditaruh di direktori yang
ternyata tidak terversi.

Pelajarannya bukan "lebih teliti". Pelajarannya: **alat yang menjaga sesuatu harus ikut
diperlakukan sebagai bagian dari yang dijaga.** Pertanyaan yang seharusnya diajukan
otomatis setiap kali sebuah angka lulus dituliskan ke dokumen:

> *Kalau repositori ini di-checkout besok di mesin lain, apakah angka ini masih dapat
> dihasilkan ulang?*

Untuk 18 harness UAT Java di `src/test`, jawabannya hari ini **tidak**.
