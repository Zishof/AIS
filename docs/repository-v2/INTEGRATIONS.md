# Konfigurasi integrasi Repository AIS

Semua integrasi eksternal bersifat opt-in. Tanpa konfigurasi, fitur lokal tetap berjalan dan
dashboard menampilkan status `NONAKTIF` atau `FALLBACK LOKAL`. Jangan menyimpan rahasia di source.

## JVM properties

```text
-Dais.repository.storage=/opt/AIS/repository-files
-Dais.repository.virusScanner=/usr/bin/clamscan
-Dais.repository.analyticsSalt=<rahasia-acak>
-Dais.repository.searchSynonyms=umkm=usaha mikro kecil menengah|usaha kecil;skripsi=tugas akhir|thesis

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

Hibernate mengelola tabel/kolom tambahan. Tidak diperlukan `ALTER TABLE` manual.
