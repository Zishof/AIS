# Kontrak Sumber Tagihan DetailBiaya

Dokumen ini menjelaskan cara mesin billing memilih `SettingBiaya` dan membaca
`DetailBiaya`, termasuk kompatibilitas data lama. Tujuannya adalah mencegah
regresi ketika logika prioritas setting diperketat.

## Ringkasan insiden 2026-09-02

Gejala:

- nominal sudah dibuat melalui layar Pengaturan Tagihan;
- profil mahasiswa dan periode terlihat sesuai;
- layar Pembayaran Mahasiswa tetap menampilkan `Belum ada tagihan`;
- kasus terlihat pada mahasiswa berstatus asli Nonaktif.

Status Nonaktif bukan akar masalah. Bila konfigurasi
`mahasiswa_dengan_status_non_aktif_bisa_melakukan_pembayaran_seperti_status_aktif`
aktif, status yang dipakai untuk pencarian tagihan adalah Aktif. Status asli
tetap ditampilkan pada UI agar informasi akademik tidak berubah secara semu.

Akar masalahnya adalah filter prioritas setting. Filter tersebut sebelumnya
hanya menerima `DetailBiaya` yang mempunyai salah satu relasi berikut:

1. `setting_biaya_detail -> setting_biaya`;
2. `detail_setting_biaya -> setting_biaya`;
3. `detail_biaya.setting_biaya`.

Baris yang dibuat oleh layar Pengaturan Tagihan legacy dapat mempunyai ketiga
relasi tersebut bernilai `null`. Barisnya ada dan nominalnya benar, tetapi query
baca membuangnya. Kondisi ini membuat pesan `Belum ada tagihan` menyesatkan.

Ada dua lapis kegagalan yang harus diperbaiki bersama. Pertama, pembatas sumber
menolak baris all-null ketika sebuah `SettingBiaya` modern ditemukan. Kedua,
bila sama sekali tidak ada `SettingBiaya` modern yang cocok, daftar item dari
setting menjadi kosong dan kode lama menambahkan kondisi SQL selalu salah.
Memperbaiki lapis pertama saja belum menyelesaikan kasus Pengaturan Tagihan
standalone. Karena itu perbaikan juga memusatkan keputusan filter item pada
`PembayaranUtilHelper.batasiItemBiayaPembacaan(...)`.

Perbaikan dipusatkan di:

`PembayaranUtilHelper.batasiPembacaanDetailBiayaKeSettingTerpilih(...)`

Semua query baca pada UI admin dan layanan pembayaran/H2H wajib memakai method
tersebut. Jangan membuat salinan criteria sendiri.

## Dua model sumber tagihan

Sistem harus mendukung dua bentuk data secara bersamaan.

### 1. Tagihan terkelola SettingBiaya

Data baru idealnya mempunyai jejak induk yang lengkap:

```text
SettingBiaya
  -> DetailSettingBiaya
       -> DetailBiaya
```

Untuk tagihan khusus mahasiswa, jalurnya dapat melalui
`SettingBiayaDetail`. Mesin prioritas memilih tepat satu `SettingBiaya` yang
paling sesuai dengan profil mahasiswa. Baris yang menunjuk setting lain harus
ditolak walaupun item dan nominalnya sama.

### 2. Tagihan langsung legacy

Layar Pengaturan Tagihan versi lama membuat `DetailBiaya` secara langsung.
Pada data seperti ini, tiga relasi sumber dapat seluruhnya `null`:

```text
detail_biaya.setting_biaya        IS NULL
detail_biaya.detail_setting_biaya IS NULL
detail_biaya.setting_biaya_detail IS NULL
```

Baris tersebut tetap sah untuk dibaca. Keamanannya bukan berasal dari relasi
setting, tetapi dari filter profil rinci pada query produksi, antara lain:

- jenis pembayaran;
- tahun akademik dan semester;
- angkatan;
- jenjang dan prodi;
- program;
- status awal dan status pembayaran efektif;
- semester mulai;
- kewarganegaraan;
- kelas, tempat tinggal, dan parameter tambahan bila fitur terkait aktif.

