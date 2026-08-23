# 14 — Mesin posting per modul untuk dasbor Draft Jurnal

Dasbor Draft Jurnal ([09](09-draft-jurnal-dasbor.md)) hanya menampilkan angka sampai modulnya
punya **mesin posting** yang bisa dipanggil dari luar ZK. Dokumen ini mencatat pola porting-nya,
apa yang sudah terpasang, dan keputusan yang diambil per modul.

> Setiap penambahan modul di sini menambah satu baris di `DraftJurnalApiHelper.modulPosting()`.
> Bendera `bisaPosting` pada respons ringkasan langsung ikut menyala, sehingga tombolnya muncul
> di aplikasi tanpa perubahan kode klien.

---

## 1. Pola porting (berlaku untuk semua modul)

Layar ZK menaruh logika posting **di dalam listener dialog**: ia membaca rentang tanggal dari
datebox layar, memperbarui label progress, dan berjalan di thread sendiri. Tidak ada satu pun
yang bisa dipanggil dari servlet.

Pola yang dipakai — mengikuti preseden `PostingKasKecilAction` yang lebih dulu dibuka untuk
dasbor — adalah menambahkan **tiga method statis non-ZK** pada kelas Posting-nya:

```java
private static Criteria kriteriaPostingStatic(Session session, Date mulai, Date sampai)
public  static int       batalkanPostingSemua(Date mulai, Date sampai)
public  static int       postingSemua(Date mulai, Date sampai, Tbmuser oleh, Date tglPosting)
```

Aturan yang dipegang:

1. **Murni penambahan.** Tidak satu baris pun kode ZK yang ada dihapus atau diubah; diperiksa
   dengan `svn diff` (tidak boleh ada baris `-`). Layar web tetap berperilaku persis seperti
   sebelumnya.
2. **Kriteria harus sama dengan baris dasbornya.** `kriteriaPostingStatic` menyalin bagian
   `initCriteria` yang tidak berhubungan dengan kotak pencarian layar, dan dicocokkan dengan
   kriteria baris terkait di `DraftJurnalRingkasanUtil`. Kalau keduanya berbeda, pengguna
   memposting himpunan yang bukan yang ia lihat angkanya.
3. **`postingSemua` mengembalikan jumlah yang BERHASIL**, bukan jumlah yang dicoba — angka itu
   yang membedakan "tidak ada yang diproses" dari "semuanya diproses".
4. **Java 6/7**: tanpa lambda, diamond operator, maupun streams. Dikompilasi dengan
   `-source 7 -target 7`.

### Dua penyimpangan sadar dari kode ZK

Keduanya diterapkan pada setiap modul yang diport di sini, dan alasannya dicatat di JavaDoc
masing-masing:

| Penyimpangan | Alasan |
|---|---|
| Dokumen yang jurnalnya **tidak lengkap** (akun debet kosong atau akun kredit null) **dilewati**, bukan diproses separuh | Sama seperti layar yang memang tidak menampilkan tombol posting untuk baris berjurnal tidak valid |
| `postingHistory` **hanya dipasang bila jurnalnya benar-benar tersimpan** | Kode ZK memasangnya di luar blok penyimpanan, sehingga dokumen yang jurnalnya gagal tetap tercatat "sudah diposting" — hilang dari daftar draft padahal jurnalnya tidak pernah ada |

### Dua penjaga di sisi API

Keduanya di `DraftJurnalApiHelper.jalankanPosting`, berlaku untuk semua modul:

1. **Rentang kosong → mesin tidak dipanggil sama sekali.** `PostingKasKecilAction.postingSemua`
   menyimpan satu baris `PostingHistory` **sebelum** memeriksa ada-tidaknya dokumen; menekan
   tombol pada angka nol akan meninggalkan riwayat posting kosong.
2. **Ada dokumen tetapi nol terproses → dilaporkan sebagai PENOLAKAN.** Mesin lama menelan
   kegagalan per dokumen (`Common.tampilErrorJikaAdmin`), sehingga "berhasil, 0 dokumen" adalah
   kalimat yang menyesatkan persis ketika ada yang perlu diperiksa.

### Gerbang hak akses

Hak **`create` pada kunci MODUL-nya sendiri** (`kas_kecil`, `kas_besar`, `pengajuan_transfer`,
dst) — bukan kunci dasbor. Peran yang boleh membaca dasbor tidak otomatis boleh memposting isi
modul yang bukan wewenangnya.

---

## 2. Modul yang sudah punya mesin

