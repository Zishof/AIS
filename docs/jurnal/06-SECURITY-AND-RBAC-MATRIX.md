# Security and RBAC matrix

Authorization jurnal memakai model role AIS existing. `Tbmrole` ditambah `private String jurnalAksesJson;` yang dipetakan ke `jurnal_akses_json text`; tidak dibuat master role/group jurnal atau join user-role baru. Existing `JurnalPenelitian`/Repository objects tetap pada schema existing, sedangkan `PenugasanTahapJurnal` dan tabel journal-specific minimum berada pada `penelitiandanpengabdian`, semuanya melalui main `HibernateUtil`. Source OJS tidak masuk authorization graph dan hanya dibaca JDBC read-only oleh job importer khusus.

## Keputusan terhadap implementasi existing

Audit kode menunjukkan `tokoAksesJson` hanya menyimpan array ID toko. Grid **Read/Create/Update/Delete/Approve/Reject** pada tab Hak Akses Pedagang sebenarnya dibentuk dari katalog dan disimpan pada `Tbmrole.ebisnisMenu`. Karena kebutuhan jurnal adalah capability, `jurnalAksesJson` harus mengikuti pola `ebisnisMenu`, bukan pola `tokoAksesJson`.

Perubahan runtime yang harus dibuat pada fase implementasi:

1. `Tbmrole.java`: tambah `@NotAudited @Column(name="jurnal_akses_json", columnDefinition="text") private String jurnalAksesJson;` beserta getter/setter. `@NotAudited` mencegah kebutuhan kolom baru pada tabel audit Envers existing; perubahan permission tetap wajib menghasilkan audit event eksplisit.
2. `TbmroleAction.java`: tambah tab indeks 3 **Hak Akses Pengelolaan Jurnal** setelah Hak Akses Pedagang. Isi tab dibentuk dari katalog, bukan hard-code berulang pada action/JSP.
3. `JurnalAksesKatalog.java`: satu registry/parser/evaluator untuk key menu, CRUD, workflow, migrasi versi JSON, dan default-deny.
4. Semua layar journal management berada di `C:\opt\AIS\ais\src\main\webapp\WEB-INF\baru\modul\jurnal\*`; client-side visibility tidak pernah menggantikan pemeriksaan Java service.

## Kontrak JSON

Format canonical minimum:

```json
{
  "version": 1,
  "menu": {
    "dashboard": true,
    "submissions": true,
    "settings": false,
    "import": false
  },
  "crud": {
    "submission": {
      "create": true,
      "update": true,
      "delete": false,
      "approve": true,
      "reject": true
    }
  },
  "workflow": {
    "assignEditor": true,
    "assignReviewer": true,
    "viewReviewerIdentity": false,
    "makeFinalDecision": true,
    "publish": false,
    "retract": false,
    "manageImport": false,
    "retryJob": false,
    "viewAudit": false,
    "manageSubscription": false,
    "managePayment": false,
    "manageIdentifier": false
  }
}
```

- `menu` mengatur keterlihatan/akses halaman untuk 28 submenu jurnal, tetapi tetap memerlukan parent module pada `job_has_menu`.
- `crud` memakai operasi existing `read` melalui `menu` serta `create`, `update`, `delete`, `approve`, dan `reject`. Tombol **Semua** hanya convenience UI untuk satu baris dan bukan capability tersimpan terpisah.
- `workflow` menampung operasi yang tidak aman direduksi menjadi CRUD, termasuk assign, identity reveal, decision, publish/retract, import/job, audit, subscription/payment, dan identifier/deposit.
- JSON null/kosong/rusak, versi tidak didukung, key hilang, tipe selain boolean, atau key asing menghasilkan deny. Key asing dapat dipertahankan saat round-trip rolling deployment, tetapi tidak pernah dievaluasi sebagai allow.
- Parser mengembalikan immutable normalized object; JSP/action/service tidak boleh melakukan pencarian substring atau deserialize ad hoc.
- Save memakai optimistic locking/transaction role existing dan menulis audit event dengan actor, target role, before/after SHA-256, daftar key berubah, timestamp, dan correlation ID. Secret maupun seluruh JSON tidak dicetak ke stdout.

