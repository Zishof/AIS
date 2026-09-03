# Riwayat revisi yang ikut membawa data pribadi

`RevisiApiHelper` menyajikan riwayat Envers per baris. Aturan aksesnya, tertulis
di JavaDoc-nya sendiri:

> daftar/detail **semua user login** boleh melihat; pulihkan admin-only.

Aturan itu masuk akal ketika daftar putih entitasnya masih berisi data produk.
Ia ikut terbawa ketika daftar itu tumbuh menjadi **54 entitas** dan mulai memuat
data pribadi.

## Apa yang sebenarnya terbaca

| Entitas | Field yang ikut tersaji |
|---|---|
| `anggota` (`AnggotaKoperasi`) | `alamat`, `telp`, `hp`, `email` |
| `hotel_tamu` (`Tamu`) | `noIdentitas`, `alamat`, `telp`, `email` |

`propertiSensitif` **tidak** menutupinya — dan memang tidak seharusnya. Isinya
`pin`, `pass`, `password`, `*hash`, `*salt`, `*token*`, `*secret*`: penyaring
**kredensial**, bukan data pribadi. Untuk `anggota` ia bekerja tepat
(`pin`, `pinHash`, `pinSalt` tersamar); nomor identitas tamu bukan urusannya.

## Tidak ada lapis lain

- `revisi_*` **tidak dipetakan** di `PosApi.bolehAksesActionKantin`, jadi jatuh
  ke `return true` di ujung metode itu (lihat dok.
  [98](98-audit-77-aksi-yang-jatuh-ke-default-allow.md)).
- Token POS terbit untuk akun AIS mana pun yang kredensialnya sah.
- Satu-satunya penghalang adalah **mengetahui id barisnya**, dan id entitas di
  sini bilangan berurutan.

## Yang TIDAK terbuka

Diperiksa sebelum menyimpulkan, dan hasilnya membatasi temuan ini:

- **`revisi_jelajah` sudah admin-only sejak semula**, dengan alasan yang
  tertulis: *"menampilkan data terhapus dari seluruh toko"*. Jadi **enumerasi
  lintas baris tertutup** — yang terbuka hanyalah riwayat baris yang id-nya
  sudah diketahui.
- `revisi_pulihkan` juga admin-only.

## Perbaikannya: hak melihat riwayat mengikuti hak melihat barisnya

Aturan barunya sederhana: **boleh membuka riwayat baris yang memang boleh
dilihat.** Diterapkan di `daftar()` dan `detail()` saja — `pulihkan()` punya
prolog yang sama tetapi sudah admin-only, jadi gerbang ini mubazir di sana.

Pemetaannya sengaja **tidak menebak**. Dari 54 entitas, hanya **12** yang kode
entitasnya PERSIS sama dengan kunci menu yang terdaftar di
`EbisnisMenuKatalog.DAFTAR`; hanya itu yang dipetakan:

```
produk, grup_produk, penyedia, anggota, diskon, hotel_properti,
hotel_kamar, pesanan, pengadaan_pr, pengadaan_po, pengadaan_bast, produksi
```

Beruntung `anggota` — kasus data pribadi terpenting — ada di antaranya.

`hotel_tamu` adalah **satu-satunya** pemetaan eksplisit di luar itu: data tamu
tidak punya menu sendiri, tetapi hanya berguna bagi yang mengerjakan salah satu
layar Hotel. Ia karena itu menerima kunci hotel mana pun — lebih longgar
daripada satu kunci, jauh lebih sempit daripada "setiap pengguna login".

**42 entitas sisanya mempertahankan perilaku lama.** Memetakannya butuh
keputusan per entitas (`jenis_produk` milik menu apa? `sesi_kas`? `ujian`?), dan
menebak di sini berbahaya dua arah: kunci yang salah mengunci pengguna yang
berhak, atau — lebih buruk — meloloskan semua orang, karena
`menu.optBoolean(kunci, true)` mengembalikan **true** untuk kunci yang tidak
dikenal. Sebagian besar dari 42 itu memang data operasional, bukan data pribadi.

## Kompatibilitas

Dialog riwayat di klien (`riwayat_data_dialog.dart`) dipanggil DARI layar yang
penggunanya sudah bisa buka, jadi mereka pasti punya kunci menunya. Yang
kehilangan akses hanyalah pemanggilan API langsung oleh pengguna yang tidak
punya menu itu — persis yang dimaksud.

Dua jalan keluar dipertahankan sengaja, mengikuti pola `bolehAksesActionKantin`:
pengguna **tanpa role sama sekali** dan akun `ADMINISTRATOR` tetap lolos.
Tanpa itu, perubahan ini akan memutus akun lama yang belum pernah dikonfigurasi.

## Penjaga

`test/riwayat_revisi_hak_test.dart` (repositori Flutter), 4 uji.

Uji ketiganya yang paling penting, dan ia menjaga **cara gagal yang senyap**:
setiap kunci di pemetaan wajib benar-benar ada di `DAFTAR`. Kunci yang salah
ketik tidak akan menimbulkan galat apa pun — `optBoolean` mengembalikan `true`
untuk kunci tak dikenal, sehingga gerbangnya berdiri utuh tetapi meloloskan
semua orang. Uji negatifnya mengganti `"diskon"` menjadi `"diskonn"`, dan uji
itu jatuh sebagaimana mestinya.

Uji keempat mengunci bahwa `jelajah` tetap admin-only — batas yang membuat
temuan ini tidak lebih besar daripada semestinya.
