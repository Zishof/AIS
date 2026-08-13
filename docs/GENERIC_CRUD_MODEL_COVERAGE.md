# Cakupan Generic CRUD Seluruh Model

Audit dijalankan terhadap metadata Hibernate dan database PostgreSQL lokal
`ais`. Tujuannya memastikan New UI tidak hanya mempunyai scaffold, tetapi
mempunyai lifecycle yang jelas untuk setiap model.

## Kebijakan lifecycle

1. Bila class `*Action` existing mempunyai kontrak `init(entity)` dan
   `onSave(Event)` yang aman, validasi dan efek simpan Action tersebut tetap
   menjadi sumber kebenaran.
2. Bila model benar-benar tidak mempunyai class Action, lifecycle metadata
   Hibernate boleh digunakan setelah filter model/field sensitif, RBAC, scope
   institusi, validasi tipe, relasi allow-list, constraint database, dan
   transaksi diterapkan.
3. Bila class Action ada tetapi polanya belum dapat dipanggil secara headless,
   mutasi tetap ditolak. Sistem tidak menebak atau melewati business rule.
4. Delete generik hanya berupa soft-delete pada property `aktif`. Hard-delete
   tidak pernah diaktifkan berdasarkan tebakan.
5. Model user, role, privilege, credential, token, pembayaran, audit, file,
   log, dan kelompok sensitif lain tetap dibatasi oleh allow-list keamanan.

## Hasil audit strict terbaru

- Model Hibernate: 1.489
- Terdaftar untuk baca/filter/ekspor: 1.489
- Model sensitif/restricted: 132
- Lifecycle Action existing yang dapat dipanggil headless: 746
- Lifecycle metadata hanya untuk model tanpa Action: 572
- Definition/adapter eksplisit: 50
- Action kompleks yang tetap fail-closed dan perlu review: 27
- Create aktif: 1.300
- Update aktif: 1.314
- Delete/soft-delete aktif: 186

Audit keamanan tambahan memperlakukan model yang mempunyai field password,
credential, token, secret, PIN, path/blob sensitif, atau data sejenis sebagai
restricted walaupun nama class-nya terlihat umum. Field wajib yang sengaja
disembunyikan juga otomatis mematikan create agar constraint tidak dilewati.

Angka di atas dihasilkan oleh `GenericCrudModelCoverageAudit` setelah kebijakan
strict diterapkan. Dua puluh tujuh Action kompleks bukan dianggap selesai: mutation
ditolak sampai Action tersebut diklasifikasikan sebagai read-only atau memperoleh
adapter/service native yang mempertahankan validasi dan efek bisnis existing.

`PenumumanWebsiteAction` kini mempunyai adapter native hasil review: judul wajib,
default tanggal/kategori/status, scope perguruan tinggi, audit user, urutan tanggal
terbaru, batas baca 200, serta capability tambah/ubah tanpa delete/import. Hook
konfigurasi adapter juga dijalankan setelah field metadata dibentuk agar aturan
Action tersebut tidak tertimpa default auto-definition.

Guard institusi generik sekarang juga mengikat properti `perguruanTinggi` dari
user aktif. Sebelumnya property tenant tersebut belum ikut pada criteria baca,
count, create default, dan validasi object; kondisi itu telah ditutup tanpa
memanggil atau merender komponen ZK.

`PenugasanDosenMengajarAction` kini memakai adapter native hasil review. New UI
hanya mengizinkan edit-inline pada kode SK, tanggal surat, TMT, dan keterangan;
create/delete manual tetap dimatikan. Aksi “Generate No. SK Berdasarkan Jadwal”
tersedia sebagai custom action ber-CSRF dan privilege UPDATE, memproses tahun
akademik/semester secara transaksional, mempertahankan natural key existing,
memperbarui SKS, audit user, dan membatasi jadwal ke perguruan tinggi pengguna.

`TransaksiKoperasiDetailAction` kini mempertahankan pola checkbox existing:
record hanya dapat diubah pada field `aktif`; create, delete, import, serta edit
pokok/margin/sisa ditutup. Daftar default hanya menampilkan record aktif,
diurutkan ID terbaru, diaudit, dan untuk role anggota dibatasi ke transaksi milik
anggota tersebut.

`SertifikatKursusAction` kini tetap review-only: sertifikat diterbitkan otomatis
oleh service kursus, sedangkan New UI hanya dapat mengubah status Aktif/Dicabut.
Nomor, peserta, nilai, dan data terbit tidak dapat diedit. Aksi per-baris
“Verifikasi Publik” membuka servlet verifikasi pada tab baru melalui redirect URL
same-origin yang tervalidasi; peserta hanya dapat melihat sertifikatnya sendiri.

`KehadiranPegawaiBulananAction` telah diklasifikasikan eksplisit sebagai laporan
read/search/export. Semua metrik kehadiran immutable di layar ini, record tanpa
pegawai disaring, urutan dimulai dari tahun terbaru, dan role pegawai tanpa hak
melihat pegawai lain dibatasi pada data dirinya; scope satuan kerja digunakan
untuk role yang memang mempunyai hak melihat pegawai lain.

`DashboardAction` diklasifikasikan sebagai launcher laporan, bukan CRUD tabel
`dashboard`. Model tetap read-only dan aksi “Buka Katalog SAPTO” mengarah ke
katalog native `WEB-INF/new/sapto`, yang memuat halaman laporan SAPTO New UI;
class laporan ZK tidak lagi diinstansiasi dari nilai `clazz` di browser.

