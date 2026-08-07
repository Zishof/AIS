# New UI RBAC — Audit & Parity Matrix (Fase 0)

Dokumen ini adalah luaran wajib sebelum implementasi, sesuai
`PERINTAH_MASTER_CODEX_RBAC_NEW_UI_TBMROLE_TBMUSER.md` §1.2.
Berisi hasil audit source **aktual** (branch `feat/new-ui-rbac-role-user`),
kontrak hak akses existing, akar masalah, keputusan arsitektur, matriks parity,
risiko kompatibilitas, dan rencana file yang akan berubah.

> Status: **AUDIT SELESAI**. Implementasi belum dimulai. Semua nama kelas/metode di
> bawah sudah diverifikasi dari source, bukan asumsi.

---

## 1. Ringkasan temuan

| Aspek | Kondisi aktual |
|---|---|
| New UI | Ada & besar: **7.994 file** di `webapp/WEB-INF/new/`. Shell/dispatcher: `webapp/WEB-INF/new/index.jsp`. Aset: `_shared/assets/new-ui.{css,js}`. |
| Sidebar | **Statik**: `String[][] modules` di `index.jsp` (baris 29–96) merender SELURUH modul (per-paket source) ke sidebar + command palette untuk **semua** user, tanpa cek hak akses. |
| Route/service guard | **Tidak ada** otorisasi. `index.jsp` hanya cek `session["mytbmuser"]` (login) + path-safety + keberadaan file, lalu `include`/`forward` target. Semua user login bisa membuka modul/halaman/service apa pun via URL. |
| Model RBAC | `Tbmuser` (multi-role), `Tbmrole` (`getMenus()` = relasi `job_has_menu`), `Menu`, `RolePrivilage` (`role_privilage`). Ejaan **`RolePrivilage`** dipertahankan. |
| Mekanisme privilege | `CommonPrivilages.checkPrevilages(Menu, kode, Tbmuser)` — sudah benar & harus dipakai ulang. |
| Cache | Session: `current_menus`, `current_menu`. Statik JVM: `CommonPrivilages.rolePrivilagesUtama` (Map `roleId_menuId → List<RolePrivilage>`). |

**Kesimpulan keamanan:** New UI saat ini memiliki celah otorisasi menyeluruh
(broken access control). Sidebar bocor daftar modul, dan tidak ada pemeriksaan
READ/CREATE/UPDATE/DELETE/APPROVE/REJECT di sisi server untuk route New UI.

---

## 2. Kontrak hak akses existing (terverifikasi)

### 2.1 Role aktif — `Tbmuser`
`src/ais/database/model/Tbmuser.java`

- `Tbmrole getUserRole()` — role utama (slot 1).
- `Tbmrole getUserRole2()` … `getUserRole5()` — role tambahan (slot 2–5).
- `Boolean getMemilikiHakAksesTambahan()` — penanda pemakaian role tambahan.
- `List<Tbmrole> ambilRoles()` — daftar role yang benar-benar dimiliki user.
- `Tbmrole hakAkses()` — **satu role aktif** pada satu waktu (sumber kebenaran untuk semua cek).

> Aturan kritis: **jangan** meng-union privilege dari kelima role. Sistem memilih
> satu active role. Union = privilege escalation.

### 2.2 Menu role — `Tbmrole`
`src/ais/database/model/Tbmrole.java`

- `Set<Menu> getMenus()` — menu yang di-assign ke role (relasi `job_has_menu`).
- `String getRoleId()`, `String getRoleName()`, `Boolean getAktif()`.

### 2.3 Menu — `Menu`
`src/ais/database/model/Menu.java`

- `Long getId()`, `Long getRoot()` (0 = level teratas), `Long getChild()` (kunci grup anak).
- `String getLabel()`, `String getUrl()`.
- `Boolean getAktif()`, `Integer getNomorUrut()`.
- `Boolean getTampilDiPt()`, `Boolean getTampilDiSekolah()` — lingkup lembaga.
- `Menu implements Comparable` (dipakai `Collections.sort` di `CommonMenu`).

Struktur tree: menu top-level `root == 0`; anak merujuk induk via `root == induk.getChild()`
(lihat `CommonMenu.child(...)` + `buildMenuItem(...)`).

### 2.4 Privilege — `RolePrivilage` (tabel `role_privilage`)
`src/ais/database/model/RolePrivilage.java`