| Baris dasbor | Kelas | Kunci hak akses | Dikerjakan |
|---|---|---|---|
| Kas Kecil | `PostingKasKecilAction` | `kas_kecil` | sudah ada sebelum dasbor dibuat |
| Kas Besar | `PostingKasBesarAction` | `kas_besar` | sesi POS Akuntansi |
| Uang Muka | `PostingUangMukaAction` | `uang_muka` | sesi lain |
| Pertanggungjawaban Uang Muka | `PostingPertangungjawabanAction` | `pj_uang_muka` | sesi lain |
| Pertanggungjawaban Kas Besar | `PostingPertangungjawabanKasBesarAction` | `pj_kas_besar` | sesi lain |
| Penggantian Kas Kecil | `PostingPenggantianKasKecilAction` | `penggantian_kas_kecil` | sesi lain |
| **Jurnal Pengajuan Transfer** | `PostingProsesTransferAction` | `pengajuan_transfer` | sesi POS Akuntansi |
| **Transitori** | `PostingProsesTransitoriAction` | `transitori` | sesi POS Akuntansi |
| **Penerimaan Tagihan Vendor** | `PostingPengadaanAction` (`ais.action.master.asset`) | `pengadaan_tagihan` | sesi POS Akuntansi |
| **Pekerjaan Vendor** | `PostingPemesananPekerjaanAction` (`ais.action.master.asset`) | `pengadaan_tagihan` | sesi POS Akuntansi |

Layar ZK sendiri **hanya** menyediakan tombol posting massal untuk Kas Kecil; sisanya di sana
masih menjawab "belum tersedia". Jadi mulai Kas Besar, mesin-mesin ini adalah kemampuan baru,
bukan pemindahan tombol yang sudah ada.

---

## 3. Kas Besar (`PostingKasBesarAction`)

**Kriteria**: `disetujuiOleh` tidak null, `nilai` ≠ 0 dan tidak null, `date(tanggal_persetujuan)`
di dalam rentang. Sama dengan baris "Kas Besar" pada dasbor.

**Jurnal per dokumen** (identik dengan `onPostingSemua`):

- Bila kas besar ini penggantian kas kecil (`getKasKecil()` ada, jenis kas kecilnya punya akun,
  dan jenis kas besarnya punya akun): **debet** = akun jenis kas kecil, **kredit** = akun jenis
  kas besar.
- Selain itu: **debet** = akun penerima jenis kas besar (bila ada), **kredit** = akun jenis kas
  besar.
- Nilai dari `getNilai()`; bila ≤ 0,1 posisi debet/kredit ditukar (perilaku `saveTransaksi` yang
  sama dengan layar).
- Satuan kerja: milik dokumen (`kasBesar.getSatuanKerja()`).
- `PostingHistory.JENIS_PENGGUNAAN_KAS_BESAR`.

**Batal**: hapus `akunting.grup_transaksi` yang `kas_besar=<id>` **dan `closing is null`**, lalu
kosongkan `postingHistory`. Jurnal yang sudah closing tidak ikut terhapus.

---

## 4. Jurnal Pengajuan Transfer (`PostingProsesTransferAction`)

Modul ini yang paling banyak cabangnya, jadi dicatat rinci.

**Kriteria**: `aktif` null/true, `disposisiSop.aktif` null/true, `prosesTransfer.realisasikanOleh`
tidak null, `prosesTransfer.disetujuiOleh` tidak null, `nominal` ≠ 0 dan tidak null,
`date(tanggal_realisasikan)` di dalam rentang. Cocok dengan `kriteriaPengajuanTransfer` pada
dasbor.

**Jurnal per dokumen**:

| Unsur | Aturan |
|---|---|
| Akun debet | akun pengajuan (`dpt.getAkun()`) — **kecuali** pengajuan bertanda transitori, maka memakai `prosesTransfer.caraPembayaranTransfer.akunTransitori` |
| Akun kredit | `prosesTransfer.caraPembayaranTransfer.akun` |
| Tanggal jurnal | `tanggalRealisasikan` bila ada, selain itu `tanggalPersetujuan` |
| Pemecahan PPh | bila pengajuan bertaut `SaldoAwalMasterAsset`: tiap `SaldoAwalMasterAssetDetail` yang jenis pajaknya punya **akun dana titipan** menambah satu baris debet senilai `persenPph/100 × (jumlah × harga)`; totalnya dikurangkan dari nilai debet akun utama |
| Kredit | satu baris senilai `nominal` penuh |
| Tukar posisi | bila `nominal` ≤ 0,1 |
| Jenis riwayat | `PostingHistory.JENIS_PENGAJUAN_TRANSFER` |

