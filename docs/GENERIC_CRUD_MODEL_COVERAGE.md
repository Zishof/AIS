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
- Definition/adapter eksplisit: 38
- Action kompleks yang tetap fail-closed dan perlu review: 39
- Create aktif: 1.299
- Update aktif: 1.307
- Delete/soft-delete aktif: 186

Audit keamanan tambahan memperlakukan model yang mempunyai field password,
credential, token, secret, PIN, path/blob sensitif, atau data sejenis sebagai
restricted walaupun nama class-nya terlihat umum. Field wajib yang sengaja
disembunyikan juga otomatis mematikan create agar constraint tidak dilewati.

Angka di atas dihasilkan oleh `GenericCrudModelCoverageAudit` setelah kebijakan
strict diterapkan. Tiga puluh sembilan Action kompleks bukan dianggap selesai: mutation
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