Bukti pembentuk data ini berada pada
`NewDetailBiayaExcelAction.getDefaultDetailBiaya(...)`. Saat editor bulanan
tidak sedang terikat ke sebuah `SettingBiaya`, method tersebut membentuk
`DetailBiaya` dan mengisi seluruh atribut profil, tetapi memang tidak memanggil
`setSettingBiaya`, `setDetailSettingBiaya`, atau `setSettingBiayaDetail`.
Sebaliknya, `getDefaultDetailBiayaSettingBulanan(...)` adalah jalur modern yang
menyimpan `settingBiaya` dan `detailSettingBiaya`. Jadi bentuk all-null bukan
dugaan berdasarkan gejala UI, melainkan konsekuensi eksplisit dari dua jalur
pembentukan yang masih hidup berdampingan di kode produksi.

Jangan memperbaiki masalah ini dengan menebak induk lalu menulis relasi pada
baris lama ketika dibaca. Satu `ItemBiaya` dapat digunakan oleh beberapa
setting dengan cohort dan prioritas berbeda. Migrasi relasi hanya aman bila
memiliki prosedur terpisah yang membuktikan tepat satu setting cocok untuk
setiap baris dan menyediakan laporan konflik untuk kasus ambigu.

## Algoritma pemilihan

Urutan algoritma untuk mahasiswa lama adalah:

1. Validasi mahasiswa, semester, dan jenis pembayaran.
2. Hitung tahun akademik serta tahap pembayaran.
3. Ambil status akademik asli dari `HistoryStatusMahasiswa`.
4. Bentuk status pembayaran efektif melalui
   `PembayaranUtilHelper.statusMahasiswaPembayaranEfektif(...)`.
5. Cari tagihan khusus mahasiswa.
6. Cari `SettingBiaya` cohort dengan prioritas terkecil yang masih mempunyai
   kandidat cocok; di dalam prioritas yang sama pilih kandidat paling spesifik.
   Hasil tahap ini boleh `null` untuk tagihan standalone legacy.
7. Bila setting ditemukan, ambil daftar `ItemBiaya` dari setting terpilih.
   Daftar ini membatasi baris modern saja; sumber legacy all-null tidak dibuat
   dari daftar item modern dan tetap dinilai lewat profil langsung.
8. Bangun query `DetailBiaya` atau `PengaturanPembayaranBulanan`.
9. Terapkan batas sumber dengan helper kanonis.
10. Terapkan batas item dengan `batasiItemBiayaPembacaan`.
11. Terapkan seluruh filter profil mahasiswa.
12. Kurangi pembayaran yang sudah terjadi, deduplikasi item, lalu tampilkan.

Calon mahasiswa dan layanan H2H mengikuti prinsip yang sama. Perbedaan hanya
pada sumber profil dan aturan pembayaran bulanan.

## Kontrak batas sumber

Untuk `SettingBiaya S` yang terpilih, satu baris `DetailBiaya D` boleh dibaca
bila memenuhi salah satu kondisi berikut:

```text
D.settingBiayaDetail != null
  AND D.settingBiayaDetail.settingBiaya = S

OR

D.settingBiayaDetail = null
  AND D.detailSettingBiaya != null
  AND D.detailSettingBiaya.settingBiaya = S

OR

D.settingBiayaDetail = null
  AND D.detailSettingBiaya = null
  AND D.settingBiaya = S

OR

D.settingBiayaDetail = null
  AND D.detailSettingBiaya = null
  AND D.settingBiaya = null
```

Cabang terakhir adalah kompatibilitas legacy. Cabang ini tidak berarti semua
baris tanpa relasi boleh tampil. Query pemanggil tetap wajib menerapkan seluruh
filter profil.

Jika relasi yang lebih spesifik tersedia tetapi menunjuk setting lain, baris
harus ditolak. Contoh: `settingBiayaDetail` tidak boleh diabaikan hanya karena
`detailBiaya.settingBiaya` kebetulan menunjuk setting yang dipilih.

### Ketika setting terpilih tidak ada

Jika mesin prioritas menghasilkan `S = null`, query tidak boleh dibiarkan tanpa
pembatas sumber. Hanya bentuk berikut yang boleh lewat:

```text
D.settingBiayaDetail = null
  AND D.detailSettingBiaya = null
  AND D.settingBiaya = null
```

Setelah itu seluruh filter profil tetap diterapkan. Pembatas ini penting karena
filter item mempunyai cabang khusus untuk sumber legacy. Tanpa pembatas
all-null, baris milik setting modern lain yang kebetulan mempunyai profil sama
dapat ikut tampil.

Keputusan item adalah sebagai berikut:

| Setting terpilih | Daftar item | Kondisi item |
|---|---|---|
| Ada | Berisi | baris modern wajib `IN`; legacy all-null tetap lewat |
| Ada | Kosong | baris modern ditolak; legacy all-null tetap lewat |
| Tidak ada | Berisi | hanya legacy all-null; daftar item tidak memperluas sumber |
| Tidak ada | Kosong | hanya legacy all-null; profil menjadi penentu akhir |

## Query baca dan query tulis berbeda

Ini adalah invariant terpenting.

### Query baca

Query baca boleh menerima baris legacy tanpa relasi. Gunakan:

```java
criteria = PembayaranUtilHelper
        .batasiPembacaanDetailBiayaKeSettingTerpilih(criteria, settingBiayaTerpilih);
criteria = PembayaranUtilHelper
        .batasiItemBiayaPembacaan(criteria, itemBiayas);
```

Setelah itu tambahkan semua filter profil. Method ini saat ini digunakan oleh:

- `ais.action.master.helper.PembayaranUtilHelper`, jalur mahasiswa lama;
- `ais.action.master.helper.PembayaranUtilHelper`, jalur calon mahasiswa;
- `ais.action.ws.util.PembayaranUtil`, jalur mahasiswa lama/H2H;
- `ais.action.ws.util.PembayaranUtil`, jalur calon mahasiswa/H2H.

### Query tulis atau reuse

Query yang akan memperbarui, memakai ulang, atau memindahkan relasi
`DetailBiaya` tidak boleh menerima baris tanpa induk secara bebas. Helper privat
di `SetingBiayaHelper` sengaja lebih ketat. Tujuannya agar tagihan legacy tidak
diam-diam diubah menjadi milik setting lain.

Jangan menyamakan kedua helper hanya karena bentuk Criteria terlihat mirip.

## Semantik status mahasiswa

Ada dua nilai status yang tidak boleh dicampur:

- status asli: untuk display dan kebenaran data akademik;
- status pembayaran efektif: hanya untuk pencocokan template/tagihan.

Bila konfigurasi Nonaktif sebagai Aktif dinyalakan:

```text
Status UI                 = Nonaktif
Status pencarian tagihan  = Aktif
```

Konfigurasi tersebut tidak mengubah `HistoryStatusMahasiswa`. Pembayaran juga
tidak boleh menulis status akademik hanya agar tagihan ditemukan.

Semua jalur pembayaran wajib memanggil helper kanonis. Jangan menulis ulang
blok `if Nonaktif then Aktif` pada action atau web service lain.

## Pola yang dilarang

Jangan menambahkan filter berikut secara tunggal pada query baca produksi:

```java
Restrictions.eq("settingBiaya", settingBiayaTerpilih)
```

Pola tersebut membuang dua bentuk relasi turunan dan seluruh data legacy.

Jangan pula menyalin implementasi `LEFT_JOIN` ke kelas lain. Salinan mudah
tertinggal ketika kontrak kompatibilitas berubah. Panggil helper kanonis.

Jangan melonggarkan filter profil sebagai jalan pintas. Masalah relasi sumber
harus diselesaikan pada batas sumber; semester, prodi, periode, status, dan
atribut mahasiswa tetap harus cocok.

## Matriks uji minimum

Setiap perubahan mesin tagihan harus menguji kasus berikut:

| Kasus | Relasi sumber | Status asli | Konfigurasi Nonaktif | Hasil |
|---|---|---|---|---|
| Setting modern | `detailSettingBiaya -> S` | Aktif | bebas | tampil |
| Setting langsung | `settingBiaya = S` | Aktif | bebas | tampil |
| Setting individual | `settingBiayaDetail -> S` | Aktif | bebas | tampil |
| Legacy langsung | semua relasi `null` | Aktif | bebas | tampil bila profil cocok |
| Legacy langsung | semua relasi `null` | Nonaktif | aktif | tampil bila profil Aktif cocok |
| Legacy langsung | semua relasi `null` | Nonaktif | tidak aktif | tidak memakai tarif Aktif |
| Legacy standalone | setting tidak ditemukan, semua relasi `null` | Aktif | bebas | tampil bila profil cocok |
| Tanpa setting | relasi menunjuk setting lain | Aktif | bebas | tidak tampil |
| Setting tanpa item | relasi menunjuk setting terpilih | Aktif | bebas | baris modern tidak tampil |
| Setting tanpa item | semua relasi `null` | Aktif | bebas | legacy tampil bila profil cocok |
| Setting salah | relasi menunjuk setting lain | Aktif | bebas | tidak tampil |
| Profil salah | relasi benar, semester/prodi salah | Aktif | bebas | tidak tampil |
| Sudah lunas | relasi dan profil benar | Aktif | bebas | tidak tampil sebagai tunggakan |

