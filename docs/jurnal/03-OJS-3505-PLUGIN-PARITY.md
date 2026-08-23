# Paritas 45 plugin bawaan OJS 3.5.0-5

`Required disposition` adalah target akhir yang diizinkan. `Current evidence` mencatat kondisi nyata; tidak ada item yang dianggap selesai hanya karena mempunyai baris pemetaan. Implementasi adalah clean-room native AIS.

Semua padanan plugin memakai `HibernateUtil` dan aggregate existing-first: `RepoCollection` profiles untuk konfigurasi allowlisted, `RepoItem`/`RepoBitstream` untuk DOI/OAI/galley, `RepoIntegrationEvent` untuk attempt/deposit audit, `RepoUsageEvent` untuk usage, `Notifikasi` untuk delivery, dan AIS payment abstraction. Entity existing tetap pada schema existing; hanya tabel journal-specific minimum berada di `penelitiandanpengabdian`. Integrasi import/export tidak memakai `OjsHibernateUtil`; source dibaca JDBC read-only dan BLOB memakai `LampiranJurnal` streaming.

OJS `plugin_settings`, `filter_*`, queue payload, dan executable configuration tidak disalin menjadi tabel plugin/filter/job baru. Hanya key yang mempunyai padanan native dan masuk allowlist yang ditransformasikan ke profile JSON; sisanya menjadi provenance/manual configuration. Status parity menyatakan perilaku AIS, bukan kompatibilitas executable dengan plugin PHP.

Hak melihat, mengonfigurasi, menjalankan, mengekspor, deposit, retry, dan membaca laporan plugin memakai capability stabil dalam `Tbmrole.jurnalAksesJson`, dievaluasi `JurnalAksesKatalog` secara fail-closed. `job_has_menu` hanya membuka modul dan tidak menggantikan capability tersebut; object journal, workflow state, dan assignment tetap wajib. Tidak dibuat tabel role/plugin privilege baru dan CRUD jurnal tidak diduplikasi ke `RolePrivilage`.

