# 97 — Daftar keputusan, lanjutan dok. 78

Tanggal: 2026-09-02

Dok. 78 mengumpulkan keputusan pemilik untuk dok. 63–77. Sejak itu dok. 79–96
menumpuk kumpulan baru, dan tiap keputusan terkubur di bagian §5 atau §6
dokumennya masing-masing. Halaman ini mengumpulkannya kembali menjadi satu.

Cakupannya **hanya seri dokumen ini** (79–96). Dokumen bernomor sama dari sesi
paralel tidak termasuk.

---

## A. Menunggu keputusan Anda

Diurut dari yang paling berdampak ke pengguna.

### 1. "Minimal Saldo Mengendap" tidak menahan apa-apa — dok. 95

Layar Jenis Anggota Koperasi menjanjikan *"batas saldo minimum yang tertahan dan
tidak dapat digunakan untuk transaksi."* Nilainya dapat disetel di dua
antarmuka, disimpan, dikirim ke klien — dan **nol kali dibandingkan** di server.
Satu-satunya penegak adalah layar bayar-QR di aplikasi ecanteen; kasir
mengabaikannya.

| Pilihan | Tempatnya | Risikonya |
|---|---|---|
| Tegakkan | `TopupHelper.bayarOnline` **dan** `KantinHelper.bayar` — keduanya, karena satu jalur saja persis keadaan hari ini | **mulai menolak pembayaran yang hari ini berhasil**; kalau ada jenis anggota berlantai bukan nol dan anggotanya rutin belanja di bawahnya, penjualan kasir bisa berhenti di hari pertama |
| Hapus isiannya | layar Jenis Anggota (JSP + Flutter) | administrator berhenti mengira lantainya berlaku |

Menilai risikonya menuntut melihat data produksi (berapa jenis anggota berlantai
bukan nol, seberapa sering saldo turun di bawahnya). Basis data UAT tidak dapat
diakses dari sini, jadi saya tidak menyalakannya sendiri.

### 2. Alasan reversal wajib diketik, lalu tak pernah ditampilkan — dok. 96

Server menolak reversal tanpa alasan, hanya Pemilik/Admin yang boleh, klien
menyediakan kolom ketiknya — dan tidak ada satu pun tempat yang membacanya
kembali. Berlaku pada `NotaSalesBiaya`, `PembayaranHutangSupplier`, dan
`PenerimaanPiutangCustomer`, berikut `reversalDari` (tautan ke dokumen asal).

* **Tampilkan** di layar riwayat/detail. Ini menambah field respons baru, jadi
  klien harus benar-benar membacanya — kalau tidak, `field-tanpa-pembaca.py`
  akan menandainya, dan memang seharusnya.
* **Berhenti memintanya**, kalau memang tidak ada yang perlu melihatnya.

Membiarkannya seperti sekarang berarti menuntut perhatian seorang Pemilik setiap
kali sebuah pembayaran dibatalkan, lalu membuangnya.

### 3. Biometrik: pengganti PIN atau tambahan? — dok. 85

Dialog "Verifikasi member" hanya muncul ketika PIN wajib. Label PIN berbunyi
*"Metode cadangan saat perangkat biometrik tidak tersedia"* — menyiratkan
biometrik yang utama. Server berpendapat sebaliknya.

* **Biometrik boleh menggantikan PIN** → gerbang `BiometricApi` harus menerima
  event FACE/FINGERPRINT sebagai pemenuhan syarat PIN. Itu perubahan kebijakan
  keamanan, bukan perbaikan cacat, jadi tidak saya lakukan sendiri. Buktinya
  kini sudah mengalir (dok. 85), sehingga perubahannya nanti hanya di sisi
  server.
* **PIN memang selalu wajib** → label "metode cadangan" keliru dan sebaiknya
  diperbaiki, karena menjanjikan sesuatu yang tidak berlaku.

Salinan kata-katanya sengaja tidak saya ubah: itu teks yang dilihat pengguna dan
keputusannya bergantung pada bacaan mana yang benar.

### 4. Lima setelan jenis anggota yang tak dibaca kode mana pun — dok. 95

