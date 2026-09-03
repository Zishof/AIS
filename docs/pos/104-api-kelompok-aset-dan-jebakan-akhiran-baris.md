# API Kelompok Aset — dan satu diff 6.737 baris yang nyaris ter-commit

Lanjutan doc 103. Kelompok Aset sudah bisa dilayani API, sehingga layar natifnya tidak perlu
membuka `kelompok_asset.zul` di browser. Dokumen ini juga mencatat kesalahan saya sendiri yang
tertangkap tepat sebelum masuk repositori.

---

## 1. Yang dibuat

`ais/action/servlet/api/KelompokAsetApiHelper.java`, disalurkan lewat `PosApi`:

| Aksi | Isi |
|---|---|
| `kelompok_aset_list` | Daftar Kelompok Aset + keempat pemetaan akunnya, lengkap dengan kode/nama akun dan nama satuan kerja |
| `kelompok_aset_akun_simpan` | Simpan satu bidang akun (pembelian / penyusutan / biaya / hpp) |

Empat keputusan yang layak dicatat:

- **Bidang berupa daftar putih.** Nama kolom tidak pernah diambil dari permintaan. Klien
  mengirim `bidang` yang harus salah satu dari empat kata; pemetaan ke setter dilakukan di
  server. Tanpa itu, klien bisa mengarahkan penyimpanan ke kolom lain.
- **Nama akun & satuan kerja diambil sekali untuk semua baris**, bukan per entri — daftar dengan
  puluhan kelompok × empat bidang akan menjadi ratusan kueri kalau tidak.
- **Akun bukan-daun diperingatkan, bukan ditolak.** Akun induk tidak menampung transaksi, jadi
  memilihnya menghasilkan jurnal salah tempat. Tetapi menolak simpan akan mengunci pengguna yang
  datanya sudah terlanjur begitu dan hanya ingin memperbaiki baris lain, jadi hasilnya membawa
  `peringatan` alih-alih gagal.
- **Teks JSON rusak dibaca sebagai kosong, bukan dilempar.** Satu baris rusak tidak boleh membuat
  seluruh daftar gagal dimuat.

Aksi simpannya didaftarkan di `MutasiIdempotenEBisnisUtil` karena akan mengalir lewat antrean
`MasterOffline` di klien. Ia menyimpan SELURUH daftar satu bidang sekaligus, bukan menambah
baris, jadi kiriman ulang menghasilkan keadaan yang sama — didaftarkan demi konsistensi dengan
master lain, bukan karena replay-nya berbahaya.

Formatnya tetap sama persis dengan yang dibaca/ditulis `AssetUtil.reloadDataFormula` di layar ZK,
jadi kedua sisi saling terbaca dan perubahan dari salah satu tidak merusak yang lain.

## 2. Diff 6.737 baris yang nyaris ter-commit

Suntingan ke `PosApi.java` saya lakukan lewat Python dengan
`io.open(p, 'w', encoding='utf-8', newline='\n')` — cara yang sama yang dipakai sepanjang sesi
ini dan selalu benar sebelumnya.

Pada berkas ini ia salah. `PosApi.java` berakhiran **CRLF**; menulis dengan `newline='\n'`
mengubah seluruh berkas menjadi LF. Perubahan sebenarnya 6 baris, tetapi diffnya:

```
6737 baris dihapus
6743 baris ditambah
```

Yang membuat ini berbahaya bukan diffnya, melainkan tiga hal lain:

1. **Tidak ada yang gagal.** Kompilasi bersih, pemeriksa bersih, `svn status` cuma `M`.
2. **Akhiran barisnya campuran.** 7.004 dari 7.025 baris CRLF, sisanya LF. Jadi "perbaikan"
   borongan LF→CRLF pun akan salah pada 21 baris.
3. **Ada proses cermin** yang menyalin `java/` ↔ `src/` dan meng-commit dalam hitungan detik.
   Jeda antara membuat kekeliruan dan kekeliruan itu masuk repositori sangat pendek.

Cara memperbaikinya juga bukan yang pertama terpikir. `svn revert` akan menghapus suntingan sesi
lain bila ada. Jadi urutannya: bandingkan berkas kerja dengan HEAD **setelah kedua sisi
dinormalkan ke LF**, pastikan satu-satunya beda isi adalah 6 baris milik saya, lalu bangun ulang
berkasnya dari **byte HEAD** ditambah sisipan ber-CRLF. Sesudahnya diffnya 6 tambah, 0 hapus.

Yang menangkapnya cuma satu kebiasaan: membaca diff sebelum commit, bukan mempercayai bahwa
suntingan kecil menghasilkan diff kecil.

Catatan bagi yang menyunting berkas AIS lewat skrip: **periksa akhiran baris berkasnya lebih
dulu.** Di repositori ini keduanya bercampur — `LaporanKantinUtil.java` dan
`LaporanKatalogData.java` murni LF, `PosApi.java` campuran dengan mayoritas CRLF. Menyunting byte
(`open(p,'rb')` lalu ganti byte) tidak pernah punya persoalan ini.

## 3. Sisa

Layar Flutter-nya belum dibuat; `posting_akun_perbaikan.dart` masih membuka
`kelompok_asset.zul`. API-nya sudah siap dipakai, dan `master_asset.zul` tetap menunggu
keputusan pemilik seperti dicatat doc 103.