| # | Plugin | Required disposition | Native target | Current evidence |
|---:|---|---|---|---|
| 1 | blocks/browse | IMPLEMENTED_NATIVE | browse journal/issue/category | GAP_CONFIRMED |
| 2 | blocks/developedBy | NOT_APPLICABLE_WITH_PROOF | configurable AIS footer attribution | GAP_CONFIRMED; OJS attribution is not copied |
| 3 | blocks/information | IMPLEMENTED_NATIVE | reader/author/librarian information | GAP_CONFIRMED |
| 4 | blocks/languageToggle | MAPPED_TO_EXISTING | AIS i18n switch plus journal locale | PLATFORM_PARTIAL |
| 5 | blocks/makeSubmission | IMPLEMENTED_NATIVE | safe submission CTA | GAP_CONFIRMED |
| 6 | blocks/subscription | IMPLEMENTED_NATIVE | access/subscription information | GAP_CONFIRMED |
| 7 | generic/announcementFeed | IMPLEMENTED_NATIVE | announcement RSS/Atom | GAP_CONFIRMED |
| 8 | generic/citationStyleLanguage | IMPLEMENTED_NATIVE | citation formatting/download | GAP_CONFIRMED |
| 9 | generic/credit | IMPLEMENTED_NATIVE | CRediT contribution roles | GAP_CONFIRMED |
| 10 | generic/crossref | IMPLEMENTED_NATIVE | export/deposit/job/retry | GAP_CONFIRMED |
| 11 | generic/customBlockManager | IMPLEMENTED_NATIVE | sanitized configurable blocks | GAP_CONFIRMED |
| 12 | generic/datacite | IMPLEMENTED_NATIVE | export/deposit/test/job | GAP_CONFIRMED |
| 13 | generic/driver | IMPLEMENTED_NATIVE | DRIVER-compliant OAI behavior | GAP_CONFIRMED |
| 14 | generic/dublinCoreMeta | IMPLEMENTED_NATIVE | DC meta tags | GAP_CONFIRMED |
| 15 | generic/googleAnalytics | MAPPED_TO_EXISTING | consent-aware analytics hook | PLATFORM_REVIEW_REQUIRED |
| 16 | generic/googleScholar | IMPLEMENTED_NATIVE | Scholar meta tags | GAP_CONFIRMED |
| 17 | generic/htmlArticleGalley | IMPLEMENTED_NATIVE | sanitized HTML galley | GAP_CONFIRMED |
| 18 | generic/jatsTemplate | IMPLEMENTED_NATIVE | original JATS generator | GAP_CONFIRMED |
| 19 | generic/lensGalley | IMPLEMENTED_NATIVE | accessible JATS/XML reader | GAP_CONFIRMED |
| 20 | generic/pdfJsViewer | IMPLEMENTED_NATIVE | safe browser PDF viewer | GAP_CONFIRMED |
| 21 | generic/pflPlugin | IMPLEMENTED_NATIVE | publication facts/integrity label | GAP_CONFIRMED |
| 22 | generic/recommendByAuthor | IMPLEMENTED_NATIVE | same-author recommendations | GAP_CONFIRMED |
| 23 | generic/recommendBySimilarity | IMPLEMENTED_NATIVE | similarity recommendations | GAP_CONFIRMED |
| 24 | generic/staticPages | IMPLEMENTED_NATIVE | sanitized journal static pages | GAP_CONFIRMED; its two source tables are mapped |
| 25 | generic/tinymce | MAPPED_TO_EXISTING | safe rich-text editor abstraction | PLATFORM_REVIEW_REQUIRED |
| 26 | generic/usageEvent | IMPLEMENTED_NATIVE | normalized usage events | GAP_CONFIRMED |
| 27 | generic/webFeed | IMPLEMENTED_NATIVE | RSS/Atom | GAP_CONFIRMED |
| 28 | importexport/doaj | IMPLEMENTED_NATIVE | DOAJ export/deposit | GAP_CONFIRMED |
| 29 | importexport/native | IMPLEMENTED_NATIVE | native journal XML import/export | GAP_CONFIRMED |
| 30 | importexport/pubmed | IMPLEMENTED_NATIVE | PubMed export | GAP_CONFIRMED |
| 31 | importexport/users | IMPLEMENTED_NATIVE | safe user/profile import/export | GAP_CONFIRMED |
| 32 | metadata/dc11 | IMPLEMENTED_NATIVE | DC 1.1 mapping | GAP_CONFIRMED |
| 33 | oaiMetadataFormats/dc | IMPLEMENTED_NATIVE | OAI Dublin Core | GAP_CONFIRMED |
| 34 | oaiMetadataFormats/marc | IMPLEMENTED_NATIVE | OAI MARC | GAP_CONFIRMED |
| 35 | oaiMetadataFormats/marcxml | IMPLEMENTED_NATIVE | OAI MARCXML | GAP_CONFIRMED |
| 36 | oaiMetadataFormats/oaiJats | IMPLEMENTED_NATIVE | OAI JATS | GAP_CONFIRMED |
| 37 | oaiMetadataFormats/rfc1807 | IMPLEMENTED_NATIVE | OAI RFC1807 | GAP_CONFIRMED |
| 38 | paymethod/manual | MAPPED_TO_EXISTING | AIS manual payment/reconciliation | PLATFORM_PARTIAL |
| 39 | paymethod/paypal | MAPPED_TO_EXISTING | AIS payment provider abstraction | PLATFORM_PARTIAL; no provider hard-code |
| 40 | pubIds/urn | IMPLEMENTED_NATIVE | URN assign/resolve/audit | GAP_CONFIRMED |
| 41 | reports/articles | IMPLEMENTED_NATIVE | paged/streaming article report | GAP_CONFIRMED |
| 42 | reports/counter | IMPLEMENTED_NATIVE | versioned COUNTER report | GAP_CONFIRMED |
| 43 | reports/reviewReport | IMPLEMENTED_NATIVE | review performance/report | GAP_CONFIRMED |
| 44 | reports/subscriptions | IMPLEMENTED_NATIVE | subscription report | GAP_CONFIRMED |
| 45 | themes/default | MAPPED_TO_EXISTING | eCampus theme slots, original design | PLATFORM_PARTIAL; OJS theme/assets not copied |

Coverage: **45/45 inventoried**, **0/45 claimed complete** at discovery.

## Implementation evidence update — 2026-08-22

Discovery status above tetap menjadi baseline dan tidak ditimpa secara retroaktif. Evidence implementasi yang sudah lulus setelah discovery:

