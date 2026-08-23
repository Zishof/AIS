# Menu aksi baris "…" — Desktop, Android, JSP, dan ZKoss

Deretan tombol ikon pada kolom Aksi diringkas menjadi **satu tombol "…"** yang membuka
menu berisi **ikon dan label**.

## Mengapa

Baris tabel CRUD memuat empat sampai tujuh tombol ikon berjajar. Tiga masalahnya nyata:

1. **Kolom Aksi memakan lebar** yang seharusnya milik data — pada PO lebarnya 240px.
2. **Ikon tanpa label hanya dapat ditebak.** Tooltip tidak muncul di layar sentuh, dan
   sebagian besar POS dipakai lewat sentuhan.
3. **Target sentuhnya terlalu rapat**, sehingga salah tekan mudah terjadi. Berbahaya
   ketika salah satunya Hapus.

## Tiga kanal, tiga mekanisme, satu perilaku

| Kanal | Berkas | Cara pakai |
|---|---|---|
| Desktop & Android | `apps/ebisnis/lib/widgets/aksi_baris_menu.dart` | `AksiBarisMenu(aksi: [AksiBaris(...)])` |
| JSP | `webapp/js/aksi_baris.js` | `aksiBarisMenu([...])`, atau tandai `<td data-aksi-baris>` |
| ZKoss | `src/ais/ui/util/MenuAksiBaris.java` | `MenuAksiBaris.pasang(toolbar)` satu baris |

### JSP — dua jalan masuk

Halaman JSP merangkai barisnya sebagai string JavaScript, jadi `aksiBarisMenu([...])`
mengembalikan potongan HTML, bukan simpul DOM.

Sebagian halaman dirender di **server** lewat scriptlet, sehingga barisnya sudah menjadi
HTML sebelum sampai ke peramban dan tidak dapat memanggil fungsi itu. Untuk halaman
seperti itu cukup tandai selnya:

```html
<td class="text-center" data-aksi-baris>
    <button ... title="Ubah"><i class="fas fa-edit"></i></button>
    <button ... title="Hapus"><i class="fas fa-trash"></i></button>
</td>
```

Deretan tombolnya diubah sendiri di sisi peramban. Label diambil dari `title`, ikon dari
elemen `<i>`, dan **tombol aslinya disimpan tersembunyi lalu diklik ulang** — sehingga
penangan klik yang sudah terpasang tetap hidup, termasuk yang dipasang lewat
`addEventListener`. Penyisipan baris lewat AJAX ikut diamati, sebab banyak tabel diisi
sesudah halaman selesai dimuat.

**Tidak memakai dropdown Bootstrap.** Halaman modul dipakai bersama oleh beberapa kerangka
(`index.jsp`, `ebisnis.jsp`, `erp.jsp`, `dashboard.jsp`) yang tidak memuat berkas pendukung
yang sama. Menggantungkan diri pada Bootstrap berarti menu ini hidup di sebagian kerangka
dan mati di sebagian lain, tanpa gejala sampai ada yang mengklik.

Panelnya **dipindahkan ke `<body>`** saat dibuka. Banyak tabel berada di dalam
`table-responsive`; menu yang tinggal di dalam `<td>` akan terpotong oleh overflow-nya.

### ZKoss — satu baris pada renderer

```java
Hbox toolbar = new Hbox();
... buat dan pasang tombol seperti biasa ...
MenuAksiBaris.pasang(toolbar);
toolbar.setParent(row);
```

Tombol aslinya dipindah ke wadah tersembunyi yang tetap berada di pohon komponen, lalu
diklik ulang lewat `Events.postEvent`. Menyalin listener-nya ke butir menu akan melahirkan
salinan kedua yang lambat laun berbeda dari aslinya.

## Perbedaan yang disengaja antar kanal

| | POS & JSP | ZKoss |
|---|---|---|
| Aksi yang sedang tidak berlaku | tampil **redup** | mengikuti `setVisible` apa adanya |

Di POS dan JSP, aksi yang tidak berlaku diredupkan supaya pengguna tahu aksi itu ada namun
belum dapat dipakai; menu yang isinya berubah-ubah membuat orang tidak dapat menghafal letak.

Di ZKoss berbeda: `setVisible` pada renderer hampir selalu berarti **hak akses**
(`button.setVisible(edit)`), bukan "sedang tidak berlaku". Meredupkannya akan memberi tahu
pengguna tentang kewenangan yang memang bukan miliknya.

## Yang sengaja TIDAK diubah

**Sel beraksi tunggal.** Menu di situ menyembunyikan satu-satunya aksi di balik klik
tambahan. Delapan layar POS dibiarkan: Laporan, Laporan Transaksi, Posting Toko,
Notifikasi, Saldo Voucher, Aturan Diskon, Kas Jurnal, Sesi Kasir.

**Satu pengecualian:** Log Error ikut diubah meski aksinya tunggal, karena ikon tong
sampahnya menghapus **seketika tanpa konfirmasi dan tanpa label**. Menu memberinya nama dan
menjadikan penghapusan sebuah pilihan, bukan refleks.

**Sel yang memuat keterangan, bukan aksi.** Dua penanda tetap berdiri di baris karena
tabelnya tidak punya kolom lain yang memuatnya:

- `"dibatalkan"` pada Setoran Pajak
- tanda `"sudah masuk stok"` pada BAST

Meleburnya ke dalam menu berarti petugas harus membuka menu satu per satu hanya untuk tahu
status tiap baris.

**Bukan sel aksi sama sekali.** Beranda Anggota dan Opname Scan tersaring oleh survei karena
punya dua tombol dalam satu sel, tetapi itu penambah/pengurang jumlah di sekitar kotak
isian. Satu sel di Pembayaran Online Mahasiswa adalah bilah alat kaki tabel.

