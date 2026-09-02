# Seribu anggaran yang menjadi satu

Cacat id 19 digit yang ditemukan di modul Anggaran ([35-anggaran-id-negatif.md](35-anggaran-id-negatif.md))
ternyata **belum tertutup di modul Pengadaan**. Dua layar JSP mengambil daftar
anggaran lewat `pengadaan_anggaran_cari`, dan aksi itu mengirim id sebagai
**angka JSON**.

## Sebab pokoknya

Id `rab.workspace` dibangkitkan sebagai:

```java
final Long mustid = -(Long.MAX_VALUE
        - (Long.parseLong(RabUtil.DEFAULT_SATUAN_KERJA + "" + tahunWorkspace + "" + s)));
```

— selalu **19 digit**, jauh di atas `Number.MAX_SAFE_INTEGER` (2^53 ≈ 9,007 × 10^15).
Pada magnitudo 9,2 × 10^18, jarak antar-bilangan `double` adalah **1.024**.

Dan baris anggaran bersaudara diberi id **berurutan** (`nilaiMinInDb + n` di
`RabImporter`). Artinya sampai **1.024 anggaran berturut-turut runtuh menjadi
satu nilai yang sama** begitu melewati JavaScript.

## Yang terukur, bukan diperkirakan

Dua id workspace bersebelahan (selisih **satu**) dijalankan melalui Node:

```
LAMA  baris 1 : <a onclick="prPilihAnggaranX(-9223272037652164000)">
LAMA  baris 2 : <a onclick="prPilihAnggaranX(-9223272037652164000)">
  -> dua anggaran BERBEDA menghasilkan onclick identik? true
```

Perhatikan angkanya: **-9223272037652164000 bukan salah satu dari kedua id itu.**
JavaScript merender `double` dengan bentuk terpendek yang bolak-balik konsisten,
jadi yang terkirim adalah bilangan bulat **ketiga** yang tidak pernah ada di
basis data.

## Gejalanya di lapangan

Karena `prSimpan` mencari `session.get(Workspace.class, id)`, id yang tidak ada
akan **ditolak terang-terangan**:

> Anggaran yang dipilih tidak ditemukan.

Pengguna baru saja memilih anggaran itu dari daftar yang muncul di layar, lalu
diberi tahu bahwa anggarannya tidak ada. Nyaris mustahil ditebak sebabnya, dan
sangat mudah disalahartikan sebagai kerusakan data.

Yang lebih berbahaya adalah kasus keduanya: bila pembulatan kebetulan mendarat
pada id yang **sah**, PR dibebankan ke **anggaran yang salah** — tersimpan
rapi, tanpa galat, tanpa tanda apa pun.

## Perbaikan

Mengikuti pola yang sudah baku di `anggaran.jsp`: identitas dipegang sebagai
**teks**.

| Tempat | Perubahan |
|---|---|
| `PengadaanPosApiHelper.cariAnggaran` | tambah `idTeks` di samping `id` |
| `PengadaanPosApiHelper` detail PR | tambah `workspace_idTeks` |
| `pengadaan_pr/index.jsp` | `a.idTeks` untuk onclick, nilai elemen, dan pencocokan; `workspace_idTeks` saat membuka PR lama |
| `pengadaan_bulk/index.jsp` | `<option value="a.idTeks">` |

Bentuk **angkanya sengaja dipertahankan**. Klien Flutter tidak terpengaruh
(`int` Dart 64-bit pada build native Windows/Android), dan membuangnya hanya
akan memutus klien yang sudah benar.

Sisi simpan **tidak perlu diubah**: `Long.valueOf((request.get("workspace_id") + "").trim())`
sudah melewati String, jadi tepat begitu nilai yang masuk tepat.

## Satu jebakan kutip yang nyaris lolos

Tambalan pertama menghasilkan JavaScript **rusak**:

```js
+ RND + '('' + a.idTeks + '')">'
```

Kutip tunggal tidak dapat berdiri polos di dalam string berkutip tunggal — literalnya
tertutup lebih awal. Ini tidak terlihat dari hasil `svn diff` yang "berhasil"; yang
menemukannya adalah membaca diff itu **baris demi baris**.

Perbaikannya memakai entitas HTML `&quot;`, yang tidak perlu di-escape sama sekali
di dalam sumber JS **maupun** di dalam atribut HTML berkutip ganda:

```js
+ RND + '(&quot;' + a.idTeks + '&quot;)">'
```

## Sapuan yang membatalkan dua dugaan

Dicatat supaya tidak diulang:

- **`Transaksi.randomValue` dan `TemplateTransaksi.randomValue`** memakai
  `(long)(Long.MAX_VALUE * random.nextDouble())` — bilangan 18-19 digit, mirip
  sekali dengan pola cacat ini. Ditelusuri: **tidak dibaca di mana pun**, baik di
  Java, JSP, maupun ZUL. Bukan cacat.
- **JSON `formula`** pada Kas Kecil/Kas Besar memuat id workspace. Ditelusuri:
  seluruhnya dibangun di **Java** (`array.toString()`), tidak pernah lewat
  JavaScript. Aman.
- `AnggaranKeuanganUtil` dan `UangMukaApiHelper` sudah memancarkan id workspace
  sebagai `String.valueOf(...)` sejak semula. Aman.

Dari enam titik yang memancarkan id workspace ke API, hanya **dua** yang belum
diperbaiki — dan keduanya di Pengadaan.

## Penjaga

`test/anggaran_id_teks_kanal_jsp_test.dart` (repositori Flutter), 4 uji.

Uji pertamanya **tidak bergantung pada working copy AIS**: ia membuktikan sebab
pokoknya dengan aritmetika `double` di Dart, sehingga alasan seluruh berkas tetap
terbaca walaupun tiga penjaga lintas-repo di bawahnya dilewati di CI Flutter yang
berdiri sendiri. Ini menjawab kelemahan pola `if (!f.existsSync()) return;` yang
pernah membuat satu berkas uji hijau tetapi hampa
([84-sapuan-hak-akses-tombol.md](84-sapuan-hak-akses-tombol.md)).

Dibuktikan dengan uji negatif: JSP dikembalikan ke bentuk angka, dua uji jatuh
dengan pesan yang dimaksud, lalu berkasnya dipulihkan dan diverifikasi
byte-identik.

## Catatan kompilasi

`PengadaanPosApiHelper.java` **tidak dapat dikompilasi dengan JDK 7** — sudah
memakai `java.util.Base64` di tiga tempat (baris 2488, 2637, 4167) sejak sebelum
pekerjaan ini. Kompilasi verifikasi memakai `jdk1.8.0_202`, sesuai `JAVA_HOME`
yang dipakai Tomcat UAT. Galat Base64 yang muncul saat memakai JDK 7 **bukan**
akibat perubahan ini.
