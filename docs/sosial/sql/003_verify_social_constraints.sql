-- Read-only constraint/index evidence. Simpan output ke TEST_EVIDENCE.md.
SELECT n.nspname AS schema_name,t.relname AS table_name,c.conname,c.contype,pg_get_constraintdef(c.oid) AS definition
FROM pg_constraint c JOIN pg_class t ON t.oid=c.conrelid JOIN pg_namespace n ON n.oid=t.relnamespace
WHERE n.nspname='public' AND t.relname IN
('social_tenant_setting','sosial_channel','social_donor_identity','jenis_dana_sosial','jenis_zakat','kebijakan_perhitungan_zakat','perhitungan_zakat','social_program_extension','transaksi_donasi','alokasi_donasi','pembayaran_donasi','perkembangan_program_sosial','kategori_penerima_manfaat','detail_penyaluran_donasi','bukti_setor_sosial','social_payment_reconciliation','social_correction_event','social_prayer_message')
ORDER BY t.relname,c.contype,c.conname;

-- Unique business keys. PASS bila tidak ada MISSING.
WITH expected(table_name,fragments) AS (VALUES
 ('social_tenant_setting','tenant_key'),('sosial_channel','tenant_key,kode'),
 ('social_donor_identity','tenant_key,tbmuser_id'),('social_donor_identity','tenant_key,donatur_id'),
 ('jenis_dana_sosial','tenant_key,kode'),('jenis_zakat','tenant_key,kode'),
 ('social_program_extension','program_id'),('social_program_extension','tenant_key,slug'),
 ('transaksi_donasi','transaction_number'),('transaksi_donasi','tenant_key,idempotency_key'),
 ('pembayaran_donasi','gateway_id,gateway_order_id'),
 ('social_payment_reconciliation','tenant_key,gateway,settlement_reference'),
 ('bukti_setor_sosial','tenant_key,receipt_number'),('bukti_setor_sosial','verification_token'),
 ('social_correction_event','tenant_key,request_id')
), actual AS (
 SELECT t.relname table_name,replace(replace(pg_get_constraintdef(c.oid),'UNIQUE (',''),')','') fragments
 FROM pg_constraint c JOIN pg_class t ON t.oid=c.conrelid JOIN pg_namespace n ON n.oid=t.relnamespace
 WHERE n.nspname='public' AND c.contype='u'
)
SELECT 'MISSING_UNIQUE' issue,e.* FROM expected e LEFT JOIN actual a ON a.table_name=e.table_name AND replace(a.fragments,' ','')=e.fragments WHERE a.table_name IS NULL;

SELECT schemaname,tablename,indexname,indexdef FROM pg_indexes
WHERE schemaname='public' AND tablename LIKE ANY(ARRAY['%sosial%','%donasi%','jenis_dana_sosial','jenis_zakat','perhitungan_zakat','kategori_penerima_manfaat'])
ORDER BY tablename,indexname;
