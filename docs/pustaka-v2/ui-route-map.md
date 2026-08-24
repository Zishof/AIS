# UI Route Map Perpustakaan AIS V2

| Menu | Parameter `s` | Fragmen |
|---|---|---|
| Katalog | `katalog` | `katalog.jsp` |
| Koleksi populer | `populer` | `populer.jsp` |
| Beranda anggota | `beranda_anggota` | `beranda_anggota.jsp` |
| Masuk anggota | `login_pustaka` | `login_pustaka.jsp` |
| Layanan anggota | `layanan_anggota` | `layanan_anggota.jsp` |
| Reader digital | `reader` | `reader.jsp` |
| Dashboard | `dashboard` | `dashboard.jsp` |
| Sirkulasi | `sirkulasi` | `sirkulasi.jsp` |
| Kunjungan | `kunjungan` | `kunjungan.jsp` |
| Katalogisasi | `katalogisasi` | `katalogisasi.jsp` |
| Operasional | `operasional` | `operasional.jsp` |
| Integrasi | `integrasi` | `integrasi.jsp` |
| Denda | `denda` | `denda.jsp` |
| Informasi/layanan | `_informasi_pustaka` | `_informasi_pustaka.jsp` |
| Detail item | `_item_rinci` | `_item_rinci.jsp` |

Seluruh fragmen di atas diroute melalui allow-list pada `WEB-INF/baru/pustaka.jsp`. Parameter `p` wajib bernilai `pustaka`; rute modul lain, traversal, dan nama fragmen di luar allow-list ditolak.

State katalog disimpan sebagai query URL: `query`, `searchField`, facet/filter, `sort`, `page`, `pageSize`, dan `view`. URL dapat di-refresh atau dibagikan tanpa kehilangan kondisi pencarian.