## Lapisan keputusan efektif

```text
authenticated Tbmuser + active Tbmrole
  ∩ Menu/job_has_menu journal module entry
  ∩ Tbmrole.jurnalAksesJson capability
  ∩ tenant and JurnalPenelitian/RepoCollection ownership/scope
  ∩ journal/section/submission/stage assignment
  ∩ RepoItem workflow-state transition policy
  ∩ anonymity, conflict-of-interest, and object ownership
```

Semua lapisan harus allow. `RolePrivilage` tetap dipakai fitur menu generik existing, tetapi tidak menyimpan salinan capability bisnis jurnal dan bukan fallback allow. Multiple role pada `userRole`–`userRole5` tidak di-union; evaluator hanya memakai role aktif `Tbmuser.hakAkses()`. Site-admin override harus eksplisit, dibatasi operasi, dan diaudit.

`jurnalAksesJson` bukan object scope. Contoh: role mempunyai `assignReviewer=true`, tetapi hanya dapat melakukannya untuk `JurnalPenelitian`/`RepoItem` yang berada pada `PenugasanTahapJurnal` atau ownership yang sah. Dengan demikian satu role Editor dapat dipakai kembali tanpa memberi akses lintas jurnal.

Permission-change audit tidak membutuhkan tabel audit jurnal. Gunakan event existing yang mendukung nullable generic object/service payload (`RepoIntegrationEvent` setelah physical-schema verification) atau existing platform audit adapter; Envers tetap melindungi entity audited. Before/after JSON penuh tidak dicetak—hanya checksum, daftar key berubah, actor, role, correlation ID, dan hasil.

## Actor/operation baseline

| Operation | Public | Author | Reviewer | Editor/section editor | Production roles | Journal manager | Site admin |
|---|---:|---:|---:|---:|---:|---:|---:|
| View public metadata/open galley | policy | policy | policy | policy | policy | policy | policy |
| Create/edit own draft | no | capability + own | no | scoped assist | no | scoped | override/audit |
| View submitted manuscript | no | own visibility | assigned/blinded | capability + scope | assigned stage | journal scope | override/audit |
| Accept/decline/submit review | no | no | assigned round | no | no | no | override/audit |
| See reviewer identity | no | method policy | self | explicit capability + method + scope | no | explicit capability + scope | override/audit |
| Assign editor/reviewer | no | no | no | explicit capability + scope | no | explicit capability + journal scope | override/audit |
| Record recommendation | no | no | reviewer assignment | capability + scope | no | capability + scope | override/audit |
| Final editorial decision | no | no | no | explicit capability + scope/state | no | explicit capability + scope/state | override/audit |
| Copyedit/produce/proof | no | consultation only | no | scoped | capability + assigned stage | journal scope | override/audit |
| Schedule/publish/retract/version | no | no | no | explicit capability | capability + assigned stage | explicit capability + journal scope | override/audit |
| Settings/users/import/payment | no | no | no | no | no | separate explicit capabilities | override/audit |
| Audit/security report | no | own events only | own events only | scoped limited | scoped limited | `viewAudit` + journal scope | full audited |

## TbmroleAction tab behavior

- Tab label: **Hak Akses Pengelolaan Jurnal**; placement: after **Hak Akses Pedagang**.
- Baris menu berasal dari 28 submenu canonical `root=4605, child=460501..460528`; label boleh dari `Menu`, tetapi security key berasal dari `JurnalAksesKatalog` dan tidak bergantung pada label yang dapat berubah.
- Baris capability resource menampilkan Read/Create/Update/Delete/Approve/Reject hanya bila operasinya relevan. Operasi workflow khusus ditampilkan sebagai checkbox tersendiri dengan penjelasan scope.
- Role baru dan JSON lama/kosong mulai dengan seluruh capability `false`. Tidak boleh meniru default-allow legacy pada katalog pedagang.
- Saat edit, UI membaca normalized model; saat save, server menolak duplicate key, unknown version, payload oversized, invalid type, dan key yang tidak boleh diubah actor tersebut.
- Actor yang mengubah role harus memiliki capability administrasi role existing serta journal permission-administration override; seseorang tidak boleh memberi capability yang tidak ia miliki kecuali site-admin policy eksplisit.

