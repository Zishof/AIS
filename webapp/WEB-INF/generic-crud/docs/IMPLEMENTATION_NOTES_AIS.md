# Catatan implementasi AIS

Engine berada di `ais.action.master.generic.v2`. JSP pilot adalah alias tipis;
query, mutasi, security, audit, dan export tetap di Java.

Urutan rollout:

1. deploy source dan verifikasi pilot `root/agama` dengan role uji;
2. jalankan SQL `001`, `002`, lalu `003` pada staging setelah backup dan review schema;
3. lakukan uji paging/count/search/sort, CREATE/UPDATE/soft deactivate, XLSX,
   audit row, CSRF, privilege negatif, dan scope negatif;
4. jangan mengaktifkan restore/admin delete/import sebelum adapter dan policy
   entity lulus test matrix;
5. aktifkan entity berikutnya satu per satu melalui registry/config versioned.

Permanent delete hanya menghapus row bisnis aktif. Implementasi menuntut Super
Admin, privilege DELETE, scope, policy entity, preflight, konfirmasi bertipe, dan
alasan. Histori Envers/audit tidak pernah dipurge.

## Cakupan runtime yang telah diterapkan

- Registry deny-by-default dan verifikasi field terhadap metadata Hibernate runtime.
- List/detail, pencarian, filter lanjutan, sort, paging database, serta scope yang
  sama untuk count, data, lookup, audit, dan export.
- Create/update/soft-delete dengan allow-list field, validasi, CSRF, transaksi,
  dan optimistic token.
- XLSX template, dry-run, preview, konfirmasi eksplisit, idempotensi per sesi,
  batas file/baris, dan workbook error. Tombol dan endpoint import mensyaratkan
  CREATE+UPDATE+DELETE sesuai matriks acceptance.
- Export XLSX/PDF/DOCX/PPTX yang memakai query/filter/sort/scope yang sama;
  export di atas batas sinkron ditolak dengan `EXPORT_ASYNC_REQUIRED` agar tidak
  memotong hasil secara diam-diam.
- Column preference dan private saved view terikat user+active role+entity.
- Audit row/global dan compare, restore melalui adapter/policy, serta permanent
  delete melalui preflight dan policy eksplisit. Fitur berisiko tetap off jika
  adapter/policy tidak tersedia.
- Responsive shared UI untuk filter chips, column chooser, form drawer, audit,
  export multi-format, dan import dry-run/confirm.

## Bukti verifikasi lokal 2026-08-08

- Kompilasi seluruh package `ais.action.master.generic.v2` dengan `javac
  -source 1.7 -target 1.7`: lulus.
- Tomcat 9 JspC untuk shared page dan dispatcher: 0 error.
- `node --check` untuk shared JavaScript: lulus.
- `tests/validate_implementation.py`: lulus seluruh pemeriksaan statis.

Verifikasi ini tidak menggantikan migrasi database dan UAT terhadap data/role
staging. SQL `001`–`003` sengaja tidak dijalankan otomatis karena memerlukan
backup, review schema, dan kewenangan database. Mahasiswa tetap disabled sampai
parity form/action dan adapter domain lulus test matrix.
