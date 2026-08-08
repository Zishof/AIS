# New UI Hybrid Menu — Existing Parity

Baseline implementasi: `b738f8de` (Menu/RBAC New UI V1), setelah digabungkan dengan `origin/master` pada branch `feat/new-ui-hybrid-menu-rbac`.

## Perilaku existing yang dipertahankan

| Area | Perilaku legacy/existing | Implementasi hybrid |
|---|---|---|
| Role aktif | `Common.getCurrentUser(request)` lalu `Tbmuser.hakAkses()`; multi-role memakai `Tbmuser.getUserRoleYgDipakai` | Snapshot selalu diberi marker user, role aktif, konteks lembaga, dan versi cache. Privilege antar-role tidak digabung. |
| Assignment | `CommonMenu.loadTree()` memuat `tbmrole.getMenus()` dan menyimpan daftar terurut ke session `current_menus` | Hanya hasil join `Tbmrole.menus`/`job_has_menu` untuk role aktif yang dimaterialisasi. Menu tidak assigned tidak masuk DTO atau DOM. |
| Hierarchy | Root `0`; child dicari ketika `childMenu.root == parentMenu.child`; urutan `nomorUrut, root, child` | Relasi sama, dengan tie-breaker `id`, post-order classification, batas depth, serta diagnosis duplicate/cycle/orphan. |
| Active path | `current_menu` dan `MainHelper.parents()` membuka ancestor menu aktif | `groupMenuId`/`menuId` diotorisasi server-side; snapshot menyediakan breadcrumb dan ancestor untuk auto-expand. |
| Privilege | `CommonPrivilages` memeriksa pasangan role + `Menu` pada `RolePrivilage` untuk READ/CREATE/UPDATE/DELETE/APPROVE/REJECT | Permission dimuat batch. Nilai kosong/tidak ada baris adalah deny. READ menentukan route; privilege aksi tetap diperiksa oleh route/service guard. |
| Scope lembaga | `Menu.aktif`, `tampilDiPt`, `tampilDiSekolah`, serta konfigurasi `aktifkan_filter_per_sekolah` | Filter yang sama diterapkan sebelum klasifikasi branch/leaf. |
| Cache | `current_menus`, `current_menu`, dan `CommonPrivilages.rolePrivilagesUtama` | Cache snapshot hybrid per session/user/role/scope; invalidasi juga membersihkan cache existing tersebut. |
| Route legacy | `Menu.url` dibuka melalui entry point `/baru` dan menu aktif membawa ID menu | Route legacy hanya dibentuk dari URL internal aman setelah assignment + READ tervalidasi. |
| Route New UI | Registry explicit `Menu.id`/URL memetakan beberapa menu ke module/page JSP | Mapping explicit tetap prioritas pertama; route ditandai `NEW_UI`, fallback aman `LEGACY_EMBED`, atau `NOT_MAPPED`. |
| Include konten | Shell frame merender target dengan `pageContext.include(target, true)` | Pola tersebut dipertahankan; query/tree/guard tidak ditempatkan dalam renderer JSP. |
| Pencarian | V1 Ctrl+K memakai tree authorized, sedangkan katalog source generator terpisah | Ctrl+K memakai snapshot authorized branch + leaf. Pencarian lokal hanya menerima leaf katalog branch yang dipilih. |

## Perubahan navigasi V2

- Node dengan minimal satu visible child menjadi `BRANCH` dan hanya dirender di sidebar.
- Node tanpa visible child, assignment valid, READ=1, dan route valid menjadi `LEAF` dan hanya dirender sebagai card katalog.
- Parent READ=0 dengan descendant readable tetap menjadi structural branch; route parent tidak dapat dibuka.
- Parent branch dengan READ dan route valid mendapat tombol `Buka Ringkasan` di hero katalog.
- Root leaf atau assignment tanpa parent assigned ditempatkan pada katalog virtual `Menu Lainnya`; orphan dicatat dalam diagnostik.
- Badge branch menghitung leaf authorized descendant, bukan class/action hasil scanner.

## Routing dan guard

- `/new?groupMenuId=<id>` hanya memilih branch visible dalam snapshot role aktif.
- `/new?menuId=<id>` hanya membuka node assigned + READ + route valid.
- `module` dan `page` tidak lagi cukup sebagai otorisasi request browser; keduanya hanya hasil resolution sesudah `menuId` lolos guard.
- Request service/action tetap memeriksa READ dan privilege mutasi pada permission node yang sama.
- Status route yang dikenali: `NEW_UI`, `LEGACY_EMBED`, `LEGACY_REDIRECT`, `FORBIDDEN`, `NOT_MAPPED`, dan `NOT_FOUND`.

## File yang diubah

- `src/ais/common/newui/menu/**`: snapshot, tree, catalog, route, cache, dan diagnostics hybrid.
- `src/ais/common/newui/NewUiMenuAccessService.java`, `NewUiRouteGuard.java`, `NewUiCacheInvalidator.java`: facade kompatibilitas V1.
- `webapp/WEB-INF/new/index.jsp`: adapter tipis untuk snapshot dan route guard.
- `webapp/WEB-INF/new/_shared/menu/**`: renderer sidebar branch, breadcrumb, leaf catalog/card, empty state, diagnostics.
- `webapp/WEB-INF/new/_shared/ui/sidebar.jsp` dan `command_palette.jsp`: delegasi ke snapshot hybrid.
- `webapp/WEB-INF/new/_shared/assets/new-ui.css` dan `new-ui.js`: styling/interaction additive.
- `tools/generate_new_jsp_scaffold.py`: validator agar navigasi produksi tetap hybrid/RBAC dan partial tidak ditimpa generator.