## Jebakan yang ditemui

**`dart format` memecah baris dan menggagalkan uji kontrak.**
`master_offline_kontrak_test` mencocokkan **teks sumber** secara harfiah. Ketika formatter
memecah `daftarCacheDulu('jenis_produk_list'` menjadi dua baris, penjaganya gagal padahal
perilakunya tidak berubah. Ujinya kemudian dibuat abai spasi — perbaikan di akar. Pelajaran:
`flutter analyze` saja tidak cukup, `flutter test` harus ikut dijalankan.

**Komentar bermuatan koma mematahkan pemisah argumen.** Transformator JSP memecah daftar
pada koma tingkat teratas; komentar `// Cetak dokumen: pratinjau lebih dulu, mencetak
menyusul.` membuatnya menghasilkan potongan ngawur. Sel seperti itu ditolak dan dikerjakan
dengan tangan.

**Awalan penugasan ikut terhapus.** Saat mengganti sel pada `_daftar_transaksi.jsp` dan
`toko.jsp`, awalan `htmlTbody +=` dan `htmlList +=` ikut terbuang sehingga blok JS-nya rusak.
Ditemukan oleh pemeriksa sintaks yang membandingkan blok `<script>` terhadap keadaan
sebelumnya, bukan oleh mata.

## Cakupan

| Kanal | Selesai |
|---|---|
| Desktop & Android | 24 layar (8 layar beraksi tunggal sengaja dilewati) |
| JSP | 33 halaman (3 sel dikecualikan, bukan aksi baris) |
| ZKoss | **331 kelas** — 4 ditolak, 2 sisa dikerjakan manual |

## Sapuan ZKoss — 331 kelas

Penyisipan satu baris tepat sebelum `wadah.setParent(induk)`, lewat transformator yang
**sengaja konservatif**. Sebuah wadah hanya disentuh bila:

1. dideklarasikan `Hbox <v> = new Hbox();`
2. dipasangi **minimal dua** tombol (`x.setParent(<v>)`)
3. dipasang ke induknya **tepat sekali** dalam jangkauan dekat
4. titik sisipnya **tidak berada di dalam baris komentar**

Wadah yang variabelnya dipakai ulang dilewati: menyisipkan di sana berisiko mengenai
pemakaian berikutnya, dan wadah yang sudah berisi menu akan dibungkus dua kali.

### Empat berkas ditolak, dan apa yang disingkapnya

| Berkas | Sebab |
|---|---|
| `PenilaianKpiHelper` | variabel bernama sama tetapi bertipe `Toolbar`, bukan `Hbox` |
| `RpsObeAction` | wadahnya **di luar cakupan** pada titik sisip |
| `ItemTreeAction` | sisipan masuk **ke dalam blok yang dikomentari** |
| `RiwayatKenaikanGajiBerkalaUntukPegawaiAction` | idem |

Regex tidak memahami **cakupan** maupun **komentar** Java. Dua sebab pertama selalu
berujung galat kompilasi, jadi kompilator menangkapnya. Sebab ketiga **tidak selalu**:
pernyataan hidup di tengah kode mati dapat lolos kompilasi dan hanya jadi panggilan sia-sia
yang membingungkan pembaca berikutnya.

Karena itu seluruh sisipan disapu ulang mencari **tetangga berupa komentar**. Berkas
keempat ditemukan lewat pemeriksaan itu, bukan lewat kompilator. Transformatornya kemudian
diperbaiki agar melewati titik sisip di dalam komentar — perbaikan itu langsung membuka
tiga berkas sekerabat (`DdcItemTree`, `KategoriItemTree`, `UdcItemTree`) yang sebelumnya
ikut tertutup oleh penolakan `ItemTreeAction`.

### Jebakan proses: daftar berkas dari `svn status`

Daftar berkas untuk kompilasi dan commit mula-mula disusun dari `svn status ... M`. Working
copy ini **dipakai beberapa sesi sekaligus**, sehingga daftar itu ikut memuat dua berkas
yang sedang disunting sesi lain (`DraftJurnalApiHelper`, `DraftJurnalRingkasanUtil`) —
nyaris ikut ter-commit atas nama sapuan ini.

Keduanya dikeluarkan setelah diperiksa **tidak memuat `Hbox` maupun tombol sama sekali**.
Pelajarannya: daftar commit harus disusun dari **jejak perubahan sendiri**, bukan dari
status working copy.

> `svn update` di tengah pekerjaan juga sempat **menimpa dua sisipan**. Sesudah setiap
> update, keberadaan `MenuAksiBaris` pada tiap berkas diperiksa ulang.

### Verifikasi

- 327 berkas dikompilasi **bersama-sama**, `EXIT=0`. Kompilasi per-berkas tidak cukup:
  `-sourcepath` membaca cermin `src/`, sehingga berkas yang belum disalin ke sana
  memberi hasil lolos yang palsu.
- Cermin `java/` dan `src/` identik untuk seluruhnya.
- Akhiran baris tiap berkas dibandingkan dengan versi repo: 12 berkas memang **sudah
  campur sejak semula**, dan jumlah LF-nya tidak berubah oleh sisipan ini.

### Sisa

Dua berkas (`RpsObeAction`, `PenilaianKpiHelper`) dikerjakan manual — keduanya gagal karena
cakupan dan tipe variabel, yang memang tidak dapat diselesaikan regex.

88 wadah lain dilewati karena variabelnya dipakai ulang. Masing-masing perlu dibaca sendiri
untuk memastikan sisipan mengenai pemakaian yang tepat.
