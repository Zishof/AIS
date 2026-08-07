# Referensi Best Practice dan Keputusan Desain

Dokumen ini mencatat referensi utama yang perlu dibaca oleh implementer. Gunakan versi terbaru saat coding.

## Authorization dan keamanan

1. OWASP Authorization Cheat Sheet
   - least privilege;
   - deny by default;
   - validate permission pada setiap request;
   - authorization unit/integration tests.

2. OWASP Mass Assignment Cheat Sheet
   - jangan binding request langsung ke domain entity;
   - gunakan allow-list DTO/command fields.

3. OWASP File Upload Cheat Sheet
   - allow-list extension/type;
   - validasi signature;
   - random/generated filename;
   - size limit;
   - simpan di luar web root;
   - scan/hook bila tersedia.

4. OWASP Input Validation, CSRF Prevention, SQL Injection Prevention, dan IDOR guidance
   - client validation bukan security boundary;
   - mutation memakai CSRF;
   - dynamic field/sort/operator hanya dari allow-list;
   - object-level scope diperiksa setelah menerima ID.

## Database paging dan sorting

1. Hibernate 3.6 Criteria API
   - `setFirstResult` dan `setMaxResults` untuk server-side pagination.

2. PostgreSQL LIMIT/OFFSET
   - hasil page harus mempunyai `ORDER BY` yang deterministic;
   - OFFSET besar dapat tidak efisien;
   - gunakan keyset/hybrid untuk page sangat dalam.

## Office document generation

1. Apache POI XSSF/SXSSF
   - SXSSF untuk streaming worksheet besar;
   - temporary files harus dibersihkan;
   - tetap lakukan row/column/file limits.

2. Apache POI XWPF
   - dokumen Word OOXML/DOCX.

3. Apache POI XSLF
   - presentasi PowerPoint OOXML/PPTX.

4. JasperReports exporter existing pada AIS
   - prioritaskan reuse PDF/DOCX/PPTX exporter dan template/report helper existing.

## Responsive dan accessibility

1. W3C WCAG Reflow
   - konten/fungsi tetap tersedia pada lebar kecil dan zoom tinggi;
   - hindari scroll dua dimensi untuk penggunaan utama.

2. W3C Forms Tutorial
   - label eksplisit;
   - grouping dan instruksi yang jelas;
   - responsive form.

3. GOV.UK Design System — Error summary dan validation errors
   - error summary di atas form;
   - link ke field;
   - pertahankan nilai pengguna setelah validasi gagal;
   - pesan menjelaskan cara memperbaiki.

## Penerapan pada AIS

Keputusan implementasi yang diambil:

- Generic CRUD tidak mengaktifkan semua entity secara otomatis.
- Hibernate runtime metadata menjadi bukti property mapped.
- Satu entity dapat mempunyai beberapa page/menu binding.
- Privilege dan scope diperiksa pada page, endpoint, object, dan job.
- Paging/filter/sort terjadi di database.
- Import selalu dry-run sebelum mutation.
- Export besar menjadi background job.
- Form tidak melakukan mass assignment.
- Mobile memakai card/list adaptation, bukan sekadar tabel diperkecil.
- Error penting tidak hanya berupa toast.



## Audit, restore, destructive action, dan complex tabs

Referensi resmi tambahan untuk implementasi V2:

- Hibernate ORM/Envers documentation — revision entities, audit queries, dan historical entity state: https://docs.jboss.org/hibernate/envers/3.6/reference/en-US/html_single/
- OWASP Authorization Cheat Sheet — least privilege, deny by default, dan permission validation pada setiap request: https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html
- OWASP Logging Cheat Sheet — event attributes, protection, masking, dan log integrity: https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html
- WAI-ARIA APG Tabs Pattern — tablist/tab/tabpanel dan keyboard interaction: https://www.w3.org/WAI/ARIA/apg/patterns/tabs/
- WAI-ARIA APG Dialog Modal Pattern — focus management dan modal semantics: https://www.w3.org/WAI/ARIA/apg/patterns/dialog-modal/
- WAI-ARIA APG Alert Dialog Pattern — destructive/confirmation dialog: https://www.w3.org/WAI/ARIA/apg/patterns/alertdialog/
