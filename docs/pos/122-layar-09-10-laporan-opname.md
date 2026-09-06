# 122 — Layar 09-10 (Laporan Opname): dari celah menjadi lengkap

> **KOREKSI (2026-09-06, sesudah tangkapan layar aplikasi lama dibuka).** Versi pertama
> dokumen ini menyatakan layar 09 = daftar sesi opname dan layar 10 = rinciannya. **Keduanya
> keliru.** Setelah gambar aplikasi lamanya benar-benar dilihat:
>
> - **Layar 09 "LAPORAN STOK OPNAME"** adalah daftar **datar** — satu baris per produk per
>   tanggal, berkolom `TGL.OPNAME`, `#KODE`, `NAMA BARANG`, `SAT.`, `STOK KOMP.`, `STOK FISIK`,
>   `SELISIH`, `HRG.POKOK`, `TOTAL HARGA`, dengan **satu angka total** di kanan bawah dan tombol
>   `CETAK`.
> - **Layar 10 "Mencetak Laporan Opname"** adalah **hasil cetak** layar 09 (pratinjau Report
>   Designer), bukan rincian per dokumen.
>
> Layarnya sudah diperbaiki: tampilan bawaannya kini datar per produk (`mode=produk`), dengan
> total seluruh periode; tampilan per sesi tetap ada sebagai pilihan, dan penelusuran satu sesi
> tetap ada — keduanya tambahan di luar paritas, bukan padanan layar 10.
>
> **Pelajarannya, dan ini yang penting:** paritas fungsional tidak dapat disimpulkan dari NAMA
> layar lama. "Laporan Opname" dan "Mencetak Laporan Opname" terdengar persis seperti daftar dan
> rinciannya. Saya membangun seluruh layar, menulis dokumen, dan memverifikasi tiga lapis data
> — seluruhnya SETARA — di atas dugaan yang tidak pernah diperiksa terhadap gambarnya.
> Angka yang setara tidak menyelamatkan bentuk yang salah.

**Tanggal:** 2026-09-06
**Sifat:** catatan perubahan + hasil verifikasi.
**Kode:** r86049 (`SalesInventoryOpnameHelper`, `SalesInventoryOpnameTenant`,
`SalesInventoryApiDispatcher`, `TenantRbac`).
**Pendahulu:** [120](120-uat-paritas-data-tuntas.md), [121](121-manual-pengguna-inventory-sales.md).

---

## 0. Catatan tentang r86049

Keempat berkas di atas **ter-commit oleh sesi lain** pada 18:51 sebagai r86049, dengan **pesan
kosong** — working copy `C:\opt\AIS\ais\src\main\java` dipakai bersama beberapa sesi, dan sesi
lain menyapu perubahan yang belum saya commit. Kodenya utuh; yang hilang hanya penjelasannya.
Dokumen ini menggantikan pesan commit itu.

Pelajaran operasional yang sudah tercatat sebelumnya dan terbukti lagi: pada working copy ini,
`svn status` yang kosong **tidak** berarti tidak ada perubahan — bisa berarti perubahan Anda
sudah dibawa pergi orang lain. Periksa `svn log -r <rev> -v`, jangan `svn status` saja.

---

## 1. Celahnya, dan mengapa tidak terlihat

Doc 120 mencatat 46 dari 48 layar tercakup, dengan 09-10 sebagai celah. Penyebabnya ternyata
berlapis tiga, dan **tiap lapis sendirian tidak menghasilkan galat apa pun** — hanya angka nol:

**Lapis 1 — gerbang tanpa handler.** `PosApi` sudah punya cabang izin

```java
if (action.startsWith("si_stock_count_")) {
    // Layar 9-10 reuse Stok Opname existing -- gate paritas dgn layar so_* lama.
    return menu.optBoolean("stokopname", true);
}
```

tetapi **tidak ada satu pun handler** `si_stock_count_*` di dispatcher. Gerbangnya menjaga pintu
yang tidak pernah dibangun.

**Lapis 2 — entitas tidak dapat melihat schema tenant.** Karena tidak ada aksi tenant, layar
09-10 jatuh ke `StokOpnameScreen` POS beserta aksi `so_*`. Aksi-aksi itu membaca lewat Hibernate,
dan 1.569 entitas AIS mematok `@Table(schema=...)`. Hasilnya: **461 dokumen opname ada di
`cmnmedika.stok_opname` dan terbaca NOL.** Tidak ada galat, tidak ada peringatan — layar kosong
yang terlihat seperti "memang belum ada data".

