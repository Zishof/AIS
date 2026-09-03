# 103 — Hitungan yang melaporkan pekerjaan yang tidak terjadi

Tanggal: 2026-09-03

Dok. 102 menyusutkan butir A.5 dari "risiko deadlock + semantik yang tidak dapat
diputuskan" menjadi satu hal: **ringkasan impor Excel melaporkan "diperbarui"
untuk baris yang tidak berubah.** Batch ini mengerjakannya.

## 1. Perubahannya

`KantinHelper.produkImporExcelKomitSatuPercobaan` sebelumnya menghitung setiap
baris yang cocok sebagai `diperbarui++`. Impor 8.000 baris yang tidak mengubah
apa pun tetap berbunyi "Diperbarui: 8.000".

Sekarang nilai lama difoto sebelum setter dijalankan — pola yang sudah dipakai
kode itu untuk `stokLama` dan `kodeLama` — lalu dibandingkan sesudahnya:

```java
if (tandaSebelum == null
        || !tandaSebelum.equals(tandaTanganImporProduk(p))) {
    diperbarui++;
} else {
    tidakBerubah++;
}
```

`tandaSebelum == null` berarti barisnya baru (jalur pemulihan konflik kunci
unik), dan itu memang perubahan — karena itu tetap dihitung `diperbarui`.

## 2. Mengapa sembilan field, dan mengapa itu aman

Kekhawatiran lama: *"perbandingan yang terlalu longgar akan MELEWATI perubahan
yang sah, dan itu kehilangan data yang sunyi."*

Benar untuk himpunan field yang dipilih sembarang. Di sini himpunannya **bukan
pilihan** — ia persis field yang disetel jalur impor ini: `kode`, `nama`,
`barcode`, `kunciUnik`, `hargaJual`, `hargaBeli`, `jenisProduk`, `pemasok`,
`satuan`. Tidak ada field lain yang ditulis, jadi tidak ada perubahan sah yang
dapat terlewat.

Relasi dibandingkan lewat `getId()`, bukan objeknya: getter itu aman terhadap
proxy Hibernate dan tidak bergantung pada identitas objek antar-sesi. Stok tidak
ikut — perubahannya ditangani jalur opname yang sudah punya penjaga
`if (selisih != 0)`.

## 3. Dua akibat yang harus ikut dibereskan

**Total.** `hasil.put("total", dibuat + diperbarui + dilewati)` akan menyusut
diam-diam begitu satu kategori dipecah. `tidakBerubah` ikut dijumlahkan — ia
baris yang diproses, bukan yang dilewati.

**Pembaca.** Menambahkan field respons tanpa pembaca adalah persis cacat yang
dikejar `field-tanpa-pembaca.py`, dan alat itu memang akan menandainya. Jadi
klien ikut diubah: `impor_excel_produk_screen.dart` membaca `tidakBerubah`,
mereset-nya tiap impor baru, dan menampilkannya sebagai kartu "Tidak berubah"
di samping "Diperbarui". Kedua penjaga diperiksa hijau sesudahnya.

Tanpa langkah kedua, angka "Diperbarui" hanya akan tampak menyusut tanpa
penjelasan, dan sisanya hilang tanpa jejak di layar — memperbaiki satu laporan
keliru dengan membuat laporan keliru yang lain.

## 4. Bukti

* `javac -source 1.7` bersih.
* `dart analyze` pada layar yang disunting: `No issues found!`
* Uji baru `impor_hitungan_diperbarui_test.dart` (3 periksaan) lulus, dan
  **dibuktikan dapat merah**: baris pembacanya dicabut, suntingan diverifikasi
  lebih dulu (`tersisa = 0`), hasilnya `+2 -1` dengan pesan *"field baru tanpa
  pembaca = angka yang hilang tanpa jejak"*.
* Diff Java: **51 baris**, tiga di antaranya penghapusan yang memang dimaksud.

Angka terakhir itu perlu disebut. Percobaan pertama menghasilkan **549 baris
berubah** karena penulisan byte-mode menormalkan EOL pada berkas ber-EOL
campuran — jebakan yang sama seperti dok. 89 §5. Diperiksa dulu bahwa tidak ada
pekerjaan sesi lain yang hilang (3 baris terhapus, semuanya jangkar saya), lalu
diulang dengan penyambungan yang mempertahankan EOL tiap baris.

## 5. Satu uji merah yang bukan milik saya

`flutter test` seluruh suite: **749 lulus, 1 gagal**.

Yang gagal `riwayat_revisi_hak_test.dart` — *"pemetaannya tidak menebak: kodenya
harus kunci menu sungguhan"*, mengharapkan `si_customer` ada di
`EbisnisMenuKatalog`. Kunci itu tidak ada, baik di working copy maupun di HEAD.
Uji itu membaca katalog menu dan sebuah helper hak akses; ia tidak menyentuh
satu pun berkas yang saya ubah.

Itu pekerjaan menu sesi paralel yang sedang berjalan. Dilaporkan di sini, tidak
disentuh — memperbaiki pekerjaan orang lain yang sedang berlangsung lebih
mungkin merusak daripada menolong.

## 6. Yang dipelajari

**Memecah satu hitungan menyentuh tiga tempat.** Counter-nya, totalnya, dan
pembacanya di klien. Mengerjakan yang pertama saja menghasilkan laporan yang
justru lebih membingungkan daripada sebelumnya — angka yang menyusut tanpa ada
yang menjelaskan ke mana perginya.

**Kesalahan revert saya sendiri.** Setelah kontrol negatif, saya memakai
`git checkout --` pada seluruh berkas, yang ikut menghapus perubahan saya yang
belum di-commit. Tidak ada pekerjaan orang lain yang hilang dan perubahannya
diterapkan ulang dari skripnya — tetapi kontrol negatif seharusnya memulihkan
**baris yang dicabut**, bukan mengembalikan berkasnya ke HEAD.