Pemetaan `PensiunAction` juga telah dikoreksi: Action tersebut sebenarnya
menurunkan `PegawaiAction` dengan filter status Pensiun, bukan mengelola entity
`employ.Pensiun`. Route New UI kini memakai entity `Pegawai`, menampilkan kolom
pegawai yang relevan, mencari NIP/nama, membatasi satuan kerja sesuai role, dan
menyediakan aksi UPDATE “Ubah ke Aktif” yang menyelaraskan status `Pegawai`,
`Dosen`, atau `Guru` dalam satu transaksi. Entity rekam pengajuan
`employ.Pensiun` dipertahankan read-only karena lifecycle mutasinya tidak terdapat
pada `PensiunAction`; sistem tidak lagi menebak CRUD dari kemiripan nama class.

`NilaiKegiatanKemahasiswaanAction` kini terikat ke entity nilai yang benar,
bukan kandidat pertama `SkalaKegiatanKemahasiswaan`. New UI hanya mengizinkan
perubahan angka nilai; relasi rincian, skala, jabatan, serta kode unik immutable.
Validasi server memastikan kombinasi relasi memang terdapat dalam konfigurasi
detail. Aksi “Lengkapi Matriks Nilai” mempertahankan perilaku renderer existing
dengan membuat hanya sel kombinasi yang belum ada secara transaksional; create,
delete, dan import generik tetap ditutup.

Parity yang sama diterapkan pada `NilaiKegiatanKesiswaanAction` menggunakan
model sekolahnya sendiri. Route tidak lagi salah memilih master skala sebagai
entity utama. Hanya angka nilai yang dapat diedit, kombinasi relasi divalidasi,
dan aksi pelengkap matriks hanya membuat sel yang belum tersedia. Database lokal
saat audit belum memiliki konfigurasi kombinasi kesiswaan, sehingga aksi tersebut
terverifikasi sebagai no-op aman sampai master rincian/skala diisi.

`PembagianShuAction` telah diakui sebagai workflow native yang sebelumnya sudah
tersedia tetapi belum tersambung ke route menu. Halaman dan endpoint
`koperasi/pembagian_shu` kini langsung memakai `NewUiPembagianShuController` dan
`NewUiPembagianShuService`: form keputusan RAT, validasi total alokasi 100%,
perhitungan jasa modal/usaha, penulisan ulang rincian anggota dalam transaksi,
scope koperasi, RBAC, CSRF, pencarian anggota, dan ringkasan nilai. Model kepala
SHU diklasifikasikan read-only pada Generic CRUD agar perhitungan rincian tidak
dapat dilewati melalui edit tabel biasa.

`PengajuanPembelianGudangAction` kini mempunyai workflow native lengkap di
`WEB-INF/new`: daftar dan form inline ambang stok per produk/gudang, pilihan
master aktif, hapus ambang, 200 pengajuan terbaru, pencarian lokal, perubahan
status, dan tombol menjalankan siklus `StokThresholdScheduler` saat itu juga.
Semua mutasi menggunakan Java service transaksional, privilege UPDATE existing,
CSRF, validasi status, dan audit user. Generic CRUD model pengajuan hanya
mengizinkan perubahan status; create/delete dan perubahan nilai hasil scheduler
tetap ditutup. Audit PostgreSQL lokal menemukan nol status invalid dan nol
pasangan ambang duplikat pada kondisi database saat pengujian.

`InterviewCalonSiswaAction` kini mempunyai workflow native lengkap untuk PSB:
filter dan pencarian sesi, tambah/edit sesi, tahun ajaran, rentang waktu,
platform konferensi beserta tautannya, kapasitas, pewawancara, gelombang,
scope sekolah/yayasan, daftar peserta, penambahan berdasarkan nomor registrasi,
waktu khusus, status kesiapan, penghapusan peserta, serta penghapusan sesi dan
seluruh assignment dalam satu transaksi. Endpoint menerapkan pemisahan privilege
CREATE/UPDATE/DELETE dan CSRF. Validasi server menolak jadwal terbalik, platform
atau tautan yang tidak konsisten, peserta duplikat, dan kapasitas penuh. Smoke
test PostgreSQL create/read/update/delete dijalankan di dalam transaksi rollback;
tidak meninggalkan data uji. Generic CRUD model sesi bersifat read-only agar
mutation tidak dapat melewati cascade peserta dan scope workflow native.

Perubahan penting dari audit awal adalah `metadataBacked` tidak lagi digunakan
bila class Action existing ditemukan. Sebelumnya kondisi tersebut dapat membuat
entity tersimpan langsung ketika pola Action tidak didukung invoker, sehingga
business rule Action berpotensi terlewati. Sekarang kondisi itu fail-closed.

Audit menghitung definition route eksplisit sebagai lifecycle utama untuk entity
yang sama. Model tersebut tidak lagi dilabel `UNBOUND` hanya karena browser
administratif mempunyai definition terpisah.

## Adapter kompleks yang sudah dipindahkan

- `BadanHukumAction`: form singleton ID 1, validasi kode wajib, seluruh field
  legalitas/kontak, audit, ekspor, serta larangan delete telah menjadi definition
  dan adapter native. Penyimpanan tidak merender komponen ZUL.