**Satuan kerja — satu-satunya tempat perilakunya berbeda dari ZK.** Layar memakai satuan kerja
**pengguna yang sedang login** (`Common.getSatuanKerja()`, dibaca dari konteks sesi ZK). Dari
API konteks itu biasanya kosong, jadi versi statis memakai satuan kerja pengguna **bila
tersedia** dan satuan kerja **dokumen** sebagai cadangan. Menulis jurnal tanpa satuan kerja
sama sekali akan membuat laporan per unit kehilangan barisnya.

**Batal**: dijalankan pada `currentNativeSession()` dengan transaksi eksplisit per dokumen —
hapus `akunting.transaksi` lebih dulu (grup_transaksi adalah induknya), lalu
`akunting.grup_transaksi` yang `daftar_pengajuan_transfer=<id>` dan `closing is null`, kosongkan
`postingHistory`, commit; bila gagal, rollback dan lanjut ke dokumen berikutnya.

> **Kenapa bukan `currentSession()` seperti tombol layar.** `currentSession()` hanya
> ter-commit di dalam permintaan ZK, yang kerangkanya menutup sesi berjalan. Dipanggil dari
> API, perubahannya **tidak pernah tersimpan** — pembatalan melaporkan sukses padahal jurnal
> dan penanda postingnya masih utuh, dan kegagalannya diam. Cacat ini ditemukan sesi lain pada
> Kas Kecil lewat harness khusus, sudah diperbaiki di sana, dan pola perbaikannya diikuti di
> sini.

---

## 4b. Akhir baris berkas: konvensinya BERBEDA PER BERKAS

Menyunting berkas Java di working copy ini lewat skrip yang membaca sebagai teks
(universal newline) lalu menulis balik akan **mengubah seluruh akhir baris berkas**. Akibatnya
`svn diff` menampilkan setiap baris sebagai terhapus-lalu-ditambah: suntingan tujuh baris jadi
mustahil ditinjau, dan sesi lain yang menyentuh berkas yang sama kena konflik palsu di semua
baris.

Yang penting: **konvensinya tidak seragam**. Terverifikasi pada berkas-berkas ini —

| Berkas | Akhir baris di HEAD |
|---|---|
| `DraftJurnalApiHelper.java` | CRLF |
| `DraftJurnalRingkasanUtil.java` | CRLF |
| `PostingKasBesarAction.java` | CRLF |
| `PostingProsesTransferAction.java` | **LF** (sejak r76329, jauh sebelum pekerjaan ini) |

Jadi "kembalikan saja semuanya ke CRLF" pun salah — dan menebak dari direktori juga tidak
aman: `ais/action/servlet/PosApi.java` LF sementara `ais/action/servlet/api/SopService.java`
CRLF, dua berkas yang duduk berdampingan.

**Jebakan kedua: berkas seragam yang menjadi CAMPURAN karena baris tambahan.** Ini yang paling
sulit terlihat. Ketika kita hanya MENAMBAH baris, berkas HEAD-nya seragam, `svn diff` tetap
kecil dan wajar — tetapi bila baris baru itu datang dari skrip yang menulis `
` sementara
berkasnya CRLF, hasilnya berkas campuran. Sesi lain pernah membuat `DasboardSop.java` menjadi
4.319 CRLF + 45 LF persis karena 45 baris tambahannya. Tidak ada satu pun tanda di `svn diff`;
hanya ketahuan kalau bita-nya dihitung.

**Aturan dua lapis yang dipakai sekarang:**

1. Sunting pada teks yang sudah dinormalkan ke `
`, lalu tulis balik memakai akhir baris
   **berkas itu sendiri** — bukan konvensi repositori, bukan konvensi direktori.
2. Setelah menambal, **hitung ulang** CRLF vs LF dan pastikan **salah satunya nol**. Berkas
   campuran berarti langkah 1 gagal, dan `svn diff` tidak akan memberi tahu.

Skrip bantu yang dipakai sesi ini: `cek_eol.py` (mengukur, dan menandai CAMPURAN secara
eksplisit) dan `samakan_eol.py` (menyamakan dengan `svn cat -r HEAD` berkas itu sendiri).

**Audit berkas pekerjaan ini terhadap HEAD** — tidak ada yang campuran:

| Berkas | Akhir baris |
|---|---|
| `DraftJurnalApiHelper.java` | murni CRLF |
| `DraftJurnalRingkasanUtil.java` | murni CRLF |
| `PostingKasBesarAction.java` | murni CRLF |
| `Data.java` | murni CRLF |
| `PostingProsesTransferAction.java` | murni LF |
| `EbisnisMenuKatalog.java` | murni LF |
| `PosApi.java` | murni LF |

