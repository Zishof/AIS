# 89 — Gerbang yang tidak pernah menutup

Tanggal: 2026-09-02

Dok. 88 menutup dengan menyebut kelas terakhir yang belum tersentuh:

> **Pintu yang dibuka oleh peran pengguna** — belum disentuh. Bentuknya berbeda
> secara mendasar: yang membukanya bukan sesuatu yang dikirim klien, melainkan
> siapa yang login… itu menuntut pembacaan tabel hak akses, bukan pemindaian
> sumber.

Perkiraan itu keliru. Tabelnya memang tidak perlu dibaca — karena **grid
pengaturan role dibangun dari sebuah katalog di sumber**, `EbisnisMenuKatalog`.
Kunci yang tidak ada di katalog tidak pernah muncul di layar pengaturan mana
pun, dan karena itu tidak pernah dapat diberikan maupun dicabut.

Memeriksanya menemukan dua endpoint yang gerbangnya **tidak pernah menutup.**

## 1. Cacatnya

```java
if (!bolehAksiCrud(tbmuser, pemanggilRb, adminGlobalRb, supervisorRb,
        "returpembelian", "create")) { ... tolak ... }
```

`returpembelian` tidak pernah terdaftar di katalog maupun di `KUNCI_CRUD`.
Katalog punya `returpenjualan` (Retur **Penjualan**); Retur **Pembelian** tidak
pernah ditambahkan. Hal yang sama berlaku untuk `pencairandiskon` — katalog
punya `diskon`, tidak punya pencairannya.

`EbisnisMenuKatalog.bolehAksi` berbunyi:

```java
String kunciKanonik = EbisnisMenuActionRegistry.kanonik(kunciMenu);
if (kunciKanonik.length() == 0 || !KUNCI_CRUD.contains(kunciKanonik)) {
    return aksiLegacy;      // AKSI_LEGACY_DEFAULT_BOLEH = AKSI_CRUD
}
```

`AKSI_LEGACY_DEFAULT_BOLEH` persis sama dengan `AKSI_CRUD` =
{create, update, delete, approve, reject}. Keempat titik gerbang memakai
`create`, `update`, atau `delete`.

Maka gerbangnya **tidak menolak siapa pun**. Ia meloloskan setiap peran:

| Endpoint | Aksi | Hasil gerbang |
|---|---|---|
| `pencairanDiskonSimpan` | create / update | selalu lolos |
| `pencairanDiskonHapus` | delete | selalu lolos |
| `returPembelianSimpan` | create | selalu lolos |
| `returPembelianHapus` | delete | selalu lolos |

Izin keempatnya tidak pernah dapat dicabut dari peran mana pun, karena grid CRUD
tidak pernah menawarkan barisnya. Seorang admin yang mencabut seluruh centang
tetap meninggalkan keempat endpoint ini terbuka bagi setiap kasir.

Bahayanya **berlawanan arah** dengan dok. 86. Di sana pintu yang tak bisa dibuka
mengunci pengguna yang sah di luar; di sini gerbang yang tak pernah menutup
membiarkan semua orang masuk. Keduanya sunyi: kodenya tampak dijaga,
dikompilasi tanpa keluhan, dan tidak ada uji yang merah.

## 2. Perbaikannya

Kedua kunci didaftarkan — di katalog (supaya muncul di grid) dan di `KUNCI_CRUD`
(supaya gerbangnya benar-benar menggigit):

```java
DAFTAR.add(new Entri(MODUL_POS, "returpembelian", "Retur Pembelian", "desktop", "android"));
DAFTAR.add(new Entri(MODUL_POS, "pencairandiskon", "Pencairan Diskon", "desktop", "android"));
```

**Peran yang sudah ada tidak kehilangan akses.** Untuk role yang belum pernah
menyimpan grid CRUD, `objectKompatibel(crud, kunci)` mengembalikan null →
`return aksiLegacy` → tetap `true`. Yang berubah hanya: administrator kini
*dapat* membatasinya. Perubahan ini membuka kemampuan, bukan mencabut akses.

