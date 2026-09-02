# Audit aksi yang jatuh ke default-allow

Dok. [97](97-gerbang-yang-menyaring-orang-yang-salah.md) menutup dengan
menyatakan bahwa aksi yang lolos gerbang awal **belum diaudit satu per satu**,
dan angka "77" di sana tidak dapat dipercaya. Dokumen ini menyelesaikannya.

## Angkanya lebih dulu diperbaiki

`PosApi.bolehAksesActionKantin` berakhir `return true`. Yang jatuh ke situ:

| Cara menghitung | Hasil |
|---|---|
| "77" pada dok. 97 | **salah** — artefak skrip pertama |
| seluruh `"x".equals(action)` di luar metode gerbang | 72 |
| dikurangi `login`, `logout`, `i18n_kamus` (memang bebas token) | **69** |
| bentuk dispatch `") {"` saja | 66 |

Selisih 69 vs 66 sudah direkonsiliasi: tiga aksi `draft_jurnal_*` di-dispatch
dalam satu cabang gabungan (`||`), bukan cabang sendiri-sendiri. Ketiganya
diperiksa dan **aman** — diteruskan ke `DraftJurnalApiHelper.proses`, yang
memuat enam pemeriksaan hak.

## Parser divalidasi lebih dulu, hasilnya baru dipakai

Tiga audit sebelumnya di sesi ini menghasilkan angka palsu karena langkah ini
dilewati. Kali ini parser diuji lebih dulu terhadap **empat kasus yang sudah
dibaca manual** — `satuan_kerja_hapus` (tanpa `Tbmuser`, tanpa gerbang),
`ebisnis_role_menu_simpan` (ada keduanya), `hak_akses_list` (tanpa `Tbmuser`),
`pengguna_toko_list` (ada `Tbmuser`) — dan hasilnya tidak dicetak sama sekali
bila salah satu meleset. Lulus 4/4.

Hasilnya, dari 66 aksi yang terbaca:

| | jumlah |
|---|---:|
| menerima `Tbmuser` **dan** memuat pola gerbang | 33 |
| menerima `Tbmuser`, tanpa pola gerbang | 22 |
| **tidak menerima `Tbmuser`** — mustahil menjaga per-pengguna | 9 |
| deklarasi tak terbaca | 2 |

"Tanpa pola gerbang" **bukan** berarti tanpa gerbang: gerbangnya bisa berada di
panggilan bersarang. Karena itu yang dilaporkan sebagai temuan di bawah hanya
yang **dibaca langsung**, bukan yang sekadar masuk hitungan.

## Yang diperbaiki: tiga daftar keuangan anggota

`mutasi_tabungan_list`, `mutasi_hutang_list`, dan `pembantu_piutang_list`
gagal di **kedua** lapis:

- dua yang pertama bertanda tangan `(JSONObject, JSONObject)` — **tidak
  menerima `Tbmuser` sama sekali**, jadi tidak mungkin menegakkan izin;
- ketiganya **tidak menyebut `toko` sama sekali**. Satu-satunya penyaring
  adalah `id_anggota` yang **opsional**.

Artinya permintaan tanpa `id_anggota` mengembalikan mutasi tabungan, hutang, dan
piutang **seluruh anggota lintas toko** — lengkap dengan kode, nama, waktu, dan
nominalnya. Dan token POS diterbitkan kepada akun AIS mana pun yang
kredensialnya sah (`terbitkanToken` → `doAutoLogin`, tanpa batasan peran).

Ketiganya kini dipetakan ke kunci menu **`anggota`**, mengikuti
saudara-saudaranya (`jenis_anggota_`, `tipe_anggota_`) dan mengikuti perbaikan
`satuan_kerja_` di dok. 97. Layarnya memang tab di dalam menu Anggota
(`tab_mutasi_tabungan.dart`, `tab_mutasi_hutang.dart`,
`tab_pembantu_piutang.dart`).

## Yang SENGAJA tidak disentuh

**Aksi referensi bersama.** `uom_list`, `jenis_produk_list`,
`kebijakan_retur_list`, `akun_list` juga tanpa `Tbmuser`, tetapi dipakai dari
BEBERAPA layar sekaligus — `uom_list` dipanggil keranjang kasir *dan* formulir
produk. Memetakannya ke satu kunci akan mematikan pemilih satuan kasir yang
tidak punya hak menu Produk. Isinya pun data referensi (satuan, jenis produk),
bukan data anggota atau transaksi. Perlu kunci gabungan atau pemisahan aksi —
pekerjaan tersendiri, bukan tambalan.

**Pola `adminGlobal = getPedagang() == null`.** Ditemukan di 13 lokasi, antara
lain `mutasiStokSimpan` yang memperlakukan setiap akun tanpa Pedagang sebagai
"admin/manager". Ini **bukan temuan baru**: `EbisnisActorContextResolver`
sudah menyebutnya "asumsi berbahaya … ~30 lokasi" dan menegaskan
*"toko == null TIDAK PERNAH berarti admin"* — tetapi perbaikannya **sengaja
dibatasi pada permukaan `si_`**, dengan jalur POS lama tidak diubah demi
kompatibilitas. Mengubah ke-13 lokasi itu sepihak berpotensi mengunci akun admin
yang hari ini berfungsi. Itu keputusan migrasi, bukan perbaikan bug.

Perbedaannya dengan yang diperbaiki di atas layak ditegaskan: pola `adminGlobal`
adalah gerbang **lemah tetapi ada**; tiga daftar keuangan anggota itu **tidak
ada gerbangnya sama sekali**, di kedua lapis.

## Penjaga

`test/gerbang_awal_posapi_test.dart` (repositori Flutter), 4 uji.

Uji pertamanya mengunci **asumsi dasarnya sendiri**: bahwa ujung
`bolehAksesActionKantin` masih `return true`. Bila kelak ujungnya dibalik
menjadi fail-closed, uji itu jatuh — memaksa seluruh berkas ini ditinjau ulang,
alih-alih diam-diam menjaga sesuatu yang sudah tidak relevan.

Dibuktikan dengan dua uji negatif: mencabut satu aksi dari pemetaan, dan
memetakannya ke kunci yang salah (`kasir` alih-alih `anggota`). Keduanya
menjatuhkan uji yang dimaksud, dan berkasnya dipulihkan byte-identik.

## Penjaga sendiri yang ikut jatuh

Menambahkan tiga baris ke blok `anggota` memundurkan baris `return` melewati
jendela 160 karakter yang dipakai penjaga `satuan_kerja_` dari dok. 97 —
uji itu jatuh karena alasan yang **salah**, bukan karena regresi.

Jendelanya diperlebar menjadi 600 dan diuji-negatif ulang. Pelajarannya: penjaga
berbasis "jendela sesudah penanda" rapuh terhadap blok yang tumbuh; jendelanya
harus lapang sejak awal, atau penandanya diikat ke sesuatu yang tidak bergeser.
