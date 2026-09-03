# Perbaikan build `Junction cannot be be converted to Disjunction`

Tanggal: 3 September 2026

## Hasil

Kegagalan kompilasi pada `ais/common/Common.java` baris pencarian NIDN dosen dan identitas anggota telah diperbaiki. Build Maven penuh berhasil setelah perubahan.

## Gejala

Compiler Java melaporkan dua kesalahan:

```text
incompatible types: Junction cannot be converted to Disjunction
```

Kesalahan muncul pada kriteria `nidnDosen` dan `identitasDosen` dalam method `checkApakahDosenOtomatisMenjadiAnggotaPerpustakaan`.

## Akar masalah

`Restrictions.disjunction()` menghasilkan objek `Disjunction`, tetapi method `.add(Criterion)` diwarisi dari `Junction` dan mengembalikan tipe `Junction`.

Pola berikut karena itu tidak dapat di-assign ke variabel `Disjunction`:

```java
Disjunction kondisi = Restrictions.disjunction()
        .add(Restrictions.ilike("nidn", nilai, MatchMode.EXACT));
```

## Perbaikan

Objek dibuat terlebih dahulu, kemudian kriterianya ditambahkan melalui pernyataan terpisah:

```java
Disjunction kondisi = Restrictions.disjunction();
kondisi.add(Restrictions.ilike("nidn", nilai, MatchMode.EXACT));
```

Pola yang sama diterapkan pada pencarian kode dan kode identitas anggota. Logika OR tidak berubah; hanya konstruksi objek dibuat sesuai kontrak tipe Hibernate yang digunakan aplikasi.

## Verifikasi

Perintah:

```text
mvn -DskipTests compile
```

Hasil:

```text
BUILD SUCCESS
Compiling 26 source files
Total time: 14.793 s
```

Kompilasi terarah `Common.java` pada snapshot GitHub juga berhasil.

## Deployment

Perubahan berada di aplikasi server. Lakukan build dan deploy ulang WAR/server; POS Desktop tidak perlu dibangun ulang.

## Referensi perubahan

- SVN: r83992
- GitHub kode: `4fa8ec16` — `fix(common): build Disjunction criteria without chained assignment`
