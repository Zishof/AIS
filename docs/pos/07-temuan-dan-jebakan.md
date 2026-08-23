# 07 — Temuan, cacat, dan jebakan

Hal-hal yang tidak kelihatan dari kode sekilas, dan sudah menelan waktu sekali. Ditulis
supaya tidak menelan waktu kedua kali.

---

## 1. Id anggaran bernilai NEGATIF

Seluruh baris `rab.workspace` pada basis data ini memakai id **negatif** 19 digit
(mis. `-7223372036854765776`). Tabel tetangganya (`rab.satuan_kerja`, `akunting.akun`,
`public.jenis_kas_*`) tetap positif.

**Akibatnya:** penjaga bergaya `if (workspaceId <= 0) tolak("Anggaran belum dipilih")` —
pola yang benar untuk tabel ber-id serial — menolak **setiap anggaran yang sah**, tanpa
gejala yang jelas. Pada `UangMukaApiHelper` hal ini membuat pemilihan anggaran tidak
pernah bisa lolos, dan pada rincian kas membuat baris kehilangan `workspace`-nya
diam-diam sehingga pagu tidak pernah terpotong.

**Aturannya:** untuk id anggaran, "belum dipilih" berarti `== 0`, bukan `<= 0`.

Nilainya juga melampaui jangkauan aman angka JavaScript, jadi bila menyeberang ke klien
sebaiknya dikirim sebagai **teks** — `AnggaranKeuanganUtil.cari` menyertakan `idTeks` di
samping `id`, dan `formula` rincian memang menyimpannya sebagai teks.

---

## 2. Batal posting melaporkan sukses tanpa melakukan apa pun

**Terbukti** lewat harness `TesBatalKasKecil`: aksi batal posting menjawab
*"1 dokumen Kas Kecil posting-nya dibatalkan"*, padahal `posting_history` dokumen tetap
terisi dan baris `grup_transaksi`-nya masih ada.

**Penyebabnya** `HibernateUtil.currentSession()`. Di dalam permintaan ZK, kerangkanya yang
membuka dan menutup transaksi sehingga perubahannya tersimpan. Dipanggil dari API, tidak
ada yang meng-commit — perubahannya hilang begitu saja, tetapi jumlah dokumen yang
"diproses" tetap dihitung dan dilaporkan sebagai keberhasilan.

**Perbaikannya:** seluruh `batalkanPostingSemua` memakai `currentNativeSession()` dengan
transaksi eksplisit per dokumen, menghapus baris `akunting.transaksi` sebelum
`akunting.grup_transaksi`, dan rollback bila satu dokumen gagal. Aturan "jurnal yang sudah
closing tidak disentuh" (`closing is null`) dipertahankan apa adanya.

Berlaku pada kelima modul: Kas Kecil, Kas Besar, Pertanggungjawaban, Pertanggungjawaban
Kas Besar, Uang Muka, dan Penggantian Kas Kecil.

---

## 3. NullPointerException pada Pertanggungjawaban Kas Besar

`populateAkun` membaca rantai
`kasBesar.getDaftarPengajuanTransfer().getProsesTransfer().getCaraPembayaranTransfer()`
tanpa memeriksa `getDaftarPengajuanTransfer()` lebih dulu. Dokumen yang belum masuk DPC
melempar NPE — padahal akun itu **hanya dipakai bila ada dana yang dikembalikan**.

Sekarang rantainya dibaca bertahap. Bila ada selisih tetapi akun kelebihannya belum
diketahui, dokumennya dilewati alih-alih menulis jurnal yang tidak seimbang.

---

## 4. Getter terhitung — jangan percaya kolom mentah

Beberapa entitas punya getter yang **menghitung ulang** nilainya, dan karena Hibernate
memetakan lewat properti, hasil hitungan itulah yang tersimpan:

| Getter | Perilaku |
|---|---|
| `UangMuka.getStatus()` | mengembalikan `Disetujui` hanya bila `disetujuiOleh != null`; bila kolom status berisi `Disetujui` tetapi penyetujunya kosong, ia **turun** kembali menjadi `Pengajuan` |
| `UangMuka.getWorkspace()` | untuk dokumen berbasis PR, mengembalikan anggaran milik PR dari kolom `angarans` |
| `UangMuka.getAkun()` | menurunkan akun dari `getWorkspace()` bila ada |
| `PenggunaanAnggaran.getAktif()` | untuk uang muka: `aktif && status != DITOLAK` |

Konsekuensi praktis: menyetujui uang muka **wajib** mengisi `disetujuiOleh`, dan
persetujuannya sendiri menuntut Jenis Uang Muka (Akun Penerima) sudah dipilih.

---

## 5. Kolom yang namanya tidak seperti dugaan