`limit_penagihan`, `maksimal_pelanggaran`, `maksimal_potongan`,
`target_bulanan`, `target_frekuensi_belanja`. Dua di antaranya punya isian
sendiri di layar admin ("Target Belanja", "Maks SP") — diisi, tersimpan, tidak
pernah dibaca. Sama seperti no. 1: buat bekerja, atau hapus dari layar.

### 5. `hanya_perubahan` pada impor Excel produk — dok. 79

Klien selalu mengirim `hanya_perubahan: true`; server memproses semua baris.
Akibatnya hitungan "diperbarui" ikut menghitung baris yang tidak berubah, dan
tiap baris menahan lock `koperasi.produk` sehingga memperbesar permukaan
deadlock. Membayarnya menuntut memutuskan **apa artinya "tidak berubah"** —
perbandingan field yang terlalu longgar akan melewatkan perubahan yang sah, dan
itu kehilangan data yang sunyi.

Ditambah: jalur ini menulis data master produk dan tidak ada satu pun harness
basis data yang dapat dijalankan (lihat B.1), jadi mengubahnya sekarang berarti
mengubah tanpa dapat menguji.

**Dikoreksi di dok. 102 — butir ini jauh lebih kecil daripada yang tertulis
di atas.** Klaim "tiap baris menahan lock" salah: produknya managed, Hibernate
hanya menerbitkan UPDATE untuk entitas yang benar-benar berubah, dan tidak ada
timestamp yang diset tanpa syarat. Yang tersisa hanya hitungan "diperbarui"
yang keliru pada ringkasan impor.

Kedua penyumbatnya juga gugur: harness basis data kini dapat dijalankan
(dok. 101), dan "apa artinya tidak berubah" terjawab sendiri — impor ini hanya
menulis sepuluh field, jadi perbandingan atas sepuluh field itu tidak mungkin
melewatkan perubahan yang sah.

---

## B. Menunggu tindakan lingkungan

### 1. ~~Kredensial basis data UAT ditolak~~ — GUGUR, lihat dok. 101

Tiga harness bersandar-basis-data belum pernah berjalan sekali pun. Ini juga
yang membuat A.1 dan A.5 tidak dapat dinilai risikonya.

Dikoreksi di dok. 100: yang bersandar-basis-data ternyata **satu**
(`PostgreSqlInventoryLedgerIntegrationUat`), bukan tiga. Sembilan belas harness
lain di direktori yang sama bebas basis data, sudah dijalankan, dan semuanya
lulus. Kendalanya menyangkut satu berkas, bukan seluruh direktori.

Dok. 101 menutupnya sepenuhnya: harness ber-DB itu pun tidak pernah butuh
kredensial UAT. Ia menerima properti JDBC apa pun, mengizinkan `//localhost`,
membuat schema temporer, dan menghapusnya di `finally`. Dijalankan pada klaster
PostgreSQL sekali-pakai di mesin ini: **LULUS**. Dua puluh dari dua puluh
harness kini terbukti lulus, dan A.5 tidak lagi terhalang oleh butir ini.

### 2. ~~Tidak ada toolchain Dart/Flutter di mesin ini~~ — GUGUR, lihat F

Perbaikan sisi klien pada dok. 85, 86, dan 94 **sudah di-push tetapi belum
pernah dijalankan**. Yang diperiksa hanya: keseimbangan kurung, penyisipan di
kelas yang benar, dan kecocokan tiap assertion uji dengan sumber. Itu bukan
pengganti `flutter test`.

Ketiganya perlu dijalankan sebelum rilis: `harga_modal_persetujuan_test.dart`
dan `biometric_saldo_member_test.dart`.

### 3. `src/test` tidak berada di bawah SVN — dok. 82

Delapan belas harness UAT Java tidak terversi. Ia dapat hilang tanpa jejak, dan
tidak ada yang tahu versinya cocok dengan kode yang mana.

Dok. 100 memperbarui taruhannya: yang tidak terversi ada **dua puluh**, dan
sembilan belas di antaranya baru terbukti lulus hari ini. Memasukkannya ke SVN
berarti membuat jalur tingkat-atas baru (`^/` belum punya `test`), jadi itu
tetap keputusan tata letak milik Anda.