Bukti berhasil pada berkas yang sempat salah: diff `PostingProsesTransferAction.java` turun
dari 2.800 baris menjadi **246 baris tanpa satu pun baris terhapus**.

---

## 4c. Transitori (`PostingProsesTransitoriAction`)

Transitori adalah sisi lain dari pengajuan transfer: uangnya sudah keluar dari kas tetapi
belum sampai ke akun tujuan, jadi ditampung dulu di **akun transitori** milik cara pembayaran.
Karena itu bentuk jurnalnya cermin dari Jurnal Pengajuan Transfer — akun yang di sana jadi
kredit, di sini jadi debet.

**Kriteria** (sama dengan bagian non-pencarian `initCriteria`, dan sama dengan baris
"Transitori" pada dasbor):

- `prosesTransitori.disetujuiOleh` tidak null;
- `daftarPengajuanTransfer.nominal` ≠ 0 dan tidak null;
- `transfer = true` — saringan ini yang membatasi transitori pada jenis yang memang
  berpasangan dengan pengajuan transfer; tanpa itu baris transitori lain ikut terhitung;
- `prosesTransitori.tanggalPersetujuan` di dalam rentang.

Perhatikan tanggalnya: **tanggal persetujuan proses transitori**, bukan tanggal realisasi
seperti pada Pengajuan Transfer. Rentang yang sama bisa memberi jumlah dokumen berbeda pada
dua baris dasbor itu, dan itu memang benar.

**Jurnal per dokumen**:

| Unsur | Aturan |
|---|---|
| Akun debet | akun pengajuan transfer pasangannya (`dpt.getAkun()`) |
| Akun kredit | `dpt.prosesTransfer.caraPembayaranTransfer.akunTransitori` |
| Tanggal jurnal | `prosesTransitori.tanggalPersetujuan` |
| Nilai | `dpt.getNominal()` |
| Tukar posisi | bila nominal ≤ 0,1 |
| Jenis riwayat | `PostingHistory.PENGAJUAN_TRANSITORI` |
| Keterangan | `Pengajuan transitori "<nama>" senilai <nominal>` |

**Tautan balik yang dilengkapi sebelum memposting.** Bila pengajuan transfer pasangannya belum
menunjuk balik ke transitori ini (`dpt.getTransitoriData()` kosong), tautan itu diisi lebih
dulu lewat `Common.refreshUpdate` — persis seperti yang dilakukan layar. Langkah ini terlihat
seperti detail sepele tetapi tidak boleh dihilangkan: tanpa tautan itu pembatalan posting dan
penelusuran jurnal kehilangan jejak pasangannya, dan dokumen jadi sulit ditarik kembali.

**Satuan kerja.** Sama dengan Pengajuan Transfer, dan di sini alasannya lebih kuat: entitas
`Transitori` **tidak menyimpan satuan kerja sama sekali**. Layar memakai satuan kerja pengguna
yang sedang login (`Common.getSatuanKerja()`, dari konteks sesi ZK); dari API konteks itu
biasanya kosong, jadi cadangannya adalah satuan kerja **pengajuan transfer pasangannya**.

**Batal**: pola yang sama dengan Pengajuan Transfer — `currentNativeSession()`, transaksi
eksplisit per dokumen, hapus `akunting.transaksi` lebih dulu lalu `akunting.grup_transaksi`
yang `transitori=<id>` **dan `closing is null`**, kosongkan `postingHistory`, commit; bila
gagal, rollback dan lanjut ke dokumen berikutnya. Jurnal yang sudah closing tidak ikut
terhapus.

**Dua penyimpangan sadar** (lihat bagian 1) berlaku penuh di sini: dokumen yang akun debet atau
akun kreditnya belum lengkap **dilewati** alih-alih menulis jurnal cacat, dan
`postingHistory` dipasang **hanya setelah** jurnalnya benar-benar tersimpan — sehingga dokumen
yang gagal tetap terlihat sebagai draft dan bisa diulang, bukan hilang dari daftar kerja.

---

## 4d. Penerimaan Tagihan Vendor (`PostingPengadaanAction`)

Modul pertama di luar paket `akunting`: kelasnya ada di `ais.action.master.asset`, layarnya
`posting_pengadaan.zul`, dan dokumennya `SaldoAwalMasterAsset` — tagihan vendor yang **bukan**
bertermin. Kunci hak aksesnya memakai kunci menu yang memang sudah ada,
`pengadaan_tagihan` ("Pengadaan: Terima Tagihan Vendor"), sehingga admin dapat mengatur
haknya per peran hari ini juga tanpa kunci baru.

