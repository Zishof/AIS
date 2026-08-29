# Konfigurasi integrasi Repository AIS

Semua integrasi eksternal bersifat opt-in. Tanpa konfigurasi, fitur lokal tetap berjalan dan
dashboard menampilkan status `NONAKTIF` atau `FALLBACK LOKAL`. Jangan menyimpan rahasia di source.

## JVM properties

```text
-Dais.repository.storage=/opt/AIS/repository-files
-Dais.repository.virusScanner=/usr/bin/clamscan
-Dais.repository.virusScannerTimeoutSeconds=120
-Dais.repository.analyticsSalt=<rahasia-acak>
-Dais.repository.publicBaseUrl=https://repository.example
-Dais.repository.oaiTokenSecret=<rahasia-acak-minimal-32-karakter>
-Dais.repository.oaiTokenTtlSeconds=86400
-Dais.repository.semanticCandidateLimit=750
-Dais.repository.searchSynonyms=umkm=usaha mikro kecil menengah|usaha kecil;skripsi=tugas akhir|thesis
-Dais.repository.anonymousFullText=false

-Dais.repository.oaiBaseUrl=https://repository.example/ais/oai
-Dais.repository.oaiRepositoryName=Repository Institusi
-Dais.repository.oaiAdminEmail=repository@example.org

-Dais.repository.dataciteUrl=https://api.datacite.org
-Dais.repository.dataciteUser=<repository-id>
-Dais.repository.datacitePassword=<password>
-Dais.repository.datacitePrefix=10.xxxx
-Dais.repository.dataciteEvent=register

-Dais.repository.coarNotifyUrl=https://target.example/inbox
-Dais.repository.coarNotifyToken=<bearer-token>

-Dais.repository.orcidClientId=<client-id>
-Dais.repository.orcidClientSecret=<client-secret>
-Dais.repository.orcidRedirectUri=https://repository.example/orcid/callback
-Dais.repository.orcidAuthorizeUrl=https://orcid.org/oauth/authorize
-Dais.repository.orcidTokenUrl=https://orcid.org/oauth/token
-Dais.repository.rorUrl=https://api.ror.org

-Dais.repository.aiEndpoint=https://ai-gateway.internal/repository
```

Konfigurasi aplikasi berikut disimpan melalui mekanisme `Common.getKonfigurasi`:

```text
repository_search_alerts=Aktif
repository_search_alert_interval_minutes=30
```

`oaiTokenSecret` wajib stabil pada seluruh node dan seluruh restart agar token halaman OAI tetap
valid. Gunakan nilai acak berbeda dari `analyticsSalt`, minimal 32 karakter, dan simpan melalui
secret manager/server configuration. `semanticCandidateLimit` membatasi kandidat yang diranking
Tanya Repository; rentang yang diterima 100–5.000.

`publicBaseUrl` harus berupa origin publik HTTP(S) tanpa path aplikasi, query, fragment, atau
kredensial, misalnya `https://repository.example`. Nilai ini mencegah URL DOI/COAR terbentuk dari
Host header proxy yang keliru. Timeout scanner menerima 10–900 detik; proses yang melewati batas
akan dihentikan dan unggahan dicatat berstatus scan `ERROR`.

`ais.repository.anonymousFullText=false` adalah nilai bawaan: pengguna umum hanya melihat
metadata dan abstrak, sedangkan naskah lengkap memerlukan login eCampus. Ubah menjadi `true`
hanya bila kebijakan institusi memang mengizinkan unduhan anonim untuk berkas Open Access.

Gunakan endpoint test DataCite terlebih dahulu dan ubah `dataciteEvent` dari `register` ke
`publish` hanya setelah landing page permanen dapat diakses. DOI yang sudah terdaftar tidak
dihapus; record yang ditarik menggunakan tombstone.

## Validasi setelah deployment

1. Buka `/ais/repository-workspace?view=admin` dan periksa **Deployment readiness**.
2. Validasi `/ais/oai?verb=Identify`, `ListMetadataFormats`, `ListSets`, `ListIdentifiers`,
   `ListRecords`, dan `GetRecord` menggunakan validator OAI-PMH.
3. Pastikan halaman detail publik memiliki `citation_title`, `citation_author`, tahun, dan
   `citation_pdf_url` hanya untuk PDF yang dapat dibaca publik.
4. Unggah berkas uji untuk memastikan signature, checksum, scanner, ekstraksi teks, dan fixity.
5. Jalankan DataCite/COAR hanya dari item uji; periksa `repo_integration_event` melalui audit.
6. Jalankan satu pencarian tersimpan dengan alert dan verifikasi notifikasi in-app serta status
   `last_checked_at`; administrator juga dapat memakai tombol **Jalankan sekarang**.
7. Kirim satu penilaian bantuan dan pastikan agregat membantu/perlu diperjelas muncul di dashboard.

Smoke test publik yang tidak mengubah data dapat dijalankan dari server/operator:

```text
sh docs/repository-v2/validate-repository-server.sh https://HOST/ais
```

Script memeriksa portal, robots, sitemap, enam jalur OAI utama yang dapat diuji tanpa kredensial,
well-formed XML bila `xmllint` tersedia, pengambilan satu record, dan penolakan argumen OAI ilegal.
Pengujian role, upload, workflow, serta integrasi eksternal tetap harus memakai akun/data uji khusus.

Hibernate mengelola tabel/kolom tambahan. Tidak diperlukan `ALTER TABLE` manual.