Jalankan matriks minimal pada UI admin dan satu jalur H2H/inquiry karena kedua
jalur mempunyai builder Criteria yang berbeda.

## Pemeriksaan data saat insiden

Untuk NIM yang bermasalah, gunakan tombol `Analisis Data` pada layar pembayaran.
Catat titik pertama yang menghasilkan nol. Pemeriksaan database berikut dapat
dipakai sebagai panduan dan harus disesuaikan dengan ID/konteks tenant:

```sql
select
    db.id,
    db.nilai_biaya,
    db.semester,
    db.tahun_akademik,
    db.angkatan,
    db.jurusan,
    db.status_mahasiswa,
    db.status_awal_mahasiswa,
    db.setting_biaya,
    db.detail_setting_biaya,
    db.setting_biaya_detail
from public.detail_biaya db
where db.semester = :semester
  and db.tahun_akademik = :tahun_akademik
  and db.jurusan = :jurusan_id
order by db.id desc;
```

Interpretasi cepat:

- nominal ada dan tiga relasi sumber `null`: data legacy, harus lolos cabang
  kompatibilitas setelah profil cocok;
- satu relasi berisi ID setting lain: periksa prioritas dan sumber pembuatan;
- tidak ada baris sama sekali: masalah berada pada proses pembuatan tagihan;
- baris ada tetapi profil berbeda: perbaiki konfigurasi, jangan melonggarkan
  query produksi secara global.

## Checklist perubahan berikutnya

Sebelum merge perubahan billing:

1. Cari seluruh pemakaian `settingBiayaTerpilih`.
2. Pastikan query baca memakai helper kanonis.
3. Pastikan pembatas sumber selalu dijalankan sebelum pembatas item.
4. Pastikan baris modern selalu dibatasi daftar item, sedangkan legacy all-null
   tidak digugurkan hanya karena daftar item setting kosong atau berbeda.
5. Pastikan query tulis/reuse tetap ketat.
6. Pastikan UI menampilkan status asli.
7. Pastikan query memakai status pembayaran efektif.
8. Uji data modern, individual, legacy dengan setting, dan legacy standalone.
9. Uji UI admin serta H2H.
10. Kompilasi dengan JDK 1.8.
11. Jalankan `git diff --check`.

## Berkas utama

- `src/ais/action/master/helper/PembayaranUtilHelper.java`
- `src/ais/action/ws/util/PembayaranUtil.java`
- `src/ais/action/master/helper/SetingBiayaHelper.java`
- `src/ais/action/master/helper/SettingBiayaMahasiswaSelector.java`
- `src/ais/action/master/NewDetailBiayaExcelAction.java`
- `src/ais/action/master/DaftarUlangMahasiswaLamaAction.java`

## Status mahasiswa setelah cicilan dihapus dan dibayar kembali

Bagian ini mendokumentasikan insiden ketika status mahasiswa mula-mula Aktif,
berubah menjadi Nonaktif setelah cicilan dihapus, tetapi tidak kembali Aktif
setelah cicilan dibuat lagi. Gejala tersebut mudah disalahartikan sebagai cache
cicilan, keterlambatan commit, atau nilai rekap `Kegiatan.bulans` yang belum
diperbarui. Tiga hal itu memang harus diperiksa, tetapi pada insiden September
2026 bukti debug menunjukkan jalur pembayaran sudah sehat: `DetailBiaya` yang
dipilih benar, satu baris `CicilanPembayaran` sudah terlihat dari sesi database
baru, jumlah bayar lebih besar daripada nilai tagihan, dan renderer juga
menampilkan pembayaran tersebut. Jadi status Nonaktif bukan disebabkan oleh
cicilan yang tidak terbaca.

### Sumber masalah

Mesin status menggunakan
`CommonHelperClass.jenisKegiatansUntukSyaratAktif` untuk menentukan jenis
tagihan yang boleh memengaruhi status mahasiswa. Sebelumnya cache ini dibangun
dengan kondisi berikut:

```text
digunakanSyaratKeaktifan IS NULL OR digunakanSyaratKeaktifan = true
```

