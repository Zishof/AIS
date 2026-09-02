# Gerbang yang menyaring orang yang salah

Pengelolaan **Grup Pengguna & Hak Akses** dijaga oleh:

```java
if (tbmuser != null && tbmuser.getPedagang() != null
        && !Common.getApakahAdminLain(tbmuser)) {
    hasil.put("description", "Hanya admin sistem yang dapat mengelola Grup Pengguna & Hak Akses.");
    return;
}
```

Pesannya benar. Kodenya tidak menegakkannya.

Yang ditolak hanyalah akun yang **terikat ke sebuah toko**. Akun yang **tidak**
terikat Pedagang lolos begitu saja — dan `@JoinColumn(name = "pedagang",
nullable = true)`: basis pengguna AIS mencakup pegawai, guru, dan dosen yang
seluruhnya `null` di kolom itu.

## Tidak ada lapis lain di belakangnya

Diperiksa satu per satu, bukan diasumsikan:

1. **Gerbang awal `PosApi.bolehAksesActionKantin` tidak memetakan
   `ebisnis_role_*`.** Metode itu memetakan nama/prefiks aksi ke kunci menu,
   lalu **berakhir dengan `return true`** (baris 2529) — default meloloskan,
   kecuali prefiks `si_` yang memang fail-closed. Ketiga aksi peran jatuh ke
   situ.

2. **Token POS diterbitkan kepada akun AIS mana pun.**
   `PosDeviceAuthApi.terbitkanToken` memanggil `SecurityFilter.doAutoLogin(u, p, …)`
   dan menerbitkan token begitu kredensialnya sah — tanpa syarat peran,
   Pedagang, maupun toko.

3. **`Common.getApakahAdminLain(tbmuser)`** hanyalah
   `hakAkses().getRoleId().equals(Tbmrole.ADMINISTRATOR)` — pemeriksaan peran
   tunggal yang ketat. Jadi mayoritas pengguna memang bukan admin, dan syarat
   Pedagang itulah satu-satunya yang menentukan.

4. **Sisa metodenya hanya validasi masukan** (role_id wajib, menu wajib, role
   ditemukan). Tidak ada pemeriksaan wewenang kedua.

Rantainya utuh: siapa pun yang dapat masuk ke AIS dan tidak terikat toko dapat
memanggil `ebisnis_role_list` untuk memetakan sasaran, lalu
`ebisnis_role_menu_simpan` untuk menulis ulang hak akses peran mana pun —
termasuk menyalakan `supervisor`, yang di `EbisnisMenuKatalog.bolehAksi`
berarti bypass total atas seluruh pemeriksaan aksi granular.

## Perbaikannya

Gerbangnya menjadi apa yang pesannya sudah katakan sejak semula, di **ketiga**
jalur (`ebisnisRoleList`, `ebisnisRoleMenuAmbil`, `ebisnisRoleMenuSimpan`):

```java
if (!Common.getApakahAdminLain(tbmuser)) { … tolak … }
```

Supervisor toko tetap tertutup — itu memang maksud aslinya, dan sekarang
berlaku juga untuk semua orang lain yang bukan admin sistem.

**Konsekuensi operasional yang perlu diketahui:** bila selama ini ada akun
BUKAN `Tbmrole.ADMINISTRATOR` yang dipakai mengelola grup pengguna, akun itu
akan tertolak setelah perubahan ini. Itu memang yang dikehendaki gerbangnya,
tetapi bisa terasa sebagai regresi bila prakteknya berbeda dari niatnya.

## Temuan kedua: `satuan_kerja_*` tanpa gerbang sama sekali

Ditemukan dari sapuan yang sama. Prefiks `satuan_kerja_` tidak pernah dipetakan
di `bolehAksesActionKantin`, jadi keempat aksinya jatuh ke `return true`. Dan
dua di antaranya **tidak menerima `Tbmuser` sama sekali** —
`satuanKerjaHapus(JSONObject, JSONObject)` dan
`satuanKerjaAnggotaSimpan(JSONObject, JSONObject)` — sehingga tidak mungkin ada
lapis kedua. `satuanKerjaHapus` menonaktifkan satuan kerja; `satuanKerjaAnggotaSimpan`
menugaskan anggota ke satuan kerja.

