# Source Manifest Perpustakaan AIS V2

## UI publik JSP

- `webapp/WEB-INF/baru/modul/pustaka/landing_page.jsp` — shell dan pemuat fragmen berurutan.
- `katalog.jsp` — discovery, facet, URL state, holdings, dan tindakan kontekstual.
- `_item_rinci.jsp` — detail bibliografi modern, metadata grid, holdings per eksemplar, rekomendasi, serta ulasan.
- `_header_perpustakaan.jsp` / `_footer_perpustakaan.jsp` — navigasi, identitas, dan bantuan.
- `beranda_anggota.jsp` — pinjaman, reservasi, favorit, dan kunjungan anggota.
- `layanan_anggota.jsp` / `_engagement_api.jsp` — bantuan pustakawan, usulan, booking fasilitas, dan interlibrary loan.
- `reader.jsp` — reader dokumen/audio/video sesuai policy akses.
- `dashboard.jsp`, `sirkulasi.jsp`, `kunjungan.jsp`, `katalogisasi.jsp`, `operasional.jsp`, `integrasi.jsp`, `denda.jsp` — workspace bertipe.

## Typed Java service

- `LibraryCatalogApi`, `LibraryCatalogSearchService`, `LibraryFacetService`.
- `LibraryCatalogSearchRequest`, `LibraryCatalogSearchResult`, `LibraryCatalogItemDto`, `LibraryHoldingDto`.
- `LibraryEngagementApi` — layanan anggota berbasis entitas AIS existing dan audit trail.
- `LibraryItemDetailService`, `LibraryMemberApi`, `LibraryWorkspaceApi`, `LibraryOperationsApi`.
- `LibraryLoginApi`, `LibraryVisitKioskApi`, `LibraryMarcApi`, `LibraryIntegrationGateway`, `LibraryOaiPmhService`.
- `LibraryScopeResolver` membatasi katalog, facet, detail, holdings, dan mutasi pada institusi/cabang aktif.
- `LibraryPermissionGuard` menyatukan pemeriksaan capability pustakawan dan administrator.

## ZKoss

- `KatalogOnlineAction` memakai `LibraryCatalogSearchService` yang sama dengan JSP.
- `ItemAction` mengelola workflow penerbitan katalog publik.
- `item.zul` dan `karya_tulis_item.zul` menyediakan tab Penerbitan Katalog Publik.
- `operasional_modern.zul` dan `katalogisasi_marc.zul` mengadaptasi workspace typed.

## Model utama

`Item`, `ItemPunyaBarcode`, `Perpustakaan`, `Anggota`, `PesananAnggota`, `ItemFavoritAnggota`, `PeminjamanPengadaanItem`, dan `PeminjamanPengadaanItemDetail`.

## Asset

- `webapp/assets/library-modern/library.css`
- `webapp/assets/library-modern/library.js`

Tidak ada CDN atau framework frontend baru.

Dokumen handoff utama: `docs/pustaka-v2/ai-handoff-2026-08-24.md`.
