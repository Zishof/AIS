# Lampiran Fase 0 — Audit Webapp, JSP, dan JAR

Hasil inventaris statis `src/main/webapp` (2026-08-19).

## Profil ukuran
Total byte file ±896 MB (klaim 2,2 GB dari `du` termasuk slack NTFS untuk ±29 ribu file kecil).

Kontributor besar: `WEB-INF` 430 MB (15.947 file), `help/` 196 MB (102 dokumen DOCX/PDF/PPTX), `component/` 175 MB (10.106 file), `report/` 62 MB, `img/` 26 MB.

File terbesar: video presentasi 77 MB di `WEB-INF/website/eschool/`, dokumen panduan 10–15 MB di `help/`, `ooxml-schemas-1.1.jar` 14 MB, `icons.json` FontAwesome 13,8 MB, video demo `beach.mp4/.webm` 18 MB di `component/uiux/`.

## Verdict per direktori
| Direktori | MB | Isi | Verdict |
|---|---:|---|---|
| `WEB-INF/website` | 128 | Microsite marketing eschool/ecampus/emedik (video 77 MB, 121 JPG) | **Excludable — 0 referensi kode** (tidak terjangkau browser dari WEB-INF) |
| `help/` | 196 | Dokumen panduan end-user | **Excludable / pindah ke portal docs/CDN** (11 referensi — sajikan eksternal) |
| `component/` | 175 | Bundle plugin vendor 82 MB (termasuk source map 13 MB), `uiux` 75 MB (bundle theme KEDUA + video demo), `adminlte` 7,7 MB | **Sebagian prunable**: hapus `.map`, video demo, plugin tak terpakai; duplikasi theme perlu dipetakan |
| `WEB-INF/bantuan` | 62 | 2.659 HTML bantuan generated per-layar | Runtime-required (15 ref); kandidat kompresi/eksternalisasi |
| `WEB-INF/baru` | 35 | 2.085 JSP + **18,9 MB proposal penjualan `tbu_penawaran/`** | JSP dipakai; `tbu_penawaran/` **excludable** |
| `WEB-INF/new` | 12 | 7.961 JSP generated (scaffold) | Lihat bagian JSP |
| `WEB-INF/o` | 11 | Theme ketiga (Metronic-style) + 134 JSP | Dipakai; duplikasi bundle UI ketiga |
| `WEB-INF/z` | 6 | 1.536 ZUL (UI ZK legacy) | Runtime-required (76 ref) |
| `WEB-INF/uiux` | 7 | Prototipe terbengkalai (stub "Insert title here") + JPG 5,7 MB | Verifikasi 10 ref → kemungkinan **drop** |
| `WEB-INF/sapto` | 1,3 | 60 XLSX dump data pribadi developer | **Excludable** |
| `WEB-INF/lib-zk9-ce` | 6,5 | 10 JAR ZK 9.6.0.2 (upgrade ZK5→ZK9 yang belum jadi) — TIDAK di classpath | **Excludable dari WAR** (simpan di repo bila upgrade masih direncanakan) |
| `report/` | 62 | 816 `.jasper` + 806 `.jrxml` + 72 `.bak` | `.jasper` wajib; `.jrxml` perlu (recompile runtime bila lebih baru); `.bak` **excludable** |

**Set quick-win exclude WAR produksi: `website` + `help` + `tbu_penawaran` + `lib-zk9-ce` + `sapto` + `report/**.bak` ≈ 345 MB, risiko hampir nol.**

## Pola JSP (10.252 file, 26,6 MB, rata-rata 2,7 KB)
| Kategori | Jumlah | % |
|---|---:|---:|
| Scaffold generated (`generate_new_jsp_scaffold.py`, 2026-08-06, dispatcher `WEB-INF/new/_shared/ui/page.jsp`) | 7.871 | 76,8% |
| One-liner `DynamicJspCrudGenerator.generate(<Entity>.class)` (`WEB-INF/baru/modul/*/index.jsp`) | 1.118 | 10,9% |
| Stub kosong | 3 | — |
| **Logika unik nyata** (termasuk JSP yang membuka Hibernate Session sendiri) | **1.260** | **12,3%** |

Implikasi: **8.989 JSP (87,7%) tanpa logika unik** — hanya metadata `request.setAttribute("nui*", ...)` + satu `jsp:include` ke dispatcher bersama. Kandidat konsolidasi Fase 7 (route registry/servlet + parity URL); menghemat kompilasi JSP, Metaspace, dan waktu deploy.

## WEB-INF/lib (184 JAR, 157 MB) — temuan utama
1. **`servlet_.jar` KRITIS** — membundel `javax.servlet.*` (Servlet API) di dalam WAR → risiko `LinkageError`; harus provided oleh container.
2. `jetty-6.1.26` + `jetty-util-6.1.26` — container tertanam di dalam WAR; hampir pasti tak perlu.
3. `ooxml-schemas-1.1` (14 MB) vs `poi-ooxml-schemas-3.17` (5,7 MB) — schema OOXML dobel generasi, package sama; buang yang lama (−14 MB).
4. `jasperreports-6.4.1` vs `jasperreports-javaflow-6.4.1` — build alternatif artifact yang sama; kirim salah satu (−5,4 MB).
5. Lima stack POI/Excel tumpang tindih (`poi-3.17`, `zpoi`, `jxl`, dll.); dua versi iText (2.1.7 + 5.0.3, lisensi berbeda); dua stack PDF (pdfbox + iText).
6. JAR ZK 5 tanpa versi di `lib` + mirror ZK 9.6 di `lib-zk9-ce`; dua theme ZK sekaligus.
7. `styles-1.0.1-SNAPSHOT.jar` — 7,3 MB, **nol class file**, versi SNAPSHOT di produksi.
8. Font JAR 20 MB (JhengHei 13 MB berisi satu TTF Microsoft — perhatikan lisensi; bookman berisi arial/micross).
9. 4 engine scripting (jruby 8,4 MB, groovy-all 6,7 MB, Rhino, bsh) — verifikasi pemakaian runtime.
10. Banyak JAR legacy tanpa versi (log4j 1.x EOL, commons-*, hibernate3, dll.) — split-brain commons-lang/lang3, collections/collections4, tiga jalur logging.
11. Dua generasi Google api/oauth/http client secara bersamaan.

Estimasi pengurangan lib dari item 1–4 + 7 saja: **±45–50 MB** (dengan verifikasi caller sebelum hapus, sesuai aturan Fase 8).