## OJS role import

| OJS source | Target existing-first | Rule |
|---|---|---|
| `user_groups` | reviewed mapping to `Tbmrole` + `jurnalAksesJson` profile + provenance | no automatic grant; no new journal role table |
| `user_group_settings` | localized/source metadata in `ImportMappingOjs.rawPayload` | only recognized stable semantics transform to known capability keys |
| `user_group_stage` | capability proposal plus stage-scope provenance | does not itself grant access |
| `user_user_groups` | `Tbmuser` identity + `PenugasanTahapJurnal` | date/context respected; no global role duplication |
| `stage_assignments`, `subeditor_submission_group` | `PenugasanTahapJurnal` | exact journal/submission/stage scope and provenance required |

## Threat controls

| Threat | Required control | Regression evidence ID |
|---|---|---|
| CSRF | POST-only mutations, session token/header/form verification, SameSite cookie configuration | SEC-CSRF-01 |
| Stored/reflected XSS | default escaping; server-side rich-text allowlist; safe URL schemes | SEC-XSS-01 |
| SQL/HQL injection | parameter binding; filter/sort allowlists; no concatenated source IDs | SEC-INJ-01 |
| IDOR/cross-tenant | load by ID plus tenant/journal predicate and object policy | SEC-IDOR-01 |
| Privilege escalation | active role only; `jurnalAksesJson` fail-closed; assignment intersection; audited permission changes | SEC-RBAC-01 |
| Reviewer leak | blinded DTO/file names/metadata/log/email; method-aware identity policy | SEC-BLIND-01 |
| Review/policy JSON abuse | typed schema/version allowlist, size/depth/count limits, stable-key validation, checksum and reject unknown executable/content-bearing structures | SEC-JSON-01 |
| Review response disclosure | `PenugasanReviewerJurnal.responseJson` loaded only through assignment-scoped service; never included in generic repository DTO/search/log/audit diff | SEC-RESPONSE-01 |
| Malicious upload | size/quota, MIME sniff, extension policy, optional AV hook, safe disposition | SEC-UPLOAD-01 |
| Import traversal/archive bomb | canonical allowlist, symlink/NUL/absolute rejection, expansion limits | SEC-IMPORT-01 |
| Source DB mutation/credential leak | JDBC read-only, external secret, least-privilege account, parameter binding, timeout and redaction | SEC-SOURCE-DB-01 |
| SSRF | provider allowlist, DNS/IP validation, redirect limits, network timeouts | SEC-SSRF-01 |
| Secret/token exposure | external secrets, redaction, hashed invitation/reset tokens, expiry/one-time use | SEC-SECRET-01 |
| Duplicate mutation/email/deposit | idempotency and correlation keys; durable attempt state | SEC-IDEMP-01 |
| Audit tampering | append-only permission/domain events and restricted read paths | SEC-AUDIT-01 |
| Metrics privacy | IP retention limits, aggregation thresholds and anonymization | SEC-PRIV-01 |

## Menu hierarchy authorization

Required hierarchy is parent `root=46, child=4605`, followed by 28 children `root=4605, child=460501..460528`. `Menu.id` is a separate PK. Seeding is blocked until database collision checks cover ID, root, child, URL and label. `job_has_menu` grants module/page entry only; `jurnalAksesJson` grants business capability; scoped assignment grants object reach. Author/reviewer/student roles never receive the 28 management pages wholesale.