Kondisi itu tidak sama dengan aturan pada model `JenisKegiatan`. Getter
`getDigunakanSyaratKeaktifan()` membaca `NULL` sebagai `false` untuk kegiatan
biasa. Hanya jenis pendaftaran kanonik, seperti Daftar Ulang mahasiswa lama dan
pendaftaran ulang mahasiswa baru, yang mempunyai default domain khusus. Dengan
kata lain, `NULL` adalah data legacy yang belum memilih fitur syarat keaktifan,
bukan persetujuan implisit bahwa kegiatan itu wajib lunas.

Akibat query lama, hampir semua jenis kegiatan lama yang kolomnya masih
`NULL` masuk ke cache syarat aktif. Mahasiswa pada kasus contoh mempunyai 18
kegiatan pada semester yang diperiksa. Walaupun kegiatan Daftar Ulang sudah
lunas, satu atau lebih kegiatan lain yang seharusnya tidak relevan dapat belum
memiliki pembayaran. Algoritma promosi Nonaktif ke Aktif mensyaratkan seluruh
kegiatan yang benar-benar menjadi syarat aktif telah memenuhi pembayaran.
Karena cache berisi kewajiban palsu, hasil akhirnya selalu `false`.

Asimetri gejala juga dapat dijelaskan. Ketika layar pertama kali dibuka, status
Aktif lama dapat berasal dari history atau cache. Penghapusan cicilan memicu
evaluasi ulang dan menemukan daftar kewajiban yang tercemar, sehingga status
turun menjadi Nonaktif. Saat cicilan dibuat kembali, evaluasi memang berjalan
lagi dan cicilan baru terbaca, tetapi kewajiban palsu yang lain tetap belum
lunas. Karena itu tombol Refresh tidak membantu. Refresh status tidak dapat
memperbaiki himpunan aturan yang sejak awal salah.

### Kontrak yang benar

Kolom `digunakanSyaratKeaktifan` harus diperlakukan sebagai berikut:

| Nilai database | Arti untuk kegiatan biasa | Arti untuk jenis kanonik |
|---|---|---|
| `true` | Menjadi syarat aktif | Menjadi syarat aktif |
| `false` | Bukan syarat aktif | Mengikuti getter domain jenis tersebut |
| `NULL` | Bukan syarat aktif | Mengikuti default getter domain |

Cache kini mengambil hanya baris dengan nilai database `true`. Setelah query,
jenis kanonik ditambahkan satu per satu hanya bila getter domainnya menghasilkan
`true`. Pendekatan ini menjaga kompatibilitas data lama tanpa menjadikan semua
nilai `NULL` sebagai opt-in.

Ada pertahanan kedua di
`HistoryStatusMahasiswaUtil.kegiatanSyaratAktifBerlaku`. Setiap kegiatan yang
dikembalikan dari pencarian mahasiswa tetap diperiksa lagi melalui
`getDigunakanSyaratKeaktifan()`. Pertahanan ini penting karena cache jenis
kegiatan bersifat statis untuk satu JVM. Pada hot-deploy atau node yang belum
di-restart, cache lama mungkin masih berisi anggota yang salah. Kegiatan itu
akan ditolak sebelum persentase pembayarannya ikut menentukan status.

### Dua arah transisi yang tidak boleh disatukan

Mesin status sengaja memakai semantik berbeda untuk dua arah transisi:

1. Aktif ke Nonaktif: bila tidak ada kegiatan syarat aktif sama sekali,
   mahasiswa tidak boleh dihukum. Nilai default pemeriksaan adalah lulus.
2. Nonaktif ke Aktif: harus ada minimal satu tagihan syarat aktif yang berlaku
   dan semua tagihan tersebut harus memenuhi ambang pembayaran. Tanpa bukti
   tagihan, mahasiswa tidak boleh dipromosikan otomatis.

Perbedaan ini mencegah dua kegagalan berlawanan. Menyamakan arah pertama dengan
arah kedua dapat menonaktifkan mahasiswa yang belum mempunyai tagihan. Menyamakan
arah kedua dengan arah pertama dapat mengaktifkan mahasiswa tanpa tagihan dan
tanpa pembayaran. Refactor berikutnya tidak boleh mengganti kedua helper itu
dengan satu ekspresi boolean tanpa mempertahankan semantik tersebut.

Nilai pembayaran untuk evaluasi status harus dibaca dari baris
`CicilanPembayaran` yang sudah committed melalui sesi baru, bukan hanya dari
rekap asynchronous `Kegiatan.bulans`. Dengan demikian, setelah transaksi bayar
selesai, proses sinkronisasi pasca-commit dan tombol Refresh melihat sumber data
yang sama. Namun pembacaan data segar hanya menyelesaikan masalah staleness;
filter jenis kegiatan tetap harus benar seperti kontrak di atas.