| Plugin IDs | Evidence aktual | Status release |
|---|---|---|
| PLG-008 | BibTeX, RIS, APA, Vancouver, IEEE dan CSL-JSON diuji pada artikel published | `BEHAVIOR_PASS` |
| PLG-009 | ordered/external contributor dan compatibility projection diuji | `BEHAVIOR_PASS` |
| PLG-026 | normalized VIEW/DOWNLOAD, bot exclusion, daily rebuild idempoten diuji | `BEHAVIOR_PASS` |
| PLG-033..037 | serializer OAI DC, OAI MARC, MARCXML, OAI JATS dan RFC1807 XML-safe/fail-closed diuji | `SERIALIZER_PASS`; protocol/schema validator masih wajib |
| PLG-038..039 | manual/provider-neutral payment preparation, settlement, HMAC-SHA256 callback, provider allowlist, timestamp anti-replay dan duplicate callback diuji pada AIS payment ledger | `MAPPED_BEHAVIOR_PASS`; sandbox provider eksternal riil masih wajib |
| PLG-040 | URN validation, uniqueness dan published immutability diuji pada journey publikasi | `BEHAVIOR_PASS` |
| PLG-041..044 | paged bounded article/review/subscription CSV dan COUNTER Release 5 aggregate JSON diuji dengan authorization | `BEHAVIOR_PASS`; large fixture/official COUNTER validator masih wajib |
| PLG-010,012,018,021,022,023,028..030 | native AIS/DOAJ/PubMed/JATS XML yang well-formed, publication integrity facts, dan bounded author/similarity recommendation diuji pada artikel published | `BEHAVIOR_PASS`; Crossref 5.3.1, DataCite 4.5, DOAJ 1.3, dan PubMed DTD 3.0 resmi lulus; sandbox deposit tetap wajib |
| PLG-007,027 | announcement/article Atom dan RSS diuji dari WAR final pada cold Tomcat | `CONTAINER_PASS` |
| PLG-013,033..037 | endpoint OAI cold-container, signed opaque resumption token, tamper/expiry/bounds denial dan header XML aman diuji | `PROTOCOL_PARTIAL_PASS`; validator resmi seluruh verb masih wajib |
| PLG-014,016,045 | public home/browse/search dari WAR final, metadata service dan theme JSP terpaket serta merespons 200 dengan CSP/nosniff | `CONTAINER_PARTIAL_PASS`; crawler/accessibility validator masih wajib |
| PLG-001,003,005,006,011 | journal/issue/subject browse, information blocks, submission CTA, subscription text dan bounded unique custom blocks memakai `RepoCollection.metadataProfileJson`; seluruh output JSP escaped | `BEHAVIOR_PASS`; authenticated/browser UI journey tetap wajib |
| PLG-015 | GA4 measurement ID tervalidasi; third-party script hanya setelah POST consent CSRF, HttpOnly SameSite cookie dan CSP nonce; DNT/GPC menonaktifkan load. First-party usage memakai privacy-bounded event existing | `SECURITY_BEHAVIOR_PASS`; browser network test tetap wajib |
| PLG-017,019,020 | entitlement-aware HTML/JATS/PDF viewer; HTML allowlist, JATS XXE denial, 2 MiB render bound dan restrictive CSP; cold container membuktikan missing ID 404 tanpa redirect dan mode invalid 422 | `CONTAINER_PARTIAL_PASS`; HTTP fixture valid HTML/JATS/PDF tetap wajib |
| PLG-026 | production article/file routes kini menangkap VIEW/DOWNLOAD ke `RepoUsageEvent` existing dengan daily HMAC, no-IP, bot class, referrer-host-only, DNT/GPC dan dedupe | `BEHAVIOR_PASS` |
| PLG-012 | DataCite export lulus Metadata Schema 4.5 resmi lewat HTTPS dengan external DTD disabled | `OFFICIAL_SCHEMA_PASS`; sandbox deposit tetap wajib |
| PLG-010 | Crossref 5.3.1 official full schema lulus setelah resource URL dibuat absolut/tervalidasi dan author terurut diproyeksikan dari `RepoItemContributor`; unsafe base/name fail-closed | `OFFICIAL_SCHEMA_PASS`; sandbox test deposit tetap wajib |
| PLG-018,021 | DOAJ Articles XSD 1.3 dan NLM PubMed DTD 3.0 lulus pada journey published; konfigurasi journal/publisher/ISSN memakai `RepoCollection.metadataProfileJson` existing | `OFFICIAL_SCHEMA_PASS`; sandbox/test upload provider tetap wajib |

Feed, public browse, dan AIS theme mapping telah lulus smoke cold-container; Scholar/DC metadata, PDF delivery valid-fixture, dan integration attempt ledger tetap memerlukan validator/container/sandbox test yang relevan. Dengan demikian dokumen ini tidak mengklaim paritas 45/45 sebelum exit gate Fase 13 benar-benar terpenuhi.

