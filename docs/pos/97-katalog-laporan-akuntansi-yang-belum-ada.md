# Sebelas laporan akuntansi yang terdaftar tetapi belum ada

Batch lanjutan sesudah doc 96, difokuskan ke modul keuangan dan akuntansi.

---

## 1. Dua sapuan yang tidak berbuah, dan kenapa itu penting disebut

Dua sudut diperiksa lebih dulu dan **keduanya bersih** — dicatat supaya tidak disapu ulang:

**Penjagaan `closing` pada 24 mesin posting.** Pembatalan posting yang menyentuh jurnal di
periode tertutup akan merusak integritas akuntansi. Pemeriksaan pertama tampak menemukan dua
mesin tanpa penjaga (`PostingTransaksiHarianAction`, `PostingPemesananPekerjaanAction`) —
tetapi itu cacat cara mengukurnya: blok metode diekstrak dengan `awk` sampai baris `\t}`
pertama, yang terpotong pada metode berkurung bersarang. Diperiksa langsung, keduanya punya
penjaga (`grup.getClosing() != null` dan `and closing is null` di SQL-nya). **Dua puluh empat
dari dua puluh empat aman.**

**Keseimbangan debet/kredit di `CommonAkunting.saveTransaksi`.** Penulis jurnal bersama
menjumlahkan `totalDebet` dan `totalKredit` lalu menyimpannya tanpa memeriksa keduanya sama,
sementara jalur jurnal manual (`NewUiJournalService.post`) menolak yang tidak balance. Itu
asimetri yang menarik — **dan sudah digarap sesi lain** di doc 72 §1, yang menutupnya dengan
"struktur berisiko teridentifikasi; menunggu bukti data". Tidak diduplikasi.

## 2. Katalog laporan POS: 11 kunci tanpa pelaksana

`LaporanKatalogData` mendaftar 184 entri laporan; `LaporanKantinUtil` melaksanakannya lewat
rantai `if ("kunci".equals(r))`. Sebelas kunci tidak punya cabang sama sekali — dan
kesebelasnya **satu keluarga utuh**, berawalan `lk_`:

| Kunci | Judul di katalog |
|---|---|
| `lk_keu2`, `lk_keu12`, `lk_keu2th` | Neraca / Laba Rugi / Arus Kas — 2 Periode, 12 Bulan, 2 Tahun |
| `lk_neracalajur` | Neraca Lajur (Kertas Kerja) |
| `lk_trial` | Neraca Saldo / Trial Balance |
| `lk_bukubesar`, `lk_bukubesartgl` | Buku Besar, Buku Besar per Tanggal |
| `lk_jurnal` | Jurnal Harian |
| `lk_aruskas12`, `lk_aruskas31` | Arus Kas — 12 Bulan, 31 Hari |
| `lk_dashakun` | Rasio, Grafik, Laba Ditahan & Proyeksi Kas |

Seluruhnya berada di satu kategori bernama **"Laporan Keuangan Resmi — Komparatif
(Akuntansi)"**, dan uraiannya berbunyi meyakinkan — "Resmi dari jurnal akuntansi".

## 3. Bukan galat, melainkan janji yang belum ditepati

Penutup rantai dispatch bukan pelemparan galat:

```java
} else {
    H.status = "soon"; H.message = "Laporan ini sedang disiapkan dan akan tersedia pada ...";
}
```

Jadi kesebelasnya tidak merusak apa pun. Yang terjadi: pengguna memilih "Buku Besar" atau
"Neraca Lajur" dari daftar, menjalankannya, lalu mendapat pesan bahwa laporannya sedang
disiapkan.

**Yang membuatnya layak dicatat**: status `"soon"` hanya ditangani di satu tempat —
`LaporanKantinPdf`, yang mencetak pesannya ke dalam PDF. Tidak ada penanganan di sisi klien,
dan **katalognya sendiri tidak memuat satu pun penanda**: kata "soon", "segera", atau
"disiapkan" tidak muncul di `LaporanKatalogData`. Kesebelas entri itu tampil persis seperti
173 entri lain yang bekerja.

Perbaikannya murah — menambahkan keterangan di uraian entri, atau bendera yang dibaca layar
supaya ditampilkan berbeda. Tetapi mengubah teks yang dilihat pengguna adalah keputusan
pemilik produk, bukan pembetulan rujukan, jadi tidak dilakukan sepihak.

## 4. Yang TIDAK terpengaruh: panduan staf yang sudah diserahkan

Karena laporan-laporan itu bernama sama dengan yang ditulis di panduan keuangan
([70-panduan-laporan-keuangan-an-nahl.pdf](70-panduan-laporan-keuangan-an-nahl.pdf)), perlu
dipastikan panduan itu tidak menjanjikan sesuatu yang belum ada.

Tidak. Panduan itu menjelaskan **Pintu A** — layar ZK `Akuntansi > Laporan-Laporan
Keuangan` — sedangkan sebelas entri ini milik katalog POS/kantin, jalur kode yang berbeda.
Pemeriksaan langsung: "Buku Besar" muncul di 22 berkas Java, "Neraca Lajur" di 7, "Trial
Balance" di 5 — jadi laporan-laporan itu memang terimplementasi di jalur akuntansi.
Hanya salinan katalog POS-nya yang masih placeholder.

Kalau pemeriksaan ini tidak dilakukan, temuan di atas mudah dibaca sebagai "panduan yang
sudah diserahkan ke staf menjanjikan laporan yang tidak ada" — kesimpulan yang salah, dan
jauh lebih mengkhawatirkan daripada keadaan yang sebenarnya.

## 5. Alatnya

[alat/katalog-laporan-tanpa-pelaksana.py](alat/katalog-laporan-tanpa-pelaksana.py)
membandingkan kunci katalog dengan seluruh sumber. Batasnya ditulis di kepalanya: kunci
tanpa pelaksana **bukan otomatis cacat** — sebagian memang placeholder yang disengaja.
Yang layak ditanyakan adalah apakah katalognya menyebutkan hal itu.
