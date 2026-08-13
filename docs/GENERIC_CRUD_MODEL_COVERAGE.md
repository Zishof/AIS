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

## Hasil audit awal

- Model Hibernate: 1.489
- Terdaftar untuk baca/filter/ekspor: 1.489
- Model sensitif/restricted: 132
- Lifecycle Action existing: 607
- Lifecycle metadata untuk model tanpa Action: 709
- Definition route eksplisit: 5
- Action kompleks yang tetap fail-closed: 64
- Create aktif: 1.289
- Update aktif: 1.294
- Delete/soft-delete aktif: 244

Audit keamanan tambahan memperlakukan model yang mempunyai field password,
credential, token, secret, PIN, path/blob sensitif, atau data sejenis sebagai
restricted walaupun nama class-nya terlihat umum. Field wajib yang sengaja
disembunyikan juga otomatis mematikan create agar constraint tidak dilewati.

Angka di atas dihasilkan oleh `GenericCrudModelCoverageAudit`; audit berikutnya
akan memisahkan definition eksplisit dan Action kompleks yang masih memerlukan
adapter khusus.

Audit menghitung definition route eksplisit sebagai lifecycle utama untuk entity
yang sama. Model tersebut tidak lagi dilabel `UNBOUND` hanya karena browser
administratif mempunyai definition terpisah.

## Adapter kompleks yang sudah dipindahkan

- `BadanHukumAction`: form singleton ID 1, validasi kode wajib, seluruh field
  legalitas/kontak, audit, ekspor, serta larangan delete telah menjadi definition
  dan adapter native. Penyimpanan tidak merender komponen ZUL.