| Entitas | Tabel |
|---|---|
| `UangMuka` | `public.uang_muka` — **bukan** `akunting.uang_muka` |
| PR detail | `asset.permintaan_pengadaan_master_asset_detail`, kolom `masterasset`, `hargabeli`, `jumlahdatang` (tanpa garis bawah) |
| `jenis_uang_muka` | `akun_kelebihan`, `akun_sponsor` (**dengan** garis bawah) |
| `uang_muka` | `ambildaripr`, `angarans`, `permintaanpengadaanmasterassets` (tanpa garis bawah) |

`asset.permintaan_pengadaan_master_asset.dibuat_oleh` dan `disetujui_oleh` ber-FK ke
`tbmuser(userid)` — harus berisi pengguna yang benar-benar ada.

---

## 6. Working copy dipakai bersama — dan commit borongan itu ulah kita sendiri

Working copy SVN ini dipakai beberapa sesi kerja sekaligus. Sebagian besar revisinya
berpesan **kosong** dan memborong berkas lintas modul milik beberapa sesi.

> **Koreksi.** Dokumen ini semula menyimpulkan ada **alat otomatis** (watcher/IDE) yang
> menyapu working copy. **Kesimpulan itu salah**, dan ditulis oleh saya bersama dua sesi
> lain yang sama-sama keliru. Pengintaian proses selama tujuh menit — cuplikan daftar
> proses tiap 700 ms lengkap dengan rantai induk sampai tiga tingkat — menangkap 15
> proses svn, dan **seluruhnya berinduk pada `claude.exe` lewat `bash.exe`**. Tidak satu
> pun berasal dari penjadwal, layanan, Startup, registry Run, hook Claude Code, maupun
> skrip mana pun; semuanya sudah dicari dan nihil. Kadensi 1–7 menit itu pun melebar
> menjadi 10–49 menit ketika mesin lebih sepi — pola aktivitas orang/agen, bukan timer.

Jadi penyebabnya sederhana: **`svn commit` yang dijalankan di direktori working copy
tanpa menyebut berkas**, sehingga menyapu seluruh isinya dengan pesan kosong.

Berkas `commit.sh` yang sempat ditemukan (satu baris, `svn commit` pesan kosong atas
wildcard, plus kata sandi SVN dalam teks polos) memang ada dan sudah dihapus dari
repositori pada r78015 beserta seluruh salinan lokalnya — tetapi ia hanya menjelaskan
**bentuk** commit-nya, bukan penyebabnya. Sapuan tetap berlanjut sesudah berkas itu
lenyap.

**Aturan yang harus dipegang setiap sesi:**

1. **Sebut berkas satu per satu** pada setiap `svn commit`, dan **selalu** sertakan
   `-m`/`-F`. Jangan pernah `svn commit` polos di akar working copy.
2. Sebelum menyatakan sesuatu belum tersimpan, **verifikasi isinya di HEAD**
   (`svn cat`), bukan hanya `svn status` — pekerjaan sering sudah masuk repositori
   sebelum sempat di-commit sendiri.
3. **Alasan perubahan tetap sebaiknya ditulis di kode.** Selama masih ada sesi yang
   commit borongan, pesan commit bisa hilang; Javadoc tidak.

**Memulihkan pesan yang hilang.** Secara teori bisa lewat
`svn propset --revprop -r <rev> svn:log -F pesan.txt`. Pada peladen ini perintah tersebut
**ditolak**: `E165006 — Repository has not been enabled to accept revision propchanges`.
Mengaktifkannya perlu hook `pre-revprop-change` di sisi peladen, dan itu wewenang
administrator.

**Jebakan here-string PowerShell.** Menulis pesan commit dengan `-m @'...'@` hanya aman
bila `'@` penutupnya berada di **kolom 0**. Bila menjorok, karakter `@` ikut masuk ke
pesan — r78055 lahir dengan pesan berawalan `[@ docs/pos: ...]` karena itu, dan tidak
bisa dibersihkan karena revprop ditolak. Dua sesi sudah tertipu jebakan ini. Cara yang
aman: tulis pesannya ke berkas lalu `-F berkas`, atau pakai heredoc `<<'MSG'` di Bash.

**Catatan keamanan yang belum tuntas:** kata sandi SVN pada `commit.sh` sudah ada di
riwayat repositori sejak r73553. Menghapus berkasnya **tidak** mencabut nilainya dari
riwayat — kata sandinya tetap perlu diganti secara terpisah.

---

## 7. Sesi Hibernate tertutup di tengah posting — nol jurnal, nol pesan

**Terbukti** lewat harness `TesPostingTagihanVendor` pada dokumen nyata: API menjawab
*"1 dokumen memenuhi syarat, tetapi tidak satu pun berhasil diproses"*, dan Error Log kosong.

