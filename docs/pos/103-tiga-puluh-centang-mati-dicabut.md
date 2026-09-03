# Tiga puluh centang mati dicabut

Dok. [96](96-centang-yang-tidak-mengubah-apa-pun.md) menemukan enam kunci di
`KUNCI_CRUD` yang tidak pernah ditegakkan peladen — 30 baris centang
(6 kunci x 5 aksi) di grid peran `TbmroleAction` yang tidak mengubah apa pun.
Dokumen itu **sengaja tidak mengubahnya**, menyebutnya keputusan produk.

Setelah risikonya benar-benar diukur, penundaan itu ternyata **terlalu
hati-hati**: perubahannya inert. Keenamnya kini dikeluarkan.

## Empat hal yang dibuktikan sebelum mencabut

Bukan "sepertinya aman", melainkan diperiksa satu per satu:

1. **Tidak ada gerbang yang memakainya.** Sudah ditetapkan di dok. 96: keenam
   kunci hanya muncul di katalog dan di `ApotikEmedikSeedHelper` (data contoh),
   tidak di satu pemanggilan `bolehAksi*` pun. Karena `bolehAksi()` untuk kunci
   di luar `KUNCI_CRUD` jatuh ke `aksiLegacy`, dan tidak ada yang memanggilnya
   dengan kunci ini, **tidak ada perilaku yang berubah**.

2. **Visibilitas menu tidak ikut tercabut.** `defaultObj()` menyusun `menu` dari
   `DAFTAR` dan `crud` dari `KUNCI_CRUD` secara terpisah. Keenamnya tetap di
   `DAFTAR`, jadi menunya tetap tampil persis seperti sebelumnya.

3. **Pengaturan yang terlanjur tersimpan tidak hilang.**
   `KantinHelper.ebisnisRoleMenuSimpan` bersifat **aditif**: ia mengambil objek
   `crud` yang sudah ada lalu memperbarui hanya kunci yang disebut payload dan
   ada di `KUNCI_CRUD`. Kunci di luar itu **tidak pernah dibuang**. Jadi bila
   suatu saat gerbangnya dibangun dan kuncinya dikembalikan, setelan lama masih
   ada di tempatnya.

4. **Klien tidak membacanya sebagai kunci CRUD.** `beranda_apotik_screen.dart`
   dan `pos_help.dart` memakai keenam nama itu lewat `bolehMenuVarianBaru`,
   yaitu jalur **visibilitas menu** (`aksesMenu`) — bukan `crud`. Tidak ada
   pembacaan `crud.apotik_narkotika` atau `crud.emedik_*` di klien.

## Yang tertinggal di kode: alasannya, bukan sekadar ketiadaannya

Menghapus enam nama dari sebuah daftar tidak menjelaskan apa pun kepada orang
berikutnya, yang kemungkinan besar akan menambahkannya kembali dengan niat baik.
Karena itu blok komentar menggantikan tempatnya di `EbisnisMenuKatalog`, memuat
alasan per kunci, catatan bahwa visibilitas dan setelan tersimpan tidak
terpengaruh, serta rujukan ke dok. 96 dan penjaganya.

## Penjaga: invariannya kini MUTLAK

`test/kunci_crud_ditegakkan_test.dart` bertambah menjadi 4 uji.

Daftar pengecualian yang dulu memuat enam kunci sekarang **kosong** — setiap
kunci di `KUNCI_CRUD` wajib benar-benar diperiksa peladen, tanpa perkecualian.
Peta kosong itu sengaja dipertahankan, bukan dihapus: bila kelak ada kunci yang
memang tidak dapat digerbangi, tempatnya di sana **beserta alasannya**, bukan
lolos diam-diam.

Dua uji baru menjaga arah sebaliknya:

- keenam kunci **wajib tetap di luar** `KUNCI_CRUD` (mencantumkannya kembali
  memunculkan centang mati lagi);
- keenamnya **wajib tetap di `DAFTAR`**. Ini penting: mencabut CRUD tidak boleh
  ikut menghapus entri menunya. Bila itu terjadi, menunya lenyap dari sidebar
  **setiap peran** — kerusakan yang jauh lebih besar daripada centang mati.

## Penjaga keempat sempat palsu, dan sebabnya persis pola yang sama

Uji "masih terdaftar sebagai menu" mula-mula memeriksa apakah nama kuncinya
muncul di berkas katalog. Uji negatifnya **lolos**: menghapus baris
`DAFTAR.add(new Entri(MODUL_EMEDIK, "emedik_deposit", …))` tidak menjatuhkannya.

Sebabnya: blok komentar yang **baru saja ditulis di pekerjaan ini** menyebutkan
keenam nama itu, sehingga pencarian nama selalu berhasil. Penjaga itu menjaga
komentar buatannya sendiri.

Sekarang yang dicocokkan adalah **baris pendaftarannya**
(`DAFTAR.add(new Entri(MODUL_…, "kunci"`), dan uji negatifnya jatuh sebagaimana
mestinya. Ini pengulangan ketiga dari pola yang sama dalam beberapa hari
terakhir — mencari yang gampang dicari, bukan yang benar-benar dimaksud —
dan ketiganya baru ketahuan lewat uji negatif, bukan lewat lulusnya uji.

## Yang tetap TIDAK diubah, dan mengapa

Pola `adminGlobal = tbmuser.getPedagang() == null` di 13 lokasi jalur POS lama
**tidak disentuh**. Berbeda dengan centang mati di atas, mengubahnya **mengubah
perilaku**: akun yang hari ini diperlakukan admin karena tidak terikat Pedagang
akan tertolak. `EbisnisActorContextResolver` sudah menyebut pola itu "asumsi
berbahaya … ~30 lokasi" dan **sengaja** hanya memperbaikinya pada permukaan
`si_`, membiarkan jalur POS lama demi kompatibilitas. Memigrasikannya adalah
keputusan yang memerlukan daftar akun terdampak, bukan tambalan.

Perbedaannya sederhana dan layak diingat: **mencabut centang yang tidak pernah
berfungsi tidak mengubah perilaku siapa pun; mencabut wewenang yang hari ini
berfungsi mengubahnya.** Yang pertama boleh dikerjakan sendiri, yang kedua tidak.