**Kriteria**: `jsonTermin` null, `disetujuiOleh` tidak null, `aktif` null/true, `nilai` ≠ 0
dan tidak null, `date(tanggal_persetujuan)` di dalam rentang. Saringan `jsonTermin is null`
itulah yang memisahkannya dari baris "Pekerjaan Vendor" — dokumennya satu tabel, tetapi yang
bertermin ditangani layar lain.

**Jurnal per dokumen** — ini yang paling banyak barisnya dari semua modul sejauh ini:

| Sisi | Aturan |
|---|---|
| Debet | satu baris **per akun**, dijumlahkan dari `hargaTotal` tiap detail. Akunnya `akunBiayaPenyusutan` bila kelompok asetnya BUKAN aset tetap, selain itu `akunTransaksi`; keduanya lewat `AssetUtil.ambilDataAkun` supaya akun per satuan kerja yang terpakai |
| Kredit PPh | tiap detail yang jenis pajak barangnya punya akun menambah satu baris senilai `persenPph/100 × (jumlah × harga)` |
| Kredit utang | `penyedia.akunUtang` bila ada, selain itu `penerimaan.jenisPenerimaanBarang.akunHutangPenyedia` |
| Bila pemesanannya ber-DP (`dptotal > 0,1`) | sisa di luar DP masuk ke akun utang; DP-nya masuk ke `jenisPemesananPengadaanAsset.akunDp` senilai `dptotal − totalPph` |
| Tanpa DP | akun utang menerima `nilai − totalPph` |
| Tukar posisi | bila `nilai` ≤ 0,1 |
| Tanggal | `tanggalPersetujuan` |
| Satuan kerja | milik dokumen (`saldoAwal.getSatuanKerja()`) — tidak perlu konteks pengguna |
| Jenis riwayat | `PostingHistory.JENIS_PENERIMAAN_TAGIHAN_BARANG_JASA` |

**Penjagaan tambahan atas layar.** Layar menambahkan akun kredit **apa adanya**, termasuk bila
akun utangnya `null`: larik kreditnya jadi berisi null dan jurnalnya cacat — lalu penanda
posting tetap dipasang. Versi API menolak dokumen seperti itu (`lengkap = false`) dan
melewatinya, sehingga dokumennya tetap terlihat sebagai draft. Dokumen `AW-00000005` pada
basis UAT persis kasus ini: tidak punya penerimaan maupun penyedia, jadi akun utangnya tidak
diketahui.

**Batal**: hapus `akunting.transaksi` lebih dulu, lalu `akunting.grup_transaksi` yang
`saldo_awal_master_asset=<id>` dan `closing is null`, kosongkan `postingHistory` — semuanya
pada `currentNativeSession()` dengan transaksi eksplisit per dokumen.

---

## 4e. Dua cacat yang baru terlihat saat jalur TULIS benar-benar dijalankan

Modul ini yang pertama punya dokumen nyata di basis UAT, jadi ia yang pertama menjalankan
mesin sampai ke `saveTransaksi`. Dua cacat langsung muncul — **keduanya mengenai SELURUH
mesin posting**, termasuk yang sudah ditulis sesi lain, dan keduanya sudah diperbaiki.

### (a) "Session is closed!" — nol jurnal tertulis, tanpa satu pun pesan galat

Mesin mengambil sesi sekali di awal (`HibernateUtil.currentNativeSession()`) lalu memakainya
untuk seluruh loop. Di tengah loop ada helper master data yang **menutup sesi thread-local di
blok `finally`-nya sendiri**. Di dalam ZK hal itu tidak kelihatan karena kerangka ZK yang
mengelola sesinya; dari jalur API, `begin()` berikutnya melempar
`org.hibernate.SessionException: Session is closed!` — dan karena exception itu tertangkap
per-dokumen, hasilnya adalah **nol dokumen terproses tanpa jejak apa pun**.

**Perbaikannya** satu baris di setiap titik transaksi:

```java
session = HibernateUtil.currentNativeSession();   // kembalikan sesi bila sudah ditutup helper
session.getTransaction().begin();
```

`currentNativeSession()` mengembalikan sesi yang sama bila masih hidup dan membuka yang baru
bila sudah tertutup, jadi memanggilnya ulang aman sekaligus memulihkan. Dipasang pada
**sembilan** kelas mesin: Kas Kecil, Kas Besar, Uang Muka, Pertanggungjawaban Uang Muka,
Pertanggungjawaban Kas Besar, Penggantian Kas Kecil, Pengajuan Transfer, Transitori, dan
Penerimaan Tagihan Vendor.

