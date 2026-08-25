# API Map Perpustakaan AIS V2

Semua endpoint tetap melalui router `/pustaka` dan JSP adaptor tipis.

| Adaptor | Action typed | Method | Guard |
|---|---|---|---|
| `_catalog_api.jsp` | `search`, `latest`, `references`, `suggestions` | GET | Allow-list parameter; hanya item Terbit |
| `_beranda_anggota_service.jsp` | `summary`, `loans`, `holds`, `favorites`, `visits` | GET | Login anggota |
| `_beranda_anggota_service.jsp` | `renew`, `hold`, `hold_cancel`, `favorite_toggle` | POST | Login anggota + CSRF + ownership |
| `_workspace_api.jsp` | dashboard/sirkulasi/kunjungan scoped | GET | Scope pengguna |
| `_operations_api.jsp` | operasi petugas, health telemetry, antrean layanan anggota | GET/POST | Role petugas + CSRF untuk mutasi |
| `_marc_api.jsp` | katalogisasi MARC | GET/POST | Role petugas + validasi typed |
| `_integrations_api.jsp` | integrasi | GET/POST | Role/admin + validasi typed |
| `_engagement_api.jsp` | referensi, riwayat, tanya pustakawan, usulan, booking, ILL | GET/POST | Login + CSRF untuk mutasi + tenant scope |
| `_login_pustaka_service.jsp` | login anggota | POST | Throttling + respons generik |
| `_welpus_service.jsp` | kunjungan kiosk | GET/POST | Validasi anggota + CSRF untuk mutasi |
| `_oai.jsp` | OAI-PMH | GET | Verb/parameter allow-list + item publik |

Browser tidak mengirim SQL, HQL, atau nama class Java pada alur modern ini. Pagination, sorting, filter, holdings, dan facet diproses server-side.

Router mempertahankan format respons ketika adaptor gagal: rute `_api`/`_service` mengembalikan JSON dengan HTTP 500 dan `_oai` mengembalikan XML, bukan halaman fallback HTML.

`LibraryPortalSecurityFilter` membungkus seluruh `/pustaka` untuk header keamanan, request ID, cache policy, dan telemetry. Search/suggestion memakai limiter per-node; pembatas cluster-wide tetap tanggung jawab gateway/reverse proxy.