- `Tbmrole getRole()`, `Menu getMenu()`.
- `Integer getRead()`, `getCreate()`, `getUpdate()`, `getDelete()`, `getApprove()`, `getReject()` — `1` = diberikan.

### 2.5 Pemeriksa privilege — `CommonPrivilages`
`src/ais/common/CommonPrivilages.java`

- Konstanta kode: `READ=0`, `CREATE=1`, `UPDATE=2`, `DELETE=3`, `APPROVE=4`, `REJECT=5`.
- `boolean checkPrevilages(Menu menu, Integer kode, Tbmuser tbmuser)`:
  1. role aktif = `tbmuser.hakAkses()`; bila null → `false`.
  2. key = `roleId + "_" + menuId`.
  3. cache `rolePrivilagesUtama.get(key)`; bila kosong → query `RolePrivilage` where role+menu.
  4. cek getter sesuai kode (`getRead().equals(1)`, dst.).
- **Fail-closed**: bila tidak ada baris `RolePrivilage` → `false`.
- Overload `checkPrevilages(Integer)` & `checkPrevilages(Integer, Tbmuser)` memakai `Common.getCurrentMenu()`.

### 2.6 Pembangun menu legacy — `CommonMenu`
`src/ais/common/CommonMenu.java`

- `String loadTree(Tbmuser, HttpServletRequest)`:
  - role aktif `tbmuser.hakAkses()`.
  - `menus = tbmrole.getMenus()` (di-`refresh`), `Collections.sort`, cache session `current_menus`.
  - render tree via `generateMenuHtml` → `buildMenuItem` (rekursif root/child), filter `menu.getAktif()`.
  - URL legacy: `Common.ROOT + "/baru?p=<url-tanpa-tanda-baca>&menu=<id>"`.
- **Catatan penting:** legacy `CommonMenu` hanya menyaring **assignment** (`getMenus()`) + `aktif`;
  **belum** menegakkan `role_privilage._read=1` untuk visibilitas. New UI **harus lebih ketat**:
  tampil hanya bila `READ=1` (fail-closed), sesuai §4.2 prompt.

---

## 3. Akar masalah pada New UI

`webapp/WEB-INF/new/index.jsp`:

1. **Sidebar statik (baris 29–96):**
   ```java
   String[][] modules = new String[][]{ {"dashboard","Dashboard...","290","Utama"}, ... };
   ```
   Dirender penuh ke sidebar (baris 133–143) dan command palette (baris 155) untuk semua user.
   Sumbernya adalah **katalog paket source** (akunting, asset, …), **bukan** `Menu`/hak akses.

2. **Tanpa route guard:** target dihitung dari `module`+`page` lalu `include`/`forward`
   tanpa memeriksa apakah user berhak. Deep-link `?module=akunting&page=...` atau
   `?service=1` dapat diakses siapa pun yang login.

3. **Impedance mismatch (inti kesulitan):** route New UI berbasis **paket source**
   (`module=akunting`), sedangkan RBAC berbasis **record `Menu`** (ber-URL `.zul` legacy).
   Tidak ada pemetaan 1:1. Ini tantangan arsitektur utama (lihat §4).

---

## 4. Keputusan arsitektur

### 4.1 Sumber kebenaran hak akses
Pakai ulang kontrak existing, **tanpa** membuat tabel/kolom baru:
- role aktif: `Tbmuser.hakAkses()`;
- assignment: `Tbmrole.getMenus()` (`job_has_menu`);
- privilege: `CommonPrivilages.checkPrevilages(menu, kode, tbmuser)` (`role_privilage`).

Visibilitas menu New UI = `aktif` **AND** ada `RolePrivilage` dengan `READ=1`
(fail-closed) **AND** lolos `tampilDiPt`/`tampilDiSekolah` + konfigurasi existing.

### 4.2 Pemetaan route New UI → `Menu` (Route Registry)
Karena mismatch §3.3, sidebar New UI **tidak** lagi digerakkan oleh `String[][] modules`.
Opsi (akan difinalkan di Fase 1 dengan data `menu`):

