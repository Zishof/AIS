# `adminGlobal`: radius dampaknya, supaya keputusan migrasinya bisa diambil

Dok. [98](98-audit-77-aksi-yang-jatuh-ke-default-allow.md) menyebut pola
`adminGlobal = tbmuser.getPedagang() == null` sebagai sesuatu yang **sengaja
tidak disentuh** — dan itu tetap benar. `EbisnisActorContextResolver` sudah
menyebutnya *"asumsi berbahaya … ~30 lokasi"* dan **sengaja** hanya
memperbaikinya pada permukaan `si_`, membiarkan jalur POS lama demi
kompatibilitas.

Yang belum ada adalah **dasar untuk memutuskan migrasinya**. Dokumen ini
menyediakannya: apa persisnya yang terbuka, dan seberapa jauh.

Tidak ada kode yang diubah oleh dokumen ini.

## Bukan pelebar cakupan — pintasan TOTAL

Selama ini `adminGlobal` mudah dibaca sebagai "boleh melihat semua toko".
Bukan. Ia masuk ke `KantinHelper.bolehAksiCrud`, yang berbunyi:

```java
public static boolean bolehAksiCrud(Tbmuser tbmuser, Pedagang pemanggil,
        boolean adminGlobal, boolean supervisorToko, String kunciMenu, String aksi) {
    if (adminGlobal || supervisorToko) {
        return true;                      // <-- keluar di sini
    }
    …
    return EbisnisMenuKatalog.bolehAksi(…, kunciMenu, aksi);
}
```

`adminGlobal == true` **melewati `EbisnisMenuKatalog.bolehAksi` sepenuhnya**.
Grid CRUD peran tidak pernah dikonsultasikan.

## Rantai lengkapnya

Setiap mata rantai sudah diverifikasi terpisah di dok. 97 dan 98:

1. **Token POS terbit untuk akun AIS mana pun.**
   `PosDeviceAuthApi.terbitkanToken` → `SecurityFilter.doAutoLogin(u, p, …)`,
   tanpa syarat peran, Pedagang, maupun toko.
2. **Gerbang menu default MEMBUKA.** `EbisnisMenuKatalog.urai(null)`
   mengembalikan `defaultObj()`, dan di sana seluruh kunci menu **lama** bernilai
   `true` (hanya varian Inventory & Sales yang fail-closed lewat
   `KUNCI_DEFAULT_NONAKTIF`). Peran yang belum pernah menyimpan `ebisnisMenu`
   karena itu melihat seluruh menu POS lama. `bolehAksesActionKantin` juga
   `return true` bila `role == null`.
3. **`pedagang` nullable.** Basis pengguna AIS mencakup pegawai, guru, dan dosen
   yang seluruhnya `null` di kolom itu — jadi `adminGlobal = true` bagi mereka.
4. **`bolehAksiCrud` keluar lebih awal.** Tidak ada pemeriksaan lain sesudahnya.

## Radius: 32 titik pemanggilan

`bolehAksiCrud` dipanggil **32 kali** di `KantinHelper`. Kunci dan aksi yang
dilewatinya:

| Kunci menu | Aksi | Konsekuensi bila pintasannya aktif |
|---|---|---|
| `pedagang` | create, update | **membuat akun pedagang baru, dan mengubah akun yang ada — termasuk `password_baru`** (lihat Javadoc `pedagangUbah`) |
| `anggota` | update, **delete** (3x) | mengubah dan menghapus data anggota |
| `kulakan` | create (3x), delete (2x) | membuat & menghapus dokumen pembelian |
| `stokopname` | create (4x) | membuat dokumen opname |
| `produk` | update (2x) | mengubah produk, foto produk |
| `returpenjualan` | create, update, delete | dokumen retur penjualan |
| `returpembelian` | create, delete | dokumen retur pembelian |
| `pencairandiskon` | delete | menghapus pencairan diskon |
| `pembayaran` | delete | menghapus pembayaran |

Yang paling berat adalah baris pertama: `pedagangUbah` menerima
`password_baru`, jadi pintasan ini mencakup **penyetelan ulang kata sandi akun
pedagang**.

Di luar `bolehAksiCrud`, tiga lokasi memakai `adminGlobal` sebagai gerbang
langsung — antara lain `mutasiStokSimpan` ("Hanya admin/manager atau supervisor
toko yang dapat mencatat Mutasi Stok Antar Outlet") dan `produkEksporExcel`.

## Mengapa tetap tidak diubah di sini

Bedanya dengan 30 centang mati yang dicabut di dok.
[103](103-tiga-puluh-centang-mati-dicabut.md) tegas:

> Mencabut centang yang tidak pernah berfungsi **tidak mengubah perilaku siapa
> pun**. Mencabut wewenang yang hari ini berfungsi **mengubahnya**.

Setiap akun yang hari ini mengelola POS **tanpa** baris `Pedagang` — dan itu
bentuk yang wajar untuk akun administrasi — akan tertolak begitu polanya
diperketat. Memperbaikinya tanpa daftar akun terdampak berarti menukar satu
insiden dengan insiden lain.

## Jalan yang disarankan, berurutan

**1. Kumpulkan buktinya lebih dulu — di satu titik saja.**
`bolehAksiCrud` adalah *chokepoint* tunggal untuk 32 dari lokasi itu. Satu blok
pencatatan di sana — dijalankan hanya ketika `adminGlobal` yang meloloskan
**dan** penggunanya bukan `Tbmrole.ADMINISTRATOR` — sudah cukup untuk menjawab
"akun mana yang benar-benar bergantung pada pintasan ini, untuk kunci apa".
Tidak ada perilaku otorisasi yang berubah.

Sengaja **tidak** dikerjakan dalam pekerjaan ini: menulis baris audit di sistem
berjalan adalah efek samping yang pantas diputuskan pemiliknya, dan volumenya
tidak dapat diperkirakan dari sini.

**2. Baru setelah daftarnya ada, ganti predikatnya.** Bentuk yang benar sudah
tersedia dan sudah dipakai permukaan `si_`:
`EbisnisActorContextResolver` — *"admin sesungguhnya: TANPA baris Pedagang DAN
(tanpa role sama sekali ATAU role ber-flag supervisor)"*. Perhatikan bahwa
definisi itu pun masih mengakui `pedagang == null && role == null` sebagai
admin; itu disengaja supaya akun lama tidak kehilangan akses.

**3. Naikkan akun yang terdampak menjadi admin sungguhan** sebelum memperketat,
bukan sesudah.

## Yang TIDAK ditemukan

Diperiksa dan ternyata bersih: pintasan ini **tidak** membuka jalur pengelolaan
Grup Pengguna & Hak Akses. Ketiga jalur itu memakai gerbangnya sendiri
(`Common.getApakahAdminLain`) sejak dok. [97](97-gerbang-yang-menyaring-orang-yang-salah.md),
dan tidak melewati `bolehAksiCrud`. Jadi pintasan `adminGlobal` tidak dapat
dipakai untuk menaikkan wewenang sendiri secara permanen — dampaknya terbatas
pada data, bukan pada peran.