Asimetrinya jelas kelalaian, bukan rancangan: saudara kandungnya
`satuanKerjaList` dan `satuanKerjaSimpan` **menerima** `Tbmuser` (walau
memakainya untuk cakupan, bukan otorisasi).

Perbaikannya mengikuti struktur yang sudah ada. Layar Satuan Kerja adalah TAB di
dalam layar Anggota, jadi prefiksnya digabungkan ke blok yang sama dengan
saudara-saudaranya:

```java
if (action.startsWith("anggota_") || action.startsWith("jenis_anggota_")
        || action.startsWith("tipe_anggota_") || action.startsWith("deposit_")
        || action.startsWith("satuan_kerja_")
        || action.startsWith("notifikasi_") || action.startsWith("sinkron_")) {
    return menu.optBoolean("anggota", true);
}
```

## Permukaan yang masih terbuka, dan batas keyakinannya

`bolehAksesActionKantin` berakhir `return true`. Dari 255 aksi yang di-dispatch
`PosApi`, **77 tidak cocok dengan satu pun cabangnya** dan hanya dijaga oleh
helper masing-masing. Sebagian besar memang dijaga di sana (sapuan hak akses
sebelumnya menutup sepuluh modul), tetapi dua yang diperiksa dalam pekerjaan ini
ternyata tidak — dan keduanya ditemukan dengan **membaca**, bukan menghitung.

Upaya mengukur ke-77 itu secara otomatis **gagal**: skrip yang memeriksa "apakah
handler menerima `tbmuser`" salah mengambil token (menangkap `if` sebagai nama
metode), sehingga angkanya tidak dapat dipercaya dan tidak dipakai. Yang
dilaporkan di sini hanya yang diverifikasi dengan membaca kodenya.

**Jadi: 77 aksi itu belum diaudit satu per satu.** Itu pekerjaan tersendiri, dan
tidak boleh dianggap bersih hanya karena dua yang diperiksa sudah diperbaiki.

Jalan yang lebih baik daripada mengaudit satu-satu adalah membalik
defaultnya — mengubah ujung `bolehAksesActionKantin` menjadi `return false`
dengan daftar putih eksplisit, seperti yang sudah dilakukan untuk prefiks `si_`.
Itu perubahan besar yang akan memutus aksi yang belum terdaftar, jadi bukan
sesuatu yang pantas diselipkan di sini.

## Penjaga

`test/gerbang_kelola_peran_test.dart` (repositori Flutter), 3 uji.

Uji kedua sempat **hijau semu**. Ia mula-mula menghitung kemunculan
`if (!Common.getApakahAdminLain(tbmuser))` dan meminta minimal tiga — padahal
pola itu dipakai **lima** kali di `KantinHelper` untuk keperluan lain, sehingga
satu jalur peran boleh saja kembali bocor tanpa mengubah jumlahnya. Sekarang
gerbangnya **diikat ke pesan penolakannya**: tiap kemunculan pesan
"Hanya admin sistem…" harus didahului gerbang admin, dan tidak boleh didahului
bentuk lama.

Uji negatifnya sendiri sempat salah sasaran: mengembalikan kemunculan
**pertama** di berkas ternyata merusak gerbang milik fitur lain, bukan salah
satu jalur peran — dan uji kedua tetap lolos. Setelah sasarannya diperbaiki
(`LastIndexOf` sebelum posisi pesan), keduanya jatuh dengan pesan yang dimaksud.
Berkas dipulihkan dan diverifikasi byte-identik.

## Catatan kompilasi

`KantinHelper.java` **tidak dapat dikompilasi hanya dengan classpath kelas
ter-deploy**: `OnlineBmtUtil` tidak ada di `webapp/WEB-INF/classes`, menghasilkan
14 galat. Dibuktikan pra-ada dengan mengompilasi versi HEAD yang tidak diubah —
galat yang sama persis. Verifikasi memakai `-sourcepath src` supaya javac
meresolusi dependensinya dari sumber; hasilnya `exit=0`, nol galat.
