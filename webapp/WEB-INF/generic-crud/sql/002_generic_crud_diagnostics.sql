-- ============================================================================
-- AIS Generic CRUD V2 - diagnostik konfigurasi, job, audit, dan kesehatan data
-- Jalankan read-only dahulu pada staging. Query EXPLAIN harus diisi dengan tabel
-- bisnis aktual; file ini tidak membuat indeks pada tabel bisnis.
-- ============================================================================

-- 1. Ringkasan lifecycle entity
SELECT lifecycle_status, enabled, read_only, count(*) AS jumlah
FROM generic_crud_entity_config
GROUP BY lifecycle_status, enabled, read_only
ORDER BY lifecycle_status, enabled, read_only;

-- 2. Entity enabled tetapi belum FULL_CRUD/READ_ONLY yang valid
SELECT id, entity_key, module_key, page_key, lifecycle_status, enabled, review_notes
FROM generic_crud_entity_config
WHERE enabled = true
  AND lifecycle_status NOT IN ('READ_ONLY','FULL_CRUD')
ORDER BY entity_key;

-- 3. Full CRUD tetapi privilege operasi di config tidak konsisten
SELECT id, entity_key, create_enabled, update_enabled, delete_enabled,
       import_enabled, import_delete_enabled
FROM generic_crud_entity_config
WHERE lifecycle_status = 'FULL_CRUD'
  AND (
      enabled = false
      OR (import_delete_enabled = true AND delete_enabled = false)
      OR (import_enabled = true AND NOT (create_enabled AND update_enabled AND delete_enabled))
  )
ORDER BY entity_key;

-- 4. Entity tanpa identifier/default sort/menu
SELECT id, entity_key, identifier_property, default_sort_property, menu_id,
       scope_adapter_class, adapter_class
FROM generic_crud_entity_config
WHERE enabled = true
  AND (
      identifier_property IS NULL
      OR btrim(identifier_property) = ''
      OR default_sort_property IS NULL
      OR btrim(default_sort_property) = ''
      OR menu_id IS NULL
  )
ORDER BY entity_key;


-- 4b. Page binding enabled tetapi menu/entity tidak siap
SELECT b.id, e.entity_key, b.module_key, b.page_key, b.menu_id,
       b.enabled AS binding_enabled, e.enabled AS entity_enabled,
       e.lifecycle_status
FROM generic_crud_page_binding b
JOIN generic_crud_entity_config e ON e.id = b.entity_config_id
WHERE b.enabled = true
  AND (e.enabled = false OR b.menu_id IS NULL OR e.lifecycle_status NOT IN ('READ_ONLY','FULL_CRUD'))
ORDER BY b.module_key, b.page_key;

-- 4c. Entity dipakai beberapa binding; review menu/scope override tiap konteks
SELECT e.entity_key, count(*) AS binding_count,
       string_agg(b.module_key || '/' || b.page_key, ', ' ORDER BY b.module_key, b.page_key) AS routes
FROM generic_crud_page_binding b
JOIN generic_crud_entity_config e ON e.id = b.entity_config_id
GROUP BY e.entity_key
HAVING count(*) > 1
ORDER BY binding_count DESC, e.entity_key;

-- 5. Entity enabled tetapi tidak mempunyai field aktif
SELECT e.id, e.entity_key, count(f.id) AS active_fields
FROM generic_crud_entity_config e
LEFT JOIN generic_crud_field_config f
       ON f.entity_config_id = e.id
      AND f.active = true
WHERE e.enabled = true
GROUP BY e.id, e.entity_key
HAVING count(f.id) = 0
ORDER BY e.entity_key;

-- 6. Field dengan konfigurasi berisiko/tidak konsisten
SELECT e.entity_key, f.field_key, f.property_path,
       f.sensitive, f.exportable, f.importable,
       f.createable, f.updateable, f.sortable, f.searchable,
       f.relation_entity_key
FROM generic_crud_field_config f
JOIN generic_crud_entity_config e ON e.id = f.entity_config_id
WHERE f.active = true
  AND (
      (f.sensitive = true AND f.exportable = true AND f.masking_mode = 'NONE')
      OR (f.readable = false AND (f.visible_in_table OR f.visible_in_detail OR f.exportable))
      OR (f.visible_in_quick_filter = true AND f.visible_in_advanced_filter = false)
      OR (f.relation_entity_key IS NOT NULL AND f.relation_display_property IS NULL)
      OR (f.required = true AND f.visible_in_form = false AND (f.createable OR f.updateable))
  )
ORDER BY e.entity_key, f.default_position, f.field_key;

-- 7. Duplicate/ambiguous module-page (constraint seharusnya mencegah)
SELECT module_key, page_key, count(*)
FROM generic_crud_entity_config
GROUP BY module_key, page_key
HAVING count(*) > 1;

-- 8. Page-size/threshold/limit yang tidak sesuai policy
SELECT entity_key, default_page_size, max_page_size, lookup_threshold,
       max_export_rows, max_import_rows, synchronous_export_limit
FROM generic_crud_entity_config
WHERE default_page_size NOT IN (5,10,25,50,100,500,1000)
   OR max_page_size NOT IN (5,10,25,50,100,500,1000)
   OR default_page_size > max_page_size
   OR lookup_threshold < 1
   OR max_export_rows <= 0
   OR max_import_rows <= 0
   OR synchronous_export_limit <= 0;

-- 9. Preferensi user yang merujuk entity nonaktif/tidak ada
SELECT v.id, v.user_key, v.active_role_key, v.entity_key, v.view_name
FROM generic_crud_user_view v
LEFT JOIN generic_crud_entity_config e ON e.entity_key = v.entity_key
WHERE e.id IS NULL OR e.enabled = false
ORDER BY v.updated_at DESC;