- **Opsi A (direkomendasikan):** Sidebar New UI digerakkan oleh **daftar `Menu` role aktif**
  (seperti `CommonMenu`), lalu tiap `Menu` dipetakan ke route New UI via registry
  berbasis nilai stabil: normalisasi `Menu.getUrl()` → modul/page New UI, dengan tabel
  alias eksplisit yang disimpan di source (`NewUiRouteRegistry`). `Menu` tanpa route New UI
  yang cocok → status `UNMAPPED` (tersembunyi dari user biasa; hanya Developer Catalog admin).
- **Opsi B:** Pertahankan grouping paket, tetapi setiap entri sidebar hanya tampil bila
  minimal satu `Menu` READ-nya termapping ke modul tsб. Lebih rapuh; hanya bila Opsi A tak layak.

Keputusan final Opsi A/B membutuhkan **hasil query diagnostik** (`docs/sql/new-ui-rbac-diagnostic.sql`,
sudah tersedia dari paket instruksi) untuk melihat sebaran `Menu.url` vs modul New UI.

### 4.3 Katalog paket (`String[][] modules`)
Dipertahankan hanya sebagai **Developer Catalog** yang: butuh menu+READ admin sendiri,
bukan navigasi operasional, tidak bisa mem-bypass guard, default tersembunyi.

### 4.4 Multi-role
Satu active role via `hakAkses()`. Role switcher di topbar (POST + CSRF), server memverifikasi
role ∈ `ambilRoles()`, lalu bersihkan `current_menus`/`current_menu` + `rolePrivilagesUtama`
(role terkait) + redirect ke route pertama yang READ.

---

## 5. Matriks hak aksi (endpoint New UI → hak wajib)

| Operasi | Hak | Sumber cek |
|---|---|---|
| Buka UI/halaman, list, detail, options, meta | READ | `checkPrevilages(menu, READ, user)` |
| Create | CREATE | idem, `CREATE` |
| Update / toggle aktif | UPDATE | idem, `UPDATE` |
| Delete permanen | DELETE | idem, `DELETE` |
| Approve | APPROVE | idem, `APPROVE` |
| Reject | REJECT | idem, `REJECT` |
| Copy role | CREATE + READ (role sumber) | kombinasi |
| Switch role | role ∈ `ambilRoles()` + CSRF + POST | `NewUiRoleSwitcherService` |

Button hiding **bukan** pengamanan — guard server memeriksa ulang.

---

## 6. Parity `TbmroleAction` & `TbmuserAction`

Ukuran source (perlu ekstraksi field lengkap pada sub-langkah berikut):
`TbmroleAction.java` = **2.826 baris**, `TbmuserAction.java` = **1.912 baris**.

### 6.1 Baseline parity Role (dari model + prompt §8.5)
Halaman modern `tbmrole` wajib menyamai minimal: list/search/filter/paging; create/update;
active/inactive; unique validation roleId/kode/nama; scope (satuan kerja, fakultas, prodi,
program, yayasan, sekolah); seluruh feature-flag Boolean pada `Tbmrole`; assign/unassign `Menu`
(`job_has_menu`); kelola `RolePrivilage` (`_read/_create/_update/_delete/_approve/_reject`);
copy role beserta menu+privilege; larangan hapus role inti/terpakai (utamakan deactivate);
simpan role+`job_has_menu`+`role_privilage` **atomik** dalam satu transaksi + invalidasi cache.

### 6.2 Baseline parity User (dari model + prompt §9.4)
Halaman modern `tbmuser` wajib menyamai minimal: list/search/filter/paging; create/update;
lima slot role (`userRole`..`userRole5`), role utama wajib, tanpa duplikat, additional sesuai
`memilikiHakAksesTambahan`; password via helper enkripsi existing (`DesEncrypter`/`is_encripted`),
tidak dikirim ke browser, tidak menimpa password lama saat kosong; relasi pegawai/dosen/guru +
scope organisasi; foto via mekanisme existing; `Common.saveOrUpdateUserAccess(...)` bila diwajibkan;
sinkron `getUserRoleYgDipakai`; deactivate untuk akun bertransaksi.

> **Sub-langkah wajib berikutnya:** ekstraksi field/form/aksi lengkap dengan membaca penuh
> `TbmroleAction.java` & `TbmuserAction.java`, lalu lengkapi tabel parity kolom-per-kolom di sini
> sebelum menulis service `NewUiRoleAdminService`/`NewUiUserAdminService`. Belum dikerjakan agar
> tidak mengarang; menunggu pembacaan penuh kedua action.

---

## 7. Rencana implementasi bertahap (branch `feat/new-ui-rbac-role-user`)