**Lapis 3 — dua ejaan yang tidak pernah bertemu.** `TenantRbac.area()` memetakan aksi ke area
izin, dan ia hanya mengenal ejaan Indonesia:

```java
if (a.startsWith("inventory") || a.startsWith("stok") || a.startsWith("opname"))
```

sedangkan gerbang di `PosApi` memakai ejaan **Inggris** (`si_stock_`, `si_stock_count_`).
Akibatnya `area()` mengembalikan `null` dan **setiap** aksi `si_stock_*` dijawab
`TENANT_ACCESS_DENIED` pada usaha ber-tenant — bukan karena perannya kurang. Cacat ini laten
sejak awal; ia baru terpapar ketika `si_stock_count_list` menjadi aksi `si_stock_*` pertama yang
benar-benar dipanggil.

### Penyebab lain yang sempat menutupi semuanya: peran karangan

Sebelum ketiga lapis di atas terlihat, layar 09-10 menolak dengan *"Toko tidak diketahui."*
Sebabnya bukan opname sama sekali: akun UAT saya diberi peran karangan (`OWNER`,
`SALES_KELILING`), sedangkan `EbisnisActorContextResolver` **hanya meresolusi toko pada cabang
`ROLE_PEMILIK`** (`pemilik_sales_inventory`); cabang ADMIN sengaja tidak memanggilnya.

Produknya sudah menyediakan peran yang benar — `SalesInventoryHelper` memakainya untuk menyemai
akun UAT — dan saya melewatkannya. Sesudah `muklis` dipindahkan ke `pemilik_sales_inventory`:

| akun | actorType | tokoId |
|---|---|---:|
| muklis | `PEMILIK_SALES_INVENTORY` | **1** |
| sales | `SALES_KELILING` | — |
| demo | `ADMIN` | — (memang begitu rancangannya) |

Toko tidak teresolusi untuk ADMIN **bukan cacat**: administrator tidak mengoperasikan satu toko.
Aksi ber-`toko_id` menerimanya dari muatan, disisipkan `ApiClient` dari pemilih toko di bilah atas.

---

## 2. Yang ditambahkan

| berkas | isi |
|---|---|
| `SalesInventoryOpnameTenant` | `SELECT` jalur tenant: daftar sesi, rincian, kepala |
| `SalesInventoryOpnameHelper` | `si_stock_count_list` + `si_stock_count_detail`, dua jalur |
| `SalesInventoryApiDispatcher` | pendaftaran kedua aksi |
| `TenantRbac` | `area()` mengenal ejaan `stock`, bukan hanya `stok` |
| `laporan_opname_screen.dart` | layar 09 (datar per produk, bawaan) + Per Sesi + penelusuran |
| `cetak_util.dart` | jahitan uji `POS_TEST_PDF_DIR` supaya hasil cetak dapat dibuktikan |
| `app_shell.dart` | tujuh titik pendaftaran menu |

### Tiga keputusan yang pantas dijelaskan

**Lebih dan kurang dipisah, bukan hanya selisih bersihnya.** Sesi yang kelebihan 50 pada satu
produk dan kekurangan 50 pada produk lain berselisih bersih **nol** — padahal justru sesi itu yang
paling perlu diperiksa. Menampilkan bersihnya saja menyembunyikan tepat kasus yang dicari laporan
ini.

**Jendela bawaan 365 hari, bukan 30.** Opname jarang harian. Jendela 30 hari seperti layar
persediaan akan tampak kosong pada sebagian besar data sungguhan, dan layar kosong terbaca sebagai
"tidak ada data" padahal hanya jendelanya yang sempit.

**Jalur cetak diberi jahitan uji, dan itu disengaja.** `Printing.layoutPdf` membuka dialog cetak
milik sistem operasi — bukan permukaan Flutter, sehingga tidak dapat dipotret. Tanpa jalan lain,
jalur cetak menjadi satu-satunya bagian aplikasi yang tak pernah terverifikasi otomatis, padahal
**sembilan dari 48 layar legacy adalah hasil cetak** (10, 13, 16, 26, 29, 36, 42, 46, 48). Jahitannya
satu `String.fromEnvironment` yang hanya hidup bila `POS_TEST_PDF_DIR` diisi; build produksi tidak
pernah mengisinya.

**Total dihitung dari rincinya, tidak disimpan di kepala.** Menyimpannya berarti dua sumber
kebenaran yang dapat berselisih diam-diam sesudah satu baris rinci disunting.

### Bentuk kedua jalur berbeda, dan itu bukan penggantian prefiks

