# Current Parity Perpustakaan AIS V2

| Kemampuan | JSP | ZKoss | Sumber bisnis |
|---|---|---|---|
| Search/paging/sort server-side | Ya | Ya | `LibraryCatalogSearchService` |
| Status publik | Ya | Ya | `Item.statusTerbitItem` |
| Facet count | Ya | Service bersama | `LibraryFacetService` |
| Holdings/cabang | Ya | Ringkasan | `ItemPunyaBarcode` + pinjaman aktif |
| Reservasi | Ya, anggota aktif | Existing | `LibraryMemberApi` / `PesananAnggota` |
| Favorit | Ya, anggota aktif | Existing | `LibraryMemberApi` / `ItemFavoritAnggota` |
| Akses digital | Sesuai metadata/hak | Ya | `Item` dan lampiran |
| Detail bibliografi | Ya | Existing | `LibraryItemDetailService` |
| Penerbitan publik | Dibaca | Dikelola | `ItemAction` |
| Halaman penulis/subjek | URL katalog terfilter | Service bersama | `LibraryCatalogSearchService` |
| Rekomendasi terkait | Detail item | Service bersama | `LibraryCatalogSearchService.recommendations` |
| Pencari rak | Cabang + nomor panggil | Ringkasan holdings | `LibraryHoldingDto` |
| Rating/ulasan moderasi | Ya | Workspace petugas | `ItemKomentar` / `LibraryOperationsApi` |
| Saved-search alert | Baseline hasil baru | N/A | `SearchHistory` / `LibraryOperationsApi` |
| Discovery analytics | Dashboard | Dashboard | `LibraryWorkspaceApi` |
| RFID/self-check | Workspace integrasi | Gateway bersama | `LibraryIntegrationGateway` |
| Multi-select/bulk action | Ya | Katalog existing | `LibraryMemberApi` + RIS/client share |
| Saved search + preferensi alert | Ya | Data bersama | `SearchHistory` + `LibraryOperationsApi` |
| Reader digital | Dokumen/audio/video | Tautan ke reader yang sama | `LibraryItemDetailService` |
| Layanan anggota | Tanya, usulan, booking, ILL | Data operasional existing | `LibraryEngagementApi` |
| ISBN/barcode scan mobile | Ya, bila browser mendukung | Scanner existing | `BarcodeDetector` / katalog typed |
| Scope institusi/cabang | Ya | Ya | `LibraryScopeResolver` |
| Guard pustakawan/admin | Ya | Ya | `LibraryPermissionGuard` |

Implementasi V2 mempertahankan rute dan fungsi V1. Tidak dibuat katalog atau basis data paralel.