### 4. `docs/pos/74-sql-pemulihan-member-pesanan.sql`

Perlu izin produksi dan cadangan sebelum dijalankan.

---

## C. Laporan untuk pemilik modul lain

Bukan keputusan Anda kalau modulnya bukan POS, tetapi perlu diteruskan.

| Temuan | Jumlah | Perintah |
|---|---|---|
| Sakelar Konfigurasi tanpa pembaca (dok. 84) | 218 kandidat, POS **bersih** | `python docs/pos/alat/audit-sakelar-tanpa-pembaca.py --semua` |
| Field respons tanpa pembaca (dok. 92) | 57 kandidat di 295 berkas pengirim | `python docs/pos/alat/field-tanpa-pembaca.py --luas` |
| Kolom hanya-ditulis (dok. 96) | 18 kandidat, 6 sudah ditelusuri | `python docs/pos/alat/kolom-hanya-ditulis.py` |

Ketiganya **melapor**, tidak memvonis, dan selalu keluar dengan kode 0. Tiap
kandidat menuntut satu keputusan yang tidak ada di dalam kode: **seharusnya
dibaca, atau seharusnya tidak ada?**

---

## D. Penjaga yang kini berjalan

Enam alat memvonis (keluar 1 bila dilanggar), semuanya hijau saat dokumen ini
ditulis, dan masing-masing sudah dibuktikan **dapat menyala** memakai cacat yang
nyata — bukan cacat buatan:

```
kontrak-payload-pesanan      SELURUH KONTRAK TERPENUHI (21 periksaan)
aturan-stok-tiga-nilai       BENTUK ATURAN UTUH (9 periksaan)
field-tanpa-pembaca          BERSIH
payload-tanpa-pembaca        BERSIH
pintu-darurat-tanpa-kunci    SELURUH PINTU DAPAT DIBUKA DARI SETIAP JALURNYA
gerbang-peran-tanpa-katalog  SELURUH GERBANG BERKUNCI TERDAFTAR
```

Cakupannya dicetak setiap kali dijalankan (dok. 93), sehingga tidak dapat
menyusut diam-diam seperti yang terjadi pada dok. 90 dan 92.

---

## E. Yang sudah selesai sejak dok. 78

Ringkas, karena tiap barisnya punya dokumennya sendiri.

| Perbaikan | Dampak | Dok. |
|---|---|---|
| Gerbang stok tiga-nilai dipulihkan | pesanan sah berhenti ditolak "STOCK MINUS" | 73, 77 |
| Saluran peringatan pasca-transaksi disatukan | biaya menambah peringatan 9 titik → 1 | 80 |
| Lima salinan payload bayar → satu perakit | penyebab tiga cacat sebelumnya | 81 |
| Bukti verifikasi biometrik tidak lagi dibuang | jalur sidik jari/wajah tidak lagi buntu | 85 |
| Persetujuan harga modal tinggi (layar Produk) | barang promo rugi dapat disimpan | 86 |
| `returpembelian` & `pencairandiskon` didaftarkan | izin keempat endpoint kini dapat dicabut | 89 |
| Persetujuan harga modal tinggi (entri massal) | satu baris tidak lagi membunuh seluruh posting | 94 |

## F. Koreksi atas B.2

Butir B.2 menyatakan tidak ada toolchain Dart/Flutter di mesin ini, sehingga
perbaikan klien dok. 85/86/94 "sudah di-push tetapi belum pernah dijalankan".

**Butir itu sudah tidak berlaku** (docs/pos/98). Flutter ada di `C:\opt\flutter`,
tidak di PATH. Seluruh suite dijalankan: **710 uji lulus**, analisis statis
bersih. B.2 dicoret dari daftar tindakan lingkungan.

Letaknya kini direkam di `alat/akar_repo.py` (`flutter_bin()` / `dart_bin()`) dan
dipakai `alat/uji-klien.py`, sehingga tidak perlu lagi bergantung pada PATH.
