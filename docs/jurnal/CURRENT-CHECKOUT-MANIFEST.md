# Current checkout manifest

Snapshot: 2026-08-23 Asia/Jakarta.

## Identitas checkout

- Root: `C:\opt\AIS\ais`.
- `git status`, branch, dan HEAD: tidak tersedia karena folder bukan repository Git.
- `svn info` dan `svn status`: tidak tersedia karena folder bukan working copy SVN.
- Status VCS canonical: `UNVERIFIED_ON_CURRENT_CHECKOUT`.
- Compiler canonical dari `pom.xml`: Java source/target 1.8.
- Package current checkout: clean compile 7.066 Java source dan incremental compile 7.068 source pernah exit 0 sebelum perubahan paralel terbaru; targeted compile perubahan jurnal dan `war:war` final exit 0. Percobaan full compile paling akhir tertahan di luar scope jurnal oleh helper `ambilNilaiData` yang hilang pada `FeederExporter.java`. WAR 737.920.653 byte/72.115 entry, SHA-256 `23763B38132EF3026F20D0C00C9AC0D9936517C4C35233840DF5AF83237DCB71`.

## Manifest relevan

Scope deterministik mencakup kedua tree `src`/`java` untuk action/service/test jurnal, entity jurnal/repository dan `LampiranJurnal`, `Jurnal*` common, `common/newui`, seluruh `docs/jurnal`, JSP jurnal, POM, serta konfigurasi Hibernate/web. File manifest ini sendiri dikecualikan.

- Jumlah file dalam composite: 367 (`CURRENT-CHECKOUT-MANIFEST.md` dikecualikan agar hash tidak self-referential).
- Format record input hash: `relativePath|size|mtimeUtcISO|sha256`, diurutkan berdasarkan path.
- Composite SHA-256: `66DFB33395D009F1F2DD0B459D1D25B0A98E85031E75F4FC766F16CD5247F5CC`.

Hash dokumen input yang dibaca lengkap:

| File | SHA-256 |
|---|---|
| `12-REVIEW-ULANG-DAN-GAP-ANALYSIS-JURNAL-AIS.md` | `E454057B1D8CCC82AFA3DCC445DA43050DEB197BBAC134CAB492CF6B5129230F` |
| `13-SPESIFIKASI-UIUX-JURNAL-MODERN.md` | `F3C1FBE3673166628048D79EFCB01A19AB38E194A2135BBB90261B94CA505F59` |
| `14-PERINTAH-MASTER-CODEX-CLAUDE-JURNAL-AIS-OJS-3505-V2.md` | `D6B48E5696AF4AEC24C61137D9E451F75DEE4A9B33130009D17FF2B2EDBC3AD3` |

Password database tidak masuk manifest, source, command output yang disimpan, atau dokumen.
