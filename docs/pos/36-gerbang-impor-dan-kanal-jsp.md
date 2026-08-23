# Gerbang impor massal dan kejujuran kanal JSP

Melengkapi [34-hak-akses-menu-pos.md](34-hak-akses-menu-pos.md). Dokumen ini mencatat dua
lubang yang ditemukan saat menyisir penegakan hak akses di sisi peladen, ditambah satu
perbaikan kompilasi yang muncul dalam sapuan yang sama.

## 1. Impor massal Kode Akun adalah pintu belakang

Simpan dan hapus **satu baris** pada Kode Akun bergerbang sejak awal, tetapi ketiga aksi
**impor**-nya menerima payload apa adanya:

```
kode_akun_impor · kode_akun_bank_impor · kode_akun_jenis_transaksi_impor
```

Akibatnya peran yang di `TbmroleAction` hanya diberi hak **melihat** Kode Akun tetap dapat
membuat atau mengubah ratusan akun sekaligus lewat unggah Excel. Gerbang di layar tidak
menolong sama sekali: aksinya dapat dipanggil langsung tanpa melewati tombol mana pun.

Pemeriksaannya kini **dua lapis**, dan keduanya perlu:

- **Di muka** — peran yang sama sekali tidak berhak (tidak `create` maupun `update`)
  ditolak sekali saja, bukan baris demi baris, sehingga berkas besar tidak diproses
  sia-sia dan pesannya jelas.
- **Per baris** — satu berkas impor lazim memuat CAMPURAN baris baru dan baris perubahan,
  sehingga hak yang diperlukan berbeda-beda di dalam satu unggahan. Peran yang boleh
  menambah tetapi tidak boleh mengubah tetap dapat mengimpor akun baru; baris yang menimpa
  akun lama ditolak sendiri-sendiri dan masuk ke ringkasan `masalah`, seperti penolakan
  validasi lainnya. Pengguna melihat persis baris mana yang tidak dikerjakan.

Kunci per tab: `kode_akun`, `bank_akun`, `jenis_transaksi`. Alasan lengkapnya ditulis pada
Javadoc kelas `KodeAkunApiHelper` supaya tidak bergantung pada pesan commit.

Di sisi klien, tombol **Upload** pada `kode_akun_screen.dart` ikut dipadamkan lewat getter
`_bolehImporTabIni` (cukup salah satu dari `create`/`update`), lengkap dengan tooltip
alasannya — tanpa itu, penolakan peladen baru terasa setelah berkas telanjur dipilih dan
diproses. Penjaganya: uji *"tombol Upload ikut hak akses"* pada
`test/kode_akun_crud_kontrak_test.dart`.

### Hasil sapuan gerbang menyeluruh

Audit **per aksi** (bukan per metode — gerbangnya bisa duduk di `proses()` maupun di dalam
metode yang dipanggilnya) atas seluruh `src/ais/action/servlet/api/`:

> **71 aksi mutasi terdaftar, 71 bergerbang.**

`TokoApiHelper` lebih ketat lagi: admin-only lewat `Common.getApakahAdminLain`. Perlu
diperhatikan bahwa audit per-metode yang naif melaporkan puluhan "lolos" palsu —
`batalkanDiam()` hanyalah rollback, dan `hapusNolEkor()` hanya memangkas nol di belakang
angka.

Konsistensi registri juga diperiksa: seluruh konstanta `KUNCI*` pada helper (15 buah)
terdaftar di `EbisnisMenuKatalog.DAFTAR`, dan tidak ada kunci pada `KUNCI_CRUD` (82),
`KUNCI_AKUNTANSI` (23), maupun `KUNCI_DEFAULT_NONAKTIF` (25) yang tidak dikenal registri.
Kunci yang salah ketik akan membuat togel di `TbmroleAction` diam-diam tidak berpengaruh.

## 2. Badge kanal adalah janji kepada admin

`Entri.platform` pada `EbisnisMenuKatalog` dirender sebagai badge di grid `TbmroleAction`;
badge itu memberi tahu admin **togel ini berlaku di kanal apa saja**. Dua janji meleset:

**Tujuh pintasan akuntansi di JSP tidak bergerbang.** Pada
`webapp/WEB-INF/baru/modul/kantin/laporan_keuangan.jsp` ada deretan tombol ke layar ZK —
Akun/Perkiraan, Posting HPP, Posting Penjualan, Posting Kulakan, Posting Bayar Hutang,
Posting Terima Piutang, Posting Penyesuaian — yang dirender tanpa syarat. Peran yang
aksesnya sudah dimatikan admin tetap melihat dan dapat mengkliknya dari kanal JSP,
padahal di Desktop/Android sudah tersembunyi. Ketujuhnya kini melewati
`EbisnisMenuKatalog.aksesAkuntansi()` — gerbang yang sama dengan menu Anggaran,
fail-closed, dan dapat diatur per peran.

**`pengadaan_sinkron` mengaku punya kanal JSP padahal tidak ada.** Tidak satu pun dari
2.125 berkas JSP menyebutnya. Klaimnya dicabut; sebaliknya enam kunci yang kini
benar-benar bergerbang (`kode_akun`, `posting_hpp`, `posting_penjualan`,
`posting_kulakan`, `posting_bayar_hutang`, `posting_terima_piutang`) ditambahi kanal
`"jsp"`.

Audit ulang: **45 kunci berkanal JSP, 0 tanpa gerbang.**

## 3. Kompilasi Java 7 sempat patah

`ais/ui/util/MyMessageboxConfig.java:348` menangkap variabel `copy` di dalam inner class
tanpa `final`. Berkas itu **lolos di javac 8** — di sana variabelnya cukup *effectively
final* — tetapi **gagal di `-source 1.7`** yang menjadi target proyek. Satu kata `final`
mengembalikannya.

Setelah perbaikan, seluruh paket `action/servlet` dan `common` — 595 berkas beserta
closure-nya — kompilasi bersih di `-source 1.7`. Perintahnya ada di
[08-harness-uji.md](08-harness-uji.md).

Pelajarannya: memverifikasi dengan `javac` bawaan (Java 8) **tidak cukup**. Konstruksi
Java 8 yang lolos diam-diam mencakup effectively-final, diamond operator, lambda, dan
streams.
