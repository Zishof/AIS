# Aturan wajib cast pada native SQL

## Ketentuan

Seluruh native SQL yang dijalankan melalui Hibernate/JPA **dilarang** memakai
sintaks singkat PostgreSQL `ekspresi::tipe`. Gunakan sintaks SQL standar:

```sql
CAST(ekspresi AS tipe)
```

Aturan ini berlaku untuk semua tipe, termasuk `date`, `time`, `text`,
`integer`, `bigint`, `numeric`, `regclass`, `json`, `jsonb`, array, dan tipe
PostgreSQL lain.

## Alasan

Parser named-parameter Hibernate dapat membaca bagian `:tipe` pada `::tipe`
sebagai parameter bernama. Dampaknya kueri gagal walaupun SQL valid di psql,
misalnya:

```text
org.hibernate.QueryException: Not all named parameters have been set: [:jsonb]
```

Kegagalan ini pernah memblokir aksi `produk_simpan`, sehingga pengguna tidak
dapat memperbarui barcode produk dan pekerjaan stok opname ikut terhambat.

## Contoh

```sql
-- Dilarang
NULLIF(px.kemasan, '')::jsonb
?::date
produk.toko::text

-- Wajib
CAST(NULLIF(px.kemasan, '') AS jsonb)
CAST(? AS date)
CAST(produk.toko AS text)
```

Untuk named parameter:

```sql
-- Dilarang
DATE(a.waktu) BETWEEN :mulai::date AND :akhir::date

-- Wajib
DATE(a.waktu) BETWEEN CAST(:mulai AS date) AND CAST(:akhir AS date)
```

## Checklist review dan UAT

1. Cari pola `::[A-Za-z_]` pada seluruh source Java.
2. Bedakan SQL dari sintaks non-SQL seperti CSS `::before`/`::after` dan contoh
   edukasi di komentar.
3. Tidak boleh ada pola `::{TYPE}` di string SQL yang dieksekusi.
4. Jalankan compile-only dari akar proyek: `mvn -Dmaven.test.skip=true compile`.
5. UAT endpoint/aksi yang memakai kueri tersebut dan pastikan log tidak berisi
   `Not all named parameters have been set`.
6. Bila server belum diperbarui, jelaskan kepada pengguna bahwa klik ulang atau
   muat ulang tidak menyelesaikan masalah; admin perlu memasang revision server
   yang sudah diperbaiki.

## Pemeriksaan 29 Agustus 2026

Seluruh pola cast PostgreSQL pada string SQL aktif di source Java telah diubah
ke `CAST(... AS tipe)`. Pencarian sisa hanya boleh menemukan dokumentasi,
komentar edukasi, atau sintaks CSS/HTML yang memang bukan SQL.