Dikompilasi bersih dengan `javac -source 1.7`.

## 3. Alatnya sadar tanda tangan, bukan menebak

Versi pertama menganggap argumen kedua selalu kunci menu, lalu menuduh **sebelas
"kunci asing"** yang ternyata semuanya nama AKSI: `create`, `delete`, `approve`,
`cancel`, `submit`, `reverse`, … Sebabnya, sebagian helper mendeklarasikan

```java
boolean bolehAksi(Tbmuser tbmuser, String aksi)          // 2 argumen
```

dengan kunci menunya tetap di dalam helper, sementara yang lain

```java
boolean bolehAksiMenu(Tbmuser tbmuser, String kunciMenu, String aksi)
```

Alatnya kini membaca deklarasi tiap gerbang lebih dulu, mencari parameter yang
benar-benar bernama `kunciMenu`/`kunci`, lalu mengambil argumen pada posisi itu.

Cakupan, diukur bukan ditebak: 58 deklarasi gerbang, 17 di antaranya berargumen
kunci, 31 kunci literal tertelusuri, dan **113 pemanggilan berkunci variabel di
luar jangkauan** — batas yang tertulis di kepala alatnya.

## 4. Dua invarian, dua kontrol negatif

| Invarian | Kontrol | Hasil |
|---|---|---|
| kunci harus ada di katalog | cabut entri `returpembelian` dari `DAFTAR` | `- returpembelian (KantinHelper.java)` — rc=1 |
| kunci beraksi CRUD harus ada di `KUNCI_CRUD` | cabut keduanya dari `KUNCI_CRUD` | `- returpembelian aksi: create, delete` — rc=1 |

Invarian kedua yang lebih tajam: ia menangkap cacat aslinya, karena di sanalah
`bolehAksi` memutuskan meloloskan atau tidak.

## 5. Kesalahan saya sendiri saat memulihkan

Kontrol negatif dikembalikan dengan `replace('"returpenjualan",', …, 1)` — yang
mengenai kemunculan **pertama di berkas**, yaitu baris `new Entri(...)`, bukan
blok `KUNCI_CRUD`. Hasilnya label menu berubah menjadi `"returpembelian"` dan
sisanya bergeser menjadi nama platform.

`svn diff` juga menunjukkan **293 baris berubah** untuk suntingan yang seharusnya
delapan: penulisan ulang byte-mode saya menormalkan EOL pada berkas yang
ternyata ber-EOL campuran (781 CRLF dari 930 baris).

Keduanya diperbaiki dengan `svn revert` lalu penyambungan tingkat-byte yang
mempertahankan EOL tiap baris apa adanya, dan pencarian `KUNCI_CRUD` dibatasi ke
rentang bloknya sendiri. Diff akhir: **10 baris**, persis yang dimaksud.

Dicatat di sini karena keduanya adalah jebakan yang berulang di repositori ini,
bukan kecelakaan sekali lewat: berkas ber-EOL campuran, dan `replace(..., 1)`
atas string yang muncul lebih dari sekali.

## 6. Yang dipelajari

**"Butuh basis data" perlu diperiksa, bukan diasumsikan.** Dok. 88 menutup kelas
ini dengan alasan yang terdengar masuk akal dan ternyata salah. Yang menentukan
bukan di mana datanya tersimpan, melainkan di mana **daftar pilihannya**
dibangun — dan itu ada di sumber.

**Gerbang yang salah bisa gagal ke dua arah.** Empat dokumen terakhir mengejar
pintu yang tidak bisa dibuka. Bentuk kebalikannya — gerbang yang tidak pernah
menutup — lebih berbahaya dan lebih sunyi, karena tidak ada pengguna yang
mengeluh tentang izin yang terlalu longgar.
