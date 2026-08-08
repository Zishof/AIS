# Pengujian Generic CRUD per Model

## Cakupan

Runner `GenericCrudPerModelIntegrationTest.java` menguji seluruh model konkret
`GeneralValueObject` yang terdaftar pada `hibernate.cfg.xml` utama.

Untuk setiap model runner melakukan:

1. membangun definisi administratif melalui `GenericCrudAutoDefinitionFactory`;
2. memverifikasi metadata melalui `GenericCrudRuntimeMetadataVerifier`;
3. menjalankan READ Hibernate nyata dengan `setMaxResults(1)`;
4. menguji izin dan keberadaan tabel untuk CREATE, UPDATE, dan DELETE menggunakan
   SQL zero-row (`where 1=0`) di dalam transaksi yang selalu di-rollback.

Metode zero-row tidak membuat row, tidak menaikkan sequence, dan tidak memicu
trigger row-level. Tes ini sengaja tidak membuat data bisnis sintetis karena
setiap model memiliki kombinasi foreign key, natural key, dan business rule yang
berbeda.

## Kredensial

Password tidak boleh disimpan pada source atau command line. Runner hanya membaca
environment variable `AIS_TEST_DB_PASSWORD`. Sinkronisasi skema lokal bersifat
opsional dan hanya aktif jika `AIS_TEST_SCHEMA_UPDATE=true`.

## Hasil lokal 8 Agustus 2026

- Model `GeneralValueObject` utama terpetakan: **1.444**
- Definisi Generic CRUD lulus: **1.444/1.444**
- READ Hibernate lulus: **1.444/1.444**
- CREATE/UPDATE/DELETE zero-row rollback lulus: **1.444/1.444**
- Kegagalan: **0**
- Model full CRUD non-sensitif: **1.358**
- Model sensitif/read-only: **86**
- Create otomatis aktif: **1.347**
- Update otomatis aktif: **1.358**
- Soft delete aktif (memiliki field `aktif`): **610**

Permanent delete tetap nonaktif. Model sensitif tetap read-only, dan model tanpa
field `aktif` tidak diberi soft delete otomatis. Batasan ini adalah pengaman data,
bukan kegagalan test.

Sebelum sinkronisasi skema, backup lokal dibuat di luar repository. Jangan commit
file backup database atau kredensial ke SVN/Git.