-- 10. Saved view shared yang perlu review
SELECT id, owner_user_key, owner_role_key, entity_key, view_name,
       visibility, shared_role_keys, active
FROM generic_crud_saved_view
WHERE active = true
  AND visibility <> 'PRIVATE'
ORDER BY entity_key, view_name;

-- 11. Import jobs aktif/stale
SELECT id, job_key, entity_key, owner_user_key, active_role_key,
       status, processed_rows, total_rows, heartbeat_at, created_at
FROM generic_crud_import_job
WHERE status IN ('VALIDATING','QUEUED','RUNNING')
ORDER BY created_at;

-- Sesuaikan interval menurut SLA job.
SELECT id, job_key, entity_key, status, heartbeat_at, created_at
FROM generic_crud_import_job
WHERE status = 'RUNNING'
  AND COALESCE(heartbeat_at, started_at, created_at) < now() - interval '15 minutes'
ORDER BY created_at;

-- 12. Export jobs aktif/stale
SELECT id, job_key, entity_key, format, owner_user_key, active_role_key,
       status, processed_rows, estimated_rows, heartbeat_at, created_at
FROM generic_crud_export_job
WHERE status IN ('QUEUED','RUNNING')
ORDER BY created_at;

SELECT id, job_key, entity_key, format, status, heartbeat_at, created_at
FROM generic_crud_export_job
WHERE status = 'RUNNING'
  AND COALESCE(heartbeat_at, started_at, created_at) < now() - interval '15 minutes'
ORDER BY created_at;

-- 13. Expired result/selection/idempotency yang perlu cleanup
SELECT 'IMPORT' AS object_type, id, job_key AS object_key, expires_at
FROM generic_crud_import_job
WHERE expires_at IS NOT NULL AND expires_at < now()
  AND status NOT IN ('EXPIRED')
UNION ALL
SELECT 'EXPORT', id, job_key, expires_at
FROM generic_crud_export_job
WHERE expires_at IS NOT NULL AND expires_at < now()
  AND status NOT IN ('EXPIRED')
UNION ALL
SELECT 'SELECTION', id, token_key, expires_at
FROM generic_crud_selection_token
WHERE expires_at < now() AND consumed_at IS NULL
UNION ALL
SELECT 'IDEMPOTENCY', id, idempotency_key, expires_at
FROM generic_crud_idempotency
WHERE expires_at < now() AND status <> 'EXPIRED'
ORDER BY expires_at;

-- 14. Import error summary
SELECT j.job_key, j.entity_key, j.status, j.total_rows,
       j.create_rows, j.update_rows, j.delete_rows, j.skip_rows,
       j.error_rows, count(e.id) AS stored_error_rows
FROM generic_crud_import_job j
LEFT JOIN generic_crud_import_row_error e ON e.import_job_id = j.id
GROUP BY j.id, j.job_key, j.entity_key, j.status, j.total_rows,
         j.create_rows, j.update_rows, j.delete_rows, j.skip_rows, j.error_rows
HAVING j.error_rows <> count(e.id)
ORDER BY j.created_at DESC;

-- 15. Audit result summary 30 hari
SELECT entity_key, operation, result_status, count(*) AS jumlah
FROM generic_crud_audit_event
WHERE created_at >= now() - interval '30 days'
GROUP BY entity_key, operation, result_status
ORDER BY entity_key, operation, result_status;

-- 16. Denied/conflict terbanyak untuk investigasi, tanpa data sensitif
SELECT entity_key, operation, result_status, count(*) AS jumlah
FROM generic_crud_audit_event
WHERE created_at >= now() - interval '7 days'
  AND result_status IN ('DENIED','CONFLICT','FAILED')
GROUP BY entity_key, operation, result_status
ORDER BY jumlah DESC, entity_key;

-- 17. Growth/retention estimasi
SELECT 'audit' AS object_type, count(*) AS rows,
       min(created_at) AS oldest, max(created_at) AS newest
FROM generic_crud_audit_event
UNION ALL
SELECT 'import_job', count(*), min(created_at), max(created_at)
FROM generic_crud_import_job
UNION ALL
SELECT 'export_job', count(*), min(created_at), max(created_at)
FROM generic_crud_export_job;

-- 18. Index configuration tables
SELECT schemaname, tablename, indexname, indexdef
FROM pg_indexes
WHERE tablename LIKE 'generic_crud_%'
ORDER BY tablename, indexname;

-- 19. Contoh query plan untuk configuration lookup
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM generic_crud_entity_config
WHERE entity_key = 'ais.database.model.Agama';

EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM generic_crud_user_view
WHERE user_key = 'TEST_USER'
  AND active_role_key = 'TEST_ROLE'
  AND entity_key = 'ais.database.model.Agama'
  AND view_name = 'default';

-- 20. TEMPLATE diagnostik query bisnis.
-- Ganti TABLE_NAME/FIELD sesuai runtime metadata, jangan jalankan mentah.
--
-- EXPLAIN (ANALYZE, BUFFERS)
-- SELECT id, kode, nama, aktif
-- FROM TABLE_NAME
-- WHERE lower(nama) LIKE lower('%kata%')
-- ORDER BY nama ASC, id ASC
-- LIMIT 10 OFFSET 0;
--
-- EXPLAIN (ANALYZE, BUFFERS)
-- SELECT count(id)
-- FROM TABLE_NAME
-- WHERE lower(nama) LIKE lower('%kata%');