`koperasi.stok_opname` adalah baris **datar** per produk (`waktuopname`, `produk`, `selisih`,
`toko`) tanpa kepala dokumen. Model tenant memisahkan kepala (`stok_opname`) dari rinci
(`stok_opname_detail`), dan lingkupnya lewat `gudang.toko_id`, bukan `toko`. Jalur legacy karena
itu **mengelompokkan** baris datarnya per tanggal × toko agar kolom keluaran kedua jalur sama dan
berurutan sama. Konsekuensinya `id` sesi legacy adalah `MIN(id)` barisnya — penunjuk, bukan kunci
dokumen — dan itu dinyatakan di JavaDoc-nya supaya tidak ada yang mengira sebaliknya.

---

## 3. Verifikasi

`banding-opname-api.py` mengadu **tiga lapis**, dan ketiganya perlu:

```
1. si_stock_count_list  vs  cmnmedika.stok_opname
   jumlah sesi opname            API=461      sumber=461      SETARA
   baris rincian                 API=1771     sumber=1771     SETARA
   total selisih LEBIH           API=211554   sumber=211554   SETARA
   total selisih KURANG          API=194728   sumber=194728   SETARA

2. si_stock_count_detail  vs  cmnmedika.stok_opname_detail   (sesi terbesar, 389 baris)
   baris rincian                 API=389      sumber=389      SETARA
   SUM stok sistem               API=67261    sumber=67261    SETARA
   SUM stok fisik                API=2465     sumber=2465     SETARA
   SUM selisih                   API=-64796   sumber=-64796   SETARA

3. si_stock_count_list  vs  DATAOPN.DBF
   1843 baris mentah  −13 produk tak ada di STOK.DBF  −59 kembar (tanggal, produk)
   baris rincian vs DBF          API=1771     sumber=1771     SETARA
```

**Mengapa lapis 1 saja tidak cukup:** kueri yang salah tetapi konsisten akan lulus.
**Mengapa lapis 3 saja tidak cukup:** ia tidak menyatakan apakah LAYAR-nya yang membacanya — dan
justru di situ cacat `si_receivable_list` dulu bersembunyi (doc 120 §3).

### Alat ukurnya sendiri sempat salah, lagi

Versi pertama `banding-opname-api.py` membandingkan **jumlah baris mentah** DBF dan melaporkan
selisih 72 yang terlihat meyakinkan. Seluruhnya terjelaskan oleh dua aturan penerimaan importir
yang memang terdokumentasi (produk tanpa induk di `STOK.DBF`; penjaga idempotensi
`(opname, produk)`). Pembandingnya yang tidak meniru aturan itu — bukan datanya yang berselisih.

Ini kali keenam dalam UAT ini sebuah "selisih besar" ternyata cacat alat ukur (doc 120 §4).
Aturannya tetap: **saat pembanding melaporkan selisih besar, periksa pembandingnya lebih dulu.**

### Penangkapan layar

Uji integrasi `uat_48_layar_test.dart` kini memotret **layar 09** (daftar) dan **layar 10**
(rincian) terpisah, dan dijalankan sebagai `muklis` — bukan `demo` — supaya tokonya teresolusi.

Dua kekeliruan yang sempat terjadi pada langkah ini, keduanya pantas dicatat:

1. **Menekan `InkWell` pertama** membuka menu lipat sidebar, bukan baris tabel — dan tetap
   menghasilkan PNG besar yang terlihat sah. Barisnya kini dicari lewat nomor dokumennya.
2. **Langkah rinciannya sempat terlewat DIAM-DIAM.** Hitungan "14 berhasil, 0 gagal" terlihat
   sama sehatnya dengan "15 berhasil, 0 gagal" kecuali seseorang menghitungnya. Lewatnya kini
   dicatat sebagai GAGAL, bukan didiamkan.

---

## 4. Yang tersisa

1. **Perbandingan berdampingan dengan `inventory.exe`** — satu-satunya penghalang UAT 100% yang
   tersisa. Sisi barunya sudah lengkap 48 dari 48.
2. Layar sesi sales (SPJ, Nota, Biaya, Rekonsiliasi) belum berdata uji — tampil kosong, bukan
   cacat tampilan.
3. Medan legacy tanpa rumah: `HARGAASLI` / `DISCOUNT` / `DISCOUNT2` pada BELI.DBF, dan
   `produk.stok_legacy`.
4. Sesudah UAT 100%: varian `sales-inventory` → `https://ebisnis.id/ebisnis`.
