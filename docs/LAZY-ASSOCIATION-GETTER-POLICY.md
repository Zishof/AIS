# Kebijakan Wajib Getter Relasi LAZY

Semua getter model yang dipetakan dengan `@ManyToOne` atau `@OneToOne` dan
`fetch = FetchType.LAZY` **WAJIB** menyelesaikan proxy melalui helper model serta menugaskan
hasilnya kembali ke field:

```java
public Akun getAkunKas() {
    akunKas = check(akunKas);
    return akunKas;
}
```

Jangan mengembalikan field relasi LAZY secara langsung. Objek model sering bertahan lebih lama
daripada session Hibernate yang memuatnya; pemanggil berikutnya akan menerima proxy detached dan
berisiko mengalami `LazyInitializationException: could not initialize proxy - no Session`.

`check(...)` adalah nama baku untuk kode baru. `chek(...)` tetap tersedia hanya sebagai alias
historis. Assign-back tidak boleh dihilangkan karena helper dapat mengembalikan instance kanonik
atau hasil reload yang berbeda dari proxy semula.

Pengecualian hanya boleh dibuat apabila getter terpetakan memang harus tetap murni saat Hibernate
melakukan flush. Pengecualian wajib memuat komentar `LAZY_GETTER_CHECK_EXCEPTION:` dan alasan
teknis yang spesifik. Pengecualian tanpa alasan tidak boleh lolos review.

## Gate pemeriksaan

Dari root checkout Git, compile dan jalankan pemeriksaan berikut:

```text
javac -source 1.6 -target 1.6 -d compile-check src/ais/database/model/test/LazyAssociationGetterSelfTest.java
java -cp compile-check ais.database.model.test.LazyAssociationGetterSelfTest src/ais/database/model
```

Untuk layout build/SVN, gunakan root `src/main/src/ais/database/model`. Commit yang menambah getter
relasi LAZY mentah harus ditolak sampai pemeriksaan menghasilkan status `OK`.