**Bukti**: dokumen uji yang sama, sebelum perbaikan `jumlah=0` dan tidak ada jurnal; sesudah
perbaikan `jumlah=1` dengan 3 baris jurnal yang seimbang.

### (b) Pelapor galat ZK menelan sebabnya di jalur API

`Common.tampilErrorJikaAdmin(e)` membangun message box ZK: `MyMessageboxConfig` →
`Common.getBahasaConfig` → query Hibernate. Dipanggil dari API ia (1) tidak menampilkan apa
pun sehingga sebab aslinya **hilang**, dan (2) pada sesi yang sudah rusak ia melempar
exception BARU (`AssertionFailure: null id ... don't flush the Session after an exception
occurs`) yang menutupi kegagalan aslinya.

**Perbaikannya**: di dalam blok statis non-ZK, pelapornya diganti
`ais.common.ErrorAuditUtil.record(e, "<Kelas> jalur API")` — masuk Error Log dan server log,
tanpa menyentuh sesi. Kode layar ZK **tidak diubah**: di sana message box memang berguna.

> Kaitannya dengan pesan API: penjaga kedua menjawab *"… tetapi tidak satu pun berhasil
> diproses. Periksa Error Log server, lalu ulangi."* Sebelum perbaikan (b), Error Log itu
> kosong dan kalimat tersebut menyesatkan. Sekarang isinya benar-benar ada.

### Jebakan yang ditemukan tetapi TIDAK diubah

`PostingHistory.getNama()` membaca `tbmuser` tanpa penjaga null padahal kolomnya
`nullable = false`; memanggil mesin posting dengan pengguna null meledak saat flush dengan
pesan yang tidak menyebut penyebabnya. Di produksi pengguna selalu ada (dari sesi POS), jadi
modelnya sengaja tidak disentuh — tetapi harness WAJIB memberi pengguna nyata, dan pemanggil
baru mana pun harus tahu ini.

---

## 4f. Pekerjaan Vendor (`PostingPemesananPekerjaanAction`)

Dokumennya **satu tabel dengan Penerimaan Tagihan Vendor** (`SaldoAwalMasterAsset`); yang
memisahkan keduanya hanya `jsonTermin` — ada berarti tagihan pekerjaan per termin, tidak ada
berarti tagihan biasa. Kunci hak aksesnya pun sengaja disatukan (`pengadaan_tagihan`):
menagih per termin tetap "menerima tagihan vendor", hanya dipecah menjadi beberapa kali.

**Kriteria**: `penerimaanPengadaanMasterAsset` tidak null, `jsonTermin` tidak null,
`disetujuiOleh` tidak null, `aktif` null/true, `nilai` ≠ 0 dan tidak null,
`date(tanggal_persetujuan)` di dalam rentang.

**Angkanya datang dari JSON, bukan dari kolom.** Isi `jsonTermin` dibaca sebagai satu objek:

| Kunci JSON | Peran |
|---|---|
| `key` | penanda termin; **dipakai sebagai `ref` grup transaksi** |
| `setuju` | hanya termin yang `true` yang boleh dijurnal |
| `penagihan`, `ppn`, `pinalti` | nilai jurnal = `\|(penagihan + ppn% × penagihan) − pinalti\|` |
| `pajak` | id `JenisPajakBarang`; bila jenis itu punya akun, sisi kredit dipecah dua |
| `nama`, `nomor` | hanya untuk keterangan jurnal |

**Jurnal per dokumen**: debet = `jenisPemesananPengadaanAsset.akunUtangPekerjaan`,
kredit = `jenisPemesananPengadaanAsset.akunUtangDp`. Bila terminnya berpajak, kredit dipecah
menjadi `nilai − nilaiPajak` ke akun utang DP dan `nilaiPajak` ke akun pajak, dengan
`nilaiPajak = penagihan × persen/100`. Tanggal = `tanggalPersetujuan`, satuan kerja milik
dokumen, jenis riwayat `PostingHistory.JENIS_TAGIHAN_PEKERJAAN`.

**`ref` bukan hiasan — ia yang memisahkan dua jurnal pada dokumen yang sama.** Satu dokumen
bisa memikul jurnal DP (`ref = 'DP_PEKERJAAN'`) sekaligus jurnal terminnya sendiri
(`ref` = kunci termin). Karena itu pembatalan **wajib** menyaring
`ref is not null and ref != 'DP_PEKERJAAN'`; tanpa itu, membatalkan tagihan pekerjaan akan
ikut menghapus jurnal DP yang bukan urusan layar ini. Syaratnya disalin apa adanya dari
`onBatalkanPostingSemua`, dan perilakunya **diuji** (lihat bagian 5).