| Fase | Isi | Risiko |
|---|---|---|
| **0. Audit** ✅ | Dokumen ini + branch. | rendah |
| 0b. Parity ekstraksi | Baca penuh 2 action → lengkapi §6. | rendah |
| 1. RBAC engine | `NewUiPermission`, `NewUiMenuNode`, `NewUiMenuAccessService`, `NewUiRouteRegistry` (pakai `hakAkses`/`getMenus`/`checkPrevilages`). | sedang |
| 2. Sidebar dinamis | `index.jsp` + `_shared/ui/sidebar.jsp` pakai service (buang navigasi `String[][]`); command palette sama. | sedang |
| 3. Route/service guard | `NewUiRouteGuard`, `403.jsp`/`404.jsp`, CSRF util, forward untuk service. | tinggi (keamanan) |
| 4. Role switcher | `NewUiRoleSwitcherService` + topbar + invalidasi cache. | sedang |
| 5. Manajemen Role | `tbmrole.jsp` + `NewUiRoleAdminService` parity. | tinggi |
| 6. Manajemen User | `tbmuser.jsp` + `NewUiUserAdminService` parity. | tinggi |
| 7. Cache | `NewUiCacheInvalidator` + versi global. | sedang |
| 8. Generator | perbarui generator agar tak mengembalikan sidebar statik + validasi statik. | sedang |
| 9. Uji | matriks RBAC (CHECKLIST) + build. | tinggi |

### 7.1 File direncanakan berubah/ditambah
- Java baru: `src/ais/common/newui/NewUi*.java` (Permission, MenuNode, MenuAccessService,
  RouteRegistry, RouteGuard, CsrfUtil, CacheInvalidator, RoleSwitcherService, RoleAdminService,
  UserAdminService, JsonUtil, HtmlUtil).
- JSP shell: `webapp/WEB-INF/new/index.jsp`, `_shared/ui/{sidebar,topbar,role_switcher,breadcrumb,403,404}.jsp`.
- Manajemen: `webapp/WEB-INF/new/root/maintenance/uiux/{tbmrole,tbmuser}.jsp` + `services/*_service.jsp`.
- Generator: `tools/generate_new_jsp_scaffold.py` (atau lokasi aktual — perlu dikonfirmasi keberadaannya).
- SQL: `docs/sql/new-ui-rbac-diagnostic.sql` (salin dari paket instruksi).
- Menu seed (idempotent, tanpa hard-code ID) untuk halaman role/user bila belum ada `Menu`.

---

## 8. Risiko kompatibilitas & keterbatasan

1. **Java 1.6/1.7 only** — tanpa lambda/stream/var/try-with-resources; Hibernate 3 Criteria; Spring 3.1.
2. **Tanpa build/test di sesi ini** — perubahan Java kritis-keamanan **harus dibangun (`ant`) & diuji**
   pada environment yang memiliki build (mis. environment Codex). Menulis kode kritis "buta" berisiko;
   setiap fase perlu diverifikasi build+uji sebelum lanjut.
3. **Menu↔route mapping** belum final — butuh output SQL diagnostik pada DB dev untuk memutuskan Opsi A/B.
4. **Sesi paralel (Codex)** pernah aktif di repo yang sama. Bekerja di branch fitur mengurangi
   bentrok pada `master`, tetapi edit bersamaan pada file New UI/RBAC yang sama tetap berisiko konflik.
   Koordinasi diperlukan bila Codex juga menggarap RBAC ini.
5. **LazyInitialization** — DTO `NewUiMenuNode`/`NewUiPermission` harus dibentuk sebelum session ditutup.
6. **Multi-node cache** — invalidasi lintas node belum ada; jangan diklaim tanpa mekanisme nyata.

---

## 9. Prasyarat sebelum menulis kode fase berikut
- [ ] Baca penuh `TbmroleAction.java` & `TbmuserAction.java`; lengkapi §6.
- [ ] Jalankan `new-ui-rbac-diagnostic.sql` di DB dev → putuskan Opsi A/B (§4.2).
- [ ] Konfirmasi lokasi generator (`tools/…`) dan sistem build (`ant`) yang berjalan.
- [ ] Sediakan lingkungan build+test untuk verifikasi tiap fase.

_— Disusun dari audit source aktual pada branch `feat/new-ui-rbac-role-user`._