**Penyebabnya** mesin posting mengambil sesi SEKALI di awal
(`HibernateUtil.currentNativeSession()`) lalu memakainya untuk seluruh loop, sementara helper
master data yang ikut terpanggil di tengah loop menutup sesi thread-local di blok `finally`
miliknya sendiri. Di dalam ZK tidak terlihat — kerangka ZK mengelola sesinya sendiri. Dari
API, `begin()` berikutnya melempar `org.hibernate.SessionException: Session is closed!`.

**Perbaikannya** satu baris sebelum setiap `begin()`:
`session = HibernateUtil.currentNativeSession();` — mengembalikan sesi yang sama bila masih
hidup, membuka yang baru bila sudah tertutup. Dipasang di **sembilan** kelas mesin posting.

**Pelajaran yang lebih umum:** di luar ZK, jangan pernah menganggap sesi thread-local bertahan
selama satu blok kode. Ambil ulang di setiap batas transaksi.

---

## 8. Pelapor galat ZK menelan sebab kegagalan di jalur API

`Common.tampilErrorJikaAdmin(e)` membangun message box ZK (`MyMessageboxConfig` →
`Common.getBahasaConfig` → query Hibernate). Dari jalur API ia:

1. tidak menampilkan apa pun — sebab aslinya **hilang**; dan
2. pada sesi yang sudah rusak, melempar exception BARU
   (`AssertionFailure: null id ... don't flush the Session after an exception occurs`) yang
   menutupi kegagalan aslinya dan menghentikan sisa batch.

Cacat (7) di atas tersembunyi persis karena ini. **Perbaikannya**: di dalam blok statis non-ZK,
pelapornya diganti `ais.common.ErrorAuditUtil.record(e, "<Kelas> jalur API")`. Kode layar ZK
tidak diubah — di sana message box memang berguna.

---

## 9. Layar ZK menandai dokumen "sudah diposting" walau jurnalnya gagal

Pada `PostingPengadaanAction.onPostingSemua`, `postingHistory` dipasang **di luar** blok
penyimpanan jurnal: bila `saveTransaksi` gagal, dokumennya tetap ditandai terposting dan
hilang dari daftar draft — tidak pernah diulang, dan tidak ada yang tahu jurnalnya tidak ada.
Layar juga menambahkan akun kredit `null` apa adanya bila akun utangnya belum diketahui,
sehingga jurnalnya cacat.

Versi API sengaja berbeda pada dua titik itu: dokumen berjurnal tidak lengkap **dilewati**,
dan penanda posting dipasang **hanya setelah** jurnalnya tersimpan. Dokumen `AW-00000005` di
basis UAT adalah contoh nyata kasus pertama (tanpa penerimaan maupun penyedia).

---

## 10. `PostingHistory.getNama()` meledak bila pengguna null

Getter itu membaca `tbmuser.ambilPegawai()` tanpa penjaga null padahal kolomnya
`nullable = false`. Memanggil mesin posting dengan pengguna null tidak gagal di tempat yang
jelas, melainkan saat flush — dengan
`PropertyAccessException: Exception occurred inside getter of ... PostingHistory.nama`.

Di produksi pengguna selalu ada (dari sesi POS), jadi modelnya sengaja **tidak** diubah.
Yang perlu diingat: setiap harness dan setiap pemanggil baru wajib memberi `Tbmuser` nyata.

---

---

## 11. Termin berpajak tidak pernah ditandai terposting

Pada `PostingPemesananPekerjaanAction.onPostingSemua`, penanda `postingHistory` hanya dipasang
di cabang **tanpa** pajak. Termin yang berpajak tetap ditulis jurnalnya, tetapi dokumennya
tidak pernah ditandai — jadi ia muncul terus sebagai draft. Pengguna menekan tombol posting
berulang kali; penjaga anti-jurnal-ganda (`GrupTransaksi` dengan `ref` = kunci termin sudah
ada) menolaknya setiap kali, tanpa penjelasan.

Versi API memasang penandanya di kedua cabang, setelah jurnalnya benar-benar tersimpan.

---

## 12. Boolean dari JDBC adalah `'t'`/`'f'`, bukan `true`/`false`

Harness yang membaca nilai semula sebuah kolom boolean lewat `ResultSet.getString` menerima
`"t"` atau `"f"`. Disisipkan kembali ke SQL tanpa tanda kutip, Postgres membacanya sebagai
**nama kolom**: `ERROR: kolom "f" tidak ada`. Akibatnya bukan sekadar galat — pemulihan
berhenti di tengah dan basis UAT tertinggal dalam keadaan skenario uji.

Dua pelajaran:

1. beri tanda kutip pada nilai boolean yang dipulihkan (atau simpan sebagai `boolean`, bukan
   `String`);
2. **pemulihan menyeluruh harus dipersempit ke baris yang memang disentuh.** Perbaikan
   pertama yang terpikir — `set bytermin = false where bytermin = true` — akan merusak delapan
   pemesanan lain yang ternyata sudah bertermin sejak semula. Baca dulu keadaan sebenarnya,
   baru pulihkan.
