# Current checkout manifest

Snapshot: 2026-08-24 23:52 Asia/Jakarta.

## Identitas checkout

- Root: `C:\opt\AIS\ais`.
- `git status`, branch, dan HEAD: tidak tersedia karena folder bukan repository Git.
- `svn info` dan `svn status`: tidak tersedia karena folder bukan working copy SVN.
- Status VCS canonical: `UNVERIFIED_ON_CURRENT_CHECKOUT`.
- Compiler canonical dari `pom.xml`: Java source/target 1.8.
- Current compile: masing-masing tree canonical/mirror mempunyai 7.130 Java source; `mvn -DskipTests compile` mengompilasi 17 source incremental dan exit 0. Helper `FeederExporter.ambilNilaiData` yang sebelumnya menjadi blocker sudah tersedia kembali.
- Package yang ditemukan saat snapshot: WAR 738.228.983 byte, SHA-256 `3A885D81A4C36D21855B5144C406EA8BDFEA79881DC56C1BEF3F8100458140F5`, modified 24 Agustus 2026 21:13 WIB. WAR ini mendahului compile snapshot 23:52 dan belum dibangun ulang/cold-tested dalam run dokumentasi, sehingga bukan klaim release candidate.

## Manifest relevan

Scope deterministik mencakup kedua tree `src`/`java` untuk action/service/test jurnal, entity jurnal/repository dan `LampiranJurnal`, `Jurnal*` common, `common/newui`, seluruh `docs/jurnal`, JSP jurnal, POM, serta konfigurasi Hibernate/web. File manifest ini sendiri dikecualikan.

- Jumlah file dalam composite: 368 (`CURRENT-CHECKOUT-MANIFEST.md` dikecualikan agar hash tidak self-referential).
- Format record input hash: `relativePath|size|mtimeUtcISO|sha256`, diurutkan berdasarkan path.
- Composite SHA-256: `DB5865CD14D40183FD431B7351D81FAB86AEF3758E5BFD125BFD0BD5F9A0C29E`.

Hash dokumen input yang dibaca lengkap:

| File | SHA-256 |
|---|---|
| `12-REVIEW-ULANG-DAN-GAP-ANALYSIS-JURNAL-AIS.md` | `E454057B1D8CCC82AFA3DCC445DA43050DEB197BBAC134CAB492CF6B5129230F` |
| `13-SPESIFIKASI-UIUX-JURNAL-MODERN.md` | `F3C1FBE3673166628048D79EFCB01A19AB38E194A2135BBB90261B94CA505F59` |
| `14-PERINTAH-MASTER-CODEX-CLAUDE-JURNAL-AIS-OJS-3505-V2.md` | `D6B48E5696AF4AEC24C61137D9E451F75DEE4A9B33130009D17FF2B2EDBC3AD3` |
| `15-HANDOFF-LANJUTAN-20260824.md` | `790B0026883B9721D57145BB0175190F994ED88671AD2103501FABC0F1B245A0` |

Password database tidak masuk manifest, source, command output yang disimpan, atau dokumen.