**Penjaga anti-jurnal-ganda**: sebelum menulis, dicek apakah sudah ada `GrupTransaksi` dengan
`ref` = kunci termin ini untuk dokumen yang sama. Bila ada, dokumennya dilewati. Penjaga ini
milik layar dan dipertahankan.

**Tiga penyimpangan sadar dari layar** — dua yang biasa, plus satu khas modul ini:

1. dokumen yang akun debet/kreditnya belum diketahui **dilewati** (layar tetap menandainya
   terposting);
2. penanda posting dipasang **hanya setelah** jurnalnya tersimpan;
3. penanda posting **juga** dipasang pada cabang berpajak. Layar hanya memasangnya di cabang
   tanpa pajak, sehingga termin berpajak yang sudah punya jurnal tetap tampil sebagai draft
   selamanya: pengguna menekan tombol berulang kali dan penjaga anti-jurnal-ganda menolaknya
   setiap kali, tanpa penjelasan.

## 5. Hasil uji (basis data UAT lokal)

Harness `TesDraftJurnal.java`; dijalankan **satu JVM pada satu waktu** — PostgreSQL di mesin ini
`max_connections=100` sementara kolam c3p0 satu JVM uji memakan ~50, sehingga dua suite
berbarengan membuat yang kedua mati dengan gejala yang menyesatkan.

| Yang diuji | Hasil |
|---|---|
| Bendera `bisaPosting` | **10 baris**: Uang Muka, PJ Uang Muka, Kas Kecil, Kas Besar, PJ Kas Besar, Penggantian Kas Kecil, Jurnal Pengajuan Transfer, Transitori, Penerimaan Tagihan Vendor, Pekerjaan Vendor |
| Seluruh modul dasbor terhitung | tanpa satu pun exception (34 baris setelah tiga baris Keuangan ditambahkan sesi lain) |
| Modul tanpa mesin (Gaji) | ditolak dengan kalimat "belum tersedia dari aplikasi" |
| Tanpa nama jenis jurnal | ditolak |
| Rentang tanpa dokumen — Pengajuan Transfer, **Transitori** (keduanya posting DAN batal), Kas Besar, Kas Kecil | ditolak sebelum mesin dipanggil, dengan kalimat yang menyebut modul dan arah aksinya |
| Diff berkas bersifat penambahan murni | `svn diff` 246 baris, 0 baris terhapus |
| Kompilasi | `-source 7 -target 7` bersih untuk seluruh kelas Posting yang terlibat |
| **Jalur TULIS Penerimaan Tagihan Vendor** (harness `TesPostingTagihanVendor`) | **LULUS** — 1 dokumen diposting, **3 baris jurnal** (debet 90.000 pada 512.101 + 290.000 pada 214.100, kredit 380.000 pada 200.000), **debet = kredit**, tanggal jurnal = tanggal persetujuan, penanda posting terpasang |
| Batal posting jalur TULIS | **LULUS** — `grup_transaksi` dan `transaksi` kembali ke 0, penanda posting kosong lagi, basis data kembali persis seperti semula |
| **Jalur TULIS Pekerjaan Vendor** (harness `TesPostingPekerjaanVendor`) | **LULUS** — jurnal termin tertulis & seimbang (110.000 = 100.000 + PPN 10%), `ref` grup transaksi = kunci termin, penanda posting terpasang |
| Batal, lalu posting ULANG | **LULUS** — batal membersihkan jurnal dan penandanya; posting ulang berhasil lagi, jadi penjaga anti-jurnal-ganda tidak menahan dokumen yang memang sudah dibatalkan |
| **Jurnal DP tidak ikut terhapus** | **LULUS** — jurnal dokumen ditandai `ref = 'DP_PEKERJAAN'` lalu pembatalan dijalankan: jurnalnya selamat, hanya penanda postingnya yang dilepas |

**Merakit skenario uji tanpa merusak data.** Basis UAT tidak punya satu pun dokumen
bertermin, jadi harness Pekerjaan Vendor merakit skenarionya dengan **membalik kolom, bukan
membuat baris**: jenis pemesanan dipinjami dua akun, pemesanannya ditandai bertermin,
penerimaan dan dokumen saldo awal diberi `json_object` termin uji, lalu semuanya dikembalikan
di blok `finally`. Nilai semula dibaca lebih dulu dan dipulihkan apa adanya.