### Prosedur diagnosis

Saat kasus serupa muncul, baca log secara berurutan:

1. Pastikan `getDetailBiayaDefault` memilih `SettingBiaya` dan `DetailBiaya`
   yang sesuai semester, tahun akademik, program, prodi, status awal, serta
   status pembayaran efektif mahasiswa.
2. Pastikan query cicilan dari sesi baru menemukan baris yang baru disimpan.
   Bandingkan total bayar dengan `Kegiatan.hitungTagihan()`.
3. Catat semua ID pada `keydataUtama`. Jumlah ID yang besar bukan bukti error,
   tetapi menjadi petunjuk untuk memeriksa berapa jenis kegiatan yang ikut dalam
   cache syarat aktif.
4. Periksa `digunakan_syarat_keaktifan` untuk setiap jenis kegiatan. Hanya nilai
   `true` atau default kanonik yang boleh ikut.
5. Pastikan semester kegiatan cocok. Pendaftaran ulang mahasiswa baru tidak
   boleh menjadi syarat untuk semester lebih besar dari satu.
6. Pastikan status tersimpan dievaluasi sesudah commit pembayaran, bukan dari
   event yang berjalan sebelum transaksi selesai.

Contoh SQL audit, dengan nama kolom disesuaikan terhadap mapping instalasi:

```sql
select jk.id, jk.nama_kegiatan, jk.digunakan_syarat_keaktifan
from jenis_kegiatan jk
where jk.id in (:id_jenis_kegiatan)
order by jk.nama_kegiatan;
```

Jangan memperbaiki insiden dengan mengubah semua `NULL` menjadi `true`. Tindakan
itu justru meresmikan kewajiban palsu. Bila institusi memang ingin satu jenis
kegiatan menjadi syarat keaktifan, atur jenis itu secara eksplisit melalui
konfigurasi sehingga kolom bernilai `true`.

### Uji regresi wajib

`HistoryStatusMahasiswaPaymentRuleSelfTest` menyediakan uji Java 8 tanpa
database untuk pagar dasar: kegiatan biasa dengan `NULL` harus ditolak, flag
`false` harus ditolak, flag `true` harus diterima, dan Daftar Ulang mahasiswa
lama dengan data legacy `NULL` tetap diterima melalui default domain. Selain
uji tersebut, lakukan uji integrasi dengan urutan keadaan berikut:

1. Buat tagihan Daftar Ulang yang menjadi syarat aktif dan lunasi; status harus
   Aktif.
2. Hapus satu-satunya cicilan; setelah commit, status harus Nonaktif.
3. Buat cicilan kembali hingga ambang terpenuhi; setelah commit, status harus
   Aktif tanpa perlu menunggu rekap asynchronous.
4. Klik Refresh beberapa kali; hasil harus stabil dan tidak berganti kembali.
5. Tambahkan kegiatan legacy biasa dengan flag `NULL` tanpa pembayaran; status
   tidak boleh terpengaruh.
6. Ubah kegiatan tersebut menjadi flag `true`; setelah cache dimuat ulang,
   kegiatan itu memang harus ikut menentukan status.

Saat deployment, restart seluruh node aplikasi atau panggil
`CommonHelperClass.reloadJenisKegiatans()` setelah perubahan konfigurasi. Restart
memastikan cache statis lama hilang. Pertahanan lapis kedua tetap diperlukan,
tetapi bukan alasan untuk mengabaikan konsistensi cache antar-node.

Pada layar Pembayaran Mahasiswa, status Nonaktif ditampilkan bersama ringkasan
diagnostik dalam tanda kurung. Ringkasan tersebut berasal dari
`HistoryStatusMahasiswaUtil.analisisPenyebabNonaktif`, bukan string yang dibuat
oleh Action. Helper yang sama dipakai pada profil mahasiswa agar operator dan
mahasiswa menerima penjelasan identik. Analyzer membedakan pembayaran wajib yang
belum terdeteksi, tagihan syarat aktif yang belum tersedia, KRS/SKS kosong, NIM
yang sudah dipindahkan, dan status history yang belum sinkron atau ditetapkan
secara akademik. Analyzer bersifat read-only dan hanya dijalankan untuk satu
mahasiswa berstatus Nonaktif yang sedang dibuka, sehingga tidak menambah query
pada daftar massal.
