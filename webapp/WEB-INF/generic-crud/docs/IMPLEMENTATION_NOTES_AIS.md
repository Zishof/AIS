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