> **Jebakan yang sempat memakan waktu:** nilai boolean yang dibaca lewat JDBC kembali sebagai
> `'t'`/`'f'`. Disisipkan tanpa tanda kutip, Postgres membacanya sebagai NAMA KOLOM
> (`kolom "f" tidak ada`) — pemulihan berhenti di tengah, dan basis UAT tertinggal dalam
> keadaan skenario uji. Pemulihannya harus dikerjakan terpisah dan **dipersempit ke baris yang
> memang disentuh**: delapan pemesanan lain ternyata sudah `bytermin = true` sejak semula,
> sehingga `set bytermin = false where bytermin = true` akan merusak data yang tidak bersalah.
> Selalu baca dulu keadaan sebenarnya sebelum memulihkan borongan.

**Penulisan jurnal sudah terverifikasi — pada DUA modul.** Penerimaan Tagihan
Vendor adalah modul pertama yang dokumennya ada di basis UAT, jadi ia yang pertama diuji
sampai jurnalnya benar-benar tertulis dan dibatalkan lagi. Dua cacat yang ditemukan di sana
(bagian 4e) bersifat lintas-modul dan sudah diperbaiki di kesembilan mesin, jadi kepercayaan
pada modul lain ikut naik — tetapi bentuk jurnal masing-masing modul tetap belum diuji satu
per satu.

**Yang masih BELUM terverifikasi untuk modul selain Penerimaan Tagihan Vendor dan Pekerjaan
Vendor:** penulisan jurnal yang sebenarnya. Basis UAT tidak punya satu pun dokumen `kas_kecil`, `kas_besar`,
`daftar_pengajuan_transfer`, maupun `transitori` yang memenuhi syarat, dan tabel master untuk menyemai dokumen uji
(`jenis_kas_besar`, `satuan_kerja`) tidak ada di basis ini. Yang sudah terbukti: jalur sesi
thread-local yang dipakai mesin lama (`HibernateUtil.currentSession` /
`currentNativeSession`) **hidup di luar ZK** — query jalan dan `begin`+`rollback` berhasil.

**Aturan pembersihan data uji** (dipetik dari kegagalan harness sesi lain): begitu sebuah
harness mulai MENULIS dokumen uji, beri baris ujinya awalan yang khas dan sapu awalan itu di
**awal** persiapan, bukan hanya di akhir. Pembersihan yang hanya menghapus buatan jalannya
sendiri akan meninggalkan sampah permanen begitu satu jalannya terbunuh di tengah — dan
sampah itu ikut terhitung sebagai draft pada jalan berikutnya.

Untuk menutup celah itu dibutuhkan basis data yang berisi dokumen nyata. Sampai saat itu,
penjaga kedua ("ada dokumen tetapi nol terproses → penolakan") adalah jaring pengaman yang
membuat kegagalan diam-diam tetap terlihat oleh pengguna.

---

## 6. Sisa modul yang belum punya mesin

Jurnal Umum, Pajak, DP Vendor, DP Pekerjaan Vendor, Jurnal Balik DP Pekerjaan, Gaji,
Closing, Posting HPP, tujuh baris Mahasiswa, enam baris Siswa, dan tiga baris Penyusutan.

Peta kelas untuk keluarga vendor/aset, diverifikasi dari `DrafJurnalAction` lewat halaman
`.zul` yang ditautkannya:

| Baris dasbor | Halaman ZK | Kelas |
|---|---|---|
| Penerimaan Tagihan Vendor | `posting_pengadaan.zul` | `PostingPengadaanAction` ✔ sudah |
| Pekerjaan Vendor | `posting_pesanan_pekerjaan.zul` | `PostingPemesananPekerjaanAction` ✔ sudah |
| DP Vendor | `posting_dp.zul` | `PostingPemesananDpAction` |
| DP Pekerjaan Vendor | `posting_dp_pesanan_pekerjaan.zul` | `PostingDpPemesananPekerjaanAction` |
| Jurnal Balik DP Pekerjaan | `posting_dp_balik_pesanan_pekerjaan.zul` | `PostingJurnalBalikDpPemesananPekerjaanAction` |

Urutan berikutnya yang direncanakan: **DP Vendor** (`PostingPemesananDpAction`), lalu
**DP Pekerjaan Vendor** dan **Jurnal Balik DP Pekerjaan** — ketiganya berputar di sekitar uang
muka pemesanan yang sama, jadi aturan `ref`-nya perlu dibaca bersama-sama supaya pembatalan
satu modul tidak menghapus jurnal modul lain.
