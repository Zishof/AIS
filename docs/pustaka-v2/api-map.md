# API Map Perpustakaan AIS V2

Semua endpoint tetap melalui router `/pustaka` dan JSP adaptor tipis.

| Adaptor | Action typed | Method | Guard |
|---|---|---|---|
| `_catalog_api.jsp` | `search`, `latest`, `references`, `suggestions` | GET | Allow-list parameter; hanya item Terbit |
| `_beranda_anggota_service.jsp` | `summary`, `loans`, `holds`, `favorites`, `visits` | GET | Login anggota |
| `_beranda_anggota_service.jsp` | `renew`, `hold`, `hold_cancel`, `favorite_toggle` | POST | Login anggota + CSRF + ownership |
| `_workspace_api.jsp` | dashboard/sirkulasi/kunjungan scoped | GET | Scope pengguna |
| `_operations_api.jsp` | operasi petugas | GET/POST | Role petugas + CSRF untuk mutasi |
| `_marc_api.jsp` | katalogisasi MARC | GET/POST | Role petugas + validasi typed |
| `_integrations_api.jsp` | integrasi | GET/POST | Role/admin + validasi typed |

Browser tidak mengirim SQL, HQL, atau nama class Java pada alur modern ini. Pagination, sorting, filter, holdings, dan facet diproses server-side.
